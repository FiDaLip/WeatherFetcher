package cz.fejdam;


import java.io.*;
import static cz.fejdam.WeatherFetcher.loadData;
import static cz.fejdam.WeatherFetcher.loadJSON;

public class Main {



    public static void main(String[] args) {
        startup();
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