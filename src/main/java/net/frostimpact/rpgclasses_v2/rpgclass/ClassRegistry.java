package net.frostimpact.rpgclasses_v2.rpgclass;

import net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
     * Initialize placeholder classes
     */
    public static void initializePlaceholderClasses() {
        // Warrior class
        RPGClass warrior = new RPGClass("warrior", "Warrior",
                "A strong melee fighter with high health and damage", "warrior");
        warrior.addBaseStat(StatType.MAX_HEALTH, new StatModifier("warrior_health", StatType.MAX_HEALTH, 20.0, -1));
        warrior.addBaseStat(StatType.DAMAGE, new StatModifier("warrior_damage", StatType.DAMAGE, 5.0, -1));
        warrior.addBaseStat(StatType.DEFENSE, new StatModifier("warrior_defense", StatType.DEFENSE, 3.0, -1));
        register(warrior);

        // Mage class
        RPGClass mage = new RPGClass("mage", "Mage",
                "A powerful spellcaster with high mana and magical abilities", "mage");
        mage.addBaseStat(StatType.MAX_MANA, new StatModifier("mage_mana", StatType.MAX_MANA, 50.0, -1));
        mage.addBaseStat(StatType.MANA_REGEN, new StatModifier("mage_regen", StatType.MANA_REGEN, 2.0, -1));
        mage.addBaseStat(StatType.DAMAGE, new StatModifier("mage_damage", StatType.DAMAGE, 3.0, -1));
        register(mage);

        // Rogue class
        RPGClass rogue = new RPGClass("rogue", "Rogue",
                "A swift and agile fighter with high speed and critical hits", "rogue");
        rogue.addBaseStat(StatType.MOVE_SPEED, new StatModifier("rogue_speed", StatType.MOVE_SPEED, 15.0, -1));
        rogue.addBaseStat(StatType.ATTACK_SPEED, new StatModifier("rogue_attack", StatType.ATTACK_SPEED, 20.0, -1));
        rogue.addBaseStat(StatType.DAMAGE, new StatModifier("rogue_damage", StatType.DAMAGE, 4.0, -1));
        register(rogue);

        LOGGER.info("Initialized {} placeholder classes", classes.size());
    }
}