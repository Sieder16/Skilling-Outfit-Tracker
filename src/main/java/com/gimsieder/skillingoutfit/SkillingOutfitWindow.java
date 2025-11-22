
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

	private static final int FIXED_WIDTH = 440;

	public SkillingOutfitWindow(
			SkillingOutfitConfig config,
			SkillingOutfitPanel panel,
			ConfigManager configManager,
			EventBus eventBus,
			ClientThread clientThread
	)
	{
		super("Skilling Outfit Tracker – Config");
		this.config = config;
		this.panel = panel;
		this.configManager = configManager;
		this.eventBus = eventBus;

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(500, 700);
		setLocationRelativeTo(null);

		JPanel main = createVerticalPanel();
		leftAlign(main);

		/* ======================================================
		 *  GENERAL SETTINGS
		 * ====================================================== */
		addHeader(main, "General Settings");

		addCheckbox(main, "Display Collected Outfits", "displayCollectedOutfits", config.displayCollectedOutfits());
		addCheckbox(main, "Display Collected Items", "displayCollectedItems", config.displayCollectedItems());
		addCheckbox(main, "Color Text For Collected", "colorTextForCollected", config.colorTextForCollected());

		addCheckbox(main, "Show Price To Obtain (Outfit)", "showTotalObtain", config.showTotalObtain());
		addCheckbox(main, "Show Price To Obtain (Items)", "showItemTotalObtain", config.showItemTotalObtain());
		addCheckbox(main, "Show Obtained Items", "showObtainedItems", config.showObtainedItems());

		addCheckbox(main, "Enable Popout Config Mode", "enablePopoutConfigMode", config.enablePopoutConfigMode());

		JLabel barkLabel = new JLabel("Anima-Infused Bark");
		leftAlign(barkLabel);
		main.add(barkLabel);
		main.add(space());

		// Spinner
		JPanel barkRow = new JPanel();
		barkRow.setLayout(new BoxLayout(barkRow, BoxLayout.X_AXIS));
		barkRow.setAlignmentX(Component.LEFT_ALIGNMENT);

		// the spinner
		JSpinner bark = new JSpinner(new SpinnerNumberModel(config.animaBark(), 0, 999999, 1));
		bark.setPreferredSize(new Dimension(80, 25));
		bark.setMaximumSize(new Dimension(80, 25));
		bark.setMinimumSize(new Dimension(80, 25));
		leftAlign(bark);

		bark.addChangeListener(e -> setConfigValue("animaBark", bark.getValue()));

		barkRow.add(bark);
		main.add(barkRow);
		main.add(space());


		/* ======================================================
		 *  SPACING SETTINGS
		 * ====================================================== */
		addHeader(main, "Spacing Settings");

		addSpinner(main, "Panel Title Spacer", "panelTitleSpacer", config.panelTitleSpacer(), 0, 200);
		addSpinner(main, "Outfit Text Spacer", "outfitTextSpacer", config.outfitTextSpacer(), 0, 100);
		addSpinner(main, "Item Text Spacer", "itemTextSpacer", config.itemTextSpacer(), 0, 100);
		addSpinner(main, "First Outfit Spacer", "firstOutfitSpacer", config.firstOutfitSpacer(), 0, 100);
		addSpinner(main, "Total Needed Text Spacer", "totalNeededTextSpacer", config.totalNeededTextSpacer(), 0, 100);
		addSpinner(main, "Icon Text Spacer", "iconTextSpacer", config.iconTextSpacer(), 0, 100);

		/* ======================================================
		 *  ICON SETTINGS
		 * ====================================================== */
		addHeader(main, "Icon Settings");

		addSpinner(main, "Icon Size", "iconSize", config.iconSize(), 16, 128);
		addSpinner(main, "Icon Gap Spacing", "iconGapSpacing", config.iconGapSpacing(), 0, 100);
		addSpinner(main, "Max Columns", "maxCols", config.maxCols(), 1, 12);

		/* ======================================================
		 *  COLOR OPTIONS
		 * ====================================================== */
		addHeader(main, "Color Options");

		addCheckbox(main, "Override Outfit Colors", "overrideOutfitColors", config.overrideOutfitColors());

		JButton pick = new JButton("Pick Outfit Name Color");
		leftAlign(pick);
		pick.setMaximumSize(new Dimension(180, 28));
		pick.setPreferredSize(new Dimension(180, 28));
		pick.setMinimumSize(new Dimension(120, 28));
		pick.addActionListener(e ->
		{
			Color newColor = JColorChooser.showDialog(this, "Choose Outfit Name Color", config.outfitNameColor());
			if (newColor != null)
				setConfigValue("outfitNameColor", newColor.getRGB());
		});
		main.add(pick);
		main.add(space());

		/* ======================================================
		 *  OUTFIT SETS
		 * ====================================================== */
		addHeader(main, "Outfit Sets");

		Map<String, Boolean> outfits = new LinkedHashMap<>();
		outfits.put("Agility", config.displayAgility());
		outfits.put("Construction", config.displayConstruction());
		outfits.put("Farming", config.displayFarming());
		outfits.put("Firemaking", config.displayFiremaking());
		outfits.put("Fishing", config.displayFishing());
		outfits.put("Hunter", config.displayHunter());
		outfits.put("Mining", config.displayMining());
		outfits.put("Prayer", config.displayPrayer());
		outfits.put("Runecraft", config.displayRunecraft());
		outfits.put("Smithing", config.displaySmithing());
		outfits.put("Thieving", config.displayThieving());
		outfits.put("Woodcutting", config.displayWoodcutting());

		for (Map.Entry<String, Boolean> e : outfits.entrySet())
			addCheckbox(main, "Display " + e.getKey() + " Outfit", "display" + e.getKey(), e.getValue());

		/* ======================================================
		 *  FIX COLLAPSING / LEFT-ALIGN WINDOW
		 * ====================================================== */
		main.setPreferredSize(new Dimension(FIXED_WIDTH, main.getPreferredSize().height));
		main.setMinimumSize(new Dimension(FIXED_WIDTH, 0));
		main.setMaximumSize(new Dimension(FIXED_WIDTH, Integer.MAX_VALUE));

		JScrollPane scroll = new JScrollPane(main);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(15);

		add(scroll);
		setVisible(true);
	}

	/* ======================================================
	 *  HELPERS
	 * ====================================================== */

	private JPanel createVerticalPanel()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		return p;
	}

	private void addHeader(JPanel p, String text)
	{
		JLabel header = new JLabel("  === " + text + " ===");
		header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
		fixComponent(header);
		leftAlign(header);
		p.add(header);
		p.add(space());
	}

	private void addCheckbox(JPanel p, String label, String key, boolean initial)
	{
		JCheckBox box = new JCheckBox(label, initial);
		fixComponent(box);
		leftAlign(box);
		box.addActionListener(e -> setConfigValue(key, box.isSelected()));
		p.add(box);
		p.add(space());
	}

	private void addSpinner(JPanel p, String label, String key, int initial, int min, int max)
	{
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(FIXED_WIDTH, 30));
		row.setMinimumSize(new Dimension(FIXED_WIDTH, 30));
		row.setPreferredSize(new Dimension(FIXED_WIDTH, 30));

		JLabel l = new JLabel(label + ": ");
		l.setAlignmentX(Component.LEFT_ALIGNMENT);

		JSpinner spinner = new JSpinner(new SpinnerNumberModel(initial, min, max, 1));
		spinner.setMaximumSize(new Dimension(80, 25));  // ❗ small spinner width
		spinner.setPreferredSize(new Dimension(80, 25));
		spinner.setMinimumSize(new Dimension(80, 25));
		spinner.setAlignmentX(Component.LEFT_ALIGNMENT);

		spinner.addChangeListener(e -> setConfigValue(key, spinner.getValue()));

		row.add(l);
		row.add(Box.createHorizontalStrut(10)); // nice spacing
		row.add(spinner);

		p.add(row);
		p.add(space());
	}

	private void fixComponent(Component c)
	{
		Dimension pref = c.getPreferredSize();
		Dimension fixed = new Dimension(FIXED_WIDTH, pref.height);

		c.setPreferredSize(fixed);
		c.setMaximumSize(fixed);
		c.setMinimumSize(new Dimension(FIXED_WIDTH, pref.height));
	}

	private void leftAlign(Component c)
	{
		if (c instanceof JComponent)
			((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
	}

	private Component space()
	{
		Component c = Box.createRigidArea(new Dimension(0, 10));
		fixComponent(c);
		leftAlign(c);
		return c;
	}

	private void setConfigValue(String key, Object value)
	{
		configManager.setConfiguration("skillingoutfit", key, value);

		if (panel != null)
			panel.refresh();

		ConfigChanged evt = new ConfigChanged();
		evt.setGroup("skillingoutfit");
		evt.setKey(key);
		evt.setNewValue(value != null ? value.toString() : null);
		eventBus.post(evt);
	}
}
