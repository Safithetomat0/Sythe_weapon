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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.safi.weapons.scythe_weapon.WeaponsMod;

import java.util.List;

public class WeatheringSwordItem extends SwordItem {
    private static final int MAX_CHARGES = 6;
    private static final int CHARGE_COOLDOWN = 14*20; // 4 seconds converted to ticks
    private int chargesRemaining = MAX_CHARGES;
    private int cooldownTimer = 0;
    public WeatheringSwordItem(ToolMaterial material, int attackDamage, float attackSpeed) {
        super(material, attackDamage, attackSpeed, new Item.Settings());
    }
    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {

        if (entity != null) {
            if (chargesRemaining > 0) {
                if (!entity.hasStatusEffect(WeaponsMod.WEATHERING_EFFECT)) {
                    // Perform the action for each charge
                    performAction(user.getWorld(), user, stack);

                    // Start cooldown timer
                    if (chargesRemaining == MAX_CHARGES) {
                        cooldownTimer = CHARGE_COOLDOWN;
                    }

                    // Decrease charges
                    chargesRemaining--;

                    entity.addStatusEffect(new StatusEffectInstance(WeaponsMod.WEATHERING_EFFECT, 1000, 1)); // Adjust duration as needed
                    return ActionResult.SUCCESS;
                } else {
                    return TypedActionResult.fail(user.getStackInHand(hand)).getResult();
                }
            }

        }
        return ActionResult.SUCCESS;
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        System.out.println("Starting use method...");
        ItemStack itemStack = new ItemStack(WeaponsMod.WEATHERING_SWORD); // Replace with the actual item

        List<Entity> entities = world.getEntitiesByClass(Entity.class, user.getBoundingBox().expand(1000.0, 100.0, 1000.0), entity -> entity instanceof LivingEntity);
        System.out.println("Found " + entities.size() + " entities within the range.");

        if (chargesRemaining > 0) {
            LivingEntity selectedEntity = null;

            for (Entity entity : entities) {
                if (entity != user && entity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity) entity;

                    // Check if the LivingEntity has the specified status effect
                    if (livingEntity.hasStatusEffect(WeaponsMod.WEATHERING_EFFECT)) {
                        selectedEntity = livingEntity;
                        break; // Found the first eligible entity, exit the loop
                    }
                }
            }

            if (selectedEntity != null) {
                System.out.println("Adjusting acceleration for entity: " + selectedEntity);

                // Adjust the acceleration of the entity towards the user
                Vec3d acceleration = new Vec3d(user.getX() - selectedEntity.getX(), user.getY() - selectedEntity.getY(), user.getZ() - selectedEntity.getZ());

                // Modify this as needed based on your desired acceleration
                double speed = -0.2; // Set speed to half the distance
                user.setVelocity(acceleration.multiply(speed));

                spawnRedstoneParticles(world, user, selectedEntity, 20);

                // Perform the action for each charge
                performAction(world, user, user.getStackInHand(hand));

                // Start cooldown timer
                if (chargesRemaining == MAX_CHARGES) {
                    cooldownTimer = CHARGE_COOLDOWN;
                }
                user.getItemCooldownManager().set(itemStack.getItem(), 10);

                // Decrease charges
                chargesRemaining--;

                return TypedActionResult.success(user.getStackInHand(hand));
            }
        }

        // Remove entities without the 'weathering effect' from the original list
        entities.removeIf(entity -> !(entity instanceof LivingEntity) || !((LivingEntity) entity).hasStatusEffect(WeaponsMod.WEATHERING_EFFECT));

        if (entities.isEmpty()) {
            // No entity with the 'weathering effect' found
            System.out.println("No entity with the 'weathering effect' found.");
        }
        System.out.println("Finished use method.");
        return TypedActionResult.pass(user.getStackInHand(hand));
    }
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Apply the weathering effect to the hit entity
        if (target != null) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100,1)); // Adjust duration as needed
        }

        return super.postHit(stack, target, attacker);
    }
    private static void spawnRedstoneParticles(World world, Entity startEntity, Entity endEntity, int particleCount) {
        Random rand = world.random;
        for (int i = 0; i < particleCount; i++) {
            double offsetX = rand.nextDouble() * 0.2 - 0.1;
            double offsetY = rand.nextDouble() * 0.2 - 0.1;
            double offsetZ = rand.nextDouble() * 0.2 - 0.1;

            double startX = startEntity.getX() + offsetX;
            double startY = startEntity.getY() + startEntity.getHeight() / 2.0 + offsetY;
            double startZ = startEntity.getZ() + offsetZ;

            double endX = endEntity.getX() + offsetX;
            double endY = endEntity.getY() + endEntity.getHeight() / 2.0 + offsetY;
            double endZ = endEntity.getZ() + offsetZ;

            world.addParticle(ParticleTypes.DRAGON_BREATH, startX, startY, startZ, endX - startX, endY - startY, endZ - startZ);
        }
    }
    private void performAction(World world, PlayerEntity user, ItemStack itemStack) {
        int numParticles = 10; // Number of particles in the circle
        double radius = 1.0; // Radius of the circular motion

        for (int i = 0; i < numParticles; i++) {
            double angle = (2.0 * Math.PI * i) / numParticles; // Angle for each particle
            double offsetX = radius * Math.cos(angle); // X offset based on angle
            double offsetY = 1.0; // Y offset (vertical position)
            double offsetZ = radius * Math.sin(angle); // Z offset based on angle

            // Add particle at the calculated position
            world.addParticle(ParticleTypes.DRAGON_BREATH, user.getX() + offsetX, user.getY() + offsetY, user.getZ() + offsetZ, 0, 0, 0);
        }
    }
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            if (selected) {
                player.sendMessage(Text.of("Cooldown time: " + cooldownTimer + "ms, Charges remaining: " + chargesRemaining/2), true);
            }
        }
        // Update cooldown timer
        if (cooldownTimer > 0) {
            cooldownTimer--;
        }

        // Recharge if charges are less than max
        if (chargesRemaining < MAX_CHARGES && cooldownTimer <= 0) {
            chargesRemaining++;
        }
    }
}