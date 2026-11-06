package pack.engine;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import static pack.engine.Vector2.*;

// simulação de agentes autônomos(serve como "main")
public final class Simulation {
    public static final int SIDEBAR = 350;
    private static final int MARGIN = 14;
    private static final double MAX_DT = 1.0 / 30.0;
    private static final double SLOW_MO = 0.3;

    private static final Color BG = new Color(20, 22, 27);
    private static final Color PANEL = new Color(28, 31, 38);
    private static final Color LINE = new Color(52, 57, 68);
    private static final Color TEXT = new Color(226, 229, 236);
    private static final Color MUTED = new Color(146, 152, 166);
    private static final Color ACCENT = new Color(255, 196, 77);
    private static final Color CHANGED = new Color(120, 220, 255);

    private static final Font TITLE = new Font("SansSerif", Font.BOLD, 17);
    private static final Font HEADER = new Font("SansSerif", Font.BOLD, 12);
    private static final Font BODY = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font SMALL = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font BIG = new Font("SansSerif", Font.BOLD, 34);

    private final List<Supplier<Scene>> factories;
    private final ConcurrentLinkedQueue<Runnable> input = new ConcurrentLinkedQueue<>();

    private int sceneIndex = 0;
    private Scene scene;
    private double time = 0;
    private boolean paused = false;
    private boolean slowMo = false;
    private boolean showHelp = true;
    private Agent selected;
    private Vector2 mouseWorld;
    private Vector2 clickWorld;
    private int groupIndex = 0;
    private int weightIndex = 0;

    // transformação mundo -> tela (calculada no render)
    private volatile double scale = 1;
    private volatile double offX = 0;
    private volatile double offY = 0;
    private volatile int mapAreaW = 1;

    private double fps = 0;
    private int frames = 0;
    private long fpsStart = System.nanoTime();

    public Simulation(List<Supplier<Scene>> factories) {
        if (factories.isEmpty()) throw new IllegalArgumentException("Setup must create at least one scene");
        this.factories = List.copyOf(factories);
        loadScene(0);
    }

    //cena
    private void loadScene(int index) {
        sceneIndex = Math.floorMod(index, factories.size());
        scene = factories.get(sceneIndex).get();
        time = 0;
        selected = scene.getDefaultSelection();
        clickWorld = null;
        if (groupIndex >= scene.getTuning().size()) groupIndex = 0;
        weightIndex = 0;
    }

    public Scene getScene() { return scene; }
    public double getTime() { return time; }

    //input
    public void keyPressed(int keyCode, boolean shift) {
        input.add(() -> handleKey(keyCode, shift));
    }

    public void mouseMoved(int sx, int sy) {
        input.add(() -> {
            if (sx < mapAreaW) mouseWorld = toWorld(sx, sy);
        });
    }

    public void mousePressed(int sx, int sy, int button) {
        input.add(() -> {
            if (sx >= mapAreaW) return;
            final var p = toWorld(sx, sy);
            if (button == MouseEvent.BUTTON1) {
                // clicar em um agente seleciona ele ou uma posição no mundo
                final var hit = agentAt(p);
                if (hit != null && hit != selected) {
                    selected = hit;
                } else {
                    clickWorld = p;
                }
            } else {
                clickWorld = null;
            }
        });
    }

    private Agent agentAt(Vector2 p) {
        Agent best = null;
        double bestD = Double.MAX_VALUE;
        for (var a : scene.getAgents()) {
            final double d = Vector2.distance(a.getPosition(), p);
            if (d <= a.getRadius() * 1.8 + 6 && d < bestD) {
                best = a;
                bestD = d;
            }
        }
        return best;
    }

    private Vector2 toWorld(int sx, int sy) {
        return vec2((sx - offX) / scale, (sy - offY) / scale);
    }

    private WeightGroup currentGroup() {
        final var groups = scene.getTuning();
        if (groups.isEmpty()) return null;
        return groups.get(Math.floorMod(groupIndex, groups.size()));
    }

