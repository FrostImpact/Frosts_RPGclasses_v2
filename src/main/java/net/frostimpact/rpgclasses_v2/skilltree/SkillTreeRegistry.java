package net.frostimpact.rpgclasses_v2.skilltree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Central registry for managing skill trees
 */
public class SkillTreeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillTreeRegistry.class);
    private static final Map<String, SkillTree> skillTrees = new HashMap<>();
    
    /**
     * Register a skill tree
     */
    public static void register(SkillTree skillTree) {
        String treeId = skillTree.getId();
        if (skillTrees.containsKey(treeId)) {
            LOGGER.warn("Skill tree {} is already registered. Overwriting.", treeId);
        }
        skillTrees.put(treeId, skillTree);
        LOGGER.debug("Registered skill tree: {}", treeId);
    }
    
    /**
     * Get a skill tree by ID
     */
    public static Optional<SkillTree> getSkillTree(String treeId) {
        return Optional.ofNullable(skillTrees.get(treeId));
    }
    
    /**
     * Check if a skill tree is registered
     */
    public static boolean isRegistered(String treeId) {
        return skillTrees.containsKey(treeId);
    }
    
    /**
     * Initialize placeholder skill trees
     */
    public static void initializePlaceholderTrees() {
        // Warrior skill tree
        SkillTree warriorTree = new SkillTree("warrior", "Warrior Skills", "Combat skills for warriors");
        
        SkillNode powerStrike = new SkillNode("power_strike", "Power Strike", 
            "Increases melee damage by 10% per level", 5, 1, 1);
        warriorTree.addNode(powerStrike);
        
        SkillNode toughness = new SkillNode("toughness", "Toughness", 
            "Increases max health by 5 per level", 5, 1, 1);
        warriorTree.addNode(toughness);
        
        SkillNode whirlwind = new SkillNode("whirlwind", "Whirlwind", 
            "Unlocks a spinning attack ability", 1, 2, 5);
        whirlwind.addRequirement("power_strike");
        warriorTree.addNode(whirlwind);
        
        register(warriorTree);
        
        // Mage skill tree
        SkillTree mageTree = new SkillTree("mage", "Mage Skills", "Magical skills for mages");
        
        SkillNode manaPool = new SkillNode("mana_pool", "Expanded Mana Pool", 
            "Increases max mana by 10 per level", 5, 1, 1);
        mageTree.addNode(manaPool);
        
        SkillNode spellPower = new SkillNode("spell_power", "Spell Power", 
            "Increases magic damage by 15% per level", 5, 1, 1);
        mageTree.addNode(spellPower);
        
        SkillNode fireball = new SkillNode("fireball", "Fireball", 
            "Unlocks the fireball spell", 1, 2, 5);
        fireball.addRequirement("spell_power");
        mageTree.addNode(fireball);
        
        register(mageTree);
        
        // Rogue skill tree
        SkillTree rogueTree = new SkillTree("rogue", "Rogue Skills", "Stealth and agility skills for rogues");
        
        SkillNode agility = new SkillNode("agility", "Agility", 
            "Increases movement speed by 5% per level", 5, 1, 1);
        rogueTree.addNode(agility);
        
        SkillNode criticalEye = new SkillNode("critical_eye", "Critical Eye", 
            "Increases critical hit chance by 3% per level", 5, 1, 1);
        rogueTree.addNode(criticalEye);
        
        SkillNode shadowStep = new SkillNode("shadow_step", "Shadow Step", 
            "Unlocks teleportation ability", 1, 2, 5);
        shadowStep.addRequirement("agility");
        rogueTree.addNode(shadowStep);
        
        register(rogueTree);
        
        LOGGER.info("Initialized {} placeholder skill trees", skillTrees.size());
    }
}
