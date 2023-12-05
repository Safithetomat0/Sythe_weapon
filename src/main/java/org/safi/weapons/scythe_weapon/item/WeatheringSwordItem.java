package org.safi.weapons.scythe_weapon.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.safi.weapons.scythe_weapon.WeaponsMod;

import java.util.List;

import static java.lang.Math.abs;

public class WeatheringSwordItem extends SwordItem {

    public WeatheringSwordItem(ToolMaterial material, int attackDamage, float attackSpeed) {
        super(material, attackDamage, attackSpeed, new Item.Settings());
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {

        if (entity != null) {
            entity.addStatusEffect(new StatusEffectInstance(WeaponsMod.WEATHERING_EFFECT, 1000, 1)); // Adjust duration as needed
            return ActionResult.SUCCESS;

        }
        return ActionResult.SUCCESS;
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        System.out.println("Starting use method...");

        List<Entity> entities = world.getEntitiesByClass(Entity.class, user.getBoundingBox().expand(100.0, 100.0, 100.0), entity -> entity instanceof LivingEntity);
        System.out.println("Found " + entities.size() + " entities within the range.");

        for (Entity entity : entities) {
            if (entity != user && entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;

                // Check if the LivingEntity has the specified status effect
                if (livingEntity.hasStatusEffect(WeaponsMod.WEATHERING_EFFECT)) {
                    System.out.println("Adjusting acceleration for entity: " + entity);

                    double distance = user.distanceTo(entity);
                    // Adjust the acceleration of the entity towards the user
                    Vec3d acceleration = new Vec3d(user.getX() - entity.getX(), user.getY() - entity.getY(), user.getZ() - entity.getZ());

                    // Modify this as needed based on your desired acceleration
                    double speed = 0.1; // Set speed to half the distance
                    entity.setVelocity(acceleration.multiply(speed));

                    // Do something else if needed
                }
            }
        }

        System.out.println("Finished use method.");

        return TypedActionResult.success(user.getStackInHand(hand));
    }
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Apply the weathering effect to the hit entity
        if (target != null) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100,1)); // Adjust duration as needed
        }

        return super.postHit(stack, target, attacker);
    }
    @Override
    public boolean isDamageable() {
        return false;
    }
}