package dev.jilaton.client.music;

import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisComment;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Decoded 16-bit PCM source. Ogg Vorbis uses LWJGL's bundled stb_vorbis (already shipped with Minecraft);
 * WAV supports uncompressed 8/16-bit PCM. Other containers are reported as unsupported rather than
 * decoded through extra native dependencies.
 */
public interface AudioStream extends AutoCloseable {
    int channels();
    int sampleRate();
    /** Total frames (samples per channel), or -1 if unknown. */
    long totalFrames();
    /** Fills {@code out} with interleaved samples; returns frames read, 0 at end of stream. */
    int read(ShortBuffer out);
    void seek(long frame);
    Map<String, String> tags();
    @Override void close();

    static boolean supported(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".ogg") || name.endsWith(".oga") || name.endsWith(".wav");
    }

    static AudioStream open(Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".wav")) return new Wav(file);
        if (name.endsWith(".ogg") || name.endsWith(".oga")) return new Vorbis(file);
        throw new IOException("Unsupported audio format: " + file.getFileName());
    }

    final class Vorbis implements AudioStream {
        private final ByteBuffer data;
        private final long handle;
        private final int channels, rate;
        private final long frames;
        private final Map<String, String> tags = new HashMap<>();

        Vorbis(Path file) throws IOException {
            byte[] bytes = Files.readAllBytes(file);
            data = MemoryUtil.memAlloc(bytes.length);
            data.put(bytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer error = stack.mallocInt(1);
                handle = STBVorbis.stb_vorbis_open_memory(data, error, null);
                if (handle == MemoryUtil.NULL) {
                    MemoryUtil.memFree(data);
                    throw new IOException("Could not decode Ogg Vorbis (error " + error.get(0) + ")");
                }
                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                STBVorbis.stb_vorbis_get_info(handle, info);
                channels = info.channels();
                rate = info.sample_rate();
            }
            frames = STBVorbis.stb_vorbis_stream_length_in_samples(handle);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                STBVorbisComment comment = STBVorbisComment.malloc(stack);
                STBVorbis.stb_vorbis_get_comment(handle, comment);
                PointerBuffer list = comment.comment_list();
                if (list != null) for (int i = 0; i < comment.comment_list_length(); i++) {
                    String entry = MemoryUtil.memUTF8(list.get(i));
                    int eq = entry.indexOf('=');
                    if (eq > 0) tags.put(entry.substring(0, eq).toUpperCase(Locale.ROOT), entry.substring(eq + 1));
                }
            } catch (RuntimeException | LinkageError ignored) {
                // Older stb builds lack comment access; filenames still provide titles.
            }
        }

        @Override public int channels() { return channels; }
        @Override public int sampleRate() { return rate; }
        @Override public long totalFrames() { return frames; }
        @Override public Map<String, String> tags() { return tags; }

        @Override
        public int read(ShortBuffer out) {
            return STBVorbis.stb_vorbis_get_samples_short_interleaved(handle, channels, out);
        }

        @Override public void seek(long frame) { STBVorbis.stb_vorbis_seek(handle, (int) Math.max(0, frame)); }

        @Override
        public void close() {
            STBVorbis.stb_vorbis_close(handle);
            MemoryUtil.memFree(data);
        }
    }

    final class Wav implements AudioStream {
        private final FileChannel channel;
        private final int channels, rate, bits;
        private final long dataStart, dataLength;
        private ByteBuffer scratch = ByteBuffer.allocate(0);

        Wav(Path file) throws IOException {
            channel = FileChannel.open(file, StandardOpenOption.READ);
            try {
                ByteBuffer header = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
                channel.read(header, 0);
                header.flip();
                if (header.getInt() != 0x46464952 || header.getInt(8) != 0x45564157) throw new IOException("Not a RIFF/WAVE file");
                long position = 12;
                int ch = 0, sr = 0, bitsPerSample = 0;
                long start = -1, length = 0;
                ByteBuffer chunk = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
                while (position + 8 <= channel.size()) {
                    chunk.clear();
                    channel.read(chunk, position);
                    chunk.flip();
                    int id = chunk.getInt();
                    long size = Integer.toUnsignedLong(chunk.getInt());
                    if (id == 0x20746D66) { // "fmt "
                        ByteBuffer fmt = ByteBuffer.allocate((int) Math.min(size, 64)).order(ByteOrder.LITTLE_ENDIAN);
                        channel.read(fmt, position + 8);
                        fmt.flip();
                        int format = fmt.getShort() & 0xFFFF;
                        ch = fmt.getShort() & 0xFFFF;
                        sr = fmt.getInt();
                        fmt.getInt();
                        fmt.getShort();
                        bitsPerSample = fmt.getShort() & 0xFFFF;
                        if (format != 1 && format != 0xFFFE) throw new IOException("Only uncompressed PCM WAV is supported");
                    } else if (id == 0x61746164) { // "data"
                        start = position + 8;
                        length = Math.min(size, channel.size() - start);
                        break;
                    }
                    position += 8 + size + (size & 1);
                }
                if (start < 0 || ch < 1 || ch > 2 || (bitsPerSample != 16 && bitsPerSample != 8))
                    throw new IOException("Unsupported WAV layout (mono/stereo 8 or 16-bit PCM only)");
                channels = ch; rate = sr; bits = bitsPerSample; dataStart = start; dataLength = length;
                channel.position(dataStart);
            } catch (IOException | RuntimeException failure) {
                channel.close();
                throw failure instanceof IOException io ? io : new IOException(failure);
            }
        }

        @Override public int channels() { return channels; }
        @Override public int sampleRate() { return rate; }
        @Override public long totalFrames() { return dataLength / (channels * (bits / 8)); }
        @Override public Map<String, String> tags() { return Map.of(); }

        @Override
        public int read(ShortBuffer out) {
            try {
                int frameBytes = channels * (bits / 8);
                long remaining = dataStart + dataLength - channel.position();
                int wanted = (int) Math.min(remaining / frameBytes, out.remaining() / channels);
                if (wanted <= 0) return 0;
                int bytes = wanted * frameBytes;
                if (scratch.capacity() < bytes) scratch = ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN);
                scratch.clear().limit(bytes);
                while (scratch.hasRemaining() && channel.read(scratch) > 0) { }
                scratch.flip();
                int frames = scratch.remaining() / frameBytes;
                int start = out.position();
                for (int i = 0; i < frames * channels; i++) {
                    short sample = bits == 16 ? scratch.getShort() : (short) (((scratch.get() & 0xFF) - 128) << 8);
                    out.put(start + i, sample);
                }
                return frames;
            } catch (IOException exception) {
                return 0;
            }
        }

        @Override
        public void seek(long frame) {
            try {
                long offset = Math.max(0, Math.min(totalFrames(), frame)) * channels * (bits / 8);
                channel.position(dataStart + offset);
            } catch (IOException ignored) { }
        }

        @Override
        public void close() {
            try { channel.close(); } catch (IOException ignored) { }
        }
    }
}
