package pack.student;

import pack.engine.*;
import pack.student.behaviors.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static pack.engine.Vector2.*;


 //presa
 //PATRULHA  Obstacle Avoidance > Path Following(rota) > Wander leve
 //ALERTA    Obstacle Avoidance > Arrive(no lugar), olhando para o som
 //FUGA      Obstacle Avoidance > Evade(raptors a menos de 150 px) > Seek(rota de fuga) > Wander(zigue-zague)
public final class Prey extends Agent {
    public enum State {
        PATRULHA("patrulha", new Color(90, 160, 255)),
        ALERTA("alerta", new Color(255, 230, 80)),
        FUGA("fuga", new Color(255, 140, 40)),
        MORTA("abatida", new Color(140, 140, 140));

        public final String label;
        public final Color color;

        State(String label, Color color) {
            this.label = label;
            this.color = color;
        }
    }

    static final Color COAT = new Color(232, 220, 192);
    static final Color EXHAUSTED_COLOR = new Color(190, 110, 255);

    // velocidades (px/s)
    static final double WALK = 60;
    static final double CAUTIOUS = 95;
    static final double RUN = 190;
    static final double EXHAUSTED = 105;
    // percepção e memória
    static final double VISION = 300;
    static final double FOV = Math.toRadians(220);
    /** Abaixo disso ela percebe o predador em 360° (ouve os passos), inclusive atrás dela. */
    static final double PROXIMITY = 130;
    static final double MEMORY = 4.0;
    static final double HEARD_MEMORY = 2.5;
    static final double PANIC_HEARING = 170;
    // decisão
    static final double CALM_TIME = 3.0;
    static final double SAFE_DISTANCE = 280;
    static final double CAUTIOUS_TIME = 8.0;
    static final double ALERT_TIME = 1.1;
    static final double RESPAWN_TIME = 3.0;
    // fôlego e vida
    static final double MAX_STAMINA = 100;
    static final double DRAIN = 14;
    static final double REGEN = 9;
    static final double RECOVER_AT = 35;
    static final double MAX_HEALTH = 100;
    /** Vida recuperada por segundo enquanto patrulha calma. */
    static final double HEAL = 3;
    // rota de fuga
    static final int ESCAPE_DIRECTIONS = 16;
    static final double ESCAPE_PROBE = 260;
    static final double ESCAPE_REPLAN = 0.3;
    static final double ESCAPE_HYSTERESIS = 0.15;
    static final double ESCAPE_LEAD = 150;
    static final double EVADE_PANIC = 150;

    private record Threat(Vector2 pos, Vector2 vel, double time) { }

    private record Known(Vector2 pos, double weight) { }

    private final Random rnd;
    private State state = State.PATRULHA;
    private double stateSince = 0;
    private double now = 0;
    private double health = MAX_HEALTH;
    private double stamina = MAX_STAMINA;
    private boolean exhausted = false;
    private double hurt = 0;
    private double cautiousUntil = -1;
    private Vector2 alertOrigin = vec2();
    private Vector2 alertSpot = vec2();
    private double lastThreatSeen = -1e9;
    private int deaths = 0;

    private final Map<Agent, Threat> memory = new LinkedHashMap<>();
    private final List<Threat> heard = new ArrayList<>();
    private final List<Raptor> visibleNow = new ArrayList<>();

    // comportamentos
    private final ObstacleAvoidance avoid = new ObstacleAvoidance().lookAhead(0.5);
    private final PathFollowing follow;
    private final Wander wanderPatrol;
    private final Wander wanderFlee;
    private final Evade evade = new Evade();
    private final Seek escape = new Seek();
    private final Arrive brake = new Arrive().slowingRadius(30);   // freio: Arrive no próprio lugar

