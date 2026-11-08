package dev.kuro.client;

import java.util.Locale;

/** Tek bir ayar (anahtar, kaydirici, secim, renk veya dugme). Degerler Config'e otomatik yazilir. */
public final class Setting {
    public enum Type { TOGGLE, SLIDER, CHOICE, COLOR, BUTTON }

    public final Type type;
    public final String key, label, unit;
    public final float min, max, step, def;
    public final String[] opts;
    public final Runnable action;
    public float v;
    /** Arayuz animasyonu icin (0..1). */
    public float anim;

    private Setting(Type type, String key, String label, float min, float max, float step, float def,
                    String[] opts, String unit, Runnable action) {
        this.type = type; this.key = key; this.label = label;
        this.min = min; this.max = max; this.step = step; this.def = def;
        this.opts = opts; this.unit = unit; this.action = action;
        this.v = type == Type.BUTTON ? def : clamp(Config.flt(key, def));
        this.anim = type == Type.TOGGLE ? this.v : 0f;
    }

    public static Setting toggle(String key, String label, boolean def) {
        return new Setting(Type.TOGGLE, key, label, 0, 1, 1, def ? 1 : 0, null, "", null);
    }

    public static Setting slider(String key, String label, float min, float max, float step, float def, String unit) {
        return new Setting(Type.SLIDER, key, label, min, max, step, def, null, unit, null);
    }

    public static Setting choice(String key, String label, String[] opts, int def) {
        return new Setting(Type.CHOICE, key, label, 0, opts.length - 1, 1, def, opts, "", null);
    }

    public static Setting color(String key, String label, int def) {
        return new Setting(Type.COLOR, key, label, 0, Ui.PALETTE.length - 1, 1, def, null, "", null);
    }

    public static Setting button(String label, String text, Runnable r) {
        return new Setting(Type.BUTTON, null, label, 0, 0, 1, 0, new String[]{text}, "", r);
    }

    float clamp(float x) {
        if (Float.isNaN(x)) x = def;
        return Math.max(min, Math.min(max, x));
    }

    public boolean bool() { return v >= 0.5f; }
    public float num() { return v; }
    public int idx() { return Math.round(v); }
    public int rgb() { return Ui.PALETTE[Math.max(0, Math.min(Ui.PALETTE.length - 1, idx()))]; }

    public void set(float nv) {
        if (type == Type.SLIDER && step > 0) nv = min + Math.round((nv - min) / step) * step;
        v = clamp(nv);
        if (key != null) Config.set(key, v);
    }

    public void toggle() { set(bool() ? 0 : 1); }

    /** Secim icin ileri/geri doner. */
    public void cycle(int dir) {
        int n = (int) max + 1;
        set(((idx() + dir) % n + n) % n);
    }

    public void reset() { if (type != Type.BUTTON) set(def); }

    public String display() {
        if (type == Type.CHOICE) return opts[Math.max(0, Math.min(opts.length - 1, idx()))];
        if (type == Type.BUTTON) return opts[0];
        if (type == Type.SLIDER) {
            String s = step >= 1f ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
            return s + unit;
        }
        return bool() ? "Açık" : "Kapalı";
    }
}
