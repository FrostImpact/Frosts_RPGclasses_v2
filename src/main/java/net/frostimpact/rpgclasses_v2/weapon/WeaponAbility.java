package net.frostimpact.rpgclasses_v2.weapon;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for custom weapon abilities triggered on right-click.
 * Implement this interface to create new weapon abilities.
 */
public interface WeaponAbility {
    
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
     * Execute the ability when right-click is performed
     * @param player The player using the ability
     * @param weapon The weapon item stack
     * @return true if the ability was successfully executed
     */
    boolean execute(Player player, ItemStack weapon);
    
    /**
     * Get the mana cost of this ability
     */
    int getManaCost();
    
    /**
     * Get the cooldown in ticks (20 ticks = 1 second)
     */
    int getCooldownTicks();
    
    /**
     * Check if this ability can be used (ignoring cooldown and mana)
     * @param player The player attempting to use the ability
     * @param weapon The weapon item stack
     * @return true if the ability can be used
     */
    default boolean canUse(Player player, ItemStack weapon) {
        return true;
    }
}
