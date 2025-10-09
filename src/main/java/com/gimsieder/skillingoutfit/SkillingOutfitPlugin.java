package com.gimsieder.skillingoutfit;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;

@PluginDescriptor(
        name = "Skilling Outfit Tracker",
        description = "Tracks skilling outfits obtained (inventory, bank, or equipped)",
        tags = {"skilling", "outfit", "tracking"}
)
@Slf4j
public class SkillingOutfitPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private SkillingOutfitTracker tracker;
    @Inject private ItemManager itemManager;
    @Inject private ClientToolbar clientToolbar;
    @Inject private ClientThread clientThread;
    @Inject private ChatMessageManager chatMessageManager;
    @Inject private SkillingOutfitConfig config;
    @Inject private ConfigManager configManager;
    @Inject private EventBus eventBus;

    private NavigationButton navButton;
    private boolean notifyOnNew;
    private SkillingOutfitWindow popoutWindow;
    private SkillingOutfitPanel panel;
    private final String configGroup = "skillingoutfit";

    @Override
    protected void startUp()
    {
        // Create the panel
        panel = new SkillingOutfitPanel(client, itemManager, tracker, config, configManager, this, clientThread);

        configManager.setConfiguration("skillingoutfit", "enablePopoutConfigMode", "false");

        // Placeholder icon while async image loads
        BufferedImage placeholder = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);

        // Build initial navigation button with placeholder
        navButton = NavigationButton.builder()
                .tooltip("Skilling Outfit Tracker")
                .icon(placeholder)
                .priority(1)
                .panel(panel)
                .build();

        // Add placeholder button to toolbar
        clientToolbar.addNavigation(navButton);

        // Async load the real icon
        clientThread.invokeLater(() ->
        {
            BufferedImage icon = itemManager.getImage(20169, 1, false);
            if (icon != null)
            {
                // Remove the old button
                clientToolbar.removeNavigation(navButton);

                // Rebuild nav button with the correct icon
                navButton = NavigationButton.builder()
                        .tooltip("Skilling Outfit Tracker")
                        .icon(icon)
                        .priority(1)
                        .panel(panel)
                        .build();

                // Add new button to toolbar
                clientToolbar.addNavigation(navButton);
            }
        });

        // Load tracker caches and update panel
        notifyOnNew = config.notifyOnNew();

        clientThread.invokeLater(() ->
        {
            tracker.loadObtainedItems();

            tracker.updateInventoryCache();
            tracker.updateEquipmentCache();
            tracker.updateBankCache();

            clientThread.invokeLater(() ->
            {
                tracker.updateOwnedItemsFromCaches();
                tracker.updateOwnedItems();
                tracker.saveObtainedItems();
                safeUpdatePanel(panel::updateAllCaches);
            });
        });
    }


    @Override
    protected void shutDown()
    {
        if (navButton != null) clientToolbar.removeNavigation(navButton);
        if (popoutWindow != null)
        {
            popoutWindow.dispose();
            popoutWindow = null;
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        int id = event.getContainerId();
        if (id == InventoryID.INVENTORY.getId()
                || id == InventoryID.EQUIPMENT.getId()
                || id == InventoryID.BANK.getId())
        {
                safeUpdatePanel(() -> {
                panel.updateAllCaches();
                tracker.updateOwnedItemsFromCaches();
            });
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        tracker.updateAllCaches();
        safeUpdatePanel(panel::updateAllCaches);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            tracker.updateInventoryCache();
            tracker.updateEquipmentCache();
            tracker.updateBankCache();
            tracker.updateOwnedItems();
            safeUpdatePanel(panel::updateAllCaches);
        }
    }

    // ===== Config Changed =====
    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!"skillingoutfit".equals(event.getGroup())) return;

        SwingUtilities.invokeLater(() -> {
            switch (event.getKey())
            {
                case "enablePopoutConfigMode":
                    if (config.enablePopoutConfigMode()) {
                        if (popoutWindow == null || !popoutWindow.isDisplayable())
                            popoutWindow = new SkillingOutfitWindow(config, panel, configManager, eventBus, clientThread);
                    } else if (popoutWindow != null) {
                        popoutWindow.dispose();
                        popoutWindow = null;
                    }   break;

                case "notifyOnNew":
                    notifyOnNew = config.notifyOnNew();
                    break;

                default:
                    if (event.getKey().startsWith("display") && panel != null)
                        panel.setupOutfitDisplayMap();
            }
            safeUpdatePanel(panel::refresh);
        });
    }

    @Provides
    SkillingOutfitConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(SkillingOutfitConfig.class);
    }

    private void safeUpdatePanel(Runnable r)
    {
        if (panel != null) r.run();
    }

    private void persistConfig(String key, int value)
    {
        if (key != null) configManager.setConfiguration("skillingoutfit", key, value);
        safeUpdatePanel(panel::updateAllCaches);
    }


}
