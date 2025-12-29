package net.frostimpact.rpgclasses_v2.rpgclass;

import net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Central registry for managing RPG classes
 */
public class ClassRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassRegistry.class);
    private static final Map<String, RPGClass> classes = new HashMap<>();

    /**
     * Register an RPG class
     */
    public static void register(RPGClass rpgClass) {
        String classId = rpgClass.getId();
        if (classes.containsKey(classId)) {
            LOGGER.warn("Class {} is already registered. Overwriting.", classId);
        }
        classes.put(classId, rpgClass);
        LOGGER.debug("Registered class: {}", classId);
    }

    /**
     * Get a class by ID
     */
    public static Optional<RPGClass> getClass(String classId) {
        return Optional.ofNullable(classes.get(classId));
    }

    /**
     * Check if a class is registered
     */
    public static boolean isRegistered(String classId) {
        return classes.containsKey(classId);
    }

    /**
     * Get all registered class IDs
     */
    public static Iterable<String> getAllClassIds() {
        return classes.keySet();
    }
    
    /**
     * Get all main classes (non-subclasses)
     */
    public static List<RPGClass> getMainClasses() {
        List<RPGClass> mainClasses = new ArrayList<>();
        for (RPGClass rpgClass : classes.values()) {
            if (!rpgClass.isSubclass()) {
                mainClasses.add(rpgClass);
            }
        }
        return mainClasses;
    }
    
    /**
     * Get subclasses for a parent class
     */
    public static List<RPGClass> getSubclasses(String parentClassId) {
        List<RPGClass> subclasses = new ArrayList<>();
        for (RPGClass rpgClass : classes.values()) {
            if (rpgClass.isSubclass() && rpgClass.getParentClassId().equals(parentClassId)) {
                subclasses.add(rpgClass);
            }
        }
        return subclasses;
    }

    /**
     * Initialize placeholder classes
     */
    public static void initializePlaceholderClasses() {
        // ===== WARRIOR CLASS =====
        RPGClass warrior = new RPGClass("warrior", "Warrior",
                "Melee fighters with high health and damage", "warrior",
                "rpgclasses_v2:textures/gui/icons/warrior.png", false, "", 1);
        warrior.addBaseStat(StatType.MAX_HEALTH, new StatModifier("warrior_health", StatType.MAX_HEALTH, 20.0, -1));
        warrior.addBaseStat(StatType.DAMAGE, new StatModifier("warrior_damage", StatType.DAMAGE, 5.0, -1));
        warrior.addBaseStat(StatType.DEFENSE, new StatModifier("warrior_defense", StatType.DEFENSE, 3.0, -1));
        register(warrior);
        
        // Warrior subclasses
        RPGClass berserker = new RPGClass("berserker", "Berserker",
                "A rage-fueled warrior who trades defense for overwhelming offense", "warrior",
                "rpgclasses_v2:textures/gui/icons/berserker.png", true, "warrior", 10);
        berserker.addBaseStat(StatType.DAMAGE, new StatModifier("berserker_damage", StatType.DAMAGE, 10.0, -1));
        berserker.addBaseStat(StatType.ATTACK_SPEED, new StatModifier("berserker_speed", StatType.ATTACK_SPEED, 15.0, -1));
        register(berserker);
        
        RPGClass paladin = new RPGClass("paladin", "Paladin",
                "A holy warrior who protects allies and smites evil", "warrior",
                "rpgclasses_v2:textures/gui/icons/paladin.png", true, "warrior", 10);
        paladin.addBaseStat(StatType.MAX_HEALTH, new StatModifier("paladin_health", StatType.MAX_HEALTH, 15.0, -1));
        paladin.addBaseStat(StatType.DEFENSE, new StatModifier("paladin_defense", StatType.DEFENSE, 5.0, -1));
        register(paladin);

        // ===== MAGE CLASS =====
        RPGClass mage = new RPGClass("mage", "Mage",
                "Spellcasters with high mana and magical abilities", "mage",
                "rpgclasses_v2:textures/gui/icons/mage.png", false, "", 1);
        mage.addBaseStat(StatType.MAX_MANA, new StatModifier("mage_mana", StatType.MAX_MANA, 50.0, -1));
        mage.addBaseStat(StatType.MANA_REGEN, new StatModifier("mage_regen", StatType.MANA_REGEN, 2.0, -1));
        mage.addBaseStat(StatType.DAMAGE, new StatModifier("mage_damage", StatType.DAMAGE, 3.0, -1));
        register(mage);
        
        // Mage subclasses
        RPGClass pyromancer = new RPGClass("pyromancer", "Pyromancer",
                "A mage who specializes in destructive fire magic", "mage",
                "rpgclasses_v2:textures/gui/icons/pyromancer.png", true, "mage", 10);
        pyromancer.addBaseStat(StatType.DAMAGE, new StatModifier("pyro_damage", StatType.DAMAGE, 8.0, -1));
        register(pyromancer);
        
        RPGClass frostmage = new RPGClass("frostmage", "Frost Mage",
                "A mage who controls ice and slows enemies", "mage",
                "rpgclasses_v2:textures/gui/icons/frostmage.png", true, "mage", 10);
        frostmage.addBaseStat(StatType.COOLDOWN_REDUCTION, new StatModifier("frost_cooldown", StatType.COOLDOWN_REDUCTION, 20.0, -1));
        register(frostmage);

        // ===== ROGUE CLASS =====
        RPGClass rogue = new RPGClass("rogue", "Rogue",
                "Fighters with high speed and critical hits", "rogue",
                "rpgclasses_v2:textures/gui/icons/rogue.png", false, "", 1);
        rogue.addBaseStat(StatType.MOVE_SPEED, new StatModifier("rogue_speed", StatType.MOVE_SPEED, 15.0, -1));
        rogue.addBaseStat(StatType.ATTACK_SPEED, new StatModifier("rogue_attack", StatType.ATTACK_SPEED, 20.0, -1));
        rogue.addBaseStat(StatType.DAMAGE, new StatModifier("rogue_damage", StatType.DAMAGE, 4.0, -1));
        register(rogue);
        
        // Rogue subclasses
        RPGClass assassin = new RPGClass("assassin", "Assassin",
                "A assassin focused on critical strikes and stealth", "rogue",
                "rpgclasses_v2:textures/gui/icons/assassin.png", true, "rogue", 10);
        assassin.addBaseStat(StatType.DAMAGE, new StatModifier("assassin_damage", StatType.DAMAGE, 8.0, -1));
        register(assassin);
        
        RPGClass shadowdancer = new RPGClass("shadowdancer", "Shadow Dancer",
                "A rogue who manipulates shadows for mobility", "rogue",
                "rpgclasses_v2:textures/gui/icons/shadowdancer.png", true, "rogue", 10);
        shadowdancer.addBaseStat(StatType.MOVE_SPEED, new StatModifier("shadow_speed", StatType.MOVE_SPEED, 25.0, -1));
        register(shadowdancer);

        // ===== RANGER CLASS =====
        RPGClass ranger = new RPGClass("ranger", "Ranger",
                "Archers who excels at ranged combat", "ranger",
                "rpgclasses_v2:textures/gui/icons/ranger.png", false, "", 1);
        ranger.addBaseStat(StatType.DAMAGE, new StatModifier("ranger_damage", StatType.DAMAGE, 4.0, -1));
        ranger.addBaseStat(StatType.ATTACK_SPEED, new StatModifier("ranger_attack", StatType.ATTACK_SPEED, 15.0, -1));
        ranger.addBaseStat(StatType.MOVE_SPEED, new StatModifier("ranger_speed", StatType.MOVE_SPEED, 10.0, -1));
        register(ranger);
        
        // Ranger subclasses
        RPGClass marksman = new RPGClass("marksman", "Marksman",
                "A ranger with unmatched accuracy and precision", "ranger",
                "rpgclasses_v2:textures/gui/icons/marksman.png", true, "ranger", 10);
        marksman.addBaseStat(StatType.DAMAGE, new StatModifier("marksman_damage", StatType.DAMAGE, 10.0, -1));
        register(marksman);
        
        RPGClass beastmaster = new RPGClass("beastmaster", "Beast Master",
                "A ranger who commands animal companions", "ranger",
                "rpgclasses_v2:textures/gui/icons/beastmaster.png", true, "ranger", 10);
        beastmaster.addBaseStat(StatType.MAX_HEALTH, new StatModifier("beast_health", StatType.MAX_HEALTH, 10.0, -1));
        register(beastmaster);

        // ===== TANK CLASS =====
        RPGClass tank = new RPGClass("tank", "Tank",
                "Defenders with high health and defense", "tank",
                "rpgclasses_v2:textures/gui/icons/tank.png", false, "", 1);
        tank.addBaseStat(StatType.MAX_HEALTH, new StatModifier("tank_health", StatType.MAX_HEALTH, 30.0, -1));
        tank.addBaseStat(StatType.DEFENSE, new StatModifier("tank_defense", StatType.DEFENSE, 5.0, -1));
        register(tank);
        
        // Tank subclasses
        RPGClass guardian = new RPGClass("guardian", "Guardian",
                "A defensive tank who protects allies", "tank",
                "rpgclasses_v2:textures/gui/icons/guardian.png", true, "tank", 10);
        guardian.addBaseStat(StatType.DEFENSE, new StatModifier("guardian_defense", StatType.DEFENSE, 8.0, -1));
        register(guardian);
        
        RPGClass juggernaut = new RPGClass("juggernaut", "Juggernaut",
                "An unstoppable force that crushes enemies", "tank",
                "rpgclasses_v2:textures/gui/icons/juggernaut.png", true, "tank", 10);
        juggernaut.addBaseStat(StatType.MAX_HEALTH, new StatModifier("jugg_health", StatType.MAX_HEALTH, 40.0, -1));
        register(juggernaut);

        // ===== PRIEST CLASS =====
        RPGClass priest = new RPGClass("priest", "Priest",
                "Healers with powerful support abilities", "priest",
                "rpgclasses_v2:textures/gui/icons/priest.png", false, "", 1);
        priest.addBaseStat(StatType.MAX_MANA, new StatModifier("priest_mana", StatType.MAX_MANA, 40.0, -1));
        priest.addBaseStat(StatType.MANA_REGEN, new StatModifier("priest_regen", StatType.MANA_REGEN, 3.0, -1));
        priest.addBaseStat(StatType.HEALTH_REGEN, new StatModifier("priest_health_regen", StatType.HEALTH_REGEN, 2.0, -1));
        register(priest);
        
        // Priest subclasses
        RPGClass cleric = new RPGClass("cleric", "Cleric",
                "A priest focused on healing and support", "priest",
                "rpgclasses_v2:textures/gui/icons/cleric.png", true, "priest", 10);
        cleric.addBaseStat(StatType.HEALTH_REGEN, new StatModifier("cleric_regen", StatType.HEALTH_REGEN, 4.0, -1));
        register(cleric);
        
        RPGClass templar = new RPGClass("templar", "Templar",
                "A warrior priest who fights on the front lines", "priest",
                "rpgclasses_v2:textures/gui/icons/templar.png", true, "priest", 10);
        templar.addBaseStat(StatType.MAX_HEALTH, new StatModifier("templar_health", StatType.MAX_HEALTH, 15.0, -1));
        templar.addBaseStat(StatType.DEFENSE, new StatModifier("templar_defense", StatType.DEFENSE, 3.0, -1));
        register(templar);

        LOGGER.info("Initialized {} classes (including {} main classes and {} subclasses)", 
                classes.size(), getMainClasses().size(), 
                classes.size() - getMainClasses().size());
    }
}