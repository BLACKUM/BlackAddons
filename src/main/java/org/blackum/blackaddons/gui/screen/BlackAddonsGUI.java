package org.blackum.blackaddons.gui.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.tabs.*;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.Consumer;

public class BlackAddonsGUI extends BaseScreen {

    private TabPanel tabPanel;
    private static int lastTabIndex = 0;
    private String currentTooltip = null;
    private final Map<String, Boolean> collapsedGroups = new HashMap<>();

    private SettingsTabController settingsController;
    private ModHiderTabController modHiderController;
    private PayloadsTabController payloadsController;
    private CheatsTabController cheatsController;
    private LegitTabController legitController;
    private AboutTabController aboutController;
    private SoundAlertsTabController soundAlertsController;

    public BlackAddonsGUI() {
        this(null);
    }

    public BlackAddonsGUI(Screen parent) {
        super(Component.literal("BlackAddons Settings"), parent);
    }

    @Override
    protected void initWidgets() {
        tabPanel = new TabPanel(containerX + 10, containerY + 40, containerWidth - 20, containerHeight - 50);
        tabPanel.setOnTabChange(index -> lastTabIndex = index);

        settingsController = new SettingsTabController(this);
        modHiderController = new ModHiderTabController(this);
        payloadsController = new PayloadsTabController(this);
        cheatsController = new CheatsTabController(this);
        legitController = new LegitTabController(this);
        aboutController = new AboutTabController(this);
        soundAlertsController = new SoundAlertsTabController(this);

        settingsController.init(tabPanel.addTab("Settings"));
        modHiderController.init(tabPanel.addTab("Mod Hider"));
        payloadsController.init(tabPanel.addTab("Payloads"));
        cheatsController.init(tabPanel.addTab("Cheats"));
        legitController.init(tabPanel.addTab("Legit"));
        soundAlertsController.init(tabPanel.addTab("Chat Triggers"));
        aboutController.init(tabPanel.addTab("About"));

        tabPanel.selectTab(lastTabIndex);
        widgets.add(tabPanel);
    }

    @Override
    protected int getContentHeight() {
        return tabPanel != null ? tabPanel.getMaxContentHeight() : super.getContentHeight();
    }

    @Override
    public void tick() {
        super.tick();
    }

    public void rebuildChannelsList(ListView channelsList) {
        channelsList.clearItems();
        List<String> channels = new ArrayList<>(ConfigManager.data.modHiderAllowedCustomPayloadChannels);
        channels.sort(String::compareToIgnoreCase);
        for (String ch : channels) {
            Button remove = new Button(0, 0, channelsList.getWidth() - 8, "Remove: " + ch, () -> {
                ConfigManager.data.modHiderAllowedCustomPayloadChannels.remove(ch);
                ConfigManager.save();
                rebuildChannelsList(channelsList);
            });
            channelsList.addItem(remove);
        }
    }

