package com.blockreplace.core.region;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blockreplace.core.nbt.NbtByteArray;
import com.blockreplace.core.nbt.NbtCompound;
import com.blockreplace.core.nbt.NbtList;
import com.blockreplace.core.nbt.NbtType;
import com.blockreplace.core.nbt.NbtIo;
import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class ChunkNbtCodecPreAnvilTest {

  @Test
  void decodeEncodeRoundtrip_preservesBlocksAndData() throws IOException {
    int size = 16 * 16 * 128;
    byte[] blocks = new byte[size];
    byte[] data = new byte[size / 2];

    // Simple pattern: lower half stone (1), upper half dirt (3), meta = y % 16.
    for (int y = 0; y < 128; y++) {
      for (int z = 0; z < 16; z++) {
        for (int x = 0; x < 16; x++) {
          int idx = (y * 16 + z) * 16 + x;
          blocks[idx] = (byte) ((y < 64) ? 1 : 3);
          setNibble(data, idx, y & 0xF);
        }
      }
    }

    NbtCompound level = new NbtCompound();
    level.put("Blocks", new NbtByteArray(blocks));
    level.put("Data", new NbtByteArray(data));

    NbtCompound root = new NbtCompound();
    root.put("Level", level);

    byte[] uncompressed = NbtIo.writeRootCompound(root);
    RegionCompression compression = RegionCompression.GZIP;
    byte[] compressed = compression.compress(uncompressed);
    RegionChunkData in = new RegionChunkData(compression, compressed, 123456);

    NbtCompound canonical = ChunkNbtCodecPreAnvil.decodeToCanonical(in);
    assertNotNull(canonical.getList("sections").orElse(NbtList.of(NbtType.COMPOUND)));

    RegionChunkData out = ChunkNbtCodecPreAnvil.encodeFromCanonical(canonical, in);
    byte[] uncompressedOut = compression.decompress(out.compressedPayload());
    NbtCompound rootOut = NbtIo.readRootCompound(uncompressedOut);
    NbtCompound levelOut =
        rootOut.getCompound("Level").orElseThrow(() -> new IllegalStateException("Missing Level"));

    byte[] blocksOut =
        levelOut
            .get("Blocks")
            .filter(t -> t instanceof NbtByteArray)
            .map(t -> ((NbtByteArray) t).value())
            .orElseThrow(() -> new IllegalStateException("Missing Blocks after roundtrip"));
    byte[] dataOut =
        levelOut
            .get("Data")
            .filter(t -> t instanceof NbtByteArray)
            .map(t -> ((NbtByteArray) t).value())
            .orElseThrow(() -> new IllegalStateException("Missing Data after roundtrip"));

    assertArrayEquals(blocks, blocksOut);
    assertArrayEquals(data, dataOut);
  }

  private static void setNibble(byte[] arr, int index, int value) {
    int i = index >> 1;
    int b = arr[i] & 0xFF;
    if ((index & 1) == 0) {
      b = (b & 0xF0) | (value & 0x0F);
    } else {
      b = (b & 0x0F) | ((value & 0x0F) << 4);
    }
    arr[i] = (byte) b;
  }
}