    // planejador da rota de fuga
    private Vector2 escapeDir = null;
    private double lastPlan = -1e9;
    private Vector2 fleeVector = vec2();
    private final double[] probeFree = new double[ESCAPE_DIRECTIONS];
    private final double[] probeScore = new double[ESCAPE_DIRECTIONS];
    private final boolean[] probeHidden = new boolean[ESCAPE_DIRECTIONS];

    // combinações
    private final SteeringMixer patrolMix;
    private final SteeringMixer alertMix;
    private final SteeringMixer fleeMix;

    public Prey(Path route, Vector2 start, double orientation, long seed) {
        super(settings -> settings
            .name("Presa")
            .color(COAT)
            .position(start)
            .orientation(orientation)
            .mass(0.9)
            .maxForce(650)
            .maxSpeed(RUN)
            .radius(11));
        this.rnd = new Random(seed);
        this.follow = new PathFollowing(route).lookAhead(55).predictTime(0.3);
        this.wanderPatrol = new Wander(new Random(seed + 1)).distance(50).radius(25).jitter(1.5);
        this.wanderFlee = new Wander(new Random(seed + 2)).distance(40).radius(35).jitter(4.0);
        setTurnRate(11);

        patrolMix = new SteeringMixer()
            .add("Obstacle Avoidance", avoid, Tuning.P_AVOID)
            .add("Path Following", follow, Tuning.P_PATH)
            .add("Wander", wanderPatrol, Tuning.P_WANDER_PATROL);

        alertMix = new SteeringMixer()
            .add("Obstacle Avoidance", avoid, Tuning.P_AVOID)
            .add("Arrive (para e escuta)", brake, Weight.constant(1));

        fleeMix = new SteeringMixer()
            .add("Obstacle Avoidance", avoid, Tuning.P_AVOID)
            .add("Evade (pânico, < 150 px)", evade, Tuning.P_EVADE)
            .add("Seek (rota de fuga)", escape, Tuning.P_ESCAPE)
            .add("Wander (zigue-zague)", wanderFlee, Tuning.P_WANDER_FLEE);
    }

    public boolean isAlive() { return state != State.MORTA; }
    public State getState() { return state; }
    public double getHealth() { return health; }
    public double getStamina() { return stamina; }
    public boolean isExhausted() { return exhausted; }
    public int getDeaths() { return deaths; }

    //streering(serve para calcular a direção de movimento do agente)
    @Override
    public Vector2 calculateSteering(World world) {
        now = world.getTime();
        final double dt = world.getSecs();
        hurt = Math.max(0, hurt - dt * 2.5);

        if (state == State.MORTA) {
            if (timeInState() > RESPAWN_TIME) respawn(world);
            return multiply(getVelocity(), -4);
        }
        perceive(world);
        updateStamina(dt);
        think();
        configure(world);
        return currentMix().calculate(this, world);
    }

    private SteeringMixer currentMix() {
        return switch (state) {
            case PATRULHA, MORTA -> patrolMix;
            case ALERTA -> alertMix;
            case FUGA -> fleeMix;
        };
    }

    private void perceive(World world) {
        visibleNow.clear();
        final var pos = getPosition();
        for (var r : world.getAgents(Raptor.class)) {
            final var to = subtract(r.getPosition(), pos);
            final double d = to.size();
            if (d > VISION) continue;
            final boolean inFov = d <= PROXIMITY || angleBetween(getDirection(), to) <= FOV / 2;
            if (!inFov || !world.hasLineOfSight(pos, r.getPosition())) continue;
            visibleNow.add(r);
            memory.put(r, new Threat(r.getPosition(), r.getVelocity(), now));
            lastThreatSeen = now;
        }
        memory.values().removeIf(t -> now - t.time() > MEMORY);
        heard.removeIf(t -> now - t.time() > HEARD_MEMORY);
    }

    private void updateStamina(double dt) {
        final boolean running = state == State.FUGA && getSpeed() > 130;
        stamina = Math.clamp(stamina + (running ? -DRAIN : REGEN) * dt, 0, MAX_STAMINA);
        if (state == State.PATRULHA) health = Math.min(MAX_HEALTH, health + HEAL * dt);
        if (!exhausted && stamina <= 0) exhausted = true;
        if (exhausted && stamina >= RECOVER_AT) exhausted = false;
    }

