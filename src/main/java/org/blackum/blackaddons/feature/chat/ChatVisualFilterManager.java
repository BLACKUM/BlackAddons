package org.blackum.blackaddons.feature.chat;

import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.config.ConfigManager;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ChatVisualFilterManager {

    private ChatVisualFilterManager() {
    }

    public static boolean shouldHide(Component message) {
        if (message == null || !ConfigManager.data.chatVisualFiltersEnabled) {
            return false;
        }

        String plainText = message.getString();
        if (plainText == null || plainText.isBlank()) {
            return false;
        }

        if (ConfigManager.data.chatVisualFilters == null || ConfigManager.data.chatVisualFilters.isEmpty()) {
            return false;
        }

        for (ConfigManager.ChatVisualFilter filter : ConfigManager.data.chatVisualFilters) {
            if (matches(filter, plainText)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matches(ConfigManager.ChatVisualFilter filter, String message) {
        if (filter == null || !filter.enabled || filter.pattern == null || filter.pattern.isBlank()) {
            return false;
        }

        String pattern = filter.pattern.trim();
        if (pattern.isEmpty()) {
            return false;
        }

        return switch (filter.matchType) {
            case STARTS_WITH -> prepare(message, filter).startsWith(prepare(pattern, filter));
            case EXACT -> prepare(message, filter).equals(prepare(pattern, filter));
            case REGEX -> matchesRegex(filter, message, pattern);
            case CONTAINS -> prepare(message, filter).contains(prepare(pattern, filter));
        };
    }

    private static boolean matchesRegex(ConfigManager.ChatVisualFilter filter, String message, String pattern) {
        try {
            int flags = filter.caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            return Pattern.compile(pattern, flags).matcher(message).find();
        } catch (PatternSyntaxException ignored) {
            return false;
        }
    }

    private static String prepare(String value, ConfigManager.ChatVisualFilter filter) {
        return filter.caseSensitive ? value : value.toLowerCase(Locale.ROOT);
    }
}
