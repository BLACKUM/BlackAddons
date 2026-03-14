package org.blackum.blackaddons.feature.chat;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.util.Constants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PacketLogger {
    private static final Path LOG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Constants.CONFIG_DIR_NAME).resolve(Constants.LOGS_DIR_NAME);
    private static final File LOG_FILE = LOG_DIR.resolve(Constants.BLOCKED_PACKETS_LOG_NAME).toFile();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        migrate();
        if (!LOG_DIR.toFile().exists()) {
            LOG_DIR.toFile().mkdirs();
        }
    }

    private static void migrate() {
        File oldFile = FabricLoader.getInstance().getConfigDir()
                .resolve(Constants.CONFIG_DIR_NAME)
                .resolve(Constants.BLOCKED_PACKETS_LOG_NAME).toFile();

        if (oldFile.exists() && !LOG_FILE.exists()) {
            try {
                if (!LOG_DIR.toFile().exists()) {
                    LOG_DIR.toFile().mkdirs();
                }
                if (oldFile.renameTo(LOG_FILE)) {
                    Blackaddons.LOGGER.info("Successfully migrated blocked_packets.log to logs folder");
                }
            } catch (Exception e) {
                Blackaddons.LOGGER.error("Failed to migrate blocked_packets.log", e);
            }
        }
    }

    public static void logBlockedPacket(String source, CustomPacketPayload payload) {
        logPacket(source, "Blocked Packet", payload);
    }

    public static void logPacket(String source, String action, CustomPacketPayload payload) {
        new Thread(() -> {
            try {
                trimLogIfNeeded();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                    String timestamp = LocalDateTime.now().format(DATE_FORMAT);
                    String id = payload.type().id().toString();
                    String data = payload.toString();

                    StringBuilder logEntry = new StringBuilder();
                    logEntry.append("[").append(timestamp).append("] ");
                    logEntry.append("[").append(source).append("] ");
                    logEntry.append(action).append(": ").append(id).append("\n");
                    logEntry.append("    Payload Content: ").append(data).append("\n");
                    logEntry.append("--------------------------------------------------\n");

                    writer.write(logEntry.toString());
                }
            } catch (IOException e) {
                Blackaddons.LOGGER.error("Failed to log packet", e);
            }
        }).start();
    }

    private static void trimLogIfNeeded() throws IOException {
        if (!LOG_FILE.exists() || LOG_FILE.length() <= Constants.BLOCKED_PACKETS_LOG_MAX_BYTES) {
            return;
        }
        List<String> lines = Files.readAllLines(LOG_FILE.toPath());
        int trimFrom = lines.size() / 2;
        while (trimFrom < lines.size() && !lines.get(trimFrom).startsWith("[")) {
            trimFrom++;
        }
        List<String> kept = lines.subList(trimFrom, lines.size());
        Files.writeString(LOG_FILE.toPath(), String.join("\n", kept) + "\n");
        Blackaddons.LOGGER.info("Trimmed blocked_packets.log to {} lines", kept.size());
    }
}
