package dev.kuro.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayDeque;

public final class Hud {
    public static int fps;
    static long lastSec = System.nanoTime();
    static int frames;
    static final ArrayDeque<Long> LC = new ArrayDeque<>(), RC = new ArrayDeque<>();
    static boolean lp, rp;
    static boolean inWorld;
    static long joinT;

    static void sample(Minecraft mc) {
        frames++;
        long n = System.nanoTime();
        if (n - lastSec >= 1_000_000_000L) { fps = frames; frames = 0; lastSec = n; }

        long now = System.currentTimeMillis();
        boolean l = mc.options.keyAttack.isDown(), r = mc.options.keyUse.isDown();
        if (l && !lp) LC.add(now);
        if (r && !rp) RC.add(now);
        lp = l; rp = r;
        while (!LC.isEmpty() && now - LC.peekFirst() > 1000) LC.pollFirst();
        while (!RC.isEmpty() && now - RC.peekFirst() > 1000) RC.pollFirst();
    }

    public static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        sample(mc);
        if (mc.player == null) { inWorld = false; return; }
        if (!inWorld) { inWorld = true; joinT = System.currentTimeMillis(); }
        if (mc.options.hideGui) return;

        if (Mods.hudShow.bool() || mc.screen instanceof KuroScreen) {
            float gs = Mods.hudScale.num();
            for (Mod m : Mods.ALL) {
                if (!m.hud || !m.enabled) continue;
                float sc = gs * m.scale();
                g.pose().pushPose();
                g.pose().translate(m.x, m.y, 0);
                g.pose().scale(sc, sc, 1f);
                m.draw(g, mc);
                g.pose().popPose();
            }
        }
        intro(g, mc);
    }

    /** Oyuna girince birkac saniye gorunen kucuk karsilama kutusu. */
    static void intro(GuiGraphics g, Minecraft mc) {
        if (!Mods.intro.bool() || mc.screen != null) return;
        long age = System.currentTimeMillis() - joinT;
        if (age > 6000) return;
        float a = age < 400 ? age / 400f : age > 5000 ? (6000 - age) / 1000f : 1f;
        if (a < 0.1f) return;
        String t1 = "KURO CLIENT";
        String t2 = "Menü: " + KuroClient.MENU.getTranslatedKeyMessage().getString();
        int w = mc.font.width(t1) + 14 + mc.font.width(t2) + 24, h = 24;
        int x = (mc.getWindow().getGuiScaledWidth() - w) / 2, y = 10 - (int) ((1f - a) * 12);
        Ui.rr(g, x, y, w, h, 6, Ui.col(0x0B0B10, 0.82f * a));
        g.fill(x, y + 4, x + 2, y + h - 4, Ui.col(Ui.acc(), a));
        g.drawString(mc.font, t1, x + 12, y + 8, Ui.col(Ui.acc(), a), false);
        g.drawString(mc.font, t2, x + 12 + mc.font.width(t1) + 14, y + 8, Ui.col(0xAAAAB8, a), false);
    }
}
