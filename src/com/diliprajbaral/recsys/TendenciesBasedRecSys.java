package com.diliprajbaral.recsys;

import java.text.DecimalFormat;
import java.util.*;

import com.diliprajbaral.recsys.util.*;

public class TendenciesBasedRecSys {
    private DataFileModel dataModel;

    private HashMap<Long, Float> userTendencies;
    private HashMap<Long, Float> itemTendencies;

    private HashMap<Long, TreeMap<Long, Float>> predictions;
    private HashMap<Long, TreeMap<Float, ArrayList<Long>>> predictedRatings;

    private float beta;

    public TendenciesBasedRecSys(DataFileModel dataModel) throws Exception {
        this(dataModel, 0.5F);
    }

    public TendenciesBasedRecSys(DataFileModel dataModel, float beta) throws Exception {
        this.beta = beta;
        this.dataModel = dataModel;

        calculateTendencies();
        generatePredictions();
    }

    public void changeBeta(float beta) throws Exception {
        this.beta = beta;
        generatePredictions();
    }

    private void calculateTendencies() throws Exception {
        System.out.println("Calculating tendencies..");
        // CALCULATE USER TENDENCIES
        userTendencies = new HashMap<Long, Float>(dataModel.getNumUsers());
        itemTendencies = new HashMap<Long, Float>(dataModel.getNumItems());

        ArrayList<Long> trainingUsers = dataModel.getUserIDs();
        for (long userID : trainingUsers) {
            ArrayList<Long> itemIDs = dataModel.getItemIDsFromUser(userID);

            float sum = 0F;
            int count = 0;

            for (long itemID : itemIDs) {
                sum += dataModel.getRating(userID, itemID) - dataModel.getItemAverageRating(itemID);
                count++;
            }
            userTendencies.put(userID, sum / (float) count);
        }

        // CALCULATE ITEM TENDENCIES
        ArrayList<Long> trainingItems = dataModel.getItemIDs();
        for (long itemID : trainingItems) {
            ArrayList<Long> userIDs = dataModel.getUserIDsFromItem(itemID);

            float sum = 0F;
            int count = 0;

            for (long userID : userIDs) {
                sum += dataModel.getRating(userID, itemID) - dataModel.getUserAverageRating(userID);
                count++;
            }

            itemTendencies.put(itemID, sum / (float) count);
        }
        System.out.println("Done.");
    }

    private void generatePredictions() throws Exception {
        System.out.println("Calculating predictions..");

        predictions = new HashMap<Long, TreeMap<Long, Float>>(dataModel.getNumUsers());
        predictedRatings = new HashMap<Long, TreeMap<Float, ArrayList<Long>>>(dataModel.getNumUsers());

        ArrayList<Long> trainingUsers = dataModel.getUserIDs();
        for (long userID : trainingUsers) {
            predictions.put(userID, new TreeMap<Long, Float>());
            predictedRatings.put(userID, new TreeMap<Float, ArrayList<Long>>(Collections.<Float>reverseOrder()));

            ArrayList<Long> trainingItems = dataModel.getItemIDs();
            for (long itemID : trainingItems) {
                float predictedRating = calculatePredictions(userID, itemID);

                predictions.get(userID).put(itemID, predictedRating);

                TreeMap<Float, ArrayList<Long>> userRatings = predictedRatings.get(userID);
                if (!userRatings.containsKey(predictedRating)) {
                    userRatings.put(predictedRating, new ArrayList<Long>());
                }
                userRatings.get(predictedRating).add(itemID);
            }
        }
        System.out.println("Done.");
    }

    private float calculatePredictions(long userID, long itemID) {
        if (dataModel.getRating(userID, itemID) != Float.MIN_VALUE) {
            return dataModel.getRating(userID, itemID);
        }

        if (!userTendencies.containsKey(userID) || !itemTendencies.containsKey(itemID)) {
            return Float.MIN_VALUE;
        }

        float uT = userTendencies.get(userID);
        float iT = itemTendencies.get(itemID);
        float uA = dataModel.getUserAverageRating(userID);
        float iA = dataModel.getItemAverageRating(itemID);

        float predictedRating = 0F;

        if (uT > 0 && iT > 0) {
            // Both positive
            predictedRating = Math.max(uA + iT, iA + uT);
        } else if (uT < 0 && iT < 0) {
            predictedRating = Math.min(uA + iT, iA + uT);
        } else if (uT < 0 && iT > 0) {
            predictedRating = Math.min(Math.max(uA, (iA + uT) * beta + (uA + iT) * (1 - beta)), iA);
        } else if (uT > 0 && iT < 0) {
            predictedRating = iA * beta + uA * (1 - beta);
        }

        return formatRating(predictedRating);
    }

    public float predictRating(long userID, long itemID) {
        if (!predictions.containsKey(userID) || !predictions.get(userID).containsKey(itemID)) {
            return Float.MIN_VALUE;
        }

        return predictions.get(userID).get(itemID);
    }

    public List<RecommendedItem> recommend(long userID, int max) throws Exception {
        if (!predictedRatings.containsKey(userID)) {
            return null;
        }

        ArrayList<Long> userRatings = dataModel.getItemIDsFromUser(userID);

        List<RecommendedItem> recommendation = new ArrayList<RecommendedItem>(max);

        TreeMap<Float, ArrayList<Long>> ratingsForUser = predictedRatings.get(userID);
        for (Map.Entry<Float, ArrayList<Long>> rating : ratingsForUser.entrySet()) {
            ArrayList<Long> items = rating.getValue();
            for (int i = 0; i < items.size() && max >= 0; i++) {
                if (!userRatings.contains(items.get(i))) {
                    RecommendedItem item = new RecommendedItem(items.get(i), rating.getKey());
                    recommendation.add(item);
                    max--;
                }
            }

            if (max < 0) {
                break;
            }
        }

        return recommendation;
    }

    private float formatRating(float rating) {
        DecimalFormat df = new DecimalFormat("#.##");
        return Float.valueOf(df.format(rating));
    }

    public void displayUsers() throws Exception {
        System.out.println("USER ID\t\tMEAN\t\tTENDENCY");

        ArrayList<Long> trainingUsers = dataModel.getUserIDs();
        for (long userID : trainingUsers) {
            float mean = dataModel.getUserAverageRating(userID);
            float tendency = userTendencies.get(userID);

            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(4);
            df.setMinimumFractionDigits(4);
            System.out.println("U-" + userID + "\t\t\t" + df.format(mean) + "\t\t" + df.format(tendency));
        }
        System.out.println("");
    }

    public void displayItems() throws Exception {
        System.out.println("ITEM ID\t\tMEAN\t\tTENDENCY");

        ArrayList<Long> trainingItems = dataModel.getItemIDs();
        for (long itemID : trainingItems) {
            float mean = dataModel.getItemAverageRating(itemID);
            float tendency = itemTendencies.get(itemID);

            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(4);
            df.setMinimumFractionDigits(4);
            System.out.println("I-" + itemID + "\t\t\t" + df.format(mean) + "\t\t" + df.format(tendency));
        }
        System.out.print("");
    }
}
