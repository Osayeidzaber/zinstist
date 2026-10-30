package dev.jilaton.client.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Timestamped lyrics parsed from LRC text supplied by the user. Supports multiple timestamps per line,
 * the {@code [offset:±ms]} tag and enhanced per-word timing ({@code <mm:ss.xx>word}).
 */
public final class Lyrics {
    private static final Pattern TIME = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]");
    private static final Pattern WORD = Pattern.compile("<(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?>");
    private static final Pattern OFFSET = Pattern.compile("\\[offset:\\s*([+-]?\\d+)]", Pattern.CASE_INSENSITIVE);

    public record Word(long startMs, String text) { }

    public record Line(long startMs, String text, List<Word> words) {
        public boolean timedWords() { return !words.isEmpty(); }
    }

    private final List<Line> lines;

    private Lyrics(List<Line> lines) { this.lines = lines; }

    public static final Lyrics EMPTY = new Lyrics(List.of());

    public List<Line> lines() { return lines; }
    public boolean isEmpty() { return lines.isEmpty(); }

    public static Lyrics parse(String text) {
        long offset = 0;
        Matcher offsetMatcher = OFFSET.matcher(text);
        if (offsetMatcher.find()) offset = Long.parseLong(offsetMatcher.group(1));
        List<Line> lines = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            Matcher m = TIME.matcher(raw);
            List<Long> stamps = new ArrayList<>();
            int end = 0;
            while (m.find() && m.start() == end) {
                stamps.add(millis(m.group(1), m.group(2), m.group(3)));
                end = m.end();
            }
            if (stamps.isEmpty()) continue;
            String body = raw.substring(end);
            List<Word> words = new ArrayList<>();
            Matcher w = WORD.matcher(body);
            int last = -1;
            long lastTime = 0;
            StringBuilder plain = new StringBuilder();
            while (w.find()) {
                if (last >= 0) {
                    String piece = body.substring(last, w.start());
                    if (!piece.isEmpty()) words.add(new Word(lastTime - offset, piece));
                    plain.append(piece);
                } else plain.append(body, 0, w.start());
                lastTime = millis(w.group(1), w.group(2), w.group(3));
                last = w.end();
            }
            if (last >= 0) {
                String piece = body.substring(last);
                if (!piece.isEmpty()) words.add(new Word(lastTime - offset, piece));
                plain.append(piece);
            } else plain.append(body);
            String clean = plain.toString().strip();
            for (long stamp : stamps) lines.add(new Line(Math.max(0, stamp - offset), clean, List.copyOf(words)));
        }
        lines.sort((a, b) -> Long.compare(a.startMs(), b.startMs()));
        return new Lyrics(Collections.unmodifiableList(lines));
    }

    private static long millis(String minutes, String seconds, String fraction) {
        long ms = Long.parseLong(minutes) * 60_000 + Long.parseLong(seconds) * 1000;
        if (fraction != null) {
            String f = (fraction + "00").substring(0, 3);
            ms += Long.parseLong(f);
        }
        return ms;
    }

    /** Index of the line active at {@code positionMs}, or -1 before the first line. */
    public int indexAt(long positionMs) {
        int lo = 0, hi = lines.size() - 1, found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (lines.get(mid).startMs() <= positionMs) { found = mid; lo = mid + 1; } else hi = mid - 1;
        }
        return found;
    }

    /** Number of words of {@code line} already sung at {@code positionMs}. */
    public static int wordsSung(Line line, long positionMs) {
        int count = 0;
        for (Word word : line.words()) if (word.startMs() <= positionMs) count++;
        return count;
    }
}
