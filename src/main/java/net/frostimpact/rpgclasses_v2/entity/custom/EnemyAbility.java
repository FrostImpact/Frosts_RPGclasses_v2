package net.frostimpact.rpgclasses_v2.entity.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Interface for custom enemy abilities.
 * Abilities can be triggered by AI conditions or on a timer.
 */
public interface EnemyAbility {
    
    /**
     * Get the unique ID of this ability
     */
    String getId();
    
    /**
     * Get the display name of this ability
     */
    String getName();
    
    /**
     * Get the description of this ability
     */
    String getDescription();
    
    /**
     * Execute the ability
     * @param entity The enemy entity using the ability
     * @param target The target (usually a player), may be null
     * @return true if the ability was successfully executed
     */
    boolean execute(LivingEntity entity, LivingEntity target);
    
    /**
     * Get the cooldown in ticks (20 ticks = 1 second)
     */
    int getCooldownTicks();
    
    /**
     * Get the range at which this ability can be used
     * @return Maximum range in blocks, or -1 for unlimited
     */
    default double getRange() {
        return 10.0;
    }
    
    /**
     * Check if this ability requires a target
     */
    default boolean requiresTarget() {
        return true;
    }
    
    /**
     * Check if this ability can be used
     * @param entity The enemy entity
     * @param target The potential target
     * @return true if the ability can be used
     */
    default boolean canUse(LivingEntity entity, LivingEntity target) {
        if (requiresTarget() && target == null) {
            return false;
        }
        if (target != null && getRange() > 0) {
            return entity.distanceTo(target) <= getRange();
        }
        return true;
    }
}
