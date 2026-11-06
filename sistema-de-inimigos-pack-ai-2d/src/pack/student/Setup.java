package pack.student;

import pack.engine.Debug;
import pack.engine.Scene;
import pack.engine.Vector2;
import pack.student.behaviors.PathFollowing;
import pack.student.behaviors.SteeringMixer;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static pack.engine.Vector2.*;

public class Setup {
    //retorna as cenas do laboratório e da caçada, para o menu principal
    public List<Supplier<Scene>> createScenes() {
        return List.of(this::laboratorio, this::cacada);
    }

    //cena 1(1 agente de teste com todos os comportamentos)
    private Scene laboratorio() {
        final var map = TestMaps.laboratorio();
        final var path = map.getPath(TestMaps.LAB_PATH);
        final var agent = new LabAgent(path, vec2(-420, 0));

        return new Scene("Laboratório",
            "Cada comportamento isolado e combinado num único agente, escolhido pelo teclado.", map)
            .add(agent)
            .tune(Tuning.LAB)
            .underlay(g -> {
                if (Debug.enabled) path.draw(g, PathFollowing.COLOR);
            })
            .onKey(code -> {
                if (code == KeyEvent.VK_C) SteeringMixer.toggleMode();
                else agent.onKey(code);
            })
            .status(agent::statusLines)
            .help(
                "1 Seek · 2 Flee · 3 Arrive · 4 Wander · 5 Path Following",
                "A liga/desliga Obstacle Avoidance · W soma Wander",
                "C alterna a mistura: prioridade / soma simples",
                "mouse = alvo do Seek e do Flee · clique = alvo do Arrive");
    }

    //cena 2(caçada da matilha de raptors)
    private Scene cacada() {
        final var map = TestMaps.arena();
        final var rnd = new Random();
        final var pack = new Pack(map, TestMaps.DEN_CENTER, TestMaps.DEN_RADIUS);

        final String[] names = {"Kira", "Rex", "Vex", "Nox"};
        final int alphaIndex = rnd.nextInt(names.length);
        final var raptors = new ArrayList<Raptor>();
        final var used = new ArrayList<Vector2>();
        for (int i = 0; i < names.length; i++) {
            final var p = spawnInDen(map, rnd, used);
            used.add(p);
            final var r = new Raptor(names[i], p, i == alphaIndex, pack, rnd.nextLong());
            raptors.add(r);
            pack.addMember(r);
        }

        final var route = map.getPath(TestMaps.PREY_PATH);
        final var prey = new Prey(route, route.pointAt(0), route.tangentAt(0).getAngle(), rnd.nextLong());
        pack.setPrey(prey);

        return new Scene("Caçada da matilha",
            "Raptors com Alfa, chamado em cadeia, cerco e botes alternados contra uma presa que patrulha e foge.", map)
            .add(raptors)
            .add(prey)
            .select(pack.getAlpha())
            .onUpdate(pack::update)
            .underlay(g -> {
                if (Debug.enabled) {
                    route.draw(g, new Color(110, 165, 255));
                    pack.drawTerritory(g);
                }
            })
            .overlay(pack::drawOverlay)
            .tune(Tuning.RAPTOR_HUNT, Tuning.PREY, Tuning.RAPTOR_PATROL)
            .onKey(code -> {
                if (code == KeyEvent.VK_C) SteeringMixer.toggleMode();
                if (code == KeyEvent.VK_G) Tuning.preyImmortal = !Tuning.preyImmortal;
            })
            .status(pack::statusLines)
            .help(
                "G presa imortal · C alterna a mistura (prioridade / soma)",
                "clique num raptor ou na presa para ver as forças dele");
    }

    private static Vector2 spawnInDen(pack.engine.GameMap map, Random rnd, List<Vector2> used) {
        Vector2 p = TestMaps.DEN_CENTER.clone();
        for (int tries = 0; tries < 60; tries++) {
            final double ang = rnd.nextDouble() * Math.PI * 2;
            final double dist = Math.sqrt(rnd.nextDouble()) * TestMaps.DEN_RADIUS * 0.6;
            p = map.freePoint(add(TestMaps.DEN_CENTER, byAngleSize(ang, dist)), 22);
            boolean ok = true;
            for (var u : used) {
                if (distance(u, p) < 60) {
                    ok = false;
                    break;
                }
            }
            if (ok) return p;
        }
        return p;
    }
}
