package pack.student;

import pack.engine.*;

import java.awt.*;
import java.util.*;
import java.util.List;

import static pack.engine.Vector2.*;

//coordena a matilha de raptors: comunicação, cerco, botes e desistência
public final class Pack {
    public enum Mood {
        CALMA("calma (patrulha)", new Color(115, 230, 115)),
        ALERTA("alerta: chamado em cadeia", new Color(255, 190, 70)),
        CACADA("CAÇADA: cerco e botes", new Color(255, 85, 64));

        public final String label;
        public final Color color;

        Mood(String label, Color color) {
            this.label = label;
            this.color = color;
        }
    }

    // Comunicação
    static final double CALL_SPEED = 520;
    static final double CALL_RADIUS = 270;
    static final double ROAR_RADIUS = 460;
    static final double RELAY_DELAY = 0.25;
    static final int MAX_HOPS = 6;
    static final double ALPHA_PROCESSING = 0.5;
    static final double MESSENGER_TIMEOUT = 5.0;
    static final double CHAIN_COOLDOWN = 2.0;
    // Cerco e ataque
    static final double STALK_RING = 170;
    static final double ATTACK_RING = 80;
    static final double TARGET_LEAD = 0.35;
    static final double SLOT_TOLERANCE = 30;
    static final double STRIKE_RANGE = 115;
    static final double ENCIRCLE_RATIO = 0.6;
    static final double ENCIRCLE_TIMEOUT = 3.5;
    static final int MAX_ATTACKERS = 2;
    static final double STRIKE_INTERVAL = 0.55;
    static final double TOKEN_MAX_TIME = 2.5;
    // Desistência
    static final double LOSE_TIME = 6.0;
    // Quanto tempo a matilha espera sem ver a presa antes de desistir da caçada
    static final double GIVE_UP_TIME = 9.0;
    static final double ALERT_GIVE_UP = 8.0;

    // Onda sonora de um chamado (ou do rugido do Alfa)
    static final class CallRing {
        final Vector2 origin;
        final double start;
        final double maxRadius;
        final Raptor emitter;
        final int chain;
        final int hop;
        final boolean roar;
        final Set<Agent> reached = new HashSet<>();

        CallRing(Vector2 origin, double start, double maxRadius, Raptor emitter, int chain, int hop, boolean roar) {
            this.origin = origin;
            this.start = start;
            this.maxRadius = maxRadius;
            this.emitter = emitter;
            this.chain = chain;
            this.hop = hop;
            this.roar = roar;
        }

        double radius(double now) { return (now - start) * CALL_SPEED; }
    }

    private final GameMap map;
    private final Vector2 territoryCenter;
    private final double territoryRadius;
    private final List<Raptor> members = new ArrayList<>();
    private Raptor alpha;
    private Prey prey;

    private Mood mood = Mood.CALMA;
    private double now = 0;
    private Vector2 lastKnownPos = vec2();
    private Vector2 lastKnownVel = vec2();
    private double lastSeen = -1e9;
    private double alertStart = 0;
    private double orderTime = 0;
    private double orderAt = Double.NaN;
    private double lastChainTime = -1e9;
    private double lastGrant = -1e9;
    private double lastBite = -1e9;
    private double slotBase = 0;
    private int chainCounter = 0;
    private boolean encircled = false;

    private final Map<Raptor, Double> slotOffset = new HashMap<>();
    private final Map<Raptor, Double> inPosition = new LinkedHashMap<>();
    private final Map<Raptor, Double> tokens = new LinkedHashMap<>();
    private final List<CallRing> rings = new ArrayList<>();
    private final Deque<String> events = new ArrayDeque<>();

    // estatísticas
    private int hunts, bites, kills, escapes;

    public Pack(GameMap map, Vector2 territoryCenter, double territoryRadius) {
        this.map = map;
        this.territoryCenter = territoryCenter.clone();
        this.territoryRadius = territoryRadius;
    }

    public void addMember(Raptor r) {
        members.add(r);
        if (r.isAlpha()) alpha = r;
    }

    public void setPrey(Prey prey) { this.prey = prey; }

