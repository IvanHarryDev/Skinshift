package com.harry.wildcraft.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class BrookTroutEntity extends AbstractFish implements GeoEntity {

    public static final EntityDataAccessor<Boolean> IS_DYING_SYNC =
            SynchedEntityData.defineId(BrookTroutEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int hurtTimer = 0;
    private static final int HURT_GRACE = 20;

    private boolean lootDropped = false;
    private DamageSource deathDamageSource = null;

    public BrookTroutEntity(EntityType<? extends BrookTroutEntity> type, Level level) {
        super(type, level);
    }

    public boolean isDying() { return entityData.get(IS_DYING_SYNC); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(IS_DYING_SYNC, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractFish.createAttributes()
                .add(Attributes.MAX_HEALTH, 3.0);
    }

    @Override
    protected SoundEvent getFlopSound() { return SoundEvents.COD_FLOP; }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(net.minecraft.world.item.Items.COD_BUCKET);
    }

    // ==================== DEATH SYSTEM ====================
    @Override
    public void die(DamageSource src) {
        if (isDying()) return;
        entityData.set(IS_DYING_SYNC, true);
        deathDamageSource = src;
        setHealth(1f);
        getNavigation().stop();
        setNoAi(true);
        setDeltaMovement(0, 0, 0);
    }

    @Override protected void tickDeath() { }
    @Override public boolean isAlive() { return isDying() || super.isAlive(); }
    @Override public boolean isDeadOrDying() { return isDying() ? false : super.isDeadOrDying(); }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        if (isDying()) return false;
        boolean h = super.hurt(src, dmg);
        if (h) hurtTimer = HURT_GRACE;
        return h;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isDying() && !level().isClientSide && level() instanceof ServerLevel sl) {
            if (!lootDropped) {
                lootDropped = true;
                dropLootFromTable(sl, player);
            }
            discard();
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private void dropLootFromTable(ServerLevel sl, Player player) {
        LootTable lootTable = sl.getServer().getLootData()
                .getLootTable(getType().getDefaultLootTable());
        LootParams.Builder builder = new LootParams.Builder(sl)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, position())
                .withParameter(LootContextParams.DAMAGE_SOURCE,
                        deathDamageSource != null ? deathDamageSource : sl.damageSources().generic());
        if (deathDamageSource != null && deathDamageSource.getEntity() instanceof Player killer) {
            builder.withParameter(LootContextParams.KILLER_ENTITY, killer)
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer);
        } else {
            builder.withParameter(LootContextParams.KILLER_ENTITY, player)
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player);
        }
        LootParams params = builder.create(LootContextParamSets.ENTITY);
        List<ItemStack> drops = lootTable.getRandomItems(params);
        for (ItemStack stack : drops) spawnAtLocation(stack);
        if (getExperienceReward() > 0)
            net.minecraft.world.entity.ExperienceOrb.award(sl, position(), getExperienceReward());
    }

    @Override public boolean isPickable() { return isDying() || super.isPickable(); }

    // ==================== TICK ====================
    @Override
    public void tick() {
        if (isDying()) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            baseTick();
            return;
        }

        super.tick();
        if (level().isClientSide) return;
        if (hurtTimer > 0) hurtTimer--;
    }

    // ==================== GECKOLIB ====================
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 2, state -> {
            if (isDying())
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("animation.brook_trout.death"));
            if (!isInWater() && hurtTimer == 0)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.brook_trout.flapping"));
            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("animation.brook_trout.swim"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}