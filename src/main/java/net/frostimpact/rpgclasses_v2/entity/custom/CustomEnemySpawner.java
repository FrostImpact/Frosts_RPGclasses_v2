package net.frostimpact.rpgclasses_v2.entity.custom;

import net. minecraft.core.BlockPos;
import net.minecraft. network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net. minecraft.world.entity.EntityType;
import net.minecraft.world. entity. Mob;
import net. minecraft.world.entity.ai.attributes.AttributeInstance;
import net. minecraft.world.entity.ai.attributes. Attributes;
import net. minecraft.world.entity.monster.Zombie;
import net.minecraft. world.entity.monster. Skeleton;
import net. minecraft.world.entity.monster.Spider;
import org.slf4j. Logger;
import org.slf4j. LoggerFactory;

/**
 * Handles spawning custom enemies by applying CustomEnemy stats to vanilla mobs
 */
public class CustomEnemySpawner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomEnemySpawner.class);

    /**
     * Spawn a custom enemy at the specified location
     * @param enemy The custom enemy definition
     * @param level The server level to spawn in
     * @param pos The position to spawn at
     * @return true if spawn was successful
     */
    public static boolean spawn(CustomEnemy enemy, ServerLevel level, BlockPos pos) {
        try {
            // Get the base entity type
            Mob mob = createBaseEntity(enemy. getBaseEntityType(), level);
            if (mob == null) {
                LOGGER.error("Failed to create base entity for type: {}", enemy. getBaseEntityType());
                return false;
            }

            // Position the entity
            mob.setPos(pos. getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

            // Apply custom stats
            applyCustomStats(mob, enemy);

            // Set custom name
            mob.setCustomName(Component.literal(enemy. getDisplayName()));
            mob.setCustomNameVisible(true);

            // Apply scale if different from default
            if (enemy.getScale() != 1.0f) {
                // Scale is applied via attribute in newer Minecraft versions
                AttributeInstance scaleAttr = mob.getAttribute(Attributes.SCALE);
                if (scaleAttr != null) {
                    scaleAttr.setBaseValue(enemy. getScale());
                }
            }

            // Apply glow effect if specified
            if (enemy.getGlowColor() >= 0) {
                mob.setGlowingTag(true);
            }

            // Store the custom enemy ID for later reference (using persistent data)
            mob.getPersistentData().putString("rpgclasses_custom_enemy", enemy.getId());

            // Spawn the entity
            level.addFreshEntity(mob);

            LOGGER.debug("Spawned custom enemy {} at {}", enemy. getId(), pos);
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to spawn custom enemy: {}", enemy. getId(), e);
            return false;
        }
    }

    private static Mob createBaseEntity(String baseType, ServerLevel level) {
        return switch (baseType. toLowerCase()) {
            case "zombie" -> new Zombie(EntityType.ZOMBIE, level);
            case "skeleton" -> new Skeleton(EntityType.SKELETON, level);
            case "spider" -> new Spider(EntityType. SPIDER, level);
            case "husk" -> EntityType.HUSK.create(level);
            case "stray" -> EntityType.STRAY.create(level);
            case "cave_spider" -> EntityType.CAVE_SPIDER.create(level);
            case "drowned" -> EntityType.DROWNED. create(level);
            case "wither_skeleton" -> EntityType.WITHER_SKELETON. create(level);
            case "piglin" -> EntityType.PIGLIN. create(level);
            case "piglin_brute" -> EntityType. PIGLIN_BRUTE.create(level);
            case "vindicator" -> EntityType.VINDICATOR.create(level);
            case "pillager" -> EntityType.PILLAGER. create(level);
            default -> new Zombie(EntityType. ZOMBIE, level); // Default fallback
        };
    }

    private static void applyCustomStats(Mob mob, CustomEnemy enemy) {
        // Apply max health
        AttributeInstance healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr. setBaseValue(enemy.getMaxHealth());
            mob.setHealth((float) enemy.getMaxHealth()); // Set current health to max
        }

        // Apply attack damage
        AttributeInstance damageAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(enemy.getDamage());
        }

        // Apply armor (defense)
        AttributeInstance armorAttr = mob.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr. setBaseValue(enemy.getDefense());
        }

        // Apply movement speed
        AttributeInstance speedAttr = mob. getAttribute(Attributes. MOVEMENT_SPEED);
        if (speedAttr != null) {
            // Base zombie speed is 0.23, scale relative to that
            double baseSpeed = speedAttr.getBaseValue();
            speedAttr.setBaseValue(baseSpeed * enemy.getMoveSpeed());
        }

        // Apply attack speed if applicable
        AttributeInstance attackSpeedAttr = mob.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeedAttr != null) {
            attackSpeedAttr. setBaseValue(enemy.getAttackSpeed());
        }

        // Apply knockback resistance
        AttributeInstance kbAttr = mob.getAttribute(Attributes. KNOCKBACK_RESISTANCE);
        if (kbAttr != null) {
            kbAttr. setBaseValue(enemy.getKnockbackResistance());
        }
    }
}