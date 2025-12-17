package net.frostimpact.rpgclasses_v2.skilltree;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder GUI for skill tree display and interaction
 */
public class SkillTreeScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillTreeScreen.class);
    private final String skillTreeId;
    private SkillTree skillTree;
    
    public SkillTreeScreen(String skillTreeId) {
        super(Component.literal("Skill Tree"));
        this.skillTreeId = skillTreeId;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Load the skill tree
        skillTree = SkillTreeRegistry.getSkillTree(skillTreeId).orElse(null);
        
        if (skillTree == null) {
            LOGGER.error("Failed to load skill tree: {}", skillTreeId);
            return;
        }
        
        LOGGER.debug("Initialized skill tree screen for: {}", skillTree.getName());
        
        // TODO: Add buttons and interactive elements
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        if (skillTree == null) {
            // Draw error message
            String errorText = "Failed to load skill tree";
            int textWidth = this.font.width(errorText);
            guiGraphics.drawString(this.font, errorText, 
                (this.width - textWidth) / 2, this.height / 2, 0xFFFF5555);
            return;
        }
        
        // Draw title
        String title = skillTree.getName();
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, 
            (this.width - titleWidth) / 2, 20, 0xFFFFFFFF);
        
        // Draw description
        String desc = skillTree.getDescription();
        int descWidth = this.font.width(desc);
        guiGraphics.drawString(this.font, desc, 
            (this.width - descWidth) / 2, 35, 0xFFAAAAAA);
        
        // Draw placeholder content
        int startY = 60;
        int index = 0;
        
        for (SkillNode node : skillTree.getAllNodes()) {
            String skillText = String.format("%s (Lvl %d)", node.getName(), node.getMaxLevel());
            guiGraphics.drawString(this.font, skillText, 50, startY + (index * 15), 0xFFFFFF00);
            
            String skillDesc = node.getDescription();
            guiGraphics.drawString(this.font, skillDesc, 70, startY + (index * 15) + 10, 0xFF888888);
            
            index++;
            
            if (startY + (index * 25) > this.height - 50) {
                break; // Don't overflow screen
            }
        }
        
        // Draw instructions
        String instructions = "This is a placeholder skill tree GUI - coming soon!";
        int instructionsWidth = this.font.width(instructions);
        guiGraphics.drawString(this.font, instructions, 
            (this.width - instructionsWidth) / 2, this.height - 30, 0xFF55FF55);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
