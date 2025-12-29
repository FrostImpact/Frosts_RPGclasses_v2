package net.frostimpact.rpgclasses_v2.mixin;

import net.frostimpact.rpgclasses_v2.skilltree.SkillTreeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Shadow
    private RecipeBookComponent recipeBookComponent;
    
    @Inject(method = "init", at = @At("TAIL"))
    private void addSkillTreeButton(CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen) (Object) this;
        
        // Position next to the recipe book button
        int leftPos = self.width / 2 - 88;
        int topPos = self.height / 2 - 83;
        
        // Recipe book button is at leftPos - 4, topPos + 10
        // We'll place our button below the recipe book button
        int buttonX = leftPos - 26;
        int buttonY = topPos + 32;
        
        Button skillTreeButton = Button.builder(
            Component.literal("S"),
            button -> openSkillTree()
        ).bounds(buttonX, buttonY, 20, 20).build();
        
        self.addRenderableWidget(skillTreeButton);
    }
    
    private void openSkillTree() {
        // For now, open with a default skill tree ID
        // In a real implementation, this would be based on the player's class
        Minecraft.getInstance().setScreen(new SkillTreeScreen("warrior"));
    }
}
