package pack.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;

//representa um vetor 2D (x,y) com operações vetoriais
public final class Vector2 implements Cloneable {
    public double x;
    public double y;

    //cria um vetor (0,0)
    public Vector2() {
        this(0, 0);
    }

    //cria um vetor (x,y)
    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    //chama o construtor
    public static Vector2 vec2() {
        return new Vector2();
    }

    //chama o construtor para criar um vetor (x,y)
    public static Vector2 vec2(double x, double y) {
        return new Vector2(x, y);
    }

    //cria um vetor a partir de um ângulo (em radianos)
    public static Vector2 byAngle(double angle) {
        return new Vector2(Math.cos(angle), Math.sin(angle));
    }

    //cria um vetor a partir de um angulo
    public static Vector2 byAngleSize(double angle, double size) {
        return Vector2.byAngle(angle).multiply(size);
    }

    //normaliza o vetor
    public static Vector2 normalize(Vector2 vector) {
        return vector.clone().normalize();
    }

    //nega o vetor
    public static Vector2 negate(Vector2 vector) {
        return vector.clone().negate();
    }

    //adiciona vetores
    public static Vector2 add(Vector2 v1, Vector2... vs) {
        var result = v1.clone();
        for (var v : vs) result.add(v);
        return result;
    }

    //subtrai vetores
    public static Vector2 subtract(Vector2 v1, Vector2... vs) {
        var result = v1.clone();
        for (var v : vs) result.subtract(v);
        return result;
    }

    //soma vetores
    public static Vector2 perp(Vector2 v1) {
        return new Vector2(-v1.y, v1.x);
    }

    //calcula o produto vetorial
    public static double perpDot(Vector2 v1, Vector2 v2) {
        return perp(v1).dot(v2);
    }

    
    //Produto escalar entre dois vetores (versão estática).
    public static double dot(Vector2 v1, Vector2 v2) {
        return v1.x * v2.x + v1.y * v2.y;
    }

    /**
     * Interpolação linear: a + (b - a) * t.
     */
    public static Vector2 lerp(Vector2 a, Vector2 b, double t) {
        return new Vector2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
    }

    /**
     * Ângulo (em radianos, entre 0 e PI) entre dois vetores.
     */
    public static double angleBetween(Vector2 a, Vector2 b) {
        if (isZero(a) || isZero(b)) return 0;
        var c = dot(a, b) / (a.size() * b.size());
        return Math.acos(Math.max(-1, Math.min(1, c)));
    }

    /**
     * Multiplies the vector by the given scalar
     *
     * @param vector The vector to multiply
     * @param scalar The scalar
     * @return The multiplication result.
     * @see #multiply(double)
     */
    public static Vector2 multiply(Vector2 vector, double scalar) {
        return vector.clone().multiply(scalar);
    }

    /**
     * Elevate each vector component by a given potency.
     *
     * @param vector  Vector to elevate
     * @param potency A scalar factor
     * @return A new vector, with the result.
     */
    public static Vector2 pow(Vector2 vector, double potency) {
        return vector.clone().pow(potency);
    }

    /**
     * Divides the vector by the given scalar.
     *
     * @param vector The vector to divide
     * @param scalar The scalar.
     * @return The division result.
     * @see #divide(double)
     */
    public static Vector2 divide(Vector2 vector, double scalar) {
        return vector.clone().divide(scalar);
    }

    /**
     * Returns a rotated copy of the given vector.
     *
     * @param vector  The vector to rotate
     * @param radians Angle to rate
     * @return The rotated vector
     */
    public static Vector2 rotate(Vector2 vector, double radians) {
        return vector.clone().rotate(radians);
    }

    /**
     * @return The distance between the two vectors
     */
    public static double distanceSqr(Vector2 v1, Vector2 v2) {
        return subtract(v1, v2).sizeSqr();
    }

    public static double distance(Vector2 v1, Vector2 v2) {
        return subtract(v1, v2).size();
    }

    /**
     * Truncates the vector to the given size.
     *
     * @param vector Vector to be truncated
     * @param size   The maximum vector size
     * @return The truncated vector
     */
    public static Vector2 truncate(Vector2 vector, double size) {
        if (vector.sizeSqr() <= size * size) {
            return vector.clone();
        }
        return normalize(vector).multiply(size);
    }

    /**
     * Reflects the vector over the normal
     *
     * @param ray    ray vector
     * @param normal normal
     * @return The reflected vector
     */
    public static Vector2 reflex(Vector2 ray, Vector2 normal) {
        return subtract(ray, multiply(normal, 2 * ray.dot(normal)));
    }

