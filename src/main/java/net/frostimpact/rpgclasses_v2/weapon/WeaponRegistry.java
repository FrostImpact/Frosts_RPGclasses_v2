package net.frostimpact.rpgclasses_v2.weapon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Central registry for managing weapon statistics
 * Weapons can be registered and their stats accessed from config files
 */
public class WeaponRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeaponRegistry.class);
    private static final Map<String, WeaponStats> weapons = new HashMap<>();
    
    /**
     * Register a weapon with its stats
     */
    public static void register(WeaponStats weaponStats) {
        String weaponId = weaponStats.getWeaponId();
        if (weapons.containsKey(weaponId)) {
            LOGGER.warn("Weapon {} is already registered. Overwriting.", weaponId);
        }
        weapons.put(weaponId, weaponStats);
        LOGGER.debug("Registered weapon: {}", weaponId);
    }
    
    /**
     * Get weapon stats by ID
     */
    public static Optional<WeaponStats> getWeaponStats(String weaponId) {
        return Optional.ofNullable(weapons.get(weaponId));
    }
    
    /**
     * Check if a weapon is registered
     */
    public static boolean isRegistered(String weaponId) {
        return weapons.containsKey(weaponId);
    }
    
    /**
     * Get all registered weapon IDs
     */
    public static Iterable<String> getAllWeaponIds() {
        return weapons.keySet();
    }
    
    /**
     * Initialize default weapons
     */
    public static void initializeDefaultWeapons() {
        // Shortswords
        register(new WeaponStats("iron_shortsword", WeaponType.SHORTSWORD)
            .setBaseDamage(5)
            .setAttackSpeed(1.8)
            .setCriticalChance(10)
            .setReach(2));
            
        register(new WeaponStats("diamond_shortsword", WeaponType.SHORTSWORD)
            .setBaseDamage(7)
            .setAttackSpeed(1.8)
            .setCriticalChance(12)
            .setReach(2));
            
        register(new WeaponStats("netherite_shortsword", WeaponType.SHORTSWORD)
            .setBaseDamage(8)
            .setAttackSpeed(1.8)
            .setCriticalChance(15)
            .setReach(2));
        
        // Longswords
        register(new WeaponStats("iron_longsword", WeaponType.LONGSWORD)
            .setBaseDamage(7)
            .setAttackSpeed(1.4)
            .setCriticalChance(8)
            .setReach(3));
            
        register(new WeaponStats("diamond_longsword", WeaponType.LONGSWORD)
            .setBaseDamage(9)
            .setAttackSpeed(1.4)
            .setCriticalChance(10)
            .setReach(3));
            
        register(new WeaponStats("netherite_longsword", WeaponType.LONGSWORD)
            .setBaseDamage(11)
            .setAttackSpeed(1.4)
            .setCriticalChance(12)
            .setReach(3));
        
        // Claymores
        register(new WeaponStats("iron_claymore", WeaponType.CLAYMORE)
            .setBaseDamage(10)
            .setAttackSpeed(1.0)
            .setCriticalChance(5)
            .setReach(4));
            
        register(new WeaponStats("diamond_claymore", WeaponType.CLAYMORE)
            .setBaseDamage(13)
            .setAttackSpeed(1.0)
            .setCriticalChance(7)
            .setReach(4));
            
        register(new WeaponStats("netherite_claymore", WeaponType.CLAYMORE)
            .setBaseDamage(15)
            .setAttackSpeed(1.0)
            .setCriticalChance(8)
            .setReach(4));
        
        LOGGER.info("Initialized {} default weapons", weapons.size());
    }
}
