package org.blackum.blackaddons.feature.chat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.mixin.core.InventoryAccessor;
import org.blackum.blackaddons.mixin.core.KeyBindingAccessor;
import org.blackum.blackaddons.core.manager.RotationManager;

import java.util.ArrayList;
import java.util.List;

public class ChatActionExecutor {
    private static ChatActionExecutor instance;
    private final List<QueuedAction> queue = new ArrayList<>();
    private final java.util.Map<KeyMapping, Integer> activeKeybinds = new java.util.HashMap<>();

    private ChatActionExecutor() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    public static ChatActionExecutor getInstance() {
        if (instance == null) {
            instance = new ChatActionExecutor();
        }
        return instance;
    }

    private void onTick(Minecraft client) {
        if (client.player == null) {
            activeKeybinds.keySet().forEach(k -> setKeyState(k, false));
            activeKeybinds.clear();
            return;
        }

        if (client.screen != null) {
            activeKeybinds.keySet().forEach(k -> setKeyState(k, false));
            activeKeybinds.clear();
            if (queue.isEmpty()) return;
        }

        List<KeyMapping> toRelease = new ArrayList<>();
        activeKeybinds.forEach((key, ticks) -> {
            if (ticks > 0) {
                activeKeybinds.put(key, ticks - 1);
            } else {
                toRelease.add(key);
            }
        });
        for (KeyMapping k : toRelease) {
            setKeyState(k, false);
            activeKeybinds.remove(k);
        }

        List<QueuedAction> toRemove = new ArrayList<>();
        for (QueuedAction action : queue) {
            action.delay--;
            if (action.delay <= 0) {
                executeAction(client, action.action, action.groups);
                toRemove.add(action);
            }
        }
        queue.removeAll(toRemove);
    }

    public void execute(List<ConfigManager.ActionStep> actions, String[] groups) {
        int totalDelay = 0;
        for (ConfigManager.ActionStep action : actions) {
            totalDelay += action.delayTicks;
            queue.add(new QueuedAction(action, totalDelay, groups));
        }
    }

    private void executeAction(Minecraft client, ConfigManager.ActionStep action, String[] groups) {
        if (client.player == null) return;

        switch (action.type) {
            case SWITCH_SLOT:
                if (action.slotIndex >= 0 && action.slotIndex < 9) {
                    ((InventoryAccessor) client.player.getInventory()).setBlackaddonsSelected(action.slotIndex);
                }
                break;
            case USE_ITEM:
                clickKey(client.options.keyUse);
                break;
            case ATTACK:
                clickKey(client.options.keyAttack);
                break;
            case SEND_MESSAGE:
                String msg = action.message;
                if (groups != null) {
                    for (int i = 1; i < groups.length; i++) {
                        if (groups[i] != null) {
                            msg = msg.replace("{" + i + "}", groups[i]);
                        }
                    }
                }
                if (msg.startsWith("/")) {
                    client.player.connection.sendCommand(msg.substring(1));
                } else {
                    client.player.connection.sendChat(msg);
                }
                break;
            case PRESS_KEYBIND:
                for (KeyMapping key : client.options.keyMappings) {
                    if (key.getName().equalsIgnoreCase(action.message)) {
                        if (action.durationTicks > 0) {
                            setKeyState(key, true);
                            activeKeybinds.put(key, action.durationTicks);
                        } else {
                            clickKey(key);
                        }
                        break;
                    }
                }
                break;
            case ROTATE:
                if (action.useCoordinates) {
                    RotationManager.getInstance().rotateToBlock(action.targetX, action.targetY, action.targetZ);
                } else {
                    RotationManager.getInstance().rotateTo(action.yaw, action.pitch);
                }
                break;
        }
    }

    private void clickKey(KeyMapping key) {
        if (key instanceof KeyBindingAccessor accessor) {
            KeyMapping.click(accessor.getBoundKey());
        }
    }

    private void setKeyState(KeyMapping key, boolean pressed) {
        if (key instanceof KeyBindingAccessor accessor) {
            accessor.setBlackaddonsIsDown(pressed);
        }
    }

    private static class QueuedAction {
        final ConfigManager.ActionStep action;
        int delay;
        final String[] groups;

        QueuedAction(ConfigManager.ActionStep action, int delay, String[] groups) {
            this.action = action;
            this.delay = delay;
            this.groups = groups;
        }
    }
}
