package org.safi.weapons.scythe_weapon.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;
import org.safi.weapons.scythe_weapon.WeaponsMod;

public class WeatheringEffect extends StatusEffect {

    public WeatheringEffect() {
        super(StatusEffectCategory.HARMFUL,
                0xFF0000); // Set the color to red
    }

}