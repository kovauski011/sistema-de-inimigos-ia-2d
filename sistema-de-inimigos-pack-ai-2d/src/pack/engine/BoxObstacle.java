package pack.engine;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import static pack.engine.Vector2.*;

// retangulo
public final class BoxObstacle implements Obstacle {
    private static final double EPS = 1e-9;
    private final double cx, cy, hw, hh;

    /** @param cx centro x, cy centro y, w largura, h altura */
    public BoxObstacle(double cx, double cy, double w, double h) {
        this.cx = cx;
        this.cy = cy;
        this.hw = w / 2.0;
        this.hh = h / 2.0;
    }

    public double minX() { return cx - hw; }
    public double maxX() { return cx + hw; }
    public double minY() { return cy - hh; }
    public double maxY() { return cy + hh; }

    @Override
    public RayHit raycast(Vector2 o, Vector2 d, double maxDist, double inflate) {
        final double minX = cx - hw - inflate, maxX = cx + hw + inflate;
        final double minY = cy - hh - inflate, maxY = cy + hh + inflate;

        if (o.x >= minX && o.x <= maxX && o.y >= minY && o.y <= maxY) {
            // origem dentro: normal da face mais próxima
            return new RayHit(o.clone(), nearestFaceNormal(o, minX, maxX, minY, maxY), 0, this);
        }

        double tEnter = -Double.MAX_VALUE, tExit = Double.MAX_VALUE;
        Vector2 nEnter = null;

        if (Math.abs(d.x) < EPS) {
            if (o.x < minX || o.x > maxX) return null;
        } else {
            double t1 = (minX - o.x) / d.x, t2 = (maxX - o.x) / d.x;
            var n1 = vec2(-1, 0);
            var n2 = vec2(1, 0);
            if (t1 > t2) { double t = t1; t1 = t2; t2 = t; var n = n1; n1 = n2; n2 = n; }
            if (t1 > tEnter) { tEnter = t1; nEnter = n1; }
            tExit = Math.min(tExit, t2);
        }
        if (Math.abs(d.y) < EPS) {
            if (o.y < minY || o.y > maxY) return null;
        } else {
            double t1 = (minY - o.y) / d.y, t2 = (maxY - o.y) / d.y;
            var n1 = vec2(0, -1);
            var n2 = vec2(0, 1);
            if (t1 > t2) { double t = t1; t1 = t2; t2 = t; var n = n1; n1 = n2; n2 = n; }
            if (t1 > tEnter) { tEnter = t1; nEnter = n1; }
            tExit = Math.min(tExit, t2);
        }
        if (nEnter == null || tEnter > tExit || tExit < 0 || tEnter < 0 || tEnter > maxDist) return null;
        return new RayHit(add(o, multiply(d, tEnter)), nEnter, tEnter, this);
    }

    private static Vector2 nearestFaceNormal(Vector2 p, double minX, double maxX, double minY, double maxY) {
        final double dl = p.x - minX, dr = maxX - p.x, dt = p.y - minY, db = maxY - p.y;
        final double m = Math.min(Math.min(dl, dr), Math.min(dt, db));
        if (m == dl) return vec2(-1, 0);
        if (m == dr) return vec2(1, 0);
        if (m == dt) return vec2(0, -1);
        return vec2(0, 1);
    }

    @Override
    public Contact penetration(Vector2 p, double r) {
        final double qx = Math.clamp(p.x, cx - hw, cx + hw);
        final double qy = Math.clamp(p.y, cy - hh, cy + hh);
        final double dx = p.x - qx, dy = p.y - qy;
        final double d2 = dx * dx + dy * dy;
        if (d2 > EPS) {
            if (d2 >= r * r) return null;
            final double d = Math.sqrt(d2);
            return new Contact(vec2(dx / d, dy / d), r - d);
        }
        // centro dentro do bloco: sai pela face mais próxima
        final double dl = p.x - (cx - hw), dr = (cx + hw) - p.x, dt = p.y - (cy - hh), db = (cy + hh) - p.y;
        final double m = Math.min(Math.min(dl, dr), Math.min(dt, db));
        if (m == dl) return new Contact(vec2(-1, 0), dl + r);
        if (m == dr) return new Contact(vec2(1, 0), dr + r);
        if (m == dt) return new Contact(vec2(0, -1), dt + r);
        return new Contact(vec2(0, 1), db + r);
    }

    @Override
    public void draw(Graphics2D g) {
        final var rect = new Rectangle2D.Double(cx - hw, cy - hh, hw * 2, hh * 2);
        // sombra
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Rectangle2D.Double(rect.x + 6, rect.y + 7, rect.width, rect.height));
        // bloco de pedra
        g.setPaint(new GradientPaint((float) rect.x, (float) rect.y, new Color(150, 136, 116),
            (float) rect.x, (float) (rect.y + rect.height), new Color(108, 96, 82)));
        g.fill(rect);
        // juntas dos tijolos
        g.setColor(new Color(70, 62, 52, 140));
        g.setStroke(new BasicStroke(1f));
        final boolean horizontal = hw >= hh;
        final double brick = 28;
        if (horizontal) {
            for (double x = rect.x + brick; x < rect.x + rect.width - 4; x += brick) {
                g.draw(new Line2D.Double(x, rect.y + 2, x, rect.y + rect.height - 2));
            }
        } else {
            for (double y = rect.y + brick; y < rect.y + rect.height - 4; y += brick) {
                g.draw(new Line2D.Double(rect.x + 2, y, rect.x + rect.width - 2, y));
            }
        }
        g.setColor(new Color(255, 255, 255, 40));
        g.draw(new Line2D.Double(rect.x + 1, rect.y + 1, rect.x + rect.width - 1, rect.y + 1));
        g.setColor(new Color(48, 42, 36));
        g.setStroke(new BasicStroke(2f));
        g.draw(rect);
    }
}
