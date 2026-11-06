package pack.engine;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

//cena = mapa + agentes + lógica + desenho extra
public final class Scene {
    // a logica é executada antes dos agentes
    @FunctionalInterface
    public interface Updater {
        void update(double dt, double time);
    }

    private final String title;
    private final String subtitle;
    private final GameMap map;
    private final List<Agent> agents = new ArrayList<>();
    private final List<Updater> updaters = new ArrayList<>();
    private final List<Consumer<Graphics2D>> underlays = new ArrayList<>();
    private final List<Consumer<Graphics2D>> overlays = new ArrayList<>();
    private final List<WeightGroup> tuning = new ArrayList<>();
    private final List<String> help = new ArrayList<>();
    private IntConsumer keyHandler = k -> { };
    private Supplier<List<HudLine>> status = List::of;
    private Agent defaultSelection;

    public Scene(String title, String subtitle, GameMap map) {
        this.title = title;
        this.subtitle = subtitle;
        this.map = map;
    }

    public Scene add(Agent... newAgents) {
        Collections.addAll(agents, newAgents);
        if (defaultSelection == null && newAgents.length > 0) defaultSelection = newAgents[0];
        return this;
    }

    public Scene add(List<? extends Agent> newAgents) {
        newAgents.forEach(this::add);
        return this;
    }

    //agente selecionado(para mudar forças)
    public Scene select(Agent agent) {
        this.defaultSelection = agent;
        return this;
    }

    public Scene onUpdate(Updater updater) {
        updaters.add(updater);
        return this;
    }

    //desenho do mapa, por baixo dos agentes
    public Scene underlay(Consumer<Graphics2D> drawer) {
        underlays.add(drawer);
        return this;
    }

    //desenho do mundo, por cima dos agentes
    public Scene overlay(Consumer<Graphics2D> drawer) {
        overlays.add(drawer);
        return this;
    }

    //pesos de cada comportamento
    public Scene tune(WeightGroup... groups) {
        Collections.addAll(tuning, groups);
        return this;
    }

    //teclas de atalho para a cena
    public Scene onKey(IntConsumer handler) {
        this.keyHandler = handler;
        return this;
    }

    //linhas de status mostradas no painel lateral
    public Scene status(Supplier<List<HudLine>> status) {
        this.status = status;
        return this;
    }

    //linhas de ajuda da cena
    public Scene help(String... lines) {
        Collections.addAll(help, lines);
        return this;
    }

    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public GameMap getMap() { return map; }
    public List<Agent> getAgents() { return Collections.unmodifiableList(agents); }
    public List<WeightGroup> getTuning() { return Collections.unmodifiableList(tuning); }
    public List<String> getHelp() { return Collections.unmodifiableList(help); }
    public Agent getDefaultSelection() { return defaultSelection; }

    void update(double dt, double time) {
        for (var u : updaters) u.update(dt, time);
    }

    void drawUnderlays(Graphics2D g) {
        for (var d : underlays) {
            final var g2 = (Graphics2D) g.create();
            d.accept(g2);
            g2.dispose();
        }
    }

    void drawOverlays(Graphics2D g) {
        for (var d : overlays) {
            final var g2 = (Graphics2D) g.create();
            d.accept(g2);
            g2.dispose();
        }
    }

    void key(int keyCode) { keyHandler.accept(keyCode); }

    List<HudLine> statusLines() {
        final var lines = status.get();
        return lines == null ? List.of() : lines;
    }
}
