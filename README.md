[README.md](https://github.com/user-attachments/files/34495590/README.md)
# Pack AI 2D: Steering Behaviors em Java

Versão **2D e simplificada** do projeto *Sistema de Inimigos – Pack AI v2* (Godot), feita no mesmo formato do **SBCarsEngine**: um motor pronto em `pack.engine` e o código "do aluno" em `pack.student`, onde cada agente implementa `calculateSteering(World world)`.

Uma matilha de raptors (com um **Alfa**, **chamado em cadeia**, **cerco** e **botes alternados**) caça uma **presa autônoma** que patrulha uma rota e foge. Tudo é feito com steering behaviors combinados por pesos, e os pesos podem ser ajustados com a simulação rodando.

### Checklist do trabalho

| Pedido | Onde está |
|---|---|
| 4 comportamentos simples: Seek, Flee, Arrive, Wander | `student/behaviors/`; isolados no Laboratório (teclas 1-4) |
| 2 avançados: Obstacle Avoidance e Path Following | `ObstacleAvoidance.java`, `PathFollowing.java`; Laboratório (A e 5) |
| Pequeno mapa de testes | `TestMaps.laboratorio()` (pilar de propósito em cima do caminho) e `TestMaps.arena()` |
| Pelo menos 2 combinações, com justificativa | Raptor caçando (comb. 1) e Presa fugindo (comb. 2), mais Raptor patrulhando (comb. 3) e as combinações por input do Laboratório; ver "As combinações e por que foram escolhidas" |
| Ajuste dos pesos | painel ao vivo (setas) + `Tuning.java` + medições em "Ajuste dos pesos" |
| Equilíbrio, fluidez, justificativa | modo de mistura com prioridade (tecla C compara), painel de forças, tabela de experimentos |

---

## Como rodar

**Requisitos:** JDK **21** ou mais novo (o código usa `Math.clamp`, igual ao SBCarsEngine) e, no VS Code, o *Extension Pack for Java*.

**No VS Code**
1. `File > Open Folder…` e escolha esta pasta (a que tem `src/` e `.vscode/`).
2. Abra `src/pack/engine/Window.java` e clique em **Run** acima do `main`, ou aperte **F5** (configuração "Pack AI 2D").

**Pelo terminal**
```bash
# Linux / macOS
javac -d bin $(find src -name "*.java")
java -cp bin pack.engine.Window
```
```powershell
# Windows (PowerShell)
javac -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp bin pack.engine.Window
```

---

## Estrutura (comparando com o SBCarsEngine)

| SBCarsEngine | Este projeto | Papel |
|---|---|---|
| `cars.engine.Car` | `pack.engine.Agent` | agente: física (mesma integração `F·dt/m`), desenho, setas de debug |
| `cars.engine.World` | `pack.engine.World` | o que o agente vê no quadro: tempo, mouse, clique, vizinhos, **mapa e raycast** |
| `cars.engine.Settings` | `pack.engine.Settings` | builder de configuração (`.color().position().maxSpeed()…` + `.radius()` e `.name()`) |
| `cars.engine.Vector2` | `pack.engine.Vector2` | a mesma classe, com `dot`, `lerp` e `angleBetween` estáticos |
| `cars.engine.Window` | `pack.engine.Window` + `Simulation` | janela + laço do jogo; a `Simulation` cuida das cenas, teclado e painel |
| `cars.student.Setup` | `pack.student.Setup` | cria as cenas (em vez de uma lista de carros) |
| `cars.student.StudentCar` | `LabAgent`, `Raptor`, `Prey` | os agentes com `calculateSteering` |

```
src/pack/
├─ engine/                    motor (equivalente ao cars.engine)
│  ├─ Agent, World, Settings, Vector2, Window, Simulation
│  ├─ GameMap, Obstacle, CircleObstacle, BoxObstacle, Path, RayHit   mapa, colisão e raycast
│  ├─ Scene, Weight, WeightGroup, ForceReport, HudLine               cenas e painel lateral
│  └─ Draw, Sprites, Debug                                           desenho e debug
└─ student/                   implementação
   ├─ behaviors/              Seek, Flee, Arrive, Wander, ObstacleAvoidance, PathFollowing,
   │                          Pursuit, Evade, Separation e SteeringMixer (combinação)
   ├─ Setup.java              as duas cenas
   ├─ TestMaps.java           mapas de teste
   ├─ Tuning.java             TODOS os pesos, com valores calibrados e explicação
   ├─ LabAgent.java           agente do laboratório (combinações por input)
   ├─ Raptor.java, Pack.java  matilha
   └─ Prey.java               presa
```

O que o motor ganhou em relação ao SBCars: mapa com paredes e obstáculos (o agente encosta e desliza em vez de atravessar), raycast e linha de visão, a "cabeça" do agente pode olhar para um lado e andar para outro, painel de **pesos ajustáveis** e painel de **forças** do agente selecionado.

---

## As duas cenas (TAB alterna)

### Cena 1: Laboratório (mapa de testes)
Um único agente; o comportamento é escolhido pelo teclado. É aqui que cada comportamento aparece **isolado** e que as **combinações por input** são montadas.

| Tecla | Comportamento |
|---|---|
| **1** | Seek: persegue o cursor do mouse |
| **2** | Flee: foge do cursor dentro do raio de pânico (fora dele, freia) |
| **3** | Arrive: vai até o último clique e para suavemente |
| **4** | Wander: passeia ao acaso |
| **5** | Path Following: segue o caminho tracejado |
| **A** | liga/desliga **Obstacle Avoidance** por cima de qualquer um deles |
| **W** | soma um **Wander** leve ao comportamento principal |

O mapa tem pilares, dois muros e um caminho fechado. **Um pilar foi colocado de propósito em cima do caminho**: com A desligado o agente bate nele; com A ligado ele contorna e volta para a rota.

### Cena 2: Caçada da matilha
Arena com ruínas e pilares. Quatro raptors (um Alfa sorteado, maior, com crista vermelha e aro dourado) vivem no território marrom à esquerda. A presa patrulha a rota azul, que atravessa a borda do território.

---

## Controles

| Tecla | Ação |
|---|---|
| **TAB** | troca de cena · **R** reinicia a cena (os pesos ajustados são mantidos) |
| **↑ ↓** | escolhe um peso no painel · **← →** ajusta (Shift = 5×) · **Backspace** restaura o grupo |
| **PgUp / PgDn** (ou **,** e **.**) | troca o grupo de pesos |
| **S** | imprime os pesos atuais no console (para copiar no relatório) |
| **C** | alterna o modo de mistura: **prioridade** × **soma simples** |
| **clique** | seleciona um agente (painel de forças); no laboratório, clicar no chão define o alvo do Arrive |
| **D** · **V** · **L** | debug geral · cones de visão · rótulos |
| **P** / Espaço · **T** | pausa · câmera lenta (0,3×) |
| **G** | presa imortal (cena 2) · **H** esconde a ajuda · **Esc** sai |

---

## Comportamentos implementados

Todos seguem a ideia de Reynolds: `steering = velocidadeDesejada - velocidadeAtual`.

| | Comportamento | Arquivo | Ideia | Onde aparece |
|---|---|---|---|---|
| simples | **Seek** | `Seek.java` | velocidade máxima em direção ao alvo | lab (1), raptor volta ao território, presa corre para a rota de fuga |
| simples | **Flee** | `Flee.java` | velocidade máxima para longe da ameaça, só dentro do raio de pânico; aceita várias ameaças ponderadas pela proximidade | lab (2), recuo do raptor depois da mordida |
| simples | **Arrive** | `Arrive.java` | Seek que desacelera dentro do raio de frenagem: `vel = máx · min(1, d / raio)` | lab (3), raptor indo ao slot do cerco, freios |
| simples | **Wander** | `Wander.java` | alvo num círculo à frente do agente, deslizando por um pequeno sorteio a cada quadro (`jitter·√dt`, independe do FPS) | lab (4), patrulha do raptor, zigue-zague da presa |
| avançado | **Obstacle Avoidance** | `ObstacleAvoidance.java` | 3 "bigodes" (raycasts a 0° e ±35°) com comprimento proporcional à velocidade; a soma das normais vira força **lateral** (desvia) + freio | todos os agentes |
| avançado | **Path Following** | `PathFollowing.java` | prevê a posição em 0,3 s, projeta no caminho e faz Seek num ponto 55-60 px à frente ao longo dele | lab (5), patrulha da presa |
| extra | **Pursuit** | `Pursuit.java` | Seek na posição futura do alvo | bote do raptor |
| extra | **Evade** | `Evade.java` | Flee da posição futura das ameaças | reflexo da presa |
| extra | **Separation** | `Separation.java` | empurra para longe dos vizinhos, mais forte quanto mais perto | matilha |

**Detalhe importante (ganho de resposta).** O motor integra `velocidade += força · dt / massa`, como o SBCars. Usar só `desejada − atual` como força faz o agente levar cerca de **1 s** para corrigir a velocidade: no teste, o Arrive passava 39 px do alvo e voltava. Por isso o erro de velocidade é multiplicado por um ganho de 5 (`Steer.RESPONSE`), o que dá cerca de 0,2 s de tempo de reação; o `maxForce` continua limitando a aceleração. Depois disso o Arrive para a 0,6 px do clique, sem passar do ponto.

---

## Como as forças são combinadas (`SteeringMixer`)

Cada agente monta, para cada estado, uma lista **ordenada** de comportamentos com peso. O modo padrão é a **soma ponderada truncada com prioridade** (Buckland, *Programming Game AI by Example*):

1. Percorre os comportamentos na ordem (o primeiro tem mais prioridade).
2. Cada força é multiplicada pelo peso e somada até gastar o "orçamento" do agente (`maxForce`).
3. Quando o orçamento acaba, os de menor prioridade ficam sem força naquele quadro.

Por que isso importa: o **desvio de obstáculos vem sempre primeiro**. Se o raptor está prestes a bater num pilar, o desvio pega a força que precisar, e um Arrive ou Pursuit forte não consegue empurrá-lo contra a parede. Na soma simples (tecla **C**) tudo é somado e só depois truncado, então uma força grande dilui a de segurança. Medido na caçada: **20 batidas de raptor e 61 da presa** contra **2 e 0** no modo prioridade (tabela abaixo).

O painel **FORÇAS** mostra isso ao vivo: a barra clara é o que o comportamento **pediu**, a barra forte é o que **foi aplicado**; quando a prioridade corta uma força aparece `>`. As setas coloridas no agente selecionado são as mesmas forças, na mesma escala da seta laranja de steering, e somadas dão exatamente ela.

---

## As combinações e por que foram escolhidas

`>` = ordem de prioridade. Os pesos estão em `Tuning.java` e no painel.

### Combinação 1: Raptor caçando (cerco + bote)

| Estado | Combinação | Pesos |
|---|---|---|
| Ronda / Cerco | Obstacle Avoidance > Separation > **Arrive** (slot do anel) | 3,0 · 1,0 · 1,0 |
| Bote | Obstacle Avoidance > Arrive (agacha 0,18 s) → **Pursuit** (presa) | 3,0 · 1 · 1,5 |
| Recuo | Obstacle Avoidance > Separation > **Flee** (0,4 s) → Arrive (slot) | 3,0 · 1,0 · 1,0 · 1,0 |

Por que esta combinação:
- **Arrive, não Seek, para o slot.** O cerco é um anel ao redor da posição *antecipada* da presa (posição + velocidade × 0,35 s). O raptor precisa **parar** no lugar dele e esperar a vez; Seek passaria do ponto e ficaria oscilando.
- **Separation abaixo do desvio e acima do Arrive.** Garante espaço entre os raptors (sem ela ficam sobrepostos **24%** do tempo; com 1,0, **~5%**), mas não impede que cheguem perto da presa.
- **Pursuit com peso 1,5 no bote.** Mira onde a presa *vai* estar. O peso acima de 1 vence a inércia de quem já vinha correndo em outra direção; com 0,5 a primeira mordida demora ~8 s em vez de ~6 s.
- **Arrive antes do Pursuit (agachar).** 0,18 s parado antes de saltar: o bote fica legível e dá uma chance à presa.
- **Flee curto depois da mordida.** O raptor se afasta e volta ao slot, abrindo espaço para o próximo bote. Isso cria o ataque alternado do projeto Godot (no máximo 2 botes ao mesmo tempo, com prioridade para flancos e retaguarda).

### Combinação 2: Presa fugindo

| Estado | Combinação | Pesos |
|---|---|---|
| Patrulha | Obstacle Avoidance > **Path Following** > Wander | 3,5 · 1,0 · 0,35 |
| Alerta | Obstacle Avoidance > Arrive (para no lugar, olhando para o som) | 3,5 · 1 |
| Fuga | Obstacle Avoidance > **Evade** (raptors a < 150 px) > **Seek** (rota de fuga) > **Wander** | 3,5 · 1,4 · 1,2 · 0,35 |

Por que esta combinação:
- **Avoidance mais alto que o do raptor (3,5).** A presa corre sem olhar para trás; encurralar-se num pilar é a pior coisa que pode acontecer.
- **Rota de fuga em vez de Flee puro.** O primeiro teste usava Flee/Evade direto: a presa corria para longe das ameaças **até o canto** e ficava presa contra a parede, quase parada, enquanto era mordida. A versão final é um *FleePlanner* simplificado: a cada 0,3 s ela testa **16 direções** com raycast e dá nota para cada uma (afasta das ameaças + espaço livre + chega atrás de uma cobertura − aponta para um predador − parede a menos de 60 px), com histerese para não ziguezaguear. Depois faz **Seek** nesse ponto. Sem isso (peso 0) a velocidade média de fuga cai de **~125 para ~95 px/s** e os abates sobem **50-80%**.
- **Evade só de perto (reflexo).** Com prioridade sobre a rota, reage a um raptor dando o bote a menos de 150 px, olhando para onde ele *vai* estar.
- **Wander baixo (0,35) na fuga.** Deixa a trajetória menos previsível: o giro médio sobe de 0,80 para 1,13 rad/s (+40%). Com 1,5 o giro quase dobra (1,55 rad/s) sem melhorar a sobrevivência; a presa só treme.
- **Path Following + Wander na patrulha.** A rota faz a presa cruzar o território da matilha; o Wander leve evita que ela ande como um trem nos trilhos.

### Combinação 3 (extra): Raptor patrulhando
Obstacle Avoidance (3,0) > Separation (1,5) > Seek ao centro do território (1,2 × *gate*) > Wander (1,0).
O Seek do território só liga a partir de 70% do raio e cresce até 100% na borda (o *gate* do mixer). Dentro do território o raptor vagueia livre e na borda é puxado de volta, sem parede invisível. Aqui a Separation é maior que no cerco porque cada raptor deve cobrir uma área diferente.

### Combinações por input (Laboratório)
No mesmo agente: **A** soma o desvio a qualquer comportamento, **W** soma Wander. Exemplos para mostrar em aula:
- **Seek + Avoidance**: ponha o mouse atrás de um pilar.
- **Path Following + Avoidance**: veja o agente sair da rota para contornar o pilar que está em cima dela e voltar.
- **Seek + Wander**: perseguição menos robótica.

---

## Ajuste dos pesos: processo e resultados

Os pesos foram ajustados **um de cada vez**, medindo com a simulação rodando sem tela: **10 simulações de 5 min (50 min de jogo) por configuração**, todos os outros pesos no padrão. "Batidas" = vezes que um agente encostou fisicamente num obstáculo; "sobreposição" = % do tempo com dois raptors um em cima do outro.

| Configuração | Batidas raptors | Batidas presa | Sobreposição | Vel. média de fuga | Caçadas / abates / fugas |
|---|---|---|---|---|---|
| **Padrão** (prioridade) | **2** | **0** | **4,6%** | **123 px/s** | 67 / 60 / 3 |
| Soma ponderada simples (C) | 20 | 61 | 5,8% | 140 | 61 / 56 / 4 |
| Avoidance = 1,0 | 61 | 15 | 3,5% | 130 | 66 / 61 / 1 |
| Avoidance = 0 | 2883 | 164 | 3,6% | 126 | 72 / 67 / 3 |
| Separation = 0 | 0 | 0 | **24,1%** | 118 | 78 / 66 / 9 |
| Presa sem rota de fuga | 1 | 0 | 8,9% | **94** | 98 / **92** / 5 |
| Pursuit (bote) = 0,5 | 0 | 0 | 5,2% | 127 | 72 / 54 / **14** (1ª mordida 7,9 s × 6,1 s) |

No laboratório (8 min de cada; Wander a 300 px/s, Path Following na velocidade padrão), variando só o peso do desvio:

| Avoidance | 0 | 0,5 | 1,0 | 1,5 | 2,0 | 3,0 |
|---|---|---|---|---|---|---|
| Batidas, Wander | 188 | **993** | 5 | 1 | 0 | 0 |
| Batidas, Path Following | 28 | 28 | 0 | 0 | 0 | 0 |

Com 0,5 o resultado é **pior que sem desvio**: o agente fica "quicando" na parede (encosta, é empurrado, volta), contando várias batidas por segundo. A partir de ~2 não bate mais; o padrão 3,0 dá folga para velocidades maiores.

Problemas encontrados durante o ajuste (e como foram resolvidos):
1. **Arrive passando do ponto:** era o tempo de reação de 1 s da física `F·dt/m`. Resolvido com o ganho de resposta.
2. **Raptor parado encostado num muro inclinado:** o desvio empurrava só pela normal da parede e gastava toda a força, sem sobrar nada para o Arrive deslizar ao longo dela. Resolvido decompondo a força em lateral (desvia) + freio.
3. **Presa morrendo nos cantos:** Flee puro leva para o canto. Resolvido com a rota de fuga.
4. **Presa "se acalmando" com o raptor logo atrás:** o perseguidor ficava no ponto cego dos 220° de visão. Resolvido com percepção de 360° a menos de 130 px (ela ouve quem corre atrás).
5. **Equilíbrio da caçada:** com o Alfa tão rápido quanto a presa, ela nunca escapava. O Alfa ficou 10% mais rápido que os outros (181 px/s), mas abaixo da corrida da presa (190); a matilha desiste depois de 9 s sem conseguir morder. Resultado: a maioria das caçadas termina em abate, mas a presa às vezes escapa. Fôlego, exaustão e desistência deixam o desfecho em aberto.

---

## Do projeto Godot para esta versão 2D

| Pack AI v2 (Godot 3D) | Esta versão |
|---|---|
| NavigationAgent3D + navmesh | só steering: Path Following na patrulha, Avoidance + Arrive no resto |
| Pipeline de 6 etapas a 10 Hz | percepção → máquina de estados → configurar comportamentos → misturar, todo quadro |
| Visão: FOV + raycast de linha de visão | igual (raptor 120°/250 px; presa 220°/300 px, 360° a menos de 130 px) |
| Feelers do raptor (3 raycasts) | `ObstacleAvoidance` com 3 bigodes |
| SoundSystem com oclusão | onda de chamado que se expande a 520 px/s até 270 px, sem oclusão |
| Cadeia: repassa com hop+1, Alfa processa e dá a ordem; mensageiro após 8 s | igual (sem mensageiro físico: após 5 s a ordem sai mesmo assim) |
| Slots em anel na posição antecipada, Alfa na frente, tokens de investida | igual (máx. 2 botes, intervalo 0,55 s, prioridade para flanco e retaguarda) |
| FleePlanner (24 destinos no navmesh, 6 critérios) | 16 direções por raycast, 4 critérios + histerese |
| Fôlego, memória de ameaças, renascer longe dos raptors | igual |
| Estados Patrol / CircleTarget / AlertSignal / CoordinatedAttack | Patrulha / Ronda / Cerco / Bote / Recuo |

---

## Legenda do debug

| Desenho | Significado |
|---|---|
| Anel no chão do raptor | verde patrulha · âmbar ronda · vermelho cerco · vermelho forte bote · laranja recuo; aro dourado = Alfa |
| Anel no chão da presa | azul patrulha · amarelo alerta · laranja fuga · roxo exausta |
| Seta azul / laranja | velocidade / steering total (como no SBCars) |
| Setas coloridas no selecionado | força de cada comportamento (cores do painel) |
| Bigodes cinza / vermelhos | raios do Obstacle Avoidance livres / batendo (com a normal no ponto) |
| Círculo roxo à frente | círculo do Wander e o alvo nele |
| Corredor verde ou azul tracejado | caminho do Path Following; ponto verde/vermelho = previsão dentro/fora do corredor |
| Leque | campo de visão; linha verde = linha de visada livre, vermelha = bloqueada |
| Anéis amarelo → laranja → rosa | ondas do chamado por salto; anel vermelho grosso = rugido (ordem do Alfa) |
| Bolinhas ao redor da presa | 16 direções da rota de fuga (verde boa, vermelha ruim; maiores = escondidas); seta verde = escolhida, seta ciano = vetor de fuga |
| × vermelho / círculo laranja | onde a presa lembra ter visto um raptor / ouviu um chamado |
| Círculos pequenos com linha tracejada | slot de cada raptor no anel do cerco; círculo vermelho grande = raptor com token de bote |
| Barras sob a presa | vida (vermelha) e fôlego (amarela) |
