package com.antest1.gotobrowser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Arrays;
import java.util.List;

// Reference: https://github.com/KC3Kai/KC3Kai/issues/1180
public class KcVoiceUtils {
    public static final int[] resourceKeys = {
            6657, 5699, 3371, 8909, 7719, 6229, 5449, 8561, 2987, 5501,
            3127, 9319, 4365, 9811, 9927, 2423, 3439, 1865, 5925, 4409,
            5509, 1517, 9695, 9255, 5325, 3691, 5519, 6949, 5607, 9539,
            4133, 7795, 5465, 2659, 6381, 6875, 4019, 9195, 5645, 2887,
            1213, 1815, 8671, 3015, 3147, 2991, 7977, 7045, 1619, 7909,
            4451, 6573, 4545, 8251, 5983, 2849, 7249, 7449, 9477, 5963,
            2711, 9019, 7375, 2201, 5631, 4893, 7653, 3719, 8819, 5839,
            1853, 9843, 9119, 7023, 5681, 2345, 9873, 6349, 9315, 3795,
            9737, 4633, 4173, 7549, 7171, 6147, 4723, 5039, 2723, 7815,
            6201, 5999, 5339, 4431, 2911, 4435, 3611, 4423, 9517, 3243
    };
    public static final Integer[] voiceDiffs = {
            2475,    0,    0, 8691, 7847, 3595, 1767, 3311, 2507,
            9651, 5321, 4473, 7117, 5947, 9489, 2669, 8741, 6149,
            1301, 7297, 2975, 6413, 8391, 9705, 2243, 2091, 4231,
            3107, 9499, 4205, 6013, 3393, 6401, 6985, 3683, 9447,
            3287, 5181, 7587, 9353, 2135, 4947, 5405, 5223, 9457,
            5767, 9265, 8191, 3927, 3061, 2805, 3273, 7331
    };
    public static final List<Integer> voiceDiffsList = Arrays.asList(voiceDiffs);

    public static final int[] workingDiffs = {
            2475, 6547, 1471, 8691, 7847, 3595, 1767, 3311, 2507,
            9651, 5321, 4473, 7117, 5947, 9489, 2669, 8741, 6149,
            1301, 7297, 2975, 6413, 8391, 9705, 2243, 2091, 4231,
            3107, 9499, 4205, 6013, 3393, 6401, 6985, 3683, 9447,
            3287, 5181, 7587, 9353, 2135, 4947, 5405, 5223, 9457,
            5767, 9265, 8191, 3927, 3061, 2805, 3273, 7331
    };

    // valentines 2016, hinamatsuri 2015
    // valentines 2016, hinamatsuri 2015
    // whiteday 2015
    // whiteday 2015
    public static final JsonObject specialDiffs =
            new JsonParser().parse("{\"1555\":2,\"3347\":3,\"6547\":2,\"1471\":3}")
                    .getAsJsonObject();

    // Graf Zeppelin (Kai):
    //   17:Yasen(2) is replaced with 917. might map to 17, but not for now;
    //   18 still used at day as random Attack, 918 used at night opening
    public static final JsonObject specialShipVoices =
            new JsonParser().parse("{\"432\": {\"917\": 917, \"918\": 918}, \"353\": {\"917\": 917, \"918\": 918}}")
                    .getAsJsonObject();

    // These ships got special (unused?) voice line (6, aka. Repair) implemented,
    // tested by trying and succeeding to http fetch mp3 from kc server
    public static final int[] specialReairVoiceShips = {
        56, 160, 224,  // Naka
        65, 194, 268,  // Haguro
        114, 200, 290, // Abukuma
        123, 142, 295, // Kinukasa
        126, 398,      // I-168
        127, 399,      // I-58
        135, 304,      // Naganami
        136,           // Yamato Kai
        418,           // Satsuki Kai Ni
        496,           // Zara due
    };

    public static int getFilenameByVoiceLine(int ship_id, int lineNum) {
        return lineNum <= 53 ? 100000 + 17 * (ship_id + 7) * (workingDiffs[lineNum - 1]) % 99173 : lineNum;
    }

    public static int getVoiceDiffByFilename(String ship_id, String filename) {
        int ship_id_val = Integer.parseInt(ship_id, 10);
        int f = Integer.parseInt(filename, 10);
        int k = 17 * (ship_id_val + 7);
        int r = f - 100000;
        if(f > 53 && r < 0) {
            return f;
        } else {
            for (int i = 0; i < 2600; ++i) {
                int a = r + i * 99173;
                if (a % k == 0) {
                    return a / k;
                }
            }
        }
        return -1;
    }

    public static int getVoiceLineByFilename(String ship_id, String filename) {
        if (ship_id.equals("9999")) {
            return Integer.parseInt(filename);
        }
        // Some ships use special voice line filenames
        JsonObject specialMap = specialShipVoices.getAsJsonObject(ship_id);
        if (specialMap != null && specialMap.has(filename)) {
            return specialMap.get(filename).getAsInt();
        }
        int computedDiff = getVoiceDiffByFilename(ship_id, filename);
        int computedIndex = voiceDiffsList.indexOf(computedDiff);
        // If computed diff is not in voiceDiffs, return the computedDiff itself so we can lookup quotes via voiceDiff
        return computedIndex > -1 ? computedIndex + 1 : computedDiff;
    }
}
