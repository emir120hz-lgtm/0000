package dev.kuro.client;

import net.minecraft.client.gui.GuiGraphics;

final class Ui {
    /** Vurgu rengi paleti (Ayarlar > Tema ve her modulun metin rengi). */
    static final int[] PALETTE = {
            0xFFFFFF, 0x9A5AFF, 0x4DA3FF, 0x3DDCFF, 0x4ADE80,
            0xFACC15, 0xFB923C, 0xFF5566, 0xF472B6, 0xAAAAB8};
    static final String[] PALETTE_NAMES = {"Beyaz", "Mor", "Mavi", "Camgöbeği", "Yesil", "Sarı", "Turuncu", "Kırmızı", "Pembe", "Gri"};

    static int acc() { return Mods.accent == null ? 0x9A5AFF : Mods.accent.rgb(); }
    static int acc2() { return mix(acc(), 0x000000, 0.28f); }
    static int hudRadius() { return Mods.hudRadius == null ? 4 : Math.round(Mods.hudRadius.num()); }

    static int col(int rgb, float a) {
        int al = Math.max(0, Math.min(255, (int) (a * 255)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return (r << 16) | (g << 8) | bl;
    }

    /** Rounded rectangle without overlapping fills (safe for translucent colors). */
    static void rr(GuiGraphics g, int x, int y, int w, int h, int r, int c) {
        if (r <= 0) { g.fill(x, y, x + w, y + h, c); return; }
        r = Math.max(1, Math.min(r, Math.min(w, h) / 2));
        g.fill(x, y + r, x + w, y + h - r, c);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.ceil(r - Math.sqrt(r * r - dy * dy));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, c);
            g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, c);
        }
    }

    static void outline(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y + 1, x + 1, y + h - 1, c);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }

    static boolean in(double px, double py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    static String ellipsize(net.minecraft.client.gui.Font f, String s, int max) {
        if (f.width(s) <= max) return s;
        while (s.length() > 1 && f.width(s + "..") > max) s = s.substring(0, s.length() - 1);
        return s + "..";
    }
}
