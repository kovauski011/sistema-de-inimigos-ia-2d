package pack.engine;

import java.awt.Graphics2D;

// obstaculo 
public interface Obstacle {
    //raycast que reconhece o primeiro obstaculo
    RayHit raycast(Vector2 origin, Vector2 dir, double maxDist, double inflate);

    // True se raio r em p encosta no obstáculo
    Contact penetration(Vector2 p, double radius);

    void draw(Graphics2D g);

    // contato do agente com o obstaculo
    record Contact(Vector2 normal, double depth) { }
}
