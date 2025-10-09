package com.gimsieder.skillingoutfit;

import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SkillingOutfitWindow extends JFrame
{
    private final SkillingOutfitConfig config;
    private final SkillingOutfitPanel panel;
    private final ConfigManager configManager;
    private final EventBus eventBus;
    private final ClientThread clientThread;

    public SkillingOutfitWindow(SkillingOutfitConfig config, SkillingOutfitPanel panel,
                                ConfigManager configManager, EventBus eventBus, ClientThread clientThread)
    {
        super("Skilling Outfit Tracker - Config");
        this.config = config;
        this.panel = panel;
        this.configManager = configManager;
        this.eventBus = eventBus;
        this.clientThread = clientThread;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(420, 720);
        setLocationRelativeTo(null);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(sectionLabel("General Display Settings"));

        addCheckBox(content, "Notify on new item", "notifyOnNew", config.notifyOnNew());
        addSpinner(content, "Panel Title Spacer", "panelTitleSpacer", config.panelTitleSpacer(), 0, 100, 1);
        addCheckBox(content, "Display Collected Outfits", "displayCollectedOutfits", config.displayCollectedOutfits());
        addSpinner(content, "Outfit Text Spacer", "outfitTextSpacer", config.outfitTextSpacer(), 0, 50, 1);
        addCheckBox(content, "Display Collected Items", "displayCollectedItems", config.displayCollectedItems());
        addSpinner(content, "Item Text Spacer", "itemTextSpacer", config.itemTextSpacer(), 0, 50, 1);
        addCheckBox(content, "Color Text for Collected", "colorTextForCollected", config.colorTextForCollected());
        addSpinner(content, "First Outfit Spacer", "firstOutfitSpacer", config.firstOutfitSpacer(), 0, 50, 1);
        addSpinner(content, "Total Needed Text Spacer", "totalNeededTextSpacer", config.totalNeededTextSpacer(), 0, 50, 1);
        addSpinner(content, "Icon Text Spacer", "iconTextSpacer", config.iconTextSpacer(), 0, 50, 1);
        addSpinner(content, "Icon Size", "iconSize", config.iconSize(), 16, 128, 1);
        addSpinner(content, "Icon Gap Spacing", "iconGapSpacing", config.iconGapSpacing(), 0, 50, 1);
        addSpinner(content, "Max Columns", "maxCols", config.maxCols(), 1, 10, 1);

        addCheckBox(content, "Show Obtained Items", "showObtainedItems", config.showObtainedItems());
        addCheckBox(content, "Override Outfit Colors", "overrideOutfitColors", config.overrideOutfitColors());

        JButton colorButton = new JButton("Pick Outfit Name Color");
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Outfit Name Color", config.outfitNameColor());
            if (newColor != null)
                setConfigValue("outfitNameColor", newColor.getRGB());
        });
        content.add(colorButton);

        addCheckBox(content, "Enable Popout Config Mode", "enablePopoutConfigMode", config.enablePopoutConfigMode());

        content.add(Box.createVerticalStrut(20));
        content.add(sectionLabel("Outfit Sets"));

        Map<String, Boolean> outfits = new LinkedHashMap<>();
        outfits.put("displayAgility", config.displayAgility());
        outfits.put("displayConstruction", config.displayConstruction());
        outfits.put("displayFarming", config.displayFarming());
        outfits.put("displayFiremaking", config.displayFiremaking());
        outfits.put("displayFishing", config.displayFishing());
        outfits.put("displayHunter", config.displayHunter());
        outfits.put("displayMining", config.displayMining());
        outfits.put("displayPrayer", config.displayPrayer());
        outfits.put("displayRunecraft", config.displayRunecraft());
        outfits.put("displaySmithing", config.displaySmithing());
        outfits.put("displayThieving", config.displayThieving());
        outfits.put("displayWoodcutting", config.displayWoodcutting());

        outfits.forEach((key, value) -> {
            String label = key.replace("display", "").replaceAll("([A-Z])", " $1").trim();
            addCheckBox(content, "Display " + label + " Outfit", key, value);
        });

        content.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }

    private void addCheckBox(JPanel panel, String label, String key, boolean initial)
    {
        JCheckBox box = new JCheckBox(label, initial);
        box.addActionListener(e -> setConfigValue(key, box.isSelected()));
        panel.add(box);
    }

    private void addSpinner(JPanel panel, String label, String key, int value, int min, int max, int step)
    {
        JLabel l = new JLabel(label);
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        spinner.addChangeListener(e -> setConfigValue(key, spinner.getValue()));
        panel.add(l);
        panel.add(spinner);
    }

    private JLabel sectionLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        label.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        return label;
    }

    private void setConfigValue(String key, Object value)
    {
        configManager.setConfiguration("skillingoutfit", key, value);
        if (panel != null)
            panel.refresh();

        ConfigChanged changed = new ConfigChanged();
        changed.setGroup("skillingoutfit");
        changed.setKey(key);
        changed.setNewValue(value != null ? value.toString() : null);
        eventBus.post(changed);
    }

    private boolean getBooleanConfigValue(String key)
    {
        Object val = configManager.getConfiguration("skillingoutfit", key);
        return val == null || Boolean.parseBoolean(val.toString());
    }

}
