package pack.engine;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

import static pack.engine.Vector2.*;

/**
 * Mapa de testes: limites (paredes nas bordas), obstáculos e caminhos nomeados.
 * Coordenadas com a origem no centro
 *
 * <pre>
 * var map = new GameMap("Arena", 1280, 760)
 *     .circle(-250, -120, 26)               // pilar
 *     .box(-60, -40, 24, 150)               // muro (centro, largura, altura)
 *     .path("presa", true, 26, x0,y0, x1,y1, ...);
 * </pre>
 */
public final class GameMap {
    public static final double DEFAULT_WIDTH = 1280;
    public static final double DEFAULT_HEIGHT = 760;
    private static final double BORDER = 80;

    private final String name;
    private final double width;
    private final double height;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<BoxObstacle> borders = new ArrayList<>();
    private final Map<String, Path> paths = new LinkedHashMap<>();
    private final List<Patch> patches = new ArrayList<>();
    private Color ground = new Color(70, 85, 60);
    private long seed = 7;

    private record Patch(double x, double y, double rx, double ry, Color color) { }

    public GameMap(String name, double width, double height) {
        this.name = name;
        this.width = width;
        this.height = height;
        final double hw = width / 2, hh = height / 2;
        // paredes externas (ficam fora da área jogável)
        borders.add(new BoxObstacle(0, -hh - BORDER / 2, width + BORDER * 2, BORDER));
        borders.add(new BoxObstacle(0, hh + BORDER / 2, width + BORDER * 2, BORDER));
        borders.add(new BoxObstacle(-hw - BORDER / 2, 0, BORDER, height + BORDER * 2));
        borders.add(new BoxObstacle(hw + BORDER / 2, 0, BORDER, height + BORDER * 2));
    }

    // ---------------- construção ----------------

    public GameMap circle(double x, double y, double radius) {
        obstacles.add(new CircleObstacle(x, y, radius));
        return this;
    }

    /** Bloco retangular: centro (cx, cy), largura w e altura h. */
    public GameMap box(double cx, double cy, double w, double h) {
        obstacles.add(new BoxObstacle(cx, cy, w, h));
        return this;
    }

    /** Caminho nomeado. xy = x0, y0, x1, y1, ... */
    public GameMap path(String pathName, boolean loop, double radius, double... xy) {
        if (xy.length % 2 != 0) throw new IllegalArgumentException("xy must have pairs");
        final var pts = new ArrayList<Vector2>();
        for (int i = 0; i < xy.length; i += 2) pts.add(vec2(xy[i], xy[i + 1]));
        paths.put(pathName, new Path(pathName, loop, radius, pts));
        return this;
    }

    /** Mancha decorativa no chão (sem colisão). */
    public GameMap patch(double x, double y, double rx, double ry, Color color) {
        patches.add(new Patch(x, y, rx, ry, color));
        return this;
    }

    public GameMap ground(Color color) {
        this.ground = color;
        return this;
    }

    public GameMap seed(long seed) {
        this.seed = seed;
        return this;
    }

    // ---------------- consultas ----------------

