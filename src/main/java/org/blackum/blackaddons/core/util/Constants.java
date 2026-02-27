package org.blackum.blackaddons.core.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import java.util.Map;

public final class Constants {

        private Constants() {
        }

        public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        // Bot link
        public static final String DISCORD_AUTH_URL = "https://discord.com/oauth2/authorize?client_id=1134507219220713472";

        // Cloudflare bypass
        public static final String BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64; x64; rv:136.0) Gecko/20100101 Firefox/136.0";
        public static final String BOT_USER_AGENT = "BlackAddons/1.0";
        public static final int HTTP_TIMEOUT_SECONDS = 60;

        // Notifications
        public static final String MOD_DETECTION_TITLE = "BlackAddons";
        public static final String MOD_DETECTION_MESSAGE = "Server attempted to read your mod list.";

        // Mod Metadata
        public static final String MOD_ID = "blackaddons";
        public static final String CONFIG_DIR_NAME = MOD_ID; // bruh
        public static final String PRICES_FILE_NAME = "prices.json";
        public static final String TEAMMATES_FILE_NAME = "teammates.json";
        public static final String RNG_DATA_FILE_NAME = "rng_data.json";
        public static final String BLOCKED_PACKETS_LOG_NAME = "blocked_packets.log";
        public static final long BLOCKED_PACKETS_LOG_MAX_BYTES = 5L * 1024 * 1024;

        // APIs
        public static final String DEFAULT_BOT_URL = "http://hypixel-skyblock-socket.pegle.com:8080";
        public static final String BOT_IRC_WS_URL = "ws://hypixel-skyblock-socket.pegle.com:8080/v1/irc";
        public static final String HYPIXEL_BAZAAR_API = "https://api.hypixel.net/skyblock/bazaar";
        public static final String MOULBERRY_AH_API = "https://moulberry.codes/auction_averages_lbin/3day.json";
        public static final String PLAYER_DB_API = "https://playerdb.co/api/player/minecraft/";
        public static final String ADJECTILS_PROFILE_API = "https://adjectilsbackend.adjectivenoun3215.workers.dev/v2/skyblock/profiles?uuid=";
        public static final String SOOPY_PROFILE_API = "https://soopy.dev/api/v2/player_skyblock/";
        public static final String SKYCRYPT_BASE_URL = "https://sky.shiiyu.moe";
        public static final String SKYCRYPT_STATS_API = "https://sky.shiiyu.moe/api/stats/";
        public static final String COFL_SHINY_NECRON_HANDLE = "https://sky.coflnet.com/api/item/price/NECRON_HANDLE?IsShiny=true";
        public static final String COFL_SKELETON_MASTER_CHESTPLATE_MAX = "https://sky.coflnet.com/api/item/price/SKELETON_MASTER_CHESTPLATE?ItemTier=10-10&NoOtherValuableEnchants=true&BaseStatBoost=50";
        public static final String COFL_SKELETON_MASTER_CHESTPLATE_BASE = "https://sky.coflnet.com/api/item/price/SKELETON_MASTER_CHESTPLATE?BaseStatBoost=50";

        // Bot Endpoints
        public static final String BOT_API_RNG = "/v1/rng";
        public static final String BOT_API_DAILY = "/v1/daily";
        public static final String BOT_API_PROFILE = "/v1/profile";
        public static final String BOT_API_RTCA = "/v1/rtca";
        public static final String BOT_API_LEADERBOARD = "/v1/leaderboard";
        public static final String BOT_API_KEY = "/v1/key";
        public static final String BOT_API_PARTY_CREATE = "/v1/party/create";
        public static final String BOT_API_PARTY_UNQUEUE = "/v1/party/unqueue";
        public static final String BOT_API_PARTY_UPDATE = "/v1/party/update";
        public static final String BOT_API_PARTY_LIST = "/v1/party/list";
        public static final String BOT_API_NAMES = "/v1/names";

        // External URLs
        public static final String GITHUB_NAMES_URL = "https://raw.githubusercontent.com/BLACKUM/rtca-bot-hypixel/refs/heads/main/data/custom_names.json";

        // API Headers
        public static final String HEADER_ENCRYPTED_IDENTITY = "X-Encrypted-Identity";
        public static final String HEADER_DEVELOPER_KEY = "X-Developer-Key";

        // Catacombs Data
        public static final double CATA_50_XP = 569809640.0;
        public static final List<Double> DUNGEON_XP = List.of(
                        0.0, 50.0, 75.0, 110.0, 160.0, 230.0, 330.0, 470.0, 670.0, 950.0, 1340.0,
                        1890.0, 2665.0, 3760.0, 5260.0, 7380.0, 10300.0, 14400.0, 20000.0, 27600.0,
                        38000.0, 52500.0, 71500.0, 97000.0, 132000.0, 180000.0, 243000.0, 328000.0,
                        445000.0, 600000.0, 800000.0, 1065000.0, 1410000.0, 1900000.0, 2500000.0,
                        3300000.0, 4300000.0, 5600000.0, 7200000.0, 9200000.0, 12000000.0, 15000000.0,
                        19000000.0, 24000000.0, 30000000.0, 38000000.0, 48000000.0, 60000000.0, 75000000.0,
                        93000000.0, 116250000.0, 200000000.0);

        public static final Map<String, Integer> FLOOR_XP_MAP = Map.ofEntries(
                        Map.entry("M7", 300000), Map.entry("M6", 100000), Map.entry("M5", 70000),
                        Map.entry("M4", 55000), Map.entry("M3", 35000), Map.entry("M2", 20000),
                        Map.entry("M1", 15000),
                        Map.entry("F7", 28000), Map.entry("F6", 4880), Map.entry("F5", 2400),
                        Map.entry("F4", 1420), Map.entry("F3", 560), Map.entry("F2", 220),
                        Map.entry("F1", 110), Map.entry("Entrance", 55));

        // RNG Display Keywords
        public static final String RARE_DROP_KEYWORD = "RARE DROP!";
        public static final String PRAY_DROP_KEYWORD = "PRAY TO RNGESUS DROP!";
        public static final String MAGIC_FIND_LABEL = "✯ Magic Find";

        // Command Constants
        public static final String BASE_COMMAND = "b";
        public static final String CMD_ARG_TYPE = "type";
        public static final String CMD_ARG_MAGIC_FIND = "magic_find";
        public static final String CMD_ARG_ITEM = "item";
        public static final String CMD_ARG_IGN = "ign";

        public static final String DROP_TYPE_RARE = "rare";
        public static final String DROP_TYPE_CRAZY = "crazy";
        public static final String DROP_TYPE_PRAY = "pray";

        // Caching
        public static final long DAY_IN_MS = 24 * 60 * 60 * 1000L;
        public static final long PRICE_CACHE_DURATION_MS = DAY_IN_MS;

        // Party Finder
        public static final String JOIN_REQUEST_TEMPLATE = "[BlackAddons] join party request - id:%s";
        public static final String JOIN_REQUEST_WHISPER_REGEX = "^(?:From (?:\\[.+\\] )?(\\w+):|(?:\\[.+\\] )?(\\w+) whispers to you:)\\s*\\[BlackAddons\\] join party request - id:([\\w-]+)\\s*$";
        public static final String PARTY_INVITE_REGEX = "(?s)^(?:.*?(?:\\[.+\\] )?(\\w+) has invited you to join (?:their|(?:\\[.+\\] )?(\\w+)'s) party!.*)$";
        public static final String PARTY_JOINED_REGEX = "^(?:\\[.+\\] )?(\\w+) joined the party\\.$";
        public static final String PARTY_LEFT_REGEX = "^(?:\\[.+\\] )?(\\w+) left the party\\.$";
        public static final String PARTY_REMOVED_REGEX = "^(?:\\[.+\\] )?(\\w+) was removed from the party\\.$";
        public static final String PARTY_JOINED_OTHERS_REGEX = "^You joined (?:\\[.+\\] )?(\\w+)'s party!$";
        public static final String PARTY_DISBANDED_REGEX = "^The party was disbanded\\.$";
        public static final String PARTY_LEAVE_REGEX = "^You left the party\\.$";
        public static final String PARTY_FINDER_TITLE = "Party Finder";
        public static final String MSG_ALREADY_IN_QUEUE = "You are already in queue!";
        public static final String MSG_WANTS_TO_JOIN = "§ewants to join your party.";
        public static final String LABEL_INVITE = "§7[§aInvite§7]";
        public static final String LABEL_PROFILE = "§7[§eProfile§7]";
        public static final String HOVER_INVITE = "Click to invite";
        public static final String HOVER_PROFILE = "Check player profile";
        public static final String MSG_AUTO_ACCEPT = "Auto-accepting invite from %s";
        public static final String MSG_QUEUE_STARTED = "Party finder queue started for %s";
        public static final String MSG_REMOVED_QUEUE = "Removed from party finder queue.";
        public static final String DEFAULT_PARTY_NOTE = "BlackAddons Party";

        // Discord Image Preview
        public static final String DISCORD_IMAGE_REGEX = "https://(?:(?:www\\.)?tenor\\.com/view/[a-zA-Z0-9-]+-\\d+(?![a-zA-Z0-9-])|[^\\s§\\[\\]]+?\\.(?:png|jpg|jpeg|webp|gif)(?![\\w-]))(?:\\?[^\\s§\\[\\]]*)?";
        public static final String PREVIEW_LABEL = " §7[§bPreview§7]";
        public static final String PREVIEW_HOVER = "Click to preview image";
        public static final int BUFFER_SIZE = 8192;
        public static final int DEFAULT_GIF_DELAY = 100;
}
