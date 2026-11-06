package pack.engine;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import static pack.engine.Vector2.*;

/** Pilar / rocha circular. */
public final class CircleObstacle implements Obstacle {
    private final Vector2 center;
    private final double radius;

    public CircleObstacle(double x, double y, double radius) {
        this.center = vec2(x, y);
        this.radius = radius;
    }

    public Vector2 getCenter() { return center.clone(); }
    public double getRadius() { return radius; }

    @Override
    public RayHit raycast(Vector2 o, Vector2 d, double maxDist, double inflate) {
        final double r = radius + inflate;
        final var m = subtract(o, center);
        final double b = m.dot(d);
        final double c = m.dot(m) - r * r;
        if (c <= 0) {
            // origem já está dentro do círculo inflado
            final var n = m.isZero() ? negate(d) : normalize(m);
            return new RayHit(o.clone(), n, 0, this);
        }
        if (b > 0) return null;              // apontando para longe
        final double disc = b * b - c;
        if (disc < 0) return null;           // passa ao lado
        final double t = -b - Math.sqrt(disc);
        if (t > maxDist) return null;
        final var p = add(o, multiply(d, t));
        return new RayHit(p, normalize(subtract(p, center)), t, this);
    }

    @Override
    public Contact penetration(Vector2 p, double r) {
        final var d = subtract(p, center);
        final double dist = d.size();
        final double min = radius + r;
        if (dist >= min) return null;
        final var n = dist > 1e-9 ? d.divide(dist) : vec2(1, 0);
        return new Contact(n, min - dist);
    }

    @Override
    public void draw(Graphics2D g) {
        final double r = radius;
        // sombra
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(center.x - r + 5, center.y - r + 6, r * 2, r * 2));
        // pedra com luz vindo de cima/esquerda
        final var paint = new RadialGradientPaint(
            new Point2D.Double(center.x - r * 0.35, center.y - r * 0.4), (float) (r * 1.4),
            new float[]{0f, 0.6f, 1f},
            new Color[]{new Color(168, 163, 152), new Color(122, 117, 108), new Color(82, 78, 72)});
        g.setPaint(paint);
        g.fill(new Ellipse2D.Double(center.x - r, center.y - r, r * 2, r * 2));
        g.setColor(new Color(50, 47, 43));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(center.x - r, center.y - r, r * 2, r * 2));
        // anel interno (topo do pilar)
        g.setColor(new Color(255, 255, 255, 35));
        g.draw(new Ellipse2D.Double(center.x - r * 0.62, center.y - r * 0.62, r * 1.24, r * 1.24));
    }
}
