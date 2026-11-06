package pack.engine;

import java.util.Locale;

/**
 * Peso (ou parâmetro) ajustável em tempo real pelo painel lateral (setas do teclado).
 * Os comportamentos leem {@link #get()} a cada quadro, então mudar o valor tem efeito imediato.
 */
public final class Weight {
    private final String name;
    private final String description;
    private final double defaultValue;
    private final double min;
    private final double max;
    private final double step;
    private volatile double value;

    public Weight(String name, double defaultValue, double min, double max, double step, String description) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = defaultValue;
    }

    /** Peso fixo (não aparece no painel). */
    public static Weight constant(double value) {
        return new Weight("fixo", value, value, value, 0, "");
    }

    public double get() { return value; }
    public void set(double v) { value = Math.clamp(Math.round(v / step) * step, min, max); }
    public void increase(int times) { set(value + step * times); }
    public void decrease(int times) { set(value - step * times); }
    public void reset() { value = defaultValue; }
    public boolean isDefault() { return Math.abs(value - defaultValue) < step * 0.5; }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getDefault() { return defaultValue; }
    public double getMin() { return min; }
    public double getMax() { return max; }

    /** 0..1: posição do valor entre o mínimo e o máximo (para a barra do painel). */
    public double ratio() { return max > min ? (value - min) / (max - min) : 1; }

    public String format() { return format(value); }

    public String format(double v) {
        if (step >= 1) return String.format(Locale.ROOT, "%.0f", v);
        if (step >= 0.1) return String.format(Locale.ROOT, "%.1f", v);
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