    private void think() {
        switch (state) {
            case PATRULHA, ALERTA -> {
                if (!visibleNow.isEmpty()) {
                    setState(State.FUGA);
                } else if (state == State.ALERTA && timeInState() > ALERT_TIME) {
                    cautiousUntil = now + CAUTIOUS_TIME;
                    setState(State.PATRULHA);
                }
            }
            case FUGA -> {
                final boolean calm = now - lastThreatSeen > CALM_TIME && nearestThreatDistance() > SAFE_DISTANCE;
                if (calm) {
                    cautiousUntil = now + CAUTIOUS_TIME;
                    follow.reset();
                    setState(State.PATRULHA);
                }
            }
            default -> { }
        }
    }

    private void configure(World world) {
        switch (state) {
            case PATRULHA -> {
                clearLook();
                final double v = now < cautiousUntil ? CAUTIOUS : WALK;
                follow.speed(v);
                wanderPatrol.speed(v);
            }
            case ALERTA -> {
                lookAt(alertOrigin);
                brake.target(alertSpot);
            }
            case FUGA -> {
                clearLook();
                final double v = exhausted ? EXHAUSTED : RUN;
                evade.threats(visibleNow).speed(v).panicRadius(EVADE_PANIC);
                if (now - lastPlan >= ESCAPE_REPLAN || escapeDir == null) {
                    planEscape(world);
                    lastPlan = now;
                }
                escape.target(add(getPosition(), multiply(escapeDir, ESCAPE_LEAD))).speed(v);
                wanderFlee.speed(v);
            }
            default -> { }
        }
    }

    // identifica as ameaças conhecidas (vistas ou ouvidas) e estima onde elas estão
    private List<Known> knownThreats() {
        final var list = new ArrayList<Known>();
        for (var e : memory.entrySet()) {
            final var t = e.getValue();
            final double age = now - t.time();
            if (visibleNow.contains(e.getKey())) {
                list.add(new Known(add(t.pos(), multiply(t.vel(), 0.3)), 1.0));
            } else {
                //se n ve mais ele imagina onde ele pode estar
                list.add(new Known(add(t.pos(), multiply(t.vel(), Math.min(age, 0.8))), 1.0 - age / MEMORY));
            }
        }
        for (var t : heard) {
            list.add(new Known(t.pos(), 0.5 * (1.0 - (now - t.time()) / HEARD_MEMORY)));
        }
        return list;
    }

    //escolhe a direção de fuga com base nas ameaças conhecidas, raycast e heurística
    private void planEscape(World world) {
        final var pos = getPosition();
        final var threats = knownThreats();
        fleeVector = vec2();
        for (var t : threats) {
            final var away = subtract(pos, t.pos());
            final double d = away.size();
            if (d < 1e-6) continue;
            fleeVector.add(away.divide(d).multiply(t.weight() * (0.05 + Math.max(0, 1 - d / 400))));
        }
        final var away = fleeVector.isZero() ? getMoveDirection() : normalize(fleeVector);

        int best = -1;
        double bestScore = -Double.MAX_VALUE;
        for (int i = 0; i < ESCAPE_DIRECTIONS; i++) {
            final var dir = byAngle(2 * Math.PI * i / ESCAPE_DIRECTIONS);
            probeScore[i] = scoreDirection(world, pos, dir, away, threats, i);
            if (probeScore[i] > bestScore) {
                bestScore = probeScore[i];
                best = i;
            }
        }
        final var bestDir = byAngle(2 * Math.PI * best / ESCAPE_DIRECTIONS);
        if (escapeDir == null) {
            escapeDir = bestDir;
            return;
        }
        final double current = scoreDirection(world, pos, escapeDir, away, threats, -1);
        if (bestScore > current + ESCAPE_HYSTERESIS) escapeDir = bestDir;
    }

