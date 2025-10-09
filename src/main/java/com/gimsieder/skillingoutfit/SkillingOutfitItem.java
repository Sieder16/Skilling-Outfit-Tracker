package com.gimsieder.skillingoutfit;

import java.util.Map;
import java.util.Objects;

public class SkillingOutfitItem
{
    private final int itemId;
    private final String name;
    private final int requirement;
    private final String costText;
    private final int costItemId;

    public SkillingOutfitItem(int itemId, String name, int requirement, String costText, int costItemId)
    {
        if (itemId < 0) throw new IllegalArgumentException("itemId must be non-negative");
        if (requirement < 0) throw new IllegalArgumentException("requirement must be non-negative");
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("name cannot be null or empty");
        if (costText == null) throw new IllegalArgumentException("costText cannot be null");

        this.itemId = itemId;
        this.name = name;
        this.requirement = requirement;
        this.costText = costText;
        this.costItemId = costItemId;
    }

    // ------------------------
    // Getters
    // ------------------------
    public int getItemId() { return itemId; }
    public String getName() { return name; }
    public int getRequirement() { return requirement; }
    public String getCostText() { return costText; }
    public int getCostItemId() { return costItemId; }

    // ------------------------
    // Overrides
    // ------------------------

    @Override
    public String toString()
    {
        return name + " (ID: " + itemId + ", Requirement: " + requirement + " " + costText + ")";
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof SkillingOutfitItem)) return false;
        SkillingOutfitItem that = (SkillingOutfitItem) o;
        return itemId == that.itemId &&
                requirement == that.requirement &&
                costItemId == that.costItemId &&
                name.equals(that.name) &&
                costText.equals(that.costText);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(itemId, name, requirement, costText, costItemId);
    }

}
