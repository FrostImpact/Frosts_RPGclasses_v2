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
 * Example custom enemies demonstrating each AI behavior type.
 * Each enemy showcases a different pathfinding/targeting behavior.
 * 
 * All enemies can be summoned via commands: /summon rpgclasses_v2:<summon_id>
 * 
 * For testing, armor stands with the custom tag "player" are also valid targets.
 */
public class ExampleEnemies {
    
    // ============================================================
    // NORMAL - Basic enemy behavior, targets closest player
    // ============================================================
    
    /**
     * GRUNT - A basic melee enemy demonstrating NORMAL AI behavior
     * 
     * Behavior: Targets the closest player
     * Summon: /summon rpgclasses_v2:grunt
     * 
     * Stats:
     * - 40 HP
     * - 6 Damage
     * - 2 Defense
     * - Normal movement speed
     */
    public static final CustomEnemy GRUNT = new CustomEnemy.Builder(
            "grunt",
            "Grunt"
        )
        .description("A basic hostile creature that attacks the closest target")
        .maxHealth(40)
        .damage(6)
        .defense(2)
        .moveSpeed(1.0)
        .attackSpeed(1.0)
        .knockbackResistance(0.0)
        .experienceReward(10)
        .isBoss(false)
        .aiPreset(AIPreset.NORMAL)
        .baseEntityType("zombie")
        .summonId("grunt")
        .scale(1.0f)
        .glowColor(-1)
        .build();
    
    // ============================================================
    // RANGED - Keeps distance from players
    // ============================================================
    
    /**
     * SKELETON ARCHER - A ranged enemy demonstrating RANGED AI behavior
     * 
     * Behavior: Will attempt to keep a distance between itself and the player
     * Summon: /summon rpgclasses_v2:skeleton_archer
     * 
     * Stats:
     * - 30 HP
     * - 8 Damage (ranged)
     * - 0 Defense
     * - Fast movement speed
     */
    public static final CustomEnemy SKELETON_ARCHER = new CustomEnemy.Builder(
            "skeleton_archer",
            "Skeleton Archer"
        )
        .description("A skeletal archer that prefers to attack from a distance")
        .maxHealth(30)
        .damage(8)
        .defense(0)
        .moveSpeed(1.1)
        .attackSpeed(0.8)
        .knockbackResistance(0.0)
        .experienceReward(15)
        .isBoss(false)
        .aiPreset(AIPreset.RANGED)
        .baseEntityType("skeleton")
        .summonId("skeleton_archer")
        .scale(1.0f)
        .glowColor(-1)
        .build();
    
    // ============================================================
    // RANGED_ADVANCED - Keeps distance and flees when too close
    // ============================================================
    
    /**
     * ELITE MARKSMAN - A sophisticated ranged enemy demonstrating RANGED_ADVANCED AI
     * 
     * Behavior: Keeps distance from players, runs away if a player gets too close
     * Summon: /summon rpgclasses_v2:elite_marksman
     * 
     * Stats:
     * - 35 HP
     * - 12 Damage (ranged)
     * - 1 Defense
     * - Very fast movement (for escaping)
     */
    public static final CustomEnemy ELITE_MARKSMAN = new CustomEnemy.Builder(
            "elite_marksman",
            "Elite Marksman"
        )
        .description("A highly trained archer that flees when enemies get too close")
        .maxHealth(35)
        .damage(12)
        .defense(1)
        .moveSpeed(1.3)
        .attackSpeed(0.9)
        .knockbackResistance(0.0)
        .experienceReward(25)
        .isBoss(false)
        .aiPreset(AIPreset.RANGED_ADVANCED)
        .baseEntityType("skeleton")
        .summonId("elite_marksman")
        .scale(1.1f)
        .glowColor(0x00BFFF) // Light blue glow
        .build();
    
    // ============================================================
    // FLANKER - Stays outside line of sight until in range
    // ============================================================
    
