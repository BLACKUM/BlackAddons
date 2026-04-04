package org.blackum.blackaddons.gui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ConfettiEffect {
    private final List<Confetti> particles = new ArrayList<>();
    private boolean active = false;
    private int timer = 0;
    private int screenWidth;
    private int screenHeight;

    public void start(int width, int height) {
        if (active)
            return;
        this.screenWidth = width;
        this.screenHeight = height;
        stop();
        active = true;
        timer = 100;
        spawnBurst();
    }

    public void stop() {
        active = false;
        timer = 0;
        particles.clear();
    }

    private void spawnBurst() {
        if (screenWidth <= 0)
            return;
        for (int i = 0; i < 100; i++) {
            double startX = screenWidth / 2.0;
            double startY = screenHeight;
            double sx = (Math.random() - 0.5) * 10;
            double sy = -(Math.random() * 5 + 5);
            Confetti c = new Confetti(startX, startY);
            c.speedX = sx;
            c.speedY = sy;
            particles.add(c);
        }
    }

    public void tick(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;

        if (active) {
            timer--;
            if (timer <= 0) {
                active = false;
            } else if (timer % 5 == 0 && particles.size() < 200) {
                for (int k = 0; k < 2; k++) {
                    double startX = width / 2.0 + (Math.random() - 0.5) * 100;
                    double startY = height;
                    Confetti c = new Confetti(startX, startY);
                    c.speedX = (Math.random() - 0.5) * 5;
                    c.speedY = -(Math.random() * 5 + 5);
                    particles.add(c);
                }
            }
        }

        if (particles.isEmpty())
            return;

        Iterator<Confetti> it = particles.iterator();
        while (it.hasNext()) {
            Confetti c = it.next();
            c.x += c.speedX;
            c.y += c.speedY;
            c.speedY += 0.2;
            c.life--;

            if (c.y > height + 20 || c.life <= 0) {
                it.remove();
            }
        }
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (particles.isEmpty())
            return;
        for (Confetti c : particles) {
            graphics.fill((int) c.x, (int) c.y, (int) (c.x + c.size), (int) (c.y + c.size), c.color);
        }
    }

    private static class Confetti {
        double x, y;
        double speedX, speedY;
        int color;
        int life;
        int maxLife;
        float size;

        Confetti(double x, double y) {
            this.x = x;
            this.y = y;
            this.speedX = (Math.random() - 0.5) * 5;
            this.speedY = -(Math.random() * 3 + 2);
            java.awt.Color c = java.awt.Color.getHSBColor((float) Math.random(), 1f, 1f);
            this.color = c.getRGB();
            this.maxLife = 100 + (int) (Math.random() * 100);
            this.life = this.maxLife;
            this.size = (float) (Math.random() * 3 + 2);
        }
    }
}
