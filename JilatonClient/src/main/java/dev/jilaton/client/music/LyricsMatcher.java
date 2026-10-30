/* Adapted from Anubis 0.9.9.3 LyricsMatcher (Horcus / 4ldenz), GPL-3.0-only, with the user's permission.
 * Reviewed recovered source, not an original source reconstruction. See ATTRIBUTION.md. */
package dev.jilaton.client.music;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Scores LRCLIB candidates against the playing track. Titles are compared after folding accents, case and
 * punctuation and after stripping "(feat. …)", "- Remastered 2011" style suffixes; artists match on any
 * shared credited name; durations must agree within three seconds. Synced lyrics outrank plain ones.
 */
final class LyricsMatcher {
    static final double DURATION_TOLERANCE_SECONDS = 3.0;
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern APOSTROPHES = Pattern.compile("['\u2019`\u00b4]");
    private static final Pattern BRACKETED = Pattern.compile("\\s*[(\\[][^)\\]]*\\b(feat|ft|featuring|with|remaster(ed)?|explicit|clean|mono|stereo|bonus|deluxe|anniversary|edition|single version|album version)\\b[^)\\]]*[)\\]]", Pattern.CASE_INSENSITIVE);
    private static final Pattern DASHED = Pattern.compile("\\s+[-\u2013\u2014]\\s+[^-\u2013\u2014]*\\b(remaster(ed)?|explicit|clean|mono|stereo|bonus track|deluxe|anniversary|edition|single version|album version)\\b.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FEATURING = Pattern.compile("\\s+(feat\\.?|ft\\.|featuring)\\s+.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARTIST_SPLIT = Pattern.compile("\\s*(?:,|;|/|&|\u2022|\\+|\\bx\\b|\\band\\b|\\bwith\\b|\\bfeat\\b\\.?|\\bft\\b\\.?|\\bfeaturing\\b)\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREDIT_SPLIT = Pattern.compile("\\s*[,;\u2022]\\s*|\\s+(?:&|x|feat\\.?|ft\\.?|featuring|with)\\s+", Pattern.CASE_INSENSITIVE);

    private LyricsMatcher() { }

    /** Best usable candidate, or null. Ties prefer the closest duration. */
    static JsonObject best(Iterable<JsonElement> candidates, String title, String artist, int durationSeconds) {
        JsonObject best = null;
        int bestScore = -1;
        double bestDrift = Double.MAX_VALUE;
        for (JsonElement element : candidates) {
            if (!element.isJsonObject()) continue;
            JsonObject candidate = element.getAsJsonObject();
            int score = score(candidate, title, artist, durationSeconds);
            if (score < 0) continue;
            double drift = drift(candidate, durationSeconds);
            if (score > bestScore || score == bestScore && drift < bestDrift) {
                best = candidate;
                bestScore = score;
                bestDrift = drift;
            }
        }
        return best;
    }

    static int score(JsonObject candidate, String title, String artist, int durationSeconds) {
        boolean synced = has(candidate, "syncedLyrics"), plain = has(candidate, "plainLyrics");
        boolean instrumental = candidate.has("instrumental") && !candidate.get("instrumental").isJsonNull() && candidate.get("instrumental").getAsBoolean();
        if (!synced && !plain && !instrumental) return -1;
        int duration = durationScore(candidate, durationSeconds);
        if (duration < 0) return -1;
        int titleScore = titleScore(title, str(candidate, "trackName"));
        if (titleScore <= 0) return -1;
        int artistScore = artistScore(artist, str(candidate, "artistName"));
        if (artistScore <= 0) return -1;
        return titleScore + artistScore + duration + (synced ? 25 : plain ? 5 : 8);
    }

    private static int durationScore(JsonObject candidate, int durationSeconds) {
        double offered = duration(candidate);
        if (durationSeconds <= 0 || offered <= 0) return 0;
        double drift = Math.abs(offered - durationSeconds);
        if (drift > DURATION_TOLERANCE_SECONDS) return -1;
        return (int) Math.round(20 * (1 - drift / 6));
    }

    private static double drift(JsonObject candidate, int durationSeconds) {
        double offered = duration(candidate);
        return durationSeconds <= 0 || offered <= 0 ? DURATION_TOLERANCE_SECONDS : Math.abs(offered - durationSeconds);
    }

    static int titleScore(String wanted, String offered) {
        String a = normalize(wanted), b = normalize(offered);
        if (a.isEmpty() || b.isEmpty()) return 0;
        if (a.equals(b)) return 40;
        String baseA = normalize(baseTitle(wanted)), baseB = normalize(baseTitle(offered));
        if (!baseA.isEmpty() && baseA.equals(baseB)) return 30;
        return containsWords(baseA, baseB) || containsWords(baseB, baseA) ? 12 : 0;
    }

    static int artistScore(String wanted, String offered) {
        String a = normalize(wanted), b = normalize(offered);
        if (a.isEmpty() || b.isEmpty()) return 0;
        if (a.equals(b)) return 30;
        Set<String> wantedArtists = artists(wanted), offeredArtists = artists(offered);
        for (String name : wantedArtists) if (offeredArtists.contains(name)) return 20;
        String first = wantedArtists.isEmpty() ? a : wantedArtists.iterator().next();
        return containsWords(b, first) || containsWords(a, b) ? 10 : 0;
    }

    static String baseTitle(String title) {
        if (title == null) return "";
        String base = BRACKETED.matcher(title).replaceAll("");
        base = DASHED.matcher(base).replaceFirst("");
        base = FEATURING.matcher(base).replaceFirst("").strip();
        return base.isEmpty() ? title.strip() : base;
    }

    static String primaryArtist(String artist) {
        if (artist == null) return "";
        for (String part : CREDIT_SPLIT.split(artist.strip())) if (!part.isBlank()) return part.strip();
        return artist.strip();
    }

    /**
     * Search terms: the reported title/artist, then the base title with the primary artist, then the
     * accent-folded variants of both. LRCLIB matches literal text, so "Monëy so big" finds nothing while
     * "Money so big" finds the entry.
     */
    static List<String[]> searchTerms(String title, String artist) {
        List<String[]> terms = new ArrayList<>(4);
        add(terms, title, artist);
        add(terms, baseTitle(title), primaryArtist(artist));
        add(terms, folded(title), folded(artist));
        add(terms, folded(baseTitle(title)), folded(primaryArtist(artist)));
        return terms;
    }

    private static void add(List<String[]> terms, String title, String artist) {
        if (title == null || title.isBlank()) return;
        for (String[] term : terms) if (term[0].equals(title) && term[1].equals(artist)) return;
        terms.add(new String[]{title, artist});
    }

    /** Strips combining accents while keeping case and punctuation, for literal text lookups. */
    static String folded(String text) {
        if (text == null || text.isEmpty()) return "";
        return MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
    }

    static String normalize(String text) {
        if (text == null || text.isEmpty()) return "";
        String folded = folded(text).toLowerCase(Locale.ROOT).replace('\u0131', 'i').replace("\u00df", "ss")
                .replace("\u00e6", "ae").replace("\u0153", "oe").replace('\u00f8', 'o').replace('\u0142', 'l')
                .replace('\u0111', 'd').replace("&", " and ");
        folded = APOSTROPHES.matcher(folded).replaceAll("");
        return NON_WORD.matcher(folded).replaceAll(" ").strip();
    }

    private static Set<String> artists(String credit) {
        Set<String> names = new LinkedHashSet<>();
        for (String part : ARTIST_SPLIT.split(credit)) {
            String name = normalize(part);
            if (!name.isEmpty()) names.add(name);
        }
        return names;
    }

    private static boolean containsWords(String haystack, String needle) {
        if (needle.length() < 3 || haystack.length() < needle.length()) return false;
        return (" " + haystack + " ").contains(" " + needle + " ");
    }

    private static boolean has(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() && !o.get(key).getAsString().isBlank();
    }

    private static String str(JsonObject o, String key) { return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : ""; }

    private static double duration(JsonObject o) {
        return o.has("duration") && !o.get("duration").isJsonNull() ? o.get("duration").getAsDouble() : 0;
    }
}