    /**
     * Returns a resized copy of v.
     *
     * @param v       The vector to resize.
     * @param newSize The new size
     * @return The resized copy of v.
     */
    public static Vector2 resize(Vector2 v, double newSize) {
        return v.clone().resize(newSize);
    }

    /**
     * Returns true if the vector is zero or null.
     *
     * @param v The vector to test
     * @return True if zero or null.
     */
    public static boolean isZero(Vector2 v) {
        return v == null || v.isZero();
    }

    private static long q8(double v) {
        if (isNaN(v)) return 0L;
        if (isInfinite(v)) {
            if (v > 0) return Long.MAX_VALUE;
            return Long.MIN_VALUE;
        }
        final var scale = 8;
        final var bd = BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP);
        return bd.movePointRight(scale).longValueExact();
    }

    /**
     * Changes all vector components
     *
     * @param x New x value
     * @param y New y value
     * @return This vector.
     */
    public Vector2 set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * @return the size of this vector squared.
     */
    public double sizeSqr() {
        return x * x + y * y;
    }

    /**
     * @return the size of this vector
     */
    public double size() {
        return Math.sqrt(sizeSqr());
    }

    /**
     * Changes this Vector2 size. Equivalent to v.normalize().multiply(size)
     *
     * @param newSize The new size.
     * @return The resized vector.
     */
    public Vector2 resize(double newSize) {
        return this.normalize().multiply(newSize);
    }

    /**
     * @return Turns this vector into a unitary vector.
     * @see Vector2#normalize(Vector2)
     */
    public Vector2 normalize() {
        return isZero() ? this : divide(size());
    }

    /**
     * Adds another vector to this vector.
     *
     * @param other The vector to add.
     * @return This vector after addition.*
     */
    public Vector2 add(Vector2 other) {
        x += other.x;
        y += other.y;
        return this;
    }

    /**
     * Negates this vector, reversing it's side.
     *
     * @return This vector after negation.
     * @see Vector2#negate(Vector2)
     */
    public Vector2 negate() {
        return set(-x, -y);
    }

    /**
     * Subtracts the other vector from this one.
     *
     * @param other The vector to subtract to.
     * @return This vector after subtraction.
     */
    public Vector2 subtract(Vector2 other) {
        x -= other.x;
        y -= other.y;
        return this;
    }

    /**
     * Multiplies this vector by the given scalar.
     *
     * @param scalar The scalar.
     * @return This vector after multiplication.
     * @see Vector2#multiply(Vector2, double)
     */
    public Vector2 multiply(double scalar) {
        return set(x * scalar, y * scalar);
    }

    /**
     * Elevate each component by the given potency.
     *
     * @param potency The potency to elevate to.
     * @return The vector after the operation.
     */
    public Vector2 pow(double potency) {
        return set(Math.pow(x, potency), Math.pow(y, potency));
    }

    /**
     * Divides this vector by the given scalar.
     *
     * @param scalar The scalar.
     * @return This vector divided.
     * @see Vector2#divide(Vector2, double)
     */
    public Vector2 divide(double scalar) {
        return multiply(1.0 / scalar);
    }

    /**
     * Calculates the dot product between this vector an the given one.
     *
     * @param other A vector.
     * @return The dot product.
     */
    public double dot(Vector2 other) {
        return x * other.x +
                y * other.y;
    }

    /**
     * Rotate the vector
     *
     * @param radians Angle to rotate
     * @return This vector, rotated
     */
    public Vector2 rotate(double radians) {
        var s = Math.sin(radians);
        var c = Math.cos(radians);

        var newX = x * c - y * s;
        var newY = x * s + y * c;

        x = newX;
        y = newY;
        return this;
    }

    /**
     * @return True if this is a unitary (normal) vector.
     */
    public boolean isUnit() {
        return Math.abs(sizeSqr() - 1.0) < 1e-9;
    }

    /**
     * @return True if this is the zero vector.
     */
    public boolean isZero() {
        return sizeSqr() < 1e-12;
    }

    /**
     * @return True se os dois componentes são números finitos (sem NaN/Infinity).
     */
    public boolean isFinite() {
        return Double.isFinite(x) && Double.isFinite(y);
    }

    /**
     * @return This Vector angle around
     */
    public double getAngle() {
        return Math.atan2(y, x);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || other.getClass() != Vector2.class) return false;
        var v = (Vector2) other;
        return q8(x) == q8(v.x) && q8(y) == q8(v.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(q8(x), q8(y));
    }

    @Override
    public Vector2 clone() {
        try {
            return (Vector2) super.clone();
        } catch (CloneNotSupportedException e) {
            return new Vector2(this.x, this.y);
        }
    }

    @Override
    public String toString() {
        return String.format("v(%.2f, %.2f)", x, y);
    }
}
