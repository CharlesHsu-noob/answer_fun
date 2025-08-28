package com.example;

import com.example.commands.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.example.ui.ConfigScreen;
import com.example.config.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Answerfun implements ClientModInitializer {
    public static final String MOD_ID = "answer-fun";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static KeyBinding OPEN_CONFIG_KEY;

    // MiniHUD 反射支援（若存在）
    private static Boolean MINIHUD_AVAILABLE = null;
    private static Method getServerTpsMethod = null;
    private static Method getServerMsptMethod = null;
    private static Class<?> miniHudMainClass = null; // 可能為 MiscUtils 或 MiniHud
    private static Class<?> miniHudDataClass = null; // DataStorage / DataStroge / data.DataStorage
    private static Field fieldServerTPS = null;
    private static Field fieldServerMSPT = null;
    private static Field fieldServerTPSValid = null;
    private static Method miniDataGetInstance = null; // getInstance/get/getDataStorage/getDataStroge
    private static Object miniDataInstance = null; // DataStorage 實例（若存在）

    static {
        // 檢查日誌級別
        LOGGER.info("目前日誌級別: ERROR={}, WARN={}, INFO={}, DEBUG={}, TRACE={}",
                LOGGER.isErrorEnabled(),
                LOGGER.isWarnEnabled(),
                LOGGER.isInfoEnabled(),
                LOGGER.isDebugEnabled(),
                LOGGER.isTraceEnabled()
        );
    }

    // ===== TPS/MSPT 取得（對外提供）=====
    public static double getEstimatedServerTps() {
        try {
            if (isMinihudAvailable()) {
                initMinihudReflection();
                // 1) 直接呼叫 MiniHUD 的靜態方法（若存在）
                if (getServerTpsMethod != null) {
                    try {
                        Object val = getServerTpsMethod.invoke(null);
                        if (val instanceof Number n) return n.doubleValue();
                    } catch (Throwable t) {
                        LOGGER.debug("invoke getServerTPS failed", t);
                    }
                }
                // 2) 從資料儲存類讀取欄位（若存在）
                Double v = tryReadMiniDataDouble(fieldServerTPS);
                if (v != null) return v;
                LOGGER.info("MiniHUD installed but TPS source unavailable (API changed). Using default.");
            }
        } catch (Throwable t) {
            LOGGER.debug("getEstimatedServerTps encountering error", t);
        }
        if (Boolean.FALSE.equals(MINIHUD_AVAILABLE)) {
            LOGGER.info("MiniHUD not detected; returning default TPS 20.0");
        }
        return 20.0; // 預設
    }

    public static double getEstimatedMspt() {
        try {
            if (isMinihudAvailable()) {
                initMinihudReflection();
                // 1) 直接呼叫 MiniHUD 的靜態方法（若存在）
                if (getServerMsptMethod != null) {
                    try {
                        Object val = getServerMsptMethod.invoke(null);
                        if (val instanceof Number n) return n.doubleValue();
                    } catch (Throwable t) {
                        LOGGER.debug("invoke getServerMSPT failed", t);
                    }
                }
                // 2) 從資料儲存類讀取欄位（若存在）
                Double v = tryReadMiniDataDouble(fieldServerMSPT);
                if (v != null) return v;
                LOGGER.info("MiniHUD installed but MSPT source unavailable (API changed). Using default.");
            }
        } catch (Throwable t) {
            LOGGER.debug("getEstimatedMspt encountering error", t);
        }
        if (Boolean.FALSE.equals(MINIHUD_AVAILABLE)) {
            LOGGER.info("MiniHUD not detected; returning default MSPT 0.0");
        }
        return 0.0; // 預設
    }

    private static boolean isMinihudAvailable() {
        if (MINIHUD_AVAILABLE != null) return MINIHUD_AVAILABLE;
        try {
            MINIHUD_AVAILABLE = FabricLoader.getInstance().isModLoaded("minihud");
            return MINIHUD_AVAILABLE;
        } catch (Throwable t) {
            LOGGER.debug("isMinihudAvailable failed", t);
            MINIHUD_AVAILABLE = false;
            return false;
        }
    }

    // 對外暴露：是否有安裝 MiniHUD
    public static boolean isMinihudPresent() {
        return isMinihudAvailable();
    }

    private static void initMinihudReflection() {
        // 先嘗試 MiniHUD 的工具類（0.34.x 常見：MiscUtils），若不可用再回退到舊的 MiniHud 類
        if (miniHudMainClass == null) {
            miniHudMainClass = tryClass("fi.dy.masa.minihud.util.MiscUtils");
            if (miniHudMainClass != null) {
                // 嘗試多組可能的方法名稱
                getServerTpsMethod = tryResolveMethod(miniHudMainClass,
                        new String[]{"getServerTPS", "getServerTps", "getServerTPSAverage", "getServerTpsAverage"});
                getServerMsptMethod = tryResolveMethod(miniHudMainClass,
                        new String[]{"getServerMSPT", "getServerMspt", "getServerMSPTAverage", "getServerMsptAverage"});
            }
            // 若上述未取得，嘗試舊類名 MiniHud 靜態方法
            if (getServerTpsMethod == null && getServerMsptMethod == null) {
                Class<?> legacy = tryClass("fi.dy.masa.minihud.MiniHud");
                if (legacy != null) {
                    Method m1 = tryMethod(legacy, "getServerTPS");
                    Method m2 = tryMethod(legacy, "getServerMSPT");
                    if (m1 != null) getServerTpsMethod = m1;
                    if (m2 != null) getServerMsptMethod = m2;
                    if (miniHudMainClass == null) miniHudMainClass = legacy;
                }
            }
        }

        // 嘗試 DataStorage 類（有些版本為 util 包，有些為 data 包，亦有拼寫 DataStroge）
        if (miniHudDataClass == null) {
            miniHudDataClass = tryClass("fi.dy.masa.minihud.util.DataStorage");
            if (miniHudDataClass == null) miniHudDataClass = tryClass("fi.dy.masa.minihud.util.DataStroge");
            if (miniHudDataClass == null) miniHudDataClass = tryClass("fi.dy.masa.minihud.data.DataStorage");
            if (miniHudDataClass != null) {
                // 嘗試取得單例方法
                miniDataGetInstance = tryResolveMethod(miniHudDataClass,
                        new String[]{"getInstance", "get", "getDataStorage", "getDataStroge"});
                // 欄位名稱嘗試（實例或靜態皆可，稍後讀取會同時支援）
                fieldServerTPS = tryResolveField(miniHudDataClass, new String[]{"serverTPS", "serverTps", "SERVER_TPS"});
                fieldServerMSPT = tryResolveField(miniHudDataClass, new String[]{"serverMSPT", "serverMspt", "SERVER_MSPT"});
                fieldServerTPSValid = tryResolveField(miniHudDataClass, new String[]{"serverTPSValid", "serverTpsValid"});
            }
        }
    }

    private static Class<?> tryClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method tryMethod(Class<?> cls, String name) {
        try {
            Method m = cls.getDeclaredMethod(name);
            m.setAccessible(true);
            return m;
        } catch (Throwable ignored) {
            return null;
        }
    }

    // 解析多個候選方法名稱（需為無參數方法）。若找不到宣告方法，再嘗試 public method。
    private static Method tryResolveMethod(Class<?> cls, String[] names) {
        for (String n : names) {
            try {
                try {
                    Method m = cls.getDeclaredMethod(n);
                    if (m.getParameterCount() == 0) { m.setAccessible(true); return m; }
                } catch (NoSuchMethodException ignore) {
                    Method m = cls.getMethod(n);
                    if (m.getParameterCount() == 0) { m.setAccessible(true); return m; }
                }
            } catch (Throwable t) {
                LOGGER.debug("Error probing method {}.{}()", cls.getName(), n, t);
            }
        }
        return null;
    }

    // 解析多個候選欄位名稱
    private static Field tryResolveField(Class<?> cls, String[] names) {
        for (String n : names) {
            try {
                Field f = cls.getDeclaredField(n);
                f.setAccessible(true);
                return f;
            } catch (Throwable ignored) { }
        }
        return null;
    }

    private static Double tryReadMiniDataDouble(Field f) {
        if (f == null) return null;
        try {
            // 優先嘗試以實例讀取（MiniHUD 0.34.x 可能將數值存於單例上）
            if (miniHudDataClass != null) {
                if (miniDataInstance == null && miniDataGetInstance != null) {
                    try {
                        miniDataInstance = miniDataGetInstance.invoke(null);
                    } catch (Throwable t) {
                        LOGGER.debug("invoke DataStorage#getInstance failed", t);
                    }
                }
                if (miniDataInstance != null) {
                    boolean valid = true;
                    if (fieldServerTPSValid != null) {
                        try {
                            Object v = fieldServerTPSValid.get(miniDataInstance);
                            if (v instanceof Boolean b) valid = b;
                        } catch (Throwable t) {
                            LOGGER.debug("read serverTPSValid failed", t);
                        }
                    }
                    if (!valid) return null;
                    Object val = f.get(miniDataInstance);
                    if (val instanceof Number n) return n.doubleValue();
                }
            }
            // 回退到靜態欄位讀取（若存在）
            Object sval = f.get(null);
            if (sval instanceof Number sn) return sn.doubleValue();
            return null;
        } catch (Throwable t) {
            LOGGER.debug("tryReadMiniDataDouble failed", t);
            return null;
        }
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Answer-Fun 客戶端已初始化");

        // 註冊聊天訊息監聽器
        registerChatHandlers();

        // 載入設定檔（若不存在則建立預設）
        ModConfig.get();

        // 註冊快捷鍵：開啟設定 UI（預設 O）
        OPEN_CONFIG_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "Answer-Fun Menu",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_O,
                        "category.answerfun"
                )
        );
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_CONFIG_KEY != null && OPEN_CONFIG_KEY.wasPressed()) {
                if (client != null) {
                    client.setScreen(new ConfigScreen(client.currentScreen));
                }
            }
        });
    }

    private void registerChatHandlers() {
        // 註冊 GAME 消息處理
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            try {
                handleMessage(message);
            } catch (Throwable t) {
                LOGGER.error("處理遊戲訊息時發生錯誤", t);
            }
        });

        // 註冊 CHAT 消息處理
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            try {
                handleMessage(message);
            } catch (Throwable t) {
                LOGGER.error("處理聊天訊息時發生錯誤", t);
            }
        });
    }

    private void handleMessage(net.minecraft.text.Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        String raw = message.getString();
        if (raw == null) return;

        // 僅處理包含 '=' 的訊息，並提取從 '=' 開始的部分
        int cmdStart = raw.indexOf('=');
        if (cmdStart < 0) return;
        String cmdStr = raw.substring(cmdStart).trim();

        // 先處理簡單命令
        String simpleResponse = SimpleCommand.handleCommand(cmdStr);
        if (simpleResponse != null) {
            client.player.networkHandler.sendChatMessage(simpleResponse);
            return;
        }

        // 處理需要用戶資訊的命令
        String requester = UserCommand.extractRequesterName(message, raw);
        String response = UserCommand.handleCommand(client, raw, requester); // 傳遞原始訊息

        // 確保 UserCommand 有回傳值，即使是 "無法解析名稱"
        if (response != null) {
            if (response.contains("/msg")) {
                client.player.networkHandler.sendChatCommand(response.startsWith("/") ? response.substring(1) : response);
            } else {
                client.player.networkHandler.sendChatMessage(response);
            }
        }
    }
}
