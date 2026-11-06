package pack.student.behaviors;

import pack.engine.Agent;
import pack.engine.Draw;
import pack.engine.Vector2;
import pack.engine.World;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

import static pack.engine.Vector2.*;

//desenha um circulo a frente do agente, e escolhe um ponto nesse circulo como alvo, mudando a cada quadro
public class Wander implements Behavior {
    public static final Color COLOR = new Color(171, 71, 188);

    private final Random rnd;
    private double distance = 80;
    private double radius = 40;
    private double jitter = 2.0;
    private double speed;
    private double angle = 0;

    private Vector2 lastCenter;
    private Vector2 lastTarget;

    public Wander() { this(new Random()); }

    public Wander(Random rnd) { this.rnd = rnd; }

    public Wander distance(double distance) { this.distance = distance; return this; }
    public Wander radius(double radius) { this.radius = radius; return this; }
    public Wander jitter(double jitter) { this.jitter = jitter; return this; }
    public Wander speed(double speed) { this.speed = speed; return this; }

    @Override
    public Vector2 calculate(Agent agent, World world) {
        angle += rnd.nextGaussian() * jitter * Math.sqrt(world.getSecs());
        final var pos = agent.getPosition();
        final var heading = agent.getDirection();
        lastCenter = add(pos, multiply(heading, distance));
        lastTarget = add(lastCenter, byAngleSize(heading.getAngle() + angle, radius));
        final var desired = subtract(lastTarget, pos);
        if (desired.isZero()) return vec2();
        return Steer.toward(agent, desired.resize(Steer.speed(speed, agent)));
    }

    @Override
    public void drawDebug(Graphics2D g, Agent agent) {
        if (lastCenter == null) return;
        Draw.line(g, agent.getPosition(), lastCenter, Draw.alpha(COLOR, 0.25f), 1f);
        Draw.circle(g, lastCenter, radius, Draw.alpha(COLOR, 0.55f), 1.2f);
        Draw.line(g, lastCenter, lastTarget, Draw.alpha(COLOR, 0.6f), 1f);
        Draw.fillCircle(g, lastTarget, 3.5, Draw.alpha(COLOR, 0.95f));
    }

    @Override
    public Color color() { return COLOR; }
}
