package com.harry.wildcraft.entity;

import com.harry.wildcraft.entity.goal.CoyotePackAttackGoal;
import com.harry.wildcraft.entity.goal.CoyoteStalkGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CoyoteEntity extends Animal implements GeoEntity {

    public enum PackState { IDLE, STALKING, ATTACKING }
    private PackState packState = PackState.IDLE;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CoyoteEntity(EntityType<? extends CoyoteEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,    20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE,  4.0)
                .add(Attributes.FOLLOW_RANGE,  150.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(2, new CoyoteStalkGoal(this));
        goalSelector.addGoal(3, new CoyotePackAttackGoal(this));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, false,
                e -> !((Player) e).isCreative() && !((Player) e).isSpectator()));
    }

    public PackState getPackState() { return packState; }
    public void setPackState(PackState s) { this.packState = s; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && getTarget() instanceof Player p
                && packState == PackState.ATTACKING && distanceTo(p) > 150) {
            setTarget(null);
            packState = PackState.IDLE;
        }
    }

    @Override
    @javax.annotation.Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar r) {
        r.add(new AnimationController<>(this, "main", 5, state -> {
            if (this.isAggressive())
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.coyote.run"));
            if (this.getDeltaMovement().horizontalDistanceSqr() > 0.001)
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.coyote.walk"));
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.coyote.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
