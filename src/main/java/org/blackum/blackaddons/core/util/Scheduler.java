package org.blackum.blackaddons.core.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class Scheduler {
    private static long currentTicks = 0;
    private static final List<Task> tasks = new CopyOnWriteArrayList<>();

    private static class Task {
        private final long targetMs;
        private final long targetTicks;
        private final Runnable action;
        private volatile boolean msPassed = false;
        private volatile boolean ticksPassed = false;
        private volatile boolean executed = false;

        public Task(long targetMs, long targetTicks, Runnable action) {
            this.targetMs = targetMs;
            this.targetTicks = targetTicks;
            this.action = action;
        }
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            currentTicks++;
            process(task -> task.ticksPassed = currentTicks >= task.targetTicks);
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("blackaddons", "scheduler"), (graphics, deltaTracker) -> {
            process(task -> task.msPassed = System.currentTimeMillis() >= task.targetMs);
        });
    }

    public static void schedule(int msDelay, int tickDelay, Runnable action) {
        tasks.add(new Task(
            System.currentTimeMillis() + msDelay,
            currentTicks + tickDelay,
            action
        ));
    }

    private static void process(Consumer<Task> updateState) {
        if (tasks.isEmpty()) return;

        for (Task task : tasks) {
            updateState.accept(task);

            if (task.msPassed && task.ticksPassed && !task.executed) {
                synchronized (task) {
                    if (!task.executed) {
                        task.executed = true;
                        task.action.run();
                        tasks.remove(task);
                    }
                }
            }
        }
    }
}