    public void rebuildAllowedModsList(ListView allowedModsList, TextField modSearch) {
        allowedModsList.clearItems();
        String query = modSearch.getText() == null ? "" : modSearch.getText().trim().toLowerCase(Locale.ROOT);

        ModOrganizer.OrganizedMods organizedMods = ModOrganizer.organizeMods();

        Consumer<String> enableDependencies = new Consumer<String>() {
            @Override
            public void accept(String modId) {
                ModOrganizer.ModInfo info = organizedMods.allMods.get(modId);
                if (info != null) {
                    ConfigManager.data.modHiderAllowedMods.add(modId);
                    for (String depId : info.dependencies) {
                        if (organizedMods.allMods.containsKey(depId)
                                && !ConfigManager.data.modHiderAllowedMods.contains(depId)) {
                            this.accept(depId);
                        }
                    }
                }
            }
        };

        Consumer<String> disableDependents = new Consumer<String>() {
            @Override
            public void accept(String modId) {
                ModOrganizer.ModInfo info = organizedMods.allMods.get(modId);
                if (info != null) {
                    ConfigManager.data.modHiderAllowedMods.remove(modId);
                    for (String dependentId : info.dependents) {
                        if (ConfigManager.data.modHiderAllowedMods.contains(dependentId)) {
                            this.accept(dependentId);
                        }
                    }
                }
            }
        };

        Consumer<String> disableUnusedDependencies = new Consumer<String>() {
            @Override
            public void accept(String modId) {
                ModOrganizer.ModInfo info = organizedMods.allMods.get(modId);
                if (info == null)
                    return;

                for (String depId : info.dependencies) {
                    if ("minecraft".equals(depId) || "java".equals(depId) || "fabricloader".equals(depId)) {
                        continue;
                    }

                    ModOrganizer.ModInfo depInfo = organizedMods.allMods.get(depId);
                    if (depInfo == null)
                        continue;

                    boolean stillNeeded = false;
                    for (String otherDependentId : depInfo.dependents) {
                        if (ConfigManager.data.modHiderAllowedMods.contains(otherDependentId)) {
                            stillNeeded = true;
                            break;
                        }
                    }

                    if (!stillNeeded && ConfigManager.data.modHiderAllowedMods.contains(depId)) {
                        ConfigManager.data.modHiderAllowedMods.remove(depId);
                        this.accept(depId);
                    }
                }
            }
        };

        if (organizedMods.minecraftGroup != null) {
            Label mcSectionLabel = new Label(0, 0, ChatFormatting.GOLD + "Minecraft", Label.Style.BODY);
            mcSectionLabel.setHeight(20);
            allowedModsList.addItem(mcSectionLabel);
            addModGroupToList(allowedModsList, organizedMods.minecraftGroup, query, enableDependencies,
                    disableDependents, disableUnusedDependencies,
                    modSearch);
        }

        if (organizedMods.userGroups.size() > 0) {
            Label userSectionLabel = new Label(0, 0, ChatFormatting.GOLD + "User Mods", Label.Style.BODY);
            userSectionLabel.setHeight(20);
            allowedModsList.addItem(userSectionLabel);
            for (ModOrganizer.ModGroup group : organizedMods.userGroups) {
                addModGroupToList(allowedModsList, group, query, enableDependencies, disableDependents,
                        disableUnusedDependencies, modSearch);
            }
        }

        if (organizedMods.libraryGroups.size() > 0) {
            Label libsSectionLabel = new Label(0, 0, ChatFormatting.GOLD + "Libraries", Label.Style.BODY);
            libsSectionLabel.setHeight(20);
            allowedModsList.addItem(libsSectionLabel);
            for (ModOrganizer.ModGroup group : organizedMods.libraryGroups) {
                addModGroupToList(allowedModsList, group, query, enableDependencies, disableDependents,
                        disableUnusedDependencies, modSearch);
            }
        }
    }

    private void addModGroupToList(ListView list, ModOrganizer.ModGroup group, String query,
            Consumer<String> enableDependencies,
            Consumer<String> disableDependents,
            Consumer<String> disableUnusedDependencies,
            TextField searchField) {
        List<ModOrganizer.ModInfo> matchingMods = new ArrayList<>();
        for (ModOrganizer.ModInfo info : group.mods) {
            boolean matches = true;
            for (String term : query.split(" ")) {
                if (term.isBlank())
                    continue;
                if (!info.name.toLowerCase(Locale.ROOT).contains(term) &&
                        !info.id.toLowerCase(Locale.ROOT).contains(term)) {
                    matches = false;
                    break;
                }
            }
            if (matches)
                matchingMods.add(info);
        }

        if (matchingMods.isEmpty())
            return;

        if (group.mods.size() > 1) {
            String groupKey = "mod_" + group.groupName;
            boolean isCollapsed = collapsedGroups.getOrDefault(groupKey, true);
            boolean allSelected = matchingMods.stream()
                    .allMatch(m -> ConfigManager.data.modHiderAllowedMods.contains(m.id));

            String arrow = isCollapsed ? "▶" : "▼";
            Button groupHeader = new Button(0, 0, list.getWidth() - 8,
                    arrow + " " + ChatFormatting.AQUA + group.groupName + " (" + matchingMods.size() + ")",
                    () -> {
                        collapsedGroups.put(groupKey, !collapsedGroups.getOrDefault(groupKey, true));
                        rebuildAllowedModsList(list, searchField);
                    });
            groupHeader.setHeight(20);
            list.addItem(groupHeader);

            if (!isCollapsed) {
                Checkbox selectAll = new Checkbox(10, 0, "Select All", allSelected, value -> {
                    for (ModOrganizer.ModInfo info : matchingMods) {
                        if (value) {
                            ConfigManager.data.modHiderAllowedMods.add(info.id);
                            enableDependencies.accept(info.id);
                        } else {
                            ConfigManager.data.modHiderAllowedMods.remove(info.id);
                            disableDependents.accept(info.id);
                            disableUnusedDependencies.accept(info.id);
                        }
                    }
                    ConfigManager.save();
                    rebuildAllowedModsList(list, searchField);
                });
                list.addItem(selectAll);

                for (ModOrganizer.ModInfo info : matchingMods) {
                    addModCheckboxToList(list, info, enableDependencies, disableDependents, disableUnusedDependencies,
                            searchField);
                }
            }
        } else {
            for (ModOrganizer.ModInfo info : matchingMods) {
                addModCheckboxToList(list, info, enableDependencies, disableDependents, disableUnusedDependencies,
                        searchField);
            }
        }
    }

