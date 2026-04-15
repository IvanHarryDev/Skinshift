package com.harry.wildcraft.entity.skinwalker.goal;

import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMode;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMorphHelper;
import com.harry.wildcraft.util.SightHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class SkinwalkerFollowPlayerGoal extends Goal {

    private final SkinwalkerEntity sw;
    private Player target;
    private boolean wasObservingAtNight = false;
    private int wanderCooldown = 0;
    private int stalkBurstTimer = 0;
    private int ambushCooldown = 0;
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

    private void tickPassive(ServerLevel level, double dist, boolean playerLooking) {
        sw.getLookControl().setLookAt(target, 30, 30);

        if (playerLooking) {
            sw.getNavigation().stop();
            return;
        }

        boolean isNight = !level.isDay();
        double minDist = isNight ? 7.0 : 10.0;
        double comfortDist = isNight ? 40.0 : 50.0;

        if (dist > comfortDist) {
            sw.getNavigation().moveTo(target, 0.7);
        } else if (dist < minDist) {
            double awayX = sw.getX() + (sw.getX() - target.getX()) * 0.3;
            double awayZ = sw.getZ() + (sw.getZ() - target.getZ()) * 0.3;
            sw.getNavigation().moveTo(awayX, sw.getY(), awayZ, 0.4);
        } else {
            wanderCooldown--;
            if (wanderCooldown <= 0) {
                wanderCooldown = 60 + sw.getRandom().nextInt(100);
                if (sw.getRandom().nextFloat() < 0.3f) {
                    sw.getNavigation().moveTo(target, 0.35);
                } else {
                    double offsetX = (sw.getRandom().nextDouble() - 0.5) * 20;
                    double offsetZ = (sw.getRandom().nextDouble() - 0.5) * 20;
                    double wanderX = sw.getX() + offsetX;
                    double wanderZ = sw.getZ() + offsetZ;
                    double dxP = wanderX - target.getX();
                    double dzP = wanderZ - target.getZ();
                    double wDist = Math.sqrt(dxP * dxP + dzP * dzP);
                    if (wDist > 50) {
                        wanderX = target.getX() + (dxP / wDist) * 48;
                        wanderZ = target.getZ() + (dzP / wDist) * 48;
                    }
                    sw.getNavigation().moveTo(wanderX, sw.getY(), wanderZ, 0.4);
                }
            }

            ambushCooldown--;
            if (ambushCooldown <= 0 && dist > 30 && sw.getRandom().nextFloat() < 0.02f) {
                ambushCooldown = 600;
                tryAmbushReposition(level);
            }
        }

        if (!sw.isMorphInProgress()) {
            SkinwalkerMorphHelper.checkAndMorphIfBiomeChanged(sw, level);
        }
    }

    private void tryAmbushReposition(ServerLevel level) {
        if (!(target instanceof ServerPlayer)) return;
        Vec3 playerLook = target.getLookAngle().normalize();
        double forwardDist = 15 + sw.getRandom().nextDouble() * 10;
        double sideOffset = (sw.getRandom().nextBoolean() ? 1 : -1) * (5 + sw.getRandom().nextDouble() * 8);
        double perpX = -playerLook.z;
        double perpZ = playerLook.x;
        double newX = target.getX() + playerLook.x * forwardDist + perpX * sideOffset;
        double newZ = target.getZ() + playerLook.z * forwardDist + perpZ * sideOffset;
        Vec3 safePos = SightHelper.findSafePosition(level, newX, newZ);
        if (safePos != null) sw.teleportTo(safePos.x, safePos.y, safePos.z);
    }

    private void tickThreatening(ServerLevel level, double dist, boolean playerLooking) {
        sw.getLookControl().setLookAt(target, 30, 30);

        if (playerLooking) {
            sw.getNavigation().stop();
            sw.incrementLookAtTimer();
            sw.setCrouchingAnim(false);
            sw.setLookingAround(false);

            if (sw.getLookAtTimer() > 100 || dist < 5) {
                if (target instanceof ServerPlayer) {
                    level.playSound(null, sw.getX(), sw.getY(), sw.getZ(),
                            SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.5f, 0.5f);
                }
                sw.poofAndRespawn(level, target);
                wasObservingAtNight = false;
                stalkBurstTimer = 0;
            }
            return;
        }
        sw.setLookAtTimer(0);

        boolean isNight = !level.isDay();
        boolean inObserveRange = dist > 15 && dist < 35;

        if (isNight && inObserveRange) {
            sw.getNavigation().stop();

            if (!wasObservingAtNight && !sw.isMorphInProgress()) {
                sw.setMorphed(false);
                sw.setMorphedInto("none");
                sw.randomizeNocturnalCrouch();
                wasObservingAtNight = true;

                if (sw.isNocturnalCrouch()) {
                    sw.setCrouchingAnim(true);
                    sw.setLookingAround(false);
                } else {
                    sw.setCrouchingAnim(false);
                    sw.setLookingAround(true);
                }
            }

            smokeParticleTick++;
            if (smokeParticleTick >= 40) {
                smokeParticleTick = 0;
                level.sendParticles(ParticleTypes.SMOKE,
                        sw.getX(), sw.getY() + 1.5, sw.getZ(), 2, 0.2, 0.3, 0.2, 0.01);
            }
            return;
        }

        if (wasObservingAtNight) {
            wasObservingAtNight = false;
            smokeParticleTick = 0;
            sw.setCrouchingAnim(false);
            sw.setLookingAround(false);
            if (!sw.isMorphed() && !sw.isMorphInProgress()) {
                SkinwalkerMorphHelper.morphToClosestBiomeAnimal(sw, level, sw.position());
            }
        }

        if (level.isDay() && !sw.isMorphed() && !sw.isMorphInProgress()) {
            sw.setCrouchingAnim(false);
            sw.setLookingAround(false);
            SkinwalkerMorphHelper.morphToClosestBiomeAnimal(sw, level, sw.position());
        }

        stalkBurstTimer--;
        if (dist > 10) {
            if (stalkBurstTimer <= 0 && dist > 20 && dist < 40
                    && sw.getRandom().nextFloat() < 0.05f && !playerLooking) {
                stalkBurstTimer = 200;
                sw.getNavigation().moveTo(target, 1.2);
            } else if (stalkBurstTimer > 170) {
                sw.getNavigation().moveTo(target, 1.2);
            } else {
                sw.getNavigation().moveTo(target, 0.8);
            }
        } else if (dist < 8) {
            double awayX = sw.getX() + (sw.getX() - target.getX()) * 0.3;
            double awayZ = sw.getZ() + (sw.getZ() - target.getZ()) * 0.3;
            sw.getNavigation().moveTo(awayX, sw.getY(), awayZ, 0.5);
        }

        if (!sw.isMorphInProgress()) {
            SkinwalkerMorphHelper.checkAndMorphIfBiomeChanged(sw, level);
        }
    }
}