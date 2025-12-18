package net.frostimpact.rpgclasses_v2.rpgclass;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Placeholder GUI for class selection
 */
public class ClassSelectionScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassSelectionScreen.class);
    private final List<RPGClass> availableClasses;
    private RPGClass selectedClass;
    
    public ClassSelectionScreen() {
        super(Component.literal("Select Your Class"));
        this.availableClasses = new ArrayList<>();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Load available classes
        availableClasses.clear();
        for (String classId : ClassRegistry.getAllClassIds()) {
            ClassRegistry.getClass(classId).ifPresent(availableClasses::add);
        }
        
        LOGGER.debug("Initialized class selection screen with {} classes", availableClasses.size());
        
        // Add buttons for each class (placeholder)
        int startY = 80;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 25;
        
        for (int i = 0; i < availableClasses.size(); i++) {
            final RPGClass rpgClass = availableClasses.get(i);
            int buttonY = startY + (i * spacing);
            
            this.addRenderableWidget(Button.builder(
                Component.literal(rpgClass.getName()),
                button -> onClassSelected(rpgClass)
            ).bounds((this.width - buttonWidth) / 2, buttonY, buttonWidth, buttonHeight).build());
        }
        
        // Add close button
        this.addRenderableWidget(Button.builder(
            Component.literal("Close"),
            button -> this.onClose()
        ).bounds((this.width - 100) / 2, this.height - 40, 100, 20).build());
    }
    
    private void onClassSelected(RPGClass rpgClass) {
        this.selectedClass = rpgClass;
        LOGGER.info("Selected class: {}", rpgClass.getName());
        
        // TODO: Apply class to player
        // For now, just log the selection
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Draw title
        String title = "Select Your Class";
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, 
            (this.width - titleWidth) / 2, 20, 0xFFFFFFFF);
        
        // Draw selected class info
        if (selectedClass != null) {
            int infoStartY = this.height - 120;
            
            String selectedText = "Selected: " + selectedClass.getName();
            int selectedWidth = this.font.width(selectedText);
            guiGraphics.drawString(this.font, selectedText, 
                (this.width - selectedWidth) / 2, infoStartY, 0xFF55FF55);
            
            String desc = selectedClass.getDescription();
            int descWidth = this.font.width(desc);
            guiGraphics.drawString(this.font, desc, 
                (this.width - descWidth) / 2, infoStartY + 15, 0xFFAAAAAA);
        }
        
        // Draw instructions
        String instructions = "This is a placeholder class selection GUI - coming soon!";
        int instructionsWidth = this.font.width(instructions);
        guiGraphics.drawString(this.font, instructions, 
            (this.width - instructionsWidth) / 2, 50, 0xFF55FF55);
    }
    
    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
