package com.kcl.hitwtimer.client.mixin;

import com.kcl.hitwtimer.client.HITWtimerClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/**
 * Append our KeyMappings so they appear in the vanilla Controls screen.
 */
@Mixin(Options.class)
public class OptionsMixin {
    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hitwtimer$appendKeys(CallbackInfo ci) {
        KeyMapping[] extra = new KeyMapping[] {
            HITWtimerClient.INSTANCE.keyToggleHud,
            HITWtimerClient.INSTANCE.keyEditHud,
            HITWtimerClient.INSTANCE.keyReload
        };
        // concat avoiding dups if reload
        keyMappings = Arrays.stream(keyMappings)
            .filter(k -> !k.getName().startsWith("key.hitwtimer."))
            .toArray(KeyMapping[]::new);
        int oldLen = keyMappings.length;
        keyMappings = Arrays.copyOf(keyMappings, oldLen + extra.length);
        System.arraycopy(extra, 0, keyMappings, oldLen, extra.length);
    }
}
