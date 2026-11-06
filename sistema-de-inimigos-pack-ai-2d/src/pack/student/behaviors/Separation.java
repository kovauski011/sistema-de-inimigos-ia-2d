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

//mantem distância de outros agentes do mesmo tipo, evitando colisões
public class Separation implements Behavior {
    public static final Color COLOR = new Color(41, 182, 246);

    private final Class<? extends Agent> type;
    private double radius;
    private final List<Agent> near = new ArrayList<>();

    public Separation(Class<? extends Agent> type, double radius) {
        this.type = type;
        this.radius = radius;
    }

    public Separation radius(double radius) { this.radius = radius; return this; }

    @Override
    public Vector2 calculate(Agent agent, World world) {
        near.clear();
        final var pos = agent.getPosition();
        final var sum = vec2();
        for (var other : world.getAgents(type)) {
            final var away = subtract(pos, other.getPosition());
            final double d = away.size();
            if (d >= radius) continue;
            near.add(other);
            final var dir = d < 1e-6 ? byAngle(Math.random() * Math.PI * 2) : away.divide(d);
            sum.add(dir.multiply(1.0 - d / radius));
        }
        if (sum.isZero()) return vec2();
        return sum.multiply(agent.getMaxForce());
    }

    @Override
    public void drawDebug(Graphics2D g, Agent agent) {
        for (var other : near) {
            Draw.line(g, agent.getPosition(), other.getPosition(), Draw.alpha(COLOR, 0.45f), 1f);
        }
    }

    @Override
    public Color color() { return COLOR; }
}
