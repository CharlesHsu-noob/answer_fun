package com.example.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "answer-fun.json";

    // Feature toggles (defaults: enabled)
    public boolean enableHelp = true;
    public boolean enableRoll = true;
    public boolean enableTime = true;
    public boolean enableKing = true;
    public boolean enableJimmy = true;
    public boolean enableTps = true;

    public boolean enablePing = true;
    public boolean enablePick = true;
    public boolean enableYan = true;
    public boolean enableMasaki = true;

    private static ModConfig INSTANCE;

    public static ModConfig get() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static ModConfig load() {
        Path path = getConfigPath();
        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path)) {
                ModConfig cfg = GSON.fromJson(r, ModConfig.class);
                if (cfg != null) {
                    INSTANCE = cfg;
                    return cfg;
                }
            } catch (IOException ignored) {
            } catch (Throwable t) {
                // If file is corrupted, fall through to defaults
            }
        }
        INSTANCE = new ModConfig();
        save(INSTANCE);
        return INSTANCE;
    }

    public static void save(ModConfig cfg) {
        try {
            Path path = getConfigPath();
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                GSON.toJson(cfg, w);
            }
        } catch (IOException ignored) {
        }
    }
}
