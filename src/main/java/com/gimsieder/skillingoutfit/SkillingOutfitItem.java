package com.gimsieder.skillingoutfit;

import java.util.List;
import java.util.Objects;

public class SkillingOutfitItem
{
	private final int itemId;
	private final String name;
	private final int requirement;
	private final String costText;
	private final int costItemId;
	private final List<Integer> otherItemIds;

	// Constructor with otherItemIds
	public SkillingOutfitItem(int itemId, String name, int requirement, String costText, int costItemId, List<Integer> otherItemIds)
	{
		if (itemId < 0) throw new IllegalArgumentException("itemId must be non-negative");
		if (requirement < 0) throw new IllegalArgumentException("requirement must be non-negative");
		if (name == null || name.isEmpty()) throw new IllegalArgumentException("name cannot be null or empty");
		if (costText == null) throw new IllegalArgumentException("costText cannot be null");
		if (otherItemIds == null) throw new IllegalArgumentException("otherItemIds cannot be null");

		this.itemId = itemId;
		this.name = name;
		this.requirement = requirement;
		this.costText = costText;
		this.costItemId = costItemId;
		this.otherItemIds = List.copyOf(otherItemIds);

	}

	// Constructor without otherItemIds (defaults to empty list)
	public SkillingOutfitItem(int itemId, String name, int requirement, String costText, int costItemId)
	{
		this(itemId, name, requirement, costText, costItemId, List.of());
	}

	// ------------------------
	// Getters
	// ------------------------
	public int getItemId() { return itemId; }
	public String getName() { return name; }
	public int getRequirement() { return requirement; }
	public String getCostText() { return costText; }
	public int getCostItemId() { return costItemId; }
	public List<Integer> getOtherItemIds() { return otherItemIds; }

	// ------------------------
	// Overrides
	// ------------------------

	@Override
	public String toString()
	{
		return name + " (ID: " + itemId + ", Requirement: " + requirement + " " + costText + ", Other IDs: " + otherItemIds + ")";
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
				costText.equals(that.costText) &&
				otherItemIds.equals(that.otherItemIds);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(itemId, name, requirement, costText, costItemId, otherItemIds);
	}

	// ---------------------------
	// Public static CostEntry class
	// ---------------------------
	public static class CostEntry
	{
		private final String itemName;
		private final int amount;
		private final int itemId;

		public CostEntry(String itemName, int amount, int itemId)
		{
			this.itemName = itemName;
			this.amount = amount;
			this.itemId = itemId;
		}

		public String getItemName() { return itemName; }
		public int getAmount() { return amount; }
		public int getItemId() { return itemId; }
	}

}