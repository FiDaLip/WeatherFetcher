package cz.fejdam;

import java.util.HashMap;
import java.util.Map;

public class KodyPocasi {

    private static final Map<Integer, String> dict = new HashMap<>();

    static {
        dict.put(0, "Jasno");
        dict.put(1, "Převážně jasno");
        dict.put(2, "Částečně zataženo");
        dict.put(3, "Zataženo");
        dict.put(45, "Mlha");
        dict.put(48, "Mlha s námrazou");
        dict.put(51, "Slabé mrholení");
        dict.put(53, "Mírné mrholení");
        dict.put(55, "Silné mrholení");
        dict.put(56, "Slabé mrznoucí mrholení");
        dict.put(57, "Silné mrznoucí mrholení");
        dict.put(61, "Slabý déšť");
        dict.put(63, "Mírný déšť");
        dict.put(65, "Silný déšť");
        dict.put(66, "Slabý mrznoucí déšť");
        dict.put(67, "Silný mrznoucí déšť");
        dict.put(71, "Slabé sněžení");
        dict.put(73, "Mírné sněžení");
        dict.put(75, "Silné sněžení");
        dict.put(77, "Sněhové krupky");
        dict.put(80, "Slabé přeháňky");
        dict.put(81, "Mírné přeháňky");
        dict.put(82, "Silné přeháňky");
        dict.put(85, "Slabé sněhové přeháňky");
        dict.put(86, "Silné sněhové přeháňky");
        dict.put(95, "Bouřka");
        dict.put(96, "Bouřka s mírným krupobitím");
        dict.put(99, "Bouřka se silným krupobitím");
    }

    public static String popisPocasi(int kod) {
        return dict.getOrDefault(kod, "Neznámý kód počasí");
    }
}
