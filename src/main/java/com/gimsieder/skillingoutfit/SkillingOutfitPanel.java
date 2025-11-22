package com.gimsieder.skillingoutfit;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.api.ChatMessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class SkillingOutfitPanel extends PluginPanel
{
	private final Client client;
	private final ItemManager itemManager;
	private final SkillingOutfitTracker tracker;
	private final SkillingOutfitConfig config;
	private final ConfigManager configManager;
	private final SkillingOutfitPlugin plugin;
	private final ClientThread clientThread;

	private final Map<Integer, BufferedImage> itemSprites = new HashMap<>();
	private final Map<Integer, Rectangle> iconBounds = new HashMap<>();
	private final Map<String, Rectangle> outfitBounds = new HashMap<>();
	private final Map<String, BooleanSupplier> outfitDisplayMap = new HashMap<>();
	private final Map<String, Integer> remainingCounts = new HashMap<>();
	private final Map<Integer, SkillingOutfitItem> hoverItemMap = new HashMap<>();


	private final Map<Integer, Integer> inventoryCacheSnapshot = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> equipmentCacheSnapshot = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> bankCacheSnapshot = new ConcurrentHashMap<>();

	private JPanel innerPanel;
	private int hoveredItemId = -1;
	private String hoveredOutfitName = null;

	private static final Map<String, Color> SKILL_COLORS = Map.ofEntries(
			Map.entry("Agility", new Color(0x33FF33)),
			Map.entry("Construction", new Color(0xCC9966)),
			Map.entry("Farming", new Color(0x33CC33)),
			Map.entry("Firemaking", new Color(0xFF3300)),
			Map.entry("Fishing", new Color(0x3399FF)),
			Map.entry("Hunter", new Color(0x66CC00)),
			Map.entry("Mining", new Color(0x999999)),
			Map.entry("Prayer", new Color(0xFFFFFF)),
			Map.entry("Runecraft", new Color(0x996633)),
			Map.entry("Smithing", new Color(0xCCCCCC)),
			Map.entry("Thieving", new Color(0xFFFF00)),
			Map.entry("Woodcutting", new Color(0x996633)),
			Map.entry("Sailing", new Color(0x1EB4C7))
	);

	public SkillingOutfitPanel(Client client, ItemManager itemManager, SkillingOutfitTracker tracker,
							   SkillingOutfitConfig config, ConfigManager configManager,
							   SkillingOutfitPlugin plugin, ClientThread clientThread)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.tracker = tracker;
		this.config = config;
		this.configManager = configManager;
		this.plugin = plugin;
		this.clientThread = clientThread;

		setBackground(new Color(40, 40, 40));
		setLayout(new BorderLayout());

		innerPanel = new JPanel() {
			@Override
			public Dimension getPreferredSize() {
				return calculatePreferredSize();
			}

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				paintItems((Graphics2D) g);
			}
		};
		innerPanel.setBackground(new Color(40, 40, 40));

		preloadSprites();
		setupOutfitDisplayMap();

		innerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				hoveredItemId = getItemAt(e.getX(), e.getY());
				hoveredOutfitName = getOutfitAt(e.getX(), e.getY());
				innerPanel.repaint();
			}
		});

		innerPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				int itemId = getItemAt(e.getX(), e.getY());
				if (itemId != -1)
				{
					// Open item wiki
					openWikiLink("https://oldschool.runescape.wiki/Special:Lookup?utm_source=wiki&type=item&id=" + itemId);
					return;
				}

				String outfitName = getOutfitAt(e.getX(), e.getY());
				if (outfitName != null)
				{
					SkillingOutfitData.SkillingOutfitDataEntry entry = SkillingOutfitData.OUTFITS_DATA.get(outfitName);
					if (entry != null && entry.wikiUrl != null && !entry.wikiUrl.isEmpty())
					{
						openWikiLink(entry.wikiUrl);
					}
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(innerPanel);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		add(scrollPane, BorderLayout.CENTER);

	}

	private void preloadSprites()
	{
		for (SkillingOutfitData.SkillingOutfitDataEntry entry : SkillingOutfitData.OUTFITS_DATA.values())
			for (int itemId : entry.items.keySet())
				itemSprites.put(itemId, itemManager.getImage(itemId, 1, false));
	}

	public void setupOutfitDisplayMap()
	{
		for (var entry : SkillingOutfitData.OUTFITS_DATA.entrySet())
		{
			String outfitName = entry.getKey();
			String configKey = entry.getValue().configKey;
			outfitDisplayMap.put(outfitName, () -> {
				Object result = configManager.getConfiguration("skillingoutfit", configKey);
				return result != null && Boolean.parseBoolean(result.toString());
			});
		}
	}

	private void paintItems(Graphics2D g)
	{
		iconBounds.clear();
		hoverItemMap.clear();
		outfitBounds.clear();

		int panelWidth = getWidth();
		int yOffset = config.panelTitleSpacer();
		FontMetrics fm = g.getFontMetrics();

		// Title
		g.setColor(Color.WHITE);
		String title = "Skilling Outfit Tracker";
		g.drawString(title, (panelWidth - fm.stringWidth(title)) / 2, yOffset + fm.getAscent());
		yOffset += fm.getHeight();

		// Display outfit and item collected counts
		yOffset = paintCollectedCounts(g, panelWidth, yOffset, fm);

		Player localPlayer = client.getLocalPlayer();
		boolean isFemale = localPlayer != null && localPlayer.getPlayerComposition() != null && localPlayer.getPlayerComposition().isFemale();

		for (String outfitName : SkillingOutfitData.OUTFITS_DATA.keySet())
		{
			if (!shouldDisplayOutfit(outfitName)) continue;

			SkillingOutfitData.SkillingOutfitDataEntry entry = SkillingOutfitData.OUTFITS_DATA.get(outfitName);
			if (entry == null) continue;
			if (outfitName.equals("Farming - Farmer's Outfit Male") && isFemale) continue;
			if (outfitName.equals("Farming - Farmer's Outfit Female") && !isFemale) continue;

			yOffset = paintOutfit(entry, outfitName, g, panelWidth, yOffset, fm);
		}

		drawItemHoverTooltip(g);
		drawOutfitHoverTooltip(g);
	}

	private int paintCollectedCounts(Graphics2D g, int panelWidth, int yOffset, FontMetrics fm)
	{
		if (config.displayCollectedOutfits())
		{
			int ownedOutfits = (int) SkillingOutfitData.OUTFITS_DATA.values().stream()
					.filter(entry -> entry.items.values().stream().allMatch(item -> isItemOwnedCached(item.getItemId())))
					.count();
			int totalOutfits = SkillingOutfitData.OUTFITS_DATA.size();
			String text = "Collected: " + ownedOutfits + " / " + totalOutfits + " Outfits";
			g.setColor(getCollectedColor(ownedOutfits, totalOutfits));
			yOffset += config.outfitTextSpacer();
			g.drawString(text, (panelWidth - fm.stringWidth(text)) / 2, yOffset + fm.getAscent());
			yOffset += fm.getHeight();
		}

		if (config.displayCollectedItems())
		{
			int ownedItems = (int) SkillingOutfitData.OUTFITS_DATA.values().stream()
					.flatMap(entry -> entry.items.values().stream())
					.mapToInt(item -> isItemOwnedCached(item.getItemId()) ? 1 : 0)
					.sum();
			int totalItems = SkillingOutfitData.OUTFITS_DATA.values().stream().mapToInt(entry -> entry.items.size()).sum();
			String text = "Collected: " + ownedItems + " / " + totalItems + " Items";
			g.setColor(getCollectedColor(ownedItems, totalItems));
			yOffset += config.itemTextSpacer();
			g.drawString(text, (panelWidth - fm.stringWidth(text)) / 2, yOffset + fm.getAscent());
			yOffset += fm.getHeight();
		}
		return yOffset;
	}

	private int paintOutfit(SkillingOutfitData.SkillingOutfitDataEntry entry, String outfitName, Graphics2D g, int panelWidth, int yOffset, FontMetrics fm)
	{
		Map<Integer, SkillingOutfitItem> items = entry.items;
		boolean allObtained = items.values().stream().allMatch(item -> isItemOwnedCached(item.getItemId()));
		if (allObtained && !config.showObtainedItems()) return yOffset;

		// ----------------------------
		// Outfit title
		// ----------------------------
		yOffset += config.firstOutfitSpacer();
		// Determine outfit color (respecting override config)
		Color outfitColor;
		if (config.overrideOutfitColors())                   // Use the user's custom override color
		{
			outfitColor = config.outfitNameColor();
		}
		else                                                 // Use the default color based on primary skill (or fallback cyan)
		{
			outfitColor = (entry.primarySkill != null)
					? SKILL_COLORS.getOrDefault(entry.primarySkill, Color.CYAN)
					: Color.CYAN;
		}
		// Draw the outfit title
		g.setColor(outfitColor);
		int nameWidth = fm.stringWidth(outfitName);
		int outfitX = (panelWidth - nameWidth) / 2;
		g.drawString(outfitName, outfitX, yOffset + fm.getAscent());
		// Track hover bounds
		outfitBounds.put(outfitName, new Rectangle(outfitX, yOffset, nameWidth, fm.getHeight()));
		// Increment offset
		yOffset += fm.getHeight();

		// ----------------------------
		// Main points / cost line
		// ----------------------------
		yOffset += config.totalNeededTextSpacer();
		String pointsLine = buildPointsLine(entry, items);
		if (!pointsLine.isEmpty())
		{
			g.setColor(Color.LIGHT_GRAY);
			g.drawString(pointsLine, (panelWidth - fm.stringWidth(pointsLine)) / 2, yOffset + fm.getAscent());
			yOffset += fm.getHeight();
		}

		// ----------------------------
		// Icons
		// ----------------------------
		yOffset += config.iconTextSpacer();
		List<Map.Entry<Integer, SkillingOutfitItem>> itemList = items.entrySet().stream()
				.filter(e -> config.showObtainedItems() || !isItemOwnedCached(e.getKey()))
				.collect(Collectors.toList());

		int totalItems = itemList.size();
		int rows = (int) Math.ceil(totalItems / (double) config.maxCols());

		for (int row = 0; row < rows; row++)
		{
			int itemsInRow = Math.min(config.maxCols(), totalItems - row * config.maxCols());
			int rowWidth = itemsInRow * config.iconSize() + (itemsInRow - 1) * config.iconGapSpacing();
			int startX = (panelWidth - rowWidth) / 2;

			for (int col = 0; col < itemsInRow; col++)
			{
				int index = row * config.maxCols() + col;
				var entryItem = itemList.get(index);
				int itemId = entryItem.getKey();
				BufferedImage sprite = itemSprites.get(itemId);
				if (sprite == null) continue;

				int x = startX + col * (config.iconSize() + config.iconGapSpacing());
				int y = yOffset + row * (config.iconSize() + config.iconGapSpacing());
				g.drawImage(sprite, x, y, config.iconSize(), config.iconSize(), null);

				if (isItemOwnedCached(itemId) && config.showObtainedItems())
				{
					g.setColor(Color.GREEN);
					g.setStroke(new BasicStroke(2));
					g.drawRect(x, y, config.iconSize(), config.iconSize());
				}

				iconBounds.put(itemId, new Rectangle(x, y, config.iconSize(), config.iconSize()));
				hoverItemMap.put(itemId, entryItem.getValue());
			}
		}
		yOffset += rows * (config.iconSize() + config.iconGapSpacing());

		// ----------------------------
		// Extra costs (centered)
		// ----------------------------
		yOffset = drawOutfitExtraCosts(g, entry.items, panelWidth, yOffset);

		return yOffset;
	}

	// Updated buildPointsLine to only count remaining unowned items
	private String buildPointsLine(SkillingOutfitData.SkillingOutfitDataEntry entry, Map<Integer, SkillingOutfitItem> items)
	{
		if (!config.showTotalObtain()) return "";

		// Only hide the line if the WHOLE SET is obtained
		boolean fullSetOwned = items.values().stream()
				.allMatch(i -> isItemOwnedCached(i.getItemId()));
		if (fullSetOwned)
		{
			return "";
		}

		int totalRequired = items.values().stream()
				.filter(i -> !isItemOwnedCached(i.getItemId()))
				.mapToInt(SkillingOutfitItem::getRequirement)
				.sum();

		int costItemId = items.values().stream()
				.filter(i -> !isItemOwnedCached(i.getItemId()))
				.mapToInt(SkillingOutfitItem::getCostItemId)
				.findFirst().orElse(-1);
		String costText = items.values().stream()
				.filter(i -> !isItemOwnedCached(i.getItemId()))
				.map(SkillingOutfitItem::getCostText)
				.findFirst().orElse("");

		int totalAvailable = (costItemId != -1)
				? tracker.getTotalCostItem(costItemId)
				: 0;

		switch (entry.primarySkill)
		{
			case "Construction": return tracker.getCarpenterPoints() + "/" + totalRequired + " Carpenter Points Owned";
			case "Farming": return tracker.getTitheFarmPoints() + "/" + totalRequired + " Farming Points Owned";
			case "Smithing": return tracker.getFoundryReputation() + "/" + totalRequired + " Reputation Owned";
			case "Firemaking": return tracker.getWintertodtCrates() + " Wintertodt Subdued";
			case "Fishing": return tracker.getTemporossPoints() + " Tempoross Kills";
			case "Hunter": return tracker.getHunterRumors() + " Hunter Rumors Completed";
			case "Prayer": return "Shade Catacombs - Gold Chest";
			case "Agility": return totalAvailable + "/" + totalRequired + " " + costText + " Owned";
			case "Woodcutting": return tracker.getAnimaBark() + "/" + totalRequired + " " + costText + " Owned";
			case "Thieving": return tracker.getRoguesDenAttempts() + " Safes Opened";
			default: return totalAvailable + "/" + totalRequired + " " + costText + " Owned";
		}
	}

	private int sumRequirements(Map<Integer, SkillingOutfitItem> items)
	{
		return items.values().stream()
				.filter(item -> !isItemOwnedCached(item.getItemId())) // only count unowned items
				.mapToInt(SkillingOutfitItem::getRequirement)
				.sum();
	}

	private boolean shouldDisplayOutfit(String outfitName)
	{
		BooleanSupplier supplier = outfitDisplayMap.get(outfitName);
		return supplier != null && supplier.getAsBoolean();
	}

	private Color getCollectedColor(long owned, long total)
	{
		if (!config.colorTextForCollected()) return Color.WHITE;
		double percent = total == 0 ? 0 : (owned * 100.0 / total);
		if (percent <= 33) return Color.RED;
		else if (percent <= 65) return Color.ORANGE;
		else if (percent < 100) return Color.YELLOW;
		else return Color.GREEN;
	}

	private int getItemAt(int x, int y)
	{
		for (var entry : iconBounds.entrySet())
			if (entry.getValue().contains(x, y)) return entry.getKey();
		return -1;
	}

	private String getOutfitAt(int x, int y)
	{
		for (var entry : outfitBounds.entrySet())
			if (entry.getValue().contains(x, y)) return entry.getKey();
		return null;
	}

	private boolean isItemOwnedCached(int itemId)
	{
		// First, find the SkillingOutfitItem for this ID
		SkillingOutfitItem outfitItem = null;
		outer:
		for (var entry : SkillingOutfitData.OUTFITS_DATA.values())
			for (SkillingOutfitItem i : entry.items.values())
				if (i.getItemId() == itemId)
				{
					outfitItem = i;
					break outer;
				}

		// If we have an outfitItem, check both main ID and otherItemIds
		if (outfitItem != null)
		{
			// Combine main ID + other IDs into one stream
			List<Integer> allIds = new ArrayList<>();
			allIds.add(outfitItem.getItemId());
			allIds.addAll(outfitItem.getOtherItemIds());

			for (int id : allIds)
			{
				if (tracker.getObtainedItems().contains(id)) return true;

				int inv = inventoryCacheSnapshot.getOrDefault(id, 0);
				int equip = equipmentCacheSnapshot.getOrDefault(id, 0);
				int bank = bankCacheSnapshot.getOrDefault(id, 0);
				if ((inv + equip + bank) > 0) return true;
			}

			return false;
		}

		// fallback: check only tracker and caches for the single ID
		if (tracker.getObtainedItems().contains(itemId)) return true;
		int inv = inventoryCacheSnapshot.getOrDefault(itemId, 0);
		int equip = equipmentCacheSnapshot.getOrDefault(itemId, 0);
		int bank = bankCacheSnapshot.getOrDefault(itemId, 0);
		return (inv + equip + bank) > 0;
	}


	public void updateAllCaches()
	{
		clientThread.invoke(() -> {
			tracker.updateAllCaches();
			inventoryCacheSnapshot.clear(); inventoryCacheSnapshot.putAll(tracker.getInventoryCacheSnapshot());
			equipmentCacheSnapshot.clear(); equipmentCacheSnapshot.putAll(tracker.getEquipmentCacheSnapshot());
			bankCacheSnapshot.clear(); bankCacheSnapshot.putAll(tracker.getBankCacheSnapshot());
			SwingUtilities.invokeLater(innerPanel::repaint);
		});
	}

	public int getSnapshotTotalCostItem(int itemId)
	{
		return inventoryCacheSnapshot.getOrDefault(itemId, 0)
				+ equipmentCacheSnapshot.getOrDefault(itemId, 0)
				+ bankCacheSnapshot.getOrDefault(itemId, 0);
	}

	private Dimension calculatePreferredSize()
	{
		int width = 300;
		int height = config.panelTitleSpacer();
		FontMetrics fm = getFontMetrics(getFont());

		if (config.displayCollectedOutfits())
			height += config.outfitTextSpacer() + fm.getHeight();
		if (config.displayCollectedItems())
			height += config.itemTextSpacer() + fm.getHeight();

		for (String outfitName : SkillingOutfitData.OUTFITS_DATA.keySet())
		{
			if (!shouldDisplayOutfit(outfitName)) continue;
			SkillingOutfitData.SkillingOutfitDataEntry entry = SkillingOutfitData.OUTFITS_DATA.get(outfitName);
			if (entry == null) continue;

			height += config.firstOutfitSpacer() + fm.getHeight();               // title
			height += config.totalNeededTextSpacer() + fm.getHeight();           // points line
			height += config.iconTextSpacer();                                   // spacing before icons

			int rows = (int) Math.ceil(entry.items.size() / (double) config.maxCols());
			height += rows * (config.iconSize() + config.iconGapSpacing());      // icons

			// --- Extra costs per item ---
			for (SkillingOutfitItem item : entry.items.values())
			{
				List<SkillingOutfitItem.CostEntry> extra = SkillingOutfitData.EXTRA_COSTS.get(item.getItemId());
				if (extra != null && !extra.isEmpty())
				{
					height += 10; // spacing before header (matches drawOutfitExtraCosts)
					height += fm.getHeight(); // header line
					height += fm.getHeight() * extra.size(); // each extra resource line
					height += 6; // spacing after each item
				}
			}
		}

		// add small bottom buffer
		height += 80;

		return new Dimension(width, height);
	}



	public void refresh()
	{
		setupOutfitDisplayMap();
		if (innerPanel != null)
		{
			innerPanel.revalidate();
			innerPanel.repaint();
		}
	}

	private void drawItemHoverTooltip(Graphics2D g)
	{
		if (hoveredItemId == -1) return;

		Rectangle iconRect = iconBounds.get(hoveredItemId);
		SkillingOutfitItem item = hoverItemMap.get(hoveredItemId);
		if (iconRect == null || item == null) return;

		List<String> tooltipLines = new ArrayList<>();
		tooltipLines.add(item.getName());

		StringBuilder requirementLine = new StringBuilder();
		int requirement = item.getRequirement();
		String costText = item.getCostText();

		if (requirement > 0)
		{
			requirementLine.append(requirement);
		}

		if (costText != null && !costText.isEmpty())
		{
			if (requirementLine.length() > 0) requirementLine.append(" ");
			requirementLine.append(costText);
		}

		if (requirementLine.length() > 0)
		{
			tooltipLines.add(requirementLine.toString());
		}

		tooltipLines.add("Click To Open Wiki");

		drawTooltip(g, iconRect, tooltipLines);
	}

	private void drawOutfitHoverTooltip(Graphics2D g)
	{
		if (hoveredOutfitName == null) return;
		Rectangle outfitRect = outfitBounds.get(hoveredOutfitName);
		if (outfitRect == null) return;

		SkillingOutfitData.SkillingOutfitDataEntry entry = SkillingOutfitData.OUTFITS_DATA.get(hoveredOutfitName);
		if (entry == null) return;

		int owned = (int) entry.items.values().stream().filter(i -> isItemOwnedCached(i.getItemId())).count();
		int total = entry.items.size();
		int totalRequired = entry.items.values().stream().filter(i -> !isItemOwnedCached(i.getItemId())).mapToInt(SkillingOutfitItem::getRequirement).sum();
		int costItemId = entry.items.values().stream().filter(i -> !isItemOwnedCached(i.getItemId())).mapToInt(SkillingOutfitItem::getCostItemId).findFirst().orElse(-1);
		int costAvailable = getSnapshotTotalCostItem(costItemId);
		String costText = entry.items.values().stream().filter(i -> !isItemOwnedCached(i.getItemId())).map(SkillingOutfitItem::getCostText).findFirst().orElse("");

		List<String> lines = new ArrayList<>();
		lines.add("Owned: " + owned + "/" + total + " Items");
		if (totalRequired > 0) lines.add(costAvailable + "/" + totalRequired + " " + costText + " Owned");
		lines.add("Click To Open Wiki");

		drawTooltip(g, outfitRect, lines);
	}

	private void drawTooltip(Graphics2D g, Rectangle rect, List<String> lines)
	{
		FontMetrics fm = g.getFontMetrics();
		int padding = 4;
		int overlayWidth = lines.stream().mapToInt(fm::stringWidth).max().orElse(0) + padding * 2;
		int overlayHeight = lines.size() * fm.getHeight() + padding * 4;

		int x = (getWidth() - overlayWidth) / 2;
		int y = rect.y + rect.height + 5;

		g.setColor(new Color(0, 0, 0, 220));
		g.fillRect(x, y, overlayWidth, overlayHeight);
		g.setColor(Color.WHITE);
		g.drawRect(x, y, overlayWidth, overlayHeight);

		for (int i = 0; i < lines.size(); i++)
			g.drawString(lines.get(i), x + (overlayWidth - fm.stringWidth(lines.get(i))) / 2, y + padding + i * fm.getHeight() + fm.getAscent());
	}

	private void openWikiLink(String url)
	{
		try
		{
			url = url.replace(" ", "_");
			Desktop.getDesktop().browse(URI.create(url));
		}
		catch (Exception e)
		{
			try
			{
				// Fallback: Encode manually
				String encoded = java.net.URLEncoder.encode(url, StandardCharsets.UTF_8);
				Desktop.getDesktop().browse(URI.create(encoded));
			}
			catch (Exception ex)
			{

			}
		}
	}

	// Updated drawOutfitExtraCosts
	private int drawOutfitExtraCosts(Graphics2D g, Map<Integer, SkillingOutfitItem> items, int panelWidth, int startY)
	{
		// Skip entirely if the user disabled "Show Price To Obtain"
		if (!config.showTotalObtain() || !config.showItemTotalObtain())
		{
			return startY;
		}

		int y = startY;
		y += 10; // spacing before totals
		FontMetrics fm = g.getFontMetrics();

		for (SkillingOutfitItem item : items.values())
		{
			List<SkillingOutfitItem.CostEntry> extra = SkillingOutfitData.EXTRA_COSTS.get(item.getItemId());
			if (extra == null || extra.isEmpty())
				continue;

			// ✅ Skip if player already owns this outfit item
			if (tracker.getObtainedItems().contains(item.getItemId()))
			{
				continue;
			}

			// ---- Check if all extra costs are already met ----
			boolean allObtained = true;
			for (SkillingOutfitItem.CostEntry e : extra)
			{
				int owned = (e.getItemId() == 28134)
						? config.animaBark()
						: tracker.getTotalCostItem(e.getItemId());
				if (owned < e.getAmount())
				{
					allObtained = false;
					break;
				}
			}

			// ✅ Skip if all costs are satisfied
			if (allObtained)
				continue;

			// ---- Determine primary skill for this item ----
			String primarySkill = null;
			for (SkillingOutfitData.SkillingOutfitDataEntry entry : SkillingOutfitData.OUTFITS_DATA.values())
			{
				if (entry.items.containsKey(item.getItemId()))
				{
					primarySkill = entry.primarySkill;
					break;
				}
			}

			// ---- Determine header color ----
			Color headerColor = config.overrideOutfitColors()
					? config.outfitNameColor()
					: (primarySkill != null
					? SKILL_COLORS.getOrDefault(primarySkill, new Color(180, 200, 255))
					: new Color(180, 200, 255));

			// ---- Draw header ----
			String header = item.getName() + " Total Cost";
			int headerX = (panelWidth - fm.stringWidth(header)) / 2;
			g.setColor(headerColor);
			g.drawString(header, headerX, y);
			y += fm.getHeight();

			// ---- Draw resource lines ----
			g.setColor(Color.LIGHT_GRAY);
			for (SkillingOutfitItem.CostEntry e : extra)
			{
				int owned = (e.getItemId() == 28134)
						? config.animaBark()
						: tracker.getTotalCostItem(e.getItemId());
				String line = String.format("%,d/%d %s", owned, e.getAmount(), e.getItemName());
				int lineX = (panelWidth - fm.stringWidth(line)) / 2;
				g.drawString(line, lineX, y);
				y += fm.getHeight();
			}

			y += 6; // spacing between items
		}

		return y;
	}

}