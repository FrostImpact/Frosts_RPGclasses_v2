package net.frostimpact.rpgclasses_v2.rpg.stats.combat;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Renders slash particle animations
 */
public class SlashRenderer {
    private static final Random RANDOM = new Random();
    
    public enum SlashType {
        DIAGONAL_LEFT,    // Hit 1: ↘ top-left to bottom-right
        DIAGONAL_RIGHT,   // Hit 2: ↙ top-right to bottom-left
        HORIZONTAL,       // Hit 3: → wide horizontal sweep
        OVERHEAD          // Hit 4: ↓ big downward arc
    }
    
    /**
     * Spawn slash particle arc based on combo hit number
     */
    public static void spawnSlashParticles(ServerLevel level, Player player, int comboHit) {
        SlashType slashType;
        switch (comboHit) {
            case 1 -> slashType = SlashType.DIAGONAL_LEFT;
            case 2 -> slashType = SlashType.DIAGONAL_RIGHT;
            case 3 -> slashType = SlashType.HORIZONTAL;
            case 4 -> slashType = SlashType.OVERHEAD;
            default -> slashType = SlashType.DIAGONAL_LEFT;
        }
        
        spawnSlashArc(level, player, slashType);
    }
    
    /**
     * Spawn an arc of particles based on slash type
     */
    private static void spawnSlashArc(ServerLevel level, Player player, SlashType slashType) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.getEyePosition();
        
        // Get a random pale color
        int[] color = CombatConfig.PALE_COLORS[RANDOM.nextInt(CombatConfig.PALE_COLORS.length)];
        Vector3f colorVec = new Vector3f(color[0] / 255f, color[1] / 255f, color[2] / 255f);
        DustParticleOptions dustOptions = new DustParticleOptions(colorVec, 1.0f);
        
        int particleCount = CombatConfig.PARTICLES_PER_ARC;
        
        for (int i = 0; i < particleCount; i++) {
            double progress = (double) i / particleCount;
            Vec3 particlePos = calculateParticlePosition(playerPos, lookVec, slashType, progress);
            
            // Add some random variation
            double offsetX = (RANDOM.nextDouble() - 0.5) * 0.1;
            double offsetY = (RANDOM.nextDouble() - 0.5) * 0.1;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 0.1;
            
            level.sendParticles(
                dustOptions,
                particlePos.x + offsetX,
                particlePos.y + offsetY,
                particlePos.z + offsetZ,
                1, // count
                0, 0, 0, // delta
                CombatConfig.PARTICLE_SPEED
            );
            
            // Add some crit particles for extra impact
            if (i % 5 == 0) {
                level.sendParticles(
                    ParticleTypes.CRIT,
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    1,
                    0, 0, 0,
                    0.02
                );
            }
        }
    }
    
    /**
     * Calculate particle position based on slash type and progress along arc
     */
    private static Vec3 calculateParticlePosition(Vec3 origin, Vec3 lookVec, SlashType slashType, double progress) {
        // Calculate perpendicular vectors for positioning
        Vec3 right = lookVec.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(lookVec).normalize();
        
        double distance = 1.5 + progress * 1.0; // Arc extends 1.5 to 2.5 blocks forward
        double arcCurve = Math.sin(progress * Math.PI) * 0.5; // Curved arc
        
        Vec3 basePos = origin.add(lookVec.scale(distance));
        
        return switch (slashType) {
            case DIAGONAL_LEFT -> {
                // Top-left to bottom-right: ↘
                double horizontal = -0.8 + progress * 1.6; // -0.8 to 0.8
                double vertical = 0.5 - progress * 1.0;    // 0.5 to -0.5
                yield basePos.add(right.scale(horizontal)).add(up.scale(vertical + arcCurve));
            }
            case DIAGONAL_RIGHT -> {
                // Top-right to bottom-left: ↙
                double horizontal = 0.8 - progress * 1.6;  // 0.8 to -0.8
                double vertical = 0.5 - progress * 1.0;    // 0.5 to -0.5
                yield basePos.add(right.scale(horizontal)).add(up.scale(vertical + arcCurve));
            }
            case HORIZONTAL -> {
                // Horizontal sweep: →
                double horizontal = -1.0 + progress * 2.0; // -1.0 to 1.0 (wider sweep)
                double vertical = arcCurve * 0.3;          // Slight curve
                yield basePos.add(right.scale(horizontal)).add(up.scale(vertical));
            }
            case OVERHEAD -> {
                // Overhead downward arc: ↓
                double angle = progress * Math.PI;        // 0 to PI (half circle)
                double horizontal = Math.sin(angle) * 0.4; // Slight horizontal variation
                double vertical = 0.8 - progress * 1.4;    // 0.8 to -0.6 (big downward arc)
                yield basePos.add(right.scale(horizontal)).add(up.scale(vertical));
            }
        };
    }
}
