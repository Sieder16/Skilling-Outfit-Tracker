package com.gimsieder.skillingoutfit;

import net.runelite.client.config.*;
import java.awt.*;

@ConfigGroup("skillingoutfit")
public interface SkillingOutfitConfig extends Config
{
    // 1. Notify on new item
    @ConfigItem(
            keyName = "notifyOnNew",
            name = "Notify on new item",
            description = "Send a notification when a new skilling outfit piece is obtained",
            position = 1
    )
    default boolean notifyOnNew()
    {
        return true;
    }

    // 2. Panel Title Spacer (formerly yOffset)
    @ConfigItem(
            keyName = "panelTitleSpacer",
            name = "Panel Title Spacer",
            description = "How far the Title is from the top of the screen",
            position = 2
    )
    default int panelTitleSpacer()
    {
        return 20;
    }

    // 3. Display Collected Outfits
    @ConfigItem(
            keyName = "displayCollectedOutfits",
            name = "Display Collected Outfits",
            description = "Show the total number of outfits collected at the top of the panel",
            position = 3
    )
    default boolean displayCollectedOutfits()
    {
        return false;
    }

    // 4. Outfit Text Spacer (formerly LINE_SPACING_HALF)
    @ConfigItem(
            keyName = "outfitTextSpacer",
            name = "Outfit Text Spacer",
            description = "Spacing between panel title/previous section and outfit text",
            position = 4
    )
    default int outfitTextSpacer()
    {
        return 10;
    }

    // 5. Display Collected Items
    @ConfigItem(
            keyName = "displayCollectedItems",
            name = "Display Collected Items",
            description = "Show the total number of items collected at the top of the panel",
            position = 5
    )
    default boolean displayCollectedItems()
    {
        return false;
    }

    // 6. Item Text Spacer (formerly LINE_SPACING_QUARTER)
    @ConfigItem(
            keyName = "itemTextSpacer",
            name = "Item Text Spacer",
            description = "Spacing between collected items line and next line",
            position = 6
    )
    default int itemTextSpacer()
    {
        return 6;
    }

    // 7. Color Text for Collected
    @ConfigItem(
            keyName = "colorTextForCollected",
            name = "Color Text For Collected",
            description = "Color-code collected totals based on percentage completed",
            position = 7
    )
    default boolean colorTextForCollected()
    {
        return false;
    }

    // 8. First Outfit Spacer
    @ConfigItem(
            keyName = "firstOutfitSpacer",
            name = "First Outfit Spacer",
            description = "Spacing between plugin title / collected outfits/items to first outfit title",
            position = 8
    )
    default int firstOutfitSpacer()
    {
        return 12;
    }

    // 10. Total Needed Text Spacer
    @ConfigItem(
            keyName = "totalNeededTextSpacer",
            name = "Total Needed Text Spacer",
            description = "Spacing between first outfit title and first icon row",
            position = 10
    )
    default int totalNeededTextSpacer()
    {
        return 6;
    }

    // 11. Icon Spacer
    @ConfigItem(
            keyName = "iconTextSpacer",
            name = "Icon Text Spacer",
            description = "Spacing between outfit text title / total needed and icons",
            position = 11
    )
    default int iconTextSpacer()
    {
        return 20;
    }

    // 12. Icon Size
    @ConfigItem(
            keyName = "iconSize",
            name = "Icon Size",
            description = "Size of each item icon in the panel",
            position = 12
    )
    default int iconSize()
    {
        return 48;
    }


    // 13. Icon Gap Spacing
    @ConfigItem(
            keyName = "iconGapSpacing",
            name = "Icon Size",
            description = "Size of each gap between each icon in the panel",
            position = 12
    )
    default int iconGapSpacing()
    {
        return 48;
    }

    // 14. Icon Column Count (maxCols)
    @ConfigItem(
            keyName = "maxCols",
            name = "Max Columns",
            description = "How many icons before going to next row",
            position = 14
    )
    default int maxCols()
    {
        return 5;
    }

    // 15. Show Obtained Items
    @ConfigItem(
            keyName = "showObtainedItems",
            name = "Show Obtained Items",
            description = "Show individual icons for items that are already obtained",
            position = 15
    )
    default boolean showObtainedItems()
    {
        return false;
    }