    /**
     * SHADOW STALKER - A stealthy enemy demonstrating FLANKER AI behavior
     * 
     * Behavior: Stays outside the player's line of sight when not in range
     * Summon: /summon rpgclasses_v2:shadow_stalker
     * 
     * Stats:
     * - 45 HP
     * - 10 Damage
     * - 3 Defense
     * - Fast movement speed
     */
    public static final CustomEnemy SHADOW_STALKER = new CustomEnemy.Builder(
            "shadow_stalker",
            "Shadow Stalker"
        )
        .description("A cunning predator that approaches from blind spots")
        .maxHealth(45)
        .damage(10)
        .defense(3)
        .moveSpeed(1.2)
        .attackSpeed(1.1)
        .knockbackResistance(0.2)
        .experienceReward(20)
        .isBoss(false)
        .aiPreset(AIPreset.FLANKER)
        .ability(new ShadowStepAbility())
        .baseEntityType("wolf")
        .summonId("shadow_stalker")
        .scale(1.1f)
        .glowColor(0x4B0082) // Dark purple glow
        .build();
    
    // ============================================================
    // ASSASSIN - Targets lowest HP player
    // ============================================================
    
    /**
     * SILENT BLADE - An assassin enemy demonstrating ASSASSIN AI behavior
     * 
     * Behavior: Targets the lowest HP player in range
     * Summon: /summon rpgclasses_v2:silent_blade
     * 
     * Stats:
     * - 35 HP
     * - 15 Damage (high burst)
     * - 1 Defense
     * - Very fast movement speed
     */
    public static final CustomEnemy SILENT_BLADE = new CustomEnemy.Builder(
            "silent_blade",
            "Silent Blade"
        )
        .description("A deadly assassin that targets wounded prey")
        .maxHealth(35)
        .damage(15)
        .defense(1)
        .moveSpeed(1.4)
        .attackSpeed(1.3)
        .knockbackResistance(0.1)
        .experienceReward(30)
        .isBoss(false)
        .aiPreset(AIPreset.ASSASSIN)
        .ability(new ExecuteAbility())
        .baseEntityType("spider")
        .summonId("silent_blade")
        .scale(0.9f)
        .glowColor(0x8B0000) // Dark red glow
        .build();
    
    // ============================================================
    // BRUTE - Targets highest HP player
    // ============================================================
    
    /**
     * IRON GOLIATH - A powerful brute demonstrating BRUTE AI behavior
     * 
     * Behavior: Targets the highest HP player in range
     * Summon: /summon rpgclasses_v2:iron_goliath
     * 
     * Stats:
     * - 100 HP
     * - 12 Damage
     * - 8 Defense
     * - Slow movement speed
     * - High knockback resistance
     */
    public static final CustomEnemy IRON_GOLIATH = new CustomEnemy.Builder(
            "iron_goliath",
            "Iron Goliath"
        )
        .description("A massive warrior that seeks out the strongest opponent")
        .maxHealth(100)
        .damage(12)
        .defense(8)
        .moveSpeed(0.7)
        .attackSpeed(0.6)
        .knockbackResistance(0.8)
        .experienceReward(40)
        .isBoss(false)
        .aiPreset(AIPreset.BRUTE)
        .ability(new GroundPoundAbility())
        .baseEntityType("iron_golem")
        .summonId("iron_goliath")
        .scale(1.3f)
        .glowColor(0x808080) // Gray glow
        .build();
    
    // ============================================================
    // COWARD - Runs away when below 20% HP
    // ============================================================
    
    /**
     * GOBLIN SCOUT - A cowardly enemy demonstrating COWARD AI behavior
     * 
     * Behavior: Runs away when below 20% HP
     * Summon: /summon rpgclasses_v2:goblin_scout
     * 
     * Stats:
     * - 25 HP
     * - 5 Damage
     * - 0 Defense
     * - Very fast movement (especially when fleeing)
     */
    public static final CustomEnemy GOBLIN_SCOUT = new CustomEnemy.Builder(
            "goblin_scout",
            "Goblin Scout"
        )
        .description("A small but cunning creature that values self-preservation")
        .maxHealth(25)
        .damage(5)
        .defense(0)
        .moveSpeed(1.3)
        .attackSpeed(1.4)
        .knockbackResistance(0.0)
        .experienceReward(8)
        .isBoss(false)
        .aiPreset(AIPreset.COWARD)
        .baseEntityType("zombie")
        .summonId("goblin_scout")
        .scale(0.7f)
        .glowColor(0x32CD32) // Lime green glow
        .build();
    
