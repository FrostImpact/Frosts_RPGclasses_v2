package net.frostimpact.rpgclasses_v2.client.event;

import net.frostimpact.rpgclasses_v2.RpgClassesMod;
import net.frostimpact.rpgclasses_v2.client.ModKeybindings;
import net.frostimpact.rpgclasses_v2.networking.ModMessages;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketUseAbility;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketMarksmanFocusMode;
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
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@EventBusSubscriber(modid = RpgClassesMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEvents.class);
    private static boolean wasInFocusMode = false;
    private static float originalFOV = 70.0f; // Default Minecraft FOV
    private static final float FOCUS_FOV_REDUCTION = 0.5f; // 50% FOV for zoom effect

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
        
        // Handle ability keybinds - send to server
        PlayerRPGData rpgData = mc.player.getData(ModAttachments.PLAYER_RPG);
        String currentClass = rpgData.getCurrentClass();
        
        if (ModKeybindings.ABILITY_1.consumeClick()) {
            LOGGER.debug("Ability 1 (Z) pressed for class: {}", currentClass);
            useAbility(1);
        }
        
        if (ModKeybindings.ABILITY_2.consumeClick()) {
            LOGGER.debug("Ability 2 (X) pressed for class: {}", currentClass);
            useAbility(2);
        }
        
        if (ModKeybindings.ABILITY_3.consumeClick()) {
            LOGGER.debug("Ability 3 (C) pressed for class: {}", currentClass);
            useAbility(3);
        }
        
        if (ModKeybindings.ABILITY_4.consumeClick()) {
            LOGGER.debug("Ability 4 (V) pressed for class: {}", currentClass);
            useAbility(4);
        }
    }
    
    /**
     * Detect FOCUS mode (Marksman holding SHIFT)
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide() && event.getEntity() == Minecraft.getInstance().player) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            
            PlayerRPGData rpgData = mc.player.getData(ModAttachments.PLAYER_RPG);
            String currentClass = rpgData.getCurrentClass();
            
            // Only Marksman class can use FOCUS mode
            if (currentClass != null && currentClass.equalsIgnoreCase("marksman")) {
                boolean isSneaking = mc.player.input.shiftKeyDown;
                boolean inFocusMode = rpgData.isInFocusMode();
                
                // State changed - notify server and update local state
                if (isSneaking && !inFocusMode) {
                    // Entering FOCUS mode
                    rpgData.setInFocusMode(true);
                    ModMessages.sendToServer(new PacketMarksmanFocusMode(true));
                    originalFOV = (float) mc.options.fov().get();
                    LOGGER.debug("Marksman entered FOCUS mode");
                } else if (!isSneaking && inFocusMode) {
                    // Exiting FOCUS mode
                    rpgData.setInFocusMode(false);
                    ModMessages.sendToServer(new PacketMarksmanFocusMode(false));
                    LOGGER.debug("Marksman exited FOCUS mode");
                }
                
                wasInFocusMode = inFocusMode;
            }
        }
    }
    
    /**
     * Lock movement while in FOCUS mode
     */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        PlayerRPGData rpgData = mc.player.getData(ModAttachments.PLAYER_RPG);
        String currentClass = rpgData.getCurrentClass();
        
        // Lock movement for Marksman in FOCUS mode
        if (currentClass != null && currentClass.equalsIgnoreCase("marksman") && rpgData.isInFocusMode()) {
            event.getInput().leftImpulse = 0;
            event.getInput().forwardImpulse = 0;
            event.getInput().jumping = false;
            // Allow looking around but not moving
        }
    }
    
    /**
     * Apply FOV zoom effect while in FOCUS mode
     */
    @SubscribeEvent
    public static void onComputeFOV(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        PlayerRPGData rpgData = mc.player.getData(ModAttachments.PLAYER_RPG);
        String currentClass = rpgData.getCurrentClass();
        
        // Apply FOV zoom for Marksman in FOCUS mode
        if (currentClass != null && currentClass.equalsIgnoreCase("marksman") && rpgData.isInFocusMode()) {
            double currentFOV = event.getFOV();
            event.setFOV(currentFOV * FOCUS_FOV_REDUCTION);
        }
    }
    
    private static void useAbility(int abilitySlot) {
        // Send to server for processing
        ModMessages.sendToServer(new PacketUseAbility(abilitySlot));
    }
}