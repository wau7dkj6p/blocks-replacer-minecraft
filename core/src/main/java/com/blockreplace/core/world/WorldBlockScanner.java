package com.blockreplace.core.world;

import com.blockreplace.core.nbt.NbtCompound;
import com.blockreplace.core.region.ChunkCodecs;
import com.blockreplace.core.region.RegionBackend;
import com.blockreplace.core.region.RegionChunkData;
import com.blockreplace.core.region.RegionCoords;
import com.blockreplace.core.region.RegionFile;
import com.blockreplace.core.region.RegionFileSelector;
import com.blockreplace.core.region.RegionFormat;
import com.blockreplace.core.task.BlockStateSpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Read-only world scanner that counts how many times a given block name appears in all existing
 * chunks of selected dimensions. It never writes anything to disk.
 */
public final class WorldBlockScanner {
  private final Path worldRoot;
  private final EnumSet<WorldDimension> dimensions;
  private final String targetBlockName;

  public WorldBlockScanner(Path worldRoot, EnumSet<WorldDimension> dimensions, String targetBlockName) {
    this.worldRoot = Objects.requireNonNull(worldRoot, "worldRoot");
    this.dimensions =
        dimensions == null || dimensions.isEmpty()
            ? EnumSet.of(WorldDimension.OVERWORLD)
            : dimensions;
    this.targetBlockName = BlockStateSpec.normalizeName(Objects.requireNonNull(targetBlockName, "targetBlockName"));
  }

  public Result run() throws IOException {
    long totalBlocks = 0L;
    int totalChunksVisited = 0;
    int totalRegions = 0;

    for (WorldDimension dim : dimensions) {
      Path regionDir = dim.regionDir(worldRoot);
      if (!Files.isDirectory(regionDir)) {
        continue;
      }
      try (var files = Files.list(regionDir)) {
        List<Path> regionFiles = new ArrayList<>();
        files
            .filter(RegionFileSelector::isRegionFile)
            .sorted()
            .forEach(regionFiles::add);

        totalRegions += regionFiles.size();

        for (Path regionFile : regionFiles) {
          RegionCoords coords = RegionCoords.parse(regionFile);
          RegionFormat format = RegionFormat.fromPath(regionFile);
          try (RegionBackend backend = RegionFileSelector.open(regionFile)) {
            for (int localZ = 0; localZ < RegionFile.CHUNKS_PER_REGION_SIDE; localZ++) {
              for (int localX = 0; localX < RegionFile.CHUNKS_PER_REGION_SIDE; localX++) {
                if (!backend.hasChunk(localX, localZ)) continue;
                totalChunksVisited++;

                RegionChunkData data = backend.readChunk(localX, localZ).orElse(null);
                if (data == null) continue;

                NbtCompound root = ChunkCodecs.decode(data, format);
                totalBlocks += countBlockInChunk(root);
              }
            }
          }
        }
      }
    }

    return new Result(totalRegions, totalChunksVisited, totalBlocks);
  }

  private long countBlockInChunk(NbtCompound chunkRoot) {
    var sectionsOpt = chunkRoot.getList("sections");
    if (sectionsOpt.isEmpty()) return 0L;
    var sections = sectionsOpt.get();
    long count = 0L;
    for (int i = 0; i < sections.size(); i++) {
      if (!(sections.get(i) instanceof NbtCompound section)) continue;
      NbtCompound blockStates = section.getCompound("block_states").orElse(null);
      if (blockStates == null) continue;

      var paletteOpt = blockStates.getList("palette");
      if (paletteOpt.isEmpty()) continue;
      var palette = paletteOpt.get();
      int paletteSize = palette.size();
      if (paletteSize <= 0) continue;

      // Build set of palette indices matching target block name.
      java.util.HashSet<Integer> indices = new java.util.HashSet<>();
      for (int pi = 0; pi < paletteSize; pi++) {
        if (!(palette.get(pi) instanceof NbtCompound entry)) continue;
        String name =
            entry
                .getString("Name")
                .orElseThrow(() -> new IllegalStateException("Palette entry missing Name"));
        String normalized = BlockStateSpec.normalizeName(name);
        if (normalized.equals(targetBlockName)) {
          indices.add(pi);
        }
      }
      if (indices.isEmpty()) continue;

      long[] data = blockStates.getLongArray("data").orElse(null);
      int bits = com.blockreplace.core.chunk.PaletteCodec.bitsPerEntry(paletteSize);
      if (bits == 0) {
        if (indices.contains(0)) count += 4096L;
        continue;
      }
      if (data == null) continue;
      int[] decoded = com.blockreplace.core.chunk.PaletteCodec.decode4096(bits, data);
      for (int v : decoded) {
        if (indices.contains(v)) count++;
      }
    }
    return count;
  }

  public record Result(int totalRegionFiles, int totalChunksVisited, long totalBlocksFound) {}
}

