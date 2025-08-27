package com.example.mixin;

import com.example.ui.ConfigScreen;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class ExampleMixin {
    @Inject(at = @At("HEAD"), method = "run")
    private void run(CallbackInfo info) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof ConfigScreen) {
            ConfigScreen configScreen = (ConfigScreen) mc.currentScreen;
            if (!configScreen.isMasterToggleEnabled()) {
                return; // 總開關關閉，不執行指令
            }
        }
        // ... 其他指令執行的程式碼 ...
    }
}
