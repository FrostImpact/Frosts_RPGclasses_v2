package net.frostimpact.rpgclasses_v2.entity.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Example custom enemy demonstrating the enemy system.
 * 
 * SHADOW WRAITH
 * An ethereal creature that lurks in dark places.
 * Uses an ambush AI preset with a unique ability: Shadow Dash.
 * 
 * Stats:
 * - 80 HP
 * - 12 Damage
 * - 5 Defense
 * - Fast movement (1.3x)
 * - 50% knockback resistance
 * 
 * Ability: Shadow Dash
 * - Teleports behind the target and deals bonus damage
 * - 5 second cooldown
 * - 15 block range
 */
public class ExampleEnemies {
    
    /**
     * SHADOW WRAITH - An ethereal ambush predator
     */
    public static final CustomEnemy SHADOW_WRAITH = new CustomEnemy.Builder(
            "shadow_wraith",
            "Shadow Wraith"
        )
        .description("An ethereal creature that emerges from the shadows to strike")
        .maxHealth(80)
        .damage(12)
        .defense(5)
        .moveSpeed(1.3)
        .attackSpeed(1.2)
        .knockbackResistance(0.5)
        .experienceReward(50)
        .isBoss(false)
        .aiPreset(AIPreset.AMBUSH)
        .ability(new ShadowDashAbility())
        .scale(1.2f)
        .glowColor(0x4B0082) // Dark purple glow
        .build();
    
    /**
     * Shadow Dash ability implementation
     * Teleports behind the target and deals bonus damage
     */
    public static class ShadowDashAbility implements EnemyAbility {
        
        @Override
        public String getId() {
            return "shadow_dash";
        }
        
        @Override
        public String getName() {
            return "Shadow Dash";
        }
        
        @Override
        public String getDescription() {
            return "Teleports behind the target and strikes";
        }
        
        @Override
        public int getCooldownTicks() {
            return 100; // 5 seconds
        }
        
        @Override
        public double getRange() {
            return 15.0;
        }
        
        @Override
        public boolean execute(LivingEntity entity, LivingEntity target) {
            if (entity.level().isClientSide || target == null) {
                return false;
            }
            
            ServerLevel level = (ServerLevel) entity.level();
            
            // Calculate position behind target
            Vec3 targetLook = target.getLookAngle();
            Vec3 behindTarget = target.position().subtract(targetLook.scale(2.0));
            
            // Spawn particles at origin position
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                entity.getX(), entity.getY() + 1, entity.getZ(),
                20, 0.5, 0.5, 0.5, 0.1);
            
            // Play teleport sound
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.2F);
            
            // Teleport to behind target
            entity.teleportTo(behindTarget.x, target.getY(), behindTarget.z);
            
            // Spawn particles at new position
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                entity.getX(), entity.getY() + 1, entity.getZ(),
                20, 0.5, 0.5, 0.5, 0.1);
            
            // Apply blindness to target briefly
            if (target instanceof LivingEntity livingTarget) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0)); // 2 seconds
            }
            
            // Deal bonus damage
            float bonusDamage = 8.0f;
            target.hurt(level.damageSources().mobAttack(entity), bonusDamage);
            
            // Visual effect on target
            level.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                15, 0.3, 0.3, 0.3, 0.1);
            
            return true;
        }
    }
    
    /**
     * Example of a simple passive ability for reference
     * (Not used by Shadow Wraith, but shows how to create passive-like abilities)
     */
    public static class ShadowAuraAbility implements EnemyAbility {
        
        @Override
        public String getId() {
            return "shadow_aura";
        }
        
        @Override
        public String getName() {
            return "Shadow Aura";
        }
        
        @Override
        public String getDescription() {
            return "Weakens nearby enemies with darkness";
        }
        
        @Override
        public int getCooldownTicks() {
            return 200; // 10 seconds
        }
        
        @Override
        public double getRange() {
            return 8.0;
        }
        
        @Override
        public boolean requiresTarget() {
            return false; // AoE ability
        }
        
        @Override
        public boolean execute(LivingEntity entity, LivingEntity target) {
            if (entity.level().isClientSide) {
                return false;
            }
            
            ServerLevel level = (ServerLevel) entity.level();
            
            // Find all players within range
            AABB aoeBox = entity.getBoundingBox().inflate(getRange());
            List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
                LivingEntity.class,
                aoeBox,
                e -> e != entity && e.isAlive()
            );
            
            // Apply effects to nearby entities
            for (LivingEntity nearby : nearbyEntities) {
                nearby.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0)); // 5 seconds
                nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0)); // 3 seconds
            }
            
            // Visual effect
            for (int i = 0; i < 50; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * getRange() * 2;
                double offsetY = level.random.nextDouble() * 2;
                double offsetZ = (level.random.nextDouble() - 0.5) * getRange() * 2;
                
                level.sendParticles(ParticleTypes.SMOKE,
                    entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ,
                    1, 0, 0, 0, 0.02);
            }
            
            // Play sound
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.WARDEN_TENDRIL_CLICKS, SoundSource.HOSTILE, 1.0F, 0.5F);
            
            return true;
        }
    }
}
