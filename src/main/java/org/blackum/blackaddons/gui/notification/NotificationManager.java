package org.blackum.blackaddons.gui.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.gui.render.RenderHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public class NotificationManager {
    private static final NotificationManager INSTANCE = new NotificationManager();
    private final List<Notification> notifications = new ArrayList<>();

    private NotificationManager() {
    }

    public static NotificationManager getInstance() {
        return INSTANCE;
    }

    public void add(String title, String message, NotificationType type) {
        notifications.add(new Notification(title, message, type));
    }

    public static void addNotification(String title, String message, NotificationType type) {
        INSTANCE.add(title, message, type);
    }

    public void tick() {
        Iterator<Notification> iterator = notifications.iterator();
        while (iterator.hasNext()) {
            Notification notification = iterator.next();
            notification.tick();
            if (notification.isExpired()) {
                iterator.remove();
            }
        }
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (notifications.isEmpty())
            return;

        float scale = RenderHelper.getGuiScaleFactor();
        Minecraft mc = Minecraft.getInstance();
        
        int screenWidth = (int) (mc.getWindow().getGuiScaledWidth() / scale);
        int screenHeight = (int) (mc.getWindow().getGuiScaledHeight() / scale);

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);

        int bottomY = screenHeight - 50;
        int x = screenWidth - 170;

        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            int height = notification.getHeight();
            int topY = bottomY - height;

            notification.render(graphics, x, topY);

            bottomY = topY - 5;
        }

        graphics.pose().popMatrix();
    }
}
