package com.example.commands;

import com.example.Answerfun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import com.example.config.ModConfig;

public class SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("answer-fun");
    private static final long JIMMY_COOLDOWN_MS = 5000L;
    private static long lastJimmyTs = 0L;

    static {
        // 在類別載入時輸出目前的日誌級別設定
        LOGGER.error("ERROR level enabled: " + LOGGER.isErrorEnabled());
        LOGGER.warn("WARN level enabled: " + LOGGER.isWarnEnabled());
        LOGGER.info("INFO level enabled: " + LOGGER.isInfoEnabled());
        LOGGER.debug("DEBUG level enabled: " + LOGGER.isDebugEnabled());
        LOGGER.trace("TRACE level enabled: " + LOGGER.isTraceEnabled());
    }

    // 處理簡單命令的回覆
    public static String handleCommand(String command) {
        if (command == null) return null;
        ModConfig cfg = ModConfig.get();

        return switch (command) {
            case "=help" -> (cfg.enableHelp ? formatHelpLine() : null);
            case "=roll" -> (cfg.enableRoll ? formatRollLine() : null);
            case "=time" -> (cfg.enableTime ? formatTimeLine() : null);
            case "=king" -> (cfg.enableKing ? "雷公助我!!" : null);
            case "=jimmy" -> (cfg.enableJimmy ? formatJimmyLine() : null);
            case "=tps" -> (cfg.enableTps ? formatTpsLine() : null);
            default -> null;
        };
    }

    private static String formatHelpLine() {
        ModConfig cfg = ModConfig.get();
        java.util.List<String> cmds = new java.util.ArrayList<>();
        if (cfg.enableHelp) cmds.add("=help");
        if (cfg.enableTps) cmds.add("=tps");
        if (cfg.enableTime) cmds.add("=time");
        if (cfg.enableKing) cmds.add("=king");
        if (cfg.enableJimmy) cmds.add("=jimmy");
        if (cfg.enableRoll) cmds.add("=roll");
        if (cfg.enablePing) cmds.add("=ping");
        if (cfg.enablePick) cmds.add("=pick");
        if (cfg.enableMasaki) cmds.add("=masaki");
        if (cfg.enableYan) cmds.add("=yan");
        String list = String.join(", ", cmds);
        return "可用指令: " + list + ".";
    }

    private static String formatRollLine() {
        try {
            long v = Math.round(Math.random() * 1000.0);
            return "你抽到了" + v;
        } catch (Throwable t) {
            LOGGER.debug("formatRollLine failed", t);
            return "你抽到了" + Math.round((Math.random() * 1000.0));
        }
    }

    private static String formatTimeLine() {
        try {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
            return now.format(fmt);
        } catch (Throwable t) {
            LOGGER.debug("formatTimeLine failed", t);
            return new java.util.Date().toString();
        }
    }

    private static String formatTpsLine() {
        try {
            double tps = Answerfun.getEstimatedServerTps();
            double mspt = Answerfun.getEstimatedMspt();
            String base = String.format("伺服器 TPS: %.4f, MSPT: %.4f", tps, mspt);
            return base;
        } catch (Throwable t) {
            LOGGER.debug("formatTpsLine failed", t);
            return "無法取得 TPS/MSPT 資訊.";
        }
    }

    private static String formatJimmyLine() {
        try {
            long now = System.currentTimeMillis();
            long remain = JIMMY_COOLDOWN_MS - (now - lastJimmyTs);
            if (remain > 0) {
                return "=jimmy 冷卻中，還需 " + String.format("%.1f 秒", remain / 1000f);
            }
            lastJimmyTs = now;
            return "歡迎加入吉米小鎮!!";
        } catch (Throwable t) {
            LOGGER.debug("formatJimmyLine failed", t);
            return "歡迎加入吉米小鎮!!";
        }
    }


}
