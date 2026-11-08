package dev.kuro.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public abstract class Mod {
    public final String id, name, desc;
    public final boolean hud, defOn;
    public boolean enabled;
    public int x, y, w = 60, h = 18;
    public float anim;
    final int dx, dy;

    /** Modüle özel ayarlar önce, ortak görünüm ayarları sonra gösterilir. */
    public final List<Setting> custom = new ArrayList<>(), common = new ArrayList<>();
    private List<Setting> all;
    protected Setting sColor, sBg, sBgA, sBar, sShadow, sScale;

    protected Mod(String id, String name, String desc, boolean hud, int dx, int dy, boolean defOn) {
        this.id = id; this.name = name; this.desc = desc; this.hud = hud; this.dx = dx; this.dy = dy; this.defOn = defOn;
        enabled = Config.bool(id + ".on", defOn);
        x = Config.num(id + ".x", dx);
        y = Config.num(id + ".y", dy);
        anim = enabled ? 1f : 0f;
        if (hud) {
            sColor = com(Setting.color(id + ".color", "Metin rengi", 0));
            sBg = com(Setting.toggle(id + ".bg", "Arka plan", true));
            sBgA = com(Setting.slider(id + ".bga", "Arka plan opaklığı", 0, 100, 5, 66, "%"));
            sBar = com(Setting.toggle(id + ".bar", "Vurgu çizgisi", true));
            sShadow = com(Setting.toggle(id + ".shadow", "Metin gölgesi", false));
            sScale = com(Setting.slider(id + ".scale", "Ölçek", 0.5f, 2f, 0.05f, 1f, "x"));
        }
    }

    private Setting com(Setting s) { common.add(s); return s; }
    protected Setting add(Setting s) { custom.add(s); return s; }

    public List<Setting> settings() {
        if (all == null) { all = new ArrayList<>(custom); all.addAll(common); }
        return all;
    }

    public float scale() { return sScale == null ? 1f : sScale.num(); }

    public void save() {
        Config.set(id + ".on", enabled);
        Config.set(id + ".x", x);
        Config.set(id + ".y", y);
    }

    public void reset() { x = dx; y = dy; save(); }

    /** Konum, durum ve bütün ayarları varsayılana döndürür. */
    public void resetAll() {
        for (Setting s : settings()) s.reset();
        enabled = defOn;
        reset();
    }

    // ---------- cizim yardimcilari ----------
    protected int textRgb() { return sColor == null ? 0xFFFFFF : sColor.rgb(); }
    protected int dimRgb() { return Ui.mix(textRgb(), 0x6E6E82, 0.55f); }
    protected boolean shadow() { return sShadow != null && sShadow.bool(); }
    protected boolean bgOn() { return sBg == null || sBg.bool(); }
    protected float bgAlpha() { return sBgA == null ? 0.66f : sBgA.num() / 100f; }

    /** HUD kutusunun arka plani ve vurgu cizgisi. */
    protected void panel(GuiGraphics g, int w, int h) {
        if (!bgOn()) return;
        Ui.rr(g, 0, 0, w, h, Ui.hudRadius(), Ui.col(0x0B0B10, bgAlpha()));
        if (sBar != null && sBar.bool()) g.fill(0, Math.min(3, h / 4), 2, h - Math.min(3, h / 4), 0xFF000000 | Ui.acc());
    }

    protected void txt(GuiGraphics g, Minecraft mc, String s, int tx, int ty, boolean dim) {
        g.drawString(mc.font, s, tx, ty, Ui.col(dim ? dimRgb() : textRgb(), 1f), shadow());
    }

    /** Draws at origin (0,0); the caller translates and scales. Must update w/h. */
    public void draw(GuiGraphics g, Minecraft mc) { }

    public void tick(Minecraft mc) { }
}
