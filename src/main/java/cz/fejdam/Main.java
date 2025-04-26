package cz.fejdam;

import com.sun.source.tree.NewArrayTree;
import netscape.javascript.JSObject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        startup();
    }

    private static void loadData() {
        try {
            String urlString = "https://archive-api.open-meteo.com/v1/archive"
                    + "?latitude=50.0096"
                    + "&longitude=14.462"
                    + "&start_date=1941-01-01"
                    + "&end_date=2024-12-31"
                    + "&daily=weather_code,temperature_2m_mean,temperature_2m_max,temperature_2m_min,"
                    + "apparent_temperature_mean,rain_sum,snowfall_sum,relative_humidity_2m_mean,wind_speed_10m_max,wind_direction_10m_dominant,visibility_mean"
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
            System.out.println("Response Code: " + status);

            if (status == 429) {
                throw new IOException("Hourly API request limit exceeded");

            } else {
                FileWriter fw = new FileWriter("out/response.json");
                fw.write(response.toString());
                fw.close();
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
        JSONArray minTeploty = daily.getJSONArray("temperature_2m_min");
        JSONArray pocitoveTeploty = daily.getJSONArray("apparent_temperature_mean");
        JSONArray srazky = daily.getJSONArray("rain_sum");
        JSONArray snezeni = daily.getJSONArray("snowfall_sum");
        JSONArray vlhkosti = daily.getJSONArray("relative_humidity_2m_mean");
        JSONArray rychlostiVetru = daily.getJSONArray("wind_speed_10m_max");
        JSONArray smeryVetru = daily.getJSONArray("wind_direction_10m_dominant");
        JSONArray viditelnosti = daily.getJSONArray("visibility_mean");

        List<Object> weatherCodes = daily.getJSONArray("weather_code").toList();

        List<String> popisy = new ArrayList<>();
        for (int i = 0; i < weatherCodes.size(); i++) {
            popisy.add(KodyPocasi.popisPocasi((Integer) weatherCodes.get(i)));
        }
        int tropyCount = 0;
        int mrazyCount = 0;

        int severCount = 0;
        int jihCount = 0;
        int vychodCount = 0;
        int zapadCount = 0;

        List<Double> teplotniRozdily = new ArrayList<>();
        List<Double> teploty = new ArrayList<>();
        List<Double> srazkyList = new ArrayList<>();
        List<Double> vlhkostList = new ArrayList<>();
        List<Double> snehu = new ArrayList<>();



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
            double smerVetru = smeryVetru.getDouble(i);
            double rychlostVetru = rychlostiVetru.getDouble(i);
            double viditelnost = viditelnosti.getDouble(i);

            double snezilo = snezeni.getDouble(i) ;

            //kvantitatuvni znaky
            double teplotniRozdil = maxTeplota - teplota;
            teplotniRozdily.add(teplotniRozdil);


            //kvalitativni znaky

            //smer vetru
            String svetStranaVetru = smerVetru > 315 || smerVetru < 45 ? "Sever" : "Jih";
            svetStranaVetru = smerVetru > 225 && smerVetru < 315 ? "Zapad" : svetStranaVetru;
            svetStranaVetru = smerVetru < 135 && smerVetru > 45 ? "Vychod" : svetStranaVetru;

            severCount = svetStranaVetru.equals("Sever") ? severCount + 1 : severCount;
            jihCount = svetStranaVetru.equals("Jih") ? jihCount + 1 : jihCount;
            vychodCount = svetStranaVetru.equals("Vychod") ? vychodCount + 1 : vychodCount;
            zapadCount = svetStranaVetru.equals("Zapad") ? zapadCount + 1 : zapadCount;
            //Extremni vykyvy(tropicky/mrazivy den)
            boolean tropy = maxTeplota >= 30.0;
            boolean mrazy = minTeplota <= 0;
            String extremniPocasi = tropy ? "tropy" : "mrazy";
            extremniPocasi = !tropy && !mrazy ? "normalni" : extremniPocasi;
            tropyCount = extremniPocasi.equals("tropy") ? tropyCount + 1 : tropyCount;
            mrazyCount = extremniPocasi.equals("mrazy") ? mrazyCount + 1 : mrazyCount;
            //Viditelnost
            String viditelnostUroven = viditelnost >= 10 ? "Velmi Dobrá" : "Dobrá";
            viditelnostUroven = viditelnost <= 5 ? "Přijatelná" : viditelnostUroven;
            viditelnostUroven = viditelnost <= 2 ? "Špatná" : viditelnostUroven;
            viditelnostUroven = viditelnost <= 1 ? "Velmi Špatná" : viditelnostUroven;
            //Vetrnost
            String vetrnostStupnice = rychlostVetru <= 1 ? "Bezvětří" : "Vánek";
            vetrnostStupnice = rychlostVetru >= 6 ? "Slabý vánek" : vetrnostStupnice;
            vetrnostStupnice = rychlostVetru >= 12 ? "Mírný vánek" : vetrnostStupnice;
            vetrnostStupnice = rychlostVetru >= 20 ? "Mírný vítr" : vetrnostStupnice;
            vetrnostStupnice = rychlostVetru >= 29 ? "Čerstvý vítr" : vetrnostStupnice;
            vetrnostStupnice = rychlostVetru >= 39 ? "Silný vítr" : vetrnostStupnice;
            vetrnostStupnice = rychlostVetru >= 50 ? "Velmi silný vítr" : vetrnostStupnice;
            vetrnostStupnice = rychlostVetru >= 62 ? "Bouřlivý vítr" : vetrnostStupnice;
            vetrnostStupnice = rychlostVetru >= 75 ? "Silná bouře" : vetrnostStupnice;
            vetrnostStupnice = rychlostVetru >= 89 ? "Plná bouře" : vetrnostStupnice;
            vetrnostStupnice = rychlostVetru >= 103 ? "Silná vichřice" : vetrnostStupnice;
            vetrnostStupnice = rychlostVetru >= 118 ? "Orkán" : vetrnostStupnice;


        }
        System.out.println(tropyCount + " Tropických dnů a " + mrazyCount + " Mrazivých dnů");
        System.out.println(zaokrouhli((tropyCount / (double) dny.toList().size()) * 100, 2) + "% všech dní je tropických(t>30°C); " + zaokrouhli((mrazyCount / (double) dny.toList().size()) * 100, 2) + "% všech dní je mrazivých(t<0°C);");
        Collections.sort(teplotniRozdily);
        double medianTeplotniRozdil;
        if (teplotniRozdily.size() % 2 == 1) {
            medianTeplotniRozdil = (teplotniRozdily.get((teplotniRozdily.size() / 2) - 1) + teplotniRozdily.get((teplotniRozdily.size() / 2) + 1)) / 2;
        } else {
            medianTeplotniRozdil = teplotniRozdily.get((teplotniRozdily.size() / 2));
        }
        System.out.println(severCount + "x Severni vitr, " + jihCount + "x Jizni vitr, " + vychodCount + "x Vychodni vitr, " + zapadCount + "x Zapadni vitr");
        System.out.println(
                "Medianovy teplotni rozdil(mezi nejvetsi a nejmensi teplotou): " + zaokrouhli(medianTeplotniRozdil, 2) + " " +
                        "Nejmensi rozdil: " + zaokrouhli(teplotniRozdily.get(0), 2) + " " +
                        "Nejvetsi rozdil: " + zaokrouhli(teplotniRozdily.get(teplotniRozdily.size() - 1), 2));


    }

    private static double zaokrouhli(double cislo, int mist) {
        BigDecimal bigDecimal = new BigDecimal(cislo);
        return bigDecimal.setScale(mist, RoundingMode.HALF_UP).doubleValue();
    }

    private static void startup() {
        System.out.println("\n" +
                " __      __               __  .__                ___________     __         .__                    ____     ____ \n" +
                "/  \\    /  \\ ____ _____ _/  |_|  |__   __________\\_   _____/____/  |_  ____ |  |__   ___________  /_   |   /_   |\n" +
                "\\   \\/\\/   // __ \\\\__  \\\\   __\\  |  \\_/ __ \\_  __ \\    __)/ __ \\   __\\/ ___\\|  |  \\_/ __ \\_  __ \\  |   |    |   |\n" +
                " \\        /\\  ___/ / __ \\|  | |   Y  \\  ___/|  | \\/     \\\\  ___/|  | \\  \\___|   Y  \\  ___/|  | \\/  |   |    |   |\n" +
                "  \\__/\\  /  \\___  >____  /__| |___|  /\\___  >__|  \\___  / \\___  >__|  \\___  >___|  /\\___  >__|     |___| /\\ |___|\n" +
                "       \\/       \\/     \\/          \\/     \\/          \\/      \\/          \\/     \\/     \\/               \\/      \n");
        System.out.println("by Fejdam | https://github.com/FiDaLip/WeatherFetcher#");

        loadData();
        try {
            loadJSON(new File("out/response.json"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}