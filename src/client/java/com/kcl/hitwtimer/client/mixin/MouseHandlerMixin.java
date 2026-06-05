package com.kcl.hitwtimer.client.mixin;

import com.kcl.hitwtimer.client.hud.HudRenderer;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Allow scroll wheel to adjust HUD scale while in edit mode.
 * Also basic drag support for HUD position in edit mode.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    private static boolean hitwtimer$dragging = false;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void hitwtimer$onScroll(long window, double x, double y, CallbackInfo ci) {
        if (HudRenderer.INSTANCE.isEditing() && y != 0) {
            HudRenderer.INSTANCE.adjustScale(y);
            ci.cancel();
        }
    }

    @Inject(method = "onPress", at = @At("HEAD"))
    private void hitwtimer$onPress(long window, int button, int action, int mods, CallbackInfo ci) {
        if (HudRenderer.INSTANCE.isEditing() && button == 0 /* left */) {
            if (action == 1) {
                // press
                // we don't have direct mouse pos here easily, use last known from mc
                hitwtimer$dragging = true;
                // approximate start using current mouse
                var mc = net.minecraft.client.Minecraft.getInstance();
                double mx = mc.mouseHandler.xpos();
                double my = mc.mouseHandler.ypos();
                // convert screen to gui? for simplicity pass raw and let renderer handle
                HudRenderer.INSTANCE.startDrag(mx, my);
            } else if (action == 0) {
                hitwtimer$dragging = false;
            }
        }
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    private void hitwtimer$onMove(long window, double x, double y, CallbackInfo ci) {
        if (hitwtimer$dragging && HudRenderer.INSTANCE.isEditing()) {
            HudRenderer.INSTANCE.updateDrag(x, y);
        }
    }
}