    public Prey getPrey() { return prey; }
    public Raptor getAlpha() { return alpha; }
    public List<Raptor> getMembers() { return Collections.unmodifiableList(members); }
    public Mood getMood() { return mood; }
    public boolean isEncircled() { return encircled; }
    public Vector2 getTerritoryCenter() { return territoryCenter.clone(); }
    public double getTerritoryRadius() { return territoryRadius; }
    public int getHunts() { return hunts; }
    public int getBites() { return bites; }
    public int getKills() { return kills; }
    public int getEscapes() { return escapes; }

    //update uma vez por quadro, antes de atualizar os raptors
    public void update(double dt, double time) {
        now = time;
        updateRings();

        if (!Double.isNaN(orderAt) && now >= orderAt) {
            issueOrder("o Alfa processou o chamado");
        }

        switch (mood) {
            case ALERTA -> {
                if (now - lastSeen > ALERT_GIVE_UP) {
                    endHunt("alerta cancelado: ninguém viu mais a presa");
                } else if (Double.isNaN(orderAt) && now - alertStart > MESSENGER_TIMEOUT) {
                    log("o chamado não chegou ao Alfa: mensageiro avisa");
                    orderAt = now;
                }
            }
            case CACADA -> {
                if (prey == null || !prey.isAlive()) {
                    endHunt("caçada encerrada");
                } else if (now - lastSeen > LOSE_TIME) {
                    escapes++;
                    endHunt("a presa sumiu de vista: fuga bem-sucedida");
                } else if (now - Math.max(orderTime, lastBite) > GIVE_UP_TIME) {
                    escapes++;
                    endHunt("%.0f s sem conseguir morder: a matilha cansou e desistiu".formatted(GIVE_UP_TIME));
                } else {
                    updateAttack();
                }
            }
            default -> { }
        }
    }

    private void updateRings() {
        for (var it = rings.iterator(); it.hasNext(); ) {
            final var ring = it.next();
            final double r = ring.radius(now);
            final double reach = Math.min(r, ring.maxRadius);
            if (!ring.roar) {
                for (var m : members) {
                    if (m == ring.emitter || ring.reached.contains(m)) continue;
                    if (distance(ring.origin, m.getPosition()) <= reach) {
                        ring.reached.add(m);
                        m.onCallHeard(ring.chain, ring.hop, now);
                    }
                }
            }
            if (prey != null && prey.isAlive() && !ring.reached.contains(prey)
                && distance(ring.origin, prey.getPosition()) <= reach) {
                ring.reached.add(prey);
                prey.hearCall(ring.origin, now);
            }
            if (r > ring.maxRadius + 60) it.remove();
        }
    }

    private void updateAttack() {
        //1 cerco: só ataca quando a maioria dos raptors está em posição 
        if (!encircled) {
            final double ratio = (double) inPosition.size() / Math.max(1, members.size());
            if (ratio >= ENCIRCLE_RATIO || now - orderTime >= ENCIRCLE_TIMEOUT) {
                encircled = true;
                log("cerco formado (%d/%d em posição): botes alternados".formatted(inPosition.size(), members.size()));
            } else {
                return;
            }
        }
        tokens.entrySet().removeIf(e -> now - e.getValue() > TOKEN_MAX_TIME);

        // 2 botes alternados, só com a presa à vista
        if (tokens.size() >= MAX_ATTACKERS || now - lastGrant < STRIKE_INTERVAL) return;
        if (now - lastSeen > 1.5) return;
        Raptor best = null;
        double bestScore = -Double.MAX_VALUE;
        for (var e : inPosition.entrySet()) {
            final var r = e.getKey();
            if (tokens.containsKey(r)) continue;
            final double score = (now - e.getValue()) + flankFactor(r) * 1.5;
            if (score > bestScore) {
                bestScore = score;
                best = r;
            }
        }
        if (best != null) {
            tokens.put(best, now);
            lastGrant = now;
        }
    }

    //eventos dos raptors

    //algum raptor viu a presa nesse quadro
    public void reportSighting() {
        if (prey == null) return;
        lastKnownPos = prey.getPosition();
        lastKnownVel = prey.getVelocity();
        lastSeen = now;
    }

