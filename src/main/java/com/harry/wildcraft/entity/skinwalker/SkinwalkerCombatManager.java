package com.harry.wildcraft.entity.skinwalker;

import com.harry.wildcraft.init.ModEntities;
import com.harry.wildcraft.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class SkinwalkerCombatManager {

    public enum Phase {
        INACTIVE,
        UNMORPH,
        CHASE,
        ARRIVE,
        STRONG_ATTACK,
        SCREAM,
        COMBAT,
        COMBAT_HIT,
        EAT,
        POST_EAT
    }

    private Phase phase = Phase.INACTIVE;
    private int phaseTimer = 0;
    private int stunCount = 0;
    private int attackCooldown = 0;
    private int stunCooldown = 0;
    private boolean usedCatchLast = false;

    private Vec3 swPos = null;
    private Vec3 playerPos = null;
    private float swYRot = 0;

    public static final int STRONG_ATTACK_TICKS = 30;
    public static final int STUN_HIT_DELAY = 15;
    public static final int SCREAM_TICKS = 115;
    public static final int SCREAM_PUSH_TICK = 3;
    public static final double PUSH_STRENGTH = 0.6;
    public static final int WEAK_ATTACK_TICKS = 18;
    public static final int EAT_TICKS = 60;
    public static final int POST_EAT_TICKS = 100;

    private static final float FACE_DIST = 1.8f;
    private static final float ATTACK_RANGE = 3.5f;
    private static final float STUN_DAMAGE = 4f;
    private static final float NORMAL_DAMAGE = 6f;
    private static final int ATTACK_CD = 30;
    private static final int STUN_CD = 400;

    private boolean stunApplied = false;

    public Phase getPhase() {
        return phase;
    }

    public void start(SkinwalkerEntity sw) {
        phase = sw.isMorphed() ? Phase.UNMORPH : Phase.CHASE;
        phaseTimer = 0;
        stunCount = 0;
        attackCooldown = 0;
        stunCooldown = STUN_CD;
        usedCatchLast = false;
        stunApplied = false;
        swPos = null;
        playerPos = null;
    }

    public void stop(SkinwalkerEntity sw) {
        phase = Phase.INACTIVE;
        swPos = null;
        playerPos = null;
        stunApplied = false;
        sw.unlockPosition();
    }

    public void tick(SkinwalkerEntity sw, ServerLevel level) {
        if (phase == Phase.INACTIVE) return;

        Player target = level.getPlayerByUUID(sw.getTargetPlayerUUID());

        if (target == null || !target.isAlive()) {
            if (phase == Phase.EAT || phase == Phase.POST_EAT) tickPostEat(sw, level);
            return;
        }
        if (target.isCreative() || target.isSpectator()) return;

        switch (phase) {
            case UNMORPH -> tickUnmorph(sw);
            case CHASE -> tickChase(sw, target, level);
            case ARRIVE -> tickArrive(sw, target);
            case STRONG_ATTACK -> tickStrongAttack(sw, target);
            case SCREAM -> tickScream(sw, target, level);
            case COMBAT -> tickCombat(sw, target);
            case COMBAT_HIT -> tickCombatHit(sw, target);
            case EAT -> tickEat(sw, target);
            case POST_EAT -> tickPostEat(sw, level);
        }
    }

    private void goTo(Phase p) {
        phase = p;
        phaseTimer = 0;
        stunApplied = false;
    }

    // ═══════════════════════════════════════════════════════════
    //  Positioning
    // ═══════════════════════════════════════════════════════════
    private void placeFaceToFace(SkinwalkerEntity sw, Player target) {
        Vec3 pPos = target.position();
        double dx = sw.getX() - target.getX();
        double dz = sw.getZ() - target.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        double dirX = dist > 0.1 ? dx / dist : target.getLookAngle().x;
        double dirZ = dist > 0.1 ? dz / dist : target.getLookAngle().z;

        swPos = new Vec3(pPos.x + dirX * FACE_DIST, pPos.y, pPos.z + dirZ * FACE_DIST);
        playerPos = pPos;
        swYRot = (float) (Math.atan2(-dirZ, -dirX) * (180.0 / Math.PI)) - 90f;

        sw.teleportTo(swPos.x, swPos.y, swPos.z);
        sw.setDeltaMovement(0, 0, 0);
        sw.getNavigation().stop();
        applyRotation(sw);
    }

    private void holdBoth(SkinwalkerEntity sw, Player target) {
        if (swPos != null) {
            sw.teleportTo(swPos.x, swPos.y, swPos.z);
            sw.setDeltaMovement(0, 0, 0);
            sw.getNavigation().stop();
            applyRotation(sw);
        }
        if (playerPos != null && target instanceof ServerPlayer sp) {
            sp.teleportTo(playerPos.x, playerPos.y, playerPos.z);
            sp.setDeltaMovement(0, 0, 0);
        }
    }

    private void holdSWOnly(SkinwalkerEntity sw) {
        if (swPos != null) {
            sw.teleportTo(swPos.x, swPos.y, swPos.z);
            sw.setDeltaMovement(0, 0, 0);
            sw.getNavigation().stop();
            applyRotation(sw);
        }
    }

    private void applyRotation(SkinwalkerEntity sw) {
        sw.setYRot(swYRot);
        sw.yBodyRot = swYRot;
        sw.setYHeadRot(swYRot);
    }

    private void faceTarget(SkinwalkerEntity sw, Player target) {
        double dx = target.getX() - sw.getX();
        double dz = target.getZ() - sw.getZ();
        float y = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
        sw.setYRot(y);
        sw.yBodyRot = y;
        sw.setYHeadRot(y);
        sw.getLookControl().setLookAt(target, 360f, 360f);
    }

    private void releaseBoth(SkinwalkerEntity sw) {
        swPos = null;
        playerPos = null;
        sw.unlockPosition();
    }

    // ═══════════════════════════════════════════════════════════
    //  UNMORPH
    // ═══════════════════════════════════════════════════════════
    private void tickUnmorph(SkinwalkerEntity sw) {
        sw.getNavigation().stop();
        sw.setDeltaMovement(0, 0, 0);
        sw.lockPosition();
        if (sw.isMorphed() && !sw.isMorphInProgress())
            SkinwalkerMorphHelper.unmorphToRealForm(sw);
        if (!sw.isMorphInProgress() && !sw.isMorphed()) {
            sw.unlockPosition();
            goTo(Phase.CHASE);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  CHASE
    // ═══════════════════════════════════════════════════════════
    private void tickChase(SkinwalkerEntity sw, Player target, ServerLevel level) {
        faceTarget(sw, target);
        double dist = sw.distanceTo(target);

        if (phaseTimer == 0 && dist > 60) {
            Path path = sw.getNavigation().createPath(target, 0);
            if (path == null || !path.canReach()) {
                if (!sw.isMorphInProgress()) {
                    SkinwalkerMorphHelper.morphToOwlForFlight(sw);
                    return;
                }
            }
        }
        phaseTimer++;

        boolean isOwl = sw.getMorphedInto().equals(ModEntities.OWL.get().getDescriptionId());
        if (isOwl) {
            double dx = target.getX() - sw.getX();
            double dy = (target.getY() + 1) - sw.getY();
            double dz = target.getZ() - sw.getZ();
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d > ATTACK_RANGE + 2) {
                double spd = Math.min(0.25, d * 0.015);
                sw.setDeltaMovement(dx / d * spd, dy / d * spd, dz / d * spd);
            } else {
                sw.setDeltaMovement(0, -0.05, 0);
                if (sw.onGround() || d <= ATTACK_RANGE) {
                    SkinwalkerMorphHelper.unmorphToRealForm(sw);
                    sw.setNoGravity(false);
                }
            }
            return;
        }

        if (sw.isMorphInProgress()) return;

        if (dist <= ATTACK_RANGE) {
            sw.getNavigation().stop();
            sw.setDeltaMovement(0, 0, 0);
            goTo(Phase.ARRIVE);
        } else {
            sw.getNavigation().moveTo(target, 1.6);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  ARRIVE
    // ═══════════════════════════════════════════════════════════
    private void tickArrive(SkinwalkerEntity sw, Player target) {
        sw.getNavigation().stop();
        sw.setDeltaMovement(0, 0, 0);
        placeFaceToFace(sw, target);
        goTo(Phase.STRONG_ATTACK);
    }

    // ═══════════════════════════════════════════════════════════
    //  STRONG_ATTACK
    // ═══════════════════════════════════════════════════════════
    private void tickStrongAttack(SkinwalkerEntity sw, Player target) {
        holdBoth(sw, target);

        if (phaseTimer == 0) {
            sw.setStrongAttacking(true);
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    STRONG_ATTACK_TICKS + SCREAM_TICKS + 20,
                    255, false, false));
        }

        phaseTimer++;

        if (phaseTimer == STUN_HIT_DELAY && !stunApplied) {
            stunApplied = true;
            target.hurt(sw.level().damageSources().mobAttack(sw), STUN_DAMAGE);
            target.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    STRONG_ATTACK_TICKS - STUN_HIT_DELAY + SCREAM_TICKS + 20,
                    0, false, false));

            if (target instanceof ServerPlayer sp) {
                ModNetwork.sendShakeToPlayer(sp, 0.6f, 15);
            }

            stunCount++;
        }

        if (phaseTimer >= STRONG_ATTACK_TICKS) {
            sw.setStrongAttacking(false);
            goTo(Phase.SCREAM);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SCREAM
    // ═══════════════════════════════════════════════════════════
    private void tickScream(SkinwalkerEntity sw, Player target, ServerLevel level) {
        holdSWOnly(sw);

        if (phaseTimer == 0) {
            sw.setScreaming(true);

            if (target instanceof ServerPlayer sp) {
                ModNetwork.sendShakeToPlayer(sp, 0.9f, SCREAM_TICKS);
            }
        }

        if (phaseTimer == SCREAM_PUSH_TICK && target instanceof ServerPlayer sp) {
            double dx = target.getX() - sw.getX();
            double dz = target.getZ() - sw.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.1) {
                target.push((dx / dist) * PUSH_STRENGTH, 0.1, (dz / dist) * PUSH_STRENGTH);
                target.hurtMarked = true;
            }
            playerPos = new Vec3(
                    target.getX() + (dx / dist) * 1.0,
                    target.getY(),
                    target.getZ() + (dz / dist) * 1.0);
        }

        if (phaseTimer > SCREAM_PUSH_TICK + 5 && playerPos != null) {
            if (target instanceof ServerPlayer sp) {
                sp.teleportTo(playerPos.x, playerPos.y, playerPos.z);
                sp.setDeltaMovement(0, 0, 0);
            }
        }

        phaseTimer++;

        if (phaseTimer >= SCREAM_TICKS) {
            sw.setScreaming(false);
            target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            target.removeEffect(MobEffects.BLINDNESS);
            releaseBoth(sw);

            if (stunCount >= 2) {
                placeFaceToFace(sw, target);
                goTo(Phase.EAT);
            } else {
                stunCooldown = STUN_CD;
                attackCooldown = ATTACK_CD;
                goTo(Phase.COMBAT);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  COMBAT
    // ═══════════════════════════════════════════════════════════
    private void tickCombat(SkinwalkerEntity sw, Player target) {
        faceTarget(sw, target);
        if (attackCooldown > 0) attackCooldown--;
        if (stunCooldown > 0) stunCooldown--;
        double dist = sw.distanceTo(target);

        if (stunCooldown <= 0 && dist <= ATTACK_RANGE) {
            sw.getNavigation().stop();
            sw.setDeltaMovement(0, 0, 0);
            placeFaceToFace(sw, target);
            goTo(Phase.STRONG_ATTACK);
            return;
        }

        if (dist <= ATTACK_RANGE && attackCooldown <= 0) {
            sw.getNavigation().stop();
            sw.setDeltaMovement(0, 0, 0);
            placeFaceToFace(sw, target);
            goTo(Phase.COMBAT_HIT);
        } else if (dist > ATTACK_RANGE + 1.5) {
            sw.getNavigation().moveTo(target, 1.6);
        } else {
            sw.getNavigation().stop();
            sw.setDeltaMovement(0, sw.getDeltaMovement().y, 0);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  COMBAT_HIT
    // ═══════════════════════════════════════════════════════════
    private void tickCombatHit(SkinwalkerEntity sw, Player target) {
        holdBoth(sw, target);

        if (phaseTimer == 0) {
            if (!usedCatchLast && sw.getRandom().nextFloat() < 0.3f) {
                sw.setStrongAttacking(true);
                usedCatchLast = true;
            } else {
                sw.setWeakAttacking(true);
                usedCatchLast = false;
            }
        }

        phaseTimer++;

        if (phaseTimer == WEAK_ATTACK_TICKS / 2 && target.isAlive()) {
            target.hurt(sw.level().damageSources().mobAttack(sw), NORMAL_DAMAGE);
            if (target instanceof ServerPlayer sp) {
                ModNetwork.sendShakeToPlayer(sp, 0.3f, 8);
            }
        }

        if (phaseTimer >= WEAK_ATTACK_TICKS) {
            sw.setWeakAttacking(false);
            sw.setStrongAttacking(false);
            releaseBoth(sw);
            attackCooldown = ATTACK_CD;
            goTo(Phase.COMBAT);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  EAT
    // ═══════════════════════════════════════════════════════════
    private void tickEat(SkinwalkerEntity sw, Player target) {
        holdBoth(sw, target);

        if (phaseTimer == 0) sw.setEating(true);
        phaseTimer++;

        if (phaseTimer == EAT_TICKS / 2 && target.isAlive()) {
            if (target instanceof ServerPlayer sp) {
                ModNetwork.sendShakeToPlayer(sp, 1.0f, 40);
                sp.level().getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal(sp.getName().getString() + " was devoured by the Skinwalker"), false);
            }
            target.hurt(sw.level().damageSources().mobAttack(sw), 999f);
        }

        if (phaseTimer >= EAT_TICKS) {
            sw.setEating(false);
            releaseBoth(sw);
            goTo(Phase.POST_EAT);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  POST_EAT
    // ═══════════════════════════════════════════════════════════
    private void tickPostEat(SkinwalkerEntity sw, ServerLevel level) {
        sw.getNavigation().stop();
        sw.setDeltaMovement(0, 0, 0);
        sw.lockPosition();
        phaseTimer++;

        if (phaseTimer >= POST_EAT_TICKS) {
            sw.unlockPosition();
            SkinwalkerMorphHelper.morphToClosestBiomeAnimal(sw, level, sw.position());
            sw.setMode(SkinwalkerMode.PASSIVE);
            sw.setModeTimer(0);
            stunCount = 0;
            phase = Phase.INACTIVE;
        }
    }
}