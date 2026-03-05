package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.widget.TabPanel;

public abstract class SimpleTabController {
    protected final BlackAddonsGUI screen;

    public SimpleTabController(BlackAddonsGUI screen) {
        this.screen = screen;
    }

    public abstract void init(TabPanel.Tab tab);

    public void tick() {
    }

    public void onSelected() {
    }
}
