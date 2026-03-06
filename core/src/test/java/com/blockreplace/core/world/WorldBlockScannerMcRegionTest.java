package com.blockreplace.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.blockreplace.core.nbt.NbtByteArray;
import com.blockreplace.core.nbt.NbtCompound;
import com.blockreplace.core.nbt.NbtIo;
import com.blockreplace.core.region.RegionChunkData;
import com.blockreplace.core.region.RegionCompression;
import com.blockreplace.core.region.RegionFile;
import com.blockreplace.core.region.RegionFileSelector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

public class WorldBlockScannerMcRegionTest {

  @Test
  void scanRunsOnEmptyMcrWorld() throws IOException {
    Path tmpDir = Files.createTempDirectory("mcr-world");
    try {
      Path regionDir = tmpDir.resolve("region");
      Files.createDirectories(regionDir);

      // Create minimal .mcr region with a single stone-only chunk at (0,0).
      Path regionFile = regionDir.resolve("r.0.0.mcr");
      createSingleChunkRegion(regionFile);

      WorldBlockScanner scanner =
          new WorldBlockScanner(tmpDir, EnumSet.of(WorldDimension.OVERWORLD), "minecraft:stone");
      WorldBlockScanner.Result result = scanner.run();

      // We created exactly one region; scanner should at least see it without errors.
      assertEquals(1, result.totalRegionFiles());
    } finally {
      // Best-effort cleanup.
      Files.walk(tmpDir)
          .sorted((a, b) -> b.compareTo(a))
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
              });
    }
  }

  private static void createSingleChunkRegion(Path path) throws IOException {
    // Build a simple Pre-Anvil chunk: all Blocks=1 (stone), Data=0.
    int size = 16 * 16 * 128;
    byte[] blocks = new byte[size];
    byte[] data = new byte[size / 2];
    for (int i = 0; i < size; i++) {
      blocks[i] = 1;
    }

    NbtCompound level = new NbtCompound();
    level.put("Blocks", new NbtByteArray(blocks));
    level.put("Data", new NbtByteArray(data));

    NbtCompound root = new NbtCompound();
    root.put("Level", level);

    byte[] uncompressed = NbtIo.writeRootCompound(root);
    RegionCompression compression = RegionCompression.GZIP;
    byte[] compressed = compression.compress(uncompressed);
    RegionChunkData chunkData = new RegionChunkData(compression, compressed, RegionFile.nowUnixSeconds());

    // Create temporary source region with a single present chunk at (0,0).
    Path src = path.resolveSibling(path.getFileName().toString() + ".src");
    Files.write(src, new byte[RegionFile.HEADER_BYTES]);

    RegionFile.ChunkTransformer transformer =
        (x, z, in) -> {
          if (x == 0 && z == 0) {
            return chunkData;
          }
          return in;
        };

    RegionFile.rebuild(src, path, transformer);
    Files.deleteIfExists(src);
  }
}