    private void addModCheckboxToList(ListView list, ModOrganizer.ModInfo info,
            Consumer<String> enableDependencies,
            Consumer<String> disableDependents,
            Consumer<String> disableUnusedDependencies,
            TextField searchField) {
        boolean checked = ConfigManager.data.modHiderAllowedMods.contains(info.id);
        String displayName = info.name + " (" + info.id + ")";
        if (!info.dependents.isEmpty())
            displayName += " " + ChatFormatting.GRAY + "[Used by: " + info.dependents.size() + "]";

        Checkbox cb = new Checkbox(0, 0, displayName, checked, value -> {
            if (value) {
                ConfigManager.data.modHiderAllowedMods.add(info.id);
                enableDependencies.accept(info.id);
            } else {
                ConfigManager.data.modHiderAllowedMods.remove(info.id);
                disableDependents.accept(info.id);
                disableUnusedDependencies.accept(info.id);
            }
            ConfigManager.save();
            rebuildAllowedModsList(list, searchField);
        });

        Widget wrapper = new Widget(0, 0, 0, 0) {
            @Override
            public void render(GuiGraphics g, int mx, int my, float p) {
                cb.setX(getX());
                cb.setY(getY());
                cb.setWidth(getWidth());
                cb.render(g, mx, my, p);
                if (cb.isHovered() && !info.dependencies.isEmpty()) {
                    currentTooltip = "Dependencies: " + String.join(", ", info.dependencies);
                }
            }

            @Override
            public void updateHoverState(int mx, int my) {
                cb.setX(getX());
                cb.setY(getY());
                cb.setWidth(getWidth());
                cb.updateHoverState(mx, my);
                super.updateHoverState(mx, my);
                if (!cb.isHovered())
                    currentTooltip = null;
            }

            @Override
            public boolean mouseClicked(double mx, double my, int b) {
                return cb.mouseClicked(mx, my, b);
            }

            @Override
            public void tick() {
                cb.tick();
            }
        };
        wrapper.setHeight(cb.getHeight());
        list.addItem(wrapper);
    }

    @Override
    protected void renderScrolledContent(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        String title = "BlackAddons Control Panel";
        int titleWidth = font.width(title);
        graphics.drawString(font, title, containerX + (containerWidth - titleWidth) / 2, containerY + 15,
                Theme.TEXT_PRIMARY);
        currentTooltip = null;
    }

    @Override
    protected void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        String tooltip = currentTooltip;
        if (tooltip != null && !tooltip.isEmpty()) {
            int tooltipWidth = font.width(tooltip) + 8;
            int tooltipXPos = mouseX + 10;
            int tooltipYPos = mouseY - 20;
            if (tooltipXPos + tooltipWidth > width)
                tooltipXPos = mouseX - tooltipWidth - 10;
            if (tooltipYPos < 0)
                tooltipYPos = mouseY + 10;
            graphics.fill(tooltipXPos - 2, tooltipYPos - 2, tooltipXPos + tooltipWidth + 2, tooltipYPos + 10 + 2,
                    Theme.TOOLTIP_BG);
            graphics.fill(tooltipXPos - 2, tooltipYPos - 2, tooltipXPos + tooltipWidth + 2, tooltipYPos - 1,
                    Theme.ACCENT);
            graphics.fill(tooltipXPos - 2, tooltipYPos + 11, tooltipXPos + tooltipWidth + 2, tooltipYPos + 12,
                    Theme.ACCENT);
            graphics.fill(tooltipXPos - 2, tooltipYPos - 2, tooltipXPos - 1, tooltipYPos + 12, Theme.ACCENT);
            graphics.fill(tooltipXPos + tooltipWidth + 1, tooltipYPos - 2, tooltipXPos + tooltipWidth + 2,
                    tooltipYPos + 12, Theme.ACCENT);
            graphics.drawString(font, tooltip, tooltipXPos, tooltipYPos, Theme.TEXT_PRIMARY);
        }
    }

    @Override
    public void onClose() {
        if (ConfigManager.data.useCardLayout) {
            saveCardLayout();
        } else {
            ConfigManager.save();
        }
        super.onClose();
    }
}
