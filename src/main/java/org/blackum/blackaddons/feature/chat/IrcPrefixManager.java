package org.blackum.blackaddons.feature.chat;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.Blackaddons;

import java.lang.reflect.Field;

public class IrcPrefixManager {
    private static String cachedPrefix = null;

    public static String getPrefix() {
        if (cachedPrefix != null) {
            return cachedPrefix;
        }

        String configPrefix = ConfigManager.data.ircPrefix;
        if (!"AUTO".equalsIgnoreCase(configPrefix)) {
            cachedPrefix = configPrefix;
            return cachedPrefix;
        }

        if (isPrefixTaken("#")) {
            Blackaddons.LOGGER.info("IRC prefix '#' is taken by another mod, using '##' instead.");
            cachedPrefix = "##";
        } else {
            cachedPrefix = "#";
        }

        return cachedPrefix;
    }

    private static boolean isPrefixTaken(String prefix) {
        try {
            Object event = net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.ALLOW_CHAT;
            Class<?> eventClass = event.getClass();
            Object[] handlers = null;

            while (eventClass != null && handlers == null) {
                for (Field f : eventClass.getDeclaredFields()) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(event);
                        if (val instanceof Object[] arr) {
                            handlers = arr;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
                eventClass = eventClass.getSuperclass();
            }

            if (handlers != null) {
                String probeMessage = prefix + " "; 
                for (Object handler : handlers) {
                    if (handler == null) continue;
                    String className = handler.getClass().getName();
                    if (className.contains("org.blackum.blackaddons")) {
                        continue;
                    }

                    try {
                        for (java.lang.reflect.Method m : handler.getClass().getDeclaredMethods()) {
                            if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class && m.getReturnType() == boolean.class) {
                                m.setAccessible(true);
                                if (!(Boolean) m.invoke(handler, probeMessage)) {
                                    return true;
                                }
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            org.blackum.blackaddons.Blackaddons.LOGGER.error("Failed to detect IRC prefix conflict: " + e.getMessage());
        }
        return false;
    }

    public static void resetCache() {
        cachedPrefix = null;
    }
}
