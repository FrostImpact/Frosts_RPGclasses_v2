package net.frostimpact.rpgclasses_v2.client.event;

import net.frostimpact.rpgclasses_v2.RpgClassesMod;
import net.frostimpact.rpgclasses_v2.client.ModKeybindings;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatAllocationScreen;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatsDropdownOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@EventBusSubscriber(modid = RpgClassesMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEvents.class);

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        
        // Only handle when no screen is open and player exists
        if (mc.screen != null || mc.player == null) {
            return;
        }
        
        if (ModKeybindings.TOGGLE_STATS.consumeClick()) {
            LOGGER.debug("Stats keybind pressed");
            StatsDropdownOverlay.toggleDropdown();
        }
        
        if (ModKeybindings.OPEN_STAT_ALLOCATION.consumeClick()) {
            LOGGER.debug("Stat allocation keybind pressed");
            mc.setScreen(new StatAllocationScreen());
        }
        
        // Handle ability keybinds
        PlayerRPGData rpgData = mc.player.getData(ModAttachments.PLAYER_RPG);
        String currentClass = rpgData.getCurrentClass();
        
        if (ModKeybindings.ABILITY_1.consumeClick()) {
            LOGGER.debug("Ability 1 (Z) pressed for class: {}", currentClass);
            useAbility(mc, rpgData, 1, currentClass);
        }
        
        if (ModKeybindings.ABILITY_2.consumeClick()) {
            LOGGER.debug("Ability 2 (X) pressed for class: {}", currentClass);
            useAbility(mc, rpgData, 2, currentClass);
        }
        
        if (ModKeybindings.ABILITY_3.consumeClick()) {
            LOGGER.debug("Ability 3 (C) pressed for class: {}", currentClass);
            useAbility(mc, rpgData, 3, currentClass);
        }
        
        if (ModKeybindings.ABILITY_4.consumeClick()) {
            LOGGER.debug("Ability 4 (V) pressed for class: {}", currentClass);
            useAbility(mc, rpgData, 4, currentClass);
        }
    }
    
    private static void useAbility(Minecraft mc, PlayerRPGData rpgData, int abilitySlot, String currentClass) {
        if (currentClass == null || currentClass.equals("NONE")) {
            mc.player.displayClientMessage(
                    Component.literal("§cYou need to select a class first!"), true);
            return;
        }
        
        String abilityName = getAbilityName(currentClass, abilitySlot);
        int manaCost = getAbilityManaCost(currentClass, abilitySlot);
        String abilityId = currentClass.toLowerCase() + "_ability_" + abilitySlot;
        
        // Check cooldown
        int cooldown = rpgData.getAbilityCooldown(abilityId);
        if (cooldown > 0) {
            mc.player.displayClientMessage(
                    Component.literal("§e" + abilityName + " §7is on cooldown (§c" + (cooldown / 20) + "s§7)"), true);
            return;
        }
        
        // Check mana
        if (rpgData.getMana() < manaCost) {
            mc.player.displayClientMessage(
                    Component.literal("§9Not enough mana for §b" + abilityName + " §7(Need §3" + manaCost + "§7)"), true);
            return;
        }
        
        // TODO: Send ability use packet to server for actual effect
        // For now, just show the ability was used
        mc.player.displayClientMessage(
                Component.literal("§a" + abilityName + " §7activated!"), true);
    }
    
    private static String getAbilityName(String classId, int slot) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> switch (slot) {
                case 1 -> "Power Strike";
                case 2 -> "Battle Cry";
                case 3 -> "Whirlwind";
                case 4 -> "Berserker Rage";
                default -> "Unknown";
            };
            case "mage" -> switch (slot) {
                case 1 -> "Fireball";
                case 2 -> "Frost Nova";
                case 3 -> "Arcane Shield";
                case 4 -> "Meteor Storm";
                default -> "Unknown";
            };
            case "rogue" -> switch (slot) {
                case 1 -> "Backstab";
                case 2 -> "Smoke Bomb";
                case 3 -> "Fan of Knives";
                case 4 -> "Shadow Dance";
                default -> "Unknown";
            };
            case "ranger" -> switch (slot) {
                case 1 -> "Precise Shot";
                case 2 -> "Multi-Shot";
                case 3 -> "Trap";
                case 4 -> "Rain of Arrows";
                default -> "Unknown";
            };
            case "tank" -> switch (slot) {
                case 1 -> "Shield Bash";
                case 2 -> "Taunt";
                case 3 -> "Iron Skin";
                case 4 -> "Fortress";
                default -> "Unknown";
            };
            case "priest" -> switch (slot) {
                case 1 -> "Holy Light";
                case 2 -> "Blessing";
                case 3 -> "Smite";
                case 4 -> "Divine Intervention";
                default -> "Unknown";
            };
            default -> "Ability " + slot;
        };
    }
    
    private static int getAbilityManaCost(String classId, int slot) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> switch (slot) {
                case 1 -> 20;
                case 2 -> 30;
                case 3 -> 40;
                case 4 -> 60;
                default -> 0;
            };
            case "mage" -> switch (slot) {
                case 1 -> 25;
                case 2 -> 35;
                case 3 -> 40;
                case 4 -> 80;
                default -> 0;
            };
            case "rogue" -> switch (slot) {
                case 1 -> 15;
                case 2 -> 20;
                case 3 -> 30;
                case 4 -> 50;
                default -> 0;
            };
            case "ranger" -> switch (slot) {
                case 1 -> 15;
                case 2 -> 25;
                case 3 -> 20;
                case 4 -> 60;
                default -> 0;
            };
            case "tank" -> switch (slot) {
                case 1 -> 15;
                case 2 -> 10;
                case 3 -> 25;
                case 4 -> 40;
                default -> 0;
            };
            case "priest" -> switch (slot) {
                case 1 -> 30;
                case 2 -> 25;
                case 3 -> 35;
                case 4 -> 80;
                default -> 0;
            };
            default -> 0;
        };
    }
}