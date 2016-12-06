package com.diliprajbaral.recsys.util;

public class RecommendedItem {
	private long itemId;
	private float rating;

	public RecommendedItem(long itemId, float rating) {
		this.itemId = itemId;
		this.rating = rating;
	}

	public long getItemId() {
		return itemId;
	}

	public float getRating() {
		return rating;
	}

	public String toString() {
		return "\n(" + itemId + ", " + rating + ")";
	}
}
