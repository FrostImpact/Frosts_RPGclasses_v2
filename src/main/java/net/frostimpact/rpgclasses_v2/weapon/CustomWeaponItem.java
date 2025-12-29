package net.frostimpact.rpgclasses_v2.weapon;

import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.frostimpact.rpgclasses_v2.rpg.stats.PlayerStats;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Custom weapon item class that integrates with the CustomWeapon system.
 * This handles right-click abilities, passives, stats application, and tooltips.
 */
public class CustomWeaponItem extends Item {
    private final CustomWeapon customWeapon;
    
    public CustomWeaponItem(CustomWeapon customWeapon, Properties properties) {
        super(properties);
        this.customWeapon = customWeapon;
    }
    
    public CustomWeapon getCustomWeapon() {
        return customWeapon;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (!customWeapon.hasAbility()) {
            return InteractionResultHolder.pass(itemStack);
        }
        
        WeaponAbility ability = customWeapon.getAbility();
        
        // Check cooldown
        PlayerRPGData rpgData = player.getData(ModAttachments.PLAYER_RPG);
        String cooldownKey = "weapon_" + customWeapon.getId();
        
        if (rpgData.getAbilityCooldown(cooldownKey) > 0) {
            if (level.isClientSide) {
                player.displayClientMessage(
                    Component.literal("Ability on cooldown: " + (rpgData.getAbilityCooldown(cooldownKey) / 20) + "s")
                        .withStyle(ChatFormatting.RED), 
                    true
                );
            }
            return InteractionResultHolder.fail(itemStack);
        }
        
        // Check mana cost
        int manaCost = ability.getManaCost();
        if (rpgData.getMana() < manaCost) {
            if (level.isClientSide) {
                player.displayClientMessage(
                    Component.literal("Not enough mana! Need: " + manaCost)
                        .withStyle(ChatFormatting.RED), 
                    true
                );
            }
            return InteractionResultHolder.fail(itemStack);
        }
        
        // Check if ability can be used
        if (!ability.canUse(player, itemStack)) {
            return InteractionResultHolder.fail(itemStack);
        }
        
        // Execute ability
        if (!level.isClientSide) {
            boolean success = ability.execute(player, itemStack);
            if (success) {
                rpgData.useMana(manaCost);
                rpgData.setAbilityCooldown(cooldownKey, ability.getCooldownTicks());
                return InteractionResultHolder.success(itemStack);
            }
        }
        
        return InteractionResultHolder.consume(itemStack);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        if (!(entity instanceof Player player)) {
            return;
        }
        
        // Only apply effects when weapon is in hand
        if (!isSelected && player.getOffhandItem() != stack) {
            return;
        }
        
        // Tick passives
        for (WeaponPassive passive : customWeapon.getPassives()) {
            passive.onTick(player, stack);
        }
        
        // Apply stat modifiers (server-side only)
        if (!level.isClientSide && level.getGameTime() % 20 == 0) {
            applyStatModifiers(player);
        }
    }
    
    private void applyStatModifiers(Player player) {
        PlayerStats stats = player.getData(ModAttachments.PLAYER_STATS);
        String source = "weapon_" + customWeapon.getId();
        
        // Remove old modifiers from this weapon
        stats.removeAllFromSource(source);
        
        // Add new modifiers
        for (StatModifier modifier : customWeapon.createStatModifiers()) {
            stats.addModifier(modifier);
        }
    }
    
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player) {
            // Trigger passive onAttack callbacks
            for (WeaponPassive passive : customWeapon.getPassives()) {
                passive.onAttack(player, target, stack, customWeapon.getEffectiveDamage());
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Rarity line
        tooltip.add(Component.literal(customWeapon.getRarity().getDisplayName())
            .withStyle(Style.EMPTY.withColor(customWeapon.getRarity().getColor())));
        
        // Weapon type
        tooltip.add(Component.literal(customWeapon.getWeaponType().getDisplayName())
            .withStyle(ChatFormatting.GRAY));
        
        // Empty line
        tooltip.add(Component.empty());
        
        // Base stats
        tooltip.add(Component.literal("Damage: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(customWeapon.getEffectiveDamage())).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("Attack Speed: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.1f", customWeapon.getAttackSpeed())).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("Critical Chance: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(customWeapon.getCriticalChance() + "%").withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("Critical Damage: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(customWeapon.getCriticalDamage() + "%").withStyle(ChatFormatting.WHITE)));
        
        // Bonus stats
        if (!customWeapon.getBonusStats().isEmpty()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Bonus Stats:").withStyle(ChatFormatting.GOLD));
            for (var entry : customWeapon.getBonusStats().entrySet()) {
                double effectiveValue = entry.getValue() * customWeapon.getRarity().getStatMultiplier();
                String statName = formatStatName(entry.getKey());
                String prefix = effectiveValue >= 0 ? "+" : "";
                String valueStr = entry.getKey().isPercentage() 
                    ? prefix + String.format("%.0f", effectiveValue) + "% " + statName
                    : prefix + String.format("%.0f", effectiveValue) + " " + statName;
                tooltip.add(Component.literal("  " + valueStr).withStyle(ChatFormatting.GREEN));
            }
        }
        
        // Ability
        if (customWeapon.hasAbility()) {
            WeaponAbility ability = customWeapon.getAbility();
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Ability: " + ability.getName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.literal(ability.getDescription())
                .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Mana Cost: " + ability.getManaCost() + 
                " | Cooldown: " + (ability.getCooldownTicks() / 20) + "s")
                .withStyle(ChatFormatting.BLUE));
        }
        
        // Passives
        if (customWeapon.hasPassives()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Passives:").withStyle(ChatFormatting.YELLOW));
            for (WeaponPassive passive : customWeapon.getPassives()) {
                tooltip.add(Component.literal("• " + passive.getName() + ": " + passive.getDescription())
                    .withStyle(ChatFormatting.GRAY));
            }
        }
        
        // Description/Lore
        if (!customWeapon.getDescription().isEmpty()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal(customWeapon.getDescription())
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        
        // Additional lore lines
        for (String lore : customWeapon.getLoreLines()) {
            tooltip.add(Component.literal(lore)
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        
        super.appendHoverText(stack, context, tooltip, flag);
    }
    
    private String formatStatName(StatType statType) {
        String name = statType.name().replace("_", " ");
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toLowerCase().toCharArray()) {
            if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else if (c == ' ') {
                result.append(c);
                capitalizeNext = true;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(customWeapon.getDisplayName())
            .withStyle(Style.EMPTY.withColor(customWeapon.getRarity().getColor()));
    }
}
