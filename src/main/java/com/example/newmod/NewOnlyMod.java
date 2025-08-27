package com.example.newmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Minimal mod that only handles "=new" on the client side.
 * Content of the reply is left for you to implement in formatNewLine().
 */
public class NewOnlyMod implements ClientModInitializer {
    // When we see "=new" in chat, send a reply on the next tick
    private static volatile boolean pendingSendNew = false;
    // Queue for multi-line sending with delay
    private static final java.util.Deque<String> pendingLines = new java.util.ArrayDeque<>();
    private static int ticksUntilNext = 0;
    // Delay between lines (in ticks). 20 ticks ≈ 1 second.
    private static final int perLineDelayTicks = 2; // 0.1s per line

    @Override
    public void onInitializeClient() {
        // Tick handler: send reply if scheduled
        ClientTickEvents.END_CLIENT_TICK.register(client -> handleNewTick(client));

        // Listen GAME messages
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            try {
                String raw = message.getString();
                checkNewTrigger(raw);
            } catch (Throwable ignored) { }
        });

        // Listen CHAT messages
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, ts) -> {
            try {
                String raw = message.getString();
                checkNewTrigger(raw);
            } catch (Throwable ignored) { }
        });
    }

    private static void checkNewTrigger(String raw) {
        if (raw != null && raw.contains("=new")) {
            pendingSendNew = true;
        }
    }

    private static void handleNewTick(MinecraftClient client) {
        if (client == null || client.player == null) return;

        // If a new send is requested, enqueue lines
        if (pendingSendNew) {
            String payload = formatNewLine();
            pendingSendNew = false;
            if (payload != null && !payload.isEmpty()) {
                String[] parts = payload.split("\\r?\\n");
                for (String part : parts) {
                    String safe = sanitizeForChat(part);
                    if (safe != null && !safe.isEmpty()) {
                        pendingLines.addLast(safe);
                    }
                }
                // send first line immediately
                ticksUntilNext = 0;
            }
        }

        // If we have queued lines, send one per delay
        if (!pendingLines.isEmpty()) {
            if (ticksUntilNext > 0) {
                ticksUntilNext--;
                return;
            }
            String next = pendingLines.pollFirst();
            if (next != null && !next.isEmpty()) {
                try {
                    client.player.networkHandler.sendChatMessage(next);
                } catch (Exception ignored) { }
            }
            // schedule next line
            ticksUntilNext = perLineDelayTicks;
        }
    }

    public static String formatNewLine() {
        return "新玩家請注意此訊息 [/levels]可以升等 [/rewards]有每小時獎勵 [/pw end公共傳點] [/home]請多加善用 伺服器無領地傳送 [/back]回到上一個地方"; // <- fill your message here
    }

    private static String sanitizeForChat(String input) {
        if (input == null) return "";
        try {
            String noColors = input.replaceAll("\u00A7.", "");
            String noCtrl = noColors.replaceAll("[\\p{Cntrl}]", "");
            return noCtrl;
        } catch (Throwable t) {
            return input;
        }
    }
}
