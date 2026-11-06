package pack.engine;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

import static java.lang.Math.toRadians;
import static pack.engine.Vector2.*;

/** Utilitários de desenho para o debug (setas, círculos, cones, rótulos). */
public final class Draw {
    static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 11);

    private Draw() { }

    public static Color alpha(Color c, float a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.clamp(Math.round(a * 255), 0, 255));
    }

    /** Seta saindo de origin com o comprimento de vector * scale. */
    public static void arrow(Graphics2D g, Vector2 origin, Vector2 vector, double scale, Color color, float alpha, float width) {
        if (Vector2.isZero(vector)) return;
        arrowTo(g, origin, add(origin, multiply(vector, scale)), alpha(color, alpha), width);
    }

    /** Seta de from até to. */
    public static void arrowTo(Graphics2D g, Vector2 from, Vector2 to, Color color, float width) {
        final var dir = subtract(to, from);
        if (dir.sizeSqr() < 1) return;
        final var old = g.getStroke();
        final var oldColor = g.getColor();
        g.setColor(color);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(from.x, from.y, to.x, to.y));
        final double angle = dir.getAngle();
        final double a = toRadians(28);
        final double head = Math.min(9.0, 3 + dir.size() * 0.35);
        final var p1 = subtract(to, byAngleSize(angle - a, head));
        final var p2 = subtract(to, byAngleSize(angle + a, head));
        g.draw(new Line2D.Double(to.x, to.y, p1.x, p1.y));
        g.draw(new Line2D.Double(to.x, to.y, p2.x, p2.y));
        g.setStroke(old);
        g.setColor(oldColor);
    }

    public static void line(Graphics2D g, Vector2 a, Vector2 b, Color color, float width) {
        g.setColor(color);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(a.x, a.y, b.x, b.y));
    }

    public static void dashedLine(Graphics2D g, Vector2 a, Vector2 b, Color color, float width) {
        g.setColor(color);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 6f}, 0f));
        g.draw(new Line2D.Double(a.x, a.y, b.x, b.y));
    }

    public static void circle(Graphics2D g, Vector2 c, double r, Color color, float width) {
        g.setColor(color);
        g.setStroke(new BasicStroke(width));
        g.draw(new Ellipse2D.Double(c.x - r, c.y - r, r * 2, r * 2));
    }

    public static void dashedCircle(Graphics2D g, Vector2 c, double r, Color color, float width) {
        final var old = g.getStroke();
        g.setColor(color);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 5f}, 0f));
        g.draw(new Ellipse2D.Double(c.x - r, c.y - r, r * 2, r * 2));
        g.setStroke(old);
    }

    public static void fillCircle(Graphics2D g, Vector2 c, double r, Color color) {
        g.setColor(color);
        g.fill(new Ellipse2D.Double(c.x - r, c.y - r, r * 2, r * 2));
    }

    public static void cross(Graphics2D g, Vector2 p, double size, Color color, float width) {
        g.setColor(color);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(p.x - size, p.y - size, p.x + size, p.y + size));
        g.draw(new Line2D.Double(p.x - size, p.y + size, p.x + size, p.y - size));
    }

    /** Cone de visão: origem, direção (unitária), meia-abertura em radianos e alcance. */
    public static void cone(Graphics2D g, Vector2 origin, Vector2 dir, double halfAngle, double range, Color fill, Color edge) {
        final var shape = new Path2D.Double();
        shape.moveTo(origin.x, origin.y);
        final double base = dir.getAngle();
        final int steps = Math.max(8, (int) (halfAngle * 14));
        for (int i = 0; i <= steps; i++) {
            final double ang = base - halfAngle + (2 * halfAngle) * i / steps;
            shape.lineTo(origin.x + Math.cos(ang) * range, origin.y + Math.sin(ang) * range);
        }
        shape.closePath();
        g.setColor(fill);
        g.fill(shape);
        if (edge != null) {
            g.setColor(edge);
            g.setStroke(new BasicStroke(1f));
            g.draw(shape);
        }
    }

    /** Texto centralizado com sombra, para rótulos sobre os agentes. */
    public static void label(Graphics2D g, String text, double x, double y, Color color) {
        final var oldFont = g.getFont();
        g.setFont(LABEL_FONT);
        final var fm = g.getFontMetrics();
        final var lines = text.split("\n");
        double yy = y - (lines.length - 1) * fm.getHeight();
        for (var line : lines) {
            final float w = fm.stringWidth(line);
            final float tx = (float) (x - w / 2);
            g.setColor(new Color(0, 0, 0, 170));
            g.drawString(line, tx + 1, (float) yy + 1);
            g.setColor(color);
            g.drawString(line, tx, (float) yy);
            yy += fm.getHeight();
        }
        g.setFont(oldFont);
    }
}
