package pack.engine;

/** Chaves globais de debug (alteradas pelo teclado na janela). */
public final class Debug {
    /** D: liga/desliga todo o debug visual (setas, feelers, caminhos...). */
    public static volatile boolean enabled = true;
    /** V: cones de visão e linhas de visada. */
    public static volatile boolean vision = true;
    /** L: rótulos com nome e estado acima dos agentes. */
    public static volatile boolean labels = true;

    private Debug() { }
}
