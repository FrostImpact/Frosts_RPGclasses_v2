package net.frostimpact.rpgclasses_v2.rpgclass;

import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.minecraft.client.Minecraft;
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
    private static final int CARD_WIDTH = 120;
    private static final int CARD_HEIGHT = 140;
    private static final int CARD_SPACING = 15;
    private static final int GRID_COLUMNS = 3;
    private static final int GRID_START_Y = 100;
    
    private final List<RPGClass> availableClasses;
    private RPGClass selectedClass;
    private RPGClass hoveredClass;
    
    public ClassSelectionScreen() {
        super(Component.literal("Select Your Class"));
        this.availableClasses = new ArrayList<>();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Load only main classes (not subclasses)
        availableClasses.clear();
        availableClasses.addAll(ClassRegistry.getMainClasses());
        
        LOGGER.debug("Initialized class selection screen with {} main classes", availableClasses.size());
        
        // Calculate grid layout
        int cols = GRID_COLUMNS;
        int rows = (int) Math.ceil((double) availableClasses.size() / cols);
        int gridWidth = cols * CARD_WIDTH + (cols - 1) * CARD_SPACING;
        int startX = (this.width - gridWidth) / 2;
        int startY = GRID_START_Y;
        
        // Add buttons for each main class in a grid layout
        for (int i = 0; i < availableClasses.size(); i++) {
            final RPGClass rpgClass = availableClasses.get(i);
            int col = i % cols;
            int row = i / cols;
            
            int buttonX = startX + col * (CARD_WIDTH + CARD_SPACING);
            int buttonY = startY + row * (CARD_HEIGHT + CARD_SPACING);
            
            this.addRenderableWidget(Button.builder(
                Component.literal(rpgClass.getName()),
                button -> onClassClicked(rpgClass)
            ).bounds(buttonX, buttonY, CARD_WIDTH, CARD_HEIGHT).build());
        }
        
        // Add close button at bottom
        this.addRenderableWidget(Button.builder(
            Component.literal("Close"),
            button -> this.onClose()
        ).bounds((this.width - 100) / 2, this.height - 30, 100, 20).build());
    }
    
    private void onClassClicked(RPGClass rpgClass) {
        this.selectedClass = rpgClass;
        LOGGER.info("Clicked on class: {}", rpgClass.getName());
        
        // Get player's class level
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            PlayerRPGData rpgData = mc.player.getData(ModAttachments.PLAYER_RPG);
            int classLevel = rpgData.getClassLevel();
            
            // Check if this class has subclasses
            List<RPGClass> subclasses = ClassRegistry.getSubclasses(rpgClass.getId());
            if (!subclasses.isEmpty()) {
                // Open subclass selection screen
                mc.setScreen(new SubclassSelectionScreen(rpgClass.getId(), classLevel));
            } else {
                // Direct selection if no subclasses
                confirmSelection(rpgClass);
            }
        }
    }
    
    private void confirmSelection(RPGClass rpgClass) {
        if (rpgClass != null) {
            LOGGER.info("Confirmed class selection: {}", rpgClass.getName());
            // TODO: Apply class to player via packet
            this.onClose();
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render dark background with gradient
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // Draw fancy title with glow effect
        String title = "⚔ SELECT YOUR CLASS ⚔";
        int titleWidth = this.font.width(title);
        int titleX = (this.width - titleWidth) / 2;
        int titleY = 30;
        
        // Title glow/shadow layers
        guiGraphics.drawString(this.font, title, titleX + 3, titleY + 3, 0x88000000);
        guiGraphics.drawString(this.font, title, titleX + 2, titleY + 2, 0xCC000000);
        guiGraphics.drawString(this.font, title, titleX + 1, titleY + 1, 0xFF222222);
        // Main title with gradient-like effect
        guiGraphics.drawString(this.font, title, titleX, titleY, 0xFFFFDD00);
        
        // Draw subtitle
        String subtitle = "Choose your path and forge your destiny";
        int subtitleWidth = this.font.width(subtitle);
        guiGraphics.drawString(this.font, subtitle, 
            (this.width - subtitleWidth) / 2, 55, 0xFFCCCCCC);
        
        // Draw fancy class cards
        int cols = GRID_COLUMNS;
        int gridWidth = cols * CARD_WIDTH + (cols - 1) * CARD_SPACING;
        int startX = (this.width - gridWidth) / 2;
        int startY = GRID_START_Y;
        
        for (int i = 0; i < availableClasses.size(); i++) {
            RPGClass rpgClass = availableClasses.get(i);
            int col = i % cols;
            int row = i / cols;
            
            int cardX = startX + col * (CARD_WIDTH + CARD_SPACING);
            int cardY = startY + row * (CARD_HEIGHT + CARD_SPACING);
            
            // Check if mouse is hovering over this card
            boolean isHovered = mouseX >= cardX && mouseX < cardX + CARD_WIDTH &&
                               mouseY >= cardY && mouseY < cardY + CARD_HEIGHT;
            
            if (isHovered) {
                hoveredClass = rpgClass;
            }
            
            // Draw fancy card background based on class type
            int cardColor = getClassColor(rpgClass.getId());
            int borderColor = isHovered ? 0xFFFFFFFF : cardColor;
            
            // Card shadow
            guiGraphics.fill(cardX + 3, cardY + 3, 
                           cardX + CARD_WIDTH + 3, cardY + CARD_HEIGHT + 3, 0x88000000);
            
            // Card background with gradient effect (darker at bottom)
            guiGraphics.fill(cardX, cardY, 
                           cardX + CARD_WIDTH, cardY + CARD_HEIGHT / 2, 
                           0xEE000000 | (cardColor & 0x00FFFFFF) >> 2);
            guiGraphics.fill(cardX, cardY + CARD_HEIGHT / 2, 
                           cardX + CARD_WIDTH, cardY + CARD_HEIGHT, 0xCC000000);
            
            // Card border (thicker if hovered)
            int borderThickness = isHovered ? 3 : 2;
            for (int t = 0; t < borderThickness; t++) {
                // Top
                guiGraphics.fill(cardX - t, cardY - t, 
                               cardX + CARD_WIDTH + t, cardY - t + 1, borderColor);
                // Bottom
                guiGraphics.fill(cardX - t, cardY + CARD_HEIGHT + t - 1, 
                               cardX + CARD_WIDTH + t, cardY + CARD_HEIGHT + t, borderColor);
                // Left
                guiGraphics.fill(cardX - t, cardY - t, 
                               cardX - t + 1, cardY + CARD_HEIGHT + t, borderColor);
                // Right
                guiGraphics.fill(cardX + CARD_WIDTH + t - 1, cardY - t, 
                               cardX + CARD_WIDTH + t, cardY + CARD_HEIGHT + t, borderColor);
            }
            
            // Draw class icon placeholder (colored square)
            int iconSize = 48;
            int iconX = cardX + (CARD_WIDTH - iconSize) / 2;
            int iconY = cardY + 15;
            guiGraphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, cardColor);
            guiGraphics.fill(iconX, iconY, iconX + iconSize, iconY + 2, 0xFFFFFFFF);
            guiGraphics.fill(iconX, iconY, iconX + 2, iconY + iconSize, 0xFFFFFFFF);
            
            // Draw class name
            String className = rpgClass.getName();
            int nameWidth = this.font.width(className);
            int nameX = cardX + (CARD_WIDTH - nameWidth) / 2;
            int nameY = cardY + 75;
            guiGraphics.drawString(this.font, className, nameX + 1, nameY + 1, 0xFF000000);
            guiGraphics.drawString(this.font, className, nameX, nameY, 0xFFFFFFFF);
            
            // Draw subclass indicator
            int subclassCount = ClassRegistry.getSubclasses(rpgClass.getId()).size();
            if (subclassCount > 0) {
                String subclassText = "+" + subclassCount + " specs";
                int subWidth = this.font.width(subclassText);
                int subX = cardX + (CARD_WIDTH - subWidth) / 2;
                guiGraphics.drawString(this.font, subclassText, subX, cardY + 95, 0xFFAAAAAA);
            }
            
            // Draw hover effect symbol
            if (isHovered) {
                String hoverSymbol = "▶";
                guiGraphics.drawString(this.font, hoverSymbol, cardX + 5, cardY + 5, 0xFFFFDD00);
            }
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Draw hovered class info panel at bottom
        if (hoveredClass != null) {
            int panelY = this.height - 80;
            int panelWidth = 500;
            int panelHeight = 45;
            int panelX = (this.width - panelWidth) / 2;
            
            // Panel background
            guiGraphics.fill(panelX, panelY, 
                           panelX + panelWidth, panelY + panelHeight, 0xEE000000);
            
            // Panel border
            int panelBorderColor = getClassColor(hoveredClass.getId());
            guiGraphics.fill(panelX, panelY, 
                           panelX + panelWidth, panelY + 2, panelBorderColor);
            guiGraphics.fill(panelX, panelY, 
                           panelX + 2, panelY + panelHeight, panelBorderColor);
            guiGraphics.fill(panelX + panelWidth - 2, panelY, 
                           panelX + panelWidth, panelY + panelHeight, panelBorderColor);
            guiGraphics.fill(panelX, panelY + panelHeight - 2, 
                           panelX + panelWidth, panelY + panelHeight, panelBorderColor);
            
            // Class description
            String desc = hoveredClass.getDescription();
            if (desc != null && !desc.isEmpty()) {
                // Word wrap the description if needed
                List<String> lines = new ArrayList<>();
                String[] words = desc.split(" ");
                StringBuilder currentLine = new StringBuilder();
                int maxWidth = panelWidth - 20;
                
                for (String word : words) {
                    String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                    if (this.font.width(testLine) <= maxWidth) {
                        if (currentLine.length() > 0) currentLine.append(" ");
                        currentLine.append(word);
                    } else {
                        if (currentLine.length() > 0) {
                            lines.add(currentLine.toString());
                            currentLine = new StringBuilder(word);
                        }
                    }
                }
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                
                // Draw description lines
                int textY = panelY + 10;
                for (String line : lines) {
                    int textX = panelX + 10;
                    guiGraphics.drawString(this.font, line, textX, textY, 0xFFFFFFFF);
                    textY += 12;
                    if (textY > panelY + panelHeight - 10) break; // Don't overflow
                }
            }
        }
        
        hoveredClass = null; // Reset for next frame
    }
    
    private int getClassColor(String classId) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> 0xFFDD4444;
            case "mage" -> 0xFF4444DD;
            case "rogue" -> 0xFF44DD44;
            case "ranger" -> 0xFF44DD44;
            case "tank" -> 0xFFDDDD44;
            case "priest" -> 0xFFFFFFAA;
            default -> 0xFFAAAAAA;
        };
    }
    
    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
