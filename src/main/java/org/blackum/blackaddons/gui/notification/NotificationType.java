package org.blackum.blackaddons.gui.notification;

import org.blackum.blackaddons.gui.render.Theme;

public enum NotificationType {
    INFO(0xFF50C8FF),
    SUCCESS(0xFF55FF55),
    WARNING(0xFFFFAA00),
    ERROR(0xFFFF5555);

    private final int color;

    NotificationType(int color) {
        this.color = color;
    }

    public int getColor() {
        switch (this) {
            case INFO:
                return Theme.ACCENT;
            default:
                return color;
        }
    }
}
