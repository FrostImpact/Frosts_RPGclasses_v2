package net.frostimpact.rpgclasses_v2.rpg.stats.weapon;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

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

    // Updated signature to match current mappings (no ItemStack parameter)
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false; // Don't break blocks on left click
    }
}