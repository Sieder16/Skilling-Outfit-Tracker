
package com.gimsieder.skillingoutfit;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.api.ChatMessageType;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.*;

@Getter
@Setter
public class SkillingOutfitTracker
{
	private int carpenterPoints = 0;
	private int carpenterContracts = 0;
	private int farmingPoints = 0;
	private int temporossPoints = 0;
	private int hunterRumors = 0;
	private int wintertodtCrates = 0;
	private int animaBark;
	private int roguesDenAttempts = 0;

	private final Map<Integer, Boolean> ownedCache = new HashMap<>();
	private final Set<Integer> obtainedItems = new HashSet<>();
	private final Map<Integer, Integer> inventoryCostCache = new HashMap<>();
	private final Map<Integer, Integer> equipmentCostCache = new HashMap<>();
	private final Map<Integer, Integer> bankCostCache = new HashMap<>();

	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;
	private final String configGroup = "skillingoutfit";
	private final SkillingOutfitConfig config;
	private final ItemManager itemManager;

	private Map<Integer, Integer> inventoryCacheSnapshot = new HashMap<>();
	private Map<Integer, Integer> equipmentCacheSnapshot = new HashMap<>();
	private Map<Integer, Integer> bankCacheSnapshot = new HashMap<>();

	@Setter
	private SkillingOutfitPanel panel;

	@Inject
	public SkillingOutfitTracker(Client client, ClientThread clientThread, ConfigManager configManager, SkillingOutfitConfig config, ItemManager itemManager)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.config = config;
		this.itemManager = itemManager;

