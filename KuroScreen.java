package dev.kuro.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class KuroScreen extends Screen {
    // panel ve yerlesim (mantiksal koordinatlar; ekran kucukse otomatik olceklenir)
    static final int PW = 640, PH = 440, SB = 150, CX = 166, CW = PW - CX - 16, TOP = 70, BOT = PH - 16;
    static final int CARD_W = 224, CARD_H = 46, CARD_G = 10, CARD_ROWS = 6;
    static final int RH = 30, RG = 4, ROWS_VIS = 10;
    static final String[] TABS = {"Modüller", "Tema", "Genel"};
    static final String[] SUBS = {"", "Menü ve HUD görünümü", "Genel davranış ve sıfırlama"};

    final long t0 = System.nanoTime();
    int tab, scroll;
    float tabAnim;
    Mod openMod, dragMod;
    Setting dragSet;
    int dragTx, offX, offY;

    public KuroScreen() { super(Component.literal("Kuro")); }

    @Override public boolean isPauseScreen() { return false; }

    float scale() { return Math.min(1f, Math.min((width - 16f) / PW, (height - 16f) / PH)); }
    double lx(double mx) { return (mx - width / 2.0) / scale() + PW / 2.0; }
    double ly(double my) { return (my - height / 2.0) / scale() + PH / 2.0; }
    static boolean in(double px, double py, int x, int y, int w, int h) { return Ui.in(px, py, x, y, w, h); }

    List<Setting> curSettings() {
        if (tab == 0) return openMod == null ? null : openMod.settings();
        return tab == 1 ? Mods.THEME : Mods.GENERAL;
    }

    int maxScroll() {
        List<Setting> l = curSettings();
        if (l == null) return Math.max(0, (Mods.ALL.size() + 1) / 2 - CARD_ROWS);
        return Math.max(0, l.size() - ROWS_VIS);
    }

    int enabledCount() { int n = 0; for (Mod m : Mods.ALL) if (m.enabled) n++; return n; }

    // ---------- yerlesim yardimcilari ----------
    static int ctlX(Setting s) {
        switch (s.type) {
            case TOGGLE: return CW - 12 - 34;
            case SLIDER: return CW - 12 - 140;
            case COLOR: return CW - 12 - 176;
            default: return CW - 12 - 130;
        }
    }

    // ---------- cizim ----------
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        boolean an = Mods.menuAnim.bool();
        float a = an ? Math.min(1f, (System.nanoTime() - t0) / 220e6f) : 1f;
        float e = 1f - (1f - a) * (1f - a) * (1f - a);
        g.fill(0, 0, width, height, Ui.col(0x000000, Mods.menuDim.num() / 100f * e));

        // HUD elemanlari: surukleme cerceveleri
        float gs = Mods.hudScale.num();
        for (Mod m : Mods.ALL) {
            if (!m.hud || !m.enabled) continue;
            float eff = gs * m.scale();
            int bw = (int) (m.w * eff), bh = (int) (m.h * eff);
            boolean hv = m == dragMod || in(mx, my, m.x, m.y, bw, bh);
            Ui.outline(g, m.x - 2, m.y - 2, bw + 4, bh + 4, Ui.col(Ui.acc(), hv ? 0.95f : 0.45f));
        }

        float s = scale() * (an ? 0.94f + 0.06f * e : 1f);
        g.pose().pushPose();
        g.pose().translate(width / 2f, height / 2f, 0);
        g.pose().scale(s, s, 1f);
        g.pose().translate(-PW / 2f, -PH / 2f, 0);
        double lx = lx(mx), ly = ly(my);
        int acc = Ui.acc();
        float op = Mods.menuOpacity.num() / 100f * e;

        Ui.rr(g, 0, 0, PW, PH, 14, Ui.col(0x0B0B10, op));
        Ui.rr(g, 8, 8, SB - 8, PH - 16, 10, Ui.col(0x12121A, op));

        // kenar cubugu
        g.pose().pushPose();
        g.pose().translate(22, 20, 0);
        g.pose().scale(2.4f, 2.4f, 1f);
        g.drawString(font, "KURO", 0, 0, Ui.col(acc, e), false);
        g.pose().popPose();
        g.drawString(font, "Client  •  v2.0", 22, 46, Ui.col(0x8C8CA0, e), false);
        g.fill(22, 66, SB - 14, 67, Ui.col(acc, 0.35f * e));

        tabAnim += (tab - tabAnim) * 0.3f;
        int hy = 80 + Math.round(tabAnim * 36);
        Ui.rr(g, 16, hy, 118, 30, 8, Ui.col(acc, 0.20f * e));
        g.fill(16, hy + 6, 18, hy + 24, Ui.col(acc, e));
        for (int i = 0; i < TABS.length; i++) {
            int ty = 80 + i * 36;
            boolean hv = in(lx, ly, 16, ty, 118, 30);
            if (hv && i != tab) Ui.rr(g, 16, ty, 118, 30, 8, Ui.col(0xFFFFFF, 0.05f * e));
            g.drawString(font, TABS[i], 30, ty + 11, Ui.col(i == tab ? 0xFFFFFF : 0x9A9AB0, e), false);
        }
        g.drawString(font, "Menü tuşu", 22, PH - 44, Ui.col(0x6E6E82, e), false);
        g.drawString(font, KuroClient.MENU.getTranslatedKeyMessage().getString(), 22, PH - 32, Ui.col(0xDDDDEE, e), false);

        // kapat
        boolean closeHv = in(lx, ly, PW - 34, 12, 20, 20);
        Ui.rr(g, PW - 34, 12, 20, 20, 7, Ui.col(closeHv ? 0xFF5566 : 0x1E1E2A, e));
        g.drawString(font, "X", PW - 34 + 7, 12 + 6, Ui.col(0xFFFFFF, e), false);

        // baslik
        String title, sub;
        if (tab == 0 && openMod != null) { title = openMod.name; sub = openMod.desc; }
        else if (tab == 0) { title = "Modüller"; sub = enabledCount() + " / " + Mods.ALL.size() + " aktif  •  HUD'ları sürükleyerek taşıyabilirsin"; }
        else { title = TABS[tab]; sub = SUBS[tab]; }
        g.pose().pushPose();
        g.pose().translate(CX, 16, 0);
        g.pose().scale(1.6f, 1.6f, 1f);
        g.drawString(font, title, 0, 0, Ui.col(0xFFFFFF, e), false);
        g.pose().popPose();
        g.drawString(font, Ui.ellipsize(font, sub, 290), CX, 40, Ui.col(0x8C8CA0, e), false);

        if (tab == 0 && openMod != null) {
            btn(g, 462, 12, 64, 20, "Sıfırla", in(lx, ly, 462, 12, 64, 20), e);
            btn(g, 534, 12, 64, 20, "< Geri", in(lx, ly, 534, 12, 64, 20), e);
        }

        // govde
        List<Setting> list = curSettings();
        if (list == null) drawCards(g, lx, ly, e);
        else drawRows(g, list, lx, ly, e);
        scrollBar(g, list == null ? (Mods.ALL.size() + 1) / 2 : list.size(), list == null ? CARD_ROWS : ROWS_VIS, e);

        g.pose().popPose();
    }

    void btn(GuiGraphics g, int x, int y, int w, int h, String t, boolean hv, float e) {
        Ui.rr(g, x, y, w, h, 7, Ui.col(hv ? 0x2E2E40 : 0x1E1E2A, e));
        g.drawString(font, t, x + (w - font.width(t)) / 2, y + (h - 8) / 2, Ui.col(0xFFFFFF, e), false);
    }

    void scrollBar(GuiGraphics g, int total, int vis, float e) {
        if (total <= vis) return;
        int th = BOT - TOP, bh = Math.max(16, th * vis / total);
        int by = TOP + (th - bh) * scroll / Math.max(1, total - vis);
        Ui.rr(g, CX + CW + 5, TOP, 3, th, 1, Ui.col(0xFFFFFF, 0.06f * e));
        Ui.rr(g, CX + CW + 5, by, 3, bh, 1, Ui.col(Ui.acc(), 0.8f * e));
    }

    void drawCards(GuiGraphics g, double lx, double ly, float e) {
        int acc = Ui.acc();
        for (int i = 0; i < Mods.ALL.size(); i++) {
            int row = i / 2 - scroll;
            if (row < 0 || row >= CARD_ROWS) continue;
            Mod m = Mods.ALL.get(i);
            int cx = CX + (i % 2) * (CARD_W + CARD_G), cy = TOP + row * (CARD_H + CARD_G);
            boolean hv = in(lx, ly, cx, cy, CARD_W, CARD_H);
            m.anim += ((m.enabled ? 1f : 0f) - m.anim) * 0.25f;
            Ui.rr(g, cx, cy, CARD_W, CARD_H, 8, Ui.col(hv ? 0x1E1E2A : 0x15151C, e));
            if (m.anim > 0.02f) g.fill(cx, cy + 8, cx + 2, cy + CARD_H - 8, Ui.col(acc, m.anim * e));
            g.drawString(font, m.name, cx + 14, cy + 9, Ui.col(0xFFFFFF, e), false);
            g.drawString(font, Ui.ellipsize(font, m.desc, CARD_W - 14 - 12 - 34 - 8 - 18 - 8), cx + 14, cy + 24, Ui.col(0x8C8CA0, e), false);
            g.drawString(font, m.hud ? "HUD" : "OYUN", cx + 14, cy + 35, Ui.col(acc, 0.7f * e), false);
            // ayar dugmesi
            int gx = cx + CARD_W - 12 - 34 - 8 - 18, gy = cy + 14;
            boolean gh = in(lx, ly, gx, gy, 18, 18);
            Ui.rr(g, gx, gy, 18, 18, 6, Ui.col(gh ? 0x34344A : 0x22222E, e));
            int[] off = {5, 1, 7};
            for (int k = 0; k < 3; k++) {
                g.fill(gx + 4, gy + 5 + k * 4, gx + 14, gy + 6 + k * 4, Ui.col(0xAAAAB8, e));
                g.fill(gx + 4 + off[k], gy + 4 + k * 4, gx + 7 + off[k], gy + 7 + k * 4, Ui.col(0xFFFFFF, e));
            }
            sw(g, cx + CARD_W - 12 - 34, cy + 15, m.anim, e);
        }
    }

    void sw(GuiGraphics g, int x, int y, float anim, float e) {
        Ui.rr(g, x, y, 34, 16, 8, Ui.col(Ui.mix(0x3A3A48, Ui.acc(), anim), e));
        Ui.rr(g, x + 2 + (int) (18 * anim), y + 2, 12, 12, 6, Ui.col(0xFFFFFF, e));
    }

    void drawRows(GuiGraphics g, List<Setting> list, double lx, double ly, float e) {
        int acc = Ui.acc();
        for (int i = scroll; i < list.size() && i < scroll + ROWS_VIS; i++) {
            Setting s = list.get(i);
            int ry = TOP + (i - scroll) * (RH + RG), cx = CX + ctlX(s);
            boolean hv = in(lx, ly, CX, ry, CW, RH);
            Ui.rr(g, CX, ry, CW, RH, 8, Ui.col(hv ? 0x1B1B26 : 0x15151C, e));
            g.drawString(font, s.label.trim(), CX + 14, ry + 11, Ui.col(0xFFFFFF, e), false);
            switch (s.type) {
                case TOGGLE:
                    s.anim += ((s.bool() ? 1f : 0f) - s.anim) * 0.25f;
                    sw(g, cx, ry + 7, s.anim, e);
                    break;
                case SLIDER: {
                    float t = s.max > s.min ? (s.v - s.min) / (s.max - s.min) : 0f;
                    Ui.rr(g, cx, ry + 13, 140, 4, 2, Ui.col(0x2A2A3A, e));
                    Ui.rr(g, cx, ry + 13, Math.max(4, (int) (140 * t)), 4, 2, Ui.col(acc, e));
                    Ui.rr(g, cx + (int) (130 * t), ry + 10, 10, 10, 5, Ui.col(0xFFFFFF, e));
                    String v = s.display();
                    g.drawString(font, v, cx - 10 - font.width(v), ry + 11, Ui.col(0xAAAAB8, e), false);
                    break;
                }
                case CHOICE: {
                    Ui.rr(g, cx, ry + 6, 130, 18, 8, Ui.col(in(lx, ly, cx, ry + 6, 130, 18) ? 0x2E2E40 : 0x1E1E2A, e));
                    String v = s.display();
                    g.drawString(font, v, cx + (130 - font.width(v)) / 2, ry + 11, Ui.col(0xFFFFFF, e), false);
                    g.drawString(font, "<", cx + 7, ry + 11, Ui.col(acc, e), false);
                    g.drawString(font, ">", cx + 130 - 7 - font.width(">"), ry + 11, Ui.col(acc, e), false);
                    break;
                }
                case COLOR:
                    for (int j = 0; j < Ui.PALETTE.length; j++) {
                        int sx = cx + j * 18;
                        Ui.rr(g, sx, ry + 8, 14, 14, 3, Ui.col(Ui.PALETTE[j], e));
                        if (j == s.idx()) Ui.outline(g, sx - 2, ry + 6, 18, 18, Ui.col(0xFFFFFF, e));
                    }
                    break;
                default: {
                    boolean bh = in(lx, ly, cx, ry + 6, 130, 18);
                    Ui.rr(g, cx, ry + 6, 130, 18, 8, Ui.col(bh ? acc : Ui.mix(0x1E1E2A, acc, 0.35f), e));
                    String v = s.display();
                    g.drawString(font, v, cx + (130 - font.width(v)) / 2, ry + 11, Ui.col(0xFFFFFF, e), false);
                }
            }
        }
    }

    // ---------- girdi ----------
    void sliderFrom(Setting s, double lx) {
        double t = Math.max(0, Math.min(1, (lx - dragTx - 5) / 130.0));
        s.set((float) (s.min + t * (s.max - s.min)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        double lx = lx(mx), ly = ly(my);
        if (in(lx, ly, 0, 0, PW, PH)) {
            if (btn <= 1) panelClick(lx, ly, btn);
            return true;
        }
        if (btn == 0) {
            float gs = Mods.hudScale.num();
            for (int i = Mods.ALL.size() - 1; i >= 0; i--) {
                Mod m = Mods.ALL.get(i);
                if (!m.hud || !m.enabled) continue;
                float eff = gs * m.scale();
                if (in(mx, my, m.x, m.y, (int) (m.w * eff), (int) (m.h * eff))) {
                    dragMod = m; offX = (int) mx - m.x; offY = (int) my - m.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    void panelClick(double lx, double ly, int btn) {
        if (btn == 0 && in(lx, ly, PW - 34, 12, 20, 20)) { onClose(); return; }
        if (btn == 0) {
            for (int i = 0; i < TABS.length; i++)
                if (in(lx, ly, 16, 80 + i * 36, 118, 30)) { tab = i; openMod = null; scroll = 0; return; }
        }
        if (tab == 0 && openMod != null && btn == 0) {
            if (in(lx, ly, 534, 12, 64, 20)) { openMod = null; scroll = 0; return; }
            if (in(lx, ly, 462, 12, 64, 20)) { openMod.resetAll(); Config.save(); return; }
        }
        List<Setting> list = curSettings();
        if (list == null) {
            for (int i = 0; i < Mods.ALL.size(); i++) {
                int row = i / 2 - scroll;
                if (row < 0 || row >= CARD_ROWS) continue;
                int cx = CX + (i % 2) * (CARD_W + CARD_G), cy = TOP + row * (CARD_H + CARD_G);
                if (!in(lx, ly, cx, cy, CARD_W, CARD_H)) continue;
                Mod m = Mods.ALL.get(i);
                if (btn == 1 || in(lx, ly, cx + CARD_W - 12 - 34 - 8 - 18 - 2, cy + 12, 22, 22)) { openMod = m; scroll = 0; }
                else { m.enabled = !m.enabled; m.save(); Config.save(); }
                return;
            }
            return;
        }
        for (int i = scroll; i < list.size() && i < scroll + ROWS_VIS; i++) {
            Setting s = list.get(i);
            int ry = TOP + (i - scroll) * (RH + RG), cx = CX + ctlX(s);
            if (!in(lx, ly, CX, ry, CW, RH)) continue;
            switch (s.type) {
                case TOGGLE:
                    if (btn == 0) { s.toggle(); Config.save(); }
                    break;
                case SLIDER:
                    if (btn == 0 && in(lx, ly, cx - 6, ry, 152, RH)) { dragSet = s; dragTx = cx; sliderFrom(s, lx); }
                    break;
                case CHOICE:
                    if (in(lx, ly, cx, ry + 6, 130, 18)) {
                        s.cycle(btn == 1 || lx < cx + 65 ? -1 : 1);
                        Config.save();
                    }
                    break;
                case COLOR:
                    if (btn == 0) for (int j = 0; j < Ui.PALETTE.length; j++)
                        if (in(lx, ly, cx + j * 18 - 2, ry + 6, 18, 18)) { s.set(j); Config.save(); }
                    break;
                default:
                    if (btn == 0 && in(lx, ly, cx, ry + 6, 130, 18) && s.action != null) s.action.run();
            }
            return;
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragSet != null && btn == 0) { sliderFrom(dragSet, lx(mx)); return true; }
        if (dragMod != null && btn == 0) {
            float eff = Mods.hudScale.num() * dragMod.scale();
            int nx = (int) mx - offX, ny = (int) my - offY;
            if (Mods.snap.bool()) { nx = Math.round(nx / 4f) * 4; ny = Math.round(ny / 4f) * 4; }
            dragMod.x = Math.max(0, Math.min(width - (int) (dragMod.w * eff), nx));
            dragMod.y = Math.max(0, Math.min(height - (int) (dragMod.h * eff), ny));
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (dragSet != null) { dragSet = null; Config.save(); return true; }
        if (dragMod != null) { dragMod.save(); Config.save(); dragMod = null; return true; }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (in(lx(mx), ly(my), 0, 0, PW, PH)) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(sy)));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (KuroClient.MENU.matches(key, scan)) { onClose(); return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE && tab == 0 && openMod != null) { openMod = null; scroll = 0; return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public void removed() { Config.save(); }
}
