package pack.student.behaviors;

import pack.engine.Agent;
import pack.engine.Draw;
import pack.engine.RayHit;
import pack.engine.Vector2;
import pack.engine.World;

import java.awt.Color;
import java.awt.Graphics2D;

import static pack.engine.Vector2.*;


//tres raycasts saindo do agente para verificar se há obstaculos na frente, e desviar deles
public class ObstacleAvoidance implements Behavior {
    public static final Color COLOR = new Color(255, 82, 82);

    private double minLength = 30;
    private double lookAhead = 0.45;
    private double sideAngle = Math.toRadians(35);
    private double sideRatio = 0.7;
    private int preferredSide = 1;

    // debug
    private final Vector2[] from = new Vector2[3];
    private final Vector2[] to = new Vector2[3];
    private final RayHit[] hits = new RayHit[3];

    public ObstacleAvoidance minLength(double minLength) { this.minLength = minLength; return this; }
    /** Segundos de antecipação: o bigode central mede minLength + lookAhead * velocidade. */
    public ObstacleAvoidance lookAhead(double seconds) { this.lookAhead = seconds; return this; }
    public ObstacleAvoidance sideAngleDegrees(double degrees) { this.sideAngle = Math.toRadians(degrees); return this; }

    @Override
    public Vector2 calculate(Agent agent, World world) {
        final var pos = agent.getPosition();
        final var dir = agent.getMoveDirection();
        final double len = minLength + lookAhead * agent.getSpeed();
        final double inflate = agent.getRadius();

        final var force = vec2();
        final double[] pen = new double[3];
        double maxPen = 0;
        for (int k = 0; k < 3; k++) {
            final int side = k - 1;                       // -1, 0, +1
            final var fdir = rotate(dir, side * sideAngle);
            final double flen = side == 0 ? len : len * sideRatio;
            final var hit = world.raycast(pos, fdir, flen, inflate);
            from[k] = pos;
            to[k] = add(pos, multiply(fdir, flen));
            hits[k] = hit;
            if (hit != null) {
                pen[k] = 1.0 - hit.distance() / flen;
                force.add(multiply(hit.normal(), pen[k]));
                maxPen = Math.max(maxPen, pen[k]);
            }
        }
        if (maxPen <= 0) return vec2();

        if (force.isZero()) return vec2();

        // Decompõe no referencial do agente: lateral (desvia) e longitudinal (freia).
        final double alongDir = force.dot(dir);
        final var lateral = subtract(force, multiply(dir, alongDir));
        final double total = force.size();
        if (lateral.size() < 0.25 * total) {
            // Parede de frente: escolhe um lado para não ficar só freando.
            final int side;
            if (Math.abs(pen[0] - pen[2]) > 0.05) {
                side = pen[0] < pen[2] ? -1 : 1;          // lado mais livre
            } else if (lateral.size() > 1e-6) {
                side = perpDot(dir, lateral) > 0 ? 1 : -1;
            } else {
                side = preferredSide;
            }
            preferredSide = side;
            lateral.set(0, 0).add(multiply(rotate(dir, side * Math.PI / 2), total));
        }
        final double maxF = agent.getMaxForce() * maxPen;
        final var steer = lateral.normalize().multiply(maxF);
        if (alongDir < 0) {
            steer.add(multiply(dir, maxF * 0.5 * alongDir / total));   // freio (alongDir < 0)
        }
        return steer;
    }

    @Override
    public void drawDebug(Graphics2D g, Agent agent) {
        for (int k = 0; k < 3; k++) {
            if (from[k] == null) continue;
            final var hit = hits[k];
            if (hit == null) {
                Draw.line(g, from[k], to[k], new Color(230, 230, 230, 70), 1f);
            } else {
                Draw.line(g, from[k], hit.point(), Draw.alpha(COLOR, 0.9f), 1.6f);
                Draw.dashedLine(g, hit.point(), to[k], Draw.alpha(COLOR, 0.3f), 1f);
                Draw.fillCircle(g, hit.point(), 3, COLOR);
                Draw.arrowTo(g, hit.point(), add(hit.point(), multiply(hit.normal(), 14)), Draw.alpha(COLOR, 0.8f), 1.2f);
            }
        }
    }

    @Override
    public Color color() { return COLOR; }
}
