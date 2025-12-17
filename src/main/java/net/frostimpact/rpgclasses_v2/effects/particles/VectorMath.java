package net.frostimpact.rpgclasses_v2.effects.particles;

import net.minecraft.world.phys.Vec3;

/**
 * Utility class for vector math operations used in particle effects and animations.
 * Provides common vector operations for creating smooth, dynamic particle patterns.
 */
public class VectorMath {
    
    /**
     * Rotate a vector around the Y axis
     * @param vec The vector to rotate
     * @param angle Angle in radians
     * @return Rotated vector
     */
    public static Vec3 rotateAroundY(Vec3 vec, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(
            vec.x * cos - vec.z * sin,
            vec.y,
            vec.x * sin + vec.z * cos
        );
    }
    
    /**
     * Rotate a vector around the X axis
     * @param vec The vector to rotate
     * @param angle Angle in radians
     * @return Rotated vector
     */
    public static Vec3 rotateAroundX(Vec3 vec, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(
            vec.x,
            vec.y * cos - vec.z * sin,
            vec.y * sin + vec.z * cos
        );
    }
    
    /**
     * Rotate a vector around the Z axis
     * @param vec The vector to rotate
     * @param angle Angle in radians
     * @return Rotated vector
     */
    public static Vec3 rotateAroundZ(Vec3 vec, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(
            vec.x * cos - vec.y * sin,
            vec.x * sin + vec.y * cos,
            vec.z
        );
    }
    
    /**
     * Create a circular pattern of points around a center
     * @param center Center point
     * @param radius Radius of the circle
     * @param numPoints Number of points to generate
     * @param angleOffset Optional angle offset in radians
     * @return Array of points around the circle
     */
    public static Vec3[] createCircle(Vec3 center, double radius, int numPoints, double angleOffset) {
        Vec3[] points = new Vec3[numPoints];
        for (int i = 0; i < numPoints; i++) {
            double angle = (2 * Math.PI * i / numPoints) + angleOffset;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            points[i] = new Vec3(x, center.y, z);
        }
        return points;
    }
    
    /**
     * Create a spiral pattern of points
     * @param center Center point
     * @param startRadius Starting radius
     * @param endRadius Ending radius
     * @param height Total height of the spiral
     * @param numPoints Number of points to generate
     * @param rotations Number of complete rotations
     * @return Array of points forming a spiral
     */
    public static Vec3[] createSpiral(Vec3 center, double startRadius, double endRadius, 
                                      double height, int numPoints, double rotations) {
        Vec3[] points = new Vec3[numPoints];
        for (int i = 0; i < numPoints; i++) {
            double progress = (double) i / (numPoints - 1);
            double angle = rotations * 2 * Math.PI * progress;
            double radius = startRadius + (endRadius - startRadius) * progress;
            double y = center.y + height * progress;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            points[i] = new Vec3(x, y, z);
        }
        return points;
    }
    
    /**
     * Linear interpolation between two vectors
     * @param start Start vector
     * @param end End vector
     * @param t Interpolation factor (0-1)
     * @return Interpolated vector
     */
    public static Vec3 lerp(Vec3 start, Vec3 end, double t) {
        return new Vec3(
            start.x + (end.x - start.x) * t,
            start.y + (end.y - start.y) * t,
            start.z + (end.z - start.z) * t
        );
    }
    
    /**
     * Create a bezier curve between points
     * @param p0 Start point
     * @param p1 First control point
     * @param p2 Second control point
     * @param p3 End point
     * @param t Interpolation factor (0-1)
     * @return Point on the bezier curve
     */
    public static Vec3 bezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        double mt = 1 - t;
        double mt2 = mt * mt;
        double mt3 = mt2 * mt;
        
        return new Vec3(
            mt3 * p0.x + 3 * mt2 * t * p1.x + 3 * mt * t2 * p2.x + t3 * p3.x,
            mt3 * p0.y + 3 * mt2 * t * p1.y + 3 * mt * t2 * p2.y + t3 * p3.y,
            mt3 * p0.z + 3 * mt2 * t * p1.z + 3 * mt * t2 * p2.z + t3 * p3.z
        );
    }
    
    /**
     * Create a sphere of points around a center
     * @param center Center point
     * @param radius Radius of the sphere
     * @param numPoints Number of points to generate
     * @return Array of points on the sphere surface
     */
    public static Vec3[] createSphere(Vec3 center, double radius, int numPoints) {
        Vec3[] points = new Vec3[numPoints];
        double goldenRatio = (1 + Math.sqrt(5)) / 2;
        double angleIncrement = Math.PI * 2 * goldenRatio;
        
        for (int i = 0; i < numPoints; i++) {
            double t = (double) i / numPoints;
            double inclination = Math.acos(1 - 2 * t);
            double azimuth = angleIncrement * i;
            
            double x = center.x + radius * Math.sin(inclination) * Math.cos(azimuth);
            double y = center.y + radius * Math.sin(inclination) * Math.sin(azimuth);
            double z = center.z + radius * Math.cos(inclination);
            
            points[i] = new Vec3(x, y, z);
        }
        return points;
    }
    
    /**
     * Calculate the perpendicular vector in the XZ plane
     * @param vec Input vector
     * @return Perpendicular vector
     */
    public static Vec3 perpendicularXZ(Vec3 vec) {
        return new Vec3(-vec.z, vec.y, vec.x);
    }
    
    /**
     * Create a wave pattern along a direction
     * @param start Start point
     * @param direction Direction vector (will be normalized)
     * @param length Total length of the wave
     * @param amplitude Wave amplitude
     * @param frequency Wave frequency (oscillations per unit length)
     * @param numPoints Number of points to generate
     * @return Array of points forming a wave
     */
    public static Vec3[] createWave(Vec3 start, Vec3 direction, double length, 
                                    double amplitude, double frequency, int numPoints) {
        Vec3[] points = new Vec3[numPoints];
        Vec3 normalizedDir = direction.normalize();
        Vec3 perpendicular = perpendicularXZ(normalizedDir).normalize();
        
        for (int i = 0; i < numPoints; i++) {
            double progress = (double) i / (numPoints - 1);
            double distance = length * progress;
            double offset = amplitude * Math.sin(2 * Math.PI * frequency * progress);
            
            Vec3 basePoint = start.add(normalizedDir.scale(distance));
            points[i] = basePoint.add(perpendicular.scale(offset));
        }
        return points;
    }
}
