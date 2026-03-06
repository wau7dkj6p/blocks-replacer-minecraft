package com.blockreplace.core.region;

import com.blockreplace.core.nbt.NbtCompound;
import java.io.IOException;

/**
 * Dispatcher for chunk NBT codecs based on region format.
 *
 * <p>For ANVIL regions this delegates to {@link ChunkNbtCodec} and keeps the existing behaviour of
 * re-encoding chunks with the original compression and a fresh timestamp.
 *
 * <p>For MCREGION regions this delegates to {@link ChunkNbtCodecPreAnvil}, which is responsible
 * for translating between legacy Pre-Anvil chunk layout (Blocks/Data/Add) and the canonical
 * Anvil-like section + palette representation used by the rest of the pipeline.
 */
public final class ChunkCodecs {
  private ChunkCodecs() {}

  public static NbtCompound decode(RegionChunkData chunk, RegionFormat regionFormat)
      throws IOException {
    return switch (regionFormat) {
      case ANVIL -> ChunkNbtCodec.decode(chunk);
      case MCREGION -> ChunkNbtCodecPreAnvil.decodeToCanonical(chunk);
    };
  }

  public static RegionChunkData encode(
      NbtCompound canonical, RegionChunkData original, RegionFormat regionFormat)
      throws IOException {
    return switch (regionFormat) {
      case ANVIL -> ChunkNbtCodec.encode(
          canonical, original.compression(), RegionFile.nowUnixSeconds());
      case MCREGION -> ChunkNbtCodecPreAnvil.encodeFromCanonical(canonical, original);
    };
  }
}

