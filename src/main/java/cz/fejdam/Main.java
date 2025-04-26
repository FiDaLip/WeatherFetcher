package cz.fejdam;

import com.sun.source.tree.NewArrayTree;
import netscape.javascript.JSObject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        startup();
    }
    private static void loadData(){
        try {
            String urlString = "https://archive-api.open-meteo.com/v1/archive"
                    + "?latitude=50.0096"
                    + "&longitude=14.462"
                    + "&start_date=1941-01-01"
                    + "&end_date=2024-12-31"
                    + "&daily=weather_code,temperature_2m_mean,temperature_2m_max,temperature_2m_min,"
                    + "apparent_temperature_mean,rain_sum,snowfall_sum,relative_humidity_2m_mean, wind_speed_10m_max,wind_direction_10m_dominant&hourly=temperature_2m"
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
            if(status == 403){
                throw new IOException("API Daily limit reached");

            } else{
                FileWriter fw = new FileWriter("response.json");
                fw.write(response.toString());
                fw.close();
                System.out.println("Response Code: " + status);
            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadJSON(File file) throws FileNotFoundException {
        StringBuilder json = new StringBuilder();
        Scanner sc = new Scanner(file);
        while (sc.hasNext()) {
            json.append(sc.next());
        }
        JSONObject jsonObject = new JSONObject(json.toString());

        JSONObject daily = jsonObject.getJSONObject("daily");

        JSONArray dny = daily.getJSONArray("time");

        //statisticke znaky
        JSONArray denniTeploty = daily.getJSONArray("temperature_2m_mean");
        JSONArray maxTeploty = daily.getJSONArray("temperature_2m_max");
        JSONArray minTeploty = daily.getJSONArray("temperature_2m_max");
        JSONArray pocitoveTeploty = daily.getJSONArray("apparent_temperature_mean");
        JSONArray srazky = daily.getJSONArray("rain_sum");
        JSONArray snezeni = daily.getJSONArray("snowfall_sum");
        JSONArray vlhkosti = daily.getJSONArray("relative_humidity_2m_mean");


        List<Object> weatherCodes = daily.getJSONArray("weather_code").toList();

        List<String> popisy = new ArrayList<>();
        for (int i = 0; i < weatherCodes.size(); i++) {
            popisy.add(KodyPocasi.popisPocasi((Integer) weatherCodes.get(i)));
        }
        int tropyCount = 0;
        int mrazyCount = 0;
        for (int i = 0; i < dny.toList().size(); i++) {
            /*System.out.println(
                    dny.get(i) + " " + popisy.get(i)
                            + ", teplota: " + denniTeploty.get(i) + "°C" +
                            ", nejvyssi teplota: " + maxTeploty.get(i) + "°C" +
                            ", nejnizsi teplota: " + minTeploty.get(i) + "°C" +
                            ", pocitova teplota: " + pocitoveTeploty.get(i) + "°C" +
                            ", srazek(za cely den) " + srazky.get(i) + "mm" +
                            ", snehu(za cely den) " + snezeni.get(i) + "cm" +
                            ", vlhkost: " + vlhkosti.get(i) + "%");*/

            double teplota = denniTeploty.getDouble(i);
            double maxTeplota = maxTeploty.getDouble(i);
            double minTeplota = minTeploty.getDouble(i);
            double pocitovaTeplota = pocitoveTeploty.getDouble(i);
            double srazkek = srazky.getDouble(i);
            double vlhkost = vlhkosti.getDouble(i);
            boolean snezilo = snezeni.getDouble(i) > 0;



            boolean tropy = maxTeplota >= 30.0;
            boolean mrazy = minTeplota <= 0;

            String extremniPocasi = tropy  ? "tropy" : "mrazy";
            extremniPocasi = !tropy && !mrazy ? "normalni" : extremniPocasi;

            tropyCount = extremniPocasi.equals("tropy") ? tropyCount + 1 : tropyCount;
            mrazyCount = extremniPocasi.equals("mrazy") ? mrazyCount + 1 : mrazyCount;



        }
        System.out.println(tropyCount + "Tropických dnů a " + mrazyCount + "Mrazyvých dnů");
        System.out.println((tropyCount/(double)dny.toList().size()) *100+ "% všech dní je tropických(t>30°C); " + (mrazyCount/(double)dny.toList().size())*100 + "% všech dní je mrazivých(t<0°C);");



    }
    private static void startup(){
        System.out.println("\n" +
                " _    _  _____   ___   _____  _   _  _____ ______ ______  _____  _____  _____  _   _  _____ ______       __      _____ \n" +
                "| |  | ||  ___| / _ \\ |_   _|| | | ||  ___|| ___ \\|  ___||  ___||_   _|/  __ \\| | | ||  ___|| ___      /  |    |  _  |\n" +
                "| |  | || |__  / /_\\ \\  | |  | |_| || |__  | |_/ /| |_   | |__    | |  | /  \\/| |_| || |__  | |_/ /     `| |    | |/' |\n" +
                "| |/\\| ||  __| |  _  |  | |  |  _  ||  __| |    / |  _|  |  __|   | |  | |    |  _  ||  __| |    /       | |    |  /| |\n" +
                "\\  /\\  /| |___ | | | |  | |  | | | || |___ | |\\ \\ | |    | |___   | |  | \\__/\\| | | || |___ | |\\ \\      _| |_ _ \\ |_/ /\n" +
                " \\/  \\/ \\____/ \\_| |_/  \\_/  \\_| |_/\\____/ \\_| \\_|\\_|    \\____/   \\_/   \\____/\\_| |_/\\____/ \\_| \\_|     \\___/(_) \\___/ ");

        System.out.println("by Fejdam | https://github.com/FiDaLip/WeatherFetcher#");

        loadData();
        try {
            loadJSON(new File("response.json"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}