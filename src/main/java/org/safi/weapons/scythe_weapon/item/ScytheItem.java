package org.safi.weapons.scythe_weapon.item;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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
import org.jetbrains.annotations.NotNull;
import org.safi.weapons.scythe_weapon.WeaponsMod;
import org.safi.weapons.scythe_weapon.networking.Packets;
import org.safi.weapons.scythe_weapon.packet.ModMessages;

import java.util.*;

public class ScytheItem extends SwordItem {
    private static final int MAX_CHARGES = 3;
    private static final int timeInSeconds = 15;
    private static final int COOLDOWN_TICKS = 1000 * timeInSeconds; // seconds cooldown
    private final double speed_power = -0.12; // Adjust speed as needed (positive to push towards)
    private final Map<UUID, Long> cooldownTimers = new HashMap<>();
    private final Map<UUID, Boolean> push = new HashMap<>();
    private final Map<UUID, Integer> chargesRemainingMap = new HashMap<>();
    private LivingEntity selectedEntity =null;

    public ScytheItem(ToolMaterial material, int attackDamage, float attackSpeed) {
        super(material, attackDamage, attackSpeed, new Item.Settings());
    }

    private static void spawnLinedParticles(World world, Entity startEntity, Entity endEntity, int particleCount) {
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

            // Adjust lifespan based on particle count and desired duration (2 seconds)
            int lifespan = 40;  // Each tick is 1/20th of a second, so 20 ticks = 1 second
            world.addParticle(ParticleTypes.DRAGON_BREATH, true, startX, startY, startZ,
                    endX - startX, endY - startY, endZ - startZ);
        }
    }

    private void performAction(World world, LivingEntity entityUsedOn) {
        if (entityUsedOn == null) {
            return;  // Added null check
        }
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
    public ActionResult useOnEntity(ItemStack stack, @NotNull PlayerEntity user, LivingEntity entity, Hand hand) {
        if (entity == null) {
            return ActionResult.FAIL;  // Added null check
        }
        UUID playerUUID = user.getUuid();
        performAction(user.getWorld(), entity);
        System.out.println(entity + "given soul attached");
        user.addStatusEffect(new StatusEffectInstance(WeaponsMod.soulAttachedEffect, 1000, 1));
        entity.addStatusEffect(new StatusEffectInstance(WeaponsMod.soulAttachedEffect, 1000, 1));
        if (!user.getWorld().isClient){
            user.sendMessage(Text.of("You Have soul attached something"), false);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world == null || user == null) {
            return TypedActionResult.pass(ItemStack.EMPTY);  // Added null checks
        }
        System.out.println("Starting use method...");
        ItemStack itemStack = new ItemStack(WeaponsMod.WEATHERING_SWORD);

        UUID playerUUID = user.getUuid();
        System.out.println("charges remaining before use: " + chargesRemainingMap.get(playerUUID));

        if (selectedEntity == null || selectedEntity.isDead()) {
            System.out.println("selectedEntity==null");
            return TypedActionResult.fail(user.getStackInHand(hand));
        }

        if (chargesRemainingMap.getOrDefault(playerUUID, MAX_CHARGES) > 0) {
            System.out.println("chargesRemainingMap.getOrDefault(playerUUID,MAX_CHARGES)>0 : success");
            chargesRemainingMap.computeIfAbsent(playerUUID, uuid -> MAX_CHARGES); // Initialize here

            if (selectedEntity == null || selectedEntity.isDead()) {
                System.out.println("failed");
                return TypedActionResult.fail(user.getStackInHand(hand));
            } else {
                System.out.println(selectedEntity);
                if (hasSoulAttach(user) && hasSoulAttach(selectedEntity)) {

                    System.out.println("This is server function");
                    // Spawn particles
                    spawnLinedParticles(user.getWorld(), user, selectedEntity, 20);
                    performAction(user.getWorld(), selectedEntity);
                    user.getItemCooldownManager().set(itemStack.getItem(), 20);

                    // Move player
                    System.out.println("Adjusting acceleration for: " + user);
                    if (hasSoulAttach(user)) {
                        Vec3d acceleration = new Vec3d(user.getX() - selectedEntity.getX(),
                                user.getY() - 3 - selectedEntity.getY(), user.getZ() - selectedEntity.getZ());
                        Vec3d force = new Vec3d(acceleration.x * speed_power, acceleration.y * speed_power, acceleration.z * speed_power);

                        user.setVelocity(force);

                        System.out.println("pushed");
                        System.out.println("force: " + force);
                        System.out.println("acceleration: " + acceleration);
                    }
                    if (!world.isClient) {
                        // Start cooldown timer
                        if (chargesRemainingMap.get(playerUUID) >= MAX_CHARGES) {
                            // Start cooldown timer
                            cooldownTimers.put(playerUUID, System.currentTimeMillis() + COOLDOWN_TICKS);
                        }
                        // Decrease charges remaining
                        chargesRemainingMap.put(playerUUID, chargesRemainingMap.get(playerUUID) - 1);
                    }
                    System.out.println("Finished use method.");
                    return TypedActionResult.success(user.getStackInHand(hand));

                } else {
                    System.out.println("failed");
                    selectedEntity = null;
                    return TypedActionResult.fail(user.getStackInHand(hand));
                }
            }
        } else {
            System.out.println("chargesRemainingMap.getOrDefault(playerUUID,MAX_CHARGES)>0 : fail");
            return TypedActionResult.fail(user.getStackInHand(hand));
        }
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target != null) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 200, 1));
        }

        return super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        stack.setHolder(entity);
        UUID playerUUID = entity.getUuid();

        if (selected) {
            List<Entity> entities = world.getEntitiesByClass(Entity.class,
                    entity.getBoundingBox().expand(80.0, 100.0, 80.0), searchEntities -> searchEntities instanceof LivingEntity);
            //System.out.println("got near entities");

            double nearestDistanceSquared = Double.MAX_VALUE;
            if (!world.isClient) {
                if (cooldownTimers.containsKey(playerUUID) && chargesRemainingMap.containsKey(playerUUID)) {
                    if (entity instanceof PlayerEntity && selected) {
                        int cooldownSeconds = Math.max(0, (int) Math.ceil(
                                (cooldownTimers.get(playerUUID) - System.currentTimeMillis()) / 1000.0));
                        ((PlayerEntity) entity).sendMessage(Text.of(
                                        cooldownSeconds + "s " + "remaining to charge, "
                                                + chargesRemainingMap.get(playerUUID) + " Charges Remaining."),
                                true);
                    }
                } else {
                    if (entity instanceof PlayerEntity && selected) {
                        ((PlayerEntity) entity).sendMessage(Text.of("Fully charged"), true);
                    }
                }

                for (Entity searchEntities : entities) {
                    if (searchEntities instanceof LivingEntity livingEntity && entity != searchEntities && hasSoulAttach(livingEntity)) {
                        double distanceSquared = entity.squaredDistanceTo(searchEntities);

                        if (distanceSquared < nearestDistanceSquared) {
                            nearestDistanceSquared = distanceSquared;
                            selectedEntity = livingEntity;
                            //System.out.println("Living entity set to selected entity");

                        }
                    }
                }
            }
        }

        if (push.containsKey(playerUUID) && selectedEntity != null) {
            // Adjust the acceleration of the entity towards the user

            if (push.getOrDefault(playerUUID, false) ) {


                push.put(playerUUID,false);
            }

            chargesRemainingMap.computeIfAbsent(playerUUID, uuid -> MAX_CHARGES);

            if (cooldownTimers.containsKey(playerUUID)) {
                if (cooldownTimers.get(playerUUID) <= System.currentTimeMillis()) {
                    cooldownTimers.remove(playerUUID);
                    chargesRemainingMap.remove(playerUUID);
                }
            }
        }
    }

    private boolean hasSoulAttach(LivingEntity entity) {
        return entity != null && entity.hasStatusEffect(WeaponsMod.soulAttachedEffect);
    }
}
