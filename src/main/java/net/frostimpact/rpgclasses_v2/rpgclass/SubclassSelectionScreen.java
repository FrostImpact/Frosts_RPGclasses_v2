package net.frostimpact.rpgclasses_v2.rpgclass;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Screen for selecting a subclass
 */
public class SubclassSelectionScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubclassSelectionScreen.class);
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 350;
    private static final int CARD_WIDTH = 180;
    private static final int CARD_HEIGHT = 80;
    private static final int CARD_SPACING = 10;
    private static final int GRID_COLUMNS = 2;
    private static final int GRID_START_Y = 100;
    
    private final String parentClassId;
    private final int playerClassLevel;
    private final List<RPGClass> subclasses;
    private RPGClass selectedSubclass;
    
    public SubclassSelectionScreen(String parentClassId, int playerClassLevel) {
        super(Component.literal("Choose Your Specialization"));
        this.parentClassId = parentClassId;
        this.playerClassLevel = playerClassLevel;
        this.subclasses = ClassRegistry.getSubclasses(parentClassId);
    }
    
    @Override
    protected void init() {
        super.init();
        
        LOGGER.debug("Initialized subclass selection screen for class: {}", parentClassId);
        
        // Calculate grid layout
        int cols = GRID_COLUMNS;
        int startX = (this.width - (cols * CARD_WIDTH + (cols - 1) * CARD_SPACING)) / 2;
        int startY = GRID_START_Y;
        
        // Add buttons for each subclass in a grid layout
        for (int i = 0; i < subclasses.size(); i++) {
            final RPGClass subclass = subclasses.get(i);
            int col = i % cols;
            int row = i / cols;
            
            int buttonX = startX + col * (CARD_WIDTH + CARD_SPACING);
            int buttonY = startY + row * (CARD_HEIGHT + CARD_SPACING);
            
            // Check if player meets level requirement
            boolean unlocked = playerClassLevel >= subclass.getRequiredLevel();
            String buttonText = subclass.getName();
            if (!unlocked) {
                buttonText += " (Lv " + subclass.getRequiredLevel() + ")";
            }
            
            Button button = Button.builder(
                Component.literal(buttonText),
                btn -> {
                    if (unlocked) {
                        onSubclassSelected(subclass);
                    }
                }
            ).bounds(buttonX, buttonY, CARD_WIDTH, CARD_HEIGHT).build();
            
            // Disable button if locked
            button.active = unlocked;
            
            this.addRenderableWidget(button);
        }
        
        // Add back button
        this.addRenderableWidget(Button.builder(
            Component.literal("Back"),
            button -> this.onClose()
        ).bounds((this.width - 100) / 2, this.height - 30, 100, 20).build());
    }
    
    private void onSubclassSelected(RPGClass subclass) {
        this.selectedSubclass = subclass;
        LOGGER.info("Selected subclass: {}", subclass.getName());
        // TODO: Apply subclass to player
        this.onClose();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render dark background
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // Draw main panel background
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = 20;
        
        // Panel background
        guiGraphics.fill(panelX, panelY, 
                       panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xDD000000);
        
        // Panel border
        guiGraphics.fill(panelX - 2, panelY - 2, 
                       panelX + PANEL_WIDTH + 2, panelY, 0xFF8844FF);
        guiGraphics.fill(panelX - 2, panelY - 2, 
                       panelX, panelY + PANEL_HEIGHT + 2, 0xFF8844FF);
        guiGraphics.fill(panelX + PANEL_WIDTH, panelY - 2, 
                       panelX + PANEL_WIDTH + 2, panelY + PANEL_HEIGHT + 2, 0xFF5522AA);
        guiGraphics.fill(panelX - 2, panelY + PANEL_HEIGHT, 
                       panelX + PANEL_WIDTH + 2, panelY + PANEL_HEIGHT + 2, 0xFF5522AA);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Draw title
        String title = "CHOOSE YOUR SPECIALIZATION";
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, titleWidth + 2, 32, 0xFF000000); // Shadow
        guiGraphics.drawString(this.font, title, (this.width - titleWidth) / 2, 30, 0xFFAA44FF);
        
        // Draw parent class name
        ClassRegistry.getClass(parentClassId).ifPresent(parentClass -> {
            String parentName = parentClass.getName() + " Specializations";
            int parentWidth = this.font.width(parentName);
            guiGraphics.drawString(this.font, parentName, 
                (this.width - parentWidth) / 2, 50, 0xFFCCCCCC);
        });
        
        // Draw player level info
        String levelInfo = "Your Class Level: " + playerClassLevel;
        int levelWidth = this.font.width(levelInfo);
        guiGraphics.drawString(this.font, levelInfo, 
            (this.width - levelWidth) / 2, 70, 0xFFFFDD44);
        
        // Draw selected subclass info
        if (selectedSubclass != null) {
            int infoStartY = this.height - 100;
            String desc = selectedSubclass.getDescription();
            if (desc != null && !desc.isEmpty()) {
                int descWidth = this.font.width(desc);
                guiGraphics.drawString(this.font, desc, 
                    (this.width - descWidth) / 2, infoStartY, 0xFF55FF55);
            }
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
