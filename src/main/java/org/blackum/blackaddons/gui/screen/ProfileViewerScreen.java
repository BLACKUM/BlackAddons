package org.blackum.blackaddons.gui.screen;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import org.blackum.blackaddons.gui.widget.Label;
import org.blackum.blackaddons.gui.widget.TabPanel;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.ConfettiEffect;
import org.blackum.blackaddons.core.manager.ProfileStateManager;

import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.gui.screen.tabs.*;

public class ProfileViewerScreen extends BaseScreen {
    private final String player;
    private String profileName;
    private final boolean forceUpdate;
    private TabPanel tabPanel;
    private JsonObject profileData;
    private boolean isLoading = true;
    private String errorMessage = null;
    private static int lastTabIndex = 0;

    private DungeonsTabController dungeonsController;
    private TeammatesTabController teammatesController;
    private RngTabController rngController;
    private DailyTabController dailyController;
    private RtcaTabController rtcaController;

    @Override
    protected void initWidgets() {
        if (isLoading) {
            ProfileStateManager.getInstance().getProfile(player, profileName, forceUpdate)
                    .thenAccept(result -> {
                        isLoading = false;
                        if (result.isSuccess()) {
                            profileData = result.getData();
                        } else {
                            errorMessage = result.getError();
                        }
                        Minecraft.getInstance()
                                .execute(() -> this.init(Minecraft.getInstance(), this.width, this.height));
                    });
        }

        if (isLoading) {
            addWidget(new Label(containerX + containerWidth / 2 - 30, containerY + containerHeight / 2, "Loading...",
                    Label.Style.TITLE));
            return;
        }

        if (errorMessage != null) {
            addWidget(new Label(containerX + containerWidth / 2 - 60, containerY + containerHeight / 2, errorMessage,
                    Label.Style.TITLE));
            return;
        }

        if (profileData == null)
            return;

        tabPanel = new TabPanel(containerX, containerY + 40, containerWidth, containerHeight - 40);
        tabPanel.setOnTabChange(index -> {
            lastTabIndex = index;
            stopConfetti();
        });
        addWidget(tabPanel);

        if (dungeonsController == null) {
            dungeonsController = new DungeonsTabController(this, profileData);
        }
        dungeonsController.init(tabPanel.addTab("Dungeons"));

        if (teammatesController == null) {
            teammatesController = new TeammatesTabController(this, profileData);
        }
        teammatesController.init(tabPanel.addTab("Recent Teammates"));

        if (rngController == null) {
            rngController = new RngTabController(this, profileData, player);
        }
        rngController.init(tabPanel.addTab("RNG"));

        if (dailyController == null) {
            dailyController = new DailyTabController(this, profileData);
        }
        dailyController.init(tabPanel.addTab("Leaderboard"));

        if (rtcaController == null) {
            rtcaController = new RtcaTabController(this, profileData);
        }
        rtcaController.init(tabPanel.addTab("RTCA"));

        if (profileData.has("profiles")) {
            com.google.gson.JsonArray profiles = profileData.getAsJsonArray("profiles");
            if (profiles.size() > 1) {
                java.util.List<String> profileNames = new java.util.ArrayList<>();
                String currentSelected = profileName;

                String apiSelected = null;

                for (com.google.gson.JsonElement p : profiles) {
                    JsonObject prof = p.getAsJsonObject();
                    String name = prof.get("name").getAsString();

                    boolean isSelected;
                    if (this.profileName != null) {
                        isSelected = name.equalsIgnoreCase(this.profileName);
                    } else {
                        isSelected = prof.get("selected").getAsBoolean();
                    }

                    if (isSelected) {
                        String display = name + " (Selected)";
                        profileNames.add(display);
                        apiSelected = display;
                    } else {
                        profileNames.add(name);
                    }
                }

                if (apiSelected != null) {
                    currentSelected = apiSelected;
                }

                org.blackum.blackaddons.gui.widget.Dropdown profileDropdown = new org.blackum.blackaddons.gui.widget.Dropdown(
                        containerX + containerWidth - 160, containerY + 10, 150, 20,
                        (currentSelected != null ? currentSelected : "Profile"),
                        profileNames,
                        this::switchProfile);

                if (currentSelected != null) {
                    profileDropdown.setSelectedOption(currentSelected);
                }
                addWidget(profileDropdown);
            }
        }

        tabPanel.selectTab(lastTabIndex);
    }

    public ProfileViewerScreen(Screen parent, String player) {
        this(parent, player, null, false, null);
    }

    public ProfileViewerScreen(Screen parent, String player, boolean force) {
        this(parent, player, null, force, null);
    }

    public ProfileViewerScreen(Screen parent, String player, String profileName, boolean force,
            JsonObject data) {
        super(Component.literal("Profile: " + player), parent);
        this.player = player;
        this.profileName = profileName;
        this.forceUpdate = force;
        if (data != null) {
            this.profileData = data;
            this.isLoading = false;
        }
    }

    public String getPlayer() {
        return player;
    }

    public String getProfileName() {
        return profileName;
    }

    private void switchProfile(String newProfile) {
        if (newProfile.endsWith(" (Selected)")) {
            newProfile = newProfile.replace(" (Selected)", "");
        }

        if (newProfile.equals(this.profileName))
            return;

        NotificationManager.addNotification("Profile", "Switching to " + newProfile + "...", NotificationType.INFO);

        final String targetProfile = newProfile;

        ProfileStateManager.getInstance().getProfile(player, targetProfile, false)
                .thenAccept(result -> {
                    if (result.isSuccess() && result.getData() != null) {
                        this.profileData = result.getData();
                        this.profileName = targetProfile;

                        this.dungeonsController = null;
                        this.teammatesController = null;
                        this.rngController = null;
                        this.dailyController = null;
                        this.rtcaController = null;

                        Minecraft.getInstance().execute(() -> {
                            this.init(Minecraft.getInstance(), this.width, this.height);
                        });
                    } else {
                        String err = result.getError() != null ? result.getError() : "Unknown error";
                        NotificationManager.addNotification("Switch Failed", err, NotificationType.ERROR);
                    }
                });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderConfetti(graphics);

        if (!isLoading && profileData != null) {
            String label = "Viewing: " + player;
            int x = containerX + 10;
            int y = containerY + containerHeight - 25;
            graphics.drawString(Minecraft.getInstance().font, label, x, y,
                    Theme.TEXT_SECONDARY);

        }
    }

    @Override
    protected int getContentHeight() {
        return tabPanel != null ? tabPanel.getMaxContentHeight() : super.getContentHeight();
    }

    @Override
    public void tick() {
        super.tick();
        tickConfetti();

        if (dailyController != null) {
            dailyController.tick();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tabPanel != null && tabPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private final ConfettiEffect confetti = new ConfettiEffect();

    public void startConfetti() {
        confetti.start(width, height);
    }

    private void stopConfetti() {
        confetti.stop();
    }

    private void tickConfetti() {
        confetti.tick(width, height);
    }

    private void renderConfetti(GuiGraphics graphics) {
        confetti.render(graphics);
    }
}
