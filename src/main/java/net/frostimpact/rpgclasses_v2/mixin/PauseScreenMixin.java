package net.frostimpact.rpgclasses_v2.mixin;

import net.frostimpact.rpgclasses_v2.rpg.stats.StatsDropdownOverlay;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {
    @Shadow
    protected abstract <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.narration.NarratableEntry & net.minecraft.client.gui.components.Renderable> T addRenderableWidget(T widget);

    @Inject(method = "init", at = @At("TAIL"))
    private void addStatsButton(CallbackInfo ci) {
        PauseScreen self = (PauseScreen) (Object) this;
        
        // Add stats toggle button in the top-right corner
        int buttonX = self.width - 25;
        int buttonY = 5;
        
        Button statsButton = Button.builder(
                Component.literal("S"), // Stats button
                button -> StatsDropdownOverlay.toggleDropdown()
        ).bounds(buttonX, buttonY, 20, 20).build();
        
        this.addRenderableWidget(statsButton);
    }
}
