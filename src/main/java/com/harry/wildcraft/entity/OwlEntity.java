package com.harry.wildcraft.entity;

import com.harry.wildcraft.entity.goal.OwlFlyToTreeGoal;
import com.harry.wildcraft.init.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class OwlEntity extends FlyingMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OwlEntity(EntityType<? extends OwlEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,    10.0)
                .add(Attributes.MOVEMENT_SPEED,  0.2)
                .add(Attributes.FLYING_SPEED,    0.4);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new AvoidEntityGoal<>(this, Player.class, 2.0f, 1.0, 1.0,
                e -> !((Player) e).isCreative() && !((Player) e).isSpectator()));
        goalSelector.addGoal(1, new OwlFlyToTreeGoal(this));
        goalSelector.addGoal(2, new RandomStrollGoal(this, 0.3));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && random.nextInt(400) == 0) {
            level().playSound(null, blockPosition(),
                    ModSounds.OWL_HOOT.get(), SoundSource.NEUTRAL, 0.8f, 1.0f);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar r) {
        r.add(new AnimationController<>(this, "main", 5, state -> {
            if (this.getDeltaMovement().y != 0 || this.isNoGravity())
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.owl.fly"));
            if (this.getDeltaMovement().horizontalDistanceSqr() > 0.001)
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.owl.walk"));
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.owl.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}