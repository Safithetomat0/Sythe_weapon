package org.safi.weapons.scythe_weapon.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class WeatheringEffect extends StatusEffect {

    public WeatheringEffect() {
        super(StatusEffectCategory.HARMFUL,
                0xFF0000); // Set the color to red
    }
}