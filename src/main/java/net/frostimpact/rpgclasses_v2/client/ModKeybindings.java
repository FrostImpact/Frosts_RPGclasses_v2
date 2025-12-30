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
    
    // Ability keybinds - ZXCV
    public static final KeyMapping ABILITY_1 = new KeyMapping(
        "key.rpgclasses_v2.ability_1",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_Z,
        CATEGORY
    );
    
    public static final KeyMapping ABILITY_2 = new KeyMapping(
        "key.rpgclasses_v2.ability_2",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_X,
        CATEGORY
    );
    
    public static final KeyMapping ABILITY_3 = new KeyMapping(
        "key.rpgclasses_v2.ability_3",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_C,
        CATEGORY
    );
    
    public static final KeyMapping ABILITY_4 = new KeyMapping(
        "key.rpgclasses_v2.ability_4",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        CATEGORY
    );
}
