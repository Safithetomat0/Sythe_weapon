package org.safi.weapons.scythe_weapon.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.safi.weapons.scythe_weapon.WeaponsMod;

import java.util.List;
import java.util.UUID;

public class WeatheringSwordItem extends SwordItem {
    private static final int MAX_CHARGES = 3;
    private static final int CHARGE_COOLDOWN = 20 * 14; // converted to seconds
    private int chargesRemaining = MAX_CHARGES;
    private int cooldownTimer = 0;
    private UUID holderUUID = null;

    public WeatheringSwordItem(ToolMaterial material, int attackDamage, float attackSpeed, UUID holderUUID) {
        super(material, attackDamage, attackSpeed, new Item.Settings());
        this.holderUUID = holderUUID;
    }
    private ParticleEffect getCustomParticleParameters() {
        // Customize this method based on your particle requirements
        return ParticleTypes.DRAGON_BREATH;
    }
    private void spawnCurvedParticleLine(World world, Entity user, Entity endEntity, int particleCount) {
        Vec3d userVelocity = user.getVelocity();
        double userSpeed = userVelocity.length();

        // Normalize the user's velocity vector
        Vec3d normalizedVelocity = userVelocity.normalize();

        for (int i = 0; i < particleCount; i++) {
            double t = (double) i / (particleCount - 1); // t varies from 0 to 1

            // Apply a sine function to create a curved line
            double curveFactor = Math.sin(t * Math.PI);

            // Use the normalized user velocity to determine the direction of the curve
            double offsetX = normalizedVelocity.x * curveFactor;
            double offsetY = normalizedVelocity.y * curveFactor;
            double offsetZ = normalizedVelocity.z * curveFactor;

            // Interpolate along the line connecting startEntity and endEntity
            double interpX = MathHelper.lerp(t, user.getX(), endEntity.getX());
            double interpY = MathHelper.lerp(t, user.getY() + user.getHeight() / 2.0, endEntity.getY() + endEntity.getHeight() / 2.0);
            double interpZ = MathHelper.lerp(t, user.getZ(), endEntity.getZ());

            // Apply the offsets to create the curved effect
            interpX += offsetX;
            interpY += offsetY;
            interpZ += offsetZ;

            world.addParticle(getCustomParticleParameters(), interpX, interpY, interpZ, 0, 0, 0);
        }
    }
    private void performAction(World world, LivingEntity entityUsedOn) {
        int numParticles = 30;
        double radius = 1.5;

        for (int i = 0; i < numParticles; i++) {
            double angle = (2.0 * Math.PI * i) / numParticles;
            double offsetX = radius * Math.cos(angle);
            double offsetY = 1.0;
            double offsetZ = radius * Math.sin(angle);

            world.addParticle(ParticleTypes.DRAGON_BREATH, entityUsedOn.getX() + offsetX, entityUsedOn.getY() + offsetY, entityUsedOn.getZ() + offsetZ, 0, 0.2, 0);
        }
    }
    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {

        if (entity != null) {
            if (chargesRemaining > 0) {
                if (!entity.hasStatusEffect(WeaponsMod.soulAttachedEffect)) {
                    // Perform the action for each charge
                    performAction(user.getWorld(), entity);

                    // Start cooldown timer
                    if (chargesRemaining == MAX_CHARGES  && cooldownTimer <= 0) {
                        cooldownTimer = CHARGE_COOLDOWN;
                    }

                    entity.addStatusEffect(new StatusEffectInstance(WeaponsMod.soulAttachedEffect, 1000, 1)); // Adjust duration as needed
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
                if (entity != user && entity instanceof LivingEntity livingEntity) {

                    // Check if the LivingEntity has the specified status effect
                    if (livingEntity.hasStatusEffect(WeaponsMod.soulAttachedEffect)) {
                        selectedEntity = livingEntity;
                        break; // Found the first eligible entity, exit the loop
                    }
                }
            }
            // Check if the user is the holder of this weapon
            getHolderUUID();
            if (!user.getUuid().equals(holderUUID)) {
                user.sendMessage(Text.of("You are not the holder of this weapon."), true);
                return TypedActionResult.fail(user.getStackInHand(hand));
            }
            if (selectedEntity != null && selectedEntity.hasStatusEffect(WeaponsMod.soulAttachedEffect)) {
                System.out.println("Adjusting acceleration for entity: " + selectedEntity);
                user.getItemCooldownManager().set(itemStack.getItem(), 20);

                // Adjust the acceleration of the entity towards the user
                Vec3d acceleration = new Vec3d(user.getX() - selectedEntity.getX(), user.getY() - selectedEntity.getY(), user.getZ() - selectedEntity.getZ());

                // Modify this as needed based on your desired acceleration
                double speed = -0.2; // Set speed to half the distance
                user.setVelocity(acceleration.multiply(speed));

                // Spawn custom particles only for the player who activated the item
                if (user.equals(selectedEntity)) {
                    spawnCurvedParticleLine(world, user, selectedEntity, 20);

                    // Perform the action for each charge
                    performAction(world, selectedEntity);
                }

                // Start cooldown timer
                if (chargesRemaining == MAX_CHARGES && cooldownTimer <= 0) {
                    cooldownTimer = CHARGE_COOLDOWN;
                }

                // Decrease charges
                chargesRemaining--;

                // Display messages only to the user who activated the item
                if (user.equals(selectedEntity)) {
                    if (cooldownTimer <= 0) {
                        user.sendMessage(Text.of("Charges remaining: " + chargesRemaining), true);
                    } else {
                        user.sendMessage(Text.of("Cooldown time: " + cooldownTimer / 20 + "s, Charges remaining: " + chargesRemaining), true);
                    }
                }

                return TypedActionResult.success(user.getStackInHand(hand));
            }
        }

        // Remove entities without the 'weathering effect' from the original list
        entities.removeIf(entity -> !(entity instanceof LivingEntity) || !((LivingEntity) entity).hasStatusEffect(WeaponsMod.soulAttachedEffect));

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
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        // Update cooldown timer
        if (cooldownTimer > 0) {
            cooldownTimer--;
        }

        if (entity instanceof PlayerEntity player) {
            if (selected) {
                if (chargesRemaining==MAX_CHARGES){
                    player.sendMessage(Text.of("Charges remaining: " + chargesRemaining), true);
                }else{
                    player.sendMessage(Text.of("Cooldown time: " + cooldownTimer/20 + "s, Charges remaining: " + chargesRemaining), true);
                }
            }
        }

        // Recharge if charges are less than max
        if (chargesRemaining < MAX_CHARGES && cooldownTimer <= 1) {
            chargesRemaining++;
        }
    }
    public UUID getHolderUUID() {

        return holderUUID;
    }
}