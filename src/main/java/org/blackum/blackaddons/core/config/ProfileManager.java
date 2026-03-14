package org.blackum.blackaddons.core.config;

import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.util.Constants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ProfileManager {
    public enum Category {
        CONFIG("config"),
        CHAT_ACTIONS("chat_actions"),
        WAYPOINTS("waypoints");

        private final String dirName;

        Category(String dirName) {
            this.dirName = dirName;
        }

        public String getDirName() {
            return dirName;
        }
    }

    private static final String ACTIVE_FILE = "active.txt";
    private static final String DEFAULT_PROFILE = "default";

    public static Path getCategoryDir(Category category) {
        Path path = FabricLoader.getInstance().getConfigDir()
                .resolve(Constants.CONFIG_DIR_NAME)
                .resolve(Constants.PROFILES_DIR_NAME)
                .resolve(category.getDirName());
        
        File dir = path.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }

    public static String getActiveProfile(Category category) {
        Path activeFilePath = getCategoryDir(category).resolve(ACTIVE_FILE);
        if (!Files.exists(activeFilePath)) {
            setActiveProfile(category, DEFAULT_PROFILE);
            return DEFAULT_PROFILE;
        }

        try {
            String active = Files.readString(activeFilePath).trim();
            return active.isEmpty() ? DEFAULT_PROFILE : active;
        } catch (IOException e) {
            Blackaddons.LOGGER.error("Failed to read active profile for " + category, e);
            return DEFAULT_PROFILE;
        }
    }

    public static void setActiveProfile(Category category, String profileName) {
        Path activeFilePath = getCategoryDir(category).resolve(ACTIVE_FILE);
        try {
            Files.writeString(activeFilePath, profileName);
        } catch (IOException e) {
            Blackaddons.LOGGER.error("Failed to set active profile for " + category, e);
        }
    }

    public static File getProfileFile(Category category, String profileName) {
        String fileName = profileName + ".json";
        return getCategoryDir(category).resolve(fileName).toFile();
    }

    public static File getActiveProfileFile(Category category) {
        return getProfileFile(category, getActiveProfile(category));
    }

    public static List<String> listProfiles(Category category) {
        List<String> profiles = new ArrayList<>();
        Path dir = getCategoryDir(category);
        
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(path -> path.toString().endsWith(".json"))
                  .forEach(path -> {
                      String name = path.getFileName().toString();
                      profiles.add(name.substring(0, name.length() - 5));
                  });
        } catch (IOException e) {
            Blackaddons.LOGGER.error("Failed to list profiles for " + category, e);
        }
        
        if (profiles.isEmpty()) {
            profiles.add(DEFAULT_PROFILE);
        }
        return profiles;
    }

    public static void deleteProfile(Category category, String profileName) {
        if (profileName.equals(DEFAULT_PROFILE)) return;
        
        File file = getProfileFile(category, profileName);
        if (file.exists()) {
            file.delete();
        }
        
        if (getActiveProfile(category).equals(profileName)) {
            setActiveProfile(category, DEFAULT_PROFILE);
        }
    }

    public static void duplicateProfile(Category category, String profileName, String newName) {
        File source = getProfileFile(category, profileName);
        File target = getProfileFile(category, newName);
        if (source.exists() && !target.exists()) {
            try {
                Files.copy(source.toPath(), target.toPath());
            } catch (IOException e) {
                Blackaddons.LOGGER.error("Failed to duplicate profile " + profileName + " to " + newName, e);
            }
        }
    }
}
