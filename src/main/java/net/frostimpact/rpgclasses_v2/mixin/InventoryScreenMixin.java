package net.frostimpact.rpgclasses_v2.mixin;

import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.skilltree.SkillTreeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Shadow
    protected abstract <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.narration.NarratableEntry & net.minecraft.client.gui.components.Renderable> T addRenderableWidget(T widget);

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

        this.addRenderableWidget(skillTreeButton);
    }

    private void openSkillTree() {
        // Get player's current class from their RPG data
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            var rpgData = mc.player.getData(ModAttachments.PLAYER_RPG);
            String currentClass = rpgData.getCurrentClass();

            // Default to warrior if no class is set
            if (currentClass.equals("NONE")) {
                currentClass = "warrior";
            }

            mc.setScreen(new SkillTreeScreen(currentClass));
        }
    }
}