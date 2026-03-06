package com.blockreplace.core.region;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class RegionFile implements Closeable {
  public static final int SECTOR_BYTES = 4096;
  public static final int HEADER_BYTES = SECTOR_BYTES * 2;
  public static final int CHUNKS_PER_REGION_SIDE = 32;
  public static final int CHUNK_COUNT = CHUNKS_PER_REGION_SIDE * CHUNKS_PER_REGION_SIDE;

  private final Path path;
  private final FileChannel channel;

  private final int[] offsetsSectors = new int[CHUNK_COUNT];
  private final int[] sectorCounts = new int[CHUNK_COUNT];
  private final int[] timestamps = new int[CHUNK_COUNT];

  private RegionFile(Path path, FileChannel channel) {
    this.path = path;
    this.channel = channel;
  }

  public static RegionFile open(Path path) throws IOException {
    Objects.requireNonNull(path, "path");
    FileChannel ch = FileChannel.open(path, StandardOpenOption.READ);
    RegionFile rf = new RegionFile(path, ch);
    rf.readHeader();
    return rf;
  }

  public Path path() {
    return path;
  }

  private void readHeader() throws IOException {
    ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES);
    long pos = 0;
    while (header.hasRemaining()) {
      int r = channel.read(header, pos);
      if (r < 0) throw new EOFException("Region header is incomplete: " + path);
      pos += r;
    }
    header.flip();
    for (int i = 0; i < CHUNK_COUNT; i++) {
      int v = header.getInt();
      offsetsSectors[i] = (v >>> 8) & 0x00FF_FFFF;
      sectorCounts[i] = (v) & 0xFF;
    }
    for (int i = 0; i < CHUNK_COUNT; i++) {
      timestamps[i] = header.getInt();
    }
  }

  public boolean hasChunk(int localX, int localZ) {
    int idx = index(localX, localZ);
    return offsetsSectors[idx] != 0 && sectorCounts[idx] != 0;
  }

  public Optional<RegionChunkData> readChunk(int localX, int localZ) throws IOException {
    int idx = index(localX, localZ);
    int offset = offsetsSectors[idx];
    int sectors = sectorCounts[idx];
    if (offset == 0 || sectors == 0) return Optional.empty();

    long pos = (long) offset * SECTOR_BYTES;
    ByteBuffer header = ByteBuffer.allocate(5);
    readFully(pos, header);
    header.flip();
    int length = header.getInt();
    int compressionId = header.get() & 0xFF;
    if (length <= 0) {
      throw new IOException("Invalid chunk length=" + length + " at " + path + " @" + pos);
    }
    if (length > (sectors * SECTOR_BYTES) - 4) {
      throw new IOException(
          "Chunk length="
              + length
              + " exceeds allocated sectors="
              + sectors
              + " at "
              + path
              + " local("
              + localX
              + ","
              + localZ
              + ")");
    }

    int compressedLen = length - 1;
    ByteBuffer payload = ByteBuffer.allocate(compressedLen);
    readFully(pos + 5, payload);
    payload.flip();
    byte[] compressedPayload = new byte[compressedLen];
    payload.get(compressedPayload);
    return Optional.of(
        new RegionChunkData(RegionCompression.fromId(compressionId), compressedPayload, timestamps[idx]));
  }

  public static int index(int localX, int localZ) {
    if (localX < 0 || localX >= CHUNKS_PER_REGION_SIDE || localZ < 0 || localZ >= CHUNKS_PER_REGION_SIDE) {
      throw new IllegalArgumentException("local chunk coords out of range: " + localX + "," + localZ);
    }
    return localX + localZ * CHUNKS_PER_REGION_SIDE;
  }

  private void readFully(long pos, ByteBuffer dst) throws IOException {
    while (dst.hasRemaining()) {
      int r = channel.read(dst, pos);
      if (r < 0) throw new EOFException("Unexpected EOF while reading " + path);
      pos += r;
    }
  }

  @Override
  public void close() throws IOException {
    channel.close();
  }

  @FunctionalInterface
  public interface ChunkTransformer {
    RegionChunkData transform(int localX, int localZ, RegionChunkData in) throws IOException;
  }

  public static void rebuild(Path src, Path dest, ChunkTransformer transformer) throws IOException {
    Objects.requireNonNull(src, "src");
    Objects.requireNonNull(dest, "dest");
    Objects.requireNonNull(transformer, "transformer");

    Files.createDirectories(dest.getParent());
    Path tmp = dest.resolveSibling(dest.getFileName().toString() + ".tmp");

    try (RegionFile rf = RegionFile.open(src);
        FileChannel out = FileChannel.open(
            tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {

      ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES);
      out.write(header, 0); // reserve

      int[] newOffsets = new int[CHUNK_COUNT];
      int[] newSizes = new int[CHUNK_COUNT];
      int[] newTimestamps = new int[CHUNK_COUNT];

      int nextSector = 2; // sector 0 and 1 are header

      for (int localZ = 0; localZ < CHUNKS_PER_REGION_SIDE; localZ++) {
        for (int localX = 0; localX < CHUNKS_PER_REGION_SIDE; localX++) {
          int idx = index(localX, localZ);
          if (!rf.hasChunk(localX, localZ)) {
            newOffsets[idx] = 0;
            newSizes[idx] = 0;
            newTimestamps[idx] = rf.timestamps[idx];
            continue;
          }

          RegionChunkData original = rf.readChunk(localX, localZ).orElseThrow();
          RegionChunkData transformed = transformer.transform(localX, localZ, original);
          if (transformed == null) transformed = original;

          byte[] payload = transformed.compressedPayload();
          int chunkLenField = 1 + payload.length;

          int totalBytes = 4 + chunkLenField;
          int sectors = (totalBytes + SECTOR_BYTES - 1) / SECTOR_BYTES;

          newOffsets[idx] = nextSector;
          newSizes[idx] = sectors;
          newTimestamps[idx] =
              transformed.timestampSeconds() != 0
                  ? transformed.timestampSeconds()
                  : original.timestampSeconds();

          long pos = (long) nextSector * SECTOR_BYTES;
          ByteBuffer chunkHeader = ByteBuffer.allocate(5);
          chunkHeader.putInt(chunkLenField);
          chunkHeader.put((byte) transformed.compression().id());
          chunkHeader.flip();

          out.write(chunkHeader, pos);
          out.write(ByteBuffer.wrap(payload), pos + 5);

          // pad to sector boundary with zeros
          long written = totalBytes;
          long pad = (long) sectors * SECTOR_BYTES - written;
          if (pad > 0) {
            out.write(ByteBuffer.allocate((int) Math.min(pad, 8192)), pos + written);
            long remaining = pad - Math.min(pad, 8192);
            long padPos = pos + written + Math.min(pad, 8192);
            while (remaining > 0) {
              int n = (int) Math.min(remaining, 8192);
              out.write(ByteBuffer.allocate(n), padPos);
              padPos += n;
              remaining -= n;
            }
          }

          nextSector += sectors;
        }
      }

      // write header
      ByteBuffer hdr = ByteBuffer.allocate(HEADER_BYTES);
      for (int i = 0; i < CHUNK_COUNT; i++) {
        int v = ((newOffsets[i] & 0x00FF_FFFF) << 8) | (newSizes[i] & 0xFF);
        hdr.putInt(v);
      }
      for (int i = 0; i < CHUNK_COUNT; i++) {
        hdr.putInt(newTimestamps[i]);
      }
      hdr.flip();
      out.write(hdr, 0);
    }

    atomicMove(tmp, dest);
  }

  public static void atomicMove(Path tmp, Path dest) throws IOException {
    try {
      Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException ignored) {
      Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public static int nowUnixSeconds() {
    return (int) Instant.now().getEpochSecond();
  }
}

