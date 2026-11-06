package pack.engine;

import java.awt.*;
import java.util.random.RandomGenerator;

import static pack.engine.Vector2.vec2;

//configurações de um agente (usado no construtor)
 
public final class Settings {
    private static final RandomGenerator RND = RandomGenerator.getDefault();

    String name = "agente";
    Color color;
    double mass = 1;
    double maxForce = 300;
    double maxSpeed = 350;
    double radius = 12;
    Vector2 position = vec2();
    double orientation = 0;

    Settings() {
        randomColor();
    }

    public Settings name(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name;
        return this;
    }

    public Settings rgb(int rgb) {
        this.color = new Color(rgb);
        return this;
    }

    public Settings rgb(int r, int g, int b) {
        r = Math.clamp(r, 0, 255);
        g = Math.clamp(g, 0, 255);
        b = Math.clamp(b, 0, 255);
        return color(new Color(r, g, b));
    }

    public Settings hsb(float hue, float saturation, float brightness) {
        return color(Color.getHSBColor(hue, saturation, brightness));
    }

    public Settings color(Color color) {
        return rgb(color.getRGB());
    }

    public Settings randomColor() {
        var r = RND.nextInt(200) + 55;
        var g = RND.nextInt(200) + 55;
        var b = RND.nextInt(200) + 55;
        return color(new Color(r, g, b));
    }

    public Settings mass(double mass) {
        if (mass < 0.1 || mass > 100) {
            throw new IllegalArgumentException("mass must be between 0.1 and 100");
        }
        this.mass = mass;
        return this;
    }

    public Settings maxForce(double maxForce) {
        if (maxForce < 0) {
            throw new IllegalArgumentException("maxForce must be greater than 0");
        }
        this.maxForce = maxForce;
        return this;
    }

    public Settings maxSpeed(double maxSpeed) {
        if (maxSpeed < 0) {
            throw new IllegalArgumentException("maxSpeed must be greater than 0");
        }
        this.maxSpeed = maxSpeed;
        return this;
    }

    // raio do agente (usado para colisão)
    public Settings radius(double radius) {
        if (radius < 2 || radius > 80) {
            throw new IllegalArgumentException("radius must be between 2 and 80");
        }
        this.radius = radius;
        return this;
    }

    public Settings position(Vector2 position) {
        if (position == null) {
            throw new IllegalArgumentException("position must not be null");
        }
        this.position = position.clone();
        return this;
    }

    public Settings position(double x, double y) {
        return position(vec2(x, y));
    }

    public Settings randomPosition() {
        var hw = (int) (GameMap.DEFAULT_WIDTH / 2.0) - 40;
        var hh = (int) (GameMap.DEFAULT_HEIGHT / 2.0) - 40;
        return position(RND.nextInt(-hw, hw), RND.nextInt(-hh, hh));
    }

    public Settings orientation(double radians) {
        this.orientation = radians;
        return this;
    }

    public Settings orientationDegrees(double degrees) {
        return orientation(Math.toRadians(degrees));
    }

    public Settings randomOrientation() {
        return orientation(RND.nextDouble(0, 2 * Math.PI));
    }
}
