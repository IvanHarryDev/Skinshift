package com.harry.wildcraft.entity.skinwalker.goal;

import com.harry.wildcraft.entity.skinwalker.SkinwalkerDecoyHelper;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMode;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMorphHelper;
import com.harry.wildcraft.util.SightHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class SkinwalkerFollowPlayerGoal extends Goal {

    private final SkinwalkerEntity sw;
    private Player target;

    // ─── PASSIVE ───────────────────────────────────────────────
    private static final double PASSIVE_APPROACH_DISTANCE = 10.0;
    private static final double PASSIVE_WANDER_RADIUS = 50.0;
    private static final double PASSIVE_FOLLOW_START = 50.0;
    private static final double PASSIVE_STALK_SPEED = 0.5;
    private static final int PASSIVE_ORDER_MIN = 80;
    private static final int PASSIVE_ORDER_MAX = 180;
    private int passiveStalkCooldown = 0;
    private int fallbackWanderCooldown = 0;

    // ─── THREATENING ───────────────────────────────────────────
    private static final double THREAT_MIN_DISTANCE = 10.0;
    private static final double THREAT_FOLLOW_SPEED = 0.8;
    private static final double THREAT_OBSERVE_MIN = 15.0;
    private static final double THREAT_OBSERVE_MAX = 35.0;
    private static final double THREAT_OBSERVE_IDEAL = 22.0;
    private static final double THREAT_RETREAT_SPEED = 0.9;
    private static final int THREAT_LOOK_LIMIT_TICKS = 100;
    private static final double THREAT_LOOK_POOF_DIST = 5.0;

    private boolean wasObservingAtNight = false;
    private int smokeParticleTick = 0;

    public SkinwalkerFollowPlayerGoal(SkinwalkerEntity sw) {
        this.sw = sw;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (sw.getMode() == SkinwalkerMode.AGGRESSIVE) return false;
        if (sw.getTargetPlayerUUID() == null) return false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (!(sw.level() instanceof ServerLevel level)) return;
        if (sw.getTargetPlayerUUID() == null) return;

        target = level.getPlayerByUUID(sw.getTargetPlayerUUID());
        if (target == null || !target.isAlive()) return;
        if (target.isCreative() || target.isSpectator()) {
            sw.getNavigation().stop();
            Mob decoy = sw.getCurrentDecoy();
            if (decoy != null) decoy.getNavigation().stop();
            return;
        }

        double dist = sw.distanceTo(target);
        boolean playerLooking = SightHelper.isPlayerLookingAt(target, sw.position(), 0.95);

        if (sw.getMode() == SkinwalkerMode.PASSIVE) {
            sw.setCrouchingAnim(false);
            sw.setLookingAround(false);
            tickPassive(level, dist, playerLooking);
        } else if (sw.getMode() == SkinwalkerMode.THREATENING) {
            tickThreatening(level, dist, playerLooking);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  PASSIVE
    // ═══════════════════════════════════════════════════════════
    private void tickPassive(ServerLevel level, double dist, boolean playerLooking) {
        Mob decoy = sw.getCurrentDecoy();
        boolean usingDecoy = decoy != null && decoy.isAlive() && sw.isMorphed() && !sw.isMorphInProgress();

        if (!sw.isMorphInProgress()) {
            SkinwalkerMorphHelper.checkAndMorphIfBiomeChanged(sw, level);
        }

        if (usingDecoy) {
            tickPassiveWithDecoy(decoy, dist, playerLooking);
        } else {
            tickPassiveFallback(dist, playerLooking);
        }
    }

    private void tickPassiveWithDecoy(Mob decoy, double dist, boolean playerLooking) {
        if (playerLooking) {
            SkinwalkerDecoyHelper.freezeDecoy(decoy, target);
            passiveStalkCooldown = 0;
            return;
        }

        if (dist <= PASSIVE_APPROACH_DISTANCE) {
            passiveStalkCooldown = 0;
            return;
        }

        if (dist > PASSIVE_FOLLOW_START) {
            SkinwalkerDecoyHelper.stalkTowards(decoy, target, PASSIVE_STALK_SPEED);
            passiveStalkCooldown = PASSIVE_ORDER_MAX;
            return;
        }

        passiveStalkCooldown--;
        if (passiveStalkCooldown <= 0) {
            passiveStalkCooldown = PASSIVE_ORDER_MIN + sw.getRandom().nextInt(PASSIVE_ORDER_MAX - PASSIVE_ORDER_MIN);
            if (dist > PASSIVE_APPROACH_DISTANCE + 2) {
                SkinwalkerDecoyHelper.stalkTowards(decoy, target, PASSIVE_STALK_SPEED);
            }
        }
    }

    private void tickPassiveFallback(double dist, boolean playerLooking) {
        sw.getLookControl().setLookAt(target, 30, 30);

        if (playerLooking) {
            sw.getNavigation().stop();
            Vec3 dm = sw.getDeltaMovement();
            sw.setDeltaMovement(0, dm.y, 0);
            return;
        }

        if (dist > PASSIVE_FOLLOW_START) {
            sw.getNavigation().moveTo(target, 0.6);
        } else if (dist > PASSIVE_APPROACH_DISTANCE) {
            sw.getNavigation().moveTo(target, 0.4);
        } else {
            fallbackWanderCooldown--;
            if (fallbackWanderCooldown <= 0) {
                fallbackWanderCooldown = 60 + sw.getRandom().nextInt(100);
                double offsetX = (sw.getRandom().nextDouble() - 0.5) * 20;
                double offsetZ = (sw.getRandom().nextDouble() - 0.5) * 20;
                double wx = sw.getX() + offsetX;
                double wz = sw.getZ() + offsetZ;
                double dxP = wx - target.getX();
                double dzP = wz - target.getZ();
                double wDist = Math.sqrt(dxP * dxP + dzP * dzP);
                if (wDist > PASSIVE_WANDER_RADIUS) {
                    wx = target.getX() + (dxP / wDist) * (PASSIVE_WANDER_RADIUS - 2);
                    wz = target.getZ() + (dzP / wDist) * (PASSIVE_WANDER_RADIUS - 2);
                }
                sw.getNavigation().moveTo(wx, sw.getY(), wz, 0.4);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  THREATENING
    // ═══════════════════════════════════════════════════════════
    private void tickThreatening(ServerLevel level, double dist, boolean playerLooking) {
        boolean isNight = !level.isDay();
        Mob decoy = sw.getCurrentDecoy();
        boolean usingDecoy = decoy != null && decoy.isAlive() && sw.isMorphed() && !sw.isMorphInProgress();

        if (wasObservingAtNight) {
            tickThreateningObserving(level, dist, playerLooking);
            return;
        }

        if (isNight && usingDecoy && !playerLooking) {
            if (dist < THREAT_OBSERVE_MIN) {
                retreatDecoyTo(decoy, THREAT_OBSERVE_IDEAL);
                return;
            }
            if (dist >= THREAT_OBSERVE_MIN && dist <= THREAT_OBSERVE_MAX) {
                startNightObservation(level, decoy);
                return;
            }
        }

        if (usingDecoy) {
            tickThreateningFollowWithDecoy(decoy, dist, playerLooking);
        } else {
            tickThreateningFollowFallback(dist, playerLooking);
        }

        if (level.isDay() && !sw.isMorphed() && !sw.isMorphInProgress() && !playerLooking) {
            sw.setCrouchingAnim(false);
            sw.setLookingAround(false);
            SkinwalkerMorphHelper.morphToClosestBiomeAnimal(sw, level, sw.position());
        }

        if (!sw.isMorphInProgress()) {
            SkinwalkerMorphHelper.checkAndMorphIfBiomeChanged(sw, level);
        }
    }

    private void tickThreateningFollowWithDecoy(Mob decoy, double dist, boolean playerLooking) {
        if (playerLooking) {
            SkinwalkerDecoyHelper.freezeDecoy(decoy, target);
            return;
        }

        if (dist > THREAT_MIN_DISTANCE) {
            SkinwalkerDecoyHelper.stalkTowards(decoy, target, THREAT_FOLLOW_SPEED);
        } else {
            SkinwalkerDecoyHelper.freezeDecoy(decoy, target);
        }
    }

    private void tickThreateningFollowFallback(double dist, boolean playerLooking) {
        sw.getLookControl().setLookAt(target, 30, 30);

        if (playerLooking) {
            sw.getNavigation().stop();
            Vec3 dm = sw.getDeltaMovement();
            sw.setDeltaMovement(0, dm.y, 0);
            return;
        }

        if (dist > THREAT_MIN_DISTANCE) {
            sw.getNavigation().moveTo(target, THREAT_FOLLOW_SPEED);
        } else {
            sw.getNavigation().stop();
            Vec3 dm = sw.getDeltaMovement();
            sw.setDeltaMovement(0, dm.y, 0);
        }
    }

    private void retreatDecoyTo(Mob decoy, double targetDistance) {
        double dx = decoy.getX() - target.getX();
        double dz = decoy.getZ() - target.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 0.01) {
            double ang = sw.getRandom().nextDouble() * Math.PI * 2;
            dx = Math.cos(ang);
            dz = Math.sin(ang);
            d = 1.0;
        }
        double gx = target.getX() + (dx / d) * targetDistance;
        double gz = target.getZ() + (dz / d) * targetDistance;
        decoy.getNavigation().moveTo(gx, decoy.getY(), gz, THREAT_RETREAT_SPEED);
    }

    private void startNightObservation(ServerLevel level, Mob decoy) {
        SkinwalkerMorphHelper.unmorphInstant(sw);
        sw.randomizeNocturnalCrouch();
        wasObservingAtNight = true;
        smokeParticleTick = 0;
        sw.getNavigation().stop();
        Vec3 dm = sw.getDeltaMovement();
        sw.setDeltaMovement(0, dm.y, 0);

        if (sw.isNocturnalCrouch()) {
            sw.setCrouchingAnim(true);
            sw.setLookingAround(false);
        } else {
            sw.setCrouchingAnim(false);
            sw.setLookingAround(true);
        }
        sw.setLookAtTimer(0);
    }

    private void tickThreateningObserving(ServerLevel level, double dist, boolean playerLooking) {
        sw.getNavigation().stop();
        Vec3 dm = sw.getDeltaMovement();
        sw.setDeltaMovement(0, dm.y, 0);
        sw.getLookControl().setLookAt(target, 30, 30);

        smokeParticleTick++;
        if (smokeParticleTick >= 40) {
            smokeParticleTick = 0;
            level.sendParticles(ParticleTypes.SMOKE,
                    sw.getX(), sw.getY() + 1.5, sw.getZ(), 2, 0.2, 0.3, 0.2, 0.01);
        }

        if (level.isDay()) {
            wasObservingAtNight = false;
            smokeParticleTick = 0;
            sw.setCrouchingAnim(false);
            sw.setLookingAround(false);
            if (!sw.isMorphed() && !sw.isMorphInProgress()) {
                SkinwalkerMorphHelper.morphToClosestBiomeAnimal(sw, level, sw.position());
            }
            return;
        }

        if (playerLooking) {
            sw.incrementLookAtTimer();
            if (sw.getLookAtTimer() > THREAT_LOOK_LIMIT_TICKS || dist < THREAT_LOOK_POOF_DIST) {
                if (target instanceof ServerPlayer) {
                    level.playSound(null, sw.getX(), sw.getY(), sw.getZ(),
                            SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.5f, 0.5f);
                }
                wasObservingAtNight = false;
                smokeParticleTick = 0;
                sw.setCrouchingAnim(false);
                sw.setLookingAround(false);
                sw.poofAndRespawn(level, target);
            }
        } else {
            sw.setLookAtTimer(0);
            if (dist < THREAT_LOOK_POOF_DIST) {
                wasObservingAtNight = false;
                smokeParticleTick = 0;
                sw.setCrouchingAnim(false);
                sw.setLookingAround(false);
                sw.poofAndRespawn(level, target);
            }
        }
    }
}