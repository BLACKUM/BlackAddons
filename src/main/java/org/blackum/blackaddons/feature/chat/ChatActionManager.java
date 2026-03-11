package org.blackum.blackaddons.feature.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.blackum.blackaddons.core.config.ConfigManager;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ChatActionManager {
    private static ChatActionManager instance;

    private ChatActionManager() {
    }

    public static ChatActionManager getInstance() {
        if (instance == null) {
            instance = new ChatActionManager();
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

        for (ConfigManager.ChatAction trigger : ConfigManager.data.chatActions) {
            if (!trigger.enabled || trigger.pattern == null || trigger.pattern.isEmpty())
                continue;

            boolean matched = false;
            String[] groups = new String[0];

            if (trigger.isRegex) {
                try {
                    Pattern pattern = Pattern.compile(trigger.pattern);
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
                if (rawText.contains(trigger.pattern)) {
                    matched = true;
                }
            }

            if (matched) {
                final String[] finalGroups = groups;
                client.execute(() -> {
                    if (trigger.soundId != null && !trigger.soundId.isEmpty()) {
                        try {
                            ResourceLocation location = ResourceLocation.tryParse(trigger.soundId);
                            if (location == null)
                                location = ResourceLocation.fromNamespaceAndPath("minecraft", trigger.soundId);
                            SoundEvent event = SoundEvent.createVariableRangeEvent(location);
                            client.getSoundManager().play(SimpleSoundInstance.forUI(event, trigger.pitch, trigger.volume));
                        } catch (Exception e) {
                        }
                    }

                    if (trigger.durationSeconds > 0 && trigger.title != null && !trigger.title.isEmpty() && client.gui != null) {
                        String fTitle = trigger.title;
                        String fSubtitle = trigger.subtitle != null ? trigger.subtitle : "";

                        for (int i = 1; i < finalGroups.length; i++) {
                            if (finalGroups[i] != null) {
                                String replacement = finalGroups[i];
                                fTitle = fTitle.replace("{" + i + "}", replacement);
                                fSubtitle = fSubtitle.replace("{" + i + "}", replacement);
                            }
                        }

                        client.gui.setTimes(10, (int) (trigger.durationSeconds * 20), 20);
                        client.gui.setTitle(Component.literal(org.blackum.blackaddons.core.util.FormatUtils.formatColor(fTitle)));
                        if (!fSubtitle.isEmpty()) {
                            client.gui.setSubtitle(
                                    Component.literal(org.blackum.blackaddons.core.util.FormatUtils
                                                    .formatColor(fSubtitle)));
                        }
                    }

                    if (!trigger.actions.isEmpty()) {
                        ChatActionExecutor.getInstance().execute(trigger.actions, finalGroups);
                    }
                });
            }
        }
    }
}
