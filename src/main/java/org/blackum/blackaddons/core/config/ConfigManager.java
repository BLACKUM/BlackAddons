package org.blackum.blackaddons.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.feature.cheat.AutoTNT;
import org.blackum.blackaddons.gui.render.Theme;

import org.blackum.blackaddons.feature.modhider.SpoofMode;
import org.blackum.blackaddons.core.util.Constants;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigManager {
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Constants.CONFIG_DIR_NAME);
    private static final File CONFIG_FILE = CONFIG_DIR.resolve("config.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Set<String> FABRIC_DEFAULT_CHANNELS = Set.of(
            "fabric:attachment_sync_v1",
            "fabric:recipe_sync",
            "fabric-screen-handler-api-v1:open_screen");

    public static class CardState {
        public int x;
        public int y;
        public int width;
        public int height;
        public boolean collapsed;
        public int initialWidth;
        public int expandedHeight;

        public CardState() {
        }

        public CardState(int x, int y, int width, int height, boolean collapsed, int initialWidth, int expandedHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.collapsed = collapsed;
            this.initialWidth = initialWidth;
            this.expandedHeight = expandedHeight;
        }
    }

    public static class SoundAlert {
        public String pattern;
        public boolean isRegex;
        public String soundId;
        public float volume = 1.0f;
        public float pitch = 1.0f;
        public boolean enabled;
        public String title = "";
        public String subtitle = "";
        public int durationSeconds = 2;
        public boolean collapsed = true;

        public SoundAlert() {
        }

        public SoundAlert(String pattern, boolean isRegex, String soundId, float volume, float pitch, boolean enabled,
                String title,
                String subtitle, int durationSeconds) {
            this.pattern = pattern;
            this.isRegex = isRegex;
            this.soundId = soundId;
            this.volume = volume;
            this.pitch = pitch;
            this.enabled = enabled;
            this.title = title;
            this.subtitle = subtitle;
            this.durationSeconds = durationSeconds;
            this.collapsed = true;
        }
    }

    public static enum DataSource {
        BOT, LOCAL
    }

    public enum ApiPriority {
        ADJECTILS, SOOPY, SKYCRYPT
    }

    public static class ConfigData {
        public int overlayX = 5;
        public int overlayY = 5;
        public float overlayScale = 1.0f;
        public boolean showHitboxes = false;
        public boolean showDebugOverlay = false;
        public int accentColor = Theme.ACCENT;
        public boolean useCardLayout = true;
        public Map<String, CardState> cardStates = new HashMap<>();

        // Bot
        public String botUrl = Constants.DEFAULT_BOT_URL;
        public DataSource dataSource = DataSource.LOCAL;
        public List<ApiPriority> apiPriorityList = new ArrayList<>(
                List.of(ApiPriority.ADJECTILS, ApiPriority.SOOPY, ApiPriority.SKYCRYPT));
        public boolean rngTrackerEnabled = true;
        public String developerKey = "";
        public boolean partyFinderAutoInvite = true;
        public boolean partyFinderAutoAcceptInvite = true;
        public boolean partyFinderShowStatsOnJoin = true;
        public boolean partyFinderShowStatsOnRequest = true;
        public boolean ircEnabled = true;
        public List<String> ircChannels = new ArrayList<>(List.of("general", "announcements", "admin"));

        // Mod Hider (ported from ClientSpoofer)
        public SpoofMode modHiderSpoofMode = SpoofMode.CUSTOM;
        public String modHiderCustomClient = "fabric";
        public boolean modHiderHideMods = true;
        public boolean modHiderDisableCustomPayloads = true;
        public Set<String> modHiderAllowedMods = new HashSet<>();
        public Set<String> modHiderAllowedCustomPayloadChannels = new HashSet<>(FABRIC_DEFAULT_CHANNELS);

        // Cheats
        public boolean AutoTNTEnabled = false;
        public int AutoTNTDelay = 5;
        public int UnequipDelay = 8;
        public boolean SwapBack = true;

        public AutoTNT.FeatureConfig autoTntConfig = new AutoTNT.FeatureConfig();

        public boolean FastLeapEnabled = false;
        public boolean FastLeapDoorOpener = false;
        public boolean BloodBlinkEnabled = false;

        public boolean hideMods() {
            return switch (modHiderSpoofMode) {
                case VANILLA, MODDED -> true;
                case CUSTOM -> modHiderHideMods;
                case OFF -> false;
            };
        }

        // Legit
        public boolean legitFullbrightEnabled = false;

        // Settings
        public int notificationDuration = 4000;
        public int cacheDurationMinutes = 5;
        public boolean disableCommandConfirmation = true;

        // Chat triggers defaults
        public List<SoundAlert> chatSoundAlerts = new ArrayList<>(List.of(
                new SoundAlert(
                        "(?s).*?(?:\\[.*?\\] )?([A-Za-z0-9_]+) has invited you to join their party!.*You have 60 seconds to accept.*",
                        true,
                        "entity.cat.ambient", 1.0f, 1.0f, true, "&cParty Invite!", "&6From: &e{1}", 2),
                new SoundAlert("Party Finder > ([A-Za-z0-9_]+) joined the dungeon group! \\((.*)\\)", true,
                        "entity.experience_orb.pickup",
                        1.0f, 1.0f, true, "&a{1} Joined!", "&7Class: &b{2}", 3)));

        // Command aliases
        public Map<String, String> knownAliases = new HashMap<>();

        // UI
        public Map<String, CardState> lastLoadedCardStates = new HashMap<>();
    }

    public static ConfigData data = new ConfigData();

    public static void save() {
        try {
            if (!CONFIG_DIR.toFile().exists())
                CONFIG_DIR.toFile().mkdirs();

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigData loadedData = GSON.fromJson(reader, ConfigData.class);
            if (loadedData != null) {
                data = loadedData;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
