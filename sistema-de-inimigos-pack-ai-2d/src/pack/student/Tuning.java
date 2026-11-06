package pack.student;

import pack.engine.Weight;
import pack.engine.WeightGroup;

//pesos ajustáveis para o laboratório e a caçada
public final class Tuning {
    private Tuning() { }

    //laboratorio cena1
    public static final Weight LAB_AVOID = new Weight("Obstacle Avoidance", 3.0, 0, 6, 0.25,
        "Peso do desvio (A liga/desliga). Abaixo de 1 o agente bate; com 0,5 fica quicando na parede (pior que 0). A partir de ~2 não bate mais.");
    public static final Weight LAB_PRIMARY = new Weight("Comportamento (1-5)", 1.0, 0, 3, 0.1,
        "Peso do comportamento principal escolhido nas teclas 1 a 5.");
    public static final Weight LAB_WANDER_MIX = new Weight("Wander sobreposto (W)", 0.5, 0, 2, 0.1,
        "Quanto de Wander é somado ao principal quando W está ligado: tira o ar robótico do Seek e do Path Following.");
    public static final Weight LAB_SPEED = new Weight("Velocidade (px/s)", 220, 40, 320, 10,
        "Velocidade pedida pelos comportamentos (o Wander usa 60% dela e o Path Following 75%).");
    public static final Weight LAB_ARRIVE_SLOW = new Weight("Arrive: frenagem (px)", 140, 10, 400, 10,
        "Raio em que o Arrive começa a frear. Pequeno demais para a velocidade = passa do ponto e volta.");
    public static final Weight LAB_FLEE_PANIC = new Weight("Flee: raio de pânico", 220, 40, 500, 10,
        "Distância do mouse em que o Flee começa a agir. Fora dele o agente freia.");
    public static final Weight LAB_WANDER_DIST = new Weight("Wander: distância", 80, 0, 250, 5,
        "Distância do círculo do Wander à frente do agente. Maior = curvas mais suaves.");
    public static final Weight LAB_WANDER_RADIUS = new Weight("Wander: raio", 40, 5, 150, 5,
        "Raio do círculo do Wander. Maior = curvas mais fechadas.");
    public static final Weight LAB_WANDER_JITTER = new Weight("Wander: jitter", 2.0, 0, 8, 0.25,
        "Quanto o alvo escorrega pelo círculo a cada segundo. Maior = muda de ideia mais vezes.");
    public static final Weight LAB_AVOID_LOOK = new Weight("Avoidance: antecipação", 0.45, 0.05, 1.5, 0.05,
        "Segundos de antecipação dos bigodes: comprimento = 30 + antecipação × velocidade.");

    public static final WeightGroup LAB = new WeightGroup("Laboratório",
        LAB_AVOID, LAB_PRIMARY, LAB_WANDER_MIX, LAB_SPEED, LAB_ARRIVE_SLOW, LAB_FLEE_PANIC,
        LAB_WANDER_DIST, LAB_WANDER_RADIUS, LAB_WANDER_JITTER, LAB_AVOID_LOOK);

    //conbinação 1 — Raptor caçando a presa
    public static final Weight R_HUNT_AVOID = new Weight("Obstacle Avoidance", 3.0, 0, 6, 0.25,
        "Prioridade máxima: correndo a 165 px/s o raptor precisa desviar antes de tudo. Com 1,0 as batidas em 50 min de jogo sobem de ~0-2 para ~35-60.");
    public static final Weight R_HUNT_SEPARATION = new Weight("Separation", 1.0, 0, 4, 0.1,
        "Espaço entre raptors. Em 0 eles ficam sobrepostos ~22-24% do tempo; com 1,0 cai para ~5%. Acima disso quase não melhora.");
    public static final Weight R_HUNT_ARRIVE = new Weight("Arrive (slot do cerco)", 1.0, 0, 3, 0.1,
        "Ir até o slot do anel ao redor da presa e frear lá. Arrive e não Seek, porque o cerco precisa PARAR no lugar.");
    public static final Weight R_HUNT_PURSUIT = new Weight("Pursuit (bote)", 1.5, 0, 4, 0.1,
        "Investida mirando a posição futura da presa. Acima de 1 para vencer a inércia da corrida. Com 0,5 a 1ª mordida demora ~8 s em vez de ~6 s.");
    public static final Weight R_HUNT_RECOIL = new Weight("Flee (recuo)", 1.0, 0, 3, 0.1,
        "Afasta da presa logo depois da mordida (0,4 s), antes de voltar ao slot.");

    public static final WeightGroup RAPTOR_HUNT = new WeightGroup("Raptor · Caça (comb. 1)",
        R_HUNT_AVOID, R_HUNT_SEPARATION, R_HUNT_ARRIVE, R_HUNT_PURSUIT, R_HUNT_RECOIL);

    //conbinação 2 — Presa fugindo da matilha
    public static final Weight P_AVOID = new Weight("Obstacle Avoidance", 3.5, 0, 6, 0.25,
        "Maior que o do raptor: a presa foge sem olhar para trás e não pode se encurralar num pilar.");
    public static final Weight P_EVADE = new Weight("Evade (pânico)", 1.4, 0, 4, 0.1,
        "Reflexo: foge da posição FUTURA de raptors a menos de 150 px. Vem antes da rota para reagir a um bote.");
    public static final Weight P_ESCAPE = new Weight("Seek (rota de fuga)", 1.2, 0, 4, 0.1,
        "Corre para a direção planejada (16 raycasts). Em 0 ela foge para os cantos: velocidade média de fuga cai de ~125 para ~95 px/s e os abates sobem 50-80%.");
    public static final Weight P_WANDER_FLEE = new Weight("Wander na fuga", 0.35, 0, 2, 0.05,
        "Zigue-zague: com 0,35 o giro da trajetória na fuga sobe ~40% (menos previsível). Com 1,5 quase dobra e não melhora a sobrevivência: só treme.");
    public static final Weight P_PATH = new Weight("Path Following", 1.0, 0, 3, 0.1,
        "Patrulha calma seguindo a rota (linha azul tracejada).");
    public static final Weight P_WANDER_PATROL = new Weight("Wander na patrulha", 0.35, 0, 2, 0.05,
        "Pequenas variações na rota para não andar como um trem nos trilhos.");

    public static final WeightGroup PREY = new WeightGroup("Presa (comb. 2)",
        P_AVOID, P_EVADE, P_ESCAPE, P_WANDER_FLEE, P_PATH, P_WANDER_PATROL);

    //conbinação 3 — Raptor patrulhando o território
    public static final Weight R_PATROL_AVOID = new Weight("Obstacle Avoidance", 3.0, 0, 6, 0.25,
        "Mesmo critério da caça: segurança primeiro.");
    public static final Weight R_PATROL_SEPARATION = new Weight("Separation", 1.5, 0, 4, 0.1,
        "Patrulhando, cada raptor cobre uma área: mais espaço entre eles do que no cerco.");
    public static final Weight R_PATROL_TERRITORY = new Weight("Seek (território)", 1.2, 0, 4, 0.1,
        "Puxa de volta para o território a partir de 70% do raio (cresce até 100% na borda).");
    public static final Weight R_PATROL_WANDER = new Weight("Wander", 1.0, 0, 3, 0.1,
        "Passeio pelo território.");

    public static final WeightGroup RAPTOR_PATROL = new WeightGroup("Raptor · Patrulha (comb. 3)",
        R_PATROL_AVOID, R_PATROL_SEPARATION, R_PATROL_TERRITORY, R_PATROL_WANDER);

    //g presa imortal
    public static volatile boolean preyImmortal = false;
}
