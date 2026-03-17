package org.blackum.blackaddons.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.feature.cheat.AutoBM;
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
    private static final Path OLD_CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Constants.CONFIG_DIR_NAME);
    private static final File OLD_CONFIG_FILE = OLD_CONFIG_DIR.resolve("config.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static File getConfigFile() {
        return ProfileManager.getActiveProfileFile(ProfileManager.Category.CONFIG);
    }

    public static void resetToDefaults() {
        data = new ConfigData();
        Theme.ACCENT = data.accentColor;
        Theme.refreshColors();
    }

    public static final Set<String> FABRIC_DEFAULT_CHANNELS = Set.of(
            "fabric:attachment_sync_v1",
            "fabric:recipe_sync",
            "fabric-screen-handler-api-v1:open_screen",
            "hypixel:ping",
            "hypixel:party_info",
            "hypixel:player_info",
            "hypixel:hello",
            "hypixel:register",
            "hyevent:location");

    public static final Set<String> DEFAULT_ALLOWED_MODS = Set.of(
            "minecraft",
            "fabricloader",
            "java",
            "fabric");

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

    public enum ActionStepType {
        SWITCH_SLOT("Switch Slot"),
        USE_ITEM("Use Item"),
        ATTACK("Attack"),
        SEND_MESSAGE("Send Message"),
        PRESS_KEYBIND("Press Keybind"),
        ROTATE("Rotate Camera");

        private final String displayName;

        ActionStepType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static ActionStepType fromDisplayName(String displayName) {
            for (ActionStepType type : values()) {
                if (type.displayName.equalsIgnoreCase(displayName)) {
                    return type;
                }
            }
            return SWITCH_SLOT;
        }
    }

    public static class ActionStep {
        public ActionStepType type;
        public int slotIndex;
        public String message = "";
        public int delayTicks;
        public int durationTicks;
        public float yaw;
        public float pitch;
        public boolean useCoordinates = false;
        public double targetX;
        public double targetY;
        public double targetZ;
        public boolean collapsed = false;

        public ActionStep() {
        }

        public ActionStep(ActionStepType type, int slotIndex, String message, int delayTicks, int durationTicks) {
            this.type = type;
            this.slotIndex = slotIndex;
            this.message = message;
            this.delayTicks = delayTicks;
            this.durationTicks = durationTicks;
        }

        public ActionStep(ActionStepType type, float yaw, float pitch, int delayTicks) {
            this.type = type;
            this.yaw = yaw;
            this.pitch = pitch;
            this.delayTicks = delayTicks;
        }
    }

    public static class ChatAction {
        public String pattern;
        public boolean isRegex;
        public String soundId;
        public float volume = 1.0f;
        public float pitch = 1.0f;
        public boolean enabled;
        public String title = "";
        public String subtitle = "";
        public float durationSeconds = 2.0f;
        public boolean collapsed = true;
        public List<ActionStep> actions = new ArrayList<>();

        public ChatAction() {
        }

        public ChatAction(String pattern, boolean isRegex, String soundId, float volume, float pitch, boolean enabled,
                String title,
                String subtitle, float durationSeconds) {
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

    public static class WaypointAction {
        public boolean enabled = true;
        public boolean triggerOnEntry = true;
        public boolean triggerOnExit = false;
        public String soundId = "";
        public float volume = 1.0f;
        public float pitch = 1.0f;
        public String title = "";
        public String subtitle = "";
        public float durationSeconds = 2.0f;
        public boolean collapsed = true;
        public List<ActionStep> actions = new ArrayList<>();

        public WaypointAction() {
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
        public int accentColor = Theme.DEFAULT_ACCENT;
        public boolean useCardLayout = true;
        public int forcedGuiScale = 2;
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
        public String ircPrefix = "AUTO";

        // Mod Hider (ported from ClientSpoofer)
        public SpoofMode modHiderSpoofMode = SpoofMode.CUSTOM;
        public String modHiderCustomClient = "fabric";
        public boolean modHiderHideMods = true;
        public boolean modHiderDisableCustomPayloads = true;
        public Set<String> modHiderAllowedMods = new HashSet<>(DEFAULT_ALLOWED_MODS);
        public Set<String> modHiderAllowedCustomPayloadChannels = new HashSet<>(FABRIC_DEFAULT_CHANNELS);

        // Cheats
        public boolean AutoTNTEnabled = false;
        public int AutoTNTDelay = 5;
        public int UnequipDelay = 8;
        public boolean SwapBack = true;

        public AutoTNT.FeatureConfig autoTntConfig = new AutoTNT.FeatureConfig();
        public AutoBM.FeatureConfig autoBMConfig = new AutoBM.FeatureConfig();

        public boolean AutoSSEnabled = false;
        public int AutoSSDelay = 2;
        public float AutoSSDistanceLimit = 4.5f;
        public boolean AutoSSAlerts = true;
        
        public float AutoSSRotationSpeed = 12.0f;
        public float AutoSSRotationCurve = 0.08f;
        public boolean AutoSSInstantSnap = false;
        public boolean AutoSSSkip = false;
        public boolean AutoSSAutoStart = false;
        public int AutoSSAutoStartDelay = 3;
        public boolean AutoSSTrySkip = false;
        public boolean AutoSSDebug = false;
        public boolean AutoSSSwapToItem = false;
        public int AutoSSSwapMode = 0; // 0: Swap, 1: Swap and Open 2: TODO: add Swap and Open and Leap to ... class
        public int AutoSSOverlayX = -1;
        public int AutoSSOverlayY = 5;

        public boolean FastLeapEnabled = false;
        public boolean FastLeapDoorOpener = false;
        public boolean FastLeapPositional = false;
        public boolean FastLeapDebug = false;
        public String FastLeapS1Class = "NONE";
        public String FastLeapS2Class = "NONE";
        public String FastLeapS3Class = "NONE";
        public String FastLeapS4Class = "NONE";

        public boolean showRotationDebug = false;
        public int rotationOverlayX = -1;
        public int rotationOverlayY = 5;

        public boolean rotationHumanizerEnabled = true;
        public float rotationVariance = 0.08f;
        public float rotationTargetRandomness = 0.2f;
        public float rotationSmoothness = 0.5f;
        public float rotationSpeed = 12.0f;
        public float rotationDistanceSlowdown = 2.0f;
        public float rotationDistanceRadius = 50.0f;
        public float rotationFovSlowdown = 30.0f;
        public float rotationStopThreshold = 1.00f;

        public boolean hideMods() {
            return switch (modHiderSpoofMode) {
                case VANILLA, MODDED -> true;
                case CUSTOM -> modHiderHideMods;
                case OFF -> false;
            };
        }

        // Legit
        public boolean legitFullbrightEnabled = false;
        public boolean removeFireOverlay = false;
        public boolean hideStatusEffects = false;
        public boolean disableNearbyParticles = false;

        // Settings
        public int notificationDuration = 4000;
        public int cacheDurationMinutes = 5;
        public boolean disableCommandConfirmation = true;
        public boolean disableUnsecureChatToast = true;

        // Chat actions (migration) TODO: Delete migration when enough versions have passed
        public List<ChatAction> chatActions = new ArrayList<>(List.of(
                new ChatAction(
                        "(?s).*?(?:\\[.*?\\] )?([A-Za-z0-9_]+) has invited you to join their party!.*You have 60 seconds to accept.*",
                        true,
                        "entity.cat.ambient", 1.0f, 1.0f, true, "&cParty Invite!", "&6From: &e{1}", 2.0f),
                new ChatAction("Party Finder > ([A-Za-z0-9_]+) joined the dungeon group! \\((.*)\\)", true,
                        "entity.experience_orb.pickup",
                        1.0f, 1.0f, true, "&a{1} Joined!", "&7Class: &b{2}", 3.0f)));

        // Command aliases
        public Map<String, String> knownAliases = new HashMap<>();

        // UI
        public Map<String, CardState> lastLoadedCardStates = new HashMap<>();
    }

    public static ConfigData data = new ConfigData();

    public static void save() {
        try {
            File configFile = getConfigFile();
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        migrate();
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            ConfigData loadedData = GSON.fromJson(reader, ConfigData.class);
            if (loadedData != null) {
                // TODO: Delete migration when enough versions have passed
                if (loadedData.modHiderAllowedCustomPayloadChannels != null) {
                    loadedData.modHiderAllowedCustomPayloadChannels.addAll(FABRIC_DEFAULT_CHANNELS);
                }
                if (loadedData.modHiderAllowedMods != null) {
                    loadedData.modHiderAllowedMods.addAll(DEFAULT_ALLOWED_MODS);
                }
                data = loadedData;
                Theme.ACCENT = data.accentColor;
                Theme.refreshColors();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void migrate() {
        File targetFile = getConfigFile();
        if (OLD_CONFIG_FILE.exists() && !targetFile.exists()) {
            try {
                File parent = targetFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                if (OLD_CONFIG_FILE.renameTo(targetFile)) {
                    org.blackum.blackaddons.Blackaddons.LOGGER.info("Successfully migrated config.json to default profile");
                }
            } catch (Exception e) {
                org.blackum.blackaddons.Blackaddons.LOGGER.error("Failed to migrate config.json", e);
            }
        }
    }
}