    public String getName() { return name; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public List<Obstacle> getObstacles() { return Collections.unmodifiableList(obstacles); }
    public Path getPath(String pathName) { return paths.get(pathName); }
    public Collection<Path> getPaths() { return Collections.unmodifiableCollection(paths.values()); }

    private Iterable<Obstacle> allObstacles() {
        final var all = new ArrayList<Obstacle>(obstacles.size() + borders.size());
        all.addAll(obstacles);
        all.addAll(borders);
        return all;
    }

    //raycast que reconhece o primeiro obstaculo
    public RayHit raycast(Vector2 origin, Vector2 dir, double maxDist, double inflate) {
        if (Vector2.isZero(dir)) return null;
        final var d = normalize(dir);
        RayHit best = null;
        for (var o : allObstacles()) {
            final var hit = o.raycast(origin, d, maxDist, inflate);
            if (hit != null && (best == null || hit.distance() < best.distance())) best = hit;
        }
        return best;
    }

    /** True se nada bloqueia a linha reta de a até b. */
    public boolean hasLineOfSight(Vector2 a, Vector2 b) {
        final var d = subtract(b, a);
        final double len = d.size();
        if (len < 1e-6) return true;
        return raycast(a, d, len, 0) == null;
    }

    /** True se um círculo de raio r em p não encosta em nenhum obstáculo. */
    public boolean isFree(Vector2 p, double r) {
        for (var o : allObstacles()) {
            if (o.penetration(p, r) != null) return false;
        }
        return true;
    }

    /** Empurra o ponto para fora dos obstáculos (ponto livre mais próximo, aproximado). */
    public Vector2 freePoint(Vector2 p, double r) {
        final var q = p.clone();
        for (int iter = 0; iter < 4; iter++) {
            boolean moved = false;
            for (var o : allObstacles()) {
                final var c = o.penetration(q, r);
                if (c != null) {
                    q.add(multiply(c.normal(), c.depth() + 0.5));
                    moved = true;
                }
            }
            if (!moved) break;
        }
        return q;
    }

    public Vector2 randomFreePoint(Random rnd, double margin) {
        for (int i = 0; i < 200; i++) {
            final var p = vec2(
                (rnd.nextDouble() - 0.5) * (width - margin * 2),
                (rnd.nextDouble() - 0.5) * (height - margin * 2));
            if (isFree(p, margin)) return p;
        }
        return vec2();
    }

    // colisão do agente com paredes
    boolean resolveCollisions(Agent agent) {
        final var p = agent.getPosition();
        final var v = agent.getVelocity();
        final double r = agent.getRadius();
        boolean touched = false;
        for (int iter = 0; iter < 2; iter++) {
            for (var o : allObstacles()) {
                final var c = o.penetration(p, r);
                if (c == null) continue;
                touched = true;
                p.add(multiply(c.normal(), c.depth()));
                final double vn = v.dot(c.normal());
                if (vn < 0) v.subtract(multiply(c.normal(), vn)); // desliza na parede
            }
        }
        if (touched) agent.setPhysics(p, v);
        return touched;
    }

    // ---------------- desenho ----------------

    void drawGround(Graphics2D g) {
        final double hw = width / 2, hh = height / 2;
        g.setColor(ground);
        g.fill(new Rectangle2D.Double(-hw, -hh, width, height));

        for (var p : patches) {
            g.setColor(p.color());
            g.fill(new Ellipse2D.Double(p.x() - p.rx(), p.y() - p.ry(), p.rx() * 2, p.ry() * 2));
        }

        // tufos de grama/terra (aleatórios, mas sempre iguais para o mesmo mapa)
        final var rnd = new Random(seed);
        final var light = new Color(255, 255, 220, 18);
        final var dark = new Color(0, 0, 0, 22);
        for (int i = 0; i < 260; i++) {
            final double x = (rnd.nextDouble() - 0.5) * width;
            final double y = (rnd.nextDouble() - 0.5) * height;
            final double s = 3 + rnd.nextDouble() * 9;
            g.setColor(rnd.nextBoolean() ? light : dark);
            g.fill(new Ellipse2D.Double(x - s, y - s * 0.6, s * 2, s * 1.2));
        }
    }

    void drawObstacles(Graphics2D g) {
        for (var o : obstacles) {
            final var g2 = (Graphics2D) g.create();
            o.draw(g2);
            g2.dispose();
        }
    }

    void drawBorders(Graphics2D g) {
        final double hw = width / 2, hh = height / 2;
        g.setColor(new Color(38, 34, 30));
        for (var b : borders) {
            g.fill(new Rectangle2D.Double(b.minX(), b.minY(), b.maxX() - b.minX(), b.maxY() - b.minY()));
        }
        g.setColor(new Color(20, 18, 16));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Rectangle2D.Double(-hw, -hh, width, height));
    }
}
