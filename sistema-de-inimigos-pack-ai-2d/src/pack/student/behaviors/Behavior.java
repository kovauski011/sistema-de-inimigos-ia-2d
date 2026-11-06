package pack.student.behaviors;

import pack.engine.Agent;
import pack.engine.Vector2;
import pack.engine.World;

import java.awt.Color;
import java.awt.Graphics2D;

//serve para calcular a força de direção pedida por este comportamento (antes de aplicar o peso)
public interface Behavior {
    /** Força de direção pedida por este comportamento (antes de aplicar o peso). */
    Vector2 calculate(Agent agent, World world);

    /** Desenho de debug opcional, em coordenadas do mundo. */
    default void drawDebug(Graphics2D g, Agent agent) { }

    /** Cor do comportamento nas setas de debug e no painel de forças. */
    Color color();
}
