package pack.engine;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.function.Consumer;

import static pack.engine.Vector2.*;

// para agentes autônomos 
public abstract class Agent implements Cloneable {
    private final String name;
    private final double mass;
    private final double maxForce;
    private final double maxSpeed;
    private Color color;
    private double radius;
    private Vector2 position;
    private Vector2 velocity;
    private Vector2 heading;

    //ultimo steering calculado (para debug / HUD)
    private Vector2 lastSteering = vec2();
    //ponto que a cabeça está olhando
    private Vector2 lookTarget = null;
    //velocidade angular da cabeça
    private double turnRate = 10.0;

    //quantas vezes encostou em algo
    private int contacts = 0;
    private boolean touching = false;

    public Agent(Consumer<Settings> settings) {
        var cs = new Settings();
        settings.accept(cs);
        this.name = cs.name;
        this.color = cs.color;
        this.position = cs.position.clone();
        this.heading = byAngle(cs.orientation);
        this.velocity = vec2();
        this.mass = cs.mass;
        this.maxForce = cs.maxForce;
        this.maxSpeed = cs.maxSpeed;
        this.radius = cs.radius;
    }

    public String getName() { return name; }
    public Vector2 getPosition() { return position.clone(); }
    public Vector2 getVelocity() { return velocity.clone(); }
    public double getSpeed() { return velocity.size(); }

    //vetor para onde a cabeca esta olhando
    public Vector2 getDirection() { return heading.clone(); }

    //vetor para onde o agente esta se movendo
    public Vector2 getMoveDirection() {
        return velocity.size() > 5 ? normalize(velocity) : heading.clone();
    }

    public double getMass() { return mass; }
    public double getMaxForce() { return maxForce; }
    public double getMaxSpeed() { return maxSpeed; }
    public double getRadius() { return radius; }
    public Color getColor() { return color; }
    public Vector2 getLastSteering() { return lastSteering.clone(); }
    public int getContacts() { return contacts; }

    protected final void setColor(Color color) { this.color = color; }
    protected final void setRadius(double radius) { this.radius = radius; }
    protected final void setTurnRate(double turnRate) { this.turnRate = turnRate; }

    // faz o agente olhar para um ponto específico
    protected final void lookAt(Vector2 point) { this.lookTarget = point == null ? null : point.clone(); }

    //volta a olhar pra onde anda
    protected final void clearLook() { this.lookTarget = null; }

    //move o agente para uma posição específica
    protected final void teleport(Vector2 newPosition) {
        this.position = newPosition.clone();
        this.velocity = vec2();
        this.touching = false;
    }

    //calcula a força de direção 
    public abstract Vector2 calculateSteering(World world);//"world" da referencia a tempo, mouse, vizinhos, mapa e raycasts

    // debug/HUD

    //desenho do corpo
    protected void drawBody(Graphics2D g) {
        Sprites.arrowAgent(g, color, radius);
    }

    //desenho de debug (setas, labels, etc)
    protected void drawDebug(Graphics2D g) { }

    // Cor do anel de estado desenhado
    protected Color getRingColor() { return null; }

    //texto em cima do agente
    protected String getLabel() { return name; }

    //nome do estado atual
    public String getStateName() { return null; }

    //força de cada comportamento (para debug / HUD)
    public List<ForceReport> getForceReport() { return List.of(); }

    //fisica 
    void update(World world) {
        final double dt = world.getSecs();
        var steeringForce = calculateSteering(world);
        if (steeringForce == null || !steeringForce.isFinite()) {
            steeringForce = vec2();
        }

        lastSteering = truncate(steeringForce, maxForce);

        final var impulse = multiply(lastSteering, dt);// F * dt
        final var acceleration = divide(impulse, mass);// (F*dt)/m
        velocity = truncate(add(velocity, acceleration), maxSpeed);
        position = add(position, multiply(velocity, dt));

        //diminui a velocidade se encostar em algo e aumenta a velocidade contraria a parede
        final boolean hit = world.getMap().resolveCollisions(this);
        if (hit && !touching) contacts++;
        touching = hit;

        updateHeading(dt);
    }

    void setPhysics(Vector2 newPosition, Vector2 newVelocity) {
        this.position = newPosition;
        this.velocity = newVelocity;
    }

    private void updateHeading(double dt) {
        Vector2 desired = null;
        if (lookTarget != null) {
            var d = subtract(lookTarget, position);
            if (!d.isZero()) desired = d.normalize();
        } else if (velocity.size() > 6) {
            desired = normalize(velocity);
        }
        if (desired == null) return;
        final double current = heading.getAngle();
        final double diff = wrapAngle(desired.getAngle() - current);
        final double k = 1.0 - Math.exp(-turnRate * dt);
        heading = byAngle(current + diff * k);
    }

    private static double wrapAngle(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }

    //desenho
    void draw(Graphics2D g, boolean debug, boolean selected) {
        if (debug) {
            final var gd = (Graphics2D) g.create();
            drawDebug(gd);
            gd.dispose();
        }

        //anelno chão 
        final var ring = getRingColor();
        if (ring != null) {
            final var gr = (Graphics2D) g.create();
            gr.setColor(Draw.alpha(ring, 0.85f));
            gr.setStroke(new BasicStroke(2.2f));
            final double rr = radius * 1.55;
            gr.draw(new Ellipse2D.Double(position.x - rr, position.y - rr, rr * 2, rr * 2));
            gr.dispose();
        }

        //corpo rotacionado na direção da cabeça
        final var g2 = (Graphics2D) g.create();
        g2.translate(position.x, position.y);
        g2.rotate(heading.getAngle());
        drawBody(g2);
        g2.dispose();

        if (selected) {
            Draw.dashedCircle(g, position, radius * 2.3, new Color(255, 255, 255, 200), 1.5f);
        }

        if (debug) {
            drawDebugArrows(g);   //rotulos
        }
    }

    //Setas de debug 
    private void drawDebugArrows(Graphics2D g) {
        final var origin = add(position, multiply(heading, radius * 0.8));
        Draw.arrow(g, origin, velocity, 0.30, new Color(40, 110, 255), 0.75f, 2.5f);
        Draw.arrow(g, origin, lastSteering, 0.12, new Color(255, 69, 0), 0.60f, 2.5f);
    }

    @Override
    public Agent clone() {
        try {
            final var other = (Agent) super.clone();
            other.position = position.clone();
            other.velocity = velocity.clone();
            other.heading = heading.clone();
            return other;
        } catch (CloneNotSupportedException ignored) {
            return null;
        }
    }

    public static double distance(Agent a1, Agent a2) {
        return Vector2.distance(a1.position, a2.position);
    }
}
