package pack.student;

import pack.engine.GameMap;
import pack.engine.Vector2;

import java.awt.Color;

import static pack.engine.Vector2.vec2;

/**
 * Mapas de teste. Origem no centro; x vai de -640 a 640 e y de -380 a 380 (y para baixo).
 */
public final class TestMaps {
    public static final String LAB_PATH = "laboratorio";
    public static final String PREY_PATH = "presa";

    /** Território (toca) da matilha na arena. */
    public static final Vector2 DEN_CENTER = vec2(-360, 20);
    public static final double DEN_RADIUS = 250;

    private TestMaps() { }

    /**
     * Cena 1: área aberta com alguns pilares e dois muros, e um caminho fechado em volta.
     * Um pilar foi colocado DE PROPÓSITO em cima do caminho (-225, -275) para testar a
     * combinação Path Following + Obstacle Avoidance.
     */
    public static GameMap laboratorio() {
        return new GameMap("Laboratório", 1280, 760)
            .ground(new Color(66, 80, 58))
            .seed(3)
            .circle(-200, -70, 42)
            .circle(40, 70, 52)
            .circle(250, -90, 36)
            .circle(-30, -190, 26)
            .circle(-225, -275, 26)      // em cima do caminho, de propósito
            .box(-160, 170, 180, 26)
            .box(330, 150, 26, 150)
            .path(LAB_PATH, true, 26,
                -460, -250, 0, -300, 450, -250, 540, 0,
                450, 250, 0, 300, -450, 250, -540, 0);
    }

    /**
     * Cena 2: arena com ruínas e pilares. A matilha vive à esquerda (território marrom);
     * a rota da presa contorna o mapa e atravessa a borda do território.
     * Pilares e muros servem de cobertura: bloqueiam a linha de visão dos dois lados.
     */
    public static GameMap arena() {
        return new GameMap("Arena", 1280, 760)
            .ground(new Color(70, 84, 58))
            .seed(11)
            .patch(DEN_CENTER.x, DEN_CENTER.y, 265, 235, new Color(96, 82, 58, 95))
            .patch(DEN_CENTER.x - 20, DEN_CENTER.y + 10, 140, 115, new Color(104, 88, 60, 85))
            // dentro do território
            .circle(-300, -100, 30)
            .circle(-430, 130, 26)
            .circle(-220, 110, 34)
            .box(-360, 30, 120, 24)
            // centro
            .circle(-40, -160, 32)
            .box(-40, 40, 26, 170)
            .circle(120, -40, 40)
            .circle(90, 170, 28)
            .box(260, 80, 170, 26)
            // direita
            .circle(300, -170, 30)
            .circle(430, 30, 34)
            .box(420, -120, 26, 110)
            .path(PREY_PATH, true, 28,
                470, -250, 150, -300, -180, -290, -470, -200, -545, 60,
                -420, 280, -120, 300, 200, 290, 480, 230, 565, -20);
    }
}
