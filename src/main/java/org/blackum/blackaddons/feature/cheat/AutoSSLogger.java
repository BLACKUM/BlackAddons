package org.blackum.blackaddons.feature.cheat;

import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.util.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AutoSSLogger {
    private static final Path LOG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Constants.CONFIG_DIR_NAME).resolve("logs");
    private static final File LOG_FILE = LOG_DIR.resolve("autoss_debug.log").toFile();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void log(String message) {
        if (!ConfigManager.data.AutoSSDebug) return;

        try {
            if (!LOG_DIR.toFile().exists()) {
                LOG_DIR.toFile().mkdirs();
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
                String timestamp = LocalDateTime.now().format(formatter);
                writer.println("[" + timestamp + "] " + message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void clearLog() {
        if (LOG_FILE.exists()) {
            LOG_FILE.delete();
        }
    }
}
