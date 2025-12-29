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
 * Enhanced GUI for class selection with improved visuals
 */
public class ClassSelectionScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassSelectionScreen.class);
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 350;
    private static final int PANEL_Y_OFFSET = 20;
    private static final int CARD_WIDTH = 180;
    private static final int CARD_HEIGHT = 80;
    private static final int CARD_SPACING = 10;
    private static final int GRID_COLUMNS = 2;
    private static final int GRID_START_Y = 80;
    private static final int CORNER_SIZE = 10;
    private static final int CORNER_COLOR = 0xFFFFDD00;
    private static final int CONFIRM_BUTTON_WIDTH = 150;
    private static final int CONFIRM_BUTTON_HEIGHT = 25;
    private static final int CONFIRM_BUTTON_BOTTOM_OFFSET = 60;
    private static final int CLOSE_BUTTON_WIDTH = 100;
    private static final int CLOSE_BUTTON_HEIGHT = 20;
    private static final int CLOSE_BUTTON_BOTTOM_OFFSET = 30;
    private static final int INFO_PANEL_BOTTOM_OFFSET = 120;
    
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
        
        // Calculate grid layout
        int cols = GRID_COLUMNS;
        int startX = (this.width - (cols * CARD_WIDTH + (cols - 1) * CARD_SPACING)) / 2;
        int startY = GRID_START_Y;
        
        // Add buttons for each class in a grid layout
        for (int i = 0; i < availableClasses.size(); i++) {
            final RPGClass rpgClass = availableClasses.get(i);
            int col = i % cols;
            int row = i / cols;
            
            int buttonX = startX + col * (CARD_WIDTH + CARD_SPACING);
            int buttonY = startY + row * (CARD_HEIGHT + CARD_SPACING);
            
            this.addRenderableWidget(Button.builder(
                Component.literal(rpgClass.getName()),
                button -> onClassSelected(rpgClass)
            ).bounds(buttonX, buttonY, CARD_WIDTH, CARD_HEIGHT).build());
        }
        
        // Add confirm button
        this.addRenderableWidget(Button.builder(
            Component.literal("Confirm Selection"),
            button -> confirmSelection()
        ).bounds((this.width - CONFIRM_BUTTON_WIDTH) / 2, this.height - CONFIRM_BUTTON_BOTTOM_OFFSET, 
                CONFIRM_BUTTON_WIDTH, CONFIRM_BUTTON_HEIGHT).build());
        
        // Add close button
        this.addRenderableWidget(Button.builder(
            Component.literal("Close"),
            button -> this.onClose()
        ).bounds((this.width - CLOSE_BUTTON_WIDTH) / 2, this.height - CLOSE_BUTTON_BOTTOM_OFFSET, 
                CLOSE_BUTTON_WIDTH, CLOSE_BUTTON_HEIGHT).build());
    }
    
    private void onClassSelected(RPGClass rpgClass) {
        this.selectedClass = rpgClass;
        LOGGER.info("Selected class: {}", rpgClass.getName());
    }
    
    private void confirmSelection() {
        if (selectedClass != null) {
            LOGGER.info("Confirmed class selection: {}", selectedClass.getName());
            // TODO: Apply class to player via packet
            this.onClose();
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render dark background
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // Draw main panel background
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = PANEL_Y_OFFSET;
        
        // Panel background with gradient effect
        guiGraphics.fill(panelX, panelY, 
                       panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xDD000000);
        
        // Panel border with glow effect
        guiGraphics.fill(panelX - 2, panelY - 2, 
                       panelX + PANEL_WIDTH + 2, panelY, 0xFF4488FF); // Top
        guiGraphics.fill(panelX - 2, panelY - 2, 
                       panelX, panelY + PANEL_HEIGHT + 2, 0xFF4488FF); // Left
        guiGraphics.fill(panelX + PANEL_WIDTH, panelY - 2, 
                       panelX + PANEL_WIDTH + 2, panelY + PANEL_HEIGHT + 2, 0xFF2244AA); // Right
        guiGraphics.fill(panelX - 2, panelY + PANEL_HEIGHT, 
                       panelX + PANEL_WIDTH + 2, panelY + PANEL_HEIGHT + 2, 0xFF2244AA); // Bottom
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Draw fancy title with shadow
        String title = "SELECT YOUR CLASS";
        int titleWidth = this.font.width(title);
        int titleX = (this.width - titleWidth) / 2;
        int titleY = 30;
        
        // Title shadow
        guiGraphics.drawString(this.font, title, titleX + 2, titleY + 2, 0xFF000000);
        // Title text with gradient color
        guiGraphics.drawString(this.font, title, titleX, titleY, 0xFFFFDD00);
        
        // Draw subtitle
        String subtitle = "Choose wisely, for this will shape your destiny";
        int subtitleWidth = this.font.width(subtitle);
        guiGraphics.drawString(this.font, subtitle, 
            (this.width - subtitleWidth) / 2, 50, 0xFFAAAAAA);
        
        // Draw selected class info panel
        if (selectedClass != null) {
            int infoStartY = this.height - INFO_PANEL_BOTTOM_OFFSET;
            int infoPanelWidth = 380;
            int infoPanelHeight = 50;
            int infoPanelX = (this.width - infoPanelWidth) / 2;
            
            // Info panel background
            guiGraphics.fill(infoPanelX, infoStartY - 5, 
                           infoPanelX + infoPanelWidth, infoStartY + infoPanelHeight, 0xEE000000);
            
            // Info panel border
            guiGraphics.fill(infoPanelX, infoStartY - 5, 
                           infoPanelX + infoPanelWidth, infoStartY - 4, 0xFF55FF55); // Top
            guiGraphics.fill(infoPanelX, infoStartY - 5, 
                           infoPanelX + 1, infoStartY + infoPanelHeight, 0xFF55FF55); // Left
            guiGraphics.fill(infoPanelX + infoPanelWidth - 1, infoStartY - 5, 
                           infoPanelX + infoPanelWidth, infoStartY + infoPanelHeight, 0xFF33AA33); // Right
            guiGraphics.fill(infoPanelX, infoStartY + infoPanelHeight - 1, 
                           infoPanelX + infoPanelWidth, infoStartY + infoPanelHeight, 0xFF33AA33); // Bottom
            
            // Selected class name with icon
            String className = selectedClass.getName() != null ? selectedClass.getName() : "Unknown";
            String selectedText = "Selected: " + className;
            int selectedWidth = this.font.width(selectedText);
            guiGraphics.drawString(this.font, selectedText, 
                (this.width - selectedWidth) / 2, infoStartY + 5, 0xFF55FF55);
            
            // Class description
            String desc = selectedClass.getDescription();
            if (desc != null && !desc.isEmpty()) {
                int descWidth = this.font.width(desc);
                guiGraphics.drawString(this.font, desc, 
                    (this.width - descWidth) / 2, infoStartY + 20, 0xFFCCCCCC);
            }
            
            // Additional flavor text
            String flavorText = "\"Forge your legend as a " + className + "\"";
            int flavorWidth = this.font.width(flavorText);
            guiGraphics.drawString(this.font, flavorText, 
                (this.width - flavorWidth) / 2, infoStartY + 35, 0xFF888888);
        } else {
            // Draw hint when no class selected
            String hint = "Select a class to view details";
            int hintWidth = this.font.width(hint);
            guiGraphics.drawString(this.font, hint, 
                (this.width - hintWidth) / 2, this.height - 100, 0xFF888888);
        }
        
        // Draw decorative corners
        drawDecorativeCorners(guiGraphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
    }
    
    private void drawDecorativeCorners(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // Top-left corner
        guiGraphics.fill(x, y, x + CORNER_SIZE, y + 1, CORNER_COLOR);
        guiGraphics.fill(x, y, x + 1, y + CORNER_SIZE, CORNER_COLOR);
        
        // Top-right corner
        guiGraphics.fill(x + width - CORNER_SIZE, y, x + width, y + 1, CORNER_COLOR);
        guiGraphics.fill(x + width - 1, y, x + width, y + CORNER_SIZE, CORNER_COLOR);
        
        // Bottom-left corner
        guiGraphics.fill(x, y + height - 1, x + CORNER_SIZE, y + height, CORNER_COLOR);
        guiGraphics.fill(x, y + height - CORNER_SIZE, x + 1, y + height, CORNER_COLOR);
        
        // Bottom-right corner
        guiGraphics.fill(x + width - CORNER_SIZE, y + height - 1, x + width, y + height, CORNER_COLOR);
        guiGraphics.fill(x + width - 1, y + height - CORNER_SIZE, x + width, y + height, CORNER_COLOR);
    }
    
    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
