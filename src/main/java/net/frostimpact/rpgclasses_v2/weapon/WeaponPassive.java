package net.frostimpact.rpgclasses_v2.weapon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for passive weapon effects.
 * Passives are always active when holding or equipping the weapon.
 */
public interface WeaponPassive {
    
    /**
     * Get the unique ID of this passive
     */
    String getId();
    
    /**
     * Get the display name of this passive
     */
    String getName();
    
    /**
     * Get the description of this passive
     */
    String getDescription();
    
    /**
     * Called every tick while the weapon is held
     * @param player The player holding the weapon
     * @param weapon The weapon item stack
     */
    default void onTick(Player player, ItemStack weapon) {}
    
    /**
     * Called when the player attacks an entity with this weapon
     * @param player The player attacking
     * @param target The entity being attacked
     * @param weapon The weapon item stack
     * @param damage The base damage being dealt
     * @return The modified damage to deal
     */
    default float onAttack(Player player, LivingEntity target, ItemStack weapon, float damage) {
        return damage;
    }
    
    /**
     * Called when the player takes damage while holding this weapon
     * @param player The player taking damage
     * @param weapon The weapon item stack
     * @param damage The incoming damage
     * @return The modified damage to take
     */
    default float onDamageTaken(Player player, ItemStack weapon, float damage) {
        return damage;
    }
    
    /**
     * Called when the player kills an entity with this weapon
     * @param player The player who killed the entity
     * @param killed The entity that was killed
     * @param weapon The weapon item stack
     */
    default void onKill(Player player, LivingEntity killed, ItemStack weapon) {}
}
