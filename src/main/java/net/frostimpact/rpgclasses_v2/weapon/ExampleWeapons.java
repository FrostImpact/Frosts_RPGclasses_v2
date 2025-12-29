package net.frostimpact.rpgclasses_v2.weapon;

import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Example custom weapons demonstrating the custom weapon system.
 * Use these as templates for creating your own weapons.
 */
public class ExampleWeapons {
    
    /**
     * INFERNAL BLADE - An epic fire longsword with flame abilities
     * 
     * Stats:
     * - 12 base damage (18 effective with Epic rarity)
     * - +5 Damage bonus
     * - +10% Attack Speed bonus
     * 
     * Ability: Flame Wave
     * - Shoots a wave of fire that damages enemies in front
     * - 30 mana cost, 8 second cooldown
     * 
     * Passive: Burning Strike
     * - 20% chance to set enemies on fire when hit
     */
    public static final CustomWeapon INFERNAL_BLADE = new CustomWeapon.Builder(
            "infernal_blade",
            "Infernal Blade",
            WeaponType.LONGSWORD
        )
        .description("Forged in the depths of the Nether, this blade burns with eternal flames.")
        .rarity(WeaponRarity.EPIC)
        .baseDamage(12)
        .attackSpeed(1.4)
        .criticalChance(15)
        .criticalDamage(175)
        .reach(3)
        .addStat(StatType.DAMAGE, 5)
        .addStat(StatType.ATTACK_SPEED, 10)
        .ability(new FlameWaveAbility())
        .passive(new BurningStrikePassive())
        .lore("The flames hunger for souls...")
        .build();
    
    /**
     * Flame Wave ability implementation
     * Shoots a wave of fire in front of the player
     */
    public static class FlameWaveAbility implements WeaponAbility {
        @Override
        public String getId() {
            return "flame_wave";
        }
        
        @Override
        public String getName() {
            return "Flame Wave";
        }
        
        @Override
        public String getDescription() {
            return "Unleash a wave of fire that damages enemies in front of you";
        }
        
        @Override
        public int getManaCost() {
            return 30;
        }
        
        @Override
        public int getCooldownTicks() {
            return 160; // 8 seconds
        }
        
        @Override
        public boolean execute(Player player, ItemStack weapon) {
            if (player.level().isClientSide) {
                return false;
            }
            
            ServerLevel level = (ServerLevel) player.level();
            Vec3 lookVec = player.getLookAngle();
            Vec3 startPos = player.position().add(0, 1, 0);
            
            // Play sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            
            // Create flame particles in a cone
            for (int i = 0; i < 20; i++) {
                double distance = i * 0.5;
                Vec3 particlePos = startPos.add(lookVec.scale(distance));
                
                // Spread particles in a cone shape
                double spread = distance * 0.3;
                for (int j = 0; j < 5; j++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * spread;
                    double offsetY = (level.random.nextDouble() - 0.5) * spread;
                    double offsetZ = (level.random.nextDouble() - 0.5) * spread;
                    
                    level.sendParticles(ParticleTypes.FLAME,
                        particlePos.x + offsetX,
                        particlePos.y + offsetY,
                        particlePos.z + offsetZ,
                        1, 0, 0, 0, 0.05);
                }
            }
            
            // Damage entities in the path
            AABB damageBox = new AABB(
                startPos.subtract(1, 1, 1),
                startPos.add(lookVec.scale(10)).add(1, 1, 1)
            );
            
            List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class, 
                damageBox,
                entity -> entity != player && entity.isAlive()
            );
            
            for (LivingEntity entity : entities) {
                // Check if entity is in front of player (within cone)
                Vec3 toEntity = entity.position().subtract(player.position()).normalize();
                double dot = lookVec.dot(toEntity);
                
                if (dot > 0.5) { // Within ~60 degree cone
                    entity.hurt(level.damageSources().playerAttack(player), 8.0F);
                    entity.setRemainingFireTicks(100); // 5 seconds of fire
                }
            }
            
            return true;
        }
    }
    
    /**
     * Burning Strike passive implementation
     * Has a chance to set enemies on fire when attacking
     */
    public static class BurningStrikePassive implements WeaponPassive {
        private static final double BURN_CHANCE = 0.20; // 20% chance
        private static final int BURN_DURATION = 60; // 3 seconds
        
        @Override
        public String getId() {
            return "burning_strike";
        }
        
        @Override
        public String getName() {
            return "Burning Strike";
        }
        
        @Override
        public String getDescription() {
            return "20% chance to set enemies on fire for 3 seconds";
        }
        
        @Override
        public float onAttack(Player player, LivingEntity target, ItemStack weapon, float damage) {
            if (!player.level().isClientSide && player.level().random.nextDouble() < BURN_CHANCE) {
                target.setRemainingFireTicks(BURN_DURATION);
                
                // Visual feedback
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        10, 0.3, 0.3, 0.3, 0.02);
                }
            }
            return damage;
        }
    }
}
