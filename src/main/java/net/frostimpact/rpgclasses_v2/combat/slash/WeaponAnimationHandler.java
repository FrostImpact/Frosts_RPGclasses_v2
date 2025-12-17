package net.frostimpact.rpgclasses_v2.combat.slash;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Base class for weapon-specific animation handlers
 */
public abstract class WeaponAnimationHandler {
    // Bright yellow/gold colors
    protected static final Vector3f BRIGHT_YELLOW = new Vector3f(1.0f, 0.95f, 0.5f);
    protected static final Vector3f GOLD = new Vector3f(1.0f, 0.85f, 0.35f);
    protected static final Vector3f LIGHT_GOLD = new Vector3f(1.0f, 0.90f, 0.45f);

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
     * Select color based on layer position
     */
    protected Vector3f selectColor(int layer, int totalLayers, boolean isEdge) {
        if (isEdge || layer == 0) {
            return BRIGHT_YELLOW;
        } else if (layer >= totalLayers - 1) {
            return GOLD;
        } else {
            return LIGHT_GOLD;
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
