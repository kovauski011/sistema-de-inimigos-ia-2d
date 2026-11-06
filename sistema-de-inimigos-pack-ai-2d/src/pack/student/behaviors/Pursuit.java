package pack.student.behaviors;

import pack.engine.Agent;
import pack.engine.Draw;
import pack.engine.Vector2;
import pack.engine.World;

import java.awt.Color;
import java.awt.Graphics2D;

import static pack.engine.Vector2.*;

//percegue a presa procurando a posição futura dela
public class Pursuit implements Behavior {
    public static final Color COLOR = new Color(255, 152, 0);

    private Agent target;
    private double speed;
    private double maxPrediction = 0.8;
    private Vector2 future;

    public Pursuit target(Agent target) { this.target = target; return this; }
    public Pursuit speed(double speed) { this.speed = speed; return this; }
    public Pursuit maxPrediction(double seconds) { this.maxPrediction = seconds; return this; }

    @Override
    public Vector2 calculate(Agent agent, World world) {
        if (target == null) return vec2();
        final double mySpeed = Steer.speed(speed, agent);
        final double d = Agent.distance(agent, target);
        final double t = Math.min(maxPrediction, d / Math.max(1, mySpeed + target.getSpeed()));
        future = add(target.getPosition(), multiply(target.getVelocity(), t));
        final var desired = subtract(future, agent.getPosition());
        if (desired.isZero()) return vec2();
        return Steer.toward(agent, desired.resize(mySpeed));
    }

    @Override
    public void drawDebug(Graphics2D g, Agent agent) {
        if (future == null || target == null) return;
        Draw.dashedLine(g, target.getPosition(), future, Draw.alpha(COLOR, 0.6f), 1.2f);
        Draw.arrowTo(g, agent.getPosition(), future, Draw.alpha(COLOR, 0.85f), 1.6f);
        Draw.circle(g, future, 6, COLOR, 1.5f);
    }

    @Override
    public Color color() { return COLOR; }
}
