package net.frostimpact.rpgclasses_v2.rpg.stats.weapon;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;

/**
 * Base class for melee weapons - reusable for future weapon types (axes, etc.)
 */
public class MeleeWeaponItem extends TieredItem {
    private final float attackDamage;
    private final float attackSpeed;
    
    public MeleeWeaponItem(Tier tier, float attackDamage, float attackSpeed, Properties properties) {
        super(tier, properties);
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
    }
    
    public float getAttackDamage() {
        return attackDamage;
    }
    
    public float getAttackSpeed() {
        return attackSpeed;
    }
    
    @Override
    public boolean canAttackBlock(ItemStack stack, net.minecraft.world.level.block.state.BlockState state, 
                                   net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, 
                                   net.minecraft.world.entity.player.Player player) {
        return false; // Don't break blocks on left click
    }
}
