package org.blackum.blackaddons.gui.screen.tabs;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.blackum.blackaddons.gui.widget.Label;
import org.blackum.blackaddons.gui.widget.TabPanel;
import org.blackum.blackaddons.core.util.Constants;

import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import java.util.Optional;

public class AboutTabController extends SimpleTabController {
    public AboutTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab aboutTab) {
        int contentX = aboutTab.getParent().getContentX();
        int contentY = aboutTab.getParent().getContentY();

        aboutTab.addWidget(new Label(contentX, contentY, "BlackAddons", Label.Style.TITLE));

        String version = "Unknown";
        try {
            Optional<ModContainer> mod = FabricLoader
                    .getInstance().getModContainer(Constants.MOD_ID);
            if (mod.isPresent()) {
                version = mod.get().getMetadata().getVersion().getFriendlyString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        aboutTab.addWidget(new Label(contentX, contentY + 30, "Version: " + version, Label.Style.BODY));
        aboutTab.addWidget(new Label(contentX, contentY + 50, "Created by Blackum", Label.Style.BODY));
        aboutTab.addWidget(new Label(contentX, contentY + 70, "Contributors: AtkLxve, Autismo, NotYukimii", Label.Style.BODY));
    }
}