    private void handleKey(int code, boolean shift) {
        final var group = currentGroup();
        final int n = group == null ? 0 : group.getWeights().size();
        switch (code) {
            case KeyEvent.VK_D -> Debug.enabled = !Debug.enabled;
            case KeyEvent.VK_V -> Debug.vision = !Debug.vision;
            case KeyEvent.VK_L -> Debug.labels = !Debug.labels;
            case KeyEvent.VK_H -> showHelp = !showHelp;
            case KeyEvent.VK_P, KeyEvent.VK_SPACE -> paused = !paused;
            case KeyEvent.VK_T -> slowMo = !slowMo;
            case KeyEvent.VK_R -> loadScene(sceneIndex);
            case KeyEvent.VK_TAB -> loadScene(sceneIndex + (shift ? -1 : 1));
            case KeyEvent.VK_UP -> { if (n > 0) weightIndex = Math.floorMod(weightIndex - 1, n); }
            case KeyEvent.VK_DOWN -> { if (n > 0) weightIndex = Math.floorMod(weightIndex + 1, n); }
            case KeyEvent.VK_LEFT -> { if (n > 0) group.getWeights().get(weightIndex).decrease(shift ? 5 : 1); }
            case KeyEvent.VK_RIGHT -> { if (n > 0) group.getWeights().get(weightIndex).increase(shift ? 5 : 1); }
            case KeyEvent.VK_PAGE_UP, KeyEvent.VK_COMMA -> { groupIndex--; weightIndex = 0; }
            case KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_PERIOD -> { groupIndex++; weightIndex = 0; }
            case KeyEvent.VK_BACK_SPACE -> { if (group != null) group.resetAll(); }
            case KeyEvent.VK_S -> printWeights();
            default -> scene.key(code);
        }
    }

    private void printWeights() {
        System.out.printf(Locale.ROOT, "%n==== Pesos atuais (%s, t=%.1fs) ====%n", scene.getTitle(), time);
        for (var g : scene.getTuning()) {
            System.out.println("[" + g.getName() + "]");
            for (var w : g.getWeights()) {
                System.out.printf(Locale.ROOT, "  %-26s = %-6s (padrão %s)%n", w.getName(), w.format(), w.format(w.getDefault()));
            }
        }
    }

    //update do mundo
    public void update(double realSecs) {
        Runnable r;
        while ((r = input.poll()) != null) r.run();

        frames++;
        final long now = System.nanoTime();
        if (now - fpsStart >= 500_000_000L) {
            fps = frames * 1e9 / (now - fpsStart);
            frames = 0;
            fpsStart = now;
        }

        if (paused) return;
        final double dt = Math.min(realSecs, MAX_DT) * (slowMo ? SLOW_MO : 1.0);
        if (dt <= 0) return;
        time += dt;

        scene.update(dt, time);
        final var agents = scene.getAgents();
        for (var agent : agents) {
            agent.update(new World(dt, time, agent, agents, mouseWorld, clickWorld, scene.getMap()));
        }
    }

    //desenho do mundo
    public void render(Graphics2D g, int width, int height) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        g.setColor(BG);
        g.fillRect(0, 0, width, height);

        final var map = scene.getMap();
        final int areaW = Math.max(200, width - SIDEBAR);
        final double availW = areaW - MARGIN * 2.0;
        final double availH = height - MARGIN * 2.0;
        final double s = Math.max(0.05, Math.min(availW / (map.getWidth() + 24), availH / (map.getHeight() + 24)));
        mapAreaW = areaW;
        scale = s;
        offX = MARGIN + availW / 2.0;
        offY = MARGIN + availH / 2.0;

        // ---------------- mundo ----------------
        final var gw = (Graphics2D) g.create();
        gw.clipRect(0, 0, areaW, height);
        gw.translate(offX, offY);
        gw.scale(s, s);
        gw.clip(new Rectangle2D.Double(-map.getWidth() / 2 - 12, -map.getHeight() / 2 - 12,
            map.getWidth() + 24, map.getHeight() + 24));

        map.drawGround(gw);
        scene.drawUnderlays(gw);
        map.drawObstacles(gw);
        map.drawBorders(gw);

        for (var a : scene.getAgents()) {
            a.draw(gw, Debug.enabled, a == selected);
        }
        scene.drawOverlays(gw);

        if (Debug.enabled && selected != null) drawForceArrows(gw, selected);
        if (Debug.enabled && Debug.labels) drawLabels(gw);