    private double scoreDirection(World world, Vector2 pos, Vector2 dir, Vector2 away, List<Known> threats, int probe) {
        final var hit = world.raycast(pos, dir, ESCAPE_PROBE, getRadius() + 4);
        final double free = hit == null ? ESCAPE_PROBE : hit.distance();
        if (probe >= 0) probeFree[probe] = free;
        double danger = 0;
        for (var t : threats) {
            final var to = subtract(t.pos(), pos);
            final double d = to.size();
            if (d > 280 || d < 1e-6) continue;
            final double cos = dir.dot(to) / d;
            if (cos > 0.5) danger = Math.max(danger, (cos - 0.5) / 0.5 * (1 - d / 280) * t.weight());
        }
        final double wall = free < 60 ? 1.5 : 0;
        // escondido: nenhum predador conhecido enxergaria o ponto de chegada
        final var arrival = add(pos, multiply(dir, Math.min(free, 160)));
        boolean hidden = !threats.isEmpty();
        for (var t : threats) {
            if (world.hasLineOfSight(t.pos(), arrival)) {
                hidden = false;
                break;
            }
        }
        if (probe >= 0) probeHidden[probe] = hidden;
        return 1.0 * dir.dot(away) + 1.5 * free / ESCAPE_PROBE - wall + (hidden ? 0.6 : 0) - 2.0 * danger;
    }

    private double nearestThreatDistance() {
        double best = Double.MAX_VALUE;
        final var pos = getPosition();
        for (var t : knownThreats()) best = Math.min(best, Vector2.distance(pos, t.pos()));
        return best;
    }

    private void setState(State s) {
        if (state == s) return;
        if (s == State.FUGA) {
            escapeDir = null;      // replaneja a rota de fuga na hora
            lastPlan = -1e9;
        }
        state = s;
        stateSince = now;
    }

    private double timeInState() { return now - stateSince; }

    //eventos externos

    //a presa ouve um raptor correndo atrás dela (ou outro barulho alto)
    void hearCall(Vector2 origin, double time) {
        if (state == State.MORTA) return;
        now = time;
        heard.add(new Threat(origin.clone(), vec2(), time));
        final double d = Vector2.distance(getPosition(), origin);
        if (d < PANIC_HEARING) {
            setState(State.FUGA);
        } else if (state == State.PATRULHA) {
            alertOrigin = origin.clone();
            alertSpot = getPosition();
            setState(State.ALERTA);
        }
    }

    //mordida de um raptor
    void bite(Raptor by, double damage, double time) {
        if (state == State.MORTA) return;
        now = time;
        health -= damage;
        hurt = 1;
        stamina = Math.min(MAX_STAMINA, stamina + 12);   // adrenalina
        memory.put(by, new Threat(by.getPosition(), by.getVelocity(), time));
        lastThreatSeen = time;
        if (health <= 0) {
            if (Tuning.preyImmortal) {
                health = 1;
            } else {
                health = 0;
                deaths++;
                setState(State.MORTA);
                return;
            }
        }
        setState(State.FUGA);
    }

    //revive em outra posição
    private void respawn(World world) {
        final var raptors = world.getAgents(Raptor.class);
        Vector2 best = getPosition();
        double bestScore = -1;
        for (int i = 0; i < 40; i++) {
            final var p = world.getMap().randomFreePoint(rnd, 40);
            double score = Double.MAX_VALUE;
            for (var r : raptors) score = Math.min(score, Vector2.distance(p, r.getPosition()));
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        teleport(best);
        health = MAX_HEALTH;
        stamina = MAX_STAMINA;
        exhausted = false;
        memory.clear();
        heard.clear();
        visibleNow.clear();
        follow.reset();
        escapeDir = null;
        cautiousUntil = now + CAUTIOUS_TIME;
        setState(State.PATRULHA);
    }

    //desenho hud

    public Color getStateColor() {
        return state == State.FUGA && exhausted ? EXHAUSTED_COLOR : state.color;
    }

    public String statusText() {
        if (state == State.MORTA) return "Presa: abatida (renasce em %.1f s)".formatted(Math.max(0, RESPAWN_TIME - timeInState()));
        return "Presa: %s%s · vida %.0f · fôlego %.0f%%".formatted(
            state.label, exhausted ? " (exausta)" : "", health, stamina);
    }

    @Override
    protected void drawBody(Graphics2D g) {
        Sprites.prey(g, getColor(), getRadius(), hurt, state == State.MORTA);
    }

    @Override
    protected void drawDebug(Graphics2D g) {
        if (state == State.MORTA) return;
        final var pos = getPosition();
        if (Debug.vision) {
            Draw.cone(g, pos, getDirection(), FOV / 2, VISION,
                visibleNow.isEmpty() ? new Color(160, 200, 255, 7) : new Color(255, 150, 60, 14),
                visibleNow.isEmpty() ? new Color(160, 200, 255, 26) : new Color(255, 150, 60, 60));
            for (var r : visibleNow) Draw.line(g, pos, r.getPosition(), new Color(255, 50, 30, 200), 2.5f);
        }
        for (var e : memory.entrySet()) {
            if (!visibleNow.contains(e.getKey())) Draw.cross(g, e.getValue().pos(), 6, new Color(255, 60, 60, 160), 2f);
        }
        for (var t : heard) Draw.dashedCircle(g, t.pos(), 12, new Color(255, 160, 60, 150), 1.5f);
        if (state == State.FUGA && escapeDir != null) drawEscapeProbes(g, pos);
        currentMix().drawDebug(g, this);

        // barras de vida e fôlego
        final double w = 34, x = pos.x - w / 2, y = pos.y + getRadius() * 2 + 6;
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect((int) x - 1, (int) y - 1, (int) w + 2, 9);
        g.setColor(new Color(230, 60, 50));
        g.fillRect((int) x, (int) y, (int) (w * health / MAX_HEALTH), 3);
        g.setColor(exhausted ? EXHAUSTED_COLOR : new Color(255, 214, 70));
        g.fillRect((int) x, (int) y + 4, (int) (w * stamina / MAX_STAMINA), 3);
    }

    /** Bolinhas das 16 direções testadas: verde = boa rota, vermelho = ruim; seta = escolhida. */
    private void drawEscapeProbes(Graphics2D g, Vector2 pos) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (double v : probeScore) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        for (int i = 0; i < ESCAPE_DIRECTIONS; i++) {
            final var dir = byAngle(2 * Math.PI * i / ESCAPE_DIRECTIONS);
            final double t = max > min ? (probeScore[i] - min) / (max - min) : 0.5;
            final var c = new Color((int) (255 * (1 - t)), (int) (80 + 175 * t), 70, 200);
            final var p = add(pos, multiply(dir, 26 + probeFree[i] * 0.22));
            Draw.fillCircle(g, p, probeHidden[i] ? 4.5 : 3, c);
        }
        Draw.arrowTo(g, pos, add(pos, multiply(escapeDir, 80)), new Color(60, 230, 90, 230), 3f);
        if (!fleeVector.isZero()) {
            Draw.arrowTo(g, pos, add(pos, multiply(normalize(fleeVector), 45)), new Color(80, 230, 255, 220), 2f);
        }
    }

    @Override
    protected Color getRingColor() { return getStateColor(); }

    @Override
    protected String getLabel() {
        return "Presa\n" + state.label + (exhausted && state == State.FUGA ? " (exausta)" : "");
    }

    @Override
    public String getStateName() { return state.label + (exhausted ? ", exausta" : ""); }

    @Override
    public List<ForceReport> getForceReport() { return currentMix().getReport(); }
}
