package pack.student.behaviors;

import pack.engine.Agent;
import pack.engine.Draw;
import pack.engine.Vector2;
import pack.engine.World;

import java.awt.Color;
import java.awt.Graphics2D;

import static pack.engine.Vector2.*;

//vai em linha reta até o ponto alvo, sem frear
public class Seek implements Behavior {
    public static final Color COLOR = new Color(255, 202, 40);

    private Vector2 target;
    private double speed;

    public Seek() { }

    public Seek(double speed) { this.speed = speed; }

    public Seek target(Vector2 target) {
        this.target = target == null ? null : target.clone();
        return this;
    }

    public Seek speed(double speed) {
        this.speed = speed;
        return this;
    }

    public Vector2 getTarget() { return target == null ? null : target.clone(); }

    @Override
    public Vector2 calculate(Agent agent, World world) {
        if (target == null) return vec2();
        final var desired = subtract(target, agent.getPosition());
        if (desired.isZero()) return vec2();
        desired.resize(Steer.speed(speed, agent));
        return Steer.toward(agent, desired);
    }

    @Override
    public void drawDebug(Graphics2D g, Agent agent) {
        if (target == null) return;
        Draw.dashedLine(g, agent.getPosition(), target, Draw.alpha(COLOR, 0.35f), 1f);
        Draw.cross(g, target, 6, Draw.alpha(COLOR, 0.9f), 2f);
    }

    @Override
    public Color color() { return COLOR; }
}
