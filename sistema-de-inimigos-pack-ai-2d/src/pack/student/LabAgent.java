package pack.student;

import pack.engine.*;
import pack.student.behaviors.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;


//agente de teste
public final class LabAgent extends Agent {
    public enum Mode {
        SEEK("1", "Seek", "persegue o cursor do mouse", Seek.COLOR),
        FLEE("2", "Flee", "foge do cursor dentro do raio de pânico", Flee.COLOR),
        ARRIVE("3", "Arrive", "vai até o último clique e para suavemente", Arrive.COLOR),
        WANDER("4", "Wander", "passeia ao acaso, com curvas suaves", Wander.COLOR),
        PATH("5", "Path Following", "segue o caminho tracejado (tem um pilar no meio dele)", PathFollowing.COLOR);

        final String key;
        final String title;
        final String description;
        final Color color;

        Mode(String key, String title, String description, Color color) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.color = color;
        }
    }

    private Mode mode = Mode.SEEK;
    private boolean avoidOn = true;
    private boolean wanderOn = false;
    private boolean brakeNeeded = false;

    private final ObstacleAvoidance avoid = new ObstacleAvoidance();
    private final Seek seek = new Seek();
    private final Flee flee = new Flee();
    private final Arrive arrive = new Arrive();
    private final Wander wander = new Wander();
    private final Wander wanderMix = new Wander();
    private final PathFollowing follow;
    private final Arrive brake = new Arrive().slowingRadius(30);   // freio: Arrive no próprio lugar
    private final SteeringMixer mixer;

    public LabAgent(Path path, Vector2 start) {
        super(settings -> settings
            .name("Agente de teste")
            .color(new Color(206, 150, 78))
            .position(start)
            .orientationDegrees(-90)
            .mass(1)
            .maxForce(600)
            .maxSpeed(320)
            .radius(13));
        this.follow = new PathFollowing(path).lookAhead(60).predictTime(0.3);

        // Ordem = prioridade: o desvio vem primeiro e pode usar toda a força disponível
        mixer = new SteeringMixer()
            .add("Obstacle Avoidance", avoid, Tuning.LAB_AVOID, () -> avoidOn ? 1 : 0)
            .add("Seek (mouse)", seek, Tuning.LAB_PRIMARY, () -> mode == Mode.SEEK ? 1 : 0)
            .add("Flee (mouse)", flee, Tuning.LAB_PRIMARY, () -> mode == Mode.FLEE ? 1 : 0)
            .add("Arrive (clique)", arrive, Tuning.LAB_PRIMARY, () -> mode == Mode.ARRIVE ? 1 : 0)
            .add("Wander", wander, Tuning.LAB_PRIMARY, () -> mode == Mode.WANDER ? 1 : 0)
            .add("Path Following", follow, Tuning.LAB_PRIMARY, () -> mode == Mode.PATH ? 1 : 0)
            .add("Wander (sobreposto)", wanderMix, Tuning.LAB_WANDER_MIX, () -> wanderOn && mode != Mode.WANDER ? 1 : 0)
            .add("Arrive (freio)", brake, Weight.constant(1), () -> brakeNeeded ? 1 : 0);
    }

    @Override
    public Vector2 calculateSteering(World world) {
        final var mouse = world.getMousePos();
        final var click = world.getClickPos();
        final double speed = Tuning.LAB_SPEED.get();

        avoid.lookAhead(Tuning.LAB_AVOID_LOOK.get());
        seek.target(mouse).speed(speed);
        flee.threat(mouse).speed(speed).panicRadius(Tuning.LAB_FLEE_PANIC.get());
        arrive.target(click).speed(speed).slowingRadius(Tuning.LAB_ARRIVE_SLOW.get());
        configureWander(wander, speed * 0.6);
        configureWander(wanderMix, speed * 0.6);
        follow.speed(speed * 0.75);
        brake.target(getPosition());

        // o agente freia em vez de deslizar para sempre.
        brakeNeeded = switch (mode) {
            case SEEK -> mouse == null;
            case FLEE -> !flee.isActive(this);
            case ARRIVE -> click == null;
            default -> false;
        };
        return mixer.calculate(this, world);
    }

    private static void configureWander(Wander w, double speed) {
        w.speed(speed)
            .distance(Tuning.LAB_WANDER_DIST.get())
            .radius(Tuning.LAB_WANDER_RADIUS.get())
            .jitter(Tuning.LAB_WANDER_JITTER.get());
    }

    //Teclas da cena: 1-5 escolhem o comportamento, A e W ligam as combinações
    public void onKey(int code) {
        switch (code) {
            case KeyEvent.VK_1, KeyEvent.VK_NUMPAD1 -> setMode(Mode.SEEK);
            case KeyEvent.VK_2, KeyEvent.VK_NUMPAD2 -> setMode(Mode.FLEE);
            case KeyEvent.VK_3, KeyEvent.VK_NUMPAD3 -> setMode(Mode.ARRIVE);
            case KeyEvent.VK_4, KeyEvent.VK_NUMPAD4 -> setMode(Mode.WANDER);
            case KeyEvent.VK_5, KeyEvent.VK_NUMPAD5 -> setMode(Mode.PATH);
            case KeyEvent.VK_A -> avoidOn = !avoidOn;
            case KeyEvent.VK_W -> wanderOn = !wanderOn;
            default -> { }
        }
    }

    public void setMode(Mode mode) {
        if (mode == Mode.PATH && this.mode != Mode.PATH) follow.reset();
        this.mode = mode;
    }

    public Mode getMode() { return mode; }
    public void setAvoidance(boolean on) { avoidOn = on; }
    public void setWanderOverlay(boolean on) { wanderOn = on; }

    public List<HudLine> statusLines() {
        final var lines = new ArrayList<HudLine>();
        lines.add(HudLine.of("Modo %s · %s: %s".formatted(mode.key, mode.title, mode.description), mode.color));
        lines.add(HudLine.of("A · Obstacle Avoidance: " + (avoidOn ? "LIGADO" : "desligado"),
            avoidOn ? ObstacleAvoidance.COLOR : new Color(110, 114, 124)));
        lines.add(HudLine.of("W · Wander sobreposto: " + (wanderOn ? "LIGADO" : "desligado"),
            wanderOn ? Wander.COLOR : new Color(110, 114, 124)));
        final var combo = new StringBuilder("Combinação: ");
        if (avoidOn) combo.append("Obstacle Avoidance > ");
        combo.append(mode.title);
        if (wanderOn && mode != Mode.WANDER) combo.append(" + Wander");
        lines.add(HudLine.of(combo.toString()));
        lines.add(HudLine.of("C · Mistura: " + SteeringMixer.getMode().label));
        return lines;
    }

    // ---------------- debug / HUD ----------------

    @Override
    protected void drawBody(Graphics2D g) {
        Sprites.raptor(g, getColor(), getRadius(), false, 0, false);
    }

    @Override
    protected void drawDebug(Graphics2D g) {
        mixer.drawDebug(g, this);
    }

    @Override
    protected Color getRingColor() { return mode.color; }

    @Override
    protected String getLabel() { return mode.title + (avoidOn ? " + Avoidance" : ""); }

    @Override
    public String getStateName() { return mode.title; }

    @Override
    public List<ForceReport> getForceReport() {
        // mostra só o que está ligado (e sempre o desvio, para ver quando ele entra)
        return mixer.getReport().stream()
            .filter(f -> f.weight() > 0 || f.label().startsWith("Obstacle"))
            .toList();
    }
}
