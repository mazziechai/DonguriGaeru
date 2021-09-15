package fr.hiku;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.imageio.ImageIO;

public class WorldRanking {

	public List<String> countryNames = new ArrayList<String>();
	public List<String> jpCountryNames = new ArrayList<String>();
	public List<Integer> playerCountries = new ArrayList<Integer>();
	public List<String> playerNames = new ArrayList<String>();
	public List<String> playerAlias = new ArrayList<String>();
	public List<Integer> playerAliasID = new ArrayList<Integer>();
	public List<Float> playerRatings = new ArrayList<Float>();
	public List<Matches> matches = new ArrayList<Matches>();
	public float coef = 0.001f;
	public static double timeCoefCoef = 1.5;

	private class Matches {
		public String name = null;
		public GregorianCalendar date = null;
		public List<Integer> player1 = new ArrayList<Integer>();
		public List<Integer> player2 = new ArrayList<Integer>();
		public List<Integer> score1 = new ArrayList<Integer>();
		public List<Integer> score2 = new ArrayList<Integer>();

		public void addMatch(String player1, String player2, int score1, int score2) {
			addMatch(getPlayer(player1), getPlayer(player2), score1, score2);
		}

		public void addMatch(int player1, int player2, int score1, int score2) {
			this.player1.add(player1);
			this.player2.add(player2);
			this.score1.add(score1);
			this.score2.add(score2);
		}

		public void apply() {
			for (int i = 0; i < player1.size(); i++) {
				//float points1 = (float) Math.sqrt(playerRatings.get(player2.get(i)) / playerRatings.get(player1.get(i))) * score1.get(i);
				float points1 = getPointsForPlayer1(i);
				if (points1 > 0) {
					playerRatings.set(player1.get(i), playerRatings.get(player1.get(i)) * (1 + points1));
					playerRatings.set(player2.get(i), playerRatings.get(player2.get(i)) / (1 + points1));
				} else {
					playerRatings.set(player1.get(i), playerRatings.get(player1.get(i)) / (1 - points1));
					playerRatings.set(player2.get(i), playerRatings.get(player2.get(i)) * (1 - points1));
				}
				//System.out.println(playerRatings.get(player2.get(i))/playerRatings.get(player1.get(i)));
			}
		}

		public float getPointsForPlayer1(int match) {
			float points1 = (float) Math.sqrt(playerRatings.get(player2.get(match)) / playerRatings.get(player1.get(match))) * score1.get(match);
			points1 -= Math.sqrt(playerRatings.get(player1.get(match)) / playerRatings.get(player2.get(match))) * score2.get(match);
			points1 *= coef * getTimeCoef() * getLvDifCoef(playerRatings.get(player1.get(match)), playerRatings.get(player2.get(match)));
			return points1;
		}

		/*public float getTimeCoef() {
			return (float) Math.max(0, 1.0 - (System.currentTimeMillis() - date.getTimeInMillis()) / (timeCoefCoef * 365.25 * 24 * 3600 * 1000));
		}*/
		//V2
		public float getTimeCoef() {
			return (float) (1.0 / (1.0 + Math.pow((System.currentTimeMillis() - date.getTimeInMillis()) / (365.25 * 24 * 3600 * 1000), 2) * timeCoefCoef));
		}

		public float getLvDifCoef(float r1, float r2) {
			return r1 > r2 ? r2 / r1 : r1 / r2;
		}

		public void printMatches() {
			for (int i = 0; i < player1.size(); i++) {
				System.out.println(playerNames.get(player1.get(i)) + " " + score1.get(i) + " " + score2.get(i) + " " + playerNames.get(player2.get(i)));
			}
		}

	}

	public void printAllMatchesRaw() {
		String previousDate = "";
		for(int i = 0; i<matches.size();i++) {
			String newDate = stringFromDate(matches.get(i).date);
			if(!newDate.equals(previousDate)) {
				System.out.println();
				System.out.println(newDate);
				previousDate = newDate;
			}
			
			matches.get(i).printMatches();
		}
	}
	
	public int getPlayer(String name) {
		if (!playerNames.contains(name)) {
			if (playerAlias.contains(name)) {
				return playerAliasID.get(playerAlias.indexOf(name));
			}
			playerNames.add(name);
			playerRatings.add(1f);
			playerCountries.add(0);
		}
		return playerNames.indexOf(name);

	}

	public boolean playerExists(String name) {
		if (!playerNames.contains(name)) {
			if (playerAlias.contains(name)) {
				return true;
			}
			return false;
		}
		return true;

	}

	public void printRatings() {
		for (int i = 0; i < playerNames.size(); i++) {
			System.out.println(playerNames.get(i) + " : " + playerRatings.get(i));
		}
	}

	public void printOrderedRatings() {
		int[] ordered = getOrderedRatings();
		for (int i = 0; i < playerNames.size(); i++) {
			System.out.println(playerNames.get(ordered[i]) + " : " + playerRatings.get(ordered[i]));
		}
	}

	public void printOrderedSimulations(int ft, String player) {
		int playerID = getPlayer(player);
		int[] ordered = getOrderedRatings();
		for (int i = 0; i < playerNames.size(); i++) {
			float r1 = playerRatings.get(ordered[i]);
			float r2 = playerRatings.get(playerID);
			if (r1 > r2) {
				System.out.println(playerNames.get(ordered[i]) + " " + ft + " - " + (int) (r2 / r1 * ft) + " " + player);
			} else {
				System.out.println(playerNames.get(ordered[i]) + " " + (int) (r1 / r2 * ft) + " - " + ft + " " + player);
			}
		}
	}

	public int[] getOrderedRatings() {
		int[] ordered = new int[playerRatings.size()];
		for (int i = 0; i < ordered.length; i++) {
			ordered[i] = i;
		}
		for (int i = 0; i < ordered.length; i++) {
			float biggest = Float.MIN_VALUE;
			int biggestj = 0;

			for (int j = i; j < ordered.length; j++) {

				if (biggest < playerRatings.get(ordered[j])) {
					biggest = playerRatings.get(ordered[j]);
					biggestj = j;
				}
			}
			int ex = ordered[i];
			ordered[i] = ordered[biggestj];
			ordered[biggestj] = ex;
		}
		return ordered;

	}

	public void sortMatchesByDate() {
		for (int i = 0; i < matches.size(); i++) {
			int min = i;
			for (int j = i + 1; j < matches.size(); j++) {
				if (matches.get(j).date.before(matches.get(min).date)) {
					min = j;
				}
			}
			if (min != i) {
				Matches a = matches.get(i);
				matches.set(i, matches.get(min));
				matches.set(min, a);
			}

		}
	}

	public BufferedImage getMatchupMap(double maxDif, double contrast, int size) {
		List<Double> player1Rating = new ArrayList<Double>();
		List<Double> player2Rating = new ArrayList<Double>();
		List<Float> player1Score = new ArrayList<Float>();
		List<Float> matchWeight = new ArrayList<Float>();
		for (int i = 0; i < matches.size(); i++) {
			for (int j = 0; j < matches.get(i).player1.size(); j++) {
				player1Rating.add(Math.log(playerRatings.get(matches.get(i).player1.get(j))));
				player2Rating.add(Math.log(playerRatings.get(matches.get(i).player2.get(j))));
				player1Score.add(matches.get(i).getPointsForPlayer1(j));
				matchWeight.add((matches.get(i).score1.get(j) + matches.get(i).score2.get(j))
						* matches.get(i).getLvDifCoef(playerRatings.get(matches.get(i).player1.get(j)), playerRatings.get(matches.get(i).player2.get(j))));
			}
		}

		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for (int i = 0; i < player1Rating.size(); i++) {
			if (player1Rating.get(i) < min)
				min = player1Rating.get(i);
			if (player2Rating.get(i) < min)
				min = player2Rating.get(i);
			if (player1Rating.get(i) > max)
				max = player1Rating.get(i);
			if (player2Rating.get(i) > max)
				max = player2Rating.get(i);
		}
		BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {

				double average = 0;
				double p1Wanted = (max - min) * i / (double) size + min;
				double p2Wanted = (max - min) * j / (double) size + min;
				double totalScore = 0;
				double weight = 0;
				for (int p = 0; p < player1Rating.size(); p++) {
					double dif1 = Math.abs(player1Rating.get(p) - p1Wanted);
					double dif2 = Math.abs(player2Rating.get(p) - p2Wanted);
					if (dif1 * dif1 + dif2 * dif2 <= maxDif * maxDif) {
						double w = matchWeight.get(p) * (maxDif * maxDif - dif1 * dif1 - dif2 * dif2);
						totalScore += player1Score.get(p) * w;
						weight += w;
					}
					dif1 = Math.abs(player2Rating.get(p) - p1Wanted);
					dif2 = Math.abs(player1Rating.get(p) - p2Wanted);
					if (dif1 * dif1 + dif2 * dif2 <= maxDif * maxDif) {
						double w = matchWeight.get(p) * (maxDif * maxDif - dif1 * dif1 - dif2 * dif2);
						totalScore -= player1Score.get(p) * w;
						weight += w;
					}

				}
				average = totalScore / weight;

				if (weight == 0) {
					bi.setRGB(i, j, new Color(0, 0, 0).getRGB());
				} else {
					int r = 255 + (int) (average > 0.0 ? average * (-contrast) : 0);
					int g = 255 + (int) (average < 0.0 ? average * (contrast) : 0);
					int b = 255;
					if (r < 0)
						r = 0;
					if (g < 0)
						g = 0;
					bi.setRGB(i, j, new Color(r, g, b).getRGB());
				}

			}
		}

