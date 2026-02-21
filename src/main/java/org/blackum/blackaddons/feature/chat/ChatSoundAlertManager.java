package org.blackum.blackaddons.feature.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.blackum.blackaddons.core.config.ConfigManager;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ChatSoundAlertManager {
    private static ChatSoundAlertManager instance;

    private ChatSoundAlertManager() {
    }

    public static ChatSoundAlertManager getInstance() {
        if (instance == null) {
            instance = new ChatSoundAlertManager();
        }
        return instance;
    }

    public void onChatMessage(Component message) {
        if (message == null)
            return;

        String rawText = message.getString();
        if (rawText == null || rawText.isEmpty())
            return;

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null)
            return;

        for (ConfigManager.SoundAlert alert : ConfigManager.data.chatSoundAlerts) {
            if (!alert.enabled || alert.pattern == null || alert.pattern.isEmpty() || alert.soundId == null
                    || alert.soundId.isEmpty())
                continue;

            boolean matched = false;
            String[] groups = new String[0];

            if (alert.isRegex) {
                try {
                    Pattern pattern = Pattern.compile(alert.pattern);
                    java.util.regex.Matcher matcher = pattern.matcher(rawText);
                    if (matcher.find()) {
                        matched = true;
                        groups = new String[matcher.groupCount() + 1];
                        for (int i = 0; i <= matcher.groupCount(); i++) {
                            groups[i] = matcher.group(i);
                        }
                    }
                } catch (PatternSyntaxException e) {
                }
            } else {
                if (rawText.contains(alert.pattern)) {
                    matched = true;
                }
            }

            if (matched) {
                try {
                    ResourceLocation location = ResourceLocation.tryParse(alert.soundId);
                    if (location == null)
                        location = ResourceLocation.fromNamespaceAndPath("minecraft", alert.soundId);
                    SoundEvent event = SoundEvent.createVariableRangeEvent(location);
                    final String[] finalGroups = groups;
                    client.execute(() -> {
                        client.getSoundManager().play(SimpleSoundInstance.forUI(event, alert.pitch, alert.volume));
                        if (alert.title != null && !alert.title.isEmpty() && client.gui != null) {
                            String fTitle = alert.title;
                            String fSubtitle = alert.subtitle != null ? alert.subtitle : "";

                            for (int i = 1; i < finalGroups.length; i++) {
                                if (finalGroups[i] != null) {
                                    String replacement = finalGroups[i];
                                    fTitle = fTitle.replace("{" + i + "}", replacement);
                                    fSubtitle = fSubtitle.replace("{" + i + "}", replacement);
                                }
                            }

                            client.gui.setTimes(10, alert.durationSeconds * 20, 20);
                            client.gui.setTitle(net.minecraft.network.chat.Component
                                    .literal(org.blackum.blackaddons.core.util.FormatUtils.formatColor(fTitle)));
                            if (!fSubtitle.isEmpty()) {
                                client.gui.setSubtitle(
                                        net.minecraft.network.chat.Component
                                                .literal(org.blackum.blackaddons.core.util.FormatUtils
                                                        .formatColor(fSubtitle)));
                            }
                        }
                    });
                } catch (Exception e) {
                }
            }
        }
    }
}
