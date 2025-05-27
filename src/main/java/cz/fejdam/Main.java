package cz.fejdam;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        startup();
    }

    private static void loadData() {
        try {
            String urlString = "https://archive-api.open-meteo.com/v1/archive"
                    + "?latitude=50.0096"
                    + "&longitude=14.45"
                    + "&start_date=1940-01-02"
                    + "&end_date=2024-12-31"
                    + "&daily=weather_code,temperature_2m_mean,temperature_2m_max,temperature_2m_min,"
                    + "apparent_temperature_mean,rain_sum,snowfall_sum,relative_humidity_2m_mean,"
                    + "wind_speed_10m_max,wind_direction_10m_dominant,cloud_cover_mean"
                    + "&timezone=auto"
                    + "&utm_source=chatgpt.com";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .timeout(Duration.ofSeconds(25))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body();

            System.out.println("Response Code: " + status);

            if (status == 429) {
                System.out.println(body);
                throw new IOException("API request limit exceeded");
            } else {
                FileWriter fw = new FileWriter("out/response.json");
                fw.write(body);
                fw.flush();
                fw.close();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static void loadJSON(File file) throws IOException {
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
        JSONArray oblacnosti = daily.getJSONArray("cloud_cover_mean");

        List<Object> weatherCodes = daily.getJSONArray("weather_code").toList();

        List<String> popisy = new ArrayList<>();
        for (Object weatherCode : weatherCodes) {
            popisy.add(KodyPocasi.popisPocasi((Integer) weatherCode));
        }
        int tropyCount = 0;
        int mrazyCount = 0;

        int severCount = 0;
        int jihCount = 0;
        int vychodCount = 0;
        int zapadCount = 0;

        List<Double> teplotniRozdily = new ArrayList<>();


        FileWriter csvWriter = new FileWriter("out/weather_data.csv");
        //write header row
        csvWriter.append("Date,Weather Description,Mean Temp (°C),Max Temp (°C),Min Temp (°C),Apparent Temp (°C),Rain (mm),Snowfall (cm),Humidity (%),Wind Speed Max (km/h),Wind Direction (sever; jih; zapad; vychod),Oblacnost, extremni pocasi(tropy(t>30°C); mrazy(t<0°C); normalni)\n");


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
            double cloudCover = oblacnosti.getDouble(i);

            double srazkySnehu = snezeni.getDouble(i);

            //kvantitatuvni znaky
            double teplotniRozdil = maxTeplota - teplota;
            teplotniRozdily.add(teplotniRozdil);


            //kvalitativni znaky

            //smer vetru
            String svetStranaVetru;
            if (smerVetru >= 45 && smerVetru < 135) {
                svetStranaVetru = "Vychod";
            } else if (smerVetru >= 135 && smerVetru < 225) {
                svetStranaVetru = "Jih";
            } else if (smerVetru >= 225 && smerVetru < 315) {
                svetStranaVetru = "Zapad";
            } else {
                svetStranaVetru = "Sever";
            }


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

            String oblacnostStupnice;

            if (cloudCover > 90) {
                oblacnostStupnice = "Zataženo";
            } else if (cloudCover > 75) {
                oblacnostStupnice = "Skoro zataženo";
            } else if (cloudCover > 50) {
                oblacnostStupnice = "Oblačno";
            } else if (cloudCover > 25) {
                oblacnostStupnice = "Polojasno";
            } else {
                oblacnostStupnice = "Jasno";
            }
            //Vetrnost
            String vetrnostStupnice;

            if (rychlostVetru >= 118) {
                vetrnostStupnice = "Orkán";
            } else if (rychlostVetru >= 103) {
                vetrnostStupnice = "Silná vichřice";
            } else if (rychlostVetru >= 89) {
                vetrnostStupnice = "Plná bouře";
            } else if (rychlostVetru >= 75) {
                vetrnostStupnice = "Silná bouře";
            } else if (rychlostVetru >= 62) {
                vetrnostStupnice = "Bouřlivý vítr";
            } else if (rychlostVetru >= 50) {
                vetrnostStupnice = "Velmi silný vítr";
            } else if (rychlostVetru >= 39) {
                vetrnostStupnice = "Silný vítr";
            } else if (rychlostVetru >= 29) {
                vetrnostStupnice = "Čerstvý vítr";
            } else if (rychlostVetru >= 20) {
                vetrnostStupnice = "Mírný vítr";
            } else if (rychlostVetru >= 12) {
                vetrnostStupnice = "Mírný vánek";
            } else if (rychlostVetru >= 6) {
                vetrnostStupnice = "Slabý vánek";
            } else if (rychlostVetru > 1) {
                vetrnostStupnice = "Vánek";
            } else {
                vetrnostStupnice = "Bezvětří";
            }

            csvWriter.append(dny.getString(i)).append(",")//datum
                    .append(popisy.get(i)).append(",")//popis pocasi
                    .append(denniTeploty.get(i).toString()).append(",")//medianova teplota za den
                    .append(String.valueOf(maxTeplota)).append(",")//nejvyssi teplota za den
                    .append(String.valueOf(minTeplota)).append(",")//nejmensi teplota za den
                    .append(String.valueOf(pocitovaTeplota)).append(",")//medianova pocitova teplota za den
                    .append(String.valueOf(srazkek)).append(",")//pocet srazek (mm) za den
                    .append(String.valueOf(srazkySnehu)).append(",")//kolik snehu nasnezilo (cm) za den
                    .append(String.valueOf(vlhkost)).append(",")//vlhkost za den
                    .append(vetrnostStupnice).append(",")//jak moc foukal vitr
                    .append(svetStranaVetru).append(",")//jakym smerem foukal vitr
                    .append(oblacnostStupnice).append(",")//jaka je uroven uvIndex
                    .append(extremniPocasi)//extremni pocasi(tropy, mrazy)
                    .append("\n");

        }
        getYearlyData(daily);
        csvWriter.flush();
        csvWriter.close();
        System.out.println("CSV file generated: out/weather_data.csv");
        System.out.println(tropyCount + " Tropických dnů a " + mrazyCount + " Mrazivých dnů");
        System.out.println(zaokrouhli((tropyCount / (double) dny.toList().size()) * 100) + "% všech dní je tropických(t>30°C); " + zaokrouhli((mrazyCount / (double) dny.toList().size()) * 100) + "% všech dní je mrazivých(t<0°C);");
        Collections.sort(teplotniRozdily);
        double medianTeplotniRozdil;
        if (teplotniRozdily.size() % 2 == 0) {
            int mid = teplotniRozdily.size() / 2;
            medianTeplotniRozdil = (teplotniRozdily.get(mid - 1) + teplotniRozdily.get(mid)) / 2;
        } else {
            medianTeplotniRozdil = teplotniRozdily.get(teplotniRozdily.size() / 2);
        }
        System.out.println(severCount + "x Severni vitr, " + jihCount + "x Jizni vitr, " + vychodCount + "x Vychodni vitr, " + zapadCount + "x Zapadni vitr");
        System.out.println(
                "Medianovy teplotni rozdil(mezi nejvetsi a nejmensi teplotou): " + zaokrouhli(medianTeplotniRozdil) + " " +
                        "Nejmensi rozdil: " + zaokrouhli(teplotniRozdily.get(0)) + " " +
                        "Nejvetsi rozdil: " + zaokrouhli(teplotniRozdily.get(teplotniRozdily.size() - 1)));


    }

    private static void getYearlyData(JSONObject daily) throws IOException {
        JSONArray dny = daily.getJSONArray("time");

        // Počet let (včetně posledního roku)
        int pocetRoku = Integer.parseInt(dny.getString(dny.length() - 1).split("-")[0]) - Integer.parseInt(dny.getString(0).split("-")[0]) + 1;

        JSONArray denniTeploty = daily.getJSONArray("temperature_2m_mean");
        JSONArray maxTeploty = daily.getJSONArray("temperature_2m_max");
        JSONArray minTeploty = daily.getJSONArray("temperature_2m_min");
        JSONArray snowfalls = daily.getJSONArray("snowfall_sum");
        JSONArray rains = daily.getJSONArray("rain_sum");
        JSONArray cloudCover = daily.getJSONArray("cloud_cover_mean");

        FileWriter csvWriter = new FileWriter("out/weather_data_yearly.csv");
        // write header row
        csvWriter.append("Year,Winter Avg Temp (°C),Winter Maximum Temp (°C),Winter Minimum Temp (°C),Winter Average Cloud Cover(%)," +
                "Spring Avg Temp (°C),Spring Maximum Temp (°C),Spring Minimum Temp (°C),Spring Average Cloud Cover(%)," +
                "Summer Avg Temp (°C),Summer Maximum Temp (°C),Summer Minimum Temp (°C),Summer Average Cloud Cover(%)," +
                "Autumn Avg Temp (°C),Autumn Maximum Temp (°C),Autumn Minimum Temp (°C),Autumn Average Cloud Cover(%)," +
                "Rain (mm),Snowfall (cm), Cloud Cover Average(%), Avg Temp Yearly(°C),  Max Temp Yearly (°C), Min Temp Yearly(°C) \n");

        Map<String, List<Double>> seasonalTemps = new HashMap<>();
        seasonalTemps.put("jaro", new ArrayList<>());
        seasonalTemps.put("leto", new ArrayList<>());
        seasonalTemps.put("podzim", new ArrayList<>());
        seasonalTemps.put("zima", new ArrayList<>());

        // Zpracování dat pro každý rok
        for (int i = 0; i < pocetRoku; i++) {
            double snowfall = 0;
            double rain = 0;

            double zimaAvgTeplotas = 0;
            double zimaMaxTeplota = Double.MIN_VALUE;
            double zimaMinTeplota = Double.MAX_VALUE;

            double jaroAvgTeplotas = 0;
            double jaroMaxTeplota = Double.MIN_VALUE;
            double jaroMinTeplota = Double.MAX_VALUE;

            double letoAvgTeplotas = 0;
            double letoMaxTeplota = Double.MIN_VALUE;
            double letoMinTeplota = Double.MAX_VALUE;

            double podzimAvgTeplotas = 0;
            double podzimMaxTeplota = Double.MIN_VALUE;
            double podzimMinTeplota = Double.MAX_VALUE;

            double avgTeplota = 0;
            double maxTeplota = Double.MIN_VALUE;
            double minTeplota = Double.MAX_VALUE;

            double cloudCoverAvg = 0;
            double cloudCoverZimaAvg = 0;
            double cloudCoverLetoAvg = 0;
            double cloudCoverJaroAvg = 0;
            double cloudCoverPodzimAvg = 0;

            int daysInWinter = 0, daysInSpring = 0, daysInSummer = 0, daysInAutumn = 0;

            // Zpracování dní pro daný rok
            for (int days = 0; days < dny.length(); days++) {
                String[] parts = dny.getString(days).split("-");
                int yearIndex = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);

                int seasonYear = (month == 12) ? yearIndex + 1 : yearIndex;
                if (seasonYear != i + Integer.parseInt(dny.getString(0).split("-")[0]))
                    continue; // Ověření, že jde o správný rok

                double avg = denniTeploty.getBigDecimal(days).doubleValue();
                double max = maxTeploty.getBigDecimal(days).doubleValue();
                double min = minTeploty.getBigDecimal(days).doubleValue();

                if (month >= 6 && month <= 8) { // léto
                    letoAvgTeplotas += avg;
                    letoMaxTeplota = Math.max(letoMaxTeplota, max);
                    letoMinTeplota = Math.min(letoMinTeplota, min);
                    cloudCoverLetoAvg += cloudCover.getBigDecimal(days).doubleValue();

                    daysInSummer++;
                } else if (month >= 9 && month <= 11) { // podzim
                    podzimAvgTeplotas += avg;
                    podzimMaxTeplota = Math.max(podzimMaxTeplota, max);
                    podzimMinTeplota = Math.min(podzimMinTeplota, min);
                    cloudCoverPodzimAvg += cloudCover.getBigDecimal(days).doubleValue();


                    daysInAutumn++;
                } else if (month == 12 || month <= 2) { // zima
                    zimaAvgTeplotas += avg;
                    zimaMaxTeplota = Math.max(zimaMaxTeplota, max);
                    zimaMinTeplota = Math.min(zimaMinTeplota, min);
                    cloudCoverZimaAvg += cloudCover.getBigDecimal(days).doubleValue();

                    daysInWinter++;
                } else { // jaro
                    jaroAvgTeplotas += avg;
                    jaroMaxTeplota = Math.max(jaroMaxTeplota, max);
                    jaroMinTeplota = Math.min(jaroMinTeplota, min);
                    cloudCoverJaroAvg += cloudCover.getBigDecimal(days).doubleValue();

                    daysInSpring++;
                }
                avgTeplota += avg;
                maxTeplota = Math.max(maxTeplota, max);
                minTeplota = Math.min(minTeplota, min);

                cloudCoverAvg += cloudCover.getBigDecimal(days).doubleValue();
                snowfall += snowfalls.getDouble(days);
                rain += rains.getDouble(days);
            }
            avgTeplota = avgTeplota / 365.25;


            cloudCoverAvg = cloudCoverAvg / 365.25;
            cloudCoverLetoAvg = cloudCoverLetoAvg / daysInSummer;
            cloudCoverPodzimAvg = cloudCoverPodzimAvg / daysInAutumn;
            cloudCoverZimaAvg = cloudCoverZimaAvg / daysInWinter;
            cloudCoverJaroAvg = cloudCoverJaroAvg / daysInSpring;

            seasonalTemps.get("jaro").add(zaokrouhli(jaroAvgTeplotas / daysInSpring));
            seasonalTemps.get("leto").add(zaokrouhli(letoAvgTeplotas / daysInSummer));
            seasonalTemps.get("podzim").add(zaokrouhli(podzimAvgTeplotas / daysInAutumn));
            seasonalTemps.get("zima").add(zaokrouhli(zimaAvgTeplotas / daysInWinter));

            // Výpočet průměrné teploty na základě skutečného počtu dní
            csvWriter.append(String.valueOf(i + Integer.parseInt(dny.getString(0).split("-")[0]))).append(",")
                    .append(String.valueOf(zaokrouhli(zimaAvgTeplotas / daysInWinter))).append(",")
                    .append(String.valueOf(zaokrouhli(zimaMaxTeplota))).append(",")
                    .append(String.valueOf(zaokrouhli(zimaMinTeplota))).append(",")
                    .append(String.valueOf(zaokrouhli(cloudCoverZimaAvg))).append(",")
                    .append(String.valueOf(zaokrouhli(jaroAvgTeplotas / daysInSpring))).append(",")
                    .append(String.valueOf(zaokrouhli(jaroMaxTeplota))).append(",")
                    .append(String.valueOf(zaokrouhli(jaroMinTeplota))).append(",")
                    .append(String.valueOf(zaokrouhli(cloudCoverJaroAvg))).append(",")
                    .append(String.valueOf(zaokrouhli(letoAvgTeplotas / daysInSummer))).append(",")
                    .append(String.valueOf(zaokrouhli(letoMaxTeplota))).append(",")
                    .append(String.valueOf(zaokrouhli(letoMinTeplota))).append(",")
                    .append(String.valueOf(zaokrouhli(cloudCoverLetoAvg))).append(",")
                    .append(String.valueOf(zaokrouhli(podzimAvgTeplotas / daysInAutumn))).append(",")
                    .append(String.valueOf(zaokrouhli(podzimMaxTeplota))).append(",")
                    .append(String.valueOf(zaokrouhli(podzimMinTeplota))).append(",")
                    .append(String.valueOf(zaokrouhli(cloudCoverPodzimAvg))).append(",")
                    .append(String.valueOf(zaokrouhli(rain))).append(",")
                    .append(String.valueOf(zaokrouhli(snowfall))).append(",")
                    .append(String.valueOf(zaokrouhli(cloudCoverAvg))).append(",")
                    .append(String.valueOf(zaokrouhli(avgTeplota))).append(",")
                    .append(String.valueOf(zaokrouhli(maxTeplota))).append(",")
                    .append(String.valueOf(zaokrouhli(minTeplota))).append(",").append("\n");

            System.out.println(i + Integer.parseInt(dny.getString(0).split("-")[0]) + " Zima; Průměrná teplota: " + zaokrouhli(zimaAvgTeplotas / daysInWinter));
            System.out.println(i + Integer.parseInt(dny.getString(0).split("-")[0]) + " Jaro; Průměrná teplota: " + zaokrouhli(jaroAvgTeplotas / daysInSpring));
            System.out.println(i + Integer.parseInt(dny.getString(0).split("-")[0]) + " Leto; Průměrná teplota: " + zaokrouhli(letoAvgTeplotas / daysInSummer));
            System.out.println(i + Integer.parseInt(dny.getString(0).split("-")[0]) + " Podzim; Průměrná teplota: " + zaokrouhli(podzimAvgTeplotas / daysInAutumn));
            System.out.println(i + Integer.parseInt(dny.getString(0).split("-")[0]) + " Rainfall: " + rain + "; Snowfall: " + snowfall + "; Cloud Cover: " + cloudCoverAvg);
            System.out.println("-------------------------------------------------------------------------------------------------");

            System.out.println("JARO: median = " + median(seasonalTemps.get("jaro")) + " °C, modus = " + modus(seasonalTemps.get("jaro")) + " °C");
            System.out.println("LÉTO: median = " + median(seasonalTemps.get("leto")) + " °C, modus = " + modus(seasonalTemps.get("leto")) + " °C");
            System.out.println("PODZIM: median = " + median(seasonalTemps.get("podzim")) + " °C, modus = " + modus(seasonalTemps.get("podzim")) + " °C");
            System.out.println("ZIMA: median = " + median(seasonalTemps.get("zima")) + " °C, modus = " + modus(seasonalTemps.get("zima")) + " °C");
        }


        csvWriter.flush();
        csvWriter.close();
        System.out.println("CSV file generated: out/weather_data_yearly.csv");
    }

    public static double median(List<Double> values) {
        if (values.isEmpty()) return Double.NaN;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 1) {
            return sorted.get(size / 2);
        } else {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        }
    }

    public static double modus(List<Double> values) {
        if (values.isEmpty()) return Double.NaN;
        Map<Double, Integer> freqMap = new HashMap<>();
        for (double val : values) {
            freqMap.put(val, freqMap.getOrDefault(val, 0) + 1);
        }

        double mode = values.get(0);
        int maxCount = 0;
        for (Map.Entry<Double, Integer> entry : freqMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mode = entry.getKey();
            }
        }

        return mode;
    }

    private static double zaokrouhli(double cislo) {
        BigDecimal bigDecimal = new BigDecimal(cislo);
        return bigDecimal.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static void startup() {
        System.out.println("""
                
                 __      __               __  .__                ___________     __         .__                    ____     ________ \s
                /  \\    /  \\ ____ _____ _/  |_|  |__   __________\\_   _____/____/  |_  ____ |  |__   ___________  /_   |    \\_____  \\\s
                \\   \\/\\/   // __ \\\\__  \\\\   __\\  |  \\_/ __ \\_  __ \\    __)/ __ \\   __\\/ ___\\|  |  \\_/ __ \\_  __ \\  |   |     /  ____/\s
                 \\        /\\  ___/ / __ \\|  | |   Y  \\  ___/|  | \\/     \\\\  ___/|  | \\  \\___|   Y  \\  ___/|  | \\/  |   |    /       \\\s
                  \\__/\\  /  \\___  >____  /__| |___|  /\\___  >__|  \\___  / \\___  >__|  \\___  >___|  /\\___  >__|     |___| /\\ \\_______ \\
                       \\/       \\/     \\/          \\/     \\/          \\/      \\/          \\/     \\/     \\/               \\/         \\/
                """);
        System.out.println("by Fejdam | https://github.com/FiDaLip/WeatherFetcher#");

        loadData();
        try {
            loadJSON(new File("out/response.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}