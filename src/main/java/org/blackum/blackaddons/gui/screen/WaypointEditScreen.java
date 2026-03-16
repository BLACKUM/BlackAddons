package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.waypoint.Waypoint;
import org.blackum.blackaddons.core.waypoint.WaypointManager;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.widget.*;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

public class WaypointEditScreen extends BaseScreen {
    private final Waypoint waypoint;
    private final Consumer<Waypoint> onSave;
    
    private TextField nameField;
    private TextField xField;
    private TextField yField;
    private TextField zField;

    public WaypointEditScreen(Screen parent, Waypoint waypoint, Consumer<Waypoint> onSave) {
        super(Component.literal(waypoint.name == null ? "Add Waypoint" : "Edit Waypoint"), parent);
        this.waypoint = waypoint;
        this.onSave = onSave;
    }

    @Override
    protected int getContentHeight() {
        return 0;
    }

    @Override
    protected void initWidgets() {
        if (waypoint.animation == null) waypoint.animation = org.blackum.blackaddons.core.waypoint.WaypointAnimation.STATIC;
        if (waypoint.actions == null) waypoint.actions = new java.util.ArrayList<>();
        if (waypoint.id == null) waypoint.id = java.util.UUID.randomUUID();

        int listWidth = containerWidth - Theme.PADDING * 2;
        ListView list = new ListView(containerX + Theme.PADDING, containerY + 40, listWidth, containerHeight - 50);
        int itemWidth = list.getWidth() - 16;

        list.addItem(new Label(0, 0, "Name", Label.Style.CAPTION));
        nameField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Name");
        nameField.setText(waypoint.name != null ? waypoint.name : "");
        list.addItem(nameField);
        list.addItem(new Widget(0, 0, itemWidth, 5) { @Override public void render(GuiGraphics g, int mx, int my, float pt) {} });

        list.addItem(new Label(0, 0, "Coordinates", Label.Style.CAPTION));
        xField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "X");
        xField.setText(String.format(java.util.Locale.ROOT, "%.2f", waypoint.x));
        list.addItem(xField);

        yField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Y");
        yField.setText(String.format(java.util.Locale.ROOT, "%.2f", waypoint.y));
        list.addItem(yField);

        zField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Z");
        zField.setText(String.format(java.util.Locale.ROOT, "%.2f", waypoint.z));
        list.addItem(zField);

        Button lookBtn = new Button(0, 0, itemWidth, 20, "Looking at Position", () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                net.minecraft.world.phys.HitResult hit = mc.player.pick(50.0, 0.0f, false);
                if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    net.minecraft.core.BlockPos pos = ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos();
                    xField.setText(String.format(java.util.Locale.ROOT, "%.2f", (double)pos.getX()));
                    yField.setText(String.format(java.util.Locale.ROOT, "%.2f", (double)pos.getY()));
                    zField.setText(String.format(java.util.Locale.ROOT, "%.2f", (double)pos.getZ()));
                } else {
                    xField.setText(String.format(java.util.Locale.ROOT, "%.2f", mc.player.getX()));
                    yField.setText(String.format(java.util.Locale.ROOT, "%.2f", mc.player.getY()));
                    zField.setText(String.format(java.util.Locale.ROOT, "%.2f", mc.player.getZ()));
                }
            }
        });
        list.addItem(lookBtn);

        Button currentPosBtn = new Button(0, 0, itemWidth, 20, "Current Position", () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                xField.setText(String.format(java.util.Locale.ROOT, "%.2f", mc.player.getX()));
                yField.setText(String.format(java.util.Locale.ROOT, "%.2f", mc.player.getY()));
                zField.setText(String.format(java.util.Locale.ROOT, "%.2f", mc.player.getZ()));
            }
        });
        list.addItem(currentPosBtn);
        list.addItem(new Widget(0, 0, itemWidth, 5) { @Override public void render(GuiGraphics g, int mx, int my, float pt) {} });

        list.addItem(new Label(0, 0, "Animation style", Label.Style.CAPTION));
        org.blackum.blackaddons.core.waypoint.WaypointAnimation[] anims = org.blackum.blackaddons.core.waypoint.WaypointAnimation.values();
        java.util.List<String> animOptions = java.util.Arrays.stream(anims)
            .map(Object::toString)
            .collect(java.util.stream.Collectors.toList());

        Dropdown animDropdown = new Dropdown(0, 0, itemWidth, 20, "Select Animation", animOptions, 
            val -> {
                for (org.blackum.blackaddons.core.waypoint.WaypointAnimation a : anims) {
                    if (a.toString().equals(val)) {
                        waypoint.animation = a;
                        break;
                    }
                }
            });
        animDropdown.setSelectedOption(waypoint.animation.toString());
        list.addItem(animDropdown);
        list.addItem(new Widget(0, 0, itemWidth, 10) { @Override public void render(GuiGraphics g, int mx, int my, float pt) {} });

        list.addItem(new Label(0, 0, "Color Selection", Label.Style.CAPTION));
        int pickerX = (itemWidth - ColorPicker.WIDTH) / 2;
        ColorPicker picker = new ColorPicker(pickerX, 0, waypoint.color, color -> waypoint.color = color);
        list.addItem(picker);
        list.addItem(new Widget(0, 0, itemWidth, 10) { @Override public void render(GuiGraphics g, int mx, int my, float pt) {} });

        list.addItem(new Label(0, 0, "Render Style", Label.Style.CAPTION));
        Checkbox showCylinderCheckbox = new Checkbox(0, 0, "Show Cylinder", waypoint.showCylinder, val -> waypoint.showCylinder = val);
        list.addItem(showCylinderCheckbox);
        list.addItem(new Widget(0, 0, itemWidth, 10) { @Override public void render(GuiGraphics g, int mx, int my, float pt) {} });

        list.addItem(new Label(0, 0, "Radius", Label.Style.CAPTION));
        GridRow radiusRow = new GridRow(itemWidth, 20);
        TextField manualRadius = new TextField(0, 0, 50, 14, "Radius");
        manualRadius.setText(String.format(java.util.Locale.ROOT, "%.2f", waypoint.radius));
        Slider radiusSlider = new Slider(0, 0, itemWidth - 60, 0.01f, 10.00f, (float)waypoint.radius, val -> {
            waypoint.radius = val;
            manualRadius.setText(String.format(java.util.Locale.ROOT, "%.2f", val));
        });
        radiusRow.addChild(radiusSlider, 0);
        radiusRow.addChild(manualRadius, itemWidth - 55);
        list.addItem(radiusRow);

        manualRadius.setOnValueChange(text -> {
            try {
                float val = Float.parseFloat(text);
                if (val >= 0.01f && val <= 10.00f) {
                    waypoint.radius = val;
                    radiusSlider.setValue(val);
                }
            } catch (NumberFormatException ignored) {}
        });
        list.addItem(new Widget(0, 0, itemWidth, 10) { @Override public void render(GuiGraphics g, int mx, int my, float pt) {} });

        list.addItem(new Label(0, 0, "Height", Label.Style.CAPTION));
        GridRow heightRow = new GridRow(itemWidth, 20);
        TextField manualHeight = new TextField(0, 0, 50, 14, "Height");
        manualHeight.setText(String.format(java.util.Locale.ROOT, "%.2f", waypoint.height));
        Slider heightSlider = new Slider(0, 0, itemWidth - 60, 0.01f, 10.00f, (float)waypoint.height, val -> {
            waypoint.height = val;
            manualHeight.setText(String.format(java.util.Locale.ROOT, "%.2f", val));
        });
        heightRow.addChild(heightSlider, 0);
        heightRow.addChild(manualHeight, itemWidth - 55);
        list.addItem(heightRow);

        manualHeight.setOnValueChange(text -> {
            try {
                float val = Float.parseFloat(text);
                if (val >= 0.01f && val <= 10.00f) {
                    waypoint.height = val;
                    heightSlider.setValue(val);
                }
            } catch (NumberFormatException ignored) {}
        });
        list.addItem(new Widget(0, 0, itemWidth, 15) { @Override public void render(GuiGraphics g, int mx, int my, float pt) {} });

        GridRow btnRow = new GridRow(itemWidth, 20);
        Button saveBtn = new Button(0, 0, (itemWidth - Theme.PADDING) / 2, 20, "Save", () -> {
            waypoint.name = nameField.getText();
            try {
                waypoint.x = Double.parseDouble(xField.getText().replace(",", "."));
                waypoint.y = Double.parseDouble(yField.getText().replace(",", "."));
                waypoint.z = Double.parseDouble(zField.getText().replace(",", "."));
                
                try {
                    waypoint.radius = Double.parseDouble(manualRadius.getText().replace(",", "."));
                } catch (NumberFormatException ignored) {}
                
                try {
                    waypoint.height = Double.parseDouble(manualHeight.getText().replace(",", "."));
                } catch (NumberFormatException ignored) {}
                
                waypoint.showCylinder = showCylinderCheckbox.isChecked();

                if (waypoint.dimension == null && Minecraft.getInstance().level != null) {
                    waypoint.dimension = Minecraft.getInstance().level.dimension().location().toString();
                }
                onSave.accept(waypoint);
                minecraft.setScreen(parent);
            } catch (NumberFormatException ignored) {}
        });
        Button cancelBtn = new Button(0, 0, (itemWidth - Theme.PADDING) / 2, 20, "Cancel", () -> minecraft.setScreen(parent));
        btnRow.addChild(saveBtn, 0);
        btnRow.addChild(cancelBtn, (itemWidth + Theme.PADDING) / 2);
        list.addItem(btnRow);

        widgets.add(list);
    }

    @Override
    protected void renderScrolledContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderHelper.drawCenteredString(graphics, font, getTitle().getString(), containerX + containerWidth / 2, containerY + 20, Theme.TEXT_PRIMARY);
    }
}