		return bi;
	}

	public void playMatches(int times) {
		for (int i = 0; i < times; i++) {
			for (int j = 0; j < matches.size(); j++) {
				matches.get(j).apply();
			}
		}
	}

	public static boolean containsTab(List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).contains("\t"))
				return true;
		}
		return false;
	}

	public static boolean multipleSpacesInLines(List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).split(" ").length > 2)
				return true;
		}
		return false;
	}

	public static List<String> linesFromFile(String file) {
		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get(file));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}

	public void loadAlias(String file) {
		List<String> lines = linesFromFile(file);
		for (int i = 0; i < lines.size(); i++) {
			String[] names = lines.get(i).split(" ");
			int player = getPlayer(names[0]);
			for (int j = 1; j < names.length; j++) {
				playerAlias.add(names[j]);
				playerAliasID.add(player);
			}
		}
	}

	public void loadCountries(String file) {
		List<String> lines = linesFromFile(file);
		countryNames.clear();
		countryNames.add("Japan");
		jpCountryNames.add("日本");
		int current = 0;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).contains(":")) {
				//System.out.println(lines.get(i));
				if (lines.get(i).contains("/")) {
					countryNames.add(lines.get(i).substring(0, lines.get(i).indexOf("/")));
					jpCountryNames.add(lines.get(i).substring(lines.get(i).indexOf("/") + 1, lines.get(i).indexOf(":")));

				} else {
					countryNames.add(lines.get(i).substring(0, lines.get(i).indexOf(":")));

				}
				current++;
				//System.out.println("current is " + current + " : " + countryNames.get(current));

			} else if (!lines.get(i).isEmpty()) {
				//System.out.println(lines.get(i));
				playerCountries.set(getPlayer(lines.get(i)), current);
				//System.out.println(lines.get(i) + "'s country is " + countryNames.get(current));

			}
		}
	}

	public void loadPTGMatches(String file) {
		List<String> lines = linesFromFile(file);
		String oldDate = "";
		Matches currentMatches = null;
		for(int i = 0; i<lines.size();i++) {
			if(lines.get(i).isEmpty())continue;
			String[] elements = lines.get(i).split(",");
			String date = elements[1];

			if(!date.equals(oldDate)) {
				if(currentMatches!=null)matches.add(currentMatches);
				oldDate = date;
				//System.out.println(date.substring(0, 4));
				int year = Integer.parseInt(date.substring(0, 4));
				int month = Integer.parseInt(date.substring(5, 7))-1;
				int day = Integer.parseInt(date.substring(8, 10));
				
				currentMatches = new Matches();
				currentMatches.date = new GregorianCalendar(year, month, day);
			}
			String player1 = elements[2];
			String player2 = elements[3];
			int score1 = Integer.parseInt(elements[4]);
			int score2 = Integer.parseInt(elements[5]);
			currentMatches.addMatch(player1, player2, score1, score2);
		}
		if(currentMatches!=null)matches.add(currentMatches);
	}
	public void loadMatches(String file) {
		//System.out.println(file);
		List<String> lines = linesFromFile(file);

		int currentLine = 0;
		while (currentLine < lines.size()) {
			if (lines.get(currentLine).startsWith("//")) {
				currentLine++;
				continue;
			}
			if (lines.get(currentLine).contains("/")) {
				GregorianCalendar date = dateFromString(lines.get(currentLine));
				int begin = currentLine + 1;
				while (true) {
					currentLine++;
					if (currentLine >= lines.size() || lines.get(currentLine).contains("/")) {
						Matches matches = new Matches();
						matches.date = date;
						matches.name = file.substring(file.lastIndexOf("Raw/") + 4, file.indexOf("."));
						addMatches(lines.subList(begin, currentLine), matches);
						this.matches.add(matches);
						break;
					}
				}
			} else {
				System.out.println(file);
				currentLine++;
			}
		}

	}

	public static void addMatches(List<String> lines, Matches matches) {
		if (containsTab(lines)) {
			// League Matches
			addFromLeagueMatches(lines, matches);
		} else if (multipleSpacesInLines(lines)) {
			// Match List
			addFromMatchList(lines, matches);
		} else {
			// League Points
			addFromLeaguePoints(lines, matches);
		}

	}

	public static void addFromMatchList(List<String> lines, Matches matches) {

		for (int i = 0; i < lines.size(); i++) {
			if (!lines.get(i).contains(" "))
				continue;

			String line = lines.get(i);
			//System.out.println(line);
			String player1 = line.substring(0, line.indexOf(' '));
			line = line.substring(line.indexOf(' ') + 1);
			
			int score1 = Integer.parseInt(line.substring(0, line.indexOf(' ')));
			line = line.substring(line.indexOf(' ') + 1);

			int score2 = Integer.parseInt(line.substring(0, line.indexOf(' ')));
			line = line.substring(line.indexOf(' ') + 1);
			
			String player2 = line;

			matches.addMatch(player1, player2, score1, score2);
			if (player1.equals("") || player2.equals("")) {
				System.out.println(player1 + " " + score1 + " " + score2 + " " + player2);
			}
		}

	}

	public static void removeSpaces(List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			lines.set(i, lines.get(i).replace(" ", ""));
		}
	}

	public static void addFromLeagueMatches(List<String> lines, Matches matches) {
		int begin = 0;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).isEmpty()) {
				addFromOneLeagueMatches(lines.subList(begin, i), matches);
				i++;
				begin = i;
			}
		}
		addFromOneLeagueMatches(lines.subList(begin, lines.size()), matches);
	}

	public static void addFromOneLeagueMatches(List<String> lines, Matches matches) {
		removeSpaces(lines);
		String[] playerNames = new String[lines.size()];

		for (int i = 0; i < lines.size(); i++) {
			playerNames[i] = lines.get(i).substring(0, lines.get(i).indexOf('\t'));
		}
		for (int i = 1; i < lines.size(); i++) {
			String[] scores = lines.get(i).split("\t");
			for (int j = 0; j < i; j++) {
				if (scores.length > j + 1 && scores[j + 1].matches("[0-9]+-[0-9]+")) {
					int score1 = Integer.parseInt(scores[j + 1].substring(0, scores[j + 1].indexOf('-')));
					int score2 = Integer.parseInt(scores[j + 1].substring(scores[j + 1].indexOf('-') + 1));
					//System.out.println(playerNames[i] + " " + score1 + " - " + score2 +" " +playerNames[j]);
					matches.addMatch(playerNames[i], playerNames[j], score1, score2);
				} else if (scores.length > j + 1 && scores[j + 1].matches("[0-9]+")) {
					String[] scores2 = lines.get(j).split("\t");
					if (scores2.length > i + 1) {
						int score1 = Integer.parseInt(scores[j + 1]);
						int score2 = Integer.parseInt(scores2[i + 1]);
						//System.out.println(playerNames[i] + " " + score1 + " - " + score2 + " " + playerNames[j]);
						matches.addMatch(playerNames[i], playerNames[j], score1, score2);
					}

				}
			}
		}
	}

	public static void addFromLeaguePoints(List<String> lines, Matches matches) {
		int ft = Integer.parseInt(lines.get(0));
		lines = lines.subList(1, lines.size());
		String[] playerNames = new String[lines.size()];
		int[] playerGA = new int[lines.size()];
		for (int i = 0; i < lines.size(); i++) {
			playerNames[i] = lines.get(i).substring(0, lines.get(i).indexOf(' '));
			playerGA[i] = Integer.parseInt(lines.get(i).substring(lines.get(i).indexOf(' ') + 1));
		}
		double[] relativePower = new double[lines.size()];
		for (int i = 0; i < relativePower.length; i++) {
			if (playerGA[i] > 0) {
				relativePower[i] = (double) (ft * (relativePower.length - 1)) / (ft * (relativePower.length - 1) - playerGA[i]);
			} else {
				relativePower[i] = (double) (ft * (relativePower.length - 1) + playerGA[i]) / (ft * (relativePower.length - 1));
			}
		}
		for (int i = 1; i < lines.size(); i++) {
			for (int j = 0; j < i; j++) {
				if (relativePower[i] > relativePower[j]) {
					matches.addMatch(playerNames[i], playerNames[j], ft, (int) Math.round(relativePower[j] / relativePower[i] * ft));
				} else {
					matches.addMatch(playerNames[i], playerNames[j], (int) Math.round(relativePower[i] / relativePower[j] * ft), ft);

				}
			}
		}
		//matches.printMatches();
	}

	public static GregorianCalendar dateFromString(String str) {
		if (str.contains("("))
			str = str.substring(0, str.indexOf(" ("));
		int day = Integer.parseInt(str.substring(0, str.indexOf('/')));
		int month = Integer.parseInt(str.substring(str.indexOf('/') + 1, str.lastIndexOf('/'))) - 1;
		int year = 2000 + Integer.parseInt(str.substring(str.lastIndexOf('/') + 1));

		return new GregorianCalendar(year, month, day);
	}

	public static String reverseStringFromDate(GregorianCalendar date) {
		String day = date.get(Calendar.DAY_OF_MONTH) + "";
		if (day.length() == 1)
			day = "0" + day;
		String month = (date.get(Calendar.MONTH) + 1) + "";
		if (month.length() == 1)
			month = "0" + month;
		String year = (date.get(Calendar.YEAR) - 2000) + "";
		return year + "/" + month + "/" + day;

	}
	public static String stringFromDate(GregorianCalendar date) {
		String day = date.get(Calendar.DAY_OF_MONTH) + "";
		if (day.length() == 1)
			day = "0" + day;
		String month = (date.get(Calendar.MONTH) + 1) + "";
		if (month.length() == 1)
			month = "0" + month;
		String year = (date.get(Calendar.YEAR) - 2000) + "";
		return day + "/" + month + "/" + year;

	}

	public void addSingleTestMatch(String player1, String player2, int score1, int score2) {
		Matches match = new Matches();
		match.date = new GregorianCalendar();
		match.addMatch(player1, player2, score1, score2);
		matches.add(match);
	}

	public void addPlayer(String name, float rating) {
		addPlayer(name, rating, "Japan");
	}

	public void addPlayer(String name, float rating, String country) {
		playerNames.add(name);
		if (countryNames.indexOf(country) == -1) {
			countryNames.add(country);
		}
		playerCountries.add(countryNames.indexOf(country));
		playerRatings.add(rating);
	}

	public float[][] constructMatchupStrengths() {
		float[][] strengths = new float[playerNames.size()][playerNames.size()];
		for (int i = 0; i < strengths.length; i++) {
			for (int j = 0; j < strengths[i].length; j++) {
				strengths[i][j] = 0.0f;
			}
		}
		for (Matches m : matches) {
			for (int i = 0; i < m.player1.size(); i++) {
				float strToAdd = playerRatings.get(m.player1.get(i)) / playerRatings.get(m.player2.get(i));
				if (strToAdd > 1)
					strToAdd = 1 / strToAdd;

				strToAdd *= m.getTimeCoef();
				strToAdd *= m.score1.get(i) + m.score2.get(i);
				strengths[m.player1.get(i)][m.player2.get(i)] += strToAdd;
				strengths[m.player2.get(i)][m.player1.get(i)] += strToAdd;

			}
		}
		for (int i = 0; i < strengths.length; i++) {
			for (int j = 0; j < strengths[i].length; j++) {
				strengths[i][j] = (float) Math.min(Math.sqrt(strengths[i][j]) / 10, 1f);
			}
		}
		return strengths;
	}

	//static int time1 = 0;
	//static int time2 = 0;
	//static int times = 0;
	public float[] constructRatingCertaintyFromPlayer(int player, float[][] matchupStrengths) {
		float[] currentCertainty = new float[matchupStrengths.length];
		for (int i = 0; i < 3; i++) {
			//long time = System.currentTimeMillis();

			currentCertainty[player] = 1f;
			float[] newCertainty = new float[matchupStrengths.length];
			for (int p1 = 0; p1 < matchupStrengths.length; p1++) {
				for (int p2 = 0; p2 < matchupStrengths.length; p2++) {
					newCertainty[p2] += currentCertainty[p1] * matchupStrengths[p1][p2];
				}
			}
			//time1+= (System.currentTimeMillis()-time);
			//time = System.currentTimeMillis();

			for (int p = 0; p < newCertainty.length; p++) {
				//newCertainty[p] = Math.pow(newCertainty[p],0.9);
				//newCertainty[p] *= 0.5;
				newCertainty[p] = Math.min(newCertainty[p] / 2, 1f);
			}
			currentCertainty = newCertainty;
			//time2+= (System.currentTimeMillis()-time);
			//times++;
		}
		//	System.out.println("1 : " + (float)time1/times);
		//System.out.println("2 : " + (float)time2/times);
		return currentCertainty;
	}

	public boolean[] accessibleFromPlayerList(int player, float[][] matchupStrengths) {
		boolean[] accessible = new boolean[matchupStrengths.length];
		accessible[player] = true;
		boolean ex = true;
		//int rounds = 0;
		while (ex) {
			//rounds++;
			ex = false;
			for (int p1 = 0; p1 < matchupStrengths.length; p1++) {
				for (int p2 = 0; p2 < matchupStrengths.length; p2++) {
					if (accessible[p1] && matchupStrengths[p1][p2] > 0.0 && !accessible[p2]) {
						accessible[p2] = true;
						ex = true;
					}

				}
			}
		}
		return accessible;
	}

	public float[] constructAllRatingCertainties(float[][] matchupStrengths) {
		float[] certainties = new float[matchupStrengths.length];
		for (int player = 0; player < matchupStrengths.length; player++) {
			float[] playerCertainties = constructRatingCertaintyFromPlayer(player, matchupStrengths);
			for (int p2 = 0; p2 < playerCertainties.length; p2++) {
				certainties[p2] += playerCertainties[p2];
			}
		}
		return certainties;
	}

	public float[] constructAllRatingCertaintiesV2(float[][] matchupStrengths) {
		float[] certainties = new float[matchupStrengths.length];
		for (int p1 = 0; p1 < matchupStrengths.length; p1++) {
			certainties[p1] = 0;
			for (int p2 = 0; p2 < matchupStrengths[p1].length; p2++) {

				certainties[p1] += matchupStrengths[p1][p2];
			}
		}
		return certainties;
	}

	public String round(float f, int dec) {
		f *= Math.pow(10, dec);
		int i = Math.round(f);
		String s = (int) (i / Math.pow(10, dec)) + ".";
		i %= Math.pow(10, dec);
		for (int n = 0; n < dec - ((i + "").length()); n++) {
			s += "0";
		}
		s += i;

		return s;
	}

	public String roundc(float f, int dec) {
		f *= Math.pow(10, dec);
		int i = Math.round(f);
		String s = (int) (i / Math.pow(10, dec)) + ".";
		i %= Math.pow(10, dec);
		for (int n = 0; n < dec - ((i + "").length()); n++) {
			s += "0";
		}
		s += i;

		return s.replace('.', ',');
	}

	public void printOrderedRatingsWithCertainty() {
		int[] ordered = getOrderedRatings();
		float[] c = constructAllRatingCertainties(constructMatchupStrengths());
		//float[] c = constructRatingCertaintyFromPlayer(getPlayer("ペルシャ"),constructMatchupStrengths());
		for (int i = 0; i < playerNames.size(); i++) {
			if (c[ordered[i]] > 0)
				System.out.println(playerNames.get(ordered[i]) + " : " + round(playerRatings.get(ordered[i]), 2) + " (" + round(c[ordered[i]], 1) + ")");
		}
	}

	public void printOrderedRatingsWithCertainty(String... names) {
		List<String> names2 = new ArrayList<String>();
		for (int i = 0; i < names.length; i++)
			names2.add(names[i]);
		int[] ordered = getOrderedRatings();
		float[] c = constructAllRatingCertainties(constructMatchupStrengths());
		//float[] c = constructRatingCertaintyFromPlayer(getPlayer("ペルシャ"),constructMatchupStrengths());
		for (int i = 0; i < playerNames.size(); i++) {
			if (names2.contains(playerNames.get(ordered[i])))
				System.out.println(playerNames.get(ordered[i]) + " : " + round(playerRatings.get(ordered[i]), 2) + " (" + round(c[ordered[i]], 1) + ")");
		}
	}

	public void printOrderedRatingsWithCertainty(boolean[] show) {
		int[] ordered = getOrderedRatings();
		float[] c = constructAllRatingCertaintiesV2(constructMatchupStrengths());
		//float[] c = constructRatingCertaintyFromPlayer(getPlayer("ペルシャ"),constructMatchupStrengths());
		for (int i = 0; i < playerNames.size(); i++) {
			if (show[ordered[i]] && c[ordered[i]] > 1)
				System.out.println(playerNames.get(ordered[i]) + " : " + round(playerRatings.get(ordered[i]), 2) + " (" + round(c[ordered[i]], 1) + ")");
		}
	}

	public void printOrderedRatingsWithCertaintyV2(boolean[] show) {
		float[] c = constructAllRatingCertaintiesV2(constructMatchupStrengths());
		float[] lowerRatings = new float[playerRatings.size()];
		float[] upperRatings = new float[playerRatings.size()];
		for (int i = 0; i < playerRatings.size(); i++) {
			float certainty = (float) (0.6f / Math.pow(c[i], 1.2));
			lowerRatings[i] = playerRatings.get(i) / ((1 + certainty));
			upperRatings[i] = playerRatings.get(i) * ((1 + certainty));
		}

		int[] ordered = getOrdered(lowerRatings);

		//float[] c = constructRatingCertaintyFromPlayer(getPlayer("ペルシャ"),constructMatchupStrengths());
		for (int i = 0; i < playerNames.size(); i++) {
			if (show[ordered[i]] && c[ordered[i]] > 0.1)
				System.out.println(playerNames.get(ordered[i]) + " : " + round(lowerRatings[ordered[i]], 2) + " ~ " + round(upperRatings[ordered[i]], 2));
			//else System.out.println("J'affiche pas " + playerNames.get(ordered[i]) + " ! NAH !");
		}
	}

	public void printOrderedRatingsWithCertaintyV2ForWiki(boolean[] show, boolean jpVer) {
		float[] c = constructAllRatingCertaintiesV2(constructMatchupStrengths());
		float[] lowerRatings = new float[playerRatings.size()];
		float[] upperRatings = new float[playerRatings.size()];
		for (int i = 0; i < playerRatings.size(); i++) {
			/*if (playerNames.get(i).equals("Hiku"))
				System.out.println("Hiku's certainty : " + c[i]);
			if (playerNames.get(i).equals("VoidPH"))
				System.out.println("Void's certainty : " + c[i]);
			if (playerNames.get(i).equals("Doremy"))
				System.out.println("Doremy's certainty : " + c[i]);
			if (playerNames.get(i).equals("Acliv"))
				System.out.println("Acliv's certainty : " + c[i]);
			if (playerNames.get(i).equals("Yoshi100_Aus"))
				System.out.println("Yoshi100_Aus's certainty : " + c[i]);
			if (playerNames.get(i).equals("TinkFloyd"))
				System.out.println("TinkFloyd's certainty : " + c[i]);
			if (playerNames.get(i).equals("MidLVGuy"))
				System.out.println("MidLVGuy's certainty : " + c[i]);
			if (playerNames.get(i).equals("Cicadacry"))
				System.out.println("Cicadacry's certainty : " + c[i]);*/
			float certainty = (float) (0.6f / Math.pow(c[i], 1.2));
			lowerRatings[i] = playerRatings.get(i) / ((1 + certainty));
			upperRatings[i] = playerRatings.get(i) * ((1 + certainty));
		}

		int[] ordered = getOrdered(lowerRatings);

		//float[] c = constructRatingCertaintyFromPlayer(getPlayer("ペルシャ"),constructMatchupStrengths());
		if (jpVer) {
			System.out.println("== ランキング ==");
			System.out.println("Last update: " + reverseStringFromDate(new GregorianCalendar()));
			System.out.println("{| class=\"wikitable sortable\"");
			System.out.println("|-");
			System.out.println("! scope=\"col\" | ランク");
			System.out.println("! scope=\"col\" | 名前");
			System.out.println("! scope=\"col\" | 国");
			System.out.println("! scope=\"col\" | 下界レート");
			System.out.println("! scope=\"col\" | 中間レート");
			System.out.println("! scope=\"col\" | 上界レート");

		} else {
			System.out.println("== Ranking ==");
			System.out.println("Last update: " + reverseStringFromDate(new GregorianCalendar()));
			System.out.println("{| class=\"wikitable sortable\"");
			System.out.println("|-");
			System.out.println("! scope=\"col\" | Rank");
			System.out.println("! scope=\"col\" | Player");
			System.out.println("! scope=\"col\" | Country");
			System.out.println("! scope=\"col\" | Min rating");
			System.out.println("! scope=\"col\" | Med rating");
			System.out.println("! scope=\"col\" | Max rating");
		}
		int amount = 0;
		for (int i = 0; i < playerNames.size(); i++) {
			if (show[ordered[i]] && c[ordered[i]] > 0.1) {
				amount++;
				System.out.println("|-");
				System.out.println("| " + amount + " || " + playerNames.get(ordered[i]) + " || "
						+ (jpVer ? jpCountryNames : countryNames).get(playerCountries.get(ordered[i])) + " || " + round(lowerRatings[ordered[i]], 2) + " || "
						+ round(playerRatings.get(ordered[i]), 2) + " || " + round(upperRatings[ordered[i]], 2));
			}
			//else System.out.println("J'affiche pas " + playerNames.get(ordered[i]) + " ! NAH !");
		}
		System.out.println("|}");
	}

	public void printCertainty(float[] certainty) {
		int[] ordered = getOrdered(certainty);
		System.out.println(ordered[0]);
		for (int i = 0; i < certainty.length; i++) {

			System.out.println(playerNames.get(ordered[i]) + " : " + certainty[ordered[i]]);
		}
	}

	public int[] getOrdered(float[] things) {
		int[] ordered = new int[things.length];
		for (int i = 0; i < ordered.length; i++) {
			ordered[i] = i;
		}

		for (int i = 0; i < ordered.length; i++) {
			float biggest = Float.NEGATIVE_INFINITY;
			int biggestj = 0;
			for (int j = i; j < ordered.length; j++) {

				if (biggest < things[ordered[j]]) {
					biggest = things[ordered[j]];
					biggestj = j;
				} else {}
			}
			int ex = ordered[i];
			ordered[i] = ordered[biggestj];
			ordered[biggestj] = ex;
		}

		return ordered;

	}

	public int[] getOrdered(double[] things) {
		int[] ordered = new int[things.length];
		for (int i = 0; i < ordered.length; i++) {
			ordered[i] = i;
		}

		for (int i = 0; i < ordered.length; i++) {
			double biggest = Float.NEGATIVE_INFINITY;
			int biggestj = 0;
			for (int j = i; j < ordered.length; j++) {

				if (biggest < things[ordered[j]]) {
					biggest = things[ordered[j]];
					biggestj = j;
				} else {}
			}
			int ex = ordered[i];
			ordered[i] = ordered[biggestj];
			ordered[biggestj] = ex;
		}

		return ordered;

	}

	public float getTotalCertainty() {
		float[][] d = constructMatchupStrengths();
		float[] c = constructAllRatingCertainties(d);
		float total = 0f;
		for (int i = 0; i < c.length; i++) {
			total += c[i];
		}
		return total;
	}

	public float getTotalCertaintyWithMatch(int player1, int player2) {
		Matches match = new Matches();
		match.date = new GregorianCalendar();
		match.addMatch(player1, player2, 30, 30);
		matches.add(match);
		float c = getTotalCertainty();
		matches.remove(match);
		return c;
	}

	public void printUsefulMatchup() {
		float c = getTotalCertainty();

		float[][] ms = constructMatchupStrengths();
		for (int p1 = 0; p1 < playerNames.size(); p1++) {
			for (int p2 = 0; p2 < playerNames.size(); p2++) {
				//p1 = getPlayer("ともろん");
				p1 = (int) (Math.random() * playerNames.size());
				p2 = (int) (Math.random() * playerNames.size());

				if (playerRatings.get(p1) / playerRatings.get(p2) > 1.3 || playerRatings.get(p2) / playerRatings.get(p1) > 1.3)
					continue;
				if (ms[p1][p2] >= 0.3)
					continue;

				float c2 = getTotalCertaintyWithMatch(p1, p2);
				if (c2 - c > 5)
					System.out.println("With a match between " + playerNames.get(p1) + " and " + playerNames.get(p2) + ", certainty would rise by " + (c2 - c));
				//System.out.println("addSingleTestMatch(\"" + playerNames.get(p1) + "\", \"" + playerNames.get(p2) + "\", 30,30);");

			}
		}
	}

	public void printAllPlayerMatches(String name) {
		int player = getPlayer(name);
		for (int i = 0; i < matches.size(); i++) {
			if (matches.get(i).player1.contains(player) || matches.get(i).player2.contains(player))
				System.out.println(matches.get(i).name + " :");
			for (int j = 0; j < matches.get(i).player1.size(); j++) {
				if (matches.get(i).player1.get(j) == player) {
					System.out.println(playerNames.get(player) + " " + matches.get(i).score1.get(j) + " - " + matches.get(i).score2.get(j) + " "
							+ playerNames.get(matches.get(i).player2.get(j)));
				}
				if (matches.get(i).player2.get(j) == player) {
					System.out.println(playerNames.get(player) + " " + matches.get(i).score2.get(j) + " - " + matches.get(i).score1.get(j) + " "
							+ playerNames.get(matches.get(i).player1.get(j)));
				}
			}
		}
	}

	public void printAllMatches() {
		for (int i = 0; i < matches.size(); i++) {
			System.out.println(matches.get(i).name + " :");
			for (int j = 0; j < matches.get(i).player1.size(); j++) {
				System.out.println(playerNames.get(matches.get(i).player1.get(j)) + " " + matches.get(i).score1.get(j) + " - " + matches.get(i).score2.get(j)
						+ " " + playerNames.get(matches.get(i).player2.get(j)));
			}
		}
	}

	public void printAllMatchesForWiki() {
		System.out.println("This is the match list used for [[World Ranking]]\r\n" + "== List ==");
		for (int i = 0; i < matches.size(); i++) {
			System.out.println(matches.get(i).name + " (" + matches.get(i).date.toString() + ") :<br>");
			for (int j = 0; j < matches.get(i).player1.size(); j++) {
				System.out.println(playerNames.get(matches.get(i).player1.get(j)) + " " + matches.get(i).score1.get(j) + " - " + matches.get(i).score2.get(j)
						+ " " + playerNames.get(matches.get(i).player2.get(j)) + "<br>");
			}
			System.out.println("<br>");

		}
	}

	
	public int totalPointsPlayed(String name) {
		int player = getPlayer(name);
		int points = 0;
		for (int i = 0; i < matches.size(); i++) {
			for (int j = 0; j < matches.get(i).player1.size(); j++) {
				if (matches.get(i).player1.get(j) == player) {
					points += matches.get(i).score1.get(j);
					points += matches.get(i).score2.get(j);
				}
				if (matches.get(i).player2.get(j) == player) {
					points += matches.get(i).score1.get(j);
					points += matches.get(i).score2.get(j);
				}
			}
		}
		return points;
	}

	public int totalPointsPlayed(int player, GregorianCalendar beforeDate) {
		int points = 0;
		for (int i = 0; i < matches.size(); i++) {
			if (matches.get(i).date.before(beforeDate))
				for (int j = 0; j < matches.get(i).player1.size(); j++) {
					if (matches.get(i).player1.get(j) == player) {
						points += matches.get(i).score1.get(j);
						points += matches.get(i).score2.get(j);
					}
					if (matches.get(i).player2.get(j) == player) {
						points += matches.get(i).score1.get(j);
						points += matches.get(i).score2.get(j);
					}
				}
		}
		return points;
	}

	public int totalPointsPlayedBetweenCountries(ArrayList<Integer> countries1, ArrayList<Integer> countries2) {
		int points = 0;
		for (int i = 0; i < matches.size(); i++) {
			for (int j = 0; j < matches.get(i).player1.size(); j++) {
				if (countries1.contains(playerCountries.get(matches.get(i).player1.get(j)))
						&& countries2.contains(playerCountries.get(matches.get(i).player2.get(j)))) {
					points += matches.get(i).score1.get(j);
					points += matches.get(i).score2.get(j);
					System.out.println(playerNames.get(matches.get(i).player1.get(j)) + " " + matches.get(i).score1.get(j) + " - "
							+ matches.get(i).score2.get(j) + " " + playerNames.get(matches.get(i).player2.get(j)) + "<br>");
				}
				if (countries2.contains(playerCountries.get(matches.get(i).player1.get(j)))
						&& countries1.contains(playerCountries.get(matches.get(i).player2.get(j)))) {
					points += matches.get(i).score1.get(j);
					points += matches.get(i).score2.get(j);
					System.out.println(playerNames.get(matches.get(i).player1.get(j)) + " " + matches.get(i).score1.get(j) + " - "
							+ matches.get(i).score2.get(j) + " " + playerNames.get(matches.get(i).player2.get(j)) + "<br>");

				}
			}
		}
		return points;
	}

	public void printAllPlayerMatchesWithDelta(String name) {
		int player = getPlayer(name);
		for (int i = 0; i < matches.size(); i++) {
			if (matches.get(i).player1.contains(player) || matches.get(i).player2.contains(player)) {
				String date = matches.get(i).date.get(Calendar.YEAR) + "";
				date += "/" + (matches.get(i).date.get(Calendar.MONTH) + 1);
				date += "/" + matches.get(i).date.get(Calendar.DAY_OF_MONTH);
				System.out.println(matches.get(i).name + " (" + date + ") :");
				//System.out.println(matches.get(i).getTimeCoef());
				for (int j = 0; j < matches.get(i).player1.size(); j++) {
					if (matches.get(i).player1.get(j) == player) {
						System.out.println(playerNames.get(player) + " " + matches.get(i).score1.get(j) + " - " + matches.get(i).score2.get(j) + " "
								+ playerNames.get(matches.get(i).player2.get(j)) + " (" + (matches.get(i).getPointsForPlayer1(j) * 1000) + ")");
					}
					if (matches.get(i).player2.get(j) == player) {
						System.out.println(playerNames.get(player) + " " + matches.get(i).score2.get(j) + " - " + matches.get(i).score1.get(j) + " "
								+ playerNames.get(matches.get(i).player1.get(j)) + " (" + (-matches.get(i).getPointsForPlayer1(j) * 1000) + ")");
					}
				}
				System.out.println();
			}
		}
	}

	public void saveImg(BufferedImage bi, String loc) {
		File outputfile = new File(loc);
		try {
			ImageIO.write(bi, "png", outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public String StringFromDate(GregorianCalendar date) {
		String str = date.get(Calendar.YEAR) + "";
		str += "/" + (date.get(Calendar.MONTH) + 1);
		str += "/" + date.get(Calendar.DAY_OF_MONTH);
		return str;
	}

	public void splitAllPlayerMatches() {
		for (int i = 0; i < playerNames.size(); i++)
			splitMatches(i);
	}

	public void splitMatches(int player) {
		int pointsPlayed = 0;
		int splittedPlayers = 0;
		int currentPlayer = player;
		String until = StringFromDate(new GregorianCalendar());
		String first = "";
		for (int i = matches.size() - 1; i >= 0; i--) {
			for (int j = 0; j < matches.get(i).player1.size(); j++) {
				if (matches.get(i).player1.get(j) == player) {
					pointsPlayed += matches.get(i).score1.get(j) + matches.get(i).score2.get(j);
					matches.get(i).player1.set(j, currentPlayer);
					first = StringFromDate(matches.get(i).date);
				}
				if (matches.get(i).player2.get(j) == player) {
					pointsPlayed += matches.get(i).score1.get(j) + matches.get(i).score2.get(j);
					matches.get(i).player2.set(j, currentPlayer);
					first = StringFromDate(matches.get(i).date);
				}
			}
			if (pointsPlayed > 2000 && totalPointsPlayed(player, matches.get(i).date) > 1000) {
				//System.out.println(playerNames.get(player));
				pointsPlayed = 0;
				if (splittedPlayers > 0)
					playerNames.set(currentPlayer, playerNames.get(player) + " (" + StringFromDate(matches.get(i).date) + " - " + until + ")");
				until = StringFromDate(matches.get(i).date);
				splittedPlayers++;
				currentPlayer = playerNames.size();
				playerNames.add(playerNames.get(player) + "ToBeChanged");
				playerRatings.add(1.0f);
				playerCountries.add(playerCountries.get(player));
			}
		}
		if (splittedPlayers > 0) {
			playerNames.set(currentPlayer, playerNames.get(player) + " (" + first + " - " + until + ")");
		}
	}

	public void splitMatches(String player) {
		splitMatches(playerNames.indexOf(player));

	}

	boolean[] removeSplitted(boolean players[]) {
		for (int i = 0; i < players.length; i++) {
			if (playerNames.get(i).contains("/"))
				players[i] = false;
		}
		return players;
	}

	boolean[] removeCountry(boolean players[], String... countriesToRemove) {
		for (int i = 0; i < players.length; i++)
			for (int j = 0; j < countriesToRemove.length; j++)
				if (countryNames.get(playerCountries.get(i)).equals(countriesToRemove[j])) {
					players[i] = false;
					break;
				}

		return players;
	}

	public int simulatePuyoChampionship() {
		int[] best2 = new int[] {
				getPlayer("ヨダソウマ"), getPlayer("レイン")
		};
		int[] best4 = new int[] {
				getPlayer("delta"), getPlayer("マッキー")
		};
		int[] best8 = new int[] {
				getPlayer("live"), getPlayer("ぴぽにあ"), getPlayer("coo"), getPlayer("SAKI")
		};
		int[] qualified = new int[] {
				getPlayer("あべし"), getPlayer("和樹"), getPlayer("くまちょむ"), getPlayer("ざいろ"), getPlayer("ともくん"), getPlayer("へーょまは"), getPlayer("MATTYAN"),
				getPlayer("やなせ")
		};
		for (int i = 0; i < 8; i++) {//Shuffle qualified
			int r = (int) (Math.random() * 8);
			int q = qualified[i];
			qualified[i] = qualified[r];
			qualified[r] = q;
		}
		if (Math.random() > 0.5) { //Shuffle best4
			int a = best4[0];
			best4[0] = best4[1];
			best4[1] = a;
		}
		for (int i = 0; i < 4; i++) {//Shuffle best8
			int r = (int) (Math.random() * 4);
			int q = best8[i];
			best8[i] = best8[r];
			best8[r] = q;
		}
		int[] tournament = new int[] {
				best2[0], qualified[0], best8[0], qualified[1], best4[0], qualified[2], best8[1], qualified[3], best2[1], qualified[4], best8[2], qualified[5],
				best4[1], qualified[6], best8[3], qualified[7]
		};
		for (int i = 0; i < 4; i++) {
			int[] nextRound = new int[(int) Math.pow(2, 3 - i)];
			for (int j = 0; j < nextRound.length; j++) {
				double p1 = playerRatings.get(tournament[j * 2]);
				double p2 = playerRatings.get(tournament[j * 2 + 1]);
				double r = p1 / (p1 + p2);
				//9x^4-12x^5-50x^6+108x^7-72x^8+16x^9
				double chancesP1 = 27 * Math.pow(r, 4) - 36 * Math.pow(r, 5) - 42 * Math.pow(r, 6) + 108 * Math.pow(r, 7) - 72 * Math.pow(r, 8)
						+ 16 * Math.pow(r, 9);
				if (Math.random() < chancesP1) {
					nextRound[j] = tournament[j * 2];
				} else {
					nextRound[j] = tournament[j * 2 + 1];
				}
			}
			tournament = nextRound;
		}
		return tournament[0];
	}

	public void printChancesInChampionship(int times) {
		int[] winning = new int[playerNames.size()];
		for (int i = 0; i < times; i++) {
			winning[simulatePuyoChampionship()]++;
		}
		for (int i = 0; i < winning.length; i++) {
			if (winning[i] > 0)
				System.out.println(playerNames.get(i) + " : " + round(100 * winning[i] / (float) times, 2) + "%");
		}
	}

	public double getTeamPower(String... players) {
		double total = 0;
		for (int i = 0; i < players.length; i++) {
			if (!playerExists(players[i])) {
				System.out.println("Pas de données sur " + players[i]);
				continue;
			}
			total += playerRatings.get(getPlayer(players[i]));
		}
		return total;
	}

	public void giveEveryoneNMatches(int matchAmount, String... players) {
		int[] playerIDs = new int[players.length];
		for (int i = 0; i < players.length; i++) {
			playerIDs[i] = getPlayer(players[i]);
		}
		giveEveryoneNMatches(playerIDs, matchAmount);
	}

	public void giveEveryoneNMatches(int[] players, int matchAmount) {

		//Tant que ça ne finit pas par se remplir :
		int tries = 0;
		while (tries < 10000) {
			tries++;
			int[] matchesP1 = new int[players.length * matchAmount / 2];
			int[] matchesP2 = new int[players.length * matchAmount / 2];
			int[] played = new int[players.length];
			// Pour tous les matchs qui doivent se jouer
			int i;
			for (i = 0; i < players.length * matchAmount / 2; i++) {
				//  Prendre un joueur au hasard parmi ceux qui ont pas encore joué tous leurs matchs
				int player1 = (int) (Math.random() * players.length);
				while (played[player1] == matchAmount)
					player1 = (int) (Math.random() * players.length);
				int player2 = -1;
				//  Pour tous les joueurs qui ont pas encore joué tous leurs matchs sauf lui
				for (int j = 0; j < players.length; j++) {
					if (played[j] != matchAmount && player1 != j) {
						//  voir si ça matche, si oui le sélectionner, si non continuer
						double ratio = playerRatings.get(players[player1]) / playerRatings.get(players[j]);
						//System.out.println(playerNames.get(players[player1]) + " " + playerNames.get(players[j]) + " : " + ratio);
						if (ratio < 2 && ratio > 0.5) {
							boolean ex = false;
							for (int k = 0; k < matchesP1.length; k++) {
								if (matchesP1[k] == player1 && matchesP2[k] == j) {
									ex = true;
									break;
								} else if (matchesP2[k] == player1 && matchesP1[k] == j) {
									ex = true;
									break;
								}
							}
							if (ex)
								continue;
							player2 = j;
							break;
						} else continue;
					}
				}
				//  En sortie de boucle, si personne n'a matché, recommencer depuis le début
				if (player2 == -1)
					break;
				//  Si quelqu'un a matché, ajouter le match et continuer
				matchesP1[i] = player1;
				matchesP2[i] = player2;
				played[player1]++;
				played[player2]++;

			}
			if (i == matchesP1.length) {
				System.out.println("found a solution! " + tries + " tries");
				for (int k = 0; k < matchesP1.length; k++) {
					System.out.println(playerNames.get(players[matchesP1[k]]) + " vs " + playerNames.get(players[matchesP2[k]]));
				}
				break;
			}
		}

	}

	public void giveEveryoneNMatches(String[] players, int matchAmount, double maxDifference, String[] player1Previous, String player2Previous[]) {
		int[] playersInt = new int[players.length];
		int[] player1PreviousInt = new int[player1Previous.length];
		int[] player2PreviousInt = new int[player2Previous.length];
		for (int i = 0; i < players.length; i++) {
			playersInt[i] = getPlayer(players[i]);
			System.out.println(round(playerRatings.get(playersInt[i]), 3) + " : " + playerNames.get(playersInt[i]));
		}
		for (int i = 0; i < player1Previous.length; i++) {
			player1PreviousInt[i] = getPlayer(player1Previous[i]);
			player2PreviousInt[i] = getPlayer(player2Previous[i]);
			System.out.println(playerNames.get(player1PreviousInt[i]) + " vs " + playerNames.get(player2PreviousInt[i]));
		}
		giveEveryoneNMatches(playersInt, matchAmount, maxDifference, player1PreviousInt, player2PreviousInt);
	}

	public void giveEveryoneNMatches(int[] players, int matchAmount, double maxDifference, int[] player1Previous, int player2Previous[]) {

		//Tant que ça ne finit pas par se remplir :
		int tries = 0;
		while (tries < 10000) {
			tries++;
			int[] matchesP1 = new int[players.length * matchAmount / 2];
			int[] matchesP2 = new int[players.length * matchAmount / 2];
			int[] played = new int[players.length];
			// Pour tous les matchs qui doivent se jouer
			int i;
			for (i = 0; i < players.length * matchAmount / 2; i++) {
				//  Prendre un joueur au hasard parmi ceux qui ont pas encore joué tous leurs matchs
				int player1 = (int) (Math.random() * players.length);
				while (played[player1] == matchAmount || (matchAmount % 2 == 1 && players.length % 2 == 1 && playerNames.get(players[player1]).equals("Hiku")
						&& played[player1] == matchAmount - 1))
					player1 = (int) (Math.random() * players.length);
				int player2 = -1;
				//  Pour tous les joueurs qui ont pas encore joué tous leurs matchs sauf lui
				for (int j = 0; j < players.length; j++) {
					if (played[j] != matchAmount && player1 != j && !(matchAmount % 2 == 1 && players.length % 2 == 1
							&& playerNames.get(players[j]).equals("Hiku") && played[j] == matchAmount - 1)) {
						//  voir si ça matche, si oui le sélectionner, si non continuer
						double ratio = playerRatings.get(players[player1]) / playerRatings.get(players[j]);
						if (ratio < maxDifference && ratio > 1 / maxDifference) {
							boolean ex = false;
							for (int k = 0; k < matchesP1.length; k++) {
								if (matchesP1[k] == player1 && matchesP2[k] == j) {
									ex = true;
									break;
								} else if (matchesP2[k] == player1 && matchesP1[k] == j) {
									ex = true;
									break;
								}
							}
							for (int k = 0; k < player1Previous.length; k++) {
								if (player1Previous[k] == players[player1] && player2Previous[k] == players[j]) {
									ex = true;
									break;
								} else if (player2Previous[k] == players[player1] && player1Previous[k] == players[j]) {
									ex = true;
									break;
								}
							}
							if (ex)
								continue;
							player2 = j;
							break;
						} else continue;
					}
				}
				//  En sortie de boucle, si personne n'a matché, recommencer depuis le début
				if (player2 == -1)
					break;
				//  Si quelqu'un a matché, ajouter le match et continuer
				matchesP1[i] = player1;
				matchesP2[i] = player2;
				played[player1]++;
				played[player2]++;

			}
			if (i == matchesP1.length) {
				System.out.println("found a solution! " + tries + " tries");
				System.out.println("p1:");
				for (int k = 0; k < matchesP1.length; k++) {
					System.out.println(playerNames.get(players[matchesP1[k]]));
				}
				System.out.println("");
				System.out.println("p2:");

				for (int k = 0; k < matchesP1.length; k++) {
					System.out.println(playerNames.get(players[matchesP2[k]]));
				}
				break;
			}
		}
		System.out.println("done");

	}

	public void printRankingForLeagueSheet() {
		boolean[] show = removeCountry(removeSplitted(accessibleFromPlayerList(getPlayer("Hiku"), constructMatchupStrengths())), "Japan", "South Korea");

		float[] c = constructAllRatingCertaintiesV2(constructMatchupStrengths());
		float[] lowerRatings = new float[playerRatings.size()];
		float[] upperRatings = new float[playerRatings.size()];
		for (int i = 0; i < playerRatings.size(); i++) {
			float certainty = (float) (0.6f / Math.pow(c[i], 1.2));
			lowerRatings[i] = playerRatings.get(i) / ((1 + certainty));
			upperRatings[i] = playerRatings.get(i) * ((1 + certainty));
		}

		int[] ordered = getOrdered(lowerRatings);

		//float[] c = constructRatingCertaintyFromPlayer(getPlayer("ペルシャ"),constructMatchupStrengths());
		System.out.println("Rank;Name;Country;Min;Med;Max;Certainty");

		int amount = 0;
		for (int i = 0; i < playerNames.size(); i++) {
			if (show[ordered[i]]) {
				amount++;
				System.out.println(amount + ";" + playerNames.get(ordered[i]) + ";" + countryNames.get(playerCountries.get(ordered[i])) + ";"
						+ roundc(lowerRatings[ordered[i]], 2) + ";" + roundc(playerRatings.get(ordered[i]), 2) + ";" + roundc(upperRatings[ordered[i]], 2) + ";"
						+ roundc(c[ordered[i]], 2));
			}
			//else System.out.println("J'affiche pas " + playerNames.get(ordered[i]) + " ! NAH !");
		}

	}

	public double getBestLeagueGroups(double[] ratings, boolean[][] platforms, double goalScore) {
		int[] groups = new int[ratings.length];
		for (int i = 0; i < groups.length; i++)
			groups[i] = -1;
		boolean[] groupsOver = new boolean[ratings.length];
		int[] groupPlatforms = new int[ratings.length];
		for (int i = 0; i < groupPlatforms.length; i++)
			groupPlatforms[i] = -1;
		return tryWithLeagueGroups(ratings, groups, groupsOver, platforms, groupPlatforms, 0, goalScore);

	}

	/**
	 * 
	 * @param ratings The ratings of the players. /!\ must be sorted from highest to lowest
	 * @param groups The player groups. -1 is "not in any group yet". When first called, this needs to be -1 everywhere.
	 * @param groupsOver If the groups are closed, true. I'll be set to true when the rating ratio between the highest of the group and the current is >1.5. When first called, this needs to be false everywhere.
	 * @param platforms The platforms each player could play on. [playerID][platform].
	 * @param groupPlatfoms The platforms for each group. When first called, this needs to be -1 everywhere.
	 * @param current The current player being set. When first called, this needs to be 0.
	 * @param goalScore The maximum score we're trying to find. If the current score increases over this one, the function should stop there.
	 * @return
	 */
	public double tryWithLeagueGroups(double[] ratings, int[] groups, boolean[] groupsOver, boolean platforms[][], int groupPlatforms[], int current,
			double goalScore) {
		//System.out.println(current);

		// If current is after the end of the list, set all groups to over and return final score.
		if (current == ratings.length) {
			for (int i = 0; i < groupsOver.length; i++) {
				groupsOver[i] = true;
			}

			double score = getLeagueGroupsScore(ratings, groups, groupsOver);
			if (score < 2.4535) {
				System.out.println("solution:");
				for (int i = 0; i < groups.length; i++)
					System.out.println(groups[i]);
			}

			return score;
		}

		// Set if groups are over or not

		for (int i = 0; i < groupsOver.length; i++) {
			if (!groupsOver[i] && groupPlatforms[i] != -1) {
				int first = -1;
				for (int j = 0; j < ratings.length; j++) {
					if (groups[j] == i) {
						first = j;
						break;
					}
				}
				if (ratings[first] / ratings[current] > 1.5)
					groupsOver[i] = true;
			}
		}
		// Get the current score, if it's too much, return an infinite score.
		double currentScore = getLeagueGroupsScore(ratings, groups, groupsOver);
		if (currentScore > goalScore)
			return Double.MAX_VALUE;
		// For all current groups, try to add the current player to these, and call tryWithLeagueGroups again with goalScore+1
		// Try with a new group too
		double best = Double.MAX_VALUE;
		int currentGroup = 0;
		for (int i = 0; i < groupPlatforms.length; i++) {
			if (groupsOver[i])
				continue;
			if (groupPlatforms[i] == -1) {
				currentGroup = i;
				break;
			}
			if (!platforms[current][groupPlatforms[i]])
				continue;
			double[] newRatings = ratings.clone();
			int[] newGroups = groups.clone();
			newGroups[current] = i;
			boolean[] newGroupsOver = groupsOver.clone();
			int[] newGroupPlatforms = groupPlatforms.clone();
			double score = tryWithLeagueGroups(newRatings, newGroups, newGroupsOver, platforms, newGroupPlatforms, current + 1, goalScore);
			//System.out.println("score1: " + score);
			if (score < best)
				best = score;
		}

		for (int i = 0; i < platforms[current].length; i++) {
			if (!platforms[current][i])
				continue;
			double[] newRatings = ratings.clone();
			int[] newGroups = groups.clone();
			newGroups[current] = currentGroup;
			boolean[] newGroupsOver = groupsOver.clone();
			int[] newGroupPlatforms = groupPlatforms.clone();
			newGroupPlatforms[currentGroup] = i;

			double score = tryWithLeagueGroups(newRatings, newGroups, newGroupsOver, platforms, newGroupPlatforms, current + 1, goalScore);
			//System.out.println("score2: "+score);

			if (score < best)
				best = score;

		}

		// Get the best solution and return it
		return best;

	}

	public double getLeagueGroupsScore(double[] ratings, int[] groups, boolean[] groupsOver) {
		final double[] preferedSizes = {
				0.0, 1.0, 0.8, 0.6, 0.3, 0.2, 0.15, 0.1, 0.05
		};

		double total = 0;
		for (int i = 0; i < groups.length; i++) {
			//getting position of first player to be in the group i
			int first = 0;
			while (first < groups.length) {
				if (groups[first] == i) {
					break;
				}
				first++;
			}
			//if there's noone in i, stop
			if (first == groups.length)
				break;
			// getting position of the last player to be in the group i
			int amount = 1;
			int current = first + 1;
			int last = first;
			while (current < groups.length) {
				if (groups[current] == i) {
					//when we find a player in the same group, it's set as last
					last = current;
					amount++;
				}
				current++;
			}
			// Difference in rating. If first is 2x last, then the difference is 100%, so 1.0
			double ratingDif = ratings[first] / ratings[last] - 1;
			//System.out.println("group:"+ i + ", first:"+first+", last:"+last+", dif:"+ratingDif);
			total += ratingDif + (groupsOver[i] ? preferedSizes[amount] : 0);
			//System.out.println(total);

		}

		return total;
	}

	List<Integer> leaguePlayers = new ArrayList<Integer>();
	List<boolean[]> leaguePlatforms = new ArrayList<boolean[]>();

	public void addLeaguePlayer(String name, boolean VS, boolean Steam, boolean Switch, boolean PS4) {
		leaguePlayers.add(getPlayer(name));
		leaguePlatforms.add(new boolean[] {
				VS, Steam, Switch, PS4
		});
	}

	public double getCurrentLeagueBestGroups() {
		int[] ordered = new int[leaguePlayers.size()];
		for (int i = 0; i < ordered.length; i++) {
			ordered[i] = i;
		}
		/*
		 * for(int i = 0;i<things.length; i++){ System.out.println("things " + i
		 * + " : " + things[i]); }
		 */
		for (int i = 0; i < ordered.length; i++) {
			float biggest = Float.MIN_VALUE;
			int biggestj = 0;
			for (int j = i; j < ordered.length; j++) {
				if (biggest < playerRatings.get(leaguePlayers.get(ordered[j]))) {
					biggest = playerRatings.get(leaguePlayers.get(ordered[j]));
					biggestj = j;
					// System.out.println("pouet");
				}
			}
			// System.out.println(biggest);
			int ex = ordered[i];
			ordered[i] = ordered[biggestj];
			ordered[biggestj] = ex;
			// System.out.println("ordering : " + i + " -> " + ordered[i]);
		}

		double[] ratings = new double[leaguePlayers.size()];
		boolean[][] platforms = new boolean[leaguePlayers.size()][];
		for (int i = 0; i < leaguePlayers.size(); i++) {
			ratings[i] = playerRatings.get(leaguePlayers.get(ordered[i]));
			platforms[i] = leaguePlatforms.get(ordered[i]);
			System.out.println(playerNames.get(leaguePlayers.get(ordered[i])) + " : " + ratings[i] + " ; " + (platforms[i][0] ? "VS," : "")
					+ (platforms[i][1] ? "Steam," : "") + (platforms[i][2] ? "Switch," : "") + (platforms[i][3] ? "PS4," : ""));
		}
		return getBestLeagueGroups(ratings, platforms, 20.0);

	}

	public void showLeagueGroups(String... leaguePlayerNames) {
		List<String> leaguePlayerNames2 = Arrays.asList(leaguePlayerNames);
		List<Integer> leaguePlayers = new ArrayList<Integer>();
		for (int i = 0; i < playerNames.size(); i++) {
			//if (!(matches6months < 100 && matches3months == 0) && !(first5MatchesPointsPlayed.get(i) < 100 || first5MatchesPlayed.get(i) < 4)
			//		&& first5MatchesPointsPlayed.get(i) >= 100 && first5MatchesPlayed.get(i) >= 5) {

			if (leaguePlayerNames2.contains(playerNames.get(i))) {
				leaguePlayers.add(i);
				//System.out.println(playerNames.get(i));
			}
			//}
		}
		double[] ranks = new double[leaguePlayers.size()];
		for (int i = 0; i < leaguePlayers.size(); i++) {
			ranks[i] = playerRatings.get(leaguePlayers.get(i));
		}
		int[] orderedPlayers = getOrdered(ranks);
		for (int i = 0; i < orderedPlayers.length; i++) {
			ranks[i] = playerRatings.get(leaguePlayers.get(orderedPlayers[i]));
			//System.out.println(ranks[i]);
			//System.out.println(playerNames.get(frlPlayers.get(orderedPlayers[i])));
		}

		//System.out.println(getFRLGroupsScore(ranks, new int[]{5,4,4,6}));
		//System.out.println(getFRLGroupsScore(ranks, new int[]{6,5,4,4}));
		//System.out.println(getFRLGroupsScore(ranks, new int[]{4,5,4}));
		//System.out.println(getFRLGroupsScore(ranks, new int[]{5,4,4}));
		int[] groups = getBestLeagueGroups(ranks);
		//System.out.println(getFRLGroupsScore(ranks, getBestFRLGroups(ranks)));
		//System.out.println(getFRLGroupsScore(ranks, new int[] {5,4,4,4,5})-getFRLGroupsScore(ranks, new int[] {6,5,6,5}));
		int n = 0;
		System.out.println("Group;Name;Country;Med rating");

		for (int i = 0; i < groups.length; i++) {
			if (groups[i] == 0) {
				break;
			}
			for (int j = 0; j < groups[i]; j++) {
				System.out.println((i + 1) + ";" + playerNames.get(leaguePlayers.get(orderedPlayers[n])) + ";"
						+ countryNames.get(playerCountries.get(leaguePlayers.get(orderedPlayers[n]))) + ";"
						+ round(playerRatings.get(leaguePlayers.get(orderedPlayers[n])), 2));

				n++;
			}
			System.out.println();

		}
		System.out.println();
		n = 0;
		for (int i = 0; i < groups.length; i++) {
			if (groups[i] == 0)
				break;
			System.out.print("Group " + (i + 1) + ";");
			for (int j = 0; j < groups[i]; j++) {
				System.out.print(playerNames.get(leaguePlayers.get(orderedPlayers[n + j])) + ";");
			}
			System.out.println("G/A;Won;Lost");

			for (int j = 0; j < groups[i]; j++) {
				System.out.print(playerNames.get(leaguePlayers.get(orderedPlayers[n + j])) + ";");
				for (int k = 0; k < groups[i]; k++) {
					if (k <= j)
						System.out.print(";");
					else {
						System.out.print("=IF(" + (char) ('B' + j) + (n + i * 2 + 2 + k) + "=\"\",\"\",CONCATENATE(REGEXEXTRACT(" + (char) ('B' + j)
								+ (n + i * 2 + 2 + k) + ",\"[0-9]+ - ([0-9]+)\"),\" - \",REGEXEXTRACT(" + (char) ('B' + j) + (n + i * 2 + 2 + k)
								+ ",\"([0-9]+) - [0-9]+\")));");
					}
				}
				//G/A formula
				System.out.print("=");
				for (int k = 0; k < groups[i]; k++) {
					if (j != k) {
						System.out.print("+IF(" + (char) ('B' + k) + (n + i * 2 + 2 + j) + "=\"\",0,VALUE(REGEXEXTRACT(" + (char) ('B' + k)
								+ (n + i * 2 + 2 + j) + ",\"([0-9]+) - [0-9]+\"))-VALUE(REGEXEXTRACT(" + (char) ('B' + k) + (n + i * 2 + 2 + j)
								+ ",\"[0-9]+ - ([0-9]+)\")))");
					}
				}
				System.out.print(";=");
				for (int k = 0; k < groups[i]; k++) {
					if (j != k) {
						System.out.print("+IF(" + (char) ('B' + k) + (n + i * 2 + 2 + j) + "=\"\",0,IF(VALUE(REGEXEXTRACT(" + (char) ('B' + k)
								+ (n + i * 2 + 2 + j) + ",\"([0-9]+) - [0-9]+\"))-VALUE(REGEXEXTRACT(" + (char) ('B' + k) + (n + i * 2 + 2 + j)
								+ ",\"[0-9]+ - ([0-9]+)\"))>0,1,0))");
					}
				}
				System.out.print(";=");
				for (int k = 0; k < groups[i]; k++) {
					if (j != k) {
						System.out.print("+IF(" + (char) ('B' + k) + (n + i * 2 + 2 + j) + "=\"\",0,IF(VALUE(REGEXEXTRACT(" + (char) ('B' + k)
								+ (n + i * 2 + 2 + j) + ",\"([0-9]+) - [0-9]+\"))-VALUE(REGEXEXTRACT(" + (char) ('B' + k) + (n + i * 2 + 2 + j)
								+ ",\"[0-9]+ - ([0-9]+)\"))<0,1,0))");
					}
				}
				System.out.println();

			}
			n += groups[i];
			System.out.println();

		}

		//System.out.println(getFRLGroupsScore(ranks, new int[]{6,7}));

	}

	public int[] getBestLeagueGroups(double[] ranks) {
		int[] groups = new int[ranks.length / 3 + 1];
		tryWithLeagueGroups(ranks, groups);
		return groups;
	}

	public double tryWithLeagueGroups(double[] ranks, int[] groups) {
		int totalSize = 0;
		int group = 0;
		while (group < groups.length && groups[group] != 0) {
			totalSize += groups[group];
			group++;
		}
		//System.out.println("group : " +group + ", totalSize : " + totalSize);
		if (totalSize == ranks.length)
			return getLeagueGroupsScore(ranks, groups);
		else if (ranks.length - totalSize < 3)
			return Integer.MAX_VALUE;
		else {
			int[] bestGroups = null;
			double bestScore = Integer.MAX_VALUE;
			for (int size = 5; size <= 8; size++) {
				int[] newGroups = groups.clone();
				newGroups[group] = size;
				double score = tryWithLeagueGroups(ranks, newGroups);
				if (score < bestScore && score > 0) {
					bestGroups = newGroups;
					bestScore = score;
				}
			}
			if (bestGroups != null)
				for (int i = 0; i < bestGroups.length; i++)
					groups[i] = bestGroups[i];
			return bestScore;
		}

	}

	public double getLeagueGroupsScore(double[] ranks, int[] groups) {
		final double[] preferedSizes = {
				10.0, 10.0, 10.0, 10.0, 10.0, 0.3, 0.2, 0.1, 0.0
		};

		double total = 0;
		int currentIndex = 0;
		int group = 0;
		while (group < groups.length && groups[group] != 0) {
			//System.out.println(groups[group]);
			//System.out.println("currentIndex : " + currentIndex);
			//System.out.println("groups[group]-1 : " + (groups[group]-1));
			total += preferedSizes[groups[group]];
			total += (ranks[currentIndex] / ranks[currentIndex + groups[group] - 1]) - 1;
			//System.out.println(ranks[currentIndex] + "-" + ranks[currentIndex+groups[group]-1]);
			currentIndex += groups[group];
			group++;
		}
		return total;
	}

	public WorldRanking() {

		loadAlias("World Ranking/Raw/AliasList.txt");
		loadAlias("World Ranking/Raw/PTGRankedAliases.txt");
		loadMatches("World Ranking/Raw/Danisen.txt");
		loadMatches("World Ranking/Raw/French Rank League/4/1.txt");
		loadMatches("World Ranking/Raw/French Rank League/4/2.txt");
		loadMatches("World Ranking/Raw/French Rank League/4/3.txt");
		loadMatches("World Ranking/Raw/French Rank League/4/4.txt");
		loadMatches("World Ranking/Raw/French Rank League/3.txt");
		loadMatches("World Ranking/Raw/French Rank League/5.txt");
		loadMatches("World Ranking/Raw/French Rank League/5 - xenotypos and Hiku.txt");
		loadMatches("World Ranking/Raw/OIU League/1/S.txt");
		loadMatches("World Ranking/Raw/OIU League/1/A.txt");
		loadMatches("World Ranking/Raw/OIU League/1/B.txt");
		loadMatches("World Ranking/Raw/OIU League/1/B Finals.txt");
		loadMatches("World Ranking/Raw/OIU League/1/C Finals.txt");
		loadMatches("World Ranking/Raw/OIU League/1/C.txt");
		loadMatches("World Ranking/Raw/OIU League/2/S.txt");
		loadMatches("World Ranking/Raw/OIU League/2/A1.txt");
		loadMatches("World Ranking/Raw/OIU League/2/A2.txt");
		loadMatches("World Ranking/Raw/OIU League/2/B1.txt");
		loadMatches("World Ranking/Raw/OIU League/2/B2.txt");
		loadMatches("World Ranking/Raw/OIU League/2/C.txt");
		loadMatches("World Ranking/Raw/OIU League/2/A1 Finals + makkyu.txt");
		loadMatches("World Ranking/Raw/OIU League/2/A2 Finals.txt");
		loadMatches("World Ranking/Raw/OIU League/2/B1 Finals.txt");
		loadMatches("World Ranking/Raw/OIU League/2/B2 Finals.txt");
		loadMatches("World Ranking/Raw/OIU League/2/C Finals.txt");
		loadMatches("World Ranking/Raw/OIU League/3/S.txt");
		loadMatches("World Ranking/Raw/OIU League/3/A1.txt");
		loadMatches("World Ranking/Raw/OIU League/3/A2.txt");
		loadMatches("World Ranking/Raw/OIU League/3/B1.txt");
		loadMatches("World Ranking/Raw/OIU League/3/B2.txt");
		loadMatches("World Ranking/Raw/OIU League/3/C.txt");
		loadMatches("World Ranking/Raw/OIU League/3/D.txt");
		loadMatches("World Ranking/Raw/Online Pro League/1.txt");
		loadMatches("World Ranking/Raw/Online Pro League/2/A.txt");
		loadMatches("World Ranking/Raw/Online Pro League/2/B.txt");
		loadMatches("World Ranking/Raw/Online Pro League/2/C1.txt");
		loadMatches("World Ranking/Raw/Online Pro League/2/C2.txt");
		loadMatches("World Ranking/Raw/We League/1/A.txt");
		loadMatches("World Ranking/Raw/We League/1/B.txt");
		loadMatches("World Ranking/Raw/We League/1/C.txt");
		loadMatches("World Ranking/Raw/We League/1/D.txt");
		loadMatches("World Ranking/Raw/We League/2/A1.txt");
		loadMatches("World Ranking/Raw/We League/2/A2.txt");
		loadMatches("World Ranking/Raw/We League/2/B1.txt");
		loadMatches("World Ranking/Raw/We League/2/B2.txt");
		loadMatches("World Ranking/Raw/We League/2/C.txt");
		loadMatches("World Ranking/Raw/We League/2/S.txt");
		loadMatches("World Ranking/Raw/We League/2/A2 Finals.txt");
		loadMatches("World Ranking/Raw/We League/2/B1 Finals.txt");
		loadMatches("World Ranking/Raw/We League/2/B2 Finals.txt");
		loadMatches("World Ranking/Raw/We League/2/C Finals.txt");
		loadMatches("World Ranking/Raw/Puyota League/1/Steam.txt");
		loadMatches("World Ranking/Raw/Puyota League/1/Switch.txt");
		loadMatches("World Ranking/Raw/Puyota League/1/PS4.txt");
		loadMatches("World Ranking/Raw/Puyota League/1/3DS.txt");
		loadMatches("World Ranking/Raw/Puyota League/2/Main.txt");
		loadMatches("World Ranking/Raw/Puyota League/2/Finals.txt");
		//loadMatches("World Ranking/Raw/PuyoGB League/4/Rock.txt");
		//loadMatches("World Ranking/Raw/PuyoGB League/4/Star.txt");
		//loadMatches("World Ranking/Raw/PuyoGB League/4/Moon.txt");
		//loadMatches("World Ranking/Raw/PuyoGB League/4/Crown.txt");
		//loadMatches("World Ranking/Raw/PuyoGB League/4/Playoffs.txt");
		loadMatches("World Ranking/Raw/Western Puyo Bowl/November 2018/PPC Bronze.txt");
		loadMatches("World Ranking/Raw/Western Puyo Bowl/November 2018/PPe Bronze.txt");
		loadMatches("World Ranking/Raw/Western Puyo Bowl/November 2018/PPe Silver.txt");
		loadMatches("World Ranking/Raw/Western Puyo Bowl/November 2018/PPe Gold.txt");
		loadMatches("World Ranking/Raw/Western Puyo Bowl/March 2019/PPe Bronze.txt");
		loadMatches("World Ranking/Raw/Western Puyo Bowl/March 2019/PPe Silver.txt");
		loadMatches("World Ranking/Raw/Western Puyo Bowl/January 2019/PPe Bronze.txt");
		loadMatches("World Ranking/Raw/Western Puyo Bowl/January 2019/PPe Silver.txt");
		loadMatches("World Ranking/Raw/Australian Puyo League.txt");
		loadMatches("World Ranking/Raw/Taiwanese Puyo League/1/Intermediate.txt");
		loadMatches("World Ranking/Raw/Taiwanese Puyo League/1/Advanced.txt");
		loadMatches("World Ranking/Raw/Taiwanese Puyo League/2/Intermediate.txt");
		loadMatches("World Ranking/Raw/Taiwanese Puyo League/2/High Intermediate.txt");
		loadMatches("World Ranking/Raw/Taiwanese Puyo League/2/Advanced.txt");
		loadMatches("World Ranking/Raw/Community Battles/Australia vs Taiwan.txt");
		loadMatches("World Ranking/Raw/Community Battles/Brazil vs bayoen.txt");
		loadMatches("World Ranking/Raw/Community Battles/Taiwan vs bayoen.txt");
		loadMatches("World Ranking/Raw/Community Battles/Australia vs Brazil.txt");
		loadMatches("World Ranking/Raw/Junisen/S.txt");
		loadMatches("World Ranking/Raw/Junisen/A.txt");
		loadMatches("World Ranking/Raw/Junisen/B.txt");
		loadMatches("World Ranking/Raw/Junisen/C.txt");
		loadMatches("World Ranking/Raw/Ranked Matches.txt");
		loadMatches("World Ranking/Raw/International Puyo League/1.txt");
		loadMatches("World Ranking/Raw/International Puyo League/2.txt");
		loadMatches("World Ranking/Raw/International Puyo League/3.txt");
		loadMatches("World Ranking/Raw/International Puyo League/4.txt");
		loadMatches("World Ranking/Raw/International Puyo League/5.txt");
		loadMatches("World Ranking/Raw/International Puyo League/6.txt");
		loadMatches("World Ranking/Raw/International Puyo League/7.txt");
		loadMatches("World Ranking/Raw/Biweekly Carrot/Session 1.txt");
		loadMatches("World Ranking/Raw/Biweekly Carrot/Session 2.txt");
		loadMatches("World Ranking/Raw/Biweekly Carrot/Session 3.txt");
		loadMatches("World Ranking/Raw/Biweekly Carrot/Session 4.txt");
		loadMatches("World Ranking/Raw/Biweekly Carrot/Session 5.txt");
		loadMatches("World Ranking/Raw/Biweekly Carrot/Session 6.txt");
		loadMatches("World Ranking/Raw/Biweekly Carrot/Session 7.txt");
		loadMatches("World Ranking/Raw/Biweekly Carrot/Session 8.txt");
		loadMatches("World Ranking/Raw/Biweekly Carrot/Session 9.txt");
		loadMatches("World Ranking/Raw/Biweekly Carrot/Session 10.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/1/Bronze.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/1/Silver.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/1/Gold A.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/1/Gold B.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/1/Gold C.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/1/Platinum.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/2/PC/Bronze.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/2/PC/Silver.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/2/PC/Gold.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/2/PC/Platinum.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/2/PC/Diamond.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/2/Switch/Bronze.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/2/Switch/Silver.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/2/Switch/Gold.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/2/Switch/Platinum.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/2/Switch/Diamond.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/3/Star.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/3/Moon.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/3/Crown.txt");
		loadMatches("World Ranking/Raw/Puyo Training Grounds League/3/Comet.txt");
		loadMatches("World Ranking/Raw/Bayoen League/Suketoudara.txt");
		loadMatches("World Ranking/Raw/Bayoen League/Amitie.txt");
		loadMatches("World Ranking/Raw/Bayoen League/Baromett.txt");
		loadMatches("World Ranking/Raw/Bayoen League/Carbuncle.txt");
		loadMatches("World Ranking/Raw/Bayoen League/Draco.txt");
		loadMatches("World Ranking/Raw/Road Tsu Victory/1.txt");
		loadMatches("World Ranking/Raw/Saikyou League/Season 1.txt");
		loadMatches("World Ranking/Raw/Saikyou League/Season 2 1st part.txt");
		loadMatches("World Ranking/Raw/Saikyou League/Season 2 2nd part.txt");
		loadPTGMatches("World Ranking/Raw/PTGRankedMatches.txt");
		//loadMatches("World Ranking/Raw/Yuta's YouTube.txt");*/

		loadMatches("World Ranking/Raw/PuyoLobbySorted.txt");
		loadCountries("World Ranking/Raw/Countries.txt");
		sortMatchesByDate();
		//splitAllPlayerMatches();
		//printAllMatchesForWiki();
		printAllMatchesRaw();
		timeCoefCoef = 1;

		
		/*playMatches(50000);
		///playMatches(10000);
		long timeBefore = System.currentTimeMillis();
		/*addLeaguePlayer("leotrouvtou", true, false, false, false);
		addLeaguePlayer("Douxy", true, true, false, false);
		addLeaguePlayer("Alexis", true, true, true, false);
		addLeaguePlayer("Mfire", true, true, true, false);
		addLeaguePlayer("Hambota", true, true, true, false);
		addLeaguePlayer("xenotypos", true, true, false, false);
		addLeaguePlayer("Hiku", true, true, false, false);
		addLeaguePlayer("Makun", true, false, false, false);
		addLeaguePlayer("Norel", true, false, false, false);
		addLeaguePlayer("Doremy", true, true, true, true);
		addLeaguePlayer("Konata", true, true, true, false);
		addLeaguePlayer("ELLLO", true, false, true, false);
		addLeaguePlayer("Lunard8128", true, true, false, false);
		addLeaguePlayer("Kranchy", true, false, true, false);
		addLeaguePlayer("JeremGS", true, false, false, false);
		addLeaguePlayer("Civel", true, true, true, false);
		addLeaguePlayer("BBR", true, true, true, false);
		addLeaguePlayer("Aurel509", true, false, false, false);*/

		/*addLeaguePlayer("KERO", false, true, false, true);
		addLeaguePlayer("OuO", false, true, true, false);
		addLeaguePlayer("tofuyifu", true, true, false, true);
		addLeaguePlayer("toxic3Q", true, true, false, false);
		addLeaguePlayer("A_Carbancle", true, true, true, false);
		addLeaguePlayer("PokeDialga", false, true, true, true);
		addLeaguePlayer("aclivtak", true, true, true, true);
		addLeaguePlayer("ojzajy", false, false, true, true);
		addLeaguePlayer("pokemonfan", true, true, true, false);
		addLeaguePlayer("Cakee", true, true, true, false);
		addLeaguePlayer("icedwater", false, false, true, false);
		addLeaguePlayer("ihen", false, true, true, true);
		addLeaguePlayer("haleyk", false, true, false, false);
		addLeaguePlayer("gua_tsu", false, true, true, true);
		addLeaguePlayer("Volary", false, true, true, false);
		addLeaguePlayer("RM9", true, true, true, true);
		addLeaguePlayer("BevicChiu", true, true, false, false);
		addLeaguePlayer("JasonBaby", true, true, false, false);*/
		//System.out.println(getCurrentLeagueBestGroups());
		//System.out.println("took: "+(System.currentTimeMillis() - timeBefore));
		//printOrderedRatingsWithCertaintyV2ForWiki(removeSplitted(accessibleFromPlayerList(getPlayer("Touakak"), constructMatchupStrengths())), false);

		//System.out.println("Australia ver. vs Taiwan : " + getTeamPower("MalachiChoo", "cramps_man", "TinkFloyd", "Yoshi100_Aus"));
		//System.out.println("Taiwan ver. vs Australia : " + getTeamPower("Cakee", "pokemonfan", "tofuyifu", "Acliv"));
		//System.out.println("Brazil ver. vs bayoen : " + getTeamPower("goulart", "Henryzedo", "ArthurDavi", "matheusrodf","Frag","guilga","RgrDiscoG55","Lakimino","Mark3000","SumidaReko","Elias","MidLVGuy","Myzka","VoidPH"));
		//System.out.println("bayoen ver. vs Brazil : " + getTeamPower("Hiku", "Touakak", "Hambota", "Mfire","Douxy","YaSSaY","ccc","DonYann","Alexis","Havven","Box","Doremy","xenotypos","Civel"));
		//System.out.println("bayoen ver. vs Taiwan : " + (getTeamPower("leotrouvtou", "Douxy", "Alexis", "Mfire", "Hambota", "xenotypos", "Hiku", "Makun","Norel", "Doremy", "Konata", "ELLLO", "Lunard8128", "JeremGS", "Civel", "BBR", "Aurel509", "Kranchy")));
		//System.out.println("Taiwan ver. vs bayoen : " + (getTeamPower("Acliv", "A_Carbancle", "ojzajy", "icedwater", "ihen", "PokeDialga", "tofuyifu", "Cakee","pokemonfan", "KERO", "haleyk", "gua_tsu", "Volary", "RM9", "JasonBaby", "OuO", "toxic3Q", "BevicChiu")));
		//printOrderedRatingsWithCertaintyV2ForWiki(removeSplitted(accessibleFromPlayerList(getPlayer("Hiku"), constructMatchupStrengths())), true);
		//printOrderedRatingsWithCertaintyV2ForWiki(removeSplitted(accessibleFromPlayerList(getPlayer("Hiku"), constructMatchupStrengths())), false);

		//printOrderedRatingsWithCertaintyV2ForWiki(
		//		removeCountry(removeSplitted(accessibleFromPlayerList(getPlayer("Hiku"), constructMatchupStrengths())), "Japan", "Korea"), false);
		//printOrderedRatingsWithCertaintyV2ForWiki(removeCountry(accessibleFromPlayerList(getPlayer("Hiku"), constructMatchupStrengths()), "Japan", "Korea"),false);
		//printRankingForLeague();
		
		//printRankingForLeagueSheet();
		//printOrderedRatingsWithCertaintyV2ForWiki(removeSplitted(accessibleFromPlayerList(getPlayer("Hiku"), constructMatchupStrengths())), true);
		//printOrderedRatingsWithCertaintyV2ForWiki(removeSplitted(accessibleFromPlayerList(getPlayer("Hiku"), constructMatchupStrengths())), false);
		//printAllPlayerMatchesWithDelta("Hiku");
		//printOrderedRatingsWithCertaintyV2ForWiki(accessibleFromPlayerList(getPlayer("Hiku"), constructMatchupStrengths()));
		//printOrderedRatingsWithCertainty(removeSplitted(accessibleFromPlayerList(getPlayer("Hiku"), constructMatchupStrengths())));
		//printOrderedRatingsWithCertainty();

		/*giveEveryoneNMatches(new String[] {
				"Veradora","Alexis","MalachiChoo","Mfire","Void","StyleSavvy","Moon","Eyasluna","Kranchy","pokemonfan","Yoshi100_Aus","KirbyCombo35","Lunard8128","matheusrodf","Juicey","Keyronn","CaneofPacci","WhiteSummer","ShiroBrawl","Elias","Prokcyon","Coconutbowling","Kurtupo","shoe","KERO","WillFlame","Cranberry","sarracenia","leotrouvtou","slayers_boner420","Terramyst","guilga","SirButtsALot","Frag","RgrDiscoG55","Okenime","Etosan","Nusu","Zay","Protecc","Kowfi","xenotypos","Hiku","Toti22"
				}, 3, 2.0, new String[] {
				"Protecc","leotrouvtou","Void","Protecc","Void","Terramyst","Coconutbowling","Prokcyon","MalachiChoo","Alexis","Elias","Nusu","Keyronn","Mfire","Mfire","KirbyCombo35","guilga","slayers_boner420","ShiroBrawl","Okenime","SirButtsALot","Eyasluna","WillFlame","Void","KERO","guilga","CaneofPacci","Lunard8128","SirButtsALot","leotrouvtou","Juicey","Keyronn","Zay","sarracenia","Elias","matheusrodf","ShiroBrawl","slayers_boner420","xenotypos","Elias","slayers_boner420","guilga","Kurtupo","Hambota","Protecc","RgrDiscoG55","WillFlame","xenotypos","Kurtupo","Frag","Etosan","Nusu","Kowfi","WillFlame","Hambota","grass_","Kowfi","shoe","Etosan","Zay","Nusu","Kowfi","leotrouvtou","sarracenia","Hambota","Hiku","Hiku"
				}, new String[] {
						"Mfire","Kranchy","Veradora","Moon","ShiroBrawl","Alexis","Veradora","Eyasluna","Veradora","MalachiChoo","Alexis","MalachiChoo","Moon","StyleSavvy","Moon","pokemonfan","Eyasluna","Lunard8128","Yoshi100_Aus","Kranchy","KirbyCombo35","matheusrodf","Prokcyon","Coconutbowling","WhiteSummer","Kranchy","Yoshi100_Aus","pokemonfan","Lunard8128","Kurtupo","StyleSavvy","pokemonfan","StyleSavvy","WhiteSummer","KirbyCombo35","Juicey","CaneofPacci","Keyronn","Yoshi100_Aus","CaneofPacci","Coconutbowling","WhiteSummer","Prokcyon","matheusrodf","Juicey","Cranberry","KERO","shoe","Okenime","Cranberry","Cranberry","grass_","Terramyst","sarracenia","Terramyst","Frag","Frag","SirButtsALot","grass_","RgrDiscoG55","RgrDiscoG55","Etosan","KERO","Okenime","Zay","shoe","xenotypos"
						});*/
		//showLeagueGroups("Hiku","Alexis","MalachiChoo","Yoshi100_Aus","ccc","pokemonfan","xenotypos","Lunard8128","Acliv","Coconutbowling","Void","guilga","Terramyst","Etosan","matheusrodf","Lakimino","Kranchy","Prokcyon","JeremGS","Tronconneuse","DonYann","Haxel","Okenime","tofuyifu","EuclideanReport","Keyronn","Ting","PokeDialga","Nusu","DdR_Dan","Mfire","shoe","ShiroBrawl","Bribrisan","Toti22","Doremy","Frag","OuO","KERO","YaSSaY","Rayming","Luxr4y","LemonEdd","WhiteSummer","nomade","Aurel509","ShinKoala","Civel","ojzajy","SOU","RF3","RgrDiscoG55");
	}

}
