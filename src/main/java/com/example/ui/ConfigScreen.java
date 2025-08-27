package com.example.ui;

import com.example.config.ModConfig;
//import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private ModConfig cfg;
    private boolean masterToggle = true; // 新增總開關狀態

    public ConfigScreen(Screen parent) {
        super(Text.literal("Answer-Fun 設定"));
        this.parent = parent;
        this.cfg = ModConfig.get();
    }

    @Override
    protected void init() {
        cfg = ModConfig.get();
        int width = this.width;
        int x = width / 2 - 100;
        int y = 40;
        int dy = 24;

        // 總開關按鈕
        addDrawableChild(ButtonWidget.builder(toggleText("總開關", masterToggle), b -> {
            masterToggle = !masterToggle;
            b.setMessage(toggleText("總開關", masterToggle));
        }).dimensions(x, y, 200, 20).build()); y += dy;

        // Buttons for each toggle
        addDrawableChild(ButtonWidget.builder(toggleText("=help", cfg.enableHelp), b -> {
            if (masterToggle) {
                cfg.enableHelp = !cfg.enableHelp;
                ModConfig.save(cfg);
            }
            b.setMessage(toggleText("=help", cfg.enableHelp));
        }).dimensions(x, y, 200, 20).build()); y += dy;

        addDrawableChild(ButtonWidget.builder(toggleText("=roll", cfg.enableRoll), b -> {
            if (masterToggle) {
                cfg.enableRoll = !cfg.enableRoll;
                ModConfig.save(cfg);
            }
            b.setMessage(toggleText("=roll", cfg.enableRoll));
        }).dimensions(x, y, 200, 20).build()); y += dy;

        addDrawableChild(ButtonWidget.builder(toggleText("=time", cfg.enableTime), b -> {
            if (masterToggle) {
                cfg.enableTime = !cfg.enableTime;
                ModConfig.save(cfg);
            }
            b.setMessage(toggleText("=time", cfg.enableTime));
        }).dimensions(x, y, 200, 20).build()); y += dy;

        addDrawableChild(ButtonWidget.builder(toggleText("=king", cfg.enableKing), b -> {
            if (masterToggle) {
                cfg.enableKing = !cfg.enableKing;
                ModConfig.save(cfg);
            }
            b.setMessage(toggleText("=king", cfg.enableKing));
        }).dimensions(x, y, 200, 20).build()); y += dy;

        addDrawableChild(ButtonWidget.builder(toggleText("=jimmy", cfg.enableJimmy), b -> {
            if (masterToggle) {
                cfg.enableJimmy = !cfg.enableJimmy;
                ModConfig.save(cfg);
            }
            b.setMessage(toggleText("=jimmy", cfg.enableJimmy));
        }).dimensions(x, y, 200, 20).build()); y += dy;

        addDrawableChild(ButtonWidget.builder(toggleText("=tps", cfg.enableTps), b -> {
            if (masterToggle) {
                cfg.enableTps = !cfg.enableTps;
                ModConfig.save(cfg);
            }
            b.setMessage(toggleText("=tps", cfg.enableTps));
        }).dimensions(x, y, 200, 20).build()); y += dy;

        addDrawableChild(ButtonWidget.builder(toggleText("=ping", cfg.enablePing), b -> {
            if (masterToggle) {
                cfg.enablePing = !cfg.enablePing;
                ModConfig.save(cfg);
            }
            b.setMessage(toggleText("=ping", cfg.enablePing));
        }).dimensions(x, y, 200, 20).build()); y += dy;

        addDrawableChild(ButtonWidget.builder(toggleText("=pick", cfg.enablePick), b -> {
            if (masterToggle) {
                cfg.enablePick = !cfg.enablePick;
                ModConfig.save(cfg);
            }
            b.setMessage(toggleText("=pick", cfg.enablePick));
        }).dimensions(x, y, 200, 20).build()); y += dy;

        addDrawableChild(ButtonWidget.builder(toggleText("=yan", cfg.enableYan), b -> {
            if (masterToggle) {
                cfg.enableYan = !cfg.enableYan;
                ModConfig.save(cfg);
            }
            b.setMessage(toggleText("=yan", cfg.enableYan));
        }).dimensions(x, y, 200, 20).build()); y += dy;

        addDrawableChild(ButtonWidget.builder(toggleText("=masaki", cfg.enableMasaki), b -> {
            if (masterToggle) {
                cfg.enableMasaki = !cfg.enableMasaki;
                ModConfig.save(cfg);
            }
            b.setMessage(toggleText("=masaki", cfg.enableMasaki));
        }).dimensions(x, y, 200, 20).build()); y += dy;

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> {
            this.close();
        }).dimensions(x, y + 10, 200, 20).build());
    }

    private Text toggleText(String name, boolean enabled) {
        return Text.literal(name + " : " + (enabled ? "開啟" : "關閉"));
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    public boolean isMasterToggleEnabled() {
        return masterToggle;
    }
}