    // ============================================================
    // FAR_SIGHTED - Targets farthest player
    // ============================================================
    
    /**
     * EAGLE EYE - A long-range enemy demonstrating FAR_SIGHTED AI behavior
     * 
     * Behavior: Targets the farthest player in range
     * Summon: /summon rpgclasses_v2:eagle_eye
     * 
     * Stats:
     * - 30 HP
     * - 14 Damage (long-range)
     * - 1 Defense
     * - Normal movement speed
     * - Extended aggro range
     */
    public static final CustomEnemy EAGLE_EYE = new CustomEnemy.Builder(
            "eagle_eye",
            "Eagle Eye"
        )
        .description("A sniper-type enemy that prefers distant engagements")
        .maxHealth(30)
        .damage(14)
        .defense(1)
        .moveSpeed(1.0)
        .attackSpeed(0.5)
        .knockbackResistance(0.0)
        .experienceReward(25)
        .isBoss(false)
        .aiPreset(AIPreset.FAR_SIGHTED)
        .customAggroRange(48.0) // Extended range
        .baseEntityType("skeleton")
        .summonId("eagle_eye")
        .scale(1.0f)
        .glowColor(0xFFD700) // Gold glow
        .build();
    
    // ============================================================
    // CHAOTIC - Targets random player
    // ============================================================
    
    /**
     * CHAOS IMP - An unpredictable enemy demonstrating CHAOTIC AI behavior
     * 
     * Behavior: Targets a random player in range
     * Summon: /summon rpgclasses_v2:chaos_imp
     * 
     * Stats:
     * - 30 HP
     * - 7 Damage
     * - 2 Defense
     * - Fast, erratic movement
     */
    public static final CustomEnemy CHAOS_IMP = new CustomEnemy.Builder(
            "chaos_imp",
            "Chaos Imp"
        )
        .description("An unpredictable creature that switches targets randomly")
        .maxHealth(30)
        .damage(7)
        .defense(2)
        .moveSpeed(1.2)
        .attackSpeed(1.2)
        .knockbackResistance(0.1)
        .experienceReward(15)
        .isBoss(false)
        .aiPreset(AIPreset.CHAOTIC)
        .ability(new ChaosBlinkAbility())
        .baseEntityType("vex")
        .summonId("chaos_imp")
        .scale(0.8f)
        .glowColor(0xFF00FF) // Magenta glow
        .build();
    
    // ============================================================
    // ABILITY IMPLEMENTATIONS
    // ============================================================
    
    /**
     * Shadow Step ability for Shadow Stalker
     * Short-range teleport to a position behind the target
     */
    public static class ShadowStepAbility implements EnemyAbility {
        
        @Override
        public String getId() {
            return "shadow_step";
        }
        
        @Override
        public String getName() {
            return "Shadow Step";
        }
        
        @Override
        public String getDescription() {
            return "Teleports to a position behind the target";
        }
        
        @Override
        public int getCooldownTicks() {
            return 120; // 6 seconds
        }
        
        @Override
        public double getRange() {
            return 12.0;
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
            
            // Spawn particles at origin
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                entity.getX(), entity.getY() + 1, entity.getZ(),
                15, 0.4, 0.4, 0.4, 0.05);
            
            // Play teleport sound
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 1.4F);
            
            // Teleport
            entity.teleportTo(behindTarget.x, target.getY(), behindTarget.z);
            
