package pack.engine;

/**
 * Resultado de um raycast contra o mapa.
 *
 * @param point    ponto de impacto
 * @param normal   normal da superfície no ponto (unitária, apontando para fora do obstáculo)
 * @param distance distância da origem do raio até o impacto
 * @param obstacle obstáculo atingido
 */
public record RayHit(Vector2 point, Vector2 normal, double distance, Obstacle obstacle) {
}
