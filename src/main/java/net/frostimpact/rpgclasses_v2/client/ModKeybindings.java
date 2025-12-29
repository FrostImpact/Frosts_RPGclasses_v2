package net.frostimpact.rpgclasses_v2.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Manages custom keybindings for the mod
 */
public class ModKeybindings {
    public static final String CATEGORY = "key.categories.rpgclasses_v2";
    
    public static final KeyMapping TOGGLE_STATS = new KeyMapping(
        "key.rpgclasses_v2.toggle_stats",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        CATEGORY
    );

    public static final KeyMapping OPEN_STAT_ALLOCATION = new KeyMapping(
        "key.rpgclasses_v2.open_stat_allocation",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_P,
        CATEGORY
    );
}
