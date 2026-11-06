package pack.student;

import pack.engine.*;
import pack.student.behaviors.*;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static pack.engine.Vector2.*;

// PATRULHA  (combinação 3)  Obstacle Avoidance > Separation > Seek(território) > Wander
// RONDA     (combinação 1)  Obstacle Avoidance > Separation > Arrive(slot no anel largo)
// CERCO     (combinação 1)  Obstacle Avoidance > Separation > Arrive(slot no anel fechado)
// BOTE      (combinação 1)  Obstacle Avoidance > Arrive(agacha 0,18 s) | Pursuit(presa)
// RECUO     (combinação 1)  Obstacle Avoidance > Separation > Flee(presa, 0,4 s) | Arrive(slot)
 

public final class Raptor extends Agent {
    public enum State {
        PATRULHA("patrulha", new Color(115, 230, 115)),
        RONDA("ronda", new Color(255, 190, 70)),
        CERCO("cerco", new Color(255, 85, 64)),
        BOTE("BOTE", new Color(255, 30, 30)),
        RECUO("recuo", new Color(255, 150, 90));

        public final String label;
        public final Color color;

        State(String label, Color color) {
            this.label = label;
            this.color = color;
        }
    }

    static final Color SKIN = new Color(178, 126, 68);
    static final Color ALPHA_SKIN = new Color(124, 64, 42);

    // velocidades (px/s)
    static final double WALK = 70;
    static final double RUN = 165;
    static final double LUNGE = 265;
    //alpha tem um pouco mais de velocidade
    static final double ALPHA_SPEED = 1.1;
    // percepção
    static final double VISION = 250;
    static final double ALPHA_VISION = 1.25;
    static final double FOV = Math.toRadians(120);
    static final double PROXIMITY = 45;
    // bote
    static final double WINDUP = 0.18;
    static final double LUNGE_MAX = 0.85;
    static final double RECOVER_TIME = 0.9;
    static final double RECOIL_TIME = 0.4;
    static final double BITE_DAMAGE = 20;
    static final double ALPHA_DAMAGE = 1.3;

    private final Pack pack;
    private final boolean alpha;
    private final double speedMult;
    private final double patrolMult;

    private State state = State.PATRULHA;
    private double stateSince = 0;
    private double now = 0;
    private double flash = 0;

    // percepção
    private boolean seesPrey = false;
    private boolean losTested = false;
    private boolean losClear = false;
    private Vector2 losTarget = vec2();

    // chamados
    private final Set<Integer> relayed = new HashSet<>();
    private double relayAt = Double.NaN;
    private int relayChain;
    private int relayHop;

    private Vector2 windupSpot = vec2();

    // comportamentos
    private final ObstacleAvoidance avoid = new ObstacleAvoidance();
    private final Separation separation = new Separation(Raptor.class, 55);
    private final Seek home = new Seek();
    private final Wander wander;
    private final Arrive toSlot = new Arrive().slowingRadius(90);
    private final Arrive brake = new Arrive().slowingRadius(30);   // freio: Arrive no próprio lugar
    private final Pursuit pursuit = new Pursuit().maxPrediction(0.6);
    private final Flee recoil = new Flee();

    // combinações
    private final SteeringMixer patrolMix;
    private final SteeringMixer huntMix;
    private final SteeringMixer lungeMix;
    private final SteeringMixer recoverMix;

    public Raptor(String name, Vector2 position, boolean alpha, Pack pack, long seed) {
        super(settings -> settings
            .name(name)
            .color(alpha ? ALPHA_SKIN : SKIN)
            .position(position)
            .randomOrientation()
            .mass(1)
            .maxForce(alpha ? 640 : 600)
            .maxSpeed(LUNGE * (alpha ? ALPHA_SPEED : 1))
            .radius(alpha ? 17 : 13));
        this.pack = pack;
        this.alpha = alpha;
        this.speedMult = alpha ? ALPHA_SPEED : 1.0;
        final var rnd = new Random(seed);
        this.patrolMult = 0.9 + rnd.nextDouble() * 0.2;   // cada um anda num ritmo
        this.wander = new Wander(rnd).distance(60).radius(30).jitter(1.6);
        setTurnRate(9);

        patrolMix = new SteeringMixer()
            .add("Obstacle Avoidance", avoid, Tuning.R_PATROL_AVOID)
            .add("Separation", separation, Tuning.R_PATROL_SEPARATION)
            .add("Seek (território)", home, Tuning.R_PATROL_TERRITORY, this::territoryPull)
            .add("Wander", wander, Tuning.R_PATROL_WANDER);

        huntMix = new SteeringMixer()
            .add("Obstacle Avoidance", avoid, Tuning.R_HUNT_AVOID)
            .add("Separation", separation, Tuning.R_HUNT_SEPARATION)
            .add("Arrive (slot do cerco)", toSlot, Tuning.R_HUNT_ARRIVE);

        lungeMix = new SteeringMixer()
            .add("Obstacle Avoidance", avoid, Tuning.R_HUNT_AVOID)
            .add("Arrive (agacha)", brake, Weight.constant(1), () -> inWindup() ? 1 : 0)
            .add("Pursuit (bote)", pursuit, Tuning.R_HUNT_PURSUIT, () -> inWindup() ? 0 : 1);

        recoverMix = new SteeringMixer()
            .add("Obstacle Avoidance", avoid, Tuning.R_HUNT_AVOID)
            .add("Separation", separation, Tuning.R_HUNT_SEPARATION)
            .add("Flee (recuo)", recoil, Tuning.R_HUNT_RECOIL, () -> timeInState() < RECOIL_TIME ? 1 : 0)
            .add("Arrive (slot do cerco)", toSlot, Tuning.R_HUNT_ARRIVE, () -> timeInState() < RECOIL_TIME ? 0 : 1);
    }

