package pack.student.behaviors;

import pack.engine.Agent;
import pack.engine.Vector2;

import static pack.engine.Vector2.multiply;
import static pack.engine.Vector2.subtract;

/** Funções comuns aos comportamentos. */
final class Steer {
    //serve para calcular a força de direção pedida por este comportamento
    static final double RESPONSE = 5.0;

    private Steer() { }

    /** Velocidade pedida pelo comportamento, limitada à velocidade máxima do agente (0 = máxima). */
    static double speed(double requested, Agent agent) {
        return requested > 0 ? Math.min(requested, agent.getMaxSpeed()) : agent.getMaxSpeed();
    }

    /** steering = (velocidade desejada - velocidade atual) * ganho  (Reynolds). */
    static Vector2 toward(Agent agent, Vector2 desiredVelocity) {
        return multiply(subtract(desiredVelocity, agent.getVelocity()), RESPONSE);
    }

    /** Freio: leva a velocidade a zero com o mesmo tempo de reação. */
    static Vector2 brake(Agent agent) {
        return multiply(agent.getVelocity(), -RESPONSE);
    }
}