		loadObtainedItems();
		loadBankCache();
	}

	// ======== UPDATE ALL CACHES ========
	public void updateAllCaches() {
		updateInventoryCache();
		updateEquipmentCache();
		updateBankCache();
	}

	public void updateInventoryCache()
	{
		// Clear previous cache
		inventoryCostCache.clear();

		// Ensure this runs on the client thread
		clientThread.invoke(() ->
		{
			ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
			if (inv != null)
			{
				for (Item item : inv.getItems())
				{
					int id = item.getId();
					int qty = item.getQuantity();
					inventoryCostCache.put(id, qty);
				}
			}
			// Update snapshot for safe panel/thread reading
			inventoryCacheSnapshot = new HashMap<>(inventoryCostCache);
		});
	}

	public void updateEquipmentCache()
	{
		equipmentCostCache.clear();

		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment != null)
		{
			for (Item item : equipment.getItems())
			{
				equipmentCostCache.put(item.getId(), item.getQuantity());
			}
		}

		equipmentCacheSnapshot = new HashMap<>(equipmentCostCache);
	}

	public void updateBankCache()
	{
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		if (bank != null)
		{
			Map<Integer, Integer> newBankSnapshot = new HashMap<>();
			for (Item item : bank.getItems())
				newBankSnapshot.put(item.getId(), item.getQuantity());

			bankCacheSnapshot = newBankSnapshot;

			// Save to config immediately
			saveBankCache();
		}
	}

	public void saveBankCache()
	{
		if (bankCacheSnapshot.isEmpty())
			return;

		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Integer, Integer> entry : bankCacheSnapshot.entrySet())
		{
			sb.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
		}
		if (sb.length() > 0)
			sb.setLength(sb.length() - 1); // remove trailing comma

		configManager.setConfiguration(configGroup, "bankCache", sb.toString());
	}

	public void loadBankCache()
	{
		String saved = configManager.getConfiguration(configGroup, "bankCache");
		if (saved == null || saved.isEmpty())
			return;

		Map<Integer, Integer> loaded = new HashMap<>();
		for (String pair : saved.split(","))
		{
			String[] parts = pair.split(":");
			if (parts.length != 2)
				continue;
			try
			{
				int itemId = Integer.parseInt(parts[0]);
				int qty = Integer.parseInt(parts[1]);
				loaded.put(itemId, qty);
			}
			catch (NumberFormatException ignored) {}
		}

		bankCacheSnapshot = loaded;
	}

	// ======== TOTAL COST ITEMS COMBINED ========
	public int getTotalCostItem(int costId)
	{
		int inv = inventoryCacheSnapshot.getOrDefault(costId, 0);
		int equip = equipmentCacheSnapshot.getOrDefault(costId, 0);
		int bank = bankCacheSnapshot.getOrDefault(costId, 0);

		return inv + equip + bank;
	}


	// ======== OWNED ITEM CHECKS ========
	private boolean isItemOwnedCached(int itemId)
	{
		int invCount = getInventoryCacheSnapshot().getOrDefault(itemId, 0);
		int equipCount = getEquipmentCacheSnapshot().getOrDefault(itemId, 0);
		int bankCount = getBankCacheSnapshot().getOrDefault(itemId, 0);

		return (invCount + equipCount + bankCount) > 0;
	}

	public void updateOwnedItems()
	{
		for (SkillingOutfitData.SkillingOutfitDataEntry outfitEntry : SkillingOutfitData.OUTFITS_DATA.values())
		{
			for (SkillingOutfitItem item : outfitEntry.items.values())
			{
				boolean owned = getTotalCostItem(item.getItemId()) > 0; // use combined total
				ownedCache.put(item.getItemId(), owned);
			}
		}
	}

	// ======== OBTAINED ITEMS ========
	public void saveObtainedItems()
	{
		StringBuilder sb = new StringBuilder();
		for (int id : obtainedItems)
			sb.append(id).append(",");
		if (sb.length() > 0)
			sb.setLength(sb.length() - 1);

		configManager.setConfiguration(configGroup, "obtainedItems", sb.toString());
	}

	public void loadObtainedItems()
	{
		String saved = configManager.getConfiguration(configGroup, "obtainedItems");
		if (saved != null && !saved.isEmpty())
		{
			for (String s : saved.split(","))
			{
				try
				{
					int itemId = Integer.parseInt(s);
					obtainedItems.add(itemId);
				}
				catch (NumberFormatException ignored) {}
			}
		}
	}

	public void updateOwnedItemsFromCaches()
	{
		ownedCache.clear();

		// Combine tracked item sources
		Set<Integer> allTrackedItems = new HashSet<>();
		allTrackedItems.addAll(inventoryCacheSnapshot.keySet());
		allTrackedItems.addAll(bankCacheSnapshot.keySet());

		// Include equipped items
		ItemContainer equipmentContainer = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipmentContainer != null)
		{
			for (Item item : equipmentContainer.getItems())
			{
				if (item != null && item.getId() > 0)
				{
					allTrackedItems.add(item.getId());
				}
			}
		}

		// Track owned items
		for (int itemId : allTrackedItems)
		{
			boolean isInInventory = inventoryCacheSnapshot.getOrDefault(itemId, 0) > 0;
			boolean isInBank = bankCacheSnapshot.getOrDefault(itemId, 0) > 0;
			boolean isInEquipment = (equipmentContainer != null &&
					Arrays.stream(equipmentContainer.getItems())
							.anyMatch(i -> i != null && i.getId() == itemId));

			boolean isOwned = isInInventory || isInBank || isInEquipment;
			boolean wasOwnedBefore = ownedCache.getOrDefault(itemId, false);

			ownedCache.put(itemId, isOwned);

			// Only trigger notification if it's truly new — not being moved or loaded from equipment
			if (isOwned && !obtainedItems.contains(itemId))
			{
				obtainedItems.add(itemId);

				if (!wasOwnedBefore)
				{
					// Skip notification if the item is ONLY in equipment (login equip load)
					if (isInEquipment && !isInInventory && !isInBank)
					{
						continue; // Skip this one entirely
					}

					// Find outfit + item name
					String outfitName = null;
					String itemName = null;

					for (Map.Entry<String, SkillingOutfitData.SkillingOutfitDataEntry> entry : SkillingOutfitData.OUTFITS_DATA.entrySet())
					{
						SkillingOutfitData.SkillingOutfitDataEntry outfitEntry = entry.getValue();
						Map<Integer, SkillingOutfitItem> items = outfitEntry.items;

						if (items.containsKey(itemId))
						{
							outfitName = entry.getKey();
							itemName = items.get(itemId).getName();
							break;
						}
					}
				}
			}
		}

		// Remove items that are no longer owned (dropped, sold, etc.)
		obtainedItems.removeIf(id -> !ownedCache.getOrDefault(id, false));

		saveObtainedItems();
		refreshCostItemCache();

		if (panel != null)
		{
			SwingUtilities.invokeLater(() -> panel.updateAllCaches());
		}
	}


	public void markObtainedFromConfig()
	{
		// Load items saved in config
		loadObtainedItems();

		// Add all previously obtained items to ownedCache so UI knows you own them
		for (int itemId : obtainedItems)
		{
			ownedCache.put(itemId, true);
		}
	}


	// ======== COST ITEM CACHE ========
	private final Map<Integer, Integer> costItemCache = new HashMap<>();

	private int loadCostItem(int costItemId)
	{
		Integer stored = configManager.getRSProfileConfiguration(configGroup, "costItem_" + costItemId, Integer.class);
		return (stored != null) ? stored : 0;
	}

	public void loadAllCostItems()
	{
		for (SkillingOutfitData.SkillingOutfitDataEntry outfitEntry : SkillingOutfitData.OUTFITS_DATA.values())
		{
			for (SkillingOutfitItem item : outfitEntry.items.values())
			{
				int costId = item.getCostItemId();
				costItemCache.put(costId, loadCostItem(costId));
			}
		}
	}

	public void refreshCostItemCache()
	{
		costItemCache.clear();

		for (SkillingOutfitData.SkillingOutfitDataEntry outfitEntry : SkillingOutfitData.OUTFITS_DATA.values())
		{
			for (SkillingOutfitItem item : outfitEntry.items.values())
			{
				int costId = item.getCostItemId();
				int total = getTotalCostItem(costId); // inventory+equipment+bank
				costItemCache.put(costId, total);
			}
		}
	}

	public static final int FOUNDRY_REPUTATION = 3436;
	public int foundryReputation = 0;
	public static final int FARMING_POINTS = 4893;
	public int titheFarmPoints = 0;

}