    public boolean isAlpha() { return alpha; }
    public State getState() { return state; }


    // Steering
    @Override
    public Vector2 calculateSteering(World world) {
        now = world.getTime();
        flash = Math.max(0, flash - world.getSecs() * 1.8);
        perceive(world);
        think();
        configure();
        return currentMix().calculate(this, world);
    }

    private SteeringMixer currentMix() {
        return switch (state) {
            case PATRULHA -> patrolMix;
            case RONDA, CERCO -> huntMix;
            case BOTE -> lungeMix;
            case RECUO -> recoverMix;
        };
    }

    //visao e percepção
    private void perceive(World world) {
        seesPrey = false;
        losTested = false;
        final var prey = pack.getPrey();
        if (prey == null || !prey.isAlive()) return;
        final var to = subtract(prey.getPosition(), getPosition());
        final double d = to.size();
        if (d > VISION * (alpha ? ALPHA_VISION : 1)) return;
        final boolean inFov = d <= PROXIMITY || angleBetween(getDirection(), to) <= FOV / 2;
        losTested = true;
        losTarget = prey.getPosition();
        losClear = world.hasLineOfSight(getPosition(), losTarget);
        seesPrey = inFov && losClear;
        if (seesPrey) pack.reportSighting();
    }

    //maquina de
    private void think() {
        if (!Double.isNaN(relayAt) && now >= relayAt) {
            relayAt = Double.NaN;
            if (pack.getMood() != Pack.Mood.CACADA) pack.emitCall(this, relayChain, relayHop);
        }
        final var prey = pack.getPrey();
        switch (state) {
            case PATRULHA -> {
                if (seesPrey) pack.onSpotted(this);
            }
            case RONDA -> {
                if (pack.getMood() == Pack.Mood.CALMA) setState(State.PATRULHA);
                else if (pack.getMood() == Pack.Mood.CACADA) setState(State.CERCO);
            }
            case CERCO -> {
                if (pack.getMood() != Pack.Mood.CACADA) {
                    setState(State.PATRULHA);
                    return;
                }
                final double dSlot = Vector2.distance(getPosition(), pack.getSlot(this));
                final double dPrey = Agent.distance(this, prey);
                final boolean inPosition = dSlot <= Pack.SLOT_TOLERANCE
                    || (pack.isEncircled() && dPrey <= Pack.STRIKE_RANGE);
                pack.reportInPosition(this, inPosition);
                if (pack.hasToken(this)) setState(State.BOTE);
            }
            case BOTE -> {
                if (pack.getMood() != Pack.Mood.CACADA) {
                    setState(State.PATRULHA);
                    return;
                }
                final double reach = getRadius() + prey.getRadius() + 8;
                if (!inWindup() && Agent.distance(this, prey) <= reach) {
                    final double damage = BITE_DAMAGE * (alpha ? ALPHA_DAMAGE : 1);
                    flash = 1;
                    prey.bite(this, damage, now);
                    pack.onBite(this, damage);
                    if (state == State.BOTE) setState(State.RECUO);
                } else if (timeInState() > WINDUP + LUNGE_MAX) {
                    setState(State.RECUO);
                }
            }
            case RECUO -> {
                if (pack.getMood() != Pack.Mood.CACADA) {
                    setState(State.PATRULHA);
                } else if (timeInState() > RECOVER_TIME) {
                    pack.releaseToken(this);
                    setState(State.CERCO);
                }
            }
        }
    }

