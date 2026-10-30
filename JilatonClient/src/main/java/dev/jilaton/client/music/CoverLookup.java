package dev.jilaton.client.music;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Album cover from Apple's public iTunes Search API (free, no key). Only a candidate whose title and
 * artist match the playing track is accepted, so an unrelated cover is never shown.
 */
final class CoverLookup {
    private CoverLookup() { }

    static byte[] find(String title, String artist) throws IOException {
        String term = URLEncoder.encode(LyricsMatcher.primaryArtist(artist) + " " + LyricsMatcher.baseTitle(title), StandardCharsets.UTF_8);
        JsonObject response = JsonParser.parseString(new String(get("https://itunes.apple.com/search?entity=song&limit=10&term=" + term),
                StandardCharsets.UTF_8)).getAsJsonObject();
        for (JsonElement element : response.getAsJsonArray("results")) {
            JsonObject result = element.getAsJsonObject();
            if (!result.has("artworkUrl100")) continue;
            String offeredTitle = result.has("trackName") ? result.get("trackName").getAsString() : "";
            String offeredArtist = result.has("artistName") ? result.get("artistName").getAsString() : "";
            if (LyricsMatcher.titleScore(title, offeredTitle) <= 0 || LyricsMatcher.artistScore(artist, offeredArtist) <= 0) continue;
            String url = result.get("artworkUrl100").getAsString().replace("100x100bb", "300x300bb");
            if (!url.startsWith("https://")) continue;
            return get(url);
        }
        throw new IOException("No matching cover");
    }

    private static byte[] get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("User-Agent", "Jilaton Client 0.1");
        if (connection.getResponseCode() >= 300) throw new IOException("HTTP " + connection.getResponseCode());
        try (InputStream in = connection.getInputStream()) { return in.readNBytes(4 * 1024 * 1024); }
    }
}
