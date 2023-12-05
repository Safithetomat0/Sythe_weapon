package org.safi.weapons.scythe_weapon;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.safi.weapons.scythe_weapon.effect.WeatheringEffect;
import org.safi.weapons.scythe_weapon.item.WeatheringSwordItem;

public class WeaponsMod implements ModInitializer {
    public static final String MOD_ID = "legendary_weapon";
    public static final WeatheringEffect WEATHERING_EFFECT = new WeatheringEffect();
    public static final SwordItem WEATHERING_SWORD = new WeatheringSwordItem(ToolMaterials.IRON, 1, -0.4F);


    @Override
    public void onInitialize() {
        Registry.register(Registries.STATUS_EFFECT, new Identifier(MOD_ID, "weathering"), WEATHERING_EFFECT);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "weathering_sword"), WEATHERING_SWORD);

    }
}
