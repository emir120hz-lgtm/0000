package dev.kuro.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Config {
    static final Properties P = new Properties();

    static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("kuro-client.properties"); }

    public static void load() {
        try (InputStream in = Files.newInputStream(file())) { P.load(in); } catch (Exception ignored) { }
    }

    public static void save() {
        try (OutputStream out = Files.newOutputStream(file())) { P.store(out, "Kuro Client"); } catch (Exception ignored) { }
    }

    public static boolean bool(String k, boolean d) { return Boolean.parseBoolean(P.getProperty(k, String.valueOf(d))); }

    public static int num(String k, int d) {
        try { return Integer.parseInt(P.getProperty(k, String.valueOf(d))); } catch (Exception e) { return d; }
    }

    public static float flt(String k, float d) {
        try { return Float.parseFloat(P.getProperty(k, String.valueOf(d))); } catch (Exception e) { return d; }
    }

    public static void set(String k, Object v) { P.setProperty(k, String.valueOf(v)); }
}
