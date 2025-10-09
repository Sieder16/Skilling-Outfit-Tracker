package com.gimsieder.skillingoutfit;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.swing.*;
import java.util.*;

@Getter
@Setter
public class SkillingOutfitTracker
{
    private final Map<Integer, Boolean> ownedCache = new HashMap<>();
    private final Set<Integer> obtainedItems = new HashSet<>();
    private final Map<Integer, Integer> inventoryCostCache = new HashMap<>();
    private final Map<Integer, Integer> equipmentCostCache = new HashMap<>();
    private final Map<Integer, Integer> bankCostCache = new HashMap<>();

    private final Client client;
    private final ClientThread clientThread;
    private final ConfigManager configManager;
    private final String configGroup = "skillingoutfit";

    private Map<Integer, Integer> inventoryCacheSnapshot = new HashMap<>();
    private Map<Integer, Integer> equipmentCacheSnapshot = new HashMap<>();
    private Map<Integer, Integer> bankCacheSnapshot = new HashMap<>();

    @Setter
    private SkillingOutfitPanel panel;

    @Inject
    public SkillingOutfitTracker(Client client, ClientThread clientThread, ConfigManager configManager)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.configManager = configManager;

        loadObtainedItems();
    }

    // ======== UPDATE ALL CACHES ========
    public void updateAllCaches()
    {
        updateInventoryCache();
        updateEquipmentCache();
        updateBankCache();
    }

    public void updateInventoryCache()
    {
        inventoryCostCache.clear();

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
        }
    }

    public int getTotalCostItem(int costId)
    {
        int inv = inventoryCacheSnapshot.getOrDefault(costId, 0);
        int equip = equipmentCacheSnapshot.getOrDefault(costId, 0);
        int bank = bankCacheSnapshot.getOrDefault(costId, 0);

        return inv + equip + bank;
    }

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

    public void addObtainedItem(int itemId)
    {
        if (obtainedItems.add(itemId))
        {
            saveObtainedItems();

            if (panel != null)
            {
                SwingUtilities.invokeLater(panel::updateAllCaches);
            }
        }
    }

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
        Set<Integer> allItems = new HashSet<>();
        allItems.addAll(getInventoryCacheSnapshot().keySet());
        allItems.addAll(getEquipmentCacheSnapshot().keySet());
        allItems.addAll(getBankCacheSnapshot().keySet());

        for (int itemId : allItems)
        {
            addObtainedItem(itemId);
        }
    }

    public void markObtainedFromConfig()
    {
        loadObtainedItems();

        for (int itemId : obtainedItems)
        {
            ownedCache.put(itemId, true);
        }
    }

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

}
