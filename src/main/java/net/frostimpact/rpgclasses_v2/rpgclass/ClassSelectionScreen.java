package net.frostimpact.rpgclasses_v2.rpgclass;

import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        
        // Left-click selects the base class directly
        confirmSelection(rpgClass);
    }
    
    /**
     * Handle mouse clicks to support left-click for selection and right-click for specializations
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicking on a class card
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
            
            boolean isOnCard = mouseX >= cardX && mouseX < cardX + CARD_WIDTH &&
                              mouseY >= cardY && mouseY < cardY + CARD_HEIGHT;
            
            if (isOnCard) {
                if (button == 0) {
                    // Left-click: Select base class directly
                    confirmSelection(rpgClass);
                    return true;
                } else if (button == 1) {
                    // Right-click: Open specialization screen
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        PlayerRPGData rpgData = mc.player.getData(ModAttachments.PLAYER_RPG);
                        int classLevel = rpgData.getClassLevel();
                        
                        // Check if this class has subclasses
                        List<RPGClass> subclasses = ClassRegistry.getSubclasses(rpgClass.getId());
                        if (!subclasses.isEmpty()) {
                            // Open subclass selection screen
                            mc.setScreen(new SubclassSelectionScreen(rpgClass.getId(), classLevel));
                        }
                    }
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
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
        // Render solid black background without blur
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);
        
        // Draw title without blur
        String title = "⚔ SELECT YOUR CLASS ⚔";
        int titleWidth = this.font.width(title);
        int titleX = (this.width - titleWidth) / 2;
        int titleY = 30;
        
        // Title shadow (single layer, no blur)
        guiGraphics.drawString(this.font, title, titleX + 2, titleY + 2, 0xFF000000);
        // Main title
        guiGraphics.drawString(this.font, title, titleX, titleY, 0xFFFFDD00);
        
        // Draw subtitle without blur
        String subtitle = "Left-click to select - Right-click for specializations (if available)";
        int subtitleWidth = this.font.width(subtitle);
        guiGraphics.drawString(this.font, subtitle, 
            (this.width - subtitleWidth) / 2, 55, 0xFFCCCCCC);
        
        // Draw class cards without gray boxes
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
            
            // Draw card with class-specific color background (more vibrant)
            int cardColor = getClassColor(rpgClass.getId());
            int borderColor = isHovered ? 0xFFFFDD00 : cardColor;
            
            // Card shadow
            guiGraphics.fill(cardX + 3, cardY + 3, 
                           cardX + CARD_WIDTH + 3, cardY + CARD_HEIGHT + 3, 0x88000000);
            
            // Card background - use more vibrant class color (increased opacity)
            int transparentCardColor = (cardColor & 0x00FFFFFF) | 0x88000000;
            guiGraphics.fill(cardX, cardY, 
                           cardX + CARD_WIDTH, cardY + CARD_HEIGHT, transparentCardColor);
            
            // Card border (thicker if hovered, with glow effect)
            int borderThickness = isHovered ? 4 : 2;
            for (int t = 0; t < borderThickness; t++) {
                // Calculate alpha for glow effect (more transparent as distance increases)
                int alpha = isHovered ? (int)(255 * (1.0 - (float)t / borderThickness)) : 255;
                int glowBorderColor = (borderColor & 0x00FFFFFF) | (alpha << 24);
                
                // Top
                guiGraphics.fill(cardX - t, cardY - t, 
                               cardX + CARD_WIDTH + t, cardY - t + 1, glowBorderColor);
                // Bottom
                guiGraphics.fill(cardX - t, cardY + CARD_HEIGHT + t - 1, 
                               cardX + CARD_WIDTH + t, cardY + CARD_HEIGHT + t, glowBorderColor);
                // Left
                guiGraphics.fill(cardX - t, cardY - t, 
                               cardX - t + 1, cardY + CARD_HEIGHT + t, glowBorderColor);
                // Right
                guiGraphics.fill(cardX + CARD_WIDTH + t - 1, cardY - t, 
                               cardX + CARD_WIDTH + t, cardY + CARD_HEIGHT + t, glowBorderColor);
            }
            
            // Draw class icon using Minecraft items (48x48 rendered at 3x scale)
            ItemStack classIcon = getClassIcon(rpgClass.getId());
            int iconSize = 48;
            int iconX = cardX + (CARD_WIDTH - iconSize) / 2;
            int iconY = cardY + 15;
            
            // Render the item at 3x scale (16x16 -> 48x48)
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(iconX, iconY, 0);
            guiGraphics.pose().scale(3.0f, 3.0f, 1.0f);
            guiGraphics.renderItem(classIcon, 0, 0);
            guiGraphics.pose().popPose();
            
            // Draw class name with shadow (no blur)
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
            
            // Draw hover effect symbol (more prominent)
            if (isHovered) {
                String hoverSymbol = "▶";
                guiGraphics.drawString(this.font, hoverSymbol, cardX + 5, cardY + 5, 0xFFFFDD00);
                // Add a glow effect by drawing multiple times with slight offsets
                guiGraphics.drawString(this.font, hoverSymbol, cardX + 4, cardY + 5, 0x44FFDD00);
                guiGraphics.drawString(this.font, hoverSymbol, cardX + 6, cardY + 5, 0x44FFDD00);
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
    
    private ItemStack getClassIcon(String classId) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> new ItemStack(Items.IRON_SWORD);
            case "mage" -> new ItemStack(Items.BLAZE_ROD);
            case "rogue" -> new ItemStack(Items.SHEARS);  // Using shears for rogue (backstab theme)
            case "ranger" -> new ItemStack(Items.BOW);
            case "tank" -> new ItemStack(Items.SHIELD);
            case "priest" -> new ItemStack(Items.GOLDEN_APPLE);
            default -> new ItemStack(Items.NETHER_STAR);
        };
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
