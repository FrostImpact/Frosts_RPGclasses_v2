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

public class ClassSelectionScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassSelectionScreen.class);
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 400;
    private static final int BUTTON_HEIGHT = 30;
    private static final int SPACING = 5;

    private String currentClass = "NONE";
    private RPGClass selectedType = null;
    private String hoveredSpec = null;

    public ClassSelectionScreen() {
        super(Component.literal("Class Selection"));
    }

    @Override
    protected void init() {
        super.init();
        if (Minecraft.getInstance().player != null) {
            PlayerRPGData rpgData = Minecraft.getInstance().player.getData(ModAttachments.PLAYER_RPG);
            currentClass = rpgData.getCurrentClass();
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Override to prevent default blur
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Solid dark translucent background (matching old code style)
        graphics.fill(0, 0, this.width, this.height, 0xC0000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Title
        graphics.drawCenteredString(this.font, "§6§lCHOOSE YOUR CLASS",
                centerX, 20, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "§7Current: §f" + currentClass,
                centerX, 35, 0xAAAAAA);

        if (selectedType == null) {
            renderMainClasses(graphics, centerX, centerY, mouseX, mouseY);
        } else {
            renderSubclasses(graphics, centerX, centerY, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderMainClasses(GuiGraphics graphics, int centerX, int centerY, int mouseX, int mouseY) {
        List<RPGClass> mainClasses = ClassRegistry.getMainClasses();
        // Only show first 6 classes
        int displayCount = Math.min(6, mainClasses.size());
        int startY = centerY - (displayCount * (BUTTON_HEIGHT + SPACING)) / 2;
        int index = 0;

        for (int i = 0; i < displayCount; i++) {
            RPGClass rpgClass = mainClasses.get(i);
            int y = startY + (index * (BUTTON_HEIGHT + SPACING));
            int x = centerX - PANEL_WIDTH / 2;

            boolean isHovered = mouseX >= x && mouseX <= x + PANEL_WIDTH &&
                    mouseY >= y && mouseY <= y + BUTTON_HEIGHT;

            // Button background
            int bgColor = isHovered ? 0x80404040 : 0x80202020;
            graphics.fill(x, y, x + PANEL_WIDTH, y + BUTTON_HEIGHT, bgColor);

            // Border with class color
            int classColor = getClassColor(rpgClass.getId());
            int borderColor = classColor | 0xFF000000;
            drawInnerBorder(graphics, x, y, PANEL_WIDTH, BUTTON_HEIGHT, borderColor);

            // Icon and text
            String icon = getClassIcon(rpgClass.getId());
            graphics.drawString(this.font, icon + " §l" + rpgClass.getName(),
                    x + 10, y + 6, classColor, false);
            graphics.drawString(this.font, "§7" + rpgClass.getDescription(),
                    x + 10, y + 18, 0x888888, false);

            index++;
        }

        // Instructions
        graphics.drawCenteredString(this.font, "§7Left-click for class details - Right-click for specializations",
                centerX, this.height - 30, 0x888888);
    }

    private void renderSubclasses(GuiGraphics graphics, int centerX, int centerY, int mouseX, int mouseY) {
        // Back button
        int backButtonY = centerY - 150;
        int backButtonWidth = 80;
        int backButtonHeight = 20;
        int backButtonX = centerX - backButtonWidth / 2;

        boolean backButtonHovered = mouseX >= backButtonX && mouseX <= backButtonX + backButtonWidth &&
                mouseY >= backButtonY && mouseY <= backButtonY + backButtonHeight;

        int backBgColor = backButtonHovered ? 0x80303030 : 0x80101010;
        graphics.fill(backButtonX, backButtonY, backButtonX + backButtonWidth,
                backButtonY + backButtonHeight, backBgColor);
        drawInnerBorder(graphics, backButtonX, backButtonY, backButtonWidth, backButtonHeight,
                backButtonHovered ? 0xFF999999 : 0xFF444444);
        graphics.drawCenteredString(this.font, backButtonHovered ? "§e← Back" : "§7← Back",
                centerX, backButtonY + 6, backButtonHovered ? 0xFFFF55 : 0x888888);

        // Title for selected class
        String icon = getClassIcon(selectedType.getId());
        int classColor = getClassColor(selectedType.getId());
        graphics.drawCenteredString(this.font,
                icon + " §l" + selectedType.getName().toUpperCase(),
                centerX, centerY - 120, classColor);

        // Subclasses/Specializations
        List<RPGClass> subclasses = ClassRegistry.getSubclasses(selectedType.getId());

        // If no subclasses, show the base class as selectable
        if (subclasses.isEmpty()) {
            subclasses = new ArrayList<>();
            subclasses.add(selectedType);
        }

        int panelSpacing = 20;
        int totalWidth = (PANEL_WIDTH * subclasses.size()) + ((subclasses.size() - 1) * panelSpacing);
        int startX = centerX - totalWidth / 2;
        int panelY = centerY - 90;

        hoveredSpec = null;

        for (int i = 0; i < subclasses.size(); i++) {
            RPGClass spec = subclasses.get(i);
            int x = startX + (i * (PANEL_WIDTH + panelSpacing));

            boolean isHovered = mouseX >= x && mouseX <= x + PANEL_WIDTH &&
                    mouseY >= panelY && mouseY <= panelY + PANEL_HEIGHT;

            if (isHovered) hoveredSpec = spec.getId();
            boolean isCurrent = spec.getId().equals(currentClass);

            renderSpecPanel(graphics, spec, x, panelY, isHovered, isCurrent);
        }
    }

    private void renderSpecPanel(GuiGraphics graphics, RPGClass spec, int x, int y,
                                 boolean isHovered, boolean isCurrent) {
        // Background
        int bgColor = isHovered ? 0xD0202020 : 0xC0101010;
        graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, bgColor);

        // Border
        int classColor = getClassColor(spec.getId());
        int borderColor;
        if (isCurrent) {
            borderColor = 0xFFFFD700; // Gold for current class
        } else if (isHovered) {
            borderColor = classColor | 0xFF000000;
        } else {
            borderColor = 0xFF404040;
        }
        drawInnerBorder(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, borderColor);

        int contentX = x + 10;
        int contentY = y + 10;

        // Class name
        String displayName = spec.getName() + (isCurrent ? " §7(Current)" : "");
        graphics.drawString(this.font, "§l" + displayName,
                contentX, contentY, classColor, false);
        contentY += 15;

        // Description
        String description = spec.getDescription();
        List<String> wrappedDesc = wrapText(description, PANEL_WIDTH - 20);
        for (String line : wrappedDesc) {
            graphics.drawString(this.font, "§7" + line,
                    contentX, contentY, 0x888888, false);
            contentY += 10;
        }
        contentY += 10;

        // Base Stats section
        graphics.drawString(this.font, "§6Base Stats:",
                contentX, contentY, 0xFFAA00, false);
        contentY += 12;

        // Get and display base stats
        var baseStats = spec.getAllBaseStats();
        if (!baseStats.isEmpty()) {
            for (var entry : baseStats.entrySet()) {
                String statName = formatStatName(entry.getKey().name());
                double totalValue = entry.getValue().stream()
                        .mapToDouble(mod -> mod.getValue())
                        .sum();

                String statText = "  " + statName + ": +" +
                        (entry.getKey().isPercentage() ?
                                String.format("%.1f%%", totalValue) :
                                String.format("%.0f", totalValue));

                graphics.drawString(this.font, "§7" + statText,
                        contentX, contentY, 0x777777, false);
                contentY += 10;
            }
        } else {
            graphics.drawString(this.font, "§7  No base stats",
                    contentX, contentY, 0x777777, false);
            contentY += 10;
        }
        contentY += 5;

        // Placeholder for future abilities
        graphics.drawString(this.font, "§bAbilities: §7Coming Soon",
                contentX, contentY, 0x55AAFF, false);

        // Select button at bottom
        if (isHovered) {
            int buttonY = y + PANEL_HEIGHT - 35;
            int buttonWidth = 100;
            int buttonHeight = 25;
            int buttonX = x + PANEL_WIDTH / 2 - buttonWidth / 2;

            int selectColor = isCurrent ? 0xFF888888 : 0xFF008800;
            String selectText = isCurrent ? "§8§lCURRENT" : "§f§lSELECT";

            graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, selectColor);
            drawInnerBorder(graphics, buttonX, buttonY, buttonWidth, buttonHeight,
                    isCurrent ? 0xFF444444 : 0xFF00FF00);
            graphics.drawCenteredString(this.font, selectText,
                    x + PANEL_WIDTH / 2, buttonY + 8, 0xFFFFFF);
        }
    }

    private void drawInnerBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (this.font.width(testLine) <= maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) lines.add(currentLine.toString());
        return lines;
    }

    private String formatStatName(String statType) {
        return switch (statType) {
            case "MAX_HEALTH" -> "Max Health";
            case "MAX_MANA" -> "Max Mana";
            case "DAMAGE" -> "Damage";
            case "DEFENSE" -> "Defense";
            case "MOVE_SPEED" -> "Move Speed";
            case "ATTACK_SPEED" -> "Attack Speed";
            case "HEALTH_REGEN" -> "Health Regen";
            case "MANA_REGEN" -> "Mana Regen";
            case "COOLDOWN_REDUCTION" -> "Cooldown Reduction";
            default -> statType;
        };
    }

    private String getClassIcon(String classId) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> "⚔";
            case "mage" -> "✨";
            case "rogue" -> "🗡";
            case "ranger" -> "🏹";
            case "tank" -> "🛡";
            case "priest" -> "❤";
            default -> "⭐";
        };
    }

    private int getClassColor(String classId) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> 0xFF4444;
            case "mage" -> 0xAA00FF;
            case "rogue" -> 0x55FF55;
            case "ranger" -> 0x88DD44;
            case "tank" -> 0x55AAFF;
            case "priest" -> 0xFFAA00;
            default -> 0xAAAAAA;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (selectedType == null) {
            // Check main class clicks
            List<RPGClass> mainClasses = ClassRegistry.getMainClasses();
            // Only show first 6 classes
            int displayCount = Math.min(6, mainClasses.size());
            int startY = centerY - (displayCount * (BUTTON_HEIGHT + SPACING)) / 2;
            int index = 0;

            for (int i = 0; i < displayCount; i++) {
                RPGClass rpgClass = mainClasses.get(i);
                int y = startY + (index * (BUTTON_HEIGHT + SPACING));
                int x = centerX - PANEL_WIDTH / 2;

                if (mouseX >= x && mouseX <= x + PANEL_WIDTH &&
                        mouseY >= y && mouseY <= y + BUTTON_HEIGHT) {

                    if (button == 0) {
                        // Left-click: Open class detail screen
                        LOGGER.info("Opening detail screen for class: {}", rpgClass.getId());
                        Minecraft.getInstance().setScreen(new ClassDetailScreen(rpgClass, this));
                        return true;
                    } else if (button == 1) {
                        // Right-click: Open specializations (if they exist)
                        List<RPGClass> subclasses = ClassRegistry.getSubclasses(rpgClass.getId());
                        if (!subclasses.isEmpty()) {
                            selectedType = rpgClass;
                        }
                        return true;
                    }
                }
                index++;
            }
        } else {
            // Check back button
            int backButtonY = centerY - 150;
            int backButtonWidth = 80;
            int backButtonHeight = 20;
            int backButtonX = centerX - backButtonWidth / 2;

            if (mouseX >= backButtonX && mouseX <= backButtonX + backButtonWidth &&
                    mouseY >= backButtonY && mouseY <= backButtonY + backButtonHeight && button == 0) {
                selectedType = null;
                return true;
            }

            // Check specialization selection
            if (hoveredSpec != null && !hoveredSpec.equals(currentClass) && button == 0) {
                LOGGER.info("Selected class: {}", hoveredSpec);
                ModMessages.sendToServer(new PacketSelectClass(hoveredSpec));
                this.onClose();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}