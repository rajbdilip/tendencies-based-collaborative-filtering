import com.diliprajbaral.recsys.*;
import com.diliprajbaral.recsys.util.*;

import java.io.File;
import java.util.ArrayList;

public class TendenciesBasedRecSysDemo {
	private	static final String ANSI_YELLOW = "\u001B[33m";
	private static final String ANSI_BLUE = "\u001B[34m";

	public static void main(String[] args) throws Exception {
		DataFileModel dataModel = new DataFileModel(new File("data/train.dat"));
		TendenciesBasedRecSys recSys = new TendenciesBasedRecSys(dataModel);

		// Recommend items
		System.out.println(recSys.recommend(1, 20));

		evaluate(recSys);
	}

	private static void evaluate(TendenciesBasedRecSys recSys) throws Exception {
		DataFileModel testData = new DataFileModel(new File("data/test.dat"));

		int totalRatings = 0;
		float sumOfError = 0;
		float sumOfErrorGPIM = 0;
		float sumOfErrorGIM = 0;
		float sumOfErrorSquared = 0;

		float threshold = 3;		// Threshold for good/relevant items
		int totalRelevant = 0;
		int totalRelevantRetrieved = 0;

		ArrayList<Long> testUsers = testData.getUserIDs();
		for (long userID : testUsers) {
			ArrayList<Long> itemIDs = testData.getItemIDsFromUser(userID);
			for (long itemID : itemIDs) {
				float actualRating = testData.getRating(userID, itemID);
				float predictedRating = recSys.predictRating(userID, itemID);

				if (predictedRating != Float.MIN_VALUE) {
					totalRatings++;
					float error = Math.abs(predictedRating - actualRating);
					sumOfError += error;
					sumOfErrorSquared += error * error;

					if (actualRating >= threshold) {
						sumOfErrorGIM += error;
						totalRelevant++;
					}

					if (actualRating >= threshold && predictedRating >= threshold) {
						sumOfErrorGPIM += error;
						totalRelevantRetrieved++;
					}

					System.out.println(userID + "\t\t" + itemID + "\t\t" + actualRating + "\t\t" + predictedRating);
				}
			}
		}

		System.out.println("\n");
		System.out.println(ANSI_BLUE + "Total Ratings Predicted: " + ANSI_YELLOW + totalRatings);
		System.out.println(ANSI_BLUE + "Total Good/Relevant Items: " + ANSI_YELLOW + totalRelevant);
		System.out.println(ANSI_BLUE + "Total Good/Relevant Items Retrieved: " + ANSI_YELLOW + totalRelevantRetrieved);
		System.out.println("\n");
		System.out.println(ANSI_BLUE + "MAE: " + ANSI_YELLOW + sumOfError / totalRatings);
		System.out.println(ANSI_BLUE + "RMSE: " + ANSI_YELLOW + Math.sqrt(sumOfErrorSquared / totalRatings));
		System.out.println(ANSI_BLUE + "Good Items MAE: " + ANSI_YELLOW + sumOfErrorGIM / totalRelevant);
		System.out.println(ANSI_BLUE + "Good Predicted Items MAE: " + ANSI_YELLOW + sumOfErrorGPIM / totalRelevantRetrieved);
		System.out.println(ANSI_BLUE + "Precision: " + ANSI_YELLOW + (float) totalRelevantRetrieved / (float) totalRatings);
		System.out.println(ANSI_BLUE + "Recall: " + ANSI_YELLOW + (float) totalRelevantRetrieved / (float) totalRelevant);
	}
}