    //ajusta alvos e comportamento do estado atual
    private void configure() {
        final var prey = pack.getPrey();
        switch (state) {
            case PATRULHA -> {
                clearLook();
                final double v = WALK * speedMult * patrolMult;
                wander.speed(v);
                home.target(pack.getTerritoryCenter()).speed(v);
                separation.radius(55);
            }
            case RONDA, CERCO -> {
                final var slot = pack.getSlot(this);
                toSlot.target(slot).speed(RUN * speedMult);
                if (Vector2.distance(getPosition(), slot) < 110) lookAt(pack.getFocus());
                else clearLook();
                separation.radius(state == State.RONDA ? 60 : 42);
            }
            case BOTE -> {
                lookAt(prey.getPosition());
                brake.target(windupSpot);
                pursuit.target(prey).speed(LUNGE * speedMult);
            }
            case RECUO -> {
                lookAt(prey.getPosition());
                recoil.threat(prey.getPosition()).speed(RUN * speedMult).panicRadius(0);
                toSlot.target(pack.getSlot(this)).speed(RUN * speedMult);
                separation.radius(42);
            }
        }
    }

    private void setState(State s) {
        if (state == s) return;
        final var old = state;
        if (old == State.CERCO || old == State.BOTE) pack.reportInPosition(this, false);
        if ((old == State.BOTE || old == State.RECUO) && s != State.RECUO) pack.releaseToken(this);
        state = s;
        stateSince = now;
        switch (s) {
            case PATRULHA -> {
                clearLook();
                relayAt = Double.NaN;
                pack.releaseToken(this);
                pack.reportInPosition(this, false);
            }
            case BOTE -> {
                windupSpot = getPosition();
                vocalize();
            }
            default -> { }
        }
    }

    private double timeInState() { return now - stateSince; }

    private boolean inWindup() { return state == State.BOTE && timeInState() < WINDUP; }

    // 0 dentro de 70% do território, sobe até 1 na borda
    private double territoryPull() {
        final double d = Vector2.distance(getPosition(), pack.getTerritoryCenter());
        final double r = pack.getTerritoryRadius();
        return Math.clamp((d - 0.7 * r) / (0.3 * r), 0, 1);
    }

    // Chamados pela matilha
    void onCallHeard(int chain, int hop, double time) {
        now = time;
        if (alpha) {
            pack.alphaHeard(chain, hop);
            if (state == State.PATRULHA) setState(State.RONDA);
            return;
        }
        if (pack.getMood() == Pack.Mood.CACADA) return;
        if (!relayed.add(chain)) return;           // repassa cada cadeia uma única vez
        if (state == State.PATRULHA) setState(State.RONDA);
        if (hop + 1 <= Pack.MAX_HOPS) {
            relayAt = time + Pack.RELAY_DELAY;
            relayChain = chain;
            relayHop = hop + 1;
        }
    }

    void toStalk() {
        if (state == State.PATRULHA) setState(State.RONDA);
    }

    void markRelayed(int chain) { relayed.add(chain); }

    void onAttackOrder() {
        if (state != State.BOTE && state != State.RECUO) setState(State.CERCO);
    }

    void onHuntEnded() {
        setState(State.PATRULHA);
    }

    void vocalize() { flash = Math.max(flash, 0.7); }

    //desenho hud

    @Override
    protected void drawBody(Graphics2D g) {
        Sprites.raptor(g, getColor(), getRadius(), alpha, flash, state == State.BOTE && !inWindup());
    }

    @Override
    protected void drawDebug(Graphics2D g) {
        final var pos = getPosition();
        if (Debug.vision) {
            final double range = VISION * (alpha ? ALPHA_VISION : 1);
            Draw.cone(g, pos, getDirection(), FOV / 2, range,
                seesPrey ? new Color(255, 255, 150, 18) : new Color(255, 255, 255, 8),
                seesPrey ? new Color(255, 255, 150, 80) : new Color(255, 255, 255, 22));
            if (losTested) {
                if (losClear) {
                    Draw.line(g, pos, losTarget, new Color(60, 255, 90, seesPrey ? 220 : 80), seesPrey ? 1.8f : 1f);
                } else {
                    Draw.line(g, pos, losTarget, new Color(255, 40, 30, 150), 1f);
                }
            }
        }
        currentMix().drawDebug(g, this);
    }

    @Override
    protected Color getRingColor() { return state.color; }

    @Override
    protected String getLabel() {
        return (alpha ? "[ALFA] " : "") + getName() + "\n" + state.label;
    }

    @Override
    public String getStateName() { return state.label; }

    @Override
    public List<ForceReport> getForceReport() { return currentMix().getReport(); }
}
