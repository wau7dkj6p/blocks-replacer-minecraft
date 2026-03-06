package com.blockreplace.core.region;

import com.blockreplace.core.nbt.NbtCompound;
import com.blockreplace.core.nbt.NbtIo;
import java.io.IOException;

public final class ChunkNbtCodec {
  private ChunkNbtCodec() {}

  public static NbtCompound decode(RegionChunkData chunk) throws IOException {
    byte[] uncompressed = chunk.compression().decompress(chunk.compressedPayload());
    return NbtIo.readRootCompound(uncompressed);
  }

  public static RegionChunkData encode(
      NbtCompound root, RegionCompression compression, int timestampSeconds) throws IOException {
    byte[] uncompressed = NbtIo.writeRootCompound(root);
    byte[] compressed = compression.compress(uncompressed);
    return new RegionChunkData(compression, compressed, timestampSeconds);
  }
}

