package org.safi.weapons.scythe_weapon.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class WeatheringEffect extends StatusEffect {

    public WeatheringEffect() {
        super(StatusEffectCategory.HARMFUL, 0xFF0000); // Set the color to red
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        // This method is called every tick (20 times per second)
        // Apply damage to the entity every second
        if (entity != null && !entity.getWorld().isClient) {
            entity.damage(new DamageSource(null), 1f);
        }
    }
}