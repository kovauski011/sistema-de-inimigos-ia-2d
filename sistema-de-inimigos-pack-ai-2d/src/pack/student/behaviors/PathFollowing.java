package pack.student.behaviors;

import pack.engine.Agent;
import pack.engine.Draw;
import pack.engine.Path;
import pack.engine.Vector2;
import pack.engine.World;

import java.awt.Color;
import java.awt.Graphics2D;

import static pack.engine.Vector2.*;

//busca o ponto mais próximo do caminho e segue para um ponto à frente, suavizando curvas
public class PathFollowing implements Behavior {
    public static final Color COLOR = new Color(102, 187, 106);

    private Path path;
    private double speed;
    private double predictTime = 0.3;
    private double lookAhead = 60;
    private double s = Double.NaN;

    // debug
    private Vector2 predicted;
    private Vector2 closest;
    private Vector2 target;
    private boolean onPath;

    public PathFollowing() { }

    public PathFollowing(Path path) { this.path = path; }

    public PathFollowing path(Path path) {
        if (path != this.path) s = Double.NaN;
        this.path = path;
        return this;
    }

    public PathFollowing speed(double speed) { this.speed = speed; return this; }
    public PathFollowing predictTime(double predictTime) { this.predictTime = predictTime; return this; }
    public PathFollowing lookAhead(double lookAhead) { this.lookAhead = lookAhead; return this; }

    // esquece o ponto mais próximo do caminho, forçando a busca global no próximo quadro
    public void reset() { s = Double.NaN; }

    public Path getPath() { return path; }
    public boolean isOnPath() { return onPath; }

    @Override
    public Vector2 calculate(Agent agent, World world) {
        if (path == null) return vec2();
        final var pos = agent.getPosition();
        final var vel = agent.getVelocity();
        predicted = vel.size() > 5 ? add(pos, multiply(vel, predictTime)) : add(pos, multiply(agent.getDirection(), 5));

        if (Double.isNaN(s) || distance(predicted, path.pointAt(s)) > path.getRadius() * 4) {
            s = path.closestParam(predicted);                      // longe do caminho: busca global
        } else {
            s = path.closestParam(predicted, s, lookAhead * 3 + vel.size() * predictTime);
        }
        closest = path.pointAt(s);
        onPath = distance(predicted, closest) <= path.getRadius();

        final double maxSpeed = Steer.speed(speed, agent);
        final double ts = s + lookAhead;
        if (!path.isLoop() && ts >= path.getLength()) {
            // fim de um caminho aberto: Arrive no último ponto
            target = path.pointAt(path.getLength());
            final var toEnd = subtract(target, pos);
            final double d = toEnd.size();
            if (d < 2) return Steer.brake(agent);
            return Steer.toward(agent, toEnd.resize(maxSpeed * Math.min(1, d / 100)));
        }
        target = path.pointAt(ts);
        final var desired = subtract(target, pos);
        if (desired.isZero()) return vec2();
        return Steer.toward(agent, desired.resize(maxSpeed));
    }

    @Override
    public void drawDebug(Graphics2D g, Agent agent) {
        if (predicted == null) return;
        final var c = onPath ? COLOR : new Color(255, 90, 90);
        Draw.line(g, agent.getPosition(), predicted, Draw.alpha(c, 0.5f), 1f);
        Draw.fillCircle(g, predicted, 3.5, c);
        Draw.dashedLine(g, predicted, closest, Draw.alpha(c, 0.6f), 1f);
        Draw.circle(g, closest, 3.5, Draw.alpha(COLOR, 0.8f), 1.2f);
        Draw.cross(g, target, 5, Draw.alpha(COLOR, 0.95f), 2f);
    }

    @Override
    public Color color() { return COLOR; }
}
