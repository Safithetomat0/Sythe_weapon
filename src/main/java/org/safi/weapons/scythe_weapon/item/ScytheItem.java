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
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.safi.weapons.scythe_weapon.WeaponsMod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScytheItem extends SwordItem {
    private static final int MAX_CHARGES = 3;
    private static final int timeInSeconds = 15;
    private static final int COOLDOWN_TICKS = 1000 * timeInSeconds; // seconds cooldown
    private final Map<UUID, Long> cooldownTimers = new HashMap<>();
    private final Map<UUID, Integer> chargesRemainingMap = new HashMap<>();
    private static boolean push = false;
    private static Vec3d acceleration = null;
    public ScytheItem(ToolMaterial material, int attackDamage, float attackSpeed) {
        super(material, attackDamage, attackSpeed, new Item.Settings());
    }

    private ParticleEffect getCustomParticleParameters() {
        return ParticleTypes.DRAGON_BREATH;
    }

    private void spawnCurvedParticleLine(World world, Entity startEntity, Entity endEntity) {
        for (int i = 0; i < 20; i++) {
            double t = (double) i / (20 - 1);
            double interpX = MathHelper.lerp(t, startEntity.getX(), endEntity.getX());
            double interpY = MathHelper.lerp(t, startEntity.getY() + startEntity.getHeight() / 2.0, endEntity.getY() + endEntity.getHeight() / 2.0);
            double interpZ = MathHelper.lerp(t, startEntity.getZ(), endEntity.getZ());

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
    public ActionResult useOnEntity(ItemStack stack, @NotNull PlayerEntity user, LivingEntity entity, Hand hand) {
        UUID playerUUID = user.getUuid();
        performAction(user.getWorld(), entity);
        System.out.println(entity+"given soul attached");
        user.addStatusEffect(new StatusEffectInstance(WeaponsMod.soulAttachedEffect, 1000, 1));
        entity.addStatusEffect(new StatusEffectInstance(WeaponsMod.soulAttachedEffect, 1000, 1));
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        System.out.println("Starting use method...");
        ItemStack itemStack = new ItemStack(WeaponsMod.WEATHERING_SWORD);

        UUID playerUUID = user.getUuid();
        System.out.println("charges remaining before use: " + chargesRemainingMap.get(playerUUID));

        if (!world.isClient) {
            System.out.println("This is server function");

            if (chargesRemainingMap.get(playerUUID) > 0) {
                List<Entity> entities = world.getEntitiesByClass(Entity.class, user.getBoundingBox().expand(1000.0, 100.0, 1000.0), entity -> entity instanceof LivingEntity);

                LivingEntity selectedEntity = entities.stream()
                        .filter(entity -> entity != user && entity instanceof LivingEntity livingEntity && livingEntity.hasStatusEffect(WeaponsMod.soulAttachedEffect))
                        .map(entity -> (LivingEntity) entity)
                        .findFirst()
                        .orElse(null);

                if (selectedEntity != null) {
                    System.out.println("Entity exists");
                    if (selectedEntity.hasStatusEffect(WeaponsMod.soulAttachedEffect)) {
                        // Adjust the acceleration of the entity towards the user
                        acceleration = new Vec3d(user.getX() - selectedEntity.getX(), user.getY() - selectedEntity.getY(), user.getZ() - selectedEntity.getZ()).normalize();
                        push = true;

                        spawnCurvedParticleLine(world, user, selectedEntity);
                        performAction(world, selectedEntity);

                        // Start cooldown timer
                        cooldownTimers.put(playerUUID, System.currentTimeMillis() + COOLDOWN_TICKS);

                        // Decrease charges remaining
                        chargesRemainingMap.put(playerUUID, chargesRemainingMap.get(playerUUID) - 1);

                        user.getItemCooldownManager().set(itemStack.getItem(), 30);

                        System.out.println("charges remaining after use: " + chargesRemainingMap.get(playerUUID));

                        return TypedActionResult.success(user.getStackInHand(hand));
                    }
                }
            }
        }

        System.out.println("Finished use method.");
        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target != null) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1));
        }

        return super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        UUID playerUUID = entity.getUuid();

        // Update cooldown timer
        if (cooldownTimers.containsKey(playerUUID) && cooldownTimers.get(playerUUID) <= System.currentTimeMillis()) {
            cooldownTimers.remove(playerUUID);
        }

        stack.setHolder(entity);

        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            if (cooldownTimers.containsKey(playerUUID) && chargesRemainingMap.containsKey(playerUUID)) {
                if (player.getUuid().equals(playerUUID) && selected) {
                    int cooldownSeconds = Math.max(0, (int) Math.ceil((cooldownTimers.getOrDefault(playerUUID, 0L) - System.currentTimeMillis()) / 1000.0));

                    if (cooldownSeconds % 20 == 0) {
                        player.sendMessage(Text.of("Cooldown time: " + cooldownSeconds + "s, Charges remaining: " + chargesRemainingMap.get(playerUUID)), true);
                    }
                }
            }
        }

        if (push && acceleration != null) {
            System.out.println("Adjusting acceleration for: " + entity);
            double speed = 0.2; // Adjust speed as needed (positive to push towards)
            entity.addVelocity(acceleration.x * speed, acceleration.y * speed, acceleration.z * speed);
            push = false;
        }

        // Recharge if charges are less than max
        chargesRemainingMap.put(playerUUID, chargesRemainingMap.getOrDefault(playerUUID, MAX_CHARGES));
        if (chargesRemainingMap.get(playerUUID) < MAX_CHARGES && !cooldownTimers.containsKey(playerUUID)) {
            chargesRemainingMap.put(playerUUID, chargesRemainingMap.get(playerUUID) + 1);
        }
    }
}
