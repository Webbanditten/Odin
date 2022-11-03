package org.alexdev.kepler.util;

import org.alexdev.kepler.game.GameScheduler;
import org.alexdev.kepler.util.config.GameConfiguration;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

public class StringUtil {
    /**
     * Checks if is null or empty.
     *
     * @param param the param
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(Object param) {
        String str = param == null ? null : param.toString();
        return str == null || str.trim().length() == 0;
    }

    /**
     * Filter input.
     *
     * @param input the input
     * @param filerNewline if new lines (ENTER) should be filtered
     * @return the string
     */
    public static String filterInput(String input, boolean filerNewline) {
        input = input.replace((char)1, ' ');
        input = input.replace((char)2, ' ');
        input = input.replace((char)9, ' ');
        input = input.replace((char)10, ' ');
        input = input.replace((char)12, ' ');

        if (filerNewline) {
            input = input.replace((char)13, ' ');
        }

        if (GameConfiguration.getInstance().getBoolean("normalise.input.strings")) {
            input = Normalizer.normalize(input, Normalizer.Form.NFD);
        }
        
        return input;
    }

    /**
     * Paginate a list of items.
     *
     * @param <T> the generic type
     * @param originalList the original list
     * @param chunkSize the chunk size
     * @return the list
     */
    public static <T> Map<Integer, List<T>> paginate(List<T> originalList, int chunkSize) {
        Map<Integer, List<T>> chunks = new LinkedHashMap<>();
        List<List<T>> listOfChunks = new ArrayList<>();

        for (int i = 0; i < originalList.size() / chunkSize; i++) {
            listOfChunks.add(originalList.subList(i * chunkSize, i * chunkSize + chunkSize));
        }

        if (originalList.size() % chunkSize != 0) {
            listOfChunks.add(originalList.subList(originalList.size() - originalList.size() % chunkSize, originalList.size()));
        }

        for (int i = 0; i < listOfChunks.size(); i++) {
            chunks.put(i, listOfChunks.get(i));
        }

        return chunks;
    }

    /**
     * Round to two decimal places.
     *
     * @param decimal the decimal
     * @return the double
     */
    public static double format(double decimal) {
        return Math.round(decimal * 100.0) / 100.0;
    }

    /**
     * Split.
     *
     * @param str the string
     * @param delim the delimiter
     * @return the list
     */
    public static List<String> split(String str, String delim) {
        return new ArrayList<>(Arrays.asList(str.split(delim)));
    }

    public static String charsetEncode (String input){
        String output = "";
        try {
            output = new String(input.getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return output;
    }


    /**
     * Get encoding for strings
     *
     * @return the encoding
     */
    public static Charset getCharset() {
        return StandardCharsets.ISO_8859_1;
    }

    /**
     * Get words in a string
     * @param s the string to get the list for
     * @return the list of words
     */
    public static String[] getWords(String s) {
        String[] words = s.split("\\s+");

        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].replaceAll("[^\\w]", "");
        }

        return words;
    }
}
