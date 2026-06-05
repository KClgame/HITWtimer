package com.kcl.hitwtimer.client.mixin;

import com.kcl.hitwtimer.client.HITWtimerClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercept subtitle (and title) components so we can feed them to the trap keyword detector.
 * HITW traps often appear first as subtitle/title for 3s then chat.
 */
@Mixin(Gui.class)
public class GuiTitleMixin {
    @Shadow
    private Component subtitle;

    @Inject(method = "setSubtitle", at = @At("HEAD"))
    private void hitwtimer$onSetSubtitle(Component component, CallbackInfo ci) {
        if (component != null) {
            HITWtimerClient.INSTANCE.onSubtitle(component);
        }
    }

    // Also catch the main title if wanted (some traps may use title)
    @Inject(method = "setTitle", at = @At("HEAD"))
    private void hitwtimer$onSetTitle(Component component, CallbackInfo ci) {
        if (component != null) {
            // titles are usually short; feed too for BOTH/CHAT fallbacks if name matches
            HITWtimerClient.INSTANCE.onSubtitle(component);
        }
    }
}
