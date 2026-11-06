package pack.engine;

import java.awt.Color;

/**
 * Uma linha do painel "Forças": quanto cada comportamento pediu e quanto realmente
 * foi aplicado depois da combinação.
 *
 * @param label   nome do comportamento (ex.: "Obstacle Avoidance")
 * @param wanted  força pedida já multiplicada pelo peso
 * @param applied força que entrou na soma final (pode ter sido cortada pela prioridade)
 * @param color   cor usada na seta e na barra
 * @param weight  peso efetivo usado neste quadro (0 = desligado)
 */
public record ForceReport(String label, Vector2 wanted, Vector2 applied, Color color, double weight) {
}
