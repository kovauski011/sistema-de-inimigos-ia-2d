package pack.student.behaviors;

import pack.engine.Agent;
import pack.engine.Draw;
import pack.engine.Vector2;
import pack.engine.World;

import java.awt.Color;
import java.awt.Graphics2D;

import static pack.engine.Vector2.*;

//serve para chegar a um ponto e parar
public class Arrive implements Behavior {
    public static final Color COLOR = new Color(38, 166, 154);

    private Vector2 target;
    private double speed;
    private double slowingRadius = 120;
    private double arrivedRadius = 2;

    public Arrive() { }

    public Arrive target(Vector2 target) {
        this.target = target == null ? null : target.clone();
        return this;
    }

    public Arrive speed(double speed) {
        this.speed = speed;
        return this;
    }

    public Arrive slowingRadius(double slowingRadius) {
        this.slowingRadius = Math.max(1, slowingRadius);
        return this;
    }

    public Arrive arrivedRadius(double arrivedRadius) {
        this.arrivedRadius = arrivedRadius;
        return this;
    }

    public Vector2 getTarget() { return target == null ? null : target.clone(); }

    @Override
    public Vector2 calculate(Agent agent, World world) {
        if (target == null) return vec2();
        final var toTarget = subtract(target, agent.getPosition());
        final double d = toTarget.size();
        if (d < arrivedRadius) {
            return Steer.brake(agent); // chegou: freia
        }
        final double desiredSpeed = Steer.speed(speed, agent) * Math.min(1.0, d / slowingRadius);
        return Steer.toward(agent, toTarget.resize(desiredSpeed));
    }

    @Override
    public void drawDebug(Graphics2D g, Agent agent) {
        if (target == null) return;
        Draw.dashedCircle(g, target, slowingRadius, Draw.alpha(COLOR, 0.30f), 1f);
        Draw.dashedLine(g, agent.getPosition(), target, Draw.alpha(COLOR, 0.35f), 1f);
        Draw.cross(g, target, 5, Draw.alpha(COLOR, 0.95f), 2f);
    }

    @Override
    public Color color() { return COLOR; }
}
