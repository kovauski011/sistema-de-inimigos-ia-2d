package pack.engine;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

//desenhos de agentes (sprites)
public final class Sprites {
    private Sprites() { }

    private static Color mix(Color a, Color b, double t) {
        t = Math.clamp(t, 0, 1);
        return new Color(
            (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static Ellipse2D ell(double x, double y, double w, double h) {
        return new Ellipse2D.Double(x, y, w, h);
    }

    /** Agente genérico (seta arredondada), usado quando a subclasse não define um desenho. */
    public static void arrowAgent(Graphics2D g, Color color, double r) {
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(ell(-r + 3, -r + 4, r * 2, r * 2));
        final var body = new Path2D.Double();
        body.moveTo(r * 1.4, 0);
        body.lineTo(-r, -r * 0.85);
        body.lineTo(-r * 0.5, 0);
        body.lineTo(-r, r * 0.85);
        body.closePath();
        g.setColor(color);
        g.fill(body);
        g.setColor(color.darker().darker());
        g.setStroke(new BasicStroke(1.5f));
        g.draw(body);
    }

    
    public static void raptor(Graphics2D g, Color skin, double r, boolean alpha, double flash, boolean jaw) {
        final var dark = mix(skin, Color.BLACK, 0.38);
        final var light = mix(skin, Color.WHITE, 0.22);

        // sombra
        g.setColor(new Color(0, 0, 0, 65));
        g.fill(ell(-2.3 * r + 4, -0.8 * r + 5, 4.6 * r, 1.6 * r));

        // cauda afinando para trás
        final var tail = new Path2D.Double();
        tail.moveTo(-0.55 * r, -0.42 * r);
        tail.quadTo(-1.5 * r, -0.26 * r, -2.5 * r, 0);
        tail.quadTo(-1.5 * r, 0.26 * r, -0.55 * r, 0.42 * r);
        tail.closePath();
        g.setColor(dark);
        g.fill(tail);

        // patas traseiras e braços
        g.setColor(dark);
        g.fill(ell(-0.55 * r, -0.98 * r, 0.75 * r, 0.42 * r));
        g.fill(ell(-0.55 * r, 0.56 * r, 0.75 * r, 0.42 * r));
        g.fill(ell(0.45 * r, -0.62 * r, 0.42 * r, 0.24 * r));
        g.fill(ell(0.45 * r, 0.38 * r, 0.42 * r, 0.24 * r));

        // corpo
        g.setPaint(new GradientPaint(0, (float) (-0.7 * r), light, 0, (float) (0.7 * r), dark));
        g.fill(ell(-0.95 * r, -0.68 * r, 1.95 * r, 1.36 * r));
        // listras transversais
        g.setColor(new Color(0, 0, 0, 60));
        for (int i = 0; i < 4; i++) {
            g.fill(ell(-0.7 * r + i * 0.38 * r, -0.55 * r, 0.12 * r, 1.1 * r));
        }

        // pescoço, cabeça e focinho
        g.setColor(skin);
        g.fill(ell(0.7 * r, -0.3 * r, 0.75 * r, 0.6 * r));
        g.setPaint(new GradientPaint(0, (float) (-0.4 * r), light, 0, (float) (0.4 * r), skin));
        g.fill(ell(1.15 * r, -0.42 * r, 0.95 * r, 0.84 * r));
        g.fill(ell(1.75 * r, -0.27 * r, 0.75 * r, 0.54 * r));
        if (jaw) {
            g.setColor(new Color(150, 20, 20));
            g.setStroke(new BasicStroke((float) Math.max(1.2, r * 0.12)));
            g.draw(new Line2D.Double(1.95 * r, 0, 2.45 * r, 0));
        }
        // olhos
        g.setColor(new Color(255, 214, 70));
        g.fill(ell(1.5 * r, -0.36 * r, 0.2 * r, 0.16 * r));
        g.fill(ell(1.5 * r, 0.2 * r, 0.2 * r, 0.16 * r));
        g.setColor(Color.BLACK);
        g.fill(ell(1.58 * r, -0.34 * r, 0.08 * r, 0.12 * r));
        g.fill(ell(1.58 * r, 0.22 * r, 0.08 * r, 0.12 * r));

        if (alpha) {
            // crista vermelha ao longo da cabeça e das costas + aro dourado
            final var crest = new Path2D.Double();
            crest.moveTo(1.85 * r, 0);
            for (int i = 0; i <= 8; i++) {
                final double x = 1.85 * r - i * 0.3 * r;
                final double w = (i % 2 == 0 ? 0.08 : 0.2) * r;
                crest.lineTo(x, -w);
            }
            for (int i = 8; i >= 0; i--) {
                final double x = 1.85 * r - i * 0.3 * r;
                final double w = (i % 2 == 0 ? 0.08 : 0.2) * r;
                crest.lineTo(x, w);
            }
            crest.closePath();
            g.setColor(new Color(215, 30, 30));
            g.fill(crest);
            g.setColor(new Color(255, 209, 51, 210));
            g.setStroke(new BasicStroke(2f));
            g.draw(ell(-1.95 * r, -1.95 * r, 3.9 * r, 3.9 * r));
        }

        if (flash > 0.01) {
            g.setColor(new Color(255, 240, 200, (int) (140 * Math.clamp(flash, 0, 1))));
            g.fill(ell(-0.95 * r, -0.68 * r, 1.95 * r, 1.36 * r));
            g.fill(ell(1.15 * r, -0.42 * r, 0.95 * r, 0.84 * r));
        }
    }

    
    public static void prey(Graphics2D g, Color coat, double r, double hurt, boolean dead) {
        final var back = new Color(122, 86, 52);
        final var c = dead ? mix(coat, Color.GRAY, 0.6) : coat;

        if (dead) g.rotate(Math.PI / 2.2);

        g.setColor(new Color(0, 0, 0, 65));
        g.fill(ell(-1.2 * r + 4, -0.85 * r + 5, 2.5 * r, 1.7 * r));

        // patas
        g.setColor(mix(c, Color.BLACK, 0.45));
        g.fill(ell(-0.85 * r, -0.95 * r, 0.42 * r, 0.32 * r));
        g.fill(ell(-0.85 * r, 0.63 * r, 0.42 * r, 0.32 * r));
        g.fill(ell(0.35 * r, -0.9 * r, 0.42 * r, 0.3 * r));
        g.fill(ell(0.35 * r, 0.6 * r, 0.42 * r, 0.3 * r));

        // rabinho branco
        g.setColor(new Color(245, 240, 230));
        g.fill(ell(-1.35 * r, -0.2 * r, 0.42 * r, 0.4 * r));

        // corpo
        g.setPaint(new GradientPaint(0, (float) (-0.8 * r), mix(c, Color.WHITE, 0.2), 0, (float) (0.8 * r), mix(c, Color.BLACK, 0.2)));
        g.fill(ell(-1.1 * r, -0.78 * r, 2.05 * r, 1.56 * r));
        // faixa marrom nas costas
        g.setColor(dead ? mix(back, Color.GRAY, 0.6) : back);
        g.fill(ell(-0.95 * r, -0.26 * r, 1.7 * r, 0.52 * r));

        // cabeça e orelhas
        g.setColor(mix(c, Color.WHITE, 0.1));
        g.fill(ell(0.62 * r, -0.45 * r, 0.95 * r, 0.9 * r));
        g.setColor(mix(c, back, 0.5));
        g.fill(ell(0.45 * r, -0.72 * r, 0.55 * r, 0.26 * r));
        g.fill(ell(0.45 * r, 0.46 * r, 0.55 * r, 0.26 * r));
        // focinho e olhos
        g.setColor(new Color(60, 40, 30));
        g.fill(ell(1.47 * r, -0.12 * r, 0.16 * r, 0.24 * r));
        g.setColor(Color.BLACK);
        g.fill(ell(1.08 * r, -0.36 * r, 0.16 * r, 0.14 * r));
        g.fill(ell(1.08 * r, 0.22 * r, 0.16 * r, 0.14 * r));

        if (hurt > 0.01) {
            g.setColor(new Color(255, 30, 20, (int) (170 * Math.clamp(hurt, 0, 1))));
            g.fill(ell(-1.1 * r, -0.78 * r, 2.05 * r, 1.56 * r));
        }
    }
}
