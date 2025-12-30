package net.frostimpact.rpgclasses_v2.rpgclass;

import net.frostimpact.rpgclasses_v2.networking.ModMessages;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSelectClass;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for selecting a subclass with clickable detailed panels (like ClassSelectionScreen)
 */
public class SubclassSelectionScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubclassSelectionScreen.class);
    private static final int BUTTON_WIDTH = 280;
    private static final int BUTTON_HEIGHT = 35;
    private static final int SPACING = 8;
    
    private final String parentClassId;
    private final int playerClassLevel;
    private final List<RPGClass> subclasses;
    private final Screen parentScreen;
    private String currentClass = "NONE";
    private String hoveredSubclass = null;
    
    public SubclassSelectionScreen(String parentClassId, int playerClassLevel, Screen parentScreen) {
        super(Component.literal("Choose Your Specialization"));
        this.parentClassId = parentClassId;
        this.playerClassLevel = playerClassLevel;
        this.subclasses = ClassRegistry.getSubclasses(parentClassId);
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        if (Minecraft.getInstance().player != null) {
            PlayerRPGData rpgData = Minecraft.getInstance().player.getData(ModAttachments.PLAYER_RPG);
            currentClass = rpgData.getCurrentClass();
        }
        LOGGER.debug("Initialized subclass selection screen for class: {}", parentClassId);
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Override to prevent default blur
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Solid dark translucent background
        graphics.fill(0, 0, this.width, this.height, 0xC0000000);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Title
        ClassRegistry.getClass(parentClassId).ifPresent(parentClass -> {
            String icon = getClassIcon(parentClassId);
            int classColor = getClassColor(parentClassId);
            graphics.drawCenteredString(this.font, 
                    icon + " §l" + parentClass.getName().toUpperCase() + " SPECIALIZATIONS",
                    centerX, 20, classColor);
        });
        
        graphics.drawCenteredString(this.font, "§7Your Class Level: §f" + playerClassLevel,
                centerX, 40, 0xAAAAAA);
        graphics.drawCenteredString(this.font, "§7Current: §f" + currentClass,
                centerX, 55, 0xAAAAAA);
        
        // Render subclass buttons
        renderSubclassList(graphics, centerX, centerY, mouseX, mouseY);
        
        // Back button
        int backBtnWidth = 80;
        int backBtnHeight = 20;
        int backBtnX = centerX - backBtnWidth / 2;
        int backBtnY = this.height - 40;
        
        boolean backHovered = mouseX >= backBtnX && mouseX <= backBtnX + backBtnWidth &&
                mouseY >= backBtnY && mouseY <= backBtnY + backBtnHeight;
        
        graphics.fill(backBtnX, backBtnY, backBtnX + backBtnWidth, backBtnY + backBtnHeight,
                backHovered ? 0x80404040 : 0x80202020);
        drawBorder(graphics, backBtnX, backBtnY, backBtnWidth, backBtnHeight,
                backHovered ? 0xFFAAAAAA : 0xFF666666);
        graphics.drawCenteredString(this.font, backHovered ? "§e← Back" : "§7← Back",
                centerX, backBtnY + 6, backHovered ? 0xFFFF55 : 0x888888);
        
        // Instructions
        graphics.drawCenteredString(this.font, "§7Left-click for details - Right-click to select",
                centerX, this.height - 20, 0x888888);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderSubclassList(GuiGraphics graphics, int centerX, int centerY, int mouseX, int mouseY) {
        int startY = 80;
        hoveredSubclass = null;
        
        for (int i = 0; i < subclasses.size(); i++) {
            RPGClass subclass = subclasses.get(i);
            int y = startY + (i * (BUTTON_HEIGHT + SPACING));
            int x = centerX - BUTTON_WIDTH / 2;
            
            boolean isHovered = mouseX >= x && mouseX <= x + BUTTON_WIDTH &&
                    mouseY >= y && mouseY <= y + BUTTON_HEIGHT;
            boolean unlocked = playerClassLevel >= subclass.getRequiredLevel();
            boolean isCurrent = subclass.getId().equalsIgnoreCase(currentClass);
            
            if (isHovered) hoveredSubclass = subclass.getId();
            
            // Button background
            int bgColor;
            if (!unlocked) {
                bgColor = 0x60202020;
            } else if (isHovered) {
                bgColor = 0x80404040;
            } else {
                bgColor = 0x80202020;
            }
            graphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, bgColor);
            
            // Border with class color
            int classColor = getClassColor(subclass.getId());
            int borderColor;
            if (isCurrent) {
                borderColor = 0xFFFFD700; // Gold for current
            } else if (!unlocked) {
                borderColor = 0xFF444444; // Gray for locked
            } else if (isHovered) {
                borderColor = classColor | 0xFF000000;
            } else {
                borderColor = 0xFF666666;
            }
            drawBorder(graphics, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, borderColor);
            
            // Icon and name
            String icon = getClassIcon(subclass.getId());
            String name = subclass.getName();
            if (isCurrent) {
                name += " §7(Current)";
            } else if (!unlocked) {
                name += " §c(Lv " + subclass.getRequiredLevel() + " required)";
            }
            
            int textColor = unlocked ? classColor : 0x666666;
            graphics.drawString(this.font, icon + " §l" + name, x + 10, y + 5, textColor, false);
            
            // Description (truncated to fit)
            String desc = subclass.getDescription();
            if (desc != null && !desc.isEmpty()) {
                // Truncate description to fit within button
                int maxDescWidth = BUTTON_WIDTH - 20;
                if (this.font.width(desc) > maxDescWidth) {
                    while (this.font.width(desc + "...") > maxDescWidth && desc.length() > 10) {
                        desc = desc.substring(0, desc.length() - 1);
                    }
                    desc = desc + "...";
                }
                int descColor = unlocked ? 0x888888 : 0x555555;
                graphics.drawString(this.font, "§7" + desc, x + 10, y + 18, descColor, false);
            }
        }
    }
    
    private void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
    
    private String getClassIcon(String classId) {
        return AbilityUtils.getClassIcon(classId);
    }
    
    private int getClassColor(String classId) {
        return AbilityUtils.getClassColor(classId);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        
        // Check back button
        int backBtnWidth = 80;
        int backBtnHeight = 20;
        int backBtnX = centerX - backBtnWidth / 2;
        int backBtnY = this.height - 40;
        
        if (mouseX >= backBtnX && mouseX <= backBtnX + backBtnWidth &&
                mouseY >= backBtnY && mouseY <= backBtnY + backBtnHeight && button == 0) {
            if (parentScreen != null) {
                Minecraft.getInstance().setScreen(parentScreen);
            } else {
                this.onClose();
            }
            return true;
        }
        
        // Check subclass clicks
        int startY = 80;
        for (int i = 0; i < subclasses.size(); i++) {
            RPGClass subclass = subclasses.get(i);
            int y = startY + (i * (BUTTON_HEIGHT + SPACING));
            int x = centerX - BUTTON_WIDTH / 2;
            
            if (mouseX >= x && mouseX <= x + BUTTON_WIDTH &&
                    mouseY >= y && mouseY <= y + BUTTON_HEIGHT) {
                
                boolean unlocked = playerClassLevel >= subclass.getRequiredLevel();
                
                if (button == 0) {
                    // Left-click: Open detail screen
                    LOGGER.info("Opening detail screen for subclass: {}", subclass.getId());
                    Minecraft.getInstance().setScreen(new ClassDetailScreen(subclass, this));
                    return true;
                } else if (button == 1 && unlocked) {
                    // Right-click: Select directly
                    LOGGER.info("Selected subclass: {}", subclass.getName());
                    ModMessages.sendToServer(new PacketSelectClass(subclass.getId()));
                    this.onClose();
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
