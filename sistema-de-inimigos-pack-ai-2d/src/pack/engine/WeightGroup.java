package pack.engine;

import java.util.List;

/** Grupo de pesos mostrado junto no painel (ex.: "Raptor · Caça"). */
public final class WeightGroup {
    private final String name;
    private final List<Weight> weights;

    public WeightGroup(String name, Weight... weights) {
        this.name = name;
        this.weights = List.of(weights);
    }

    public String getName() { return name; }
    public List<Weight> getWeights() { return weights; }

    public void resetAll() { weights.forEach(Weight::reset); }
}
