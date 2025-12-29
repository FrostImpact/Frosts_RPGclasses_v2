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
        
        // Root node at top
        SkillNode powerStrike = new SkillNode("power_strike", "Power Strike", 
            "Increases melee damage by 10% per level", 5, 1, 1, 2, 0, "");
        warriorTree.addNode(powerStrike);
        
        // Second tier - branches out
        SkillNode toughness = new SkillNode("toughness", "Toughness", 
            "Increases max health by 5 per level", 5, 1, 1, 1, 1, "");
        warriorTree.addNode(toughness);
        
        SkillNode battleCry = new SkillNode("battle_cry", "Battle Cry", 
            "Increases attack speed by 5% per level", 3, 1, 3, 3, 1, "");
        battleCry.addRequirement("power_strike");
        warriorTree.addNode(battleCry);
        
        // Third tier - advanced skills
        SkillNode whirlwind = new SkillNode("whirlwind", "Whirlwind", 
            "Unlocks a spinning attack ability", 1, 2, 5, 2, 2, "");
        whirlwind.addRequirement("power_strike");
        warriorTree.addNode(whirlwind);
        
        register(warriorTree);
        
        // Mage skill tree
        SkillTree mageTree = new SkillTree("mage", "Mage Skills", "Magical skills for mages");
        
        // Root node
        SkillNode spellPower = new SkillNode("spell_power", "Spell Power", 
            "Increases magic damage by 15% per level", 5, 1, 1, 2, 0, "");
        mageTree.addNode(spellPower);
        
        // Second tier
        SkillNode manaPool = new SkillNode("mana_pool", "Expanded Mana Pool", 
            "Increases max mana by 10 per level", 5, 1, 1, 1, 1, "");
        manaPool.addRequirement("spell_power");
        mageTree.addNode(manaPool);
        
        SkillNode manaRegen = new SkillNode("mana_regen", "Mana Regeneration", 
            "Increases mana regen by 1 per level", 3, 1, 3, 3, 1, "");
        manaRegen.addRequirement("spell_power");
        mageTree.addNode(manaRegen);
        
        // Third tier
        SkillNode fireball = new SkillNode("fireball", "Fireball", 
            "Unlocks the fireball spell", 1, 2, 5, 2, 2, "");
        fireball.addRequirement("spell_power");
        mageTree.addNode(fireball);
        
        register(mageTree);
        
        // Rogue skill tree
        SkillTree rogueTree = new SkillTree("rogue", "Rogue Skills", "Stealth and agility skills for rogues");
        
        // Root node
        SkillNode agility = new SkillNode("agility", "Agility", 
            "Increases movement speed by 5% per level", 5, 1, 1, 2, 0, "");
        rogueTree.addNode(agility);
        
        // Second tier
        SkillNode criticalEye = new SkillNode("critical_eye", "Critical Eye", 
            "Increases critical hit chance by 3% per level", 5, 1, 1, 1, 1, "");
        criticalEye.addRequirement("agility");
        rogueTree.addNode(criticalEye);
        
        SkillNode evasion = new SkillNode("evasion", "Evasion", 
            "Increases dodge chance by 2% per level", 3, 1, 3, 3, 1, "");
        evasion.addRequirement("agility");
        rogueTree.addNode(evasion);
        
        // Third tier
        SkillNode shadowStep = new SkillNode("shadow_step", "Shadow Step", 
            "Unlocks teleportation ability", 1, 2, 5, 2, 2, "");
        shadowStep.addRequirement("agility");
        rogueTree.addNode(shadowStep);
        
        register(rogueTree);
        
        // Ranger skill tree
        SkillTree rangerTree = new SkillTree("ranger", "Ranger Skills", "Archery and tracking skills");
        
        SkillNode precision = new SkillNode("precision", "Precision", 
            "Increases ranged damage by 8% per level", 5, 1, 1, 2, 0, "");
        rangerTree.addNode(precision);
        
        SkillNode rapidFire = new SkillNode("rapid_fire", "Rapid Fire", 
            "Increases attack speed by 10% per level", 3, 1, 3, 1, 1, "");
        rapidFire.addRequirement("precision");
        rangerTree.addNode(rapidFire);
        
        SkillNode tracking = new SkillNode("tracking", "Tracking", 
            "Reveals enemies in a radius", 1, 2, 5, 3, 1, "");
        tracking.addRequirement("precision");
        rangerTree.addNode(tracking);
        
        register(rangerTree);
        
        // Tank skill tree
        SkillTree tankTree = new SkillTree("tank", "Tank Skills", "Defensive and protective skills");
        
        SkillNode ironSkin = new SkillNode("iron_skin", "Iron Skin", 
            "Increases defense by 3 per level", 5, 1, 1, 2, 0, "");
        tankTree.addNode(ironSkin);
        
        SkillNode shieldWall = new SkillNode("shield_wall", "Shield Wall", 
            "Reduces damage taken by 5% per level", 3, 1, 3, 1, 1, "");
        shieldWall.addRequirement("iron_skin");
        tankTree.addNode(shieldWall);
        
        SkillNode taunt = new SkillNode("taunt", "Taunt", 
            "Forces enemies to target you", 1, 2, 5, 3, 1, "");
        taunt.addRequirement("iron_skin");
        tankTree.addNode(taunt);
        
        register(tankTree);
        
        // Priest skill tree
        SkillTree priestTree = new SkillTree("priest", "Priest Skills", "Healing and support skills");
        
        SkillNode divineBlessing = new SkillNode("divine_blessing", "Divine Blessing", 
            "Increases healing power by 10% per level", 5, 1, 1, 2, 0, "");
        priestTree.addNode(divineBlessing);
        
        SkillNode holyLight = new SkillNode("holy_light", "Holy Light", 
            "Heals nearby allies over time", 3, 1, 3, 1, 1, "");
        holyLight.addRequirement("divine_blessing");
        priestTree.addNode(holyLight);
        
        SkillNode resurrection = new SkillNode("resurrection", "Resurrection", 
            "Revive fallen allies", 1, 2, 10, 3, 1, "");
        resurrection.addRequirement("divine_blessing");
        priestTree.addNode(resurrection);
        
        register(priestTree);
        
        LOGGER.info("Initialized {} placeholder skill trees", skillTrees.size());
    }
}
