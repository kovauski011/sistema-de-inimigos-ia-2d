package pack.student.behaviors;

import pack.engine.Agent;
import pack.engine.ForceReport;
import pack.engine.Vector2;
import pack.engine.Weight;
import pack.engine.World;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

import static pack.engine.Vector2.*;

//combina vários comportamentos numa única força na ordem de prioridade, com pesos 
public final class SteeringMixer {
    public enum Mode {
        PRIORITIZED("prioridade (soma truncada)"),
        WEIGHTED_SUM("soma ponderada simples");

        public final String label;

        Mode(String label) { this.label = label; }
    }

    private static volatile Mode mode = Mode.PRIORITIZED;

    public static Mode getMode() { return mode; }

    public static void toggleMode() {
        mode = mode == Mode.PRIORITIZED ? Mode.WEIGHTED_SUM : Mode.PRIORITIZED;
    }

    private record Entry(String label, Behavior behavior, Weight weight, DoubleSupplier gate) { }

    private final List<Entry> entries = new ArrayList<>();
    private List<ForceReport> report = List.of();

    /** Adiciona um comportamento. A ORDEM importa: o primeiro tem mais prioridade. */
    public SteeringMixer add(String label, Behavior behavior, Weight weight) {
        return add(label, behavior, weight, null);
    }

    /** Adiciona um comportamento com um multiplicador dinâmico (0 = desligado neste quadro). */
    public SteeringMixer add(String label, Behavior behavior, Weight weight, DoubleSupplier gate) {
        entries.add(new Entry(label, behavior, weight, gate));
        return this;
    }

    public Vector2 calculate(Agent agent, World world) {
        final var lines = new ArrayList<ForceReport>(entries.size());
        final var total = vec2();
        final double budget = agent.getMaxForce();
        double used = 0;

        for (var e : entries) {
            final double k = e.weight().get() * (e.gate() == null ? 1.0 : Math.max(0, e.gate().getAsDouble()));
            if (k <= 0) {
                lines.add(new ForceReport(e.label(), vec2(), vec2(), e.behavior().color(), 0));
                continue;
            }
            final var raw = e.behavior().calculate(agent, world);
            final var wanted = multiply(raw == null ? vec2() : raw, k);
            final Vector2 applied;
            if (mode == Mode.PRIORITIZED) {
                final double remaining = budget - used;
                applied = remaining <= 0 ? vec2() : truncate(wanted, remaining);
                used += applied.size();
            } else {
                applied = wanted.clone();
            }
            total.add(applied);
            lines.add(new ForceReport(e.label(), wanted, applied, e.behavior().color(), k));
        }
        report = lines;
        return total; // o motor ainda trunca o total em maxForce
    }

    /** Forças do último quadro, na ordem de prioridade. */
    public List<ForceReport> getReport() { return report; }

    /** Desenha o debug dos comportamentos que estavam ativos no último quadro. */
    public void drawDebug(Graphics2D g, Agent agent) {
        for (int i = 0; i < entries.size() && i < report.size(); i++) {
            if (report.get(i).weight() > 0) entries.get(i).behavior().drawDebug(g, agent);
        }
    }
}
