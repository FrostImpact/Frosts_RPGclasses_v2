package net.frostimpact.rpgclasses_v2.combat.slash;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Base class for weapon-specific animation handlers
 */
public abstract class WeaponAnimationHandler {
    // Gradient colors - white at the leading edge, transitioning to gold
    protected static final Vector3f WHITE = new Vector3f(1.0f, 1.0f, 1.0f);
    protected static final Vector3f BRIGHT_YELLOW = new Vector3f(1.0f, 0.95f, 0.5f);
    protected static final Vector3f GOLD = new Vector3f(1.0f, 0.85f, 0.35f);
    protected static final Vector3f LIGHT_GOLD = new Vector3f(1.0f, 0.90f, 0.45f);
    protected static final Vector3f DARK_GOLD = new Vector3f(0.9f, 0.75f, 0.25f);

    // Base particle size
    protected static final float PARTICLE_SIZE = 0.7f;

    /**
     * Get the number of combo attacks for this weapon
     */
    public abstract int getMaxComboCount();

    /**
     * Get the animation duration multiplier (1.0 = normal speed)
     */
    public abstract float getAnimationSpeedMultiplier();

    /**
     * Spawn particles for a specific combo hit
     */
    public abstract void spawnComboParticles(ServerLevel level, Vec3 basePos, Vec3 lookDir,
                                            int comboHit, int startParticle, int particleCount,
                                            float animProgress);

    /**
     * Apply easing function - fast start, slow end
     */
    protected float applyEasing(float t) {
        // Quadratic ease-out: starts fast, ends slow
        return 1 - (1 - t) * (1 - t);
    }

    /**
     * Calculate animation progress with easing
     */
    protected float getEasedProgress(float linearProgress) {
        return applyEasing(linearProgress);
    }

    /**
     * Helper to spawn a particle
     */
    protected void spawnParticle(ServerLevel level, Vec3 pos, Vector3f color, float alpha) {
        float size = PARTICLE_SIZE * alpha;
        DustParticleOptions dustOptions = new DustParticleOptions(color, size);
        level.sendParticles(dustOptions, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    /**
     * Get standard directional vectors
     */
    protected Vec3[] getDirectionVectors(Vec3 lookDir) {
        Vec3 forward = new Vec3(lookDir.x, 0, lookDir.z).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = new Vec3(0, 1, 0);
        return new Vec3[]{forward, right, up};
    }

    /**
     * Select color based on layer position - creates a gradient from white (leading edge) to gold (back)
     * @param layer Current layer index (0 = leading edge)
     * @param totalLayers Total number of layers
     * @param isSwingEdge Whether this is the horizontal swing edge (should be white)
     */
    protected Vector3f selectColor(int layer, int totalLayers, boolean isSwingEdge) {
        // The swing edge (horizontal leading edge) should always be white
        if (isSwingEdge) {
            return WHITE;
        }
        
        // Create gradient from white (front) to dark gold (back)
        float progress = (float) layer / (totalLayers - 1);
        
        if (progress < 0.25f) {
            // White to bright yellow
            return WHITE;
        } else if (progress < 0.5f) {
            // Bright yellow
            return BRIGHT_YELLOW;
        } else if (progress < 0.75f) {
            // Light gold
            return LIGHT_GOLD;
        } else {
            // Dark gold
            return DARK_GOLD;
        }
    }
    
    /**
     * Get gradient color based on progress along the slash (0.0 to 1.0)
     * Used for creating smooth gradients where the swing direction is white
     * @param progress 0.0 = start of swing (white), 1.0 = end of swing (gold)
     */
    protected Vector3f getGradientColor(double progress) {
        if (progress < 0.2) {
            // Leading edge - white
            return WHITE;
        } else if (progress < 0.4) {
            // Transition to bright yellow
            return BRIGHT_YELLOW;
        } else if (progress < 0.6) {
            // Light gold
            return LIGHT_GOLD;
        } else if (progress < 0.8) {
            // Gold
            return GOLD;
        } else {
            // Trailing edge - dark gold
            return DARK_GOLD;
        }
    }

    /**
     * Calculate fade alpha based on animation progress
     */
    protected float calculateAlpha(float animProgress) {
        // Fade out in last 20% of animation
        return animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
    }
}
