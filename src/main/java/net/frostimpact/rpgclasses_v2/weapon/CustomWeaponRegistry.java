package net.frostimpact.rpgclasses_v2.weapon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for managing CustomWeapon definitions.
 * This is separate from WeaponRegistry which handles basic WeaponStats.
 */
public class CustomWeaponRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomWeaponRegistry.class);
    private static final Map<String, CustomWeapon> customWeapons = new HashMap<>();
    
    /**
     * Register a custom weapon
     */
    public static void register(CustomWeapon weapon) {
        String weaponId = weapon.getId();
        if (customWeapons.containsKey(weaponId)) {
            LOGGER.warn("Custom weapon {} is already registered. Overwriting.", weaponId);
        }
        customWeapons.put(weaponId, weapon);
        LOGGER.debug("Registered custom weapon: {}", weaponId);
    }
    
    /**
     * Get a custom weapon by ID
     */
    public static Optional<CustomWeapon> getWeapon(String weaponId) {
        return Optional.ofNullable(customWeapons.get(weaponId));
    }
    
    /**
     * Check if a custom weapon is registered
     */
    public static boolean isRegistered(String weaponId) {
        return customWeapons.containsKey(weaponId);
    }
    
    /**
     * Get all registered custom weapon IDs
     */
    public static Iterable<String> getAllWeaponIds() {
        return customWeapons.keySet();
    }
    
    /**
     * Get all registered custom weapons
     */
    public static Iterable<CustomWeapon> getAllWeapons() {
        return customWeapons.values();
    }
    
    /**
     * Initialize example custom weapons
     */
    public static void initializeExampleWeapons() {
        // Example: Infernal Blade - An epic fire sword with ability and passive
        register(ExampleWeapons.INFERNAL_BLADE);
        
        LOGGER.info("Initialized {} custom weapons", customWeapons.size());
    }
}
