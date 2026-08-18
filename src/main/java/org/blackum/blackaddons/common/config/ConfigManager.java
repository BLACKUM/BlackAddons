package org.blackum.blackaddons.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.feature.cheat.AutoBM;
import org.blackum.blackaddons.feature.cheat.AutoClicker;
import org.blackum.blackaddons.feature.cheat.AutoTNT;
import org.blackum.blackaddons.gui.screen.main.BaseScreen;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.feature.modhider.SpoofMode;
import org.blackum.blackaddons.common.constants.Constants;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import org.blackum.blackaddons.Blackaddons;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfigManager {
    private static final Path OLD_CONFIG_DIR = FabricLoader.getInstance().getConfigDir()
            .resolve(Constants.CONFIG_DIR_NAME);
    private static final File OLD_CONFIG_FILE = OLD_CONFIG_DIR.resolve("config.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final int API_PRIORITY_SLOTS = 3;

    private static File getConfigFile() {
        return ProfileManager.getActiveProfileFile(ProfileManager.Category.CONFIG);
    }

    public static void resetToDefaults() {
        data = new ConfigData();
        Theme.ACCENT = data.accentColor;
        Theme.refreshColors();
        syncOverlayState();
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
        MOVE_KEYBINDS("Move Keybinds"),
        ROTATE("Rotate Camera"),
        SWITCH_SLOT("Switch Slot"),
        USE_ITEM("Use Item"),
        ATTACK("Attack"),
        SEND_MESSAGE("Send Message"),
        PRESS_KEYBIND("Press Keybind"),
        ALIGN("Align [Cheat]");

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
        public float delaySeconds = 0;
        public float durationSeconds = 0;
        public int delayTicks;
        public int durationTicks;
        public float yaw;
        public float pitch;
        public boolean useCoordinates = false;
        public double targetX;
        public double targetY;
        public double targetZ;
        public float rotationSpeed = 0;
        public float lookAtSeconds = 0;
        public boolean instaSnap = false;
        public boolean collapsed = false;
        public boolean lookAfterAlign = false;
        public float alignPostYaw = 0;
        public float alignPostPitch = 0;
        public boolean useLookAfterCoords = false;
        public double alignLookAtX = 0;
        public double alignLookAtY = 0;
        public double alignLookAtZ = 0;
        public List<String> movementKeybinds = new ArrayList<>();

        public ActionStep() {
        }

        public ActionStep(ActionStepType type, int slotIndex, String message, int delayTicks, int durationTicks) {
            this.type = type;
            this.slotIndex = slotIndex;
            this.message = message;
            this.delayTicks = delayTicks;
            this.durationTicks = durationTicks;
            this.delaySeconds = ticksToSeconds(delayTicks);
            this.durationSeconds = ticksToSeconds(durationTicks);
        }

        public ActionStep(ActionStepType type, float yaw, float pitch, int delayTicks) {
            this.type = type;
            this.yaw = yaw;
            this.pitch = pitch;
            this.delayTicks = delayTicks;
            this.delaySeconds = ticksToSeconds(delayTicks);
        }

        public void normalizeTiming() {
            if (type == null) {
                type = ActionStepType.SWITCH_SLOT;
            }
            delaySeconds = normalizeSeconds(delaySeconds, delayTicks);
            durationSeconds = normalizeSeconds(durationSeconds, durationTicks);
            lookAtSeconds = Math.max(0.0f, lookAtSeconds);
            yaw = normalizeYaw(yaw);
            pitch = clampPitch(pitch);
            alignPostYaw = normalizeYaw(alignPostYaw);
            alignPostPitch = clampPitch(alignPostPitch);
            delayTicks = secondsToTicks(delaySeconds);
            durationTicks = secondsToTicks(durationSeconds);
        }

        public void setDelaySeconds(float delaySeconds) {
            this.delaySeconds = normalizeSeconds(delaySeconds, 0);
            this.delayTicks = secondsToTicks(this.delaySeconds);
        }

        public void setDurationSeconds(float durationSeconds) {
            this.durationSeconds = normalizeSeconds(durationSeconds, 0);
            this.durationTicks = secondsToTicks(this.durationSeconds);
        }

        public int getDelayTicks() {
            normalizeTiming();
            return delayTicks;
        }

        public int getDurationTicks() {
            normalizeTiming();
            return durationTicks;
        }

        private static float normalizeSeconds(float seconds, int legacyTicks) {
            if (seconds > 0.0f) {
                return Math.max(0.0f, roundToMillis(seconds));
            }
            if (seconds < 0.0f) {
                return 0.0f;
            }
            if (legacyTicks > 0) {
                return ticksToSeconds(legacyTicks);
            }
            return 0.0f;
        }

        private static int secondsToTicks(float seconds) {
            return Math.max(0, Math.round(Math.max(0.0f, seconds) * 20.0f));
        }

        private static float ticksToSeconds(int ticks) {
            return roundToMillis(Math.max(0, ticks) / 20.0f);
        }

        private static float roundToMillis(float value) {
            return Math.round(Math.max(0.0f, value) * 1000.0f) / 1000.0f;
        }

        private static float normalizeYaw(float yaw) {
            yaw %= 360.0f;
            if (yaw > 180.0f) {
                yaw -= 360.0f;
            }
            if (yaw < -180.0f) {
                yaw += 360.0f;
            }
            return yaw;
        }

        private static float clampPitch(float pitch) {
            return Math.clamp(pitch, -90.0f, 90.0f);
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
        public boolean showNotification = false;
        public String notificationTitle = "";
        public String notificationMessage = "";
        public NotificationType notificationType = NotificationType.INFO;
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
        public boolean triggerOnGuiExit = false;
        public String soundId = "";
        public float volume = 1.0f;
        public float pitch = 1.0f;
        public String title = "";
        public String subtitle = "";
        public float durationSeconds = 2.0f;
        public boolean showNotification = false;
        public String notificationTitle = "";
        public String notificationMessage = "";
        public NotificationType notificationType = NotificationType.INFO;
        public boolean collapsed = true;
        public List<ActionStep> actions = new ArrayList<>();

        public WaypointAction() {
        }
    }

    public static enum DataSource {
        BOT, LOCAL
    }

    public enum ApiPriority {
        PLAIN_DAWN, ADJECTILS, SKYCRYPT, SOOPY
    }

    public enum ChatFilterMatchType {
        CONTAINS,
        STARTS_WITH,
        EXACT,
        REGEX
    }

    public static class ChatVisualFilter {
        public String pattern = "";
        public ChatFilterMatchType matchType = ChatFilterMatchType.CONTAINS;
        public boolean caseSensitive = false;
        public boolean enabled = true;

        public ChatVisualFilter() {
        }

        public ChatVisualFilter(String pattern, ChatFilterMatchType matchType, boolean caseSensitive) {
            this.pattern = pattern;
            this.matchType = matchType;
            this.caseSensitive = caseSensitive;
            this.enabled = true;
        }
    }

    public static class SoloClearInfo {
        public String type;
        public String time;
        public int secrets;
        public List<String> puzzles = new ArrayList<>();
        public boolean princeKilled;
        public boolean mimicKilled;
        public JsonObject mapData;

        public SoloClearInfo() {
        }

        public SoloClearInfo(String type, String time, int secrets, List<String> puzzles, boolean princeKilled,
                boolean mimicKilled, JsonObject mapData) {
            this.type = type;
            this.time = time;
            this.secrets = secrets;
            this.puzzles = puzzles;
            this.princeKilled = princeKilled;
            this.mimicKilled = mimicKilled;
            this.mapData = mapData;
        }
    }

    public static class ConfigData {
        public int overlayX = 5;
        public int overlayY = 5;
        public float overlayScale = 1.0f;
        public boolean showHitboxes = false;
        public boolean showDebugOverlay = false;
        public int accentColor = Theme.DEFAULT_ACCENT;
        public boolean useCardLayout = true;
        public float forcedGuiScale = 1.5f;
        public Map<String, CardState> cardStates = new HashMap<>();

        // Bot
        public String botUrl = Constants.DEFAULT_BOT_URL;
        public DataSource dataSource = DataSource.LOCAL;
        public List<ApiPriority> apiPriorityList = new ArrayList<>(
                List.of(ApiPriority.ADJECTILS, ApiPriority.SKYCRYPT, ApiPriority.PLAIN_DAWN));
        public boolean rngTrackerEnabled = true;
        public String developerKey = "";
        // Sent as "Authorization: Bearer <token>" on every bot request when it is set, and not sent
        // at all when it is empty. For a self-hosted botUrl that refuses anonymous requests; the
        // official backend does not need it, so nothing changes by default.
        public String botToken = "";
        public boolean partyFinderAutoInvite = true;
        public boolean partyFinderAutoAcceptInvite = true;
        public boolean partyFinderShowStatsOnJoin = true;
        public boolean partyFinderShowStatsOnRequest = true;
        public boolean ircEnabled = true;
        public List<String> ircChannels = new ArrayList<>(List.of("general", "announcements", "admin"));
        public String ircPrefix = "AUTO";
        public boolean ircChatMode = false;

        // Mod Hider (ported from ClientSpoofer)
        public SpoofMode modHiderSpoofMode = SpoofMode.CUSTOM;
        public String modHiderCustomClient = "fabric";
        public boolean modHiderHideMods = true;
        public boolean modHiderDisableCustomPayloads = true;
        public Set<String> modHiderAllowedMods = new HashSet<>(DEFAULT_ALLOWED_MODS);
        public Set<String> modHiderAllowedCustomPayloadChannels = new HashSet<>(FABRIC_DEFAULT_CHANNELS);

        // Cheats
        public AutoTNT.FeatureConfig autoTntConfig = new AutoTNT.FeatureConfig();
        public AutoBM.FeatureConfig autoBMConfig = new AutoBM.FeatureConfig();
        public AutoClicker.FeatureConfig autoClickerConfig = new AutoClicker.FeatureConfig();

        public boolean freecamEnabled = false;
        public float freecamSpeed = 1.0f;
        public float freecamScrollSpeed = 0.25f;
        public boolean freecamShowHands = true;
        public int freecamKeyCode = -1;
        public boolean freecamHoldMode = false;

        public boolean perspectiveEnabled = false;
        public float perspectiveDistance = 4.0f;
        public int perspectiveKeyCode = -1;
        public boolean perspectiveHoldMode = false;
        public float perspectiveSensitivity = 1.0f;
        public boolean perspectiveScrollEnabled = true;

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
        public float AutoSSOverlayScale = 1.0f;

        public boolean RelicLookEnabled = false;
        public boolean RelicLookDebug = false;

        // Critter Ebug: report nearby Critter Safari mobs to chat
        public boolean CritterEbugEnabled = false;
        public int CritterEbugRadius = 20;
        public int CritterEbugIntervalTicks = 40;
        public boolean CritterEbugDebug = false;

        public boolean CritterEspEnabled = false;
        public int CritterEspRadius = Constants.DEFAULT_CRITTER_ESP_RADIUS;
        public int CritterEspColor = Constants.DEFAULT_CRITTER_ESP_COLOR;
        public boolean CritterEspBox = true;
        public boolean CritterEspTracers = false;
        public boolean CritterEspNametag = true;
        public boolean shinyCritterAlert = true;
        public int shinyCritterAlertX = -1;
        public int shinyCritterAlertY = Constants.SHINY_CRITTER_HUD_DEFAULT_Y;
        public float shinyCritterAlertScale = 1.0f;


        public boolean showRotationDebug = false;
        public int rotationOverlayX = -1;
        public int rotationOverlayY = 5;
        public float rotationOverlayScale = 1.0f;
        public boolean showAlignDebug = false;
        public boolean alignSilent = true;
        public int alignOverlayX = -1;
        public int alignOverlayY = 125;
        public float alignOverlayScale = 1.0f;
        public boolean showLocationDebug = false;
        public int locationOverlayX = -1;
        public int locationOverlayY = 65;
        public float locationOverlayScale = 1.0f;

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

        // Dungeon Map
        public boolean dungeonMapEnabled = false;
        public boolean dungeonFunnyMap = false;
        public int dungeonMapX = 10;
        public int dungeonMapY = 10;
        public int dungeonMapSize = 128;
        public int dungeonMapColorNormal = 0xFF794600;
        public int dungeonMapColorEntrance = 0xFF20C020;
        public int dungeonMapColorBlood = 0xFFCC2020;
        public int dungeonMapColorFairy = 0xFFDD44DD;
        public int dungeonMapColorPuzzle = 0xFF9B39C8;
        public int dungeonMapColorTrap = 0xFFA97442;
        public int dungeonMapColorChampion = 0xFFFFD400;
        public int dungeonMapColorMimic = 0xFFFF6600;
        public int dungeonMapColorUndiscovered = 0xFF555555;
        public int dungeonMapBackgroundColor = 0x111111;
        public float dungeonMapBackgroundOpacity = 0.75f;
        public boolean dungeonMapShowRoomNames = false;
        public float dungeonMapRoomNameScale = 0.7f;
        public int dungeonMapColorNameDiscovered = 0xFFAAAAAA;
        public int dungeonMapColorNameCleared = 0xFFFFFFFF;
        public int dungeonMapColorNameCompleted = 0xFF55FF55;
        public float dungeonMapUndiscoveredDarkness = 0.25f;
        public int dungeonMapColorBorder = 0xFF555555;
        public boolean dungeonMapBorderEnabled = true;
        public int dungeonMapBorderThickness = 1;
        public float dungeonMapCornerRadius = 2.0f;

        // Water Board Solver
        public boolean waterBoardSolverEnabled = false;
        public boolean waterBoardHudEnabled = true;
        public int waterBoardHudX = -1;
        public int waterBoardHudY = -1;
        public float waterBoardHudScale = 1.0f;
        public float waterBoardTimerScale = 1.0f;

        // Key Timer HUD
        public boolean keyTimerEnabled = false;
        public int keyTimerX = 10;
        public int keyTimerY = 150;
        public float keyTimerScale = 1.0f;

        // TP Maze Solver
        public boolean teleportMazeSolverEnabled = false;
        public boolean teleportMazePrioritizeDiagonal = true;
        public boolean teleportMazeAutoRotate = false;
        public boolean teleportMazeSmoothSnap = true;
        public float teleportMazeAutoRotateSpeed = 60.0f;
        public int teleportMazeVisitedColor = 0xFFFF5555;
        public int teleportMazeCorrectColor = 0xFF55FF55;
        public int teleportMazeMultipleColor = 0xFFFFFF55;
        public int teleportMazeTracerColor = 0xFF55FF55;
        public float teleportMazeTracerWidth = 2.0f;

        // Legit
        public boolean legitFullbrightEnabled = false;
        public boolean removeFireOverlay = false;
        public boolean hideStatusEffects = false;
        public boolean disableNearbyParticles = false;
        public boolean customNicknames = true;
        public boolean scoreboardEnabled = true;
        public int toggleScoreboardKeyCode = -1;
        public boolean chinaHatEnabled = false;

        // Settings
        public int notificationDuration = 4000;
        public int cacheDurationMinutes = 5;
        public boolean disableCommandConfirmation = true;
        public boolean disableUnsecureChatToast = true;
        public boolean disableTutorialToasts = true;
        public boolean toggleTabList = false;
        public boolean chatVisualFiltersEnabled = false;
        public boolean actionTriggersEnabled = true;

        // Custom Font
        public boolean customTextEnabled = true;
        public float customTextScale = 12.5f;
        public boolean customTextGuiOnly = true;
        public boolean customFontEmoji = false;
        public boolean customFontAntiAliasing = true;
        public float customFontAntiAliasingWidth = 0.5f;
        public boolean customFontShadow = false;
        public int customFontShadowColor = 0xAA000000;
        public float customFontShadowOffsetX = 0.5f;
        public float customFontShadowOffsetY = 0.5f;
        public boolean customFontOutline = false;
        public int customFontOutlineColor = 0xFF000000;
        public float customFontOutlineWidth = 0.5f;
        public boolean customFontBold = false;
        public float customFontBoldStrength = 0.5f;
        public boolean customFontItalic = false;
        public float customFontItalicSlant = 0.25f;
        public String customFontGoogleName = "";
        public float customFontSpacing = 0.0f;

        // Vector Text
        public boolean vectorTextEnabled = false;
        public float vectorTextScale = 12.5f;

        // Dungeon Score Calculator
        public boolean enableDungeonScoreTracker = true;
        public String mimicKilledMessage = "Mimic Killed!";
        public String princeKilledMessage = "Prince Killed!";
        public boolean enableMimicKilledMessage = true;
        public boolean enablePrinceKilledMessage = true;

        public List<ChatVisualFilter> chatVisualFilters = new ArrayList<>();

        // Solo Clears
        public List<SoloClearInfo> f7SoloClears = new ArrayList<>();
        public List<SoloClearInfo> m7SoloClears = new ArrayList<>();

        // Chat actions (migration) TODO: Delete migration when enough versions have
        // passed
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

    private static final LinkedHashMap<String, List<String>> GROUP_MAP = new LinkedHashMap<>();
    static {
        GROUP_MAP.put("ui", List.of(
                "overlayX", "overlayY", "overlayScale",
                "showHitboxes", "showDebugOverlay",
                "accentColor", "useCardLayout", "forcedGuiScale",
                "cardStates", "lastLoadedCardStates"));
        GROUP_MAP.put("bot", List.of(
                "botUrl", "dataSource", "apiPriorityList", "developerKey", "botToken",
                "rngTrackerEnabled",
                "partyFinderAutoInvite", "partyFinderAutoAcceptInvite",
                "partyFinderShowStatsOnJoin", "partyFinderShowStatsOnRequest"));
        GROUP_MAP.put("irc", List.of(
                "ircEnabled", "ircChannels", "ircPrefix", "ircChatMode"));
        GROUP_MAP.put("modHider", List.of(
                "modHiderSpoofMode", "modHiderCustomClient",
                "modHiderHideMods", "modHiderDisableCustomPayloads",
                "modHiderAllowedMods", "modHiderAllowedCustomPayloadChannels"));
        GROUP_MAP.put("freecam", List.of(
                "freecamEnabled", "freecamSpeed", "freecamScrollSpeed",
                "freecamShowHands", "freecamKeyCode", "freecamHoldMode"));
        GROUP_MAP.put("perspective", List.of(
                "perspectiveEnabled", "perspectiveDistance",
                "perspectiveKeyCode", "perspectiveHoldMode",
                "perspectiveSensitivity", "perspectiveScrollEnabled"));
        GROUP_MAP.put("autoSS", List.of(
                "AutoSSEnabled", "AutoSSDelay", "AutoSSDistanceLimit", "AutoSSAlerts",
                "AutoSSRotationSpeed", "AutoSSRotationCurve",
                "AutoSSInstantSnap", "AutoSSSkip",
                "AutoSSAutoStart", "AutoSSAutoStartDelay",
                "AutoSSTrySkip", "AutoSSDebug",
                "AutoSSSwapToItem", "AutoSSSwapMode",
                "AutoSSOverlayX", "AutoSSOverlayY", "AutoSSOverlayScale"));
        GROUP_MAP.put("autoClicker", List.of("autoClickerConfig"));
        GROUP_MAP.put("relicLook", List.of(
                "RelicLookEnabled", "RelicLookDebug"));
        GROUP_MAP.put("critterEbug", List.of(
            "CritterEbugEnabled", "CritterEbugRadius", "CritterEbugIntervalTicks", "CritterEbugDebug",
            "CritterEspEnabled", "CritterEspRadius", "CritterEspColor", "CritterEspBox", "CritterEspTracers", "CritterEspNametag",
            "shinyCritterAlert", "shinyCritterAlertX", "shinyCritterAlertY", "shinyCritterAlertScale"));

        GROUP_MAP.put("rotation", List.of(
                "rotationHumanizerEnabled", "rotationVariance", "rotationTargetRandomness",
                "rotationSmoothness", "rotationSpeed",
                "rotationDistanceSlowdown", "rotationDistanceRadius", "rotationFovSlowdown",
                "rotationStopThreshold"));
        GROUP_MAP.put("debugOverlays", List.of(
                "showRotationDebug", "rotationOverlayX", "rotationOverlayY", "rotationOverlayScale",
                "showAlignDebug", "alignOverlayX", "alignOverlayY", "alignOverlayScale",
                "showLocationDebug", "locationOverlayX", "locationOverlayY", "locationOverlayScale"));
        GROUP_MAP.put("dungeonMap", List.of(
                "dungeonMapEnabled", "dungeonFunnyMap",
                "dungeonMapX", "dungeonMapY", "dungeonMapSize",
                "dungeonMapColorNormal", "dungeonMapColorEntrance", "dungeonMapColorBlood",
                "dungeonMapColorFairy", "dungeonMapColorPuzzle", "dungeonMapColorTrap",
                "dungeonMapColorChampion", "dungeonMapColorMimic", "dungeonMapColorUndiscovered",
                "dungeonMapBackgroundColor", "dungeonMapBackgroundOpacity",
                "dungeonMapShowRoomNames", "dungeonMapRoomNameScale",
                "dungeonMapColorNameDiscovered", "dungeonMapColorNameCleared", "dungeonMapColorNameCompleted",
                "dungeonMapUndiscoveredDarkness",
                "dungeonMapColorBorder", "dungeonMapBorderEnabled", "dungeonMapBorderThickness",
                "dungeonMapCornerRadius"));
        GROUP_MAP.put("waterBoard", List.of(
                "waterBoardSolverEnabled", "waterBoardHudEnabled",
                "waterBoardHudX", "waterBoardHudY", "waterBoardHudScale",
                "waterBoardTimerScale"));
        GROUP_MAP.put("tpMaze", List.of(
                "teleportMazeSolverEnabled", "teleportMazePrioritizeDiagonal", "teleportMazeAutoRotate",
                "teleportMazeSmoothSnap",
                "teleportMazeAutoRotateSpeed", "teleportMazeVisitedColor", "teleportMazeCorrectColor",
                "teleportMazeMultipleColor",
                "teleportMazeTracerColor", "teleportMazeTracerWidth"));
        GROUP_MAP.put("visuals", List.of(
                "legitFullbrightEnabled", "removeFireOverlay",
                "hideStatusEffects", "disableNearbyParticles",
                "customNicknames", "scoreboardEnabled", "toggleScoreboardKeyCode",
                "toggleTabList", "disableCommandConfirmation", "disableTutorialToasts",
                "disableUnsecureChatToast", "chinaHatEnabled"));
        GROUP_MAP.put("chat", List.of(
                "notificationDuration", "cacheDurationMinutes",
                "chatVisualFiltersEnabled", "actionTriggersEnabled",
                "chatVisualFilters", "chatActions", "knownAliases"));
        GROUP_MAP.put("customText", List.of(
                "customTextEnabled", "customTextScale", "customTextGuiOnly", "customFontEmoji",
                "customFontAntiAliasing", "customFontAntiAliasingWidth",
                "customFontShadow", "customFontShadowColor",
                "customFontShadowOffsetX", "customFontShadowOffsetY",
                "customFontOutline", "customFontOutlineColor", "customFontOutlineWidth",
                "customFontBold", "customFontBoldStrength",
                "customFontItalic", "customFontItalicSlant",
                "customFontGoogleName", "customFontSpacing"));
        GROUP_MAP.put("vectorText", List.of(
                "vectorTextEnabled", "vectorTextScale"));
        GROUP_MAP.put("dungeonScore", List.of(
                "enableDungeonScoreTracker",
                "mimicKilledMessage", "princeKilledMessage",
                "enableMimicKilledMessage", "enablePrinceKilledMessage",
                "f7SoloClears", "m7SoloClears"));
    }

    private static JsonObject groupJson(JsonObject flat) {
        JsonObject out = new JsonObject();
        for (Map.Entry<String, List<String>> entry : GROUP_MAP.entrySet()) {
            JsonObject group = new JsonObject();
            for (String field : entry.getValue()) {
                if (flat.has(field)) {
                    group.add(field, flat.remove(field));
                }
            }
            if (!group.entrySet().isEmpty()) {
                out.add(entry.getKey(), group);
            }
        }
        for (Map.Entry<String, JsonElement> e : flat.entrySet()) {
            out.add(e.getKey(), e.getValue());
        }
        return out;
    }

    private static void flattenJson(JsonObject root) {
        for (String groupKey : GROUP_MAP.keySet()) {
            JsonElement g = root.get(groupKey);
            if (g != null && g.isJsonObject()) {
                JsonObject group = g.getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : group.entrySet()) {
                    root.add(e.getKey(), e.getValue());
                }
                root.remove(groupKey);
            }
        }
    }

    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Blackaddons-ConfigSave");
        thread.setDaemon(true);
        return thread;
    });

    public static void resetDungeonMapColors() {
        ConfigData defaults = new ConfigData();
        data.dungeonMapColorNormal = defaults.dungeonMapColorNormal;
        data.dungeonMapColorEntrance = defaults.dungeonMapColorEntrance;
        data.dungeonMapColorBlood = defaults.dungeonMapColorBlood;
        data.dungeonMapColorFairy = defaults.dungeonMapColorFairy;
        data.dungeonMapColorPuzzle = defaults.dungeonMapColorPuzzle;
        data.dungeonMapColorTrap = defaults.dungeonMapColorTrap;
        data.dungeonMapColorChampion = defaults.dungeonMapColorChampion;
        data.dungeonMapColorMimic = defaults.dungeonMapColorMimic;
        data.dungeonMapColorUndiscovered = defaults.dungeonMapColorUndiscovered;
        data.dungeonMapBackgroundColor = defaults.dungeonMapBackgroundColor;
        data.dungeonMapColorBorder = defaults.dungeonMapColorBorder;
        data.dungeonMapColorNameDiscovered = defaults.dungeonMapColorNameDiscovered;
        data.dungeonMapColorNameCleared = defaults.dungeonMapColorNameCleared;
        data.dungeonMapColorNameCompleted = defaults.dungeonMapColorNameCompleted;
    }

    public static ConfigData data = new ConfigData();

    public static void save() {
        data.showHitboxes = BaseScreen.showHitboxes;
        data.showDebugOverlay = BaseScreen.showDebugOverlay;
        data.overlayX = BaseScreen.overlayX;
        data.overlayY = BaseScreen.overlayY;
        data.overlayScale = BaseScreen.overlayScale;

        SAVE_EXECUTOR.submit(() -> {
            try {
                File configFile = getConfigFile();
                File parent = configFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                try (FileWriter writer = new FileWriter(configFile)) {
                    JsonObject flat = GSON.toJsonTree(data).getAsJsonObject();
                    JsonObject grouped = groupJson(flat);
                    GSON.toJson(grouped, writer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void load() {
        migrate();
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            flattenJson(root);
            ConfigData loadedData = GSON.fromJson(root, ConfigData.class);
            if (loadedData != null) {
                // TODO: Delete migration when enough versions have passed
                if (loadedData.modHiderAllowedCustomPayloadChannels != null) {
                    loadedData.modHiderAllowedCustomPayloadChannels.addAll(FABRIC_DEFAULT_CHANNELS);
                }
                if (loadedData.modHiderAllowedMods != null) {
                    loadedData.modHiderAllowedMods.addAll(DEFAULT_ALLOWED_MODS);
                }
                if (loadedData.chatVisualFilters == null) {
                    loadedData.chatVisualFilters = new ArrayList<>();
                }
                if (loadedData.apiPriorityList != null) {
                    loadedData.apiPriorityList.removeIf(Objects::isNull);
                }
                if (loadedData.apiPriorityList == null) {
                    loadedData.apiPriorityList = new ArrayList<>(List.of(
                            ApiPriority.ADJECTILS, ApiPriority.SKYCRYPT, ApiPriority.PLAIN_DAWN));
                } else {
                    while (loadedData.apiPriorityList.size() < API_PRIORITY_SLOTS) {
                        ApiPriority fallback = ApiPriority.PLAIN_DAWN;
                        for (ApiPriority p : ApiPriority.values()) {
                            if (!loadedData.apiPriorityList.contains(p)) {
                                fallback = p;
                                break;
                            }
                        }
                        loadedData.apiPriorityList.add(fallback);
                    }
                    if (loadedData.apiPriorityList.size() > API_PRIORITY_SLOTS) {
                        loadedData.apiPriorityList = new ArrayList<>(
                                loadedData.apiPriorityList.subList(0, API_PRIORITY_SLOTS));
                    }
                }
                boolean shouldSave = false;
                if ("http://hypixel-skyblock-socket.pegle.com:8080".equals(loadedData.botUrl)
                        || "http://ba.neutrality.cc:8080".equals(loadedData.botUrl)) {
                    loadedData.botUrl = Constants.DEFAULT_BOT_URL;
                    shouldSave = true;
                }
                data = loadedData;
                if (shouldSave) {
                    save();
                }
                normalizeLegacyActionTimings();
                Theme.ACCENT = data.accentColor;
                Theme.refreshColors();
                syncOverlayState();
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                File backupFile = new File(configFile.getParentFile(), configFile.getName() + ".corrupted");
                configFile.renameTo(backupFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            save();
        }
    }

    private static void syncOverlayState() {
        BaseScreen.showHitboxes = data.showHitboxes;
        BaseScreen.showDebugOverlay = data.showDebugOverlay;
        BaseScreen.overlayX = data.overlayX;
        BaseScreen.overlayY = data.overlayY;
        BaseScreen.overlayScale = data.overlayScale;
    }

    public static void normalizeLegacyActionTimings() {
        if (data.chatActions != null) {
            for (ChatAction chatAction : data.chatActions) {
                normalizeActionSteps(chatAction.actions);
            }
        }
    }

    public static void normalizeActionSteps(List<ActionStep> steps) {
        if (steps == null) {
            return;
        }
        steps.removeIf(Objects::isNull);
        for (ActionStep step : steps) {
            step.normalizeTiming();
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
                    Blackaddons.LOGGER.info("Successfully migrated config.json to default profile");
                }
            } catch (Exception e) {
                Blackaddons.LOGGER.error("Failed to migrate config.json", e);
            }
        }
    }
}
