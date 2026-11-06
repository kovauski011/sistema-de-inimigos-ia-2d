package pack.student.behaviors;

import pack.engine.Agent;
import pack.engine.Draw;
import pack.engine.Vector2;
import pack.engine.World;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import static pack.engine.Vector2.*;

//oposto do seek, serve para fugir de ameaças
public class Flee implements Behavior {
    public static final Color COLOR = new Color(236, 64, 122);

    private final List<Vector2> threats = new ArrayList<>();
    private final List<Double> weights = new ArrayList<>();
    private double speed;
    private double panicRadius;
    private Vector2 lastDirection;

    public Flee() { }

    /** Uma única ameaça (substitui as anteriores). */
    public Flee threat(Vector2 threat) {
        clear();
        if (threat != null) addThreat(threat, 1.0);
        return this;
    }

    public Flee addThreat(Vector2 threat, double weight) {
        threats.add(threat.clone());
        weights.add(weight);
        return this;
    }

    public Flee clear() {
        threats.clear();
        weights.clear();
        return this;
    }

    public Flee speed(double speed) {
        this.speed = speed;
        return this;
    }

    public Flee panicRadius(double panicRadius) {
        this.panicRadius = panicRadius;
        return this;
    }

    public double getPanicRadius() { return panicRadius; }

    //true se houver ameaça dentro do raio de pânico
    public boolean isActive(Agent agent) {
        for (var t : threats) {
            if (panicRadius <= 0 || distance(agent.getPosition(), t) <= panicRadius) return true;
        }
        return false;
    }

    @Override
    public Vector2 calculate(Agent agent, World world) {
        final var pos = agent.getPosition();
        final var sum = vec2();
        for (int i = 0; i < threats.size(); i++) {
            final var away = subtract(pos, threats.get(i));
            final double d = away.size();
            if (panicRadius > 0 && d > panicRadius) continue;
            final double proximity = panicRadius > 0 ? 1.0 - d / panicRadius : 1.0;
            final var dir = d < 1e-6 ? byAngle(Math.random() * Math.PI * 2) : away.divide(d);
            sum.add(dir.multiply(weights.get(i) * Math.max(0.05, proximity)));
        }
        if (sum.isZero()) {
            lastDirection = null;
            return vec2();
        }
        lastDirection = normalize(sum);
        return Steer.toward(agent, multiply(lastDirection, Steer.speed(speed, agent)));
    }

    @Override
    public void drawDebug(Graphics2D g, Agent agent) {
        final var pos = agent.getPosition();
        for (var t : threats) {
            if (panicRadius > 0 && distance(pos, t) > panicRadius) continue;
            Draw.cross(g, t, 5, Draw.alpha(COLOR, 0.8f), 1.8f);
            Draw.dashedLine(g, t, pos, Draw.alpha(COLOR, 0.25f), 1f);
        }
        if (threats.size() == 1 && panicRadius > 0) {
            Draw.dashedCircle(g, threats.get(0), panicRadius, Draw.alpha(COLOR, 0.35f), 1f);
        }
        if (lastDirection != null) {
            Draw.arrowTo(g, pos, add(pos, multiply(lastDirection, agent.getRadius() * 3.5)),
                new Color(80, 230, 255, 220), 2.2f);
        }
    }

    @Override
    public Color color() { return COLOR; }
}
