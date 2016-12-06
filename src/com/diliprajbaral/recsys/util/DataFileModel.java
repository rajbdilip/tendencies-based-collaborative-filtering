package com.diliprajbaral.recsys.util;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class DataFileModel {
	private File dataFile;
	private String delimiter;

	private HashMap<Long, Float> userAverages;
	private HashMap<Long, Float> itemAverages;

	// Items rated by a user
	private HashMap<Long, HashMap<Long, Float>> userRatings;

	// Users who has rated an item
	private HashMap<Long, HashMap<Long, Float>> itemRatings;

	public DataFileModel(File dataFile) throws Exception {
		this(dataFile, "\t");
	}

	public DataFileModel(File dataFile, String delimiter) throws Exception {
		if (!dataFile.exists() || dataFile.isDirectory()) {
			throw new FileNotFoundException();
		}

		this.dataFile = dataFile;
		this.delimiter = delimiter;
		buildModel();
		calculateMeans();
	}

	private void buildModel() throws IOException {
		userRatings = new HashMap<Long, HashMap<Long, Float>>();
		itemRatings = new HashMap<Long, HashMap<Long, Float>>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile)));
		String line;

		while ((line = reader.readLine()) != null) {
			String[] ratings = line.split(this.delimiter);

			if (ratings.length < 3) {
				throw new IOException("Data format not correct");
			}

			long userID = Long.parseLong(ratings[0]);
			long itemID = Long.parseLong(ratings[1]);
			float rating = Float.parseFloat(ratings[2]);

			// Update userRatings
			HashMap<Long, Float> userRating;
			if (!userRatings.containsKey(userID)) {
				userRating = new HashMap<Long, Float>();
				userRatings.put(userID, userRating);
			} else {
				userRating = userRatings.get(userID);
			}
			userRating.put(itemID, rating);

			// Update itemRatings
			HashMap<Long, Float> itemRating;
			if (!itemRatings.containsKey(itemID)) {
				itemRating = new HashMap<Long, Float>();
				itemRatings.put(itemID, itemRating);
			} else {
				itemRating = itemRatings.get(itemID);
			}
			itemRating.put(userID, rating);
		}
	}

	public float getRating(long userID, long itemID) {
		if (!userRatings.containsKey(userID) || !userRatings.get(userID).containsKey(itemID)) {
			return Float.MIN_VALUE;
		}
		return userRatings.get(userID).get(itemID);
	}

	private void calculateMeans() throws Exception {
		userAverages = new HashMap<Long, Float>(this.getNumUsers());
		itemAverages = new HashMap<Long, Float>(this.getNumItems());

		// USER MEAN
		ArrayList<Long> trainingUsers = this.getUserIDs();
		for (long userID : trainingUsers) {
			ArrayList<Long> itemIDs = this.getItemIDsFromUser(userID);

			float sum = 0F;
			int count = 0;

			for (long itemID : itemIDs) {
				sum += this.getRating(userID, itemID);
				count++;
			}
			userAverages.put(userID, sum / (float) count);
		}

		// ITEM MEAN
		ArrayList<Long> trainingItems = this.getItemIDs();
		for (long itemID : trainingItems) {
			ArrayList<Long> userIDs = this.getUserIDsFromItem(itemID);

			float sum = 0F;
			int count = 0;

			for (long userID : userIDs) {
				sum += this.getRating(userID, itemID);
				count++;
			}

			itemAverages.put(itemID, sum / (float) count);
		}
	}

	public int getNumUsers() {
		return userRatings.size();
	}

	public int getNumItems() {
		return itemRatings.size();
	}

	public ArrayList<Long> getUserIDs() {
		return new ArrayList<Long>(userRatings.keySet());
	}

	public ArrayList<Long> getItemIDs() {
		return new ArrayList<Long>(itemRatings.keySet());
	}

	public ArrayList<Long> getItemIDsFromUser(Long userID) {
		return new ArrayList<Long>(userRatings.get(userID).keySet());
	}

	public ArrayList<Long> getUserIDsFromItem(Long itemID) {
		return new ArrayList<Long>(itemRatings.get(itemID).keySet());
	}

	public float getUserAverageRating(long userID) {
		if (!userAverages.containsKey(userID)) {
			return Float.NaN;
		}
		return userAverages.get(userID);
	}

	public float getItemAverageRating(long itemID) {
		if (!itemAverages.containsKey(itemID)) {
			return Float.NaN;
		}
		return itemAverages.get(itemID);
	}

}