    //patrulha avistou a presa: se não for o Alfa, repassa o chamado; se for o Alfa, dá a ordem de cercar
    public void onSpotted(Raptor r) {
        reportSighting();
        if (mood == Mood.CACADA) {
            r.onAttackOrder();
            return;
        }
        if (r.isAlpha()) {
            if (mood == Mood.CALMA) alertStart = now;
            issueOrder(r.getName() + " (Alfa) avistou a presa");
            return;
        }
        if (mood == Mood.CALMA) {
            mood = Mood.ALERTA;
            alertStart = now;
            assignSlots();
        }
        r.toStalk();
        if (now - lastChainTime >= CHAIN_COOLDOWN) {
            chainCounter++;
            lastChainTime = now;
            log("%s avistou a presa e chamou (#%d)".formatted(r.getName(), chainCounter));
            r.markRelayed(chainCounter);
            emitCall(r, chainCounter, 0);
        }
    }

    //raptor grita e repassa o chamado para a matilha
    public void emitCall(Raptor r, int chain, int hop) {
        if (mood == Mood.CALMA) {
            mood = Mood.ALERTA;
            alertStart = now;
            assignSlots();
        }
        rings.add(new CallRing(r.getPosition(), now, CALL_RADIUS, r, chain, hop, false));
        r.vocalize();
        if (hop > 0) log("%s repassou o chamado #%d (salto %d)".formatted(r.getName(), chain, hop));
    }

    // O chamado chegou ao Alfa: ele processa e depois dá a ordem
    public void alphaHeard(int chain, int hop) {
        if (mood == Mood.CACADA || !Double.isNaN(orderAt)) return;
        orderAt = now + ALPHA_PROCESSING;
        log("Alfa %s ouviu o chamado #%d (salto %d)".formatted(alpha.getName(), chain, hop));
    }

    public void reportInPosition(Raptor r, boolean in) {
        if (in) inPosition.putIfAbsent(r, now);
        else inPosition.remove(r);
    }

    public boolean hasToken(Raptor r) { return tokens.containsKey(r); }

    public void releaseToken(Raptor r) { tokens.remove(r); }

    public void onBite(Raptor r, double damage) {
        bites++;
        lastBite = now;
        log("%s mordeu a presa (-%.0f)".formatted(r.getName(), damage));
        if (prey != null && !prey.isAlive()) {
            kills++;
            endHunt("presa abatida por " + r.getName() + "!");
        }
    }

    private void issueOrder(String reason) {
        mood = Mood.CACADA;
        orderTime = now;
        orderAt = Double.NaN;
        encircled = false;
        inPosition.clear();
        tokens.clear();
        assignSlots();
        hunts++;
        if (alpha != null) {
            rings.add(new CallRing(alpha.getPosition(), now, ROAR_RADIUS, alpha, 0, 0, true));
            alpha.vocalize();
        }
        log("ORDEM DO ALFA: cercar! (" + reason + ")");
        for (var m : members) m.onAttackOrder();
    }

    private void endHunt(String reason) {
        mood = Mood.CALMA;
        orderAt = Double.NaN;
        encircled = false;
        tokens.clear();
        inPosition.clear();
        log(reason);
        for (var m : members) m.onHuntEnded();
    }

   //cerco

    //onde a matilha acha q a presa esta, prevendo
    public Vector2 getFocus() {
        if (prey != null && prey.isAlive() && now - lastSeen < 1.0) {
            return add(prey.getPosition(), multiply(prey.getVelocity(), TARGET_LEAD));
        }
        return lastKnownPos.clone();
    }

    //distribui os raptors em slots ao redor da presa, com o Alfa na frente
    private void assignSlots() {
        slotOffset.clear();
        if (members.isEmpty() || alpha == null) return;
        final var focus = getFocus();
        slotBase = angleTo(focus, alpha.getPosition());
        final int n = members.size();
        final var pending = new ArrayList<>(members);
        slotOffset.put(alpha, 0.0);
        pending.remove(alpha);
        // cada slot fica com o raptor mais próximo dele
        for (int i = 1; i < n && !pending.isEmpty(); i++) {
            final double offset = wrapAngle(2 * Math.PI * i / n);
            final var slotPos = add(focus, byAngleSize(slotBase + offset, STALK_RING));
            Raptor best = null;
            double bestD = Double.MAX_VALUE;
            for (var m : pending) {
                final double d = distanceSqr(m.getPosition(), slotPos);
                if (d < bestD) {
                    bestD = d;
                    best = m;
                }
            }
            slotOffset.put(best, offset);
            pending.remove(best);
        }
    }