    // 16. Override Outfit Colors
    @ConfigItem(
            keyName = "overrideOutfitColors",
            name = "Override Outfit Colors",
            description = "If enabled, all outfit section names will use the chosen override color instead of green/red",
            position = 16
    )
    default boolean overrideOutfitColors()
    {
        return false;
    }

    // 17. Outfit Name Color
    @Alpha
    @ConfigItem(
            keyName = "outfitNameColor",
            name = "Outfit Name Color",
            description = "Color for outfit names when override is enabled",
            position = 17
    )
    default Color outfitNameColor()
    {
        return new Color(255, 255, 255, 255);
    }

    // 18-19 left blank if needed for future use

    // 20. Enable Popout Config Mode
    @ConfigItem(
            keyName = "enablePopoutConfigMode",
            name = "Enable Popout Config Mode",
            description = "Opens the config as a separate popout window for live tweaking",
            position = 20
    )
    default boolean enablePopoutConfigMode()
    {
        return false;
    }

// ----------------------- Skip to Outfits Display -------------------------------

    @ConfigSection(
            name = "Outfit Sets",
            description = "Settings for displaying each outfit",
            position = 50
    )
    String displayoutfit = "Display Outfits";

    @ConfigItem(
            keyName = "displayAgility",
            name = "Display Agility Outfit",
            description = "Toggles if Agility set shows in panel display",
            position = 51,
            section = displayoutfit
    )
    default boolean displayAgility()
    {
        return true;
    }

    @ConfigItem(
            keyName = "displayConstruction",
            name = "Display Construction Outfit",
            description = "Toggles if Construction set shows in panel display",
            position = 52,
            section = displayoutfit
    )
    default boolean displayConstruction()
    {
        return true;
    }

    @ConfigItem(
            keyName = "displayFarming",
            name = "Display Farming Outfit",
            description = "Toggles if Farming set shows in panel display",
            position = 53,
            section = displayoutfit
    )
    default boolean displayFarming()
    {
        return true;
    }

    @ConfigItem(
            keyName = "displayFiremaking",
            name = "Display Firemaking Outfit",
            description = "Toggles if Firemaking set shows in panel display",
            position = 54,
            section = displayoutfit
    )
    default boolean displayFiremaking()
    {
        return true;
    }

    @ConfigItem(
            keyName = "displayFishing",
            name = "Display Fishing Outfit",
            description = "Toggles if Fishing set shows in panel display",
            position = 55,
            section = displayoutfit
    )
    default boolean displayFishing()
    {
        return true;
    }

    @ConfigItem(
            keyName = "displayHunter",
            name = "Display Hunter Outfit",
            description = "Toggles if Hunter set shows in panel display",
            position = 56,
            section = displayoutfit
    )
    default boolean displayHunter()
    {
        return true;
    }

    @ConfigItem(
            keyName = "displayMining",
            name = "Display Mining Outfit",
            description = "Toggles if Mining set shows in panel display",
            position = 57,
            section = displayoutfit
    )
    default boolean displayMining()
    {
        return true;
    }

    @ConfigItem(
            keyName = "displayPrayer",
            name = "Display Prayer Outfit",
            description = "Toggles if Prayer set shows in panel display",
            position = 58,
            section = displayoutfit
    )
    default boolean displayPrayer()
    {
        return true;
    }

    @ConfigItem(
            keyName = "displayRunecraft",
            name = "Display Runecraft Outfit",
            description = "Toggles if Runecraft set shows in panel display",
            position = 59,
            section = displayoutfit
    )
    default boolean displayRunecraft()
    {
        return true;
    }

    @ConfigItem(
            keyName = "displaySmithing",
            name = "Display Smithing Outfit",
            description = "Toggles if Smithing set shows in panel display",
            position = 60,
            section = displayoutfit
    )
    default boolean displaySmithing()
    {
        return true;
    }

    @ConfigItem(
            keyName = "displayThieving",
            name = "Display Thieving Outfit",
            description = "Toggles if Thieving set shows in panel display",
            position = 60,
            section = displayoutfit
    )
    default boolean displayThieving()
    {
        return true;
    }

    @ConfigItem(
            keyName = "displayWoodcutting",
            name = "Display Woodcutting Outfit",
            description = "Toggles if Woodcutting set shows in panel display",
            position = 60,
            section = displayoutfit
    )
    default boolean displayWoodcutting()
    {
        return true;
    }


}
