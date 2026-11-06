package pack.engine;

import java.util.List;

import static pack.engine.Agent.distance;

/**
 * O que um agente "enxerga" do mundo no quadro atual (mesmo papel do World do SBCars).
 * Uma instância nova é criada para cada agente a cada quadro.
 */
public final class World {
    private final double secs;
    private final double time;
    private final Agent current;
    private final List<Agent> agents;
    private final Vector2 mousePos;   // coordenadas do mundo; pode ser null
    private final Vector2 clickPos;   // coordenadas do mundo; pode ser null
    private final GameMap map;

    World(double secs, double time, Agent current, List<Agent> agents,
          Vector2 mousePos, Vector2 clickPos, GameMap map) {
        this.secs = secs;
        this.time = time;
        this.current = current;
        this.agents = agents;
        this.mousePos = mousePos;
        this.clickPos = clickPos;
        this.map = map;
    }

    /** Segundos desde o último quadro (já com câmera lenta aplicada). */
    public double getSecs() { return secs; }

    /** Tempo de simulação desde o início da cena, em segundos. */
    public double getTime() { return time; }

    /** Posição do cursor do mouse (coordenadas do mundo), ou null. */
    public Vector2 getMousePos() { return mousePos == null ? null : mousePos.clone(); }

    /** Posição do último clique (coordenadas do mundo), ou null se ninguém clicou ainda. */
    public Vector2 getClickPos() { return clickPos == null ? null : clickPos.clone(); }

    public GameMap getMap() { return map; }

    public double getWidth() { return map.getWidth(); }
    public double getHeight() { return map.getHeight(); }

    /** Todos os agentes, exceto o próprio. */
    public List<Agent> getNeighbors() {
        return agents.stream().filter(a -> a != current).toList();
    }

    /** Agentes a até {@code radius} pixels, exceto o próprio. */
    public List<Agent> getNeighbors(double radius) {
        return agents.stream()
            .filter(a -> a != current)
            .filter(a -> distance(current, a) <= radius)
            .toList();
    }

    /** Agentes de um tipo (ex.: {@code world.getAgents(Raptor.class)}), exceto o próprio. */
    public <T extends Agent> List<T> getAgents(Class<T> type) {
        return agents.stream()
            .filter(a -> a != current)
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }

    /** Raycast contra o mapa (ver {@link GameMap#raycast}). */
    public RayHit raycast(Vector2 origin, Vector2 dir, double maxDist, double inflate) {
        return map.raycast(origin, dir, maxDist, inflate);
    }

    /** True se nenhum obstáculo bloqueia a linha entre a e b. */
    public boolean hasLineOfSight(Vector2 a, Vector2 b) {
        return map.hasLineOfSight(a, b);
    }
}