        if (clickWorld != null) {
            Draw.circle(gw, clickWorld, 7, new Color(255, 255, 255, 180), 1.5f);
            Draw.fillCircle(gw, clickWorld, 2.5, new Color(255, 255, 255, 200));
        }
        gw.dispose();

        if (paused) {
            g.setFont(BIG);
            final var fm = g.getFontMetrics();
            final String txt = "PAUSADO";
            g.setColor(new Color(0, 0, 0, 140));
            g.drawString(txt, (areaW - fm.stringWidth(txt)) / 2 + 2, 62);
            g.setColor(Color.WHITE);
            g.drawString(txt, (areaW - fm.stringWidth(txt)) / 2, 60);
        }

        drawSidebar(g, areaW, width, height);
    }

    //desenho de labels dos agentes
    private void drawLabels(Graphics2D g) {
        g.setFont(Draw.LABEL_FONT);
        final var fm = g.getFontMetrics();
        final int lineH = fm.getHeight();
        final var agents = new ArrayList<>(scene.getAgents());
        agents.sort((a, b) -> a == selected ? -1 : b == selected ? 1 : Double.compare(b.getPosition().y, a.getPosition().y));
        final var placed = new ArrayList<Rectangle2D>();
        for (var a : agents) {
            final var text = a.getLabel();
            if (text == null) continue;
            final var lines = text.split("\n");
            int w = 0;
            for (var l : lines) w = Math.max(w, fm.stringWidth(l));
            final double h = lines.length * lineH;
            final var pos = a.getPosition();
            final double anchorY = pos.y - a.getRadius() * 2.0 - 4;
            final var rect = new Rectangle2D.Double(pos.x - w / 2.0 - 3, anchorY - h, w + 6, h + 2);
            for (int tries = 0; tries < 8; tries++) {
                Rectangle2D hit = null;
                for (var r : placed) {
                    if (r.intersects(rect)) {
                        hit = r;
                        break;
                    }
                }
                if (hit == null) break;
                rect.y = hit.getY() - rect.height - 1;
            }
            placed.add(rect);
            if (rect.y + rect.height < anchorY - 2) {
                Draw.line(g, vec2(pos.x, pos.y - a.getRadius()), vec2(pos.x, rect.y + rect.height), new Color(255, 255, 255, 90), 1f);
            }
            g.setColor(new Color(10, 12, 16, 120));
            g.fill(new java.awt.geom.RoundRectangle2D.Double(rect.x, rect.y, rect.width, rect.height, 6, 6));
            double y = rect.y + fm.getAscent();
            final var ring = a.getRingColor();
            for (int i = 0; i < lines.length; i++) {
                final float x = (float) (pos.x - fm.stringWidth(lines[i]) / 2.0);
                g.setColor(new Color(0, 0, 0, 170));
                g.drawString(lines[i], x + 1, (float) y + 1);
                g.setColor(i == 0 || ring == null ? Color.WHITE : ring);   // 2ª linha (estado) na cor do estado
                g.drawString(lines[i], x, (float) y);
                y += lineH;
            }
        }
    }

    //desenho das setas de força do agente selecionado
    private void drawForceArrows(Graphics2D g, Agent agent) {
        final var origin = add(agent.getPosition(), multiply(agent.getDirection(), agent.getRadius() * 0.8));
        for (var f : agent.getForceReport()) {
            if (f.weight() <= 0) continue;
            if (distance(f.wanted(), f.applied()) > 1) {
                Draw.arrow(g, origin, f.wanted(), 0.12, f.color(), 0.30f, 1.5f);
            }
            Draw.arrow(g, origin, f.applied(), 0.12, f.color(), 0.95f, 3f);
        }
    }

    //painel lateral
    private void drawSidebar(Graphics2D g, int x0, int width, int height) {
        g.setColor(PANEL);
        g.fillRect(x0, 0, width - x0, height);
        g.setColor(LINE);
        g.fillRect(x0, 0, 1, height);

        final int pad = 16;
        final int left = x0 + pad;
        final int right = width - pad;
        final int colW = right - left;
        int y = 28;

        // título
        g.setFont(TITLE);
        g.setColor(TEXT);
        g.drawString(fit(g, scene.getTitle(), colW - 70), left, y);
        g.setFont(SMALL);
        g.setColor(MUTED);
        final String sceneTag = "cena %d/%d  TAB".formatted(sceneIndex + 1, factories.size());
        g.drawString(sceneTag, right - g.getFontMetrics().stringWidth(sceneTag), y);
        y += 18;
        g.setFont(BODY);
        for (var line : wrap(g, scene.getSubtitle(), colW)) {
            g.drawString(line, left, y);
            y += 15;
        }
        y += 2;
        g.setColor(paused ? ACCENT : MUTED);
        final String clock = String.format(Locale.ROOT, "%s  %.0f fps  t = %.1f s  %s",
            paused ? "PAUSADO" : "rodando", fps, time, slowMo ? "câmera lenta 0.3x" : "1x");
        g.drawString(clock, left, y);
        y += 10;
        y = separator(g, x0, width, y);

        // status da cena
        for (var line : scene.statusLines()) {
            int tx = left;
            if (line.color() != null) {
                g.setColor(line.color());
                g.fill(new Ellipse2D.Double(left, y - 9, 9, 9));
                tx += 15;
            }
            g.setColor(TEXT);
            g.setFont(BODY);
            for (var l : wrap(g, line.text(), right - tx)) {
                g.drawString(l, tx, y);
                y += 15;
            }
        }
        y = separator(g, x0, width, y - 4);

        // pesos
        final var group = currentGroup();
        if (group != null) {
            final var groups = scene.getTuning();
            g.setFont(HEADER);
            g.setColor(ACCENT);
            g.drawString("PESOS · " + group.getName(), left, y);
            g.setFont(SMALL);
            g.setColor(MUTED);
            final String gi = "%d/%d  PgUp/PgDn".formatted(Math.floorMod(groupIndex, groups.size()) + 1, groups.size());
            g.drawString(gi, right - g.getFontMetrics().stringWidth(gi), y);
            y += 18;
            final int barX = left + 168;
            final int barW = colW - 168 - 44;
            final var weights = group.getWeights();
            for (int i = 0; i < weights.size(); i++) {
                final var w = weights.get(i);
                final boolean sel = i == weightIndex;
                if (sel) {
                    g.setColor(new Color(255, 255, 255, 18));
                    g.fillRect(left - 6, y - 12, colW + 12, 17);
                }
                g.setFont(BODY);
                g.setColor(sel ? TEXT : new Color(200, 204, 214));
                g.drawString((sel ? "> " : "  ") + fit(g, w.getName(), 158), left - 4, y);
                g.setColor(LINE);
                g.fillRect(barX, y - 8, barW, 6);
                g.setColor(w.isDefault() ? ACCENT : CHANGED);
                g.fillRect(barX, y - 8, (int) Math.round(barW * w.ratio()), 6);
                final String val = w.format();
                g.setColor(w.isDefault() ? TEXT : CHANGED);
                g.drawString(val, right - g.getFontMetrics().stringWidth(val), y);
                y += 17;
            }
            if (!weights.isEmpty()) {
                final var w = weights.get(Math.floorMod(weightIndex, weights.size()));
                g.setFont(SMALL);
                g.setColor(MUTED);
                var desc = w.getDescription();
                if (!w.isDefault()) desc += " (padrão " + w.format(w.getDefault()) + ")";
                for (var l : wrap(g, desc, colW)) {
                    g.drawString(l, left, y);
                    y += 14;
                }
            }
            y = separator(g, x0, width, y - 2);
        }

        // forças do agente selecionado
        if (selected != null) {
            g.setFont(HEADER);
            g.setColor(ACCENT);
            final var st = selected.getStateName();
            g.drawString(fit(g, "FORÇAS · " + selected.getName() + (st != null ? " (" + st + ")" : ""), colW), left, y);
            y += 18;
            final double maxF = selected.getMaxForce();
            final int barX = left + 150;
            final int barW = colW - 150 - 40;
            for (var f : selected.getForceReport()) {
                final boolean on = f.weight() > 0;
                final double applied = f.applied().size();
                final double wanted = f.wanted().size();
                g.setFont(BODY);
                g.setColor(on ? f.color() : new Color(110, 114, 124));
                g.fill(new Rectangle2D.Double(left, y - 9, 8, 8));
                g.setColor(on ? TEXT : new Color(110, 114, 124));
                g.drawString(fit(g, f.label(), 136), left + 13, y);
                g.setColor(LINE);
                g.fillRect(barX, y - 8, barW, 7);
                if (on) {
                    final int wantedW = (int) Math.min(barW, barW * wanted / maxF);
                    final int appliedW = (int) Math.min(barW, barW * applied / maxF);
                    g.setColor(Draw.alpha(f.color(), 0.35f));
                    g.fillRect(barX, y - 8, wantedW, 7);
                    g.setColor(f.color());
                    g.fillRect(barX, y - 8, appliedW, 7);
                    if (wanted > maxF) {
                        g.setColor(TEXT);
                        g.drawString(">", barX + barW + 2, y);
                    }
                }
                final String val = on ? "%.0f".formatted(applied) : "--";
                g.setColor(on ? TEXT : new Color(110, 114, 124));
                g.drawString(val, right - g.getFontMetrics().stringWidth(val), y);
                y += 16;
            }
            final double total = selected.getLastSteering().size();
            g.setFont(BODY);
            g.setColor(new Color(255, 69, 0));
            g.fill(new Rectangle2D.Double(left, y - 9, 8, 8));
            g.setColor(TEXT);
            g.drawString("Total (steering)", left + 13, y);
            g.setColor(LINE);
            g.fillRect(barX, y - 8, barW, 7);
            g.setColor(new Color(255, 69, 0));
            g.fillRect(barX, y - 8, (int) Math.min(barW, barW * total / maxF), 7);
            final String tv = "%.0f".formatted(total);
            g.drawString(tv, right - g.getFontMetrics().stringWidth(tv), y);
            y += 16;
            g.setFont(SMALL);
            g.setColor(MUTED);
            g.drawString(String.format(Locale.ROOT, "barra cheia = maxForce (%.0f) · clara = pedido, forte = aplicado", maxF), left, y);
            y += 14;
            g.drawString(String.format(Locale.ROOT, "velocidade %.0f / %.0f px/s · batidas em obstáculos: %d",
                selected.getSpeed(), selected.getMaxSpeed(), selected.getContacts()), left, y);
            y += 8;
            y = separator(g, x0, width, y);
        }

        // ajuda
        g.setFont(SMALL);
        if (showHelp) {
            final var lines = new ArrayList<String>(scene.getHelp());
            lines.add("D debug · V visão · L rótulos · H oculta ajuda");
            lines.add("P pausa · T câmera lenta · R reinicia · TAB troca cena");
            lines.add("setas: escolhe/ajusta peso (Shift = x5) · Backspace restaura");
            lines.add("S imprime os pesos no console · clique seleciona agente");
            for (var line : lines) {
                for (var l : wrap(g, line, colW)) {
                    g.setColor(l.startsWith("D ") || l.startsWith("P ") || l.startsWith("setas") || l.startsWith("S ")
                        ? MUTED : new Color(200, 204, 214));
                    g.drawString(l, left, y);
                    y += 14;
                }
            }
        } else {
            g.setColor(MUTED);
            g.drawString("H mostra os controles", left, y);
        }
    }

    private int separator(Graphics2D g, int x0, int width, int y) {
        g.setColor(LINE);
        g.fillRect(x0 + 12, y, width - x0 - 24, 1);
        return y + 20;
    }

    private static String fit(Graphics2D g, String s, int maxW) {
        final var fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= maxW) return s;
        var t = s;
        while (t.length() > 1 && fm.stringWidth(t + "…") > maxW) t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    private static List<String> wrap(Graphics2D g, String text, int maxW) {
        final var out = new ArrayList<String>();
        if (text == null || text.isEmpty()) return out;
        final var fm = g.getFontMetrics();
        for (var paragraph : text.split("\n")) {
            var line = new StringBuilder();
            for (var word : paragraph.split(" ")) {
                final var candidate = line.isEmpty() ? word : line + " " + word;
                if (fm.stringWidth(candidate) > maxW && !line.isEmpty()) {
                    out.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            out.add(line.toString());
        }
        return out;
    }
}