    //o slot do raptor gira junto com o alpha aconpanhando a presa
    public Vector2 getSlot(Raptor r) {
        final var focus = getFocus();
        if (alpha != null && distance(alpha.getPosition(), focus) > 1) {
            slotBase = angleTo(focus, alpha.getPosition());
        }
        double ring = mood == Mood.CACADA ? ATTACK_RING : STALK_RING;
        if (r == alpha) ring *= 0.85;
        final double offset = slotOffset.getOrDefault(r, 0.0);
        final var p = add(focus, byAngleSize(slotBase + offset, ring));
        return map.freePoint(p, r.getRadius() + 4);
    }

    // 0 = slot da frente (do Alfa), 1 = slot oposto (retaguarda)
    private double flankFactor(Raptor r) {
        return Math.abs(wrapAngle(slotOffset.getOrDefault(r, 0.0))) / Math.PI;
    }

    private static double angleTo(Vector2 from, Vector2 to) {
        return subtract(to, from).getAngle();
    }

    private static double wrapAngle(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }

    private void log(String text) {
        final var line = "%5.1fs  %s".formatted(now, text);
        System.out.println("[matilha] " + line);
        events.addFirst(line);
        while (events.size() > 4) events.removeLast();
    }

   //desenho e hud

    public void drawTerritory(Graphics2D g) {
        Draw.dashedCircle(g, territoryCenter, territoryRadius, new Color(255, 214, 140, 90), 1.5f);
        Draw.label(g, "território da matilha", territoryCenter.x, territoryCenter.y - territoryRadius + 16,
            new Color(255, 225, 170, 180));
    }

    public void drawOverlay(Graphics2D g) {
        // ondas sonoras
        for (var ring : rings) {
            final double r = Math.min(ring.radius(now), ring.maxRadius);
            final float fade = (float) Math.clamp(1.0 - ring.radius(now) / (ring.maxRadius + 60), 0, 1);
            final Color c = ring.roar ? new Color(255, 60, 40)
                : ring.hop == 0 ? new Color(255, 230, 90)
                : ring.hop == 1 ? new Color(255, 160, 60)
                : new Color(255, 110, 170);
            Draw.circle(g, ring.origin, r, Draw.alpha(c, 0.75f * fade), ring.roar ? 3f : 2f);
            if (!ring.roar) {
                Draw.circle(g, ring.origin, ring.maxRadius, Draw.alpha(c, 0.12f * fade), 1f);
            }
        }
        if (!Debug.enabled || mood == Mood.CALMA) return;

        // foco do cerco e slots
        final var focus = getFocus();
        Draw.cross(g, focus, 7, new Color(255, 255, 255, 170), 1.6f);
        Draw.dashedCircle(g, focus, mood == Mood.CACADA ? ATTACK_RING : STALK_RING, new Color(255, 120, 90, 70), 1f);
        for (var m : members) {
            final var slot = getSlot(m);
            final var c = m.getState().color;
            Draw.dashedLine(g, m.getPosition(), slot, Draw.alpha(c, 0.45f), 1f);
            Draw.circle(g, slot, 6, Draw.alpha(c, 0.9f), 1.6f);
            if (tokens.containsKey(m)) {
                Draw.circle(g, m.getPosition(), m.getRadius() * 2.6, new Color(255, 40, 40, 200), 2.5f);
            }
        }
    }

    public List<HudLine> statusLines() {
        final var lines = new ArrayList<HudLine>();
        lines.add(HudLine.of("Matilha: " + mood.label, mood.color));
        if (alpha != null) {
            lines.add(HudLine.of("Alfa: %s · chamados: %d · em posição: %d/%d · botes agora: %d"
                .formatted(alpha.getName(), chainCounter, inPosition.size(), members.size(), tokens.size()),
                new Color(255, 209, 51)));
        }
        if (prey != null) {
            lines.add(HudLine.of(prey.statusText(), prey.getStateColor()));
        }
        lines.add(HudLine.of("Caçadas %d · mordidas %d · abates %d · fugas %d"
            .formatted(hunts, bites, kills, escapes)));
        lines.add(HudLine.of("C · Mistura: " + pack.student.behaviors.SteeringMixer.getMode().label
            + " · G · presa imortal: " + (Tuning.preyImmortal ? "SIM" : "não")));
        for (var e : events) lines.add(HudLine.of(e));
        return lines;
    }

    Vector2 getLastKnownVelocity() { return lastKnownVel.clone(); }
}
