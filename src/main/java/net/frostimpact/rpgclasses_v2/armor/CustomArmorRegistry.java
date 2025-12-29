package net.frostimpact.rpgclasses_v2.armor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for managing custom armor pieces and sets.
 */
public class CustomArmorRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomArmorRegistry.class);
    private static final Map<String, CustomArmorPiece> armorPieces = new HashMap<>();
    private static final Map<String, CustomArmorSet> armorSets = new HashMap<>();
    
    /**
     * Register a custom armor piece
     */
    public static void registerPiece(CustomArmorPiece piece) {
        String pieceId = piece.getId();
        if (armorPieces.containsKey(pieceId)) {
            LOGGER.warn("Custom armor piece {} is already registered. Overwriting.", pieceId);
        }
        armorPieces.put(pieceId, piece);
        LOGGER.debug("Registered custom armor piece: {}", pieceId);
    }
    
    /**
     * Register a custom armor set (also registers all pieces in the set)
     */
    public static void registerSet(CustomArmorSet set) {
        String setId = set.getId();
        if (armorSets.containsKey(setId)) {
            LOGGER.warn("Custom armor set {} is already registered. Overwriting.", setId);
        }
        armorSets.put(setId, set);
        
        // Register all pieces in the set
        for (CustomArmorPiece piece : set.getAllPieces()) {
            registerPiece(piece);
        }
        
        LOGGER.debug("Registered custom armor set: {}", setId);
    }
    
    /**
     * Get a custom armor piece by ID
     */
    public static Optional<CustomArmorPiece> getPiece(String pieceId) {
        return Optional.ofNullable(armorPieces.get(pieceId));
    }
    
    /**
     * Get a custom armor set by ID
     */
    public static Optional<CustomArmorSet> getSet(String setId) {
        return Optional.ofNullable(armorSets.get(setId));
    }
    
    /**
     * Check if an armor piece is registered
     */
    public static boolean isPieceRegistered(String pieceId) {
        return armorPieces.containsKey(pieceId);
    }
    
    /**
     * Check if an armor set is registered
     */
    public static boolean isSetRegistered(String setId) {
        return armorSets.containsKey(setId);
    }
    
    /**
     * Get all registered armor piece IDs
     */
    public static Iterable<String> getAllPieceIds() {
        return armorPieces.keySet();
    }
    
    /**
     * Get all registered armor set IDs
     */
    public static Iterable<String> getAllSetIds() {
        return armorSets.keySet();
    }
    
    /**
     * Get all registered armor pieces
     */
    public static Iterable<CustomArmorPiece> getAllPieces() {
        return armorPieces.values();
    }
    
    /**
     * Get all registered armor sets
     */
    public static Iterable<CustomArmorSet> getAllSets() {
        return armorSets.values();
    }
    
    /**
     * Initialize example armor
     */
    public static void initializeExampleArmor() {
        // Register the Shadow Assassin set
        registerSet(ExampleArmor.SHADOW_ASSASSIN_SET);
        
        LOGGER.info("Initialized {} custom armor pieces and {} armor sets", 
            armorPieces.size(), armorSets.size());
    }
}
