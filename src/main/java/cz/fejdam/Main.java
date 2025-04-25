package cz.fejdam;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {

        System.out.println("\n" +
                " _    _  _____   ___   _____  _   _  _____ ______ ______  _____  _____  _____  _   _  _____ ______       __      _____ \n" +
                "| |  | ||  ___| / _ \\ |_   _|| | | ||  ___|| ___ \\|  ___||  ___||_   _|/  __ \\| | | ||  ___|| ___      /  |    |  _  |\n" +
                "| |  | || |__  / /_\\ \\  | |  | |_| || |__  | |_/ /| |_   | |__    | |  | /  \\/| |_| || |__  | |_/ /     `| |    | |/' |\n" +
                "| |/\\| ||  __| |  _  |  | |  |  _  ||  __| |    / |  _|  |  __|   | |  | |    |  _  ||  __| |    /       | |    |  /| |\n" +
                "\\  /\\  /| |___ | | | |  | |  | | | || |___ | |\\ \\ | |    | |___   | |  | \\__/\\| | | || |___ | |\\ \\      _| |_ _ \\ |_/ /\n" +
                " \\/  \\/ \\____/ \\_| |_/  \\_/  \\_| |_/\\____/ \\_| \\_|\\_|    \\____/   \\_/   \\____/\\_| |_/\\____/ \\_| \\_|     \\___/(_) \\___/ ");

        System.out.println("by Fejdam | www.github.com");



    }
    private static void loadData(){
        try {
            String urlString = "https://archive-api.open-meteo.com/v1/archive"
                    + "?latitude=50.0096"
                    + "&longitude=14.462"
                    + "&start_date=1941-01-01"
                    + "&end_date=2024-12-31"
                    + "&daily=weather_code,temperature_2m_mean,temperature_2m_max,temperature_2m_min,"
                    + "apparent_temperature_mean,rain_sum,snowfall_sum,relative_humidity_2m_mean"
                    + "&timezone=auto"
                    + "&utm_source=chatgpt.com";

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(10000);

            int status = connection.getResponseCode();
            BufferedReader reader;
            if (status >= 200 && status < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }

            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }

            reader.close();
            connection.disconnect();
            FileWriter fw = new FileWriter("temps.json");
            fw.write(response.toString());
            fw.close();
            System.out.println("Response Code: " + status);
            System.out.println("Response Body:\n" + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}