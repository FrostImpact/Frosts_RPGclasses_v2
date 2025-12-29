package net.frostimpact.rpgclasses_v2.armor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for passive armor effects.
 * Passives are always active when the armor piece is equipped.
 */
public interface ArmorPassive {
    
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
     * Called every tick while the armor is equipped
     * @param player The player wearing the armor
     * @param armor The armor item stack
     */
    default void onTick(Player player, ItemStack armor) {}
    
    /**
     * Called when the player takes damage while wearing this armor
     * @param player The player taking damage
     * @param armor The armor item stack
     * @param damage The incoming damage
     * @return The modified damage to take
     */
    default float onDamageTaken(Player player, ItemStack armor, float damage) {
        return damage;
    }
    
    /**
     * Called when the player deals damage while wearing this armor
     * @param player The player dealing damage
     * @param target The entity being attacked
     * @param armor The armor item stack
     * @param damage The outgoing damage
     * @return The modified damage to deal
     */
    default float onDamageDealt(Player player, LivingEntity target, ItemStack armor, float damage) {
        return damage;
    }
    
    /**
     * Called when the player kills an entity while wearing this armor
     * @param player The player who killed the entity
     * @param killed The entity that was killed
     * @param armor The armor item stack
     */
    default void onKill(Player player, LivingEntity killed, ItemStack armor) {}
    
    /**
     * Called when the armor is equipped
     * @param player The player equipping the armor
     * @param armor The armor item stack
     */
    default void onEquip(Player player, ItemStack armor) {}
    
    /**
     * Called when the armor is unequipped
     * @param player The player unequipping the armor
     * @param armor The armor item stack
     */
    default void onUnequip(Player player, ItemStack armor) {}
}
