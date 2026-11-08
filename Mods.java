package dev.kuro.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class Mods {
    public static final List<Mod> ALL = new ArrayList<>();
    public static final List<Setting> THEME = new ArrayList<>(), GENERAL = new ArrayList<>();
    public static Zoom zoom;
    public static Setting accent, menuOpacity, menuDim, menuAnim, hudRadius, hudScale, hudShow, snap, intro;

    public static void init() {
        ALL.clear(); THEME.clear(); GENERAL.clear();

        accent = tm(Setting.color("theme.accent", "Vurgu rengi", 1));
        menuOpacity = tm(Setting.slider("theme.opacity", "Menü opaklığı", 60, 100, 5, 95, "%"));
        menuDim = tm(Setting.slider("theme.dim", "Arka plan karartma", 0, 70, 5, 35, "%"));
        menuAnim = tm(Setting.toggle("theme.anim", "Menü animasyonu", true));
        hudRadius = tm(Setting.slider("theme.radius", "HUD köşe yuvarlaklığı", 0, 8, 1, 4, "px"));

        hudShow = ge(Setting.toggle("hud.show", "HUD'u göster", true));
        hudScale = ge(Setting.slider("scale", "Genel HUD ölçeği", 0.5f, 2f, 0.05f, 1f, "x"));
        snap = ge(Setting.toggle("snap", "Sürüklerken ızgaraya yapış", false));
        intro = ge(Setting.toggle("intro", "Oyuna girince karşılama mesajı", true));
        ge(Setting.button("Tüm HUD konumları", "Sıfırla", () -> { for (Mod m : ALL) m.reset(); Config.save(); }));
        ge(Setting.button("Tüm modüller", "Hepsini aç", () -> setAll(true)));
        ge(Setting.button("Tüm modüller ", "Hepsini kapat", () -> setAll(false)));
        ge(Setting.button("Her şey", "Varsayılana dön", Mods::resetEverything));

        ALL.add(new Fps());
        ALL.add(new Cps());
        ALL.add(new Ping());
        ALL.add(new Clock());
        ALL.add(new Coords());
        ALL.add(new Keys());
        ALL.add(new Armor());
        ALL.add(new Potions());
        ALL.add(new Memory());
        ALL.add(new Speed());
        ALL.add(new Biome());
        ALL.add(new Server());
        ALL.add(new Sprint());
        zoom = new Zoom();
        ALL.add(zoom);
    }

    private static Setting tm(Setting s) { THEME.add(s); return s; }
    private static Setting ge(Setting s) { GENERAL.add(s); return s; }

    static void setAll(boolean on) {
        for (Mod m : ALL) { m.enabled = on; m.save(); }
        Config.save();
    }

    static void resetEverything() {
        for (Setting s : THEME) s.reset();
        for (Setting s : GENERAL) s.reset();
        for (Mod m : ALL) m.resetAll();
        Config.save();
    }

    // ---------- tek/cok satirli metin HUD'lari ----------
    static abstract class LinesMod extends Mod {
        LinesMod(String id, String name, String desc, int x, int y) { super(id, name, desc, true, x, y, true); }
        abstract String[] lines(Minecraft mc);
        int lineRgb(int i, Minecraft mc) { return i == 0 ? textRgb() : dimRgb(); }

        public void draw(GuiGraphics g, Minecraft mc) {
            String[] ls = lines(mc);
            int mw = 0;
            for (String s : ls) mw = Math.max(mw, mc.font.width(s));
            w = mw + 18; h = ls.length == 1 ? 18 : 6 + ls.length * 12;
            panel(g, w, h);
            for (int i = 0; i < ls.length; i++)
                g.drawString(mc.font, ls[i], 10, ls.length == 1 ? 5 : 5 + i * 12, Ui.col(lineRgb(i, mc), 1f), shadow());
        }
    }

    static class Fps extends LinesMod {
        final Setting label = add(Setting.toggle("fps.label", "Etiketi göster", true));
        Fps() { super("fps", "FPS Göstergesi", "Saniyedeki kare sayısı", 6, 6); }
        String[] lines(Minecraft mc) { return new String[]{(label.bool() ? "FPS  " : "") + Hud.fps}; }
    }

    static class Cps extends LinesMod {
        final Setting fmt = add(Setting.choice("cps.fmt", "Biçim", new String[]{"Sol | Sağ", "Sadece sol", "Sadece sağ", "Toplam"}, 0));
        Cps() { super("cps", "CPS Göstergesi", "Tıklama hızı", 6, 28); }
        String[] lines(Minecraft mc) {
            int l = Hud.LC.size(), r = Hud.RC.size();
            switch (fmt.idx()) {
                case 1: return new String[]{"CPS  " + l};
                case 2: return new String[]{"CPS  " + r};
                case 3: return new String[]{"CPS  " + (l + r)};
                default: return new String[]{"CPS  " + l + " | " + r};
            }
        }
    }

    static class Ping extends LinesMod {
        final Setting byPing = add(Setting.toggle("ping.color", "Gecikmeye göre renk", true));
        Ping() { super("ping", "Ping Göstergesi", "Sunucu gecikmesi", 6, 50); }
        int latency(Minecraft mc) {
            PlayerInfo info = mc.getConnection() == null || mc.player == null ? null : mc.getConnection().getPlayerInfo(mc.player.getUUID());
            return info == null ? -1 : info.getLatency();
        }
        String[] lines(Minecraft mc) { int l = latency(mc); return new String[]{l < 0 ? "Ping  --" : "Ping  " + l + " ms"}; }
        int lineRgb(int i, Minecraft mc) {
            int l = latency(mc);
            if (!byPing.bool() || l < 0) return textRgb();
            return l < 80 ? 0x4ADE80 : l < 150 ? 0xFACC15 : 0xFF5566;
        }
    }

    static class Clock extends LinesMod {
        final Setting type = add(Setting.choice("clock.type", "Tür", new String[]{"Gerçek saat", "Oyun saati"}, 0));
        final Setting h24 = add(Setting.toggle("clock.h24", "24 saat biçimi", true));
        final Setting secs = add(Setting.toggle("clock.secs", "Saniyeyi göster", false));
        static final DateTimeFormatter[] F = {
                DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH), DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("HH:mm"), DateTimeFormatter.ofPattern("HH:mm:ss")};
        Clock() { super("clock", "Saat", "Gerçek veya oyun içi saat", 6, 72); }
        String[] lines(Minecraft mc) {
            if (type.idx() == 0) return new String[]{LocalTime.now().format(F[(h24.bool() ? 2 : 0) + (secs.bool() ? 1 : 0)])};
            if (mc.level == null) return new String[]{"--:--"};
            long t = mc.level.getDayTime();
            long day = t / 24000 + 1, tod = t % 24000;
            int hh = (int) ((tod / 1000 + 6) % 24), mm = (int) (tod % 1000 * 60 / 1000);
            String time = h24.bool() ? String.format(Locale.ROOT, "%02d:%02d", hh, mm)
                    : String.format(Locale.ROOT, "%02d:%02d %s", hh % 12 == 0 ? 12 : hh % 12, mm, hh < 12 ? "AM" : "PM");
            return new String[]{"Gün " + day + "  " + time};
        }
    }

    static class Coords extends LinesMod {
        static final String[] D = {"G", "GB", "B", "KB", "K", "KD", "D", "GD"};
        final Setting dir = add(Setting.toggle("coords.dir", "Yön göster", true));
        final Setting dec = add(Setting.slider("coords.dec", "Ondalık basamak", 0, 2, 1, 0, ""));
        final Setting other = add(Setting.toggle("coords.other", "Nether/Overworld karşılığı", false));
        Coords() { super("coords", "Koordinatlar", "XYZ ve baktığın yön", 6, 94); }
        String c(double v) {
            int d = Math.round(dec.num());
            return d == 0 ? String.valueOf((int) Math.floor(v)) : String.format(Locale.ROOT, "%." + d + "f", v);
        }
        String[] lines(Minecraft mc) {
            var p = mc.player;
            List<String> ls = new ArrayList<>();
            ls.add("X " + c(p.getX()) + "   Y " + c(p.getY()) + "   Z " + c(p.getZ()));
            if (other.bool() && mc.level != null) {
                if (mc.level.dimension() == Level.OVERWORLD) ls.add("Nether  X " + c(p.getX() / 8) + "   Z " + c(p.getZ() / 8));
                else if (mc.level.dimension() == Level.NETHER) ls.add("Overworld  X " + c(p.getX() * 8) + "   Z " + c(p.getZ() * 8));
            }
            if (dir.bool()) {
                float yaw = ((p.getYRot() % 360) + 360) % 360;
                ls.add("Yön  " + D[(int) ((yaw + 22.5f) / 45f) % 8]);
            }
            return ls.toArray(new String[0]);
        }
    }

    // ---------- tus gostergesi ----------
    static class Keys extends Mod {
        final float[] pa = new float[7];
        final Setting mouse = add(Setting.toggle("keys.mouse", "Fare tuşlarını göster", true));
        final Setting space = add(Setting.toggle("keys.space", "Boşluk tuşunu göster", true));
        Keys() { super("keys", "Tuş Göstergesi", "WASD, fare ve boşluk tuşu", true, 6, 130, true); }

        void key(GuiGraphics g, Minecraft mc, int i, boolean down, String label, int x, int y, int kw, int kh) {
            pa[i] += ((down ? 1f : 0f) - pa[i]) * 0.35f;
            float base = bgOn() ? bgAlpha() : 0.15f;
            Ui.rr(g, x, y, kw, kh, Math.min(Ui.hudRadius(), kh / 2), Ui.col(Ui.mix(0x0B0B10, Ui.acc(), pa[i]), base + (0.9f - base) * pa[i]));
            int c = Ui.col(textRgb(), 1f);
            if (!label.isEmpty()) g.drawString(mc.font, label, x + (kw - mc.font.width(label)) / 2, y + (kh - 8) / 2, c, shadow());
            else g.fill(x + 8, y + kh / 2, x + kw - 8, y + kh / 2 + 1, c);
        }

        public void draw(GuiGraphics g, Minecraft mc) {
            var o = mc.options;
            int k = 22, s = 24;
            key(g, mc, 0, o.keyUp.isDown(), "W", s, 0, k, k);
            key(g, mc, 1, o.keyLeft.isDown(), "A", 0, s, k, k);
            key(g, mc, 2, o.keyDown.isDown(), "S", s, s, k, k);
            key(g, mc, 3, o.keyRight.isDown(), "D", 2 * s, s, k, k);
            int y = 2 * s;
            if (mouse.bool()) {
                key(g, mc, 4, o.keyAttack.isDown(), "SOL", 0, y, 35, 20);
                key(g, mc, 5, o.keyUse.isDown(), "SAĞ", 37, y, 35, 20);
                y += 22;
            }
            if (space.bool()) {
                key(g, mc, 6, o.keyJump.isDown(), "", 0, y, 72, 12);
                y += 14;
            }
            w = 72; h = Math.max(2 * s - 2, y - 2);
        }
    }

    // ---------- zirh ----------
    static class Armor extends Mod {
        static final EquipmentSlot[] S = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        final Setting hand = add(Setting.toggle("armor.hand", "Eldeki eşyayı göster", true));
        final Setting num = add(Setting.choice("armor.num", "Dayanıklılık", new String[]{"Kalan", "Yüzde"}, 0));
        Armor() { super("armor", "Zırh Durumu", "Zırh ve eldeki eşya dayanıklılığı", true, 6, 222, true); }

        void row(GuiGraphics g, Minecraft mc, ItemStack st, int y) {
            if (bgOn()) Ui.rr(g, 0, y, 72, 18, Ui.hudRadius(), Ui.col(0x0B0B10, bgAlpha()));
            g.renderItem(st, 1, y + 1);
            String t = "";
            if (st.isDamageableItem()) {
                int left = st.getMaxDamage() - st.getDamageValue();
                t = num.idx() == 1 ? (left * 100 / Math.max(1, st.getMaxDamage())) + "%" : String.valueOf(left);
            } else if (st.getCount() > 1) t = "x" + st.getCount();
            txt(g, mc, t, 22, y + 5, false);
        }

        public void draw(GuiGraphics g, Minecraft mc) {
            int y = 0;
            for (EquipmentSlot s : S) {
                ItemStack st = mc.player.getItemBySlot(s);
                if (!st.isEmpty()) { row(g, mc, st, y); y += 20; }
            }
            ItemStack hd = mc.player.getMainHandItem();
            if (hand.bool() && !hd.isEmpty()) { row(g, mc, hd, y); y += 20; }
            if (y == 0) {
                panel(g, 72, 18);
                txt(g, mc, "Zırh yok", 10, 5, true);
                y = 20;
            }
            w = 72; h = y - 2;
        }
    }

    // ---------- iksirler ----------
    static class Potions extends LinesMod {
        final Setting time = add(Setting.toggle("potions.time", "Süreyi göster", true));
        final Setting max = add(Setting.slider("potions.max", "Maksimum satır", 1, 8, 1, 5, ""));
        boolean none;
        Potions() { super("potions", "Etki Göstergesi", "Aktif iksir etkileri ve süreleri", 170, 94); }
        String[] lines(Minecraft mc) {
            Collection<MobEffectInstance> fx = mc.player.getActiveEffects();
            List<String> ls = new ArrayList<>();
            for (MobEffectInstance e : fx) {
                if (ls.size() >= Math.round(max.num())) break;
                String n = e.getEffect().value().getDisplayName().getString();
                if (e.getAmplifier() > 0) n += " " + (e.getAmplifier() + 1);
                if (time.bool()) {
                    if (e.isInfiniteDuration()) n += "  **";
                    else n += String.format(Locale.ROOT, "  %d:%02d", e.getDuration() / 1200, (e.getDuration() / 20) % 60);
                }
                ls.add(n);
            }
            none = ls.isEmpty();
            if (none) ls.add("Etki yok");
            return ls.toArray(new String[0]);
        }
        int lineRgb(int i, Minecraft mc) { return none ? dimRgb() : textRgb(); }
    }

    static class Memory extends LinesMod {
        final Setting fmt = add(Setting.choice("memory.fmt", "Biçim", new String[]{"Kullanılan / Toplam", "Yüzde"}, 0));
        Memory() { super("memory", "Bellek Göstergesi", "Java bellek (RAM) kullanımı", 140, 6); }
        String[] lines(Minecraft mc) {
            Runtime r = Runtime.getRuntime();
            long max = r.maxMemory(), used = r.totalMemory() - r.freeMemory();
            return new String[]{fmt.idx() == 1 ? "RAM  " + (used * 100 / Math.max(1, max)) + "%" : "RAM  " + (used >> 20) + " / " + (max >> 20) + " MB"};
        }
    }

    static class Speed extends LinesMod {
        final Setting unit = add(Setting.choice("speed.unit", "Birim", new String[]{"blok/sn", "km/sa"}, 0));
        float sp;
        Speed() { super("speed", "Hız Göstergesi", "Yatay hareket hızın", 140, 28); }
        String[] lines(Minecraft mc) {
            var p = mc.player;
            double v = Math.hypot(p.getX() - p.xo, p.getZ() - p.zo) * 20.0;
            sp += ((float) v - sp) * 0.2f;
            return new String[]{String.format(Locale.ROOT, "Hız  %.1f %s", unit.idx() == 1 ? sp * 3.6f : sp, unit.display())};
        }
    }

    static class Biome extends LinesMod {
        Biome() { super("biome", "Biyom", "Bulunduğun biyomun adı", 140, 50); }
        String[] lines(Minecraft mc) {
            if (mc.level == null) return new String[]{"Biyom  --"};
            String s = mc.level.getBiome(mc.player.blockPosition()).unwrapKey()
                    .map(k -> Component.translatable("biome." + k.location().getNamespace() + "." + k.location().getPath()).getString())
                    .orElse("?");
            return new String[]{"Biyom  " + s};
        }
    }

    static class Server extends LinesMod {
        final Setting hide = add(Setting.toggle("server.hide", "IP'yi gizle (yayıncı modu)", false));
        Server() { super("server", "Sunucu", "Bağlı olduğun sunucu adresi", 140, 72); }
        String[] lines(Minecraft mc) {
            ServerData sd = mc.getCurrentServer();
            return new String[]{sd == null ? "Tek oyunculu" : hide.bool() ? "Sunucu" : sd.ip};
        }
    }

    // ---------- oyun kolayliklari ----------
    static class Sprint extends Mod {
        boolean forced;
        final Setting sneak = add(Setting.toggle("sprint.sneak", "Eğilirken koşma", true));
        Sprint() { super("sprint", "Sürekli Koşu", "İleri basınca otomatik koşar", false, 0, 0, false); }
        public void tick(Minecraft mc) {
            var o = mc.options;
            boolean ok = enabled && mc.player != null && mc.screen == null && o.keyUp.isDown() && !(sneak.bool() && o.keyShift.isDown());
            if (ok) { o.keySprint.setDown(true); forced = true; }
            else if (forced) { o.keySprint.setDown(false); forced = false; }
        }
    }

    public static class Zoom extends Mod {
        boolean active; int saved; double cur;
        final Setting fov = add(Setting.slider("zoom.fov", "Yakınlaştırma FOV", 30, 70, 1, 30, "°"));
        final Setting smooth = add(Setting.slider("zoom.smooth", "Hız", 10, 90, 5, 50, "%"));
        Zoom() { super("zoom", "Yakınlaştırma", "C tuşunu basılı tut", false, 0, 0, true); }
        public void tick(Minecraft mc) {
            var f = mc.options.fov();
            boolean want = enabled && mc.screen == null && mc.player != null && KuroClient.ZOOM.isDown();
            if (want && !active) { saved = f.get(); cur = saved; active = true; }
            if (active) {
                double target = want ? fov.num() : saved;
                cur += (target - cur) * (smooth.num() / 100.0);
                if (!want && Math.abs(cur - saved) < 0.6) { f.set(saved); active = false; }
                else f.set((int) Math.round(cur));
            }
        }
        public void restore(Minecraft mc) {
            if (active) { mc.options.fov().set(saved); active = false; }
        }
    }
}
