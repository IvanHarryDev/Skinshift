package com.harry.wildcraft.client.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class CameraShake {

    private static float trauma = 0.0f;
    private static final float MAX_ANGLE = 5.0f;
    private static long endTime = 0L;
    private static long startTime = 0L;
    private static float targetTrauma = 0.0f;
    private static int totalDuration = 0;
    private static float baseSpeed = 1.0f;

    public static void addTrauma(float intensity, int durationTicks) {
        trauma = 0.0f;
        targetTrauma = Math.min(1.0f, intensity * 1.5f);
        totalDuration = durationTicks;
        startTime = System.currentTimeMillis();
        endTime = startTime + (long) (durationTicks * 50L); // ticks to ms
        baseSpeed = 0.7f + intensity * 2.5f;
    }

    private static float getProgress() {
        long now = System.currentTimeMillis();
        float progress = (float) (now - startTime) / (float) (endTime - startTime);
        return Math.min(1.0f, Math.max(0.0f, progress));
    }

    private static float smoothStep(float x) {
        return x * x * (3.0f - 2.0f * x);
    }

    private static float getFadeOutMultiplier(float progress) {
        if (progress < 0.8f) return 1.0f;
        float fadeOut = (progress - 0.8f) / 0.2f;
        return 1.0f - smoothStep(fadeOut);
    }

    @SubscribeEvent
    public void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (System.currentTimeMillis() > endTime) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        float progress = getProgress();
        float fadeMultiplier;

        if (targetTrauma < 0.3f) {
            fadeMultiplier = smoothStep(1.0f - Math.abs(2.0f * progress - 1.0f));
        } else if (targetTrauma < 0.7f) {
            fadeMultiplier = 1.0f - Math.abs(2.0f * progress - 1.0f);
        } else {
            fadeMultiplier = targetTrauma >= 1.0f
                    ? 1.0f + 0.2f * (float) Math.sin(progress * Math.PI * 4.0)
                    : 1.0f;
        }

        fadeMultiplier *= getFadeOutMultiplier(progress);
        float currentTrauma = targetTrauma * fadeMultiplier;
        float shake = targetTrauma >= 1.0f
                ? currentTrauma * currentTrauma * 2.0f
                : currentTrauma * currentTrauma * 1.2f;

        float time = (float) (System.currentTimeMillis() % 1000000L) / (1000.0f / baseSpeed);

        float angleX = MAX_ANGLE * shake * ((float) Math.sin(time * (1.5f + targetTrauma))
                + (targetTrauma >= 1.0f ? 0.5f * (float) Math.sin(time * 3.7f) : 0.0f));
        float angleY = 3.5f * shake * ((float) Math.cos(time * (2.0f + targetTrauma))
                + (targetTrauma >= 1.0f ? 0.3f * (float) Math.cos(time * 4.3f) : 0.0f));

        if (targetTrauma > 0.7f) {
            float micro = targetTrauma >= 1.0f ? 3.5f : 2.5f;
            float microShake = (float) (Math.random() - 0.5) * targetTrauma * micro;
            angleX += microShake;
            angleY += microShake * 0.7f;
            if (targetTrauma >= 1.0f && Math.random() < 0.1) {
                angleX *= 1.5f;
                angleY *= 1.5f;
            }
        }

        event.setPitch(event.getPitch() + angleX);
        event.setYaw(event.getYaw() + angleY);
    }
}