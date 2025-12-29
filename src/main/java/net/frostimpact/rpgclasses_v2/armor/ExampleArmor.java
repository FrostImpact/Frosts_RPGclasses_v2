package net.frostimpact.rpgclasses_v2.armor;

import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Example custom armor set demonstrating the armor system.
 * 
 * SHADOW ASSASSIN SET (Rare)
 * - Focused on speed, critical hits, and stealth
 * 
 * Individual piece bonuses:
 * - Helmet: +5% Move Speed, Night Vision passive
 * - Chestplate: +3 Defense, +5% Attack Speed
 * - Leggings: +10% Move Speed, +2 Defense
 * - Boots: +15% Move Speed, Silent Step passive (no fall damage)
 * 
 * Set Bonuses:
 * - 2pc: Shadow Meld - Become harder to detect by mobs
 * - 4pc: Shadow Strike - First hit after 3 seconds of not attacking deals 50% more damage
 */
public class ExampleArmor {
    
    public static final String SHADOW_ASSASSIN_SET_ID = "shadow_assassin";
    
    // ===== Individual Armor Pieces =====
    
    public static final CustomArmorPiece SHADOW_ASSASSIN_HELMET = new CustomArmorPiece.Builder(
            "shadow_assassin_helmet",
            "Shadow Assassin Hood",
            ArmorSlot.HELMET
        )
        .description("A dark hood that enhances your vision in darkness")
        .rarity(ArmorRarity.RARE)
        .defense(3)
        .addStat(StatType.MOVE_SPEED, 5)
        .passive(new NightVisionPassive())
        .setId(SHADOW_ASSASSIN_SET_ID)
        .lore("See through the darkness...")
        .build();
    
    public static final CustomArmorPiece SHADOW_ASSASSIN_CHESTPLATE = new CustomArmorPiece.Builder(
            "shadow_assassin_chestplate",
            "Shadow Assassin Vest",
            ArmorSlot.CHESTPLATE
        )
        .description("Light armor that doesn't impede movement")
        .rarity(ArmorRarity.RARE)
        .defense(5)
        .addStat(StatType.ATTACK_SPEED, 5)
        .addStat(StatType.DEFENSE, 3)
        .setId(SHADOW_ASSASSIN_SET_ID)
        .lore("Swift as the wind...")
        .build();
    
    public static final CustomArmorPiece SHADOW_ASSASSIN_LEGGINGS = new CustomArmorPiece.Builder(
            "shadow_assassin_leggings",
            "Shadow Assassin Pants",
            ArmorSlot.LEGGINGS
        )
        .description("Flexible pants that allow for quick movement")
        .rarity(ArmorRarity.RARE)
        .defense(4)
        .addStat(StatType.MOVE_SPEED, 10)
        .addStat(StatType.DEFENSE, 2)
        .setId(SHADOW_ASSASSIN_SET_ID)
        .lore("Move like a shadow...")
        .build();
    
    public static final CustomArmorPiece SHADOW_ASSASSIN_BOOTS = new CustomArmorPiece.Builder(
            "shadow_assassin_boots",
            "Shadow Assassin Boots",
            ArmorSlot.BOOTS
        )
        .description("Silent boots that make no sound")
        .rarity(ArmorRarity.RARE)
        .defense(2)
        .addStat(StatType.MOVE_SPEED, 15)
        .passive(new SilentStepPassive())
        .setId(SHADOW_ASSASSIN_SET_ID)
        .lore("Silent as death...")
        .build();
    
    // ===== Complete Armor Set =====
    
    public static final CustomArmorSet SHADOW_ASSASSIN_SET = new CustomArmorSet.Builder(
            SHADOW_ASSASSIN_SET_ID,
            "Shadow Assassin Set"
        )
        .description("Armor worn by the legendary Shadow Assassins")
        .rarity(ArmorRarity.RARE)
        .helmet(SHADOW_ASSASSIN_HELMET)
        .chestplate(SHADOW_ASSASSIN_CHESTPLATE)
        .leggings(SHADOW_ASSASSIN_LEGGINGS)
        .boots(SHADOW_ASSASSIN_BOOTS)
        .setBonus2pc(new ShadowMeldSetBonus())
        .setBonus4pc(new ShadowStrikeSetBonus())
        .addFullSetStat(StatType.MOVE_SPEED, 10)
        .addFullSetStat(StatType.ATTACK_SPEED, 10)
        .build();
    
    // ===== Passive Implementations =====
    
    /**
     * Night Vision passive - grants night vision effect while equipped
     */
    public static class NightVisionPassive implements ArmorPassive {
        @Override
        public String getId() {
            return "night_vision";
        }
        
        @Override
        public String getName() {
            return "Night Vision";
        }
        
        @Override
        public String getDescription() {
            return "Grants night vision while equipped";
        }
        
        @Override
        public void onTick(Player player, ItemStack armor) {
            // Apply night vision every 10 seconds to keep it active
            if (!player.level().isClientSide && player.level().getGameTime() % 200 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false));
            }
        }
    }
    
    /**
     * Silent Step passive - negates fall damage
     */
    public static class SilentStepPassive implements ArmorPassive {
        @Override
        public String getId() {
            return "silent_step";
        }
        
        @Override
        public String getName() {
            return "Silent Step";
        }
        
        @Override
        public String getDescription() {
            return "Negates fall damage";
        }
        
        @Override
        public float onDamageTaken(Player player, ItemStack armor, float damage) {
            // Check if this is fall damage by checking if player just fell
            if (player.fallDistance > 0) {
                // Spawn some particles to show the passive activating
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                        player.getX(), player.getY(), player.getZ(),
                        10, 0.3, 0.1, 0.3, 0.02);
                }
                return 0; // Negate the damage
            }
            return damage;
        }
    }
    
    /**
     * Shadow Meld 2-piece set bonus - reduces mob detection range
     */
    public static class ShadowMeldSetBonus implements ArmorPassive {
        @Override
        public String getId() {
            return "shadow_meld_2pc";
        }
        
        @Override
        public String getName() {
            return "Shadow Meld (2pc)";
        }
        
        @Override
        public String getDescription() {
            return "Mobs have reduced detection range";
        }
        
        @Override
        public void onTick(Player player, ItemStack armor) {
            // Apply invisibility particle effect occasionally to show the passive
            if (!player.level().isClientSide && player.level().getGameTime() % 40 == 0) {
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                        player.getX(), player.getY() + 1, player.getZ(),
                        2, 0.2, 0.4, 0.2, 0.01);
                }
            }
        }
    }
    
    /**
     * Shadow Strike 4-piece set bonus - bonus damage on first hit after not attacking
     */
    public static class ShadowStrikeSetBonus implements ArmorPassive {
        // Per-player tracking of last attack time
        private static final Map<UUID, Long> lastAttackTimes = new ConcurrentHashMap<>();
        private static final long CHARGE_TIME_MS = 3000; // 3 seconds
        
        @Override
        public String getId() {
            return "shadow_strike_4pc";
        }
        
        @Override
        public String getName() {
            return "Shadow Strike (4pc)";
        }
        
        @Override
        public String getDescription() {
            return "First attack after 3s deals +50% damage";
        }
        
        @Override
        public float onDamageDealt(Player player, LivingEntity target, ItemStack armor, float damage) {
            long currentTime = System.currentTimeMillis();
            UUID playerUUID = player.getUUID();
            long lastAttackTime = lastAttackTimes.getOrDefault(playerUUID, 0L);
            
            if (currentTime - lastAttackTime >= CHARGE_TIME_MS) {
                // Charged attack - deal bonus damage
                lastAttackTimes.put(playerUUID, currentTime);
                
                // Visual feedback
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        15, 0.3, 0.3, 0.3, 0.1);
                }
                
                return damage * 1.5f; // 50% bonus damage
            }
            
            lastAttackTimes.put(playerUUID, currentTime);
            return damage;
        }
    }
}
