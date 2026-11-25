package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Eight deterministic phonological templates used to synthesize ancient names (18 §5.1.4, §11.1).
 */
public enum LanguageTemplate {
    RIVERINE(
            new String[]{"k", "t", "r", "m", "n", "s", "l", "v"},
            new String[]{"a", "e", "i", "u", "ia"},
            new String[]{"n", "r", "l", ""},
            new String[]{"ia", "ur", "en", "is"}
    ),
    STEPPE(
            new String[]{"kh", "t", "b", "g", "d", "z", "r", "s"},
            new String[]{"a", "o", "u", "ai", "ei"},
            new String[]{"n", "r", "k", "t"},
            new String[]{"ai", "un", "ar", "atai"}
    ),
    HIGHLAND(
            new String[]{"kr", "dr", "th", "g", "b", "m", "r", "k"},
            new String[]{"a", "o", "u", "e"},
            new String[]{"k", "d", "rn", "m"},
            new String[]{"or", "ak", "um", "gar"}
    ),
    MARITIME(
            new String[]{"th", "l", "n", "s", "p", "k", "m", "r"},
            new String[]{"a", "e", "i", "o", "ae"},
            new String[]{"s", "n", "l", ""},
            new String[]{"os", "ia", "is", "ea"}
    ),
    DESERT(
            new String[]{"q", "sh", "z", "h", "m", "n", "r", "t"},
            new String[]{"a", "i", "u", "aa"},
            new String[]{"m", "n", "r", "h"},
            new String[]{"am", "ir", "un", "at"}
    ),
    SYLVAN(
            new String[]{"f", "l", "w", "s", "y", "n", "m", "t"},
            new String[]{"e", "i", "a", "ea", "io"},
            new String[]{"l", "n", "th", ""},
            new String[]{"el", "en", "ia", "eth"}
    ),
    BOREAL(
            new String[]{"hj", "sk", "v", "r", "k", "n", "t", "f"},
            new String[]{"a", "i", "o", "u", "ei"},
            new String[]{"r", "l", "d", "n"},
            new String[]{"heim", "vik", "gard", "ald"}
    ),
    MONSOON(
            new String[]{"ch", "zh", "l", "y", "h", "m", "t", "s"},
            new String[]{"a", "i", "u", "ao", "ia"},
            new String[]{"n", "ng", ""},
            new String[]{"an", "zhou", "lin", "pura"}
    );

    public static final Codec<LanguageTemplate> CODEC = Codec.STRING.xmap(
            s -> LanguageTemplate.valueOf(s.toUpperCase(Locale.ROOT)),
            t -> t.name().toLowerCase(Locale.ROOT)
    );

    private final String[] onsets;
    private final String[] nuclei;
    private final String[] codas;
    private final String[] politySuffixes;

    LanguageTemplate(String[] onsets, String[] nuclei, String[] codas, String[] politySuffixes) {
        this.onsets = onsets;
        this.nuclei = nuclei;
        this.codas = codas;
        this.politySuffixes = politySuffixes;
    }

    public String generateRootName(long hash) {
        long h = mix(hash);
        int sylCount = 2 + (int) (Long.remainderUnsigned(h, 2L));
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < sylCount; i++) {
            h = mix(h + i * 0x9E3779B97F4A7C15L);
            String onset = onsets[(int) Long.remainderUnsigned(h, onsets.length)];
            String nucleus = nuclei[(int) Long.remainderUnsigned(h >>> 12, nuclei.length)];
            String coda = codas[(int) Long.remainderUnsigned(h >>> 24, codas.length)];
            sb.append(onset).append(nucleus).append(coda);
        }
        if (sb.isEmpty()) {
            return "Aru";
        }
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        return sb.length() > 20 ? sb.substring(0, 20) : sb.toString();
    }

    public String generatePolityName(long hash) {
        long h = mix(hash ^ 0x504F4C495459L);
        String root = generateRootName(h);
        String suffix = politySuffixes[(int) Long.remainderUnsigned(mix(h >>> 7), politySuffixes.length)];
        String combined = root.endsWith(suffix) ? root : root + suffix;
        return combined.length() > 28 ? combined.substring(0, 28) : combined;
    }

    public static LanguageTemplate fromOrdinal(int ord) {
        LanguageTemplate[] vals = values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : RIVERINE;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
