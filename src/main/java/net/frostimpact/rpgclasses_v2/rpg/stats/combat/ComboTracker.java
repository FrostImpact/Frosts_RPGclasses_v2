package net.frostimpact.rpgclasses_v2.rpg.stats.combat;

import net.frostimpact.rpgclasses_v2.rpg.stats.weapon.WeaponType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks combo state for each player with weapon type support
 */
public class ComboTracker {
    private static final Map<UUID, ComboState> playerCombos = new HashMap<>();

    public static class ComboState {
        private int comboCount = 0;
        private int ticksSinceLastAttack = 0;
        private WeaponType currentWeaponType = WeaponType.LONGSWORD;

        public int getComboCount() {
            return comboCount;
        }

        public void incrementCombo(WeaponType weaponType) {
            // Reset combo if weapon type changed
            if (currentWeaponType != weaponType) {
                comboCount = 0;
                currentWeaponType = weaponType;
            }

            comboCount++;
            int maxCombo = weaponType.getMaxComboCount();

            if (comboCount > maxCombo) {
                comboCount = 1; // Reset to 1 after max combo
            }
            ticksSinceLastAttack = 0;
        }

        public void resetCombo() {
            comboCount = 0;
            ticksSinceLastAttack = 0;
        }

        public void tick() {
            if (comboCount > 0) {
                ticksSinceLastAttack++;
                if (ticksSinceLastAttack >= CombatConfig.COMBO_RESET_TIME) {
                    resetCombo();
                }
            }
        }

        public WeaponType getCurrentWeaponType() {
            return currentWeaponType;
        }
    }

    public static ComboState getComboState(UUID playerUUID) {
        return playerCombos.computeIfAbsent(playerUUID, k -> new ComboState());
    }

    public static void tick(UUID playerUUID) {
        ComboState state = playerCombos.get(playerUUID);
        if (state != null) {
            state.tick();
        }
    }

    public static void removePlayer(UUID playerUUID) {
        playerCombos.remove(playerUUID);
    }
}