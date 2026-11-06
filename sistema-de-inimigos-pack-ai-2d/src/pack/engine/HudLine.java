package pack.engine;

import java.awt.Color;

/** Linha de texto do painel lateral, com cor opcional (bolinha colorida à esquerda). */
public record HudLine(String text, Color color) {
    public static HudLine of(String text) {
        return new HudLine(text, null);
    }

    public static HudLine of(String text, Color color) {
        return new HudLine(text, color);
    }
}
