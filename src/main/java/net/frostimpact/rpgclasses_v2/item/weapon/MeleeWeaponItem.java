package net.frostimpact.rpgclasses_v2.item.weapon;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.resources.ResourceLocation;

/**
 * Base class for melee weapons - reusable for future weapon types (axes, etc.)
 */
public abstract class MeleeWeaponItem extends TieredItem {
    private final float attackDamage;
    private final float attackSpeed;
    private final WeaponStats weaponStats;

    // Source identifier for stat modifiers
    public static final String WEAPON_STAT_SOURCE = "weapon_equipped";

    public MeleeWeaponItem(Tier tier, float attackDamage, float attackSpeed, Properties properties) {
        this(tier, attackDamage, attackSpeed, WeaponStats.empty(), properties);
    }

    public MeleeWeaponItem(Tier tier, float attackDamage, float attackSpeed, WeaponStats weaponStats, Properties properties) {
        super(tier, properties.attributes(createAttributes(tier, attackDamage, attackSpeed)));
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.weaponStats = weaponStats;
    }

    /**
     * Create attribute modifiers for attack damage and speed
     */
    private static ItemAttributeModifiers createAttributes(Tier tier, float attackDamage, float attackSpeed) {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                ResourceLocation.withDefaultNamespace("base_attack_damage"),
                                attackDamage + tier.getAttackDamageBonus(),
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                ResourceLocation.withDefaultNamespace("base_attack_speed"),
                                attackSpeed,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
    }

    public float getAttackDamage() {
        return attackDamage;
    }

    public float getAttackSpeed() {
        return attackSpeed;
    }

    public WeaponStats getWeaponStats() {
        return weaponStats;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false; // Don't break blocks on left click
    }

    public abstract WeaponType getWeaponType();
}