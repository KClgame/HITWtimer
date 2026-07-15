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

import java.util.ArrayList;
import java.util.List;

/**
 * Ensures HITWtimer KeyMappings appear in the vanilla Controls → Key Binds list.
 *
 * Fabric's KeyMappingHelper injects registered keys at the HEAD of Options.load().
 * We also append (idempotently) at constructor RETURN and load() RETURN so keys still
 * show up if registration is late or Fabric's process runs before our keys exist.
 */
@Mixin(Options.class)
public class OptionsMixin {
    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hitwtimer$appendKeysOnConstruct(CallbackInfo ci) {
        hitwtimer$ensureKeys();
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void hitwtimer$appendKeysOnLoad(CallbackInfo ci) {
        hitwtimer$ensureKeys();
    }

    private void hitwtimer$ensureKeys() {
        KeyMapping[] extra;
        try {
            extra = HITWtimerClient.allKeyMappings();
        } catch (Throwable t) {
            // Class may not be ready in extremely early init; skip quietly
            return;
        }
        if (extra == null || extra.length == 0 || keyMappings == null) {
            return;
        }

        List<KeyMapping> list = new ArrayList<>(keyMappings.length + extra.length);
        for (KeyMapping existing : keyMappings) {
            if (existing == null) continue;
            String name = existing.getName();
            if (name != null && name.startsWith("key.hitwtimer.")) {
                continue; // drop stale copies; re-add current instances below
            }
            list.add(existing);
        }
        for (KeyMapping key : extra) {
            if (key == null) continue;
            boolean already = false;
            for (KeyMapping existing : list) {
                if (existing == key || existing.getName().equals(key.getName())) {
                    already = true;
                    break;
                }
            }
            if (!already) {
                list.add(key);
            }
        }
        keyMappings = list.toArray(new KeyMapping[0]);
    }
}
