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
    private final Map<String, Widget> modCheckboxWrappers = new HashMap<>();
    private final Map<String, Checkbox> modCheckboxCache = new HashMap<>();
    private ModOrganizer.OrganizedMods cachedOrganizedMods = null;

    private void updateCheckboxVisuals() {
        for (Map.Entry<String, Checkbox> entry : modCheckboxCache.entrySet()) {
            entry.getValue().setChecked(ConfigManager.data.modHiderAllowedMods.contains(entry.getKey()));
        }
    }

    private SettingsTabController settingsController;
    private ModHiderTabController modHiderController;
    private PayloadsTabController payloadsController;
    private CheatsTabController cheatsController;
    private LegitTabController legitController;
    private AboutTabController aboutController;
    private ChatActionsTabController chatActionsController;
    private WaypointsTabController waypointsController;
    private ConfigsTabController configsController;
    private final List<SimpleTabController> controllers = new ArrayList<>();

    public BlackAddonsGUI() {
        this(null);
    }

    public BlackAddonsGUI(Screen parent) {
        super(Component.literal("BlackAddons Settings"), parent);
    }

    @Override
    protected void initWidgets() {
        tabPanel = new TabPanel(containerX + Theme.PADDING, containerY + 40, containerWidth - (Theme.PADDING * 2),
                containerHeight - 50);
        
        modCheckboxCache.clear();
        modCheckboxWrappers.clear();
        cachedOrganizedMods = null;

        tabPanel.setOnTabChange(index -> {
            lastTabIndex = index;
            if (index >= 0 && index < controllers.size()) {
                controllers.get(index).onSelected();
            }
        });

        settingsController = new SettingsTabController(this);
        modHiderController = new ModHiderTabController(this);
        payloadsController = new PayloadsTabController(this);
        cheatsController = new CheatsTabController(this);
        legitController = new LegitTabController(this);
        aboutController = new AboutTabController(this);
        chatActionsController = new ChatActionsTabController(this);
        waypointsController = new WaypointsTabController(this);
        configsController = new ConfigsTabController(this);

        controllers.clear();
        controllers.add(settingsController);
        controllers.add(modHiderController);
        controllers.add(payloadsController);
        controllers.add(cheatsController);
        controllers.add(legitController);
        controllers.add(chatActionsController);
        controllers.add(waypointsController);
        controllers.add(configsController);
        controllers.add(aboutController);

        settingsController.init(tabPanel.addTab("Settings"));
        modHiderController.init(tabPanel.addTab("Mod Hider"));
        payloadsController.init(tabPanel.addTab("Payloads"));
        cheatsController.init(tabPanel.addTab("Cheats"));
        legitController.init(tabPanel.addTab("Legit"));
        chatActionsController.init(tabPanel.addTab("Chat Actions"));
        waypointsController.init(tabPanel.addTab("Waypoints Actions"));
        configsController.init(tabPanel.addTab("Configs"));
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
        if (lastTabIndex >= 0 && lastTabIndex < controllers.size()) {
            controllers.get(lastTabIndex).tick();
        }
    }

    public void rebuildChannelsList(ListView channelsList) {
        channelsList.clearItems();
        List<String> channels = new ArrayList<>(ConfigManager.data.modHiderAllowedCustomPayloadChannels);
        channels.sort(String::compareToIgnoreCase);
        for (String ch : channels) {
            Button remove = new Button(0, 0, channelsList.getWidth() - Theme.PADDING_SMALL, "Remove: " + ch, () -> {
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

        if (cachedOrganizedMods == null) {
            cachedOrganizedMods = ModOrganizer.organizeMods();
        }
        ModOrganizer.OrganizedMods organizedMods = cachedOrganizedMods;

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
                Checkbox selectAll = new Checkbox(Theme.PADDING, 0, "Select All", allSelected, value -> {
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
                    updateCheckboxVisuals();
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

        Checkbox cb = modCheckboxCache.get(info.id);
        Widget wrapper = modCheckboxWrappers.get(info.id);

        if (cb == null || wrapper == null) {
            String displayName = info.name + " (" + info.id + ")";
            if (!info.dependents.isEmpty())
                displayName += " " + ChatFormatting.GRAY + "[Used by: " + info.dependents.size() + "]";

            boolean checked = ConfigManager.data.modHiderAllowedMods.contains(info.id);

            cb = new Checkbox(0, 0, displayName, checked, value -> {
                if (value) {
                    ConfigManager.data.modHiderAllowedMods.add(info.id);
                    enableDependencies.accept(info.id);
                } else {
                    ConfigManager.data.modHiderAllowedMods.remove(info.id);
                    disableDependents.accept(info.id);
                    disableUnusedDependencies.accept(info.id);
                }
                ConfigManager.save();
                updateCheckboxVisuals();
            });
            modCheckboxCache.put(info.id, cb);

            final Checkbox finalCb = cb;
            wrapper = new Widget(0, 0, 0, 0) {
                @Override
                public void render(GuiGraphics g, int mx, int my, float p) {
                    finalCb.setX(getX());
                    finalCb.setY(getY());
                    finalCb.setWidth(getWidth());
                    finalCb.render(g, mx, my, p);
                    if (finalCb.isHovered() && !info.dependencies.isEmpty()) {
                        currentTooltip = "Dependencies: " + String.join(", ", info.dependencies);
                    }
                }

                @Override
                public void updateHoverState(int mx, int my) {
                    finalCb.setX(getX());
                    finalCb.setY(getY());
                    finalCb.setWidth(getWidth());
                    finalCb.updateHoverState(mx, my);
                    super.updateHoverState(mx, my);
                    if (!finalCb.isHovered())
                        currentTooltip = null;
                }

                @Override
                public boolean mouseClicked(double mx, double my, int b) {
                    return finalCb.mouseClicked(mx, my, b);
                }

                @Override
                public void tick() {
                    finalCb.tick();
                }
            };
            wrapper.setHeight(cb.getHeight());
            modCheckboxWrappers.put(info.id, wrapper);
        }

        cb.setChecked(ConfigManager.data.modHiderAllowedMods.contains(info.id));
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
            int tooltipWidth = font.width(tooltip) + Theme.PADDING_SMALL;
            int tooltipXPos = mouseX + Theme.PADDING;
            int tooltipYPos = mouseY - Theme.SPACING_NORMAL;
            if (tooltipXPos + tooltipWidth > width)
                tooltipXPos = mouseX - tooltipWidth - Theme.PADDING;
            if (tooltipYPos < 0)
                tooltipYPos = mouseY + Theme.PADDING;
            graphics.fill(tooltipXPos - 2, tooltipYPos - 2, tooltipXPos + tooltipWidth + 2,
                    tooltipYPos + Theme.PADDING + 2,
                    Theme.TOOLTIP_BG);
            graphics.fill(tooltipXPos - 2, tooltipYPos - 2, tooltipXPos + tooltipWidth + 2, tooltipYPos - 1,
                    Theme.ACCENT);
            graphics.fill(tooltipXPos - 2, tooltipYPos + Theme.PADDING + 1, tooltipXPos + tooltipWidth + 2,
                    tooltipYPos + Theme.PADDING + 2,
                    Theme.ACCENT);
            graphics.fill(tooltipXPos - 2, tooltipYPos - 2, tooltipXPos - 1, tooltipYPos + Theme.PADDING + 2,
                    Theme.ACCENT);
            graphics.fill(tooltipXPos + tooltipWidth + 1, tooltipYPos - 2, tooltipXPos + tooltipWidth + 2,
                    tooltipYPos + Theme.PADDING + 2, Theme.ACCENT);
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
