package com.albom.ionosonde.shigaraki;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class IonogramShigaraki {

	private double[][] data;
	private double[] frequencies;
	private double[] altitudes;

	/**
	 * @return the data
	 */
	public double[][] getData() {
		return data;
	}

	/**
	 * @return the frequencies
	 */
	public double[] getFrequencies() {
		return frequencies;
	}

	/**
	 * @return the altitudes
	 */
	public double[] getAltitudes() {
		return altitudes;
	}

	public boolean load(String fileName) {
		Path path = Paths.get(fileName);

		List<String> lines = null;
		try {
			lines = Files.readAllLines(path, Charset.forName("utf-8"));
		} catch (IOException e) {
			return false;
		}

		parse(lines);

		return true;
	}

	private void parse(List<String> lines) {
		boolean head = false;
		int length_of_head = 0;
		int row = 0;
		for (String line : lines) {
			if (line.startsWith(" ")) {
				String[] values = line.trim().split("\\s+");
				if (!head) {
					data = new double[lines.size() - length_of_head][values.length];
					frequencies = new double[values.length];
					altitudes = new double[lines.size() - length_of_head];
					for (int i = 0; i < values.length; i++) {
						frequencies[i] = Double.valueOf(values[i]);
					}
					head = true;
				} else {
					altitudes[row] = Double.valueOf(values[0]);
					for (int i = 1; i < values.length; i++) {
						data[row][i - 1] = Double.valueOf(values[i]);
					}
					row++;
				}
			} else {
				length_of_head++;
				continue;
			}
		}
	}

	public boolean download(LocalDateTime date) {
		String url_base = "http://database.rish.kyoto-u.ac.jp/arch/mudb/data/ionosonde/text";
		String ending = "_ionogram.txt";
		String url = String.format(Locale.US, "%s/%d/%d%02d/%d%02d%02d/%d%02d%02d%02d%02d%s", url_base, date.getYear(),
				date.getYear(), date.getMonthValue(), date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
				date.getYear(), date.getMonthValue(), date.getDayOfMonth(), date.getHour(), date.getMinute(), ending);
		LinkedList<String> response = new LinkedList<String>();
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				return false;
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine = null;

			while ((inputLine = in.readLine()) != null) {
				response.add(inputLine);
			}
			in.close();
		} catch (IOException e) {
			return false;
		}

		parse(response);

		return true;
	}

}