            // Spawn particles at destination
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                entity.getX(), entity.getY() + 1, entity.getZ(),
                15, 0.4, 0.4, 0.4, 0.05);
            
            return true;
        }
    }
    
    /**
     * Execute ability for Silent Blade
     * Deals bonus damage to targets below 30% HP
     */
    public static class ExecuteAbility implements EnemyAbility {
        
        @Override
        public String getId() {
            return "execute";
        }
        
        @Override
        public String getName() {
            return "Execute";
        }
        
        @Override
        public String getDescription() {
            return "Deals massive damage to wounded targets";
        }
        
        @Override
        public int getCooldownTicks() {
            return 200; // 10 seconds
        }
        
        @Override
        public double getRange() {
            return 4.0;
        }
        
        @Override
        public boolean execute(LivingEntity entity, LivingEntity target) {
            if (entity.level().isClientSide || target == null) {
                return false;
            }
            
            ServerLevel level = (ServerLevel) entity.level();
            
            // Calculate damage - bonus if target is below 30% HP
            float healthPercent = target.getHealth() / target.getMaxHealth();
            float damage = healthPercent < 0.3f ? 25.0f : 10.0f;
            
            // Deal damage
            target.hurt(level.damageSources().mobAttack(entity), damage);
            
            // Visual effects
            level.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                20, 0.3, 0.3, 0.3, 0.1);
            
            // Sound
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0F, 0.8F);
            
            return true;
        }
    }
    
    /**
     * Ground Pound ability for Iron Goliath
     * AoE attack that damages and slows nearby enemies
     */
    public static class GroundPoundAbility implements EnemyAbility {
        
        @Override
        public String getId() {
            return "ground_pound";
        }
        
        @Override
        public String getName() {
            return "Ground Pound";
        }
        
        @Override
        public String getDescription() {
            return "Slams the ground, damaging and slowing nearby enemies";
        }
        
        @Override
        public int getCooldownTicks() {
            return 160; // 8 seconds
        }
        
        @Override
        public double getRange() {
            return 6.0;
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
            
            // Find all entities in range
            AABB aoeBox = entity.getBoundingBox().inflate(getRange());
            List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
                LivingEntity.class,
                aoeBox,
                e -> e != entity && e.isAlive()
            );
            
            // Damage and slow all nearby entities
            for (LivingEntity nearby : nearbyEntities) {
                nearby.hurt(level.damageSources().mobAttack(entity), 8.0f);
                nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)); // 3 seconds
            }
            
            // Visual effects - ground particles
            for (int i = 0; i < 40; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * getRange() * 2;
                double offsetZ = (level.random.nextDouble() - 0.5) * getRange() * 2;
                
                level.sendParticles(ParticleTypes.EXPLOSION,
                    entity.getX() + offsetX, entity.getY() + 0.1, entity.getZ() + offsetZ,
                    1, 0, 0, 0, 0);
            }
            
            // Sound
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6F, 0.8F);
            
            return true;
        }
    }
    
    /**
     * Chaos Blink ability for Chaos Imp
     * Teleports to a random nearby location
     */
    public static class ChaosBlinkAbility implements EnemyAbility {
        
        @Override
        public String getId() {
            return "chaos_blink";
        }
        
        @Override
        public String getName() {
            return "Chaos Blink";
        }
        
        @Override
        public String getDescription() {
            return "Teleports to a random nearby location";
        }
        
        @Override
        public int getCooldownTicks() {
            return 60; // 3 seconds
        }
        
        @Override
        public double getRange() {
            return -1; // No target needed
        }
        
        @Override
        public boolean requiresTarget() {
            return false;
        }
        
        @Override
        public boolean execute(LivingEntity entity, LivingEntity target) {
            if (entity.level().isClientSide) {
                return false;
            }
            
            ServerLevel level = (ServerLevel) entity.level();
            
            // Calculate random teleport position
            double offsetX = (level.random.nextDouble() - 0.5) * 16;
            double offsetZ = (level.random.nextDouble() - 0.5) * 16;
            
            Vec3 newPos = entity.position().add(offsetX, 0, offsetZ);
            
            // Particles at origin
            level.sendParticles(ParticleTypes.PORTAL,
                entity.getX(), entity.getY() + 1, entity.getZ(),
                30, 0.5, 0.5, 0.5, 0.5);
            
            // Sound
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 2.0F);
            
            // Teleport
            entity.teleportTo(newPos.x, entity.getY(), newPos.z);
            
            // Particles at destination
            level.sendParticles(ParticleTypes.PORTAL,
                entity.getX(), entity.getY() + 1, entity.getZ(),
                30, 0.5, 0.5, 0.5, 0.5);
            
            return true;
        }
    }
}
