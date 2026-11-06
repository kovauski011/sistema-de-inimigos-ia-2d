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

//serve para fugir de ameaças
public class Evade implements Behavior {
    public static final Color COLOR = new Color(244, 143, 177);

    private final Flee flee = new Flee();
    private final List<Agent> threats = new ArrayList<>();
    private final List<Vector2> predicted = new ArrayList<>();
    private double speed;
    private double panicRadius;
    private double maxPrediction = 0.6;

    public Evade threats(List<? extends Agent> agents) {
        threats.clear();
        threats.addAll(agents);
        return this;
    }

    public Evade speed(double speed) { this.speed = speed; return this; }
    public Evade panicRadius(double panicRadius) { this.panicRadius = panicRadius; return this; }
    public Evade maxPrediction(double seconds) { this.maxPrediction = seconds; return this; }

    @Override
    public Vector2 calculate(Agent agent, World world) {
        flee.clear().speed(speed).panicRadius(panicRadius);
        predicted.clear();
        final double mySpeed = Steer.speed(speed, agent);
        for (var t : threats) {
            final double d = Agent.distance(agent, t);
            final double time = Math.min(maxPrediction, d / Math.max(1, mySpeed + t.getSpeed()));
            final var future = add(t.getPosition(), multiply(t.getVelocity(), time));
            predicted.add(future);
            flee.addThreat(future, 1.0);
        }
        return flee.calculate(agent, world);
    }

    @Override
    public void drawDebug(Graphics2D g, Agent agent) {
        for (int i = 0; i < predicted.size() && i < threats.size(); i++) {
            Draw.dashedLine(g, threats.get(i).getPosition(), predicted.get(i), Draw.alpha(COLOR, 0.7f), 1.2f);
            Draw.cross(g, predicted.get(i), 5, COLOR, 1.8f);
        }
        flee.drawDebug(g, agent);
    }

    @Override
    public Color color() { return COLOR; }
}
