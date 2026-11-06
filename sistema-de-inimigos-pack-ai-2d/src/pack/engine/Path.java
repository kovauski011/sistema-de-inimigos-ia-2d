package pack.engine;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static pack.engine.Vector2.*;

// caminho usado pelo Path Following
public final class Path {
    private final String name;
    private final List<Vector2> points;
    private final boolean loop;
    private final double radius;
    private final double[] start;   // s no inicio de cada segmento
    private final double[] segLen;  // comprimento de cada segmento
    private final double length;

    public Path(String name, boolean loop, double radius, List<Vector2> pts) {
        if (pts.size() < 2) throw new IllegalArgumentException("path needs at least 2 points");
        this.name = name;
        this.loop = loop;
        this.radius = radius;
        var copy = new ArrayList<Vector2>();
        for (var p : pts) copy.add(p.clone());
        this.points = Collections.unmodifiableList(copy);

        final int n = segmentCount();
        start = new double[n];
        segLen = new double[n];
        double acc = 0;
        for (int i = 0; i < n; i++) {
            start[i] = acc;
            segLen[i] = Vector2.distance(a(i), b(i));
            acc += segLen[i];
        }
        length = acc;
    }

    public String getName() { return name; }
    public boolean isLoop() { return loop; }
    //raio do caminho
    public double getRadius() { return radius; }
    public double getLength() { return length; }
    public List<Vector2> getPoints() { return points; }

    private int segmentCount() { return loop ? points.size() : points.size() - 1; }
    private Vector2 a(int i) { return points.get(i); }
    private Vector2 b(int i) { return points.get((i + 1) % points.size()); }

    //da a volta do caminho se loop=true, ou  [0, length] se loop=false
    public double wrap(double s) {
        if (loop) {
            s %= length;
            return s < 0 ? s + length : s;
        }
        return Math.clamp(s, 0, length);
    }

    private int segmentAt(double s) {
        for (int i = start.length - 1; i > 0; i--) {
            if (s >= start[i]) return i;
        }
        return 0;
    }

    //ponto do caminho
    public Vector2 pointAt(double s) {
        s = wrap(s);
        final int i = segmentAt(s);
        final double t = segLen[i] < 1e-9 ? 0 : (s - start[i]) / segLen[i];
        return lerp(a(i), b(i), Math.clamp(t, 0, 1));
    }

    //direção do caminho
    public Vector2 tangentAt(double s) {
        final int i = segmentAt(wrap(s));
        return normalize(subtract(b(i), a(i)));
    }

    //descide a direção do caminho mais curto
    public double delta(double from, double to) {
        double d = to - from;
        if (loop) {
            d = ((d % length) + length) % length;
            if (d > length / 2) d -= length;
        }
        return d;
    }

    //caminho mais proximo de p 
    public double closestParam(Vector2 p) {
        return closestParam(p, Double.NaN, 0);
    }

    //serve pra n escolherem um caminho que esteja muito longe
    public double closestParam(Vector2 p, double hint, double window) {
        final boolean useHint = !Double.isNaN(hint) && window > 0;
        double best = Double.MAX_VALUE;
        double bestS = 0;
        for (int i = 0; i < start.length; i++) {
            final var ab = subtract(b(i), a(i));
            final double len2 = ab.sizeSqr();
            final double t = len2 < 1e-9 ? 0 : Math.clamp(subtract(p, a(i)).dot(ab) / len2, 0, 1);
            final var q = add(a(i), multiply(ab, t));
            double d2 = distanceSqr(p, q);
            final double s = start[i] + t * segLen[i];
            if (useHint) {
                final double dd = delta(hint, s);
                if (dd < -window * 0.25 || dd > window) d2 += 1e12;
            }
            if (d2 < best) {
                best = d2;
                bestS = s;
            }
        }
        return bestS;
    }

    /** Desenha o corredor, a linha central e setas indicando o sentido. */
    public void draw(Graphics2D g, Color color) {
        final var shape = new Path2D.Double();
        shape.moveTo(points.get(0).x, points.get(0).y);
        for (int i = 1; i < points.size(); i++) shape.lineTo(points.get(i).x, points.get(i).y);
        if (loop) shape.closePath();

        final var g2 = (Graphics2D) g.create();
        g2.setColor(Draw.alpha(color, 0.10f));
        g2.setStroke(new BasicStroke((float) (radius * 2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(shape);
        g2.setColor(Draw.alpha(color, 0.55f));
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
            1f, new float[]{10f, 8f}, 0f));
        g2.draw(shape);

        // pontos do caminho
        g2.setStroke(new BasicStroke(1.5f));
        for (var p : points) {
            g2.setColor(Draw.alpha(color, 0.75f));
            g2.fill(new Ellipse2D.Double(p.x - 3.5, p.y - 3.5, 7, 7));
        }
        // setas de sentido a cada ~140 px
        g2.setColor(Draw.alpha(color, 0.7f));
        for (double s = 70; s < length; s += 140) {
            final var p = pointAt(s);
            final var t = tangentAt(s);
            final var n = perp(t);
            final var tip = add(p, multiply(t, 7));
            final var l = add(subtract(p, multiply(t, 5)), multiply(n, 6));
            final var r = subtract(subtract(p, multiply(t, 5)), multiply(n, 6));
            final var tri = new Path2D.Double();
            tri.moveTo(tip.x, tip.y);
            tri.lineTo(l.x, l.y);
            tri.lineTo(r.x, r.y);
            tri.closePath();
            g2.fill(tri);
        }
        g2.dispose();
    }
}
