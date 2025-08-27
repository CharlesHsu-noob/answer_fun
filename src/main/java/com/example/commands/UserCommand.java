package com.example.commands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.text.ClickEvent;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.config.ModConfig;

public class UserCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("answer-fun");

    // 掃描訊息與其 siblings，找出 SUGGEST_COMMAND 形式的 /msg <name> ... 並回傳 <name>
    private static String extractNameFromSuggestCommand(Text message) {
        if (message == null) return null;

        // 建立要檢查的 Text 列表：包含自身與 siblings
        List<Text> segments = message.getSiblings();
        // 有些情況 root 本身也帶有 Style，優先檢查 root
        // 這裡透過臨時串列把 root 放在最前面
        segments = new java.util.ArrayList<>(segments == null ? List.of() : segments);
        segments.add(0, message);

        try {
            String preview = "";
            try { preview = message.getString(); } catch (Throwable ignore) {}
            LOGGER.info("extractNameFromSuggestCommand: root='{}' segments={} ", preview, segments.size());
        } catch (Throwable t) {
            // ignore logging failures
        }

        Pattern p = Pattern.compile("\\/msg\\s+([^\\s]+)\\s");
        for (Text t : segments) {
            if (t == null) continue;
            Style style = t.getStyle();
            if (style == null) continue;
            ClickEvent ce = style.getClickEvent();
            if (ce == null) continue;
            if (ce.getAction() == ClickEvent.Action.SUGGEST_COMMAND) {
                String value = ce.getValue();
                if (value == null) continue;
                LOGGER.info("extractNameFromSuggestCommand: found SUGGEST_COMMAND value='{}'", value);
                Matcher m = p.matcher(value);
                if (m.find()) {
                    String name = m.group(1);
                    if (name != null && !name.isEmpty()) {
                        LOGGER.info("extractNameFromSuggestCommand: extracted name='{}'", name);
                        return name;
                    }
                }
            }
        }
        LOGGER.info("extractNameFromSuggestCommand: no name found");
        return null;
    }

    private static String extractCommandStart(String raw) {
        // 從第一個 '=' 開始擷取，確保能辨識類似 "[TAG] 使用者 » =pname" 的指令
        if (raw == null) return "";
        int i = raw.indexOf('=');
        return (i >= 0) ? raw.substring(i).trim() : raw.trim();
    }

    public static String handleCommand(MinecraftClient client, String raw, String requester) {
        if (raw == null) return null;
        String cmd = extractCommandStart(raw);
        boolean isDiscord = isDiscordMessage(raw);
        LOGGER.info("handleCommand: raw='{}' cmd='{}' requester='{}' isDiscord={} ", raw, cmd, requester, isDiscord);
        ModConfig cfg = ModConfig.get();
        
        if (cmd.startsWith("=ping")) {
            LOGGER.info("handleCommand: dispatch -> =ping (old signature)");
            if (!cfg.enablePing) return null;
            return handlePingCommand(client, requester, null);
        } else if (cmd.startsWith("=pick")) {
            LOGGER.info("handleCommand: dispatch -> =pick");
            if (!cfg.enablePick) return null;
            return handlePickCommand(client, raw);
        } else if (cmd.startsWith("=yan")) {
            LOGGER.info("handleCommand: dispatch -> =yan");
            if (!cfg.enableYan) return null;
            return handlePrivateMessage(client, "=yan", raw, isDiscord);
        } else if (cmd.startsWith("=masaki")) {
            LOGGER.info("handleCommand: dispatch -> =masaki");
            if (!cfg.enableMasaki) return null;
            return handlePrivateMessage(client, "=masaki", raw, isDiscord);
        } else if (cmd.startsWith("=pname")) {
            LOGGER.info("handleCommand: dispatch -> =pname");
            return getnickname(raw);
        }
        
        return null;
    }

    // 新增：處理 pname 命令（改名：getnickname）
    private static String getnickname(String raw) {
        if (raw == null || raw.isEmpty()) return "無法解析名稱";

        // 記錄完整的原始訊息（符合需求樣式）
        LOGGER.info("收到完整訊息: {}", raw);

        // 新增：優先處理私訊格式 [發送者 -> 接收者]
        Pattern pmPattern = Pattern.compile("\\[\\S+\\s*->\\s*(\\S+)\\]");
        Matcher pmMatcher = pmPattern.matcher(raw);
        if (pmMatcher.find()) {
            String recipient = pmMatcher.group(1);
            if (recipient != null && !recipient.isEmpty()) {
                LOGGER.info("getnickname: parsed recipient '{}' from private message format", recipient);
                return recipient;
            }
        }

        // 以工作字串進行解析，不影響日誌
        String work = raw;

        // 0) 先移除遊戲內特殊字符（例如私用區字元等）
        work = removeGameSpecialChars(work);

        // 1) 移除各種標記 [..] {..} (..)
        work = work
            .replaceAll("\\[[^]]+\\]", " ")
            .replaceAll("\\{[^}]+\\}", " ")
            .replaceAll("\\([^)]+\\)", " ")
            .replaceAll("\\s+", " ")
            .trim();

        // 2) 移除命令本身（及其後方所有內容）
        int cmdIndex = work.indexOf("=pname");
        if (cmdIndex >= 0) {
            work = work.substring(0, cmdIndex).trim();
        }

        // 3) 截斷在第一個訊息分隔符（例如 '»' 或 ':'）之前
        int idxArrowMsg = work.indexOf('»');
        int idxColon = work.indexOf(':');
        int cut = -1;
        if (idxArrowMsg >= 0 && idxColon >= 0) {
            cut = Math.min(idxArrowMsg, idxColon);
        } else if (idxArrowMsg >= 0) {
            cut = idxArrowMsg;
        } else if (idxColon >= 0) {
            cut = idxColon;
        }
        if (cut >= 0) {
            work = work.substring(0, cut).trim();
        }

        // 4) 擷取玩家名稱：
        // 常見箭頭/指向符號包含：> ➠ → - 等
        String[] parts = work.split("[>➠➔➙➛➜➝➞➟→\\-]+");
        String candidate = parts.length > 0 ? parts[parts.length - 1].trim() : "";
        // 若箭頭後為空（例如 "meow ➠"），則取箭頭前的片段
        if (candidate.isEmpty() && parts.length >= 2) {
            candidate = parts[parts.length - 2].trim();
        }
        // 若箭頭後是指令（例如 "=yan"、"=pick"），則認定箭頭前為名稱
        if (!candidate.isEmpty() && candidate.startsWith("=") && parts.length >= 2) {
            candidate = parts[parts.length - 2].trim();
        }
        // 回退：若仍是以 '=' 開頭或解析失敗，直接從 '=' 左側推導名稱
        if (candidate.isEmpty() || candidate.startsWith("=")) {
            int eq = work.indexOf('=');
            if (eq > 0) {
                String left = work.substring(0, eq).trim();
                // 移除尾端符號/箭頭/空白
                left = left
                    .replaceAll("[\\p{Punct}\\p{Symbol}]+$", "")
                    .replaceAll("[>»➠\u27A0\u279C\\-\\s]+$", "")
                    .trim();
                // 取最後一個空白之後的片段作為名稱（ID 不包含空白）
                int sp = left.lastIndexOf(' ');
                candidate = (sp >= 0) ? left.substring(sp + 1).trim() : left;
            }
        }

        // 5) 清除前後殘留分隔符
        candidate = candidate
            // 同時移除左右尖括號與其它分隔符號
            .replaceAll("^[#\\s:<>»➠\u27A0\u279C]+", "")
            .replaceAll("[#\\s:<>»➠\u27A0\u279C]+$", "")
            .trim();

        // 最後再做一次特殊字符清理，確保名稱純淨
        candidate = removeGameSpecialChars(candidate);

        if (candidate.isEmpty()) {
            LOGGER.info("getnickname: parse failed -> '無法解析名稱' from raw='{}'", raw);
            return "無法解析名稱";
        } else {
            LOGGER.info("getnickname: parsed candidate='{}' from raw='{}'", candidate, raw);
            return candidate;
        }
    }

    // 刪除遊戲內常見的特殊字符：
    // - Unicode 私用區：U+E000–U+F8FF（BMP）、U+F0000–U+FFFFD、U+100000–U+10FFFD
    // - 控制字元
    private static String removeGameSpecialChars(String s) {
        if (s == null || s.isEmpty()) return s;
        // 去除私用區字元
        String cleaned = s
            .replaceAll("[\uE000-\uF8FF]", "")
            .replaceAll("[\uDB80-\uDBFF][\uDC00-\uDFFF]", ""); // 粗略移除部分增補私用區代理對

        // 去除其餘不可見控制字元（保留常見換行/空白外的控制符）
        cleaned = cleaned.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");

        // 可選：移除 U+FFFD 替代字元（黑鑽問號）
        cleaned = cleaned.replace("\uFFFD", "");

        return cleaned;
    }

    public static String extractRequesterName(Text message, String raw) {
        String name = extractNameFromSuggestCommand(message);
        if (name != null && !name.isEmpty()) return name;
        
        return parseRequesterFromRaw(raw);
    }

    // Simple implementation: tries to extract requester name from raw string
    private static String parseRequesterFromRaw(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        // Example: if raw is "username: message" or "[Discord | username]: message"
        String[] parts = raw.split(":");
        if (parts.length > 1) {
            String possibleName = parts[0].trim();
            // Remove Discord prefix if present
            if (possibleName.contains("|")) {
                String[] discordParts = possibleName.split("\\|");
                if (discordParts.length > 1) {
                    return discordParts[1].trim();
                }
            }
            return possibleName;
        }
        return null;
    }

    public static boolean isDiscordMessage(String raw) {
        if (raw == null) return false;
        return raw.contains("[Discord") || 
               raw.contains("【Discord") || 
               raw.matches(".*\\[\\s*Discord\\s*\\|.*");
    }

    public static String handlePickCommand(MinecraftClient client, String raw) {
        try {
            if (client == null || client.getNetworkHandler() == null) {
                return "目前沒有可挑選的玩家";
            }

            List<String> names = client.getNetworkHandler().getPlayerList().stream()
                .filter(Objects::nonNull)
                .map(e -> e.getProfile())
                .filter(Objects::nonNull)
                .map(profile -> profile.getName())
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .collect(Collectors.toList());

            if (names.isEmpty()) {
                return "你抽到了空氣~";
            }

            String picked = names.get((int)(Math.random() * names.size()));

            // 一律使用 getnickname 解析名稱（基於原始訊息 raw）
            String displayName = getnickname(raw);
            if (displayName == null || displayName.isEmpty() || "無法解析名稱".equals(displayName)) {
                displayName = (client.player != null && client.player.getName() != null)
                    ? client.player.getName().getString()
                    : "你";
            }

            LOGGER.debug("Pick command - requester: {}, picked: {}", displayName, picked);
            return String.format("%s抽到了%s~", displayName, picked);
        } catch (Throwable t) {
            LOGGER.debug("handlePickCommand failed", t);
            return "挑選玩家時發生錯誤";
        }
    }

    // 新增 overload：依照「點擊事件 → 正則抽取 → 中斷」流程解析名稱後回傳 ping
    public static String handlePingCommand(MinecraftClient client, String username, UUID uuid) {
        try {
            if (client == null || client.getNetworkHandler() == null) {
                return "無法取得連線資訊";
            }
            if (uuid != null) {
                LOGGER.info("extracted user id (uuid): {}", uuid);
            }
            if (username != null && !username.isEmpty()) {
                LOGGER.info("extracted user id (name): {}", username);
            }

            int ping = -1;
            if (uuid != null) {
                ping = findPingByUuid(client, uuid);
            }
            if (ping < 0 && username != null && !username.isEmpty()) {
                ping = findPingByPlayerName(client, username);
            }

            if (ping >= 0) {
                LOGGER.info("handlePingCommand(name,uuid): resolved ping={} for name='{}' uuid='{}'", ping, username, uuid);
                return ("你的ping:" + ping);
            } else {
                // 列出目前在線玩家名稱，協助排查大小寫/身分不一致
                try {
                    List<String> online = client.getNetworkHandler().getPlayerList().stream()
                        .filter(e -> e != null && e.getProfile() != null && e.getProfile().getName() != null)
                        .map(e -> e.getProfile().getName())
                        .sorted()
                        .collect(Collectors.toList());
                    LOGGER.info("handlePingCommand(name,uuid): not found. username='{}' uuid='{}' online={}", username, uuid, online);
                } catch (Throwable ignore) {}
                return "你的ping:未找到";
            }
        } catch (Throwable t) {
            LOGGER.debug("handlePingCommand failed", t);
            return "無法取得Ping值";
        }
    }

    public static String handlePingCommand(MinecraftClient client, Text message, String raw) {
        try {
            if (client == null || client.getNetworkHandler() == null) {
                return "無法取得連線資訊";
            }

            // 1~4：從訊息的 SUGGEST_COMMAND 解析 /msg <name> ...
            String realName = extractNameFromSuggestCommand(message);
            if ((realName == null || realName.isEmpty()) && raw != null) {
                // 退而求其次：用先前寫好的原始字串解析
                realName = getnickname(raw);
                if (realName != null && "無法解析名稱".equals(realName)) realName = null;
            }
            if (realName != null && !realName.isEmpty()) {
                LOGGER.info("extracted user id: {}", realName);
            }

            // 5：一旦取得名稱，直接查找該玩家的 ping
            int ping = -1;
            if (realName != null && !realName.isEmpty()) {
                ping = findPingByPlayerName(client, realName);
            }

            if (ping >= 0) {
                LOGGER.info("handlePingCommand(Text,...): name='{}' ping={}", realName, ping);
                return ("你的ping:" + ping);
            } else {
                LOGGER.info("handlePingCommand(Text,...): ping not found for name='{}' raw='{}'", realName, raw);
                return "你的ping:未找到";
            }
        } catch (Throwable t) {
            LOGGER.debug("handlePingCommand failed", t);
            return "無法取得Ping值";
        }
    }

    public static String handlePrivateMessage(MinecraftClient client, String command, String raw, boolean isDiscord) {
        if (isDiscord) return "DC端不可使用此指令";
        // 跟 pick 一樣：先用原始訊息 raw 經 getnickname 解析發話者
        String target = getnickname(raw);
        LOGGER.info("target: {}", target);
        /*if (target == null || target.isEmpty() || "無法解析名稱".equals(target)) {
            // 嘗試簡易回退
            target = extractSimpleRequester(raw);
        }*/
        if (target == null || target.isEmpty() || "無法解析名稱".equals(target)) {
            return "無法解析Requester名稱"; // 無法取得對方名稱則不回覆
        }

        String message = switch(command) {
            case "=yan" -> "嘴太臭被ban";
            case "=masaki" -> "小男娘服主";
            default -> null;
        };

        return message != null ? "/msg " + target + " " + message : null;
    }

    private static int findPingByPlayerName(MinecraftClient client, String name) {
        try {
            LOGGER.info("findPingByPlayerName: searching for '{}'", name);
            return client.getNetworkHandler().getPlayerList().stream()
                .filter(e -> e != null && e.getProfile() != null)
                .filter(e -> name.equalsIgnoreCase(e.getProfile().getName()))
                .findFirst()
                .map(e -> e.getLatency())
                .orElse(-1);
        } catch (Throwable t) {
            LOGGER.debug("findPingByPlayerName failed", t);
            return -1;
        }
    }

    private static int findPingByUuid(MinecraftClient client, UUID uuid) {
        try {
            return client.getNetworkHandler().getPlayerList().stream()
                .filter(e -> e != null && e.getProfile() != null)
                .filter(e -> uuid.equals(e.getProfile().getId()))
                .findFirst()
                .map(e -> e.getLatency())
                .orElse(-1);
        } catch (Throwable t) {
            LOGGER.debug("findPingByUuid failed", t);
            return -1;
        }
    }
}