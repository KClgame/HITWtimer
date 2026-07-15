package com.kcl.hitwtimer.client.mixin;

import com.kcl.hitwtimer.client.hud.HudRenderer;
import kotlin.Pair;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders the HITWtimer HUD on top of the vanilla GUI.
 *
 * Position (x, y) is the top-left corner in GUI-scaled coordinates.
 * Scale is applied via pose stack so text/background grow from that corner
 * without shifting the stored position (old code wrongly did x = baseX * scale).
 */
@Mixin(Gui.class)
public class GuiHudRenderMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void hitwtimer$renderHud(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        HudRenderer.HudSnapshot snapshot = HudRenderer.INSTANCE.getSnapshot();

        if (!com.kcl.hitwtimer.client.config.HitwConfig.INSTANCE.isHudVisible()) return;
        boolean hasContent = !snapshot.getLines().isEmpty();
        if (!hasContent && !snapshot.getEditing()) return;

        Font font = Minecraft.getInstance().font;
        float scale = snapshot.getScale();
        if (scale <= 0.01f) scale = 1.0f;

        int baseX = snapshot.getX();
        int baseY = snapshot.getY();
        int hPad = snapshot.getHPadding();
        int vPad = snapshot.getVPadding();
        int lineH = font.lineHeight + 2;

        Matrix3x2fStack pose = extractor.pose();
        pose.pushMatrix();
        // Translate to stored HUD origin, then scale content around that origin
        pose.translate(baseX, baseY);
        pose.scale(scale, scale);

        // Local coordinates after transform start at (0, 0)
        int localX = 0;
        int localY = 0;

        if (hasContent) {
            int bgColor = snapshot.getBgColor();
            if ((bgColor & 0xFF000000) != 0) {
                int maxW = 0;
                for (Object raw : snapshot.getLines()) {
                    @SuppressWarnings("unchecked")
                    Pair<String, Integer> pair = (Pair<String, Integer>) raw;
                    int w = font.width(pair.getFirst());
                    if (w > maxW) maxW = w;
                }
                int totalH = snapshot.getLines().size() * lineH + vPad * 2;
                extractor.fill(localX - hPad, localY - vPad, localX + maxW + hPad, localY + totalH, bgColor);

                // Edit-mode border so the panel is easy to grab
                if (snapshot.getEditing()) {
                    extractor.outline(localX - hPad - 1, localY - vPad - 1, maxW + hPad * 2 + 2, totalH + 2, 0xFFFFFF55);
                }
            }
        }

        int ly = localY;
        for (Object raw : snapshot.getLines()) {
            @SuppressWarnings("unchecked")
            Pair<String, Integer> pair = (Pair<String, Integer>) raw;
            int color = pair.getSecond() | 0xFF000000;
            extractor.text(font, pair.getFirst(), localX, ly, color);
            ly += lineH;
        }

        if (snapshot.getEditing()) {
            extractor.text(font, "\u00a7e[EDIT]", localX - 4, ly + 4, 0xFFFFFF55);
        }

        pose.popMatrix();
    }
}
