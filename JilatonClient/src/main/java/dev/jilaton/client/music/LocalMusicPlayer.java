package dev.jilaton.client.music;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Streams the user's local files through one OpenAL source on Minecraft's existing audio context. A single
 * daemon thread decodes ahead into a small ring of buffers; it exits and frees its source as soon as
 * playback stops, so nothing keeps running when music is idle or the feature is disabled.
 */
public final class LocalMusicPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Jilaton Music");
    private static final int BUFFERS = 4;
    private static final int FRAMES_PER_BUFFER = 8192;

    public enum Repeat { OFF, ALL, ONE }

    private final List<LocalLibrary.Track> queue = new ArrayList<>();
    private final List<Integer> order = new ArrayList<>();
    private final Random random = new Random();
    private int cursor = -1;
    private boolean shuffle;
    private Repeat repeat = Repeat.ALL;
    private volatile float volume = 0.6F;
    private volatile Worker worker;
    private volatile LocalLibrary.Track current;
    private volatile Lyrics lyrics = Lyrics.EMPTY;
    private volatile String error = "";

    public synchronized void setQueue(List<LocalLibrary.Track> tracks, int start) {
        queue.clear();
        for (LocalLibrary.Track track : tracks) if (track.supported()) queue.add(track);
        rebuildOrder();
        if (queue.isEmpty()) { stop(); return; }
        LocalLibrary.Track wanted = start >= 0 && start < tracks.size() ? tracks.get(start) : null;
        int index = wanted == null ? 0 : Math.max(0, queue.indexOf(wanted));
        cursor = order.indexOf(index);
        if (shuffle && wanted != null) { order.remove(Integer.valueOf(index)); order.add(0, index); cursor = 0; }
        playCursor();
    }

    public synchronized List<LocalLibrary.Track> queue() { return List.copyOf(queue); }

    private void rebuildOrder() {
        order.clear();
        for (int i = 0; i < queue.size(); i++) order.add(i);
        if (shuffle) Collections.shuffle(order, random);
    }

    public synchronized void setShuffle(boolean shuffle) {
        if (this.shuffle == shuffle) return;
        this.shuffle = shuffle;
        int playing = cursor >= 0 && cursor < order.size() ? order.get(cursor) : -1;
        rebuildOrder();
        if (playing >= 0) {
            if (shuffle) { order.remove(Integer.valueOf(playing)); order.add(0, playing); }
            cursor = order.indexOf(playing);
        }
    }

    public synchronized boolean shuffle() { return shuffle; }
    public synchronized void setRepeat(Repeat repeat) { this.repeat = repeat; }
    public synchronized Repeat repeat() { return repeat; }
    public void setVolume(float volume) { this.volume = Math.max(0, Math.min(1, volume)); }
    public float volume() { return volume; }
    public LocalLibrary.Track current() { return current; }
    public Lyrics lyrics() { return lyrics; }
    public String error() { return error; }

    public boolean playing() { Worker w = worker; return w != null && !w.paused && w.isAlive(); }
    public boolean active() { Worker w = worker; return w != null && w.isAlive(); }

    public void togglePause() {
        Worker w = worker;
        if (w != null && w.isAlive()) w.paused = !w.paused;
        else synchronized (this) { if (!queue.isEmpty()) playCursor(); }
    }

    public synchronized void next() { advance(1, true); }
    public synchronized void previous() {
        Worker w = worker;
        if (w != null && w.positionMs() > 3000) { w.seekMs = 0; return; }
        advance(-1, true);
    }

    public void seek(long ms) { Worker w = worker; if (w != null) w.seekMs = Math.max(0, ms); }

    public long positionMs() { Worker w = worker; return w == null ? 0 : w.positionMs(); }
    public long durationMs() { Worker w = worker; return w == null ? 0 : w.durationMs; }
    /** RMS level of the audio currently audible, 0..1; only for the user's own local files. */
    public float amplitude() { Worker w = worker; return w == null || w.paused ? 0 : w.amplitude; }

    public synchronized void stop() {
        Worker w = worker;
        worker = null;
        current = null;
        lyrics = Lyrics.EMPTY;
        if (w != null) w.running = false;
    }

    private void advance(int step, boolean manual) {
        if (queue.isEmpty()) { stop(); return; }
        if (!manual && repeat == Repeat.ONE) { playCursor(); return; }
        int next = cursor + step;
        if (next >= order.size() || next < 0) {
            if (repeat == Repeat.OFF && !manual) { stop(); return; }
            next = Math.floorMod(next, order.size());
        }
        cursor = next;
        playCursor();
    }

    private void playCursor() {
        Worker old = worker;
        if (old != null) old.running = false;
        if (cursor < 0 || cursor >= order.size()) cursor = 0;
        LocalLibrary.Track track = queue.get(order.get(cursor));
        try {
            AudioStream stream = AudioStream.open(track.file());
            current = LocalLibrary.withTags(track, stream.tags());
            lyrics = Lyrics.EMPTY;
            if (track.lyrics() != null) {
                try { lyrics = Lyrics.parse(Files.readString(track.lyrics())); }
                catch (Exception exception) { LOGGER.warn("Could not read lyrics {}", track.lyrics(), exception); }
            }
            error = "";
            Worker w = new Worker(stream);
            worker = w;
            w.start();
        } catch (Exception exception) {
            error = "Cannot play " + track.file().getFileName() + ": " + exception.getMessage();
            LOGGER.warn(error);
            current = null;
            worker = null;
        }
    }

    private synchronized void finished(Worker w) {
        if (worker == w) advance(1, false);
    }

    private final class Worker extends Thread {
        private final AudioStream stream;
        private final long durationMs;
        volatile boolean running = true;
        volatile boolean paused;
        volatile long seekMs = -1;
        volatile float amplitude;
        private volatile long playedFrames;
        private volatile int source;
        private final float[] bufferRms = new float[BUFFERS];
        private final int[] bufferFrames = new int[BUFFERS];
        private int head;

        Worker(AudioStream stream) {
            super("Jilaton Music");
            setDaemon(true);
            this.stream = stream;
            long frames = stream.totalFrames();
            durationMs = frames > 0 ? frames * 1000 / stream.sampleRate() : 0;
        }

        long positionMs() {
            int src = source;
            long frames = playedFrames;
            if (src != 0) {
                try { frames += AL10.alGetSourcei(src, AL11.AL_SAMPLE_OFFSET); } catch (RuntimeException ignored) { }
            }
            return frames * 1000 / Math.max(1, stream.sampleRate());
        }

        @Override
        public void run() {
            int format = stream.channels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            ShortBuffer pcm = BufferUtils.createShortBuffer(FRAMES_PER_BUFFER * stream.channels());
            int[] buffers = new int[BUFFERS];
            boolean ended = false;
            try {
                source = AL10.alGenSources();
                AL10.alGenBuffers(buffers);
                AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
                AL10.alSource3f(source, AL10.AL_POSITION, 0, 0, 0);
                AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 0);
                int queued = 0;
                for (int i = 0; i < BUFFERS; i++) if (fill(buffers[i], pcm, format, i)) queued++;
                AL10.alSourceQueueBuffers(source, java.util.Arrays.copyOf(buffers, queued));
                AL10.alSourcePlay(source);
                boolean wasPaused = false;
                while (running) {
                    AL10.alSourcef(source, AL10.AL_GAIN, volume * MusicCenter.masterGain());
                    if (paused != wasPaused) {
                        if (paused) AL10.alSourcePause(source); else AL10.alSourcePlay(source);
                        wasPaused = paused;
                    }
                    long seek = seekMs;
                    if (seek >= 0) {
                        seekMs = -1;
                        AL10.alSourceStop(source);
                        AL10.alSourcei(source, AL10.AL_BUFFER, 0);
                        long frame = seek * stream.sampleRate() / 1000;
                        stream.seek(frame);
                        playedFrames = frame;
                        ended = false;
                        queued = 0;
                        head = 0;
                        for (int i = 0; i < BUFFERS; i++) if (fill(buffers[i], pcm, format, i)) queued++;
                        if (queued > 0) AL10.alSourceQueueBuffers(source, java.util.Arrays.copyOf(buffers, queued));
                        if (!paused) AL10.alSourcePlay(source);
                    }
                    int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
                    while (processed-- > 0) {
                        int buffer = AL10.alSourceUnqueueBuffers(source);
                        playedFrames += bufferFrames[head];
                        int slot = head;
                        head = (head + 1) % BUFFERS;
                        if (!ended && fill(buffer, pcm, format, (slot + BUFFERS - 0) % BUFFERS)) AL10.alSourceQueueBuffers(source, buffer);
                        else ended = true;
                    }
                    amplitude = bufferRms[head];
                    int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
                    int pending = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
                    if (state == AL10.AL_STOPPED && !paused) {
                        if (pending == 0 && ended) break;
                        if (pending > 0) AL10.alSourcePlay(source); // recover from an underrun
                    }
                    if (AL10.alGetError() != AL10.AL_NO_ERROR) { error = "Audio device changed; playback stopped"; running = false; break; }
                    Thread.sleep(12);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (Throwable failure) {
                error = "Playback failed: " + failure.getMessage();
                LOGGER.warn("Local music playback failed", failure);
                running = false;
            } finally {
                int src = source;
                source = 0;
                try {
                    if (src != 0) { AL10.alSourceStop(src); AL10.alSourcei(src, AL10.AL_BUFFER, 0); AL10.alDeleteSources(src); }
                    AL10.alDeleteBuffers(buffers);
                } catch (Throwable ignored) { }
                stream.close();
                amplitude = 0;
            }
            if (running) finished(this);
        }

        /** Decodes the next chunk into {@code buffer}; slot records its frame count and loudness. */
        private boolean fill(int buffer, ShortBuffer pcm, int format, int slot) {
            pcm.clear();
            int frames = stream.read(pcm);
            if (frames <= 0) return false;
            int samples = frames * stream.channels();
            double sum = 0;
            for (int i = 0; i < samples; i += 4) { double v = pcm.get(i) / 32768.0; sum += v * v; }
            bufferRms[slot] = (float) Math.min(1, Math.sqrt(sum / Math.max(1, samples / 4)) * 2.2);
            bufferFrames[slot] = frames;
            pcm.limit(samples);
            AL10.alBufferData(buffer, format, pcm, stream.sampleRate());
            return true;
        }
    }
}
