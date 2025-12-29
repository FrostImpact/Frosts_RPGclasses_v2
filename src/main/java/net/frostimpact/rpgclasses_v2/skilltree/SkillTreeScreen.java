package net.frostimpact.rpgclasses_v2.skilltree;

import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.frostimpact.rpgclasses_v2.rpgclass.ClassRegistry;
import net.frostimpact.rpgclasses_v2.rpgclass.RPGClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced skill tree GUI with class selector and improved visuals
 */
public class SkillTreeScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillTreeScreen.class);
    private static final int NODE_SIZE = 40;
    private static final int NODE_SPACING = 80;
    private static final int LINE_COLOR = 0xFF888888;
    private static final int LINE_ACTIVE_COLOR = 0xFFFFDD00;
    private static final int TITLE_SPACE = 50;
    private static final int CLASS_PANEL_WIDTH = 180;
    private static final int CLASS_BUTTON_HEIGHT = 35;
    private static final int SUBCLASS_BUTTON_HEIGHT = 28;

    private String currentSkillTreeId;
    private SkillTree skillTree;
    private SkillNode hoveredNode;

    // Pan and zoom support
    private float offsetX = 0;
    private float offsetY = 0;
    private float zoom = 1.0f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.0f;
    private static final float ZOOM_STEP = 0.1f;

    // Class selector
    private List<RPGClass> mainClasses;
    private List<RPGClass> expandedSubclasses = new ArrayList<>();
    private String expandedClassId = null;

    // Switch message
    private String switchMessage = null;
    private long switchMessageTime = 0;
    private static final long MESSAGE_DURATION = 2000; // 2 seconds

    public SkillTreeScreen(String skillTreeId) {
        super(Component.literal("Skill Tree"));
        this.currentSkillTreeId = skillTreeId;
    }

    @Override
    protected void init() {
        super.init();

        // Load main classes
        mainClasses = ClassRegistry.getMainClasses();

        // Load the skill tree
        loadSkillTree(currentSkillTreeId);

        LOGGER.debug("Initialized skill tree screen for: {}", currentSkillTreeId);
    }

    private void loadSkillTree(String treeId) {
        skillTree = SkillTreeRegistry.getSkillTree(treeId).orElse(null);

        if (skillTree == null) {
            LOGGER.error("Failed to load skill tree: {}", treeId);
            return;
        }

        currentSkillTreeId = treeId;
        centerTree();
    }

    /**
     * Center the skill tree on the screen
     */
    private void centerTree() {
        if (skillTree == null) return;

        List<SkillNode> allNodes = skillTree.getAllNodes();
        if (allNodes.isEmpty()) return;

        // Find bounds of the tree
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (SkillNode node : allNodes) {
            minX = Math.min(minX, node.getX());
            maxX = Math.max(maxX, node.getX());
            minY = Math.min(minY, node.getY());
            maxY = Math.max(maxY, node.getY());
        }

        // Calculate tree dimensions
        int treeWidth = (maxX - minX) * NODE_SPACING + NODE_SIZE;
        int treeHeight = (maxY - minY) * NODE_SPACING + NODE_SIZE;

        // Center on screen (account for class panel on right)
        int availableWidth = this.width - CLASS_PANEL_WIDTH - 20;
        offsetX = (availableWidth - treeWidth) / 2.0f - minX * NODE_SPACING;
        offsetY = (this.height - treeHeight) / 2.0f - minY * NODE_SPACING + TITLE_SPACE;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render solid black background
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);

        if (skillTree == null) {
            // Draw error message
            String errorText = "Failed to load skill tree";
            int textWidth = this.font.width(errorText);
            guiGraphics.drawString(this.font, errorText,
                    (this.width - textWidth) / 2, this.height / 2, 0xFFFF5555, false);
            return;
        }

        // Get player class level
        Minecraft mc = Minecraft.getInstance();
        int classLevel = 1;
        if (mc.player != null) {
            PlayerRPGData rpgData = mc.player.getData(ModAttachments.PLAYER_RPG);
            classLevel = rpgData.getClassLevel();
        }

        // Draw title
        String title = skillTree.getName();
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title,
                (this.width - CLASS_PANEL_WIDTH - titleWidth) / 2, 20, 0xFFFFDD00, false);

        // Draw description
        String desc = "Level " + classLevel + " - " + skillTree.getDescription();
        int descWidth = this.font.width(desc);
        guiGraphics.drawString(this.font, desc,
                (this.width - CLASS_PANEL_WIDTH - descWidth) / 2, 40, 0xFFCCCCCC, false);

        // Save graphics state for zoom/pan
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(offsetX, offsetY, 0);
        guiGraphics.pose().scale(zoom, zoom, 1.0f);

        // Draw connection lines between nodes first
        List<SkillNode> allNodes = skillTree.getAllNodes();
        for (SkillNode node : allNodes) {
            for (String reqId : node.getRequirements()) {
                skillTree.getNode(reqId).ifPresent(reqNode -> {
                    int x1 = reqNode.getX() * NODE_SPACING + NODE_SIZE / 2;
                    int y1 = reqNode.getY() * NODE_SPACING + NODE_SIZE / 2;
                    int x2 = node.getX() * NODE_SPACING + NODE_SIZE / 2;
                    int y2 = node.getY() * NODE_SPACING + NODE_SIZE / 2;

                    drawLine(guiGraphics, x1, y1, x2, y2, LINE_COLOR);
                });
            }
        }

        // Reset hovered node
        hoveredNode = null;

        // Draw skill nodes
        for (SkillNode node : allNodes) {
            int nodeX = node.getX() * NODE_SPACING;
            int nodeY = node.getY() * NODE_SPACING;

            // Transform mouse coordinates for zoom/pan
            float transformedMouseX = (mouseX - offsetX) / zoom;
            float transformedMouseY = (mouseY - offsetY) / zoom;

            // Check if mouse is hovering over this node
            boolean isHovered = transformedMouseX >= nodeX && transformedMouseX < nodeX + NODE_SIZE &&
                    transformedMouseY >= nodeY && transformedMouseY < nodeY + NODE_SIZE;

            if (isHovered) {
                hoveredNode = node;
            }

            // Draw node
            drawSkillNode(guiGraphics, node, nodeX, nodeY, isHovered, classLevel);
        }

        // Restore graphics state
        guiGraphics.pose().popPose();

        // Draw class selector panel
        drawClassPanel(guiGraphics, mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw tooltip for hovered node
        if (hoveredNode != null) {
            drawNodeTooltip(guiGraphics, hoveredNode, mouseX, mouseY);
        }

        // Draw controls info
        String zoomInfo = String.format("Zoom: %.1fx (Scroll)", zoom);
        guiGraphics.drawString(this.font, zoomInfo, 10, this.height - 40, 0xFFFFDD00, false);

        String levelInfo = "Class Level: " + classLevel;
        guiGraphics.drawString(this.font, levelInfo, 10, this.height - 25, 0xFFFFDD00, false);

        // Draw switch message if active
        if (switchMessage != null && System.currentTimeMillis() - switchMessageTime < MESSAGE_DURATION) {
            int msgWidth = this.font.width(switchMessage);
            int msgX = (this.width - CLASS_PANEL_WIDTH - msgWidth) / 2;
            int msgY = 60;

            // Fade out effect
            long elapsed = System.currentTimeMillis() - switchMessageTime;
            float alpha = 1.0f - (float)elapsed / MESSAGE_DURATION;
            int alphaValue = (int)(alpha * 255);

            // Message background
            guiGraphics.fill(msgX - 5, msgY - 2, msgX + msgWidth + 5, msgY + 10, (alphaValue << 24) | 0x000000);

            // Message text
            guiGraphics.drawString(this.font, switchMessage, msgX, msgY, (alphaValue << 24) | 0x00FFDD, false);
        } else if (switchMessage != null) {
            switchMessage = null; // Clear message after duration
        }
    }

    private void drawSkillNode(GuiGraphics guiGraphics, SkillNode node, int x, int y, boolean isHovered, int classLevel) {
        boolean isUnlocked = classLevel >= node.getRequiredLevel();
        int nodeColor = isUnlocked ? 0xFF4488FF : 0xFF444444;
        int borderColor = isHovered ? 0xFFFFFFFF : (isUnlocked ? 0xFF6699FF : 0xFF666666);

        // Draw node shadow
        guiGraphics.fill(x + 2, y + 2, x + NODE_SIZE + 2, y + NODE_SIZE + 2, 0x88000000);

        // Draw node background
        guiGraphics.fill(x, y, x + NODE_SIZE, y + NODE_SIZE, nodeColor);

        // Draw node border
        int borderThickness = isHovered ? 3 : 2;
        for (int t = 0; t < borderThickness; t++) {
            guiGraphics.fill(x - t, y - t, x + NODE_SIZE + t, y - t + 1, borderColor);
            guiGraphics.fill(x - t, y + NODE_SIZE + t - 1, x + NODE_SIZE + t, y + NODE_SIZE + t, borderColor);
            guiGraphics.fill(x - t, y - t, x - t + 1, y + NODE_SIZE + t, borderColor);
            guiGraphics.fill(x + NODE_SIZE + t - 1, y - t, x + NODE_SIZE + t, y + NODE_SIZE + t, borderColor);
        }

        // Draw emoji icon
        String emoji = getSkillEmoji(node.getId());
        int emojiX = x + (NODE_SIZE - this.font.width(emoji)) / 2;
        int emojiY = y + (NODE_SIZE - 8) / 2;
        guiGraphics.drawString(this.font, emoji, emojiX, emojiY, 0xFFFFFFFF, false);

        // Draw level indicator
        if (node.getMaxLevel() > 1) {
            String levelText = "0/" + node.getMaxLevel();
            int levelWidth = this.font.width(levelText);
            guiGraphics.drawString(this.font, levelText,
                    x + (NODE_SIZE - levelWidth) / 2, y + NODE_SIZE + 2, 0xFFFFFFFF, false);
        }
    }

    private void drawClassPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int panelX = this.width - CLASS_PANEL_WIDTH - 10;
        int panelY = 60;
        int panelHeight = this.height - 70;

        // Panel background
        guiGraphics.fillGradient(panelX, panelY,
                panelX + CLASS_PANEL_WIDTH, panelY + panelHeight,
                0xEE000000, 0xCC000000);

        // Panel border
        guiGraphics.fill(panelX, panelY, panelX + CLASS_PANEL_WIDTH, panelY + 2, 0xFFFFDD00);
        guiGraphics.fill(panelX, panelY, panelX + 2, panelY + panelHeight, 0xFFFFDD00);
        guiGraphics.fill(panelX + CLASS_PANEL_WIDTH - 2, panelY,
                panelX + CLASS_PANEL_WIDTH, panelY + panelHeight, 0xFFAA8800);
        guiGraphics.fill(panelX, panelY + panelHeight - 2,
                panelX + CLASS_PANEL_WIDTH, panelY + panelHeight, 0xFFAA8800);

        // Panel title
        String panelTitle = "CLASSES";
        int titleWidth = this.font.width(panelTitle);
        guiGraphics.drawString(this.font, panelTitle,
                panelX + (CLASS_PANEL_WIDTH - titleWidth) / 2, panelY + 8, 0xFFFFDD00, false);

        // Draw class buttons
        int buttonY = panelY + 30;

        for (RPGClass rpgClass : mainClasses) {
            boolean isCurrentClass = rpgClass.getId().equals(currentSkillTreeId);
            boolean isExpanded = rpgClass.getId().equals(expandedClassId);

            int buttonX = panelX + 10;
            int buttonWidth = CLASS_PANEL_WIDTH - 20;

            // Check if mouse is over this button
            boolean isHovered = mouseX >= buttonX && mouseX < buttonX + buttonWidth &&
                    mouseY >= buttonY && mouseY < buttonY + CLASS_BUTTON_HEIGHT;

            // Draw class button
            drawClassButton(guiGraphics, rpgClass, buttonX, buttonY, buttonWidth, isCurrentClass, isHovered);

            buttonY += CLASS_BUTTON_HEIGHT + 5;

            // Draw subclasses if expanded
            if (isExpanded) {
                List<RPGClass> subclasses = ClassRegistry.getSubclasses(rpgClass.getId());
                for (RPGClass subclass : subclasses) {
                    boolean isCurrentSubclass = subclass.getId().equals(currentSkillTreeId);
                    boolean isSubHovered = mouseX >= buttonX + 10 && mouseX < buttonX + buttonWidth - 10 &&
                            mouseY >= buttonY && mouseY < buttonY + SUBCLASS_BUTTON_HEIGHT;

                    drawSubclassButton(guiGraphics, subclass, buttonX + 10, buttonY,
                            buttonWidth - 20, isCurrentSubclass, isSubHovered);

                    buttonY += SUBCLASS_BUTTON_HEIGHT + 3;
                }
            }
        }

        // Instructions
        String instructions = "Click: Switch tree";
        guiGraphics.drawString(this.font, instructions,
                panelX + 10, panelY + panelHeight - 30, 0xFF888888, false);

        String instructions2 = "Right-click: Specs";
        guiGraphics.drawString(this.font, instructions2,
                panelX + 10, panelY + panelHeight - 18, 0xFF888888, false);
    }

    private void drawClassButton(GuiGraphics guiGraphics, RPGClass rpgClass, int x, int y, int width, boolean isCurrent, boolean isHovered) {
        int classColor = getClassColor(rpgClass.getId());

        // Button background
        int bgColor = isCurrent ? (classColor | 0xAA000000) : (isHovered ? 0x66333333 : 0x44222222);
        guiGraphics.fill(x, y, x + width, y + CLASS_BUTTON_HEIGHT, bgColor);

        // Button border
        int borderColor = isCurrent ? classColor : (isHovered ? 0xFFFFFFFF : 0xFF666666);
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + CLASS_BUTTON_HEIGHT - 1, x + width, y + CLASS_BUTTON_HEIGHT, borderColor);
        guiGraphics.fill(x, y, x + 1, y + CLASS_BUTTON_HEIGHT, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + CLASS_BUTTON_HEIGHT, borderColor);

        // Class emoji and name
        String emoji = getClassEmoji(rpgClass.getId());
        guiGraphics.drawString(this.font, emoji, x + 5, y + 8, 0xFFFFFFFF, false);

        String name = rpgClass.getName();
        int textColor = isCurrent ? 0xFFFFDD00 : 0xFFFFFFFF;
        guiGraphics.drawString(this.font, name, x + 20, y + 8, textColor, false);

        // Expansion indicator
        boolean hasSubclasses = !ClassRegistry.getSubclasses(rpgClass.getId()).isEmpty();
        if (hasSubclasses) {
            String arrow = rpgClass.getId().equals(expandedClassId) ? "▼" : "▶";
            int arrowX = x + width - 15;
            guiGraphics.drawString(this.font, arrow, arrowX, y + 8, 0xFFCCCCCC, false);
        }

        // Current indicator
        if (isCurrent) {
            guiGraphics.drawString(this.font, "●", x + width - 10, y + 20, classColor, false);
        }
    }

    private void drawSubclassButton(GuiGraphics guiGraphics, RPGClass subclass, int x, int y, int width, boolean isCurrent, boolean isHovered) {
        // Button background
        int bgColor = isCurrent ? 0x88444444 : (isHovered ? 0x44333333 : 0x22222222);
        guiGraphics.fill(x, y, x + width, y + SUBCLASS_BUTTON_HEIGHT, bgColor);

        // Button border
        int borderColor = isCurrent ? 0xFFAA88FF : (isHovered ? 0xFFCCCCCC : 0xFF555555);
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y, x + 1, y + SUBCLASS_BUTTON_HEIGHT, borderColor);

        // Subclass name
        String name = "└ " + subclass.getName();
        int textColor = isCurrent ? 0xFFAA88FF : 0xFFCCCCCC;
        guiGraphics.drawString(this.font, name, x + 5, y + 6, textColor, false);

        // Current indicator
        if (isCurrent) {
            guiGraphics.drawString(this.font, "●", x + width - 8, y + 6, 0xFFAA88FF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelX = this.width - CLASS_PANEL_WIDTH - 10;
        int panelY = 60;
        int buttonY = panelY + 30;

        for (RPGClass rpgClass : mainClasses) {
            int buttonX = panelX + 10;
            int buttonWidth = CLASS_PANEL_WIDTH - 20;

            // Check if clicking on main class button
            if (mouseX >= buttonX && mouseX < buttonX + buttonWidth &&
                    mouseY >= buttonY && mouseY < buttonY + CLASS_BUTTON_HEIGHT) {

                if (button == 0) {
                    // Left-click: Switch to this class's skill tree
                    switchToSkillTree(rpgClass.getId(), rpgClass.getName());
                    return true;
                } else if (button == 1) {
                    // Right-click: Toggle expansion
                    if (rpgClass.getId().equals(expandedClassId)) {
                        expandedClassId = null;
                    } else {
                        expandedClassId = rpgClass.getId();
                    }
                    return true;
                }
            }

            buttonY += CLASS_BUTTON_HEIGHT + 5;

            // Check subclass buttons if expanded
            if (rpgClass.getId().equals(expandedClassId)) {
                List<RPGClass> subclasses = ClassRegistry.getSubclasses(rpgClass.getId());
                for (RPGClass subclass : subclasses) {
                    if (mouseX >= buttonX + 10 && mouseX < buttonX + buttonWidth - 10 &&
                            mouseY >= buttonY && mouseY < buttonY + SUBCLASS_BUTTON_HEIGHT) {

                        if (button == 0) {
                            // Switch to subclass skill tree
                            switchToSkillTree(subclass.getId(), subclass.getName());
                            return true;
                        }
                    }
                    buttonY += SUBCLASS_BUTTON_HEIGHT + 3;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void switchToSkillTree(String treeId, String className) {
        if (!treeId.equals(currentSkillTreeId)) {
            loadSkillTree(treeId);

            // Show switch message
            switchMessage = "Switched to " + className + " skill tree";
            switchMessageTime = System.currentTimeMillis();

            LOGGER.info("Switched to skill tree: {}", treeId);
        }
    }

    private void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));

        if (steps == 0) return;

        float xStep = (float) dx / steps;
        float yStep = (float) dy / steps;

        for (int i = 0; i <= steps; i++) {
            int x = (int) (x1 + xStep * i);
            int y = (int) (y1 + yStep * i);
            guiGraphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    private void drawNodeTooltip(GuiGraphics guiGraphics, SkillNode node, int mouseX, int mouseY) {
        List<String> tooltipLines = new ArrayList<>();

        tooltipLines.add(node.getName());
        tooltipLines.add("");
        tooltipLines.addAll(wrapText(node.getDescription(), 200));
        tooltipLines.add("");
        tooltipLines.add("Max Level: " + node.getMaxLevel());
        tooltipLines.add("Point Cost: " + node.getPointCost());
        tooltipLines.add("Required Level: " + node.getRequiredLevel());

        if (!node.getRequirements().isEmpty()) {
            tooltipLines.add("");
            tooltipLines.add("Requires:");
            for (String reqId : node.getRequirements()) {
                skillTree.getNode(reqId).ifPresent(reqNode -> {
                    tooltipLines.add("  - " + reqNode.getName());
                });
            }
        }

        int tooltipWidth = 0;
        for (String line : tooltipLines) {
            int lineWidth = this.font.width(line);
            if (lineWidth > tooltipWidth) {
                tooltipWidth = lineWidth;
            }
        }
        tooltipWidth += 16;
        int tooltipHeight = tooltipLines.size() * 12 + 8;

        int tooltipX = mouseX + 10;
        int tooltipY = mouseY + 10;
        if (tooltipX + tooltipWidth > this.width - CLASS_PANEL_WIDTH - 20) {
            tooltipX = mouseX - tooltipWidth - 10;
        }
        if (tooltipY + tooltipHeight > this.height) {
            tooltipY = this.height - tooltipHeight - 5;
        }

        // Tooltip background
        guiGraphics.fill(tooltipX, tooltipY,
                tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xEE000000);

        // Tooltip border
        guiGraphics.fill(tooltipX, tooltipY,
                tooltipX + tooltipWidth, tooltipY + 2, 0xFFAA44FF);
        guiGraphics.fill(tooltipX, tooltipY + tooltipHeight - 2,
                tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xFFAA44FF);
        guiGraphics.fill(tooltipX, tooltipY,
                tooltipX + 2, tooltipY + tooltipHeight, 0xFFAA44FF);
        guiGraphics.fill(tooltipX + tooltipWidth - 2, tooltipY,
                tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xFFAA44FF);

        // Tooltip text
        int textY = tooltipY + 5;
        for (String line : tooltipLines) {
            guiGraphics.drawString(this.font, line, tooltipX + 8, textY, 0xFFFFFFFF, false);
            textY += 12;
        }
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
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private String getSkillEmoji(String skillId) {
        return switch (skillId.toLowerCase()) {
            case "power_strike" -> "⚔";
            case "whirlwind" -> "🌪";
            case "critical_eye" -> "👁";
            case "shadow_step" -> "👤";
            case "toughness" -> "💪";
            case "agility" -> "👟";
            case "mana_pool" -> "💙";
            case "spell_power" -> "✨";
            case "fireball" -> "🔥";
            case "battle_cry" -> "📢";
            case "mana_regen" -> "💫";
            case "evasion" -> "💨";
            case "precision" -> "🎯";
            case "rapid_fire" -> "🏹";
            case "tracking" -> "🔍";
            case "iron_skin" -> "🛡";
            case "shield_wall" -> "🏰";
            case "taunt" -> "😠";
            case "divine_blessing" -> "✝";
            case "holy_light" -> "☀";
            case "resurrection" -> "🕊";
            default -> "⭐";
        };
    }

    private int getClassColor(String classId) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> 0xFFDD4444;
            case "mage" -> 0xFF4444DD;
            case "rogue" -> 0xFF44DD44;
            case "ranger" -> 0xFF88DD44;
            case "tank" -> 0xFFDDDD44;
            case "priest" -> 0xFFFFAAAA;
            default -> 0xFFAAAAAA;
        };
    }

    private String getClassEmoji(String classId) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> "⚔";
            case "mage" -> "✨";
            case "rogue" -> "🗡";
            case "ranger" -> "🏹";
            case "tank" -> "🛡";
            case "priest" -> "✝";
            default -> "⭐";
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            zoom = Math.min(zoom + ZOOM_STEP, MAX_ZOOM);
        } else if (scrollY < 0) {
            zoom = Math.max(zoom - ZOOM_STEP, MIN_ZOOM);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}