package org.safi.weapons.scythe_weapon.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long cooldownDuration; // in milliseconds

    public CooldownManager(long cooldownDuration) {
        this.cooldownDuration = cooldownDuration;
    }

    public boolean isOnCooldown(UUID playerId) {
        return cooldowns.containsKey(playerId) && System.currentTimeMillis() < cooldowns.get(playerId);
    }

    public void startCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis() + cooldownDuration);
    }

    public long getRemainingCooldown(UUID playerId) {
        if (cooldowns.containsKey(playerId)) {
            return Math.max(0, cooldowns.get(playerId) - System.currentTimeMillis());
        }
        return 0;
    }
}

