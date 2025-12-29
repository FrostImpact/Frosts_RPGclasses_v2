package net.frostimpact.rpgclasses_v2.skilltree;

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
 * Enhanced skill tree GUI with tree-like structure, icons, tooltips, and zoom functionality
 */
public class SkillTreeScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillTreeScreen.class);
    private static final int NODE_SIZE = 40;
    private static final int NODE_SPACING = 80;
    private static final int LINE_COLOR = 0xFF888888;
    private static final int LINE_ACTIVE_COLOR = 0xFFFFDD00;
    private static final int TITLE_SPACE = 50;
    
    private final String skillTreeId;
    private SkillTree skillTree;
    private SkillNode hoveredNode;
    
    // Pan and zoom support
    private float offsetX = 0;
    private float offsetY = 0;
    private float zoom = 1.0f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.0f;
    private static final float ZOOM_STEP = 0.1f;
    
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
        
        // Center the tree on screen
        centerTree();
        
        LOGGER.debug("Initialized skill tree screen for: {}", skillTree.getName());
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
        
        // Center on screen
        offsetX = (this.width - treeWidth) / 2.0f - minX * NODE_SPACING;
        offsetY = (this.height - treeHeight) / 2.0f - minY * NODE_SPACING + TITLE_SPACE;
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render solid black background (no blur)
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);
        
        if (skillTree == null) {
            // Draw error message
            String errorText = "Failed to load skill tree";
            int textWidth = this.font.width(errorText);
            guiGraphics.drawString(this.font, errorText, 
                (this.width - textWidth) / 2, this.height / 2, 0xFFFF5555);
            return;
        }
        
        // Get player class level
        Minecraft mc = Minecraft.getInstance();
        int classLevel = 1;
        if (mc.player != null) {
            PlayerRPGData rpgData = mc.player.getData(ModAttachments.PLAYER_RPG);
            classLevel = rpgData.getClassLevel();
        }
        
        // Draw title without blur
        String title = skillTree.getName() + " - Level " + classLevel;
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (this.width - titleWidth) / 2 + 2, 22, 0xFF000000);
        guiGraphics.drawString(this.font, title, (this.width - titleWidth) / 2, 20, 0xFFFFDD00);
        
        // Draw description without blur
        String desc = skillTree.getDescription();
        int descWidth = this.font.width(desc);
        guiGraphics.drawString(this.font, desc, 
            (this.width - descWidth) / 2, 40, 0xFFCCCCCC);
        
        // Save graphics state for zoom/pan
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(offsetX, offsetY, 0);
        guiGraphics.pose().scale(zoom, zoom, 1.0f);
        
        // Draw connection lines between nodes first (so they appear behind nodes)
        List<SkillNode> allNodes = skillTree.getAllNodes();
        for (SkillNode node : allNodes) {
            for (String reqId : node.getRequirements()) {
                skillTree.getNode(reqId).ifPresent(reqNode -> {
                    int x1 = reqNode.getX() * NODE_SPACING + NODE_SIZE / 2;
                    int y1 = reqNode.getY() * NODE_SPACING + NODE_SIZE / 2;
                    int x2 = node.getX() * NODE_SPACING + NODE_SIZE / 2;
                    int y2 = node.getY() * NODE_SPACING + NODE_SIZE / 2;
                    
                    // Draw line
                    drawLine(guiGraphics, x1, y1, x2, y2, LINE_COLOR);
                });
            }
        }
        
        // Reset hovered node
        hoveredNode = null;
        
        // Draw skill nodes with emoji icons
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
            
            // Determine node state colors
            boolean isUnlocked = classLevel >= node.getRequiredLevel();
            int nodeColor = isUnlocked ? 0xFF4488FF : 0xFF444444;
            int borderColor = isHovered ? 0xFFFFFFFF : (isUnlocked ? 0xFF6699FF : 0xFF666666);
            
            // Draw node shadow
            guiGraphics.fill(nodeX + 2, nodeY + 2, 
                           nodeX + NODE_SIZE + 2, nodeY + NODE_SIZE + 2, 0x88000000);
            
            // Draw node background
            guiGraphics.fill(nodeX, nodeY, 
                           nodeX + NODE_SIZE, nodeY + NODE_SIZE, nodeColor);
            
            // Draw node border (thicker if hovered)
            int borderThickness = isHovered ? 3 : 2;
            for (int t = 0; t < borderThickness; t++) {
                guiGraphics.fill(nodeX - t, nodeY - t, 
                               nodeX + NODE_SIZE + t, nodeY - t + 1, borderColor);
                guiGraphics.fill(nodeX - t, nodeY + NODE_SIZE + t - 1, 
                               nodeX + NODE_SIZE + t, nodeY + NODE_SIZE + t, borderColor);
                guiGraphics.fill(nodeX - t, nodeY - t, 
                               nodeX - t + 1, nodeY + NODE_SIZE + t, borderColor);
                guiGraphics.fill(nodeX + NODE_SIZE + t - 1, nodeY - t, 
                               nodeX + NODE_SIZE + t, nodeY + NODE_SIZE + t, borderColor);
            }
            
            // Draw emoji icon for the skill node
            String emoji = getSkillEmoji(node.getId());
            int emojiX = nodeX + (NODE_SIZE - this.font.width(emoji)) / 2;
            int emojiY = nodeY + (NODE_SIZE - 8) / 2;
            guiGraphics.drawString(this.font, emoji, emojiX, emojiY, 0xFFFFFFFF);
            
            // Draw level indicator
            if (node.getMaxLevel() > 1) {
                String levelText = "0/" + node.getMaxLevel();
                int levelWidth = this.font.width(levelText);
                guiGraphics.drawString(this.font, levelText, 
                    nodeX + (NODE_SIZE - levelWidth) / 2, nodeY + NODE_SIZE + 2, 0xFFFFFFFF);
            }
        }
        
        // Restore graphics state
        guiGraphics.pose().popPose();
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Draw tooltip for hovered node (outside zoom transform)
        if (hoveredNode != null) {
            drawNodeTooltip(guiGraphics, hoveredNode, mouseX, mouseY);
        }
        
        // Draw controls info
        String zoomInfo = String.format("Zoom: %.1fx (Scroll to zoom)", zoom);
        guiGraphics.drawString(this.font, zoomInfo, 10, this.height - 40, 0xFFFFDD00);
        
        String levelInfo = "Class Level: " + classLevel;
        guiGraphics.drawString(this.font, levelInfo, 10, this.height - 25, 0xFFFFDD00);
        
        // Draw instructions
        String instructions = "Hover over nodes to see details";
        int instructionsWidth = this.font.width(instructions);
        guiGraphics.drawString(this.font, instructions, 
            this.width - instructionsWidth - 10, this.height - 20, 0xFF888888);
    }
    
    private void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        // Simple line drawing using fill for horizontal/vertical lines
        // For diagonal, we'll approximate with small segments
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
        
        // Node name
        tooltipLines.add("§e§l" + node.getName());
        tooltipLines.add("");
        
        // Description (word wrap)
        String desc = node.getDescription();
        tooltipLines.addAll(wrapText(desc, 200));
        tooltipLines.add("");
        
        // Stats
        tooltipLines.add("§7Max Level: §f" + node.getMaxLevel());
        tooltipLines.add("§7Point Cost: §f" + node.getPointCost());
        tooltipLines.add("§7Required Level: §f" + node.getRequiredLevel());
        
        // Requirements
        if (!node.getRequirements().isEmpty()) {
            tooltipLines.add("");
            tooltipLines.add("§6Requires:");
            for (String reqId : node.getRequirements()) {
                skillTree.getNode(reqId).ifPresent(reqNode -> {
                    tooltipLines.add("  §7- " + reqNode.getName());
                });
            }
        }
        
        // Calculate tooltip size
        int tooltipWidth = 0;
        for (String line : tooltipLines) {
            int lineWidth = this.font.width(line);
            if (lineWidth > tooltipWidth) {
                tooltipWidth = lineWidth;
            }
        }
        tooltipWidth += 16; // Padding
        int tooltipHeight = tooltipLines.size() * 12 + 8;
        
        // Position tooltip (avoid going off screen)
        int tooltipX = mouseX + 10;
        int tooltipY = mouseY + 10;
        if (tooltipX + tooltipWidth > this.width) {
            tooltipX = mouseX - tooltipWidth - 10;
        }
        if (tooltipY + tooltipHeight > this.height) {
            tooltipY = this.height - tooltipHeight - 5;
        }
        
        // Draw tooltip background
        guiGraphics.fill(tooltipX, tooltipY, 
                       tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xEE000000);
        
        // Draw tooltip border
        guiGraphics.fill(tooltipX, tooltipY, 
                       tooltipX + tooltipWidth, tooltipY + 2, 0xFFAA44FF);
        guiGraphics.fill(tooltipX, tooltipY + tooltipHeight - 2, 
                       tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xFFAA44FF);
        guiGraphics.fill(tooltipX, tooltipY, 
                       tooltipX + 2, tooltipY + tooltipHeight, 0xFFAA44FF);
        guiGraphics.fill(tooltipX + tooltipWidth - 2, tooltipY, 
                       tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xFFAA44FF);
        
        // Draw tooltip text
        int textY = tooltipY + 5;
        for (String line : tooltipLines) {
            guiGraphics.drawString(this.font, line, tooltipX + 8, textY, 0xFFFFFFFF);
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
                    lines.add(word); // Word too long, add anyway
                }
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    /**
     * Get emoji icon for skill node
     */
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
    
    /**
     * Handle mouse scroll for zoom
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            // Zoom in
            zoom = Math.min(zoom + ZOOM_STEP, MAX_ZOOM);
        } else if (scrollY < 0) {
            // Zoom out
            zoom = Math.max(zoom - ZOOM_STEP, MIN_ZOOM);
        }
        return true;
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
