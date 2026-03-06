package com.blockreplace.core.chunk;

import com.blockreplace.core.nbt.NbtCompound;
import com.blockreplace.core.nbt.NbtList;
import com.blockreplace.core.region.RegionFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class BlockEntityFixer {
  private BlockEntityFixer() {}

  /**
   * Removes {@code block_entities} entries whose block was replaced by name (i.e. block type changed).
   *
   * <p>Safe default: if the block's {@code Name} changed, we drop any BE at that coordinate.
   */
  public static int removeBlockEntitiesForNameChangedBlocks(
      NbtCompound chunkRoot,
      int chunkX,
      int chunkZ,
      Map<Integer, Set<Integer>> nameChangedPaletteIndicesBySectionIndex) {
    Objects.requireNonNull(chunkRoot, "chunkRoot");
    Objects.requireNonNull(nameChangedPaletteIndicesBySectionIndex, "nameChangedPaletteIndicesBySectionIndex");
    if (nameChangedPaletteIndicesBySectionIndex.isEmpty()) return 0;

    NbtList blockEntities = chunkRoot.getList("block_entities").orElse(null);
    if (blockEntities == null || blockEntities.size() == 0) return 0;

    NbtList sections = chunkRoot.getList("sections").orElse(null);
    if (sections == null || sections.size() == 0) return 0;

    // Map sectionY -> section compound
    Map<Integer, NbtCompound> sectionByY = new HashMap<>();
    Map<Integer, Set<Integer>> nameChangedBySectionY = new HashMap<>();
    for (int i = 0; i < sections.size(); i++) {
      if (!(sections.get(i) instanceof NbtCompound s)) continue;
      Integer sectionY = readSectionY(s).orElse(null);
      if (sectionY == null) continue;
      sectionByY.put(sectionY, s);
      Set<Integer> nameChanged = nameChangedPaletteIndicesBySectionIndex.get(i);
      if (nameChanged != null && !nameChanged.isEmpty()) {
        nameChangedBySectionY.put(sectionY, nameChanged);
      }
    }
    if (nameChangedBySectionY.isEmpty()) return 0;

    Map<Integer, int[]> decodedCache = new HashMap<>();

    int removed =
        blockEntities.removeIf(
            tag -> {
              if (!(tag instanceof NbtCompound be)) return false;
              Integer x = be.getInt("x").orElse(null);
              Integer y = be.getInt("y").orElse(null);
              Integer z = be.getInt("z").orElse(null);
              if (x == null || y == null || z == null) return false;

              int localX = x - chunkX * 16;
              int localZ = z - chunkZ * 16;
              if (localX < 0 || localX >= 16 || localZ < 0 || localZ >= 16) {
                return false;
              }

              int sectionY = Math.floorDiv(y, 16);
              Set<Integer> changedIndices = nameChangedBySectionY.get(sectionY);
              if (changedIndices == null || changedIndices.isEmpty()) return false;

              NbtCompound section = sectionByY.get(sectionY);
              if (section == null) return false;

              int[] decoded =
                  decodedCache.computeIfAbsent(
                      sectionY,
                      __ -> {
                        try {
                          return decodeSection4096(section);
                        } catch (RuntimeException e) {
                          return null;
                        }
                      });
              if (decoded == null) return false;

              int localY = y - sectionY * 16;
              if (localY < 0 || localY >= 16) return false;

              int pos = localY * 256 + localZ * 16 + localX;
              int paletteIndex = decoded[pos];
              return changedIndices.contains(paletteIndex);
            });

    return removed;
  }

  private static Optional<Integer> readSectionY(NbtCompound section) {
    return section.getInt("Y");
  }

  private static int[] decodeSection4096(NbtCompound section) {
    NbtCompound blockStates = section.getCompound("block_states").orElse(null);
    if (blockStates == null) return new int[4096];
    NbtList palette = blockStates.getList("palette").orElse(null);
    int paletteSize = palette == null ? 0 : palette.size();
    int bits = PaletteCodec.bitsPerEntry(paletteSize);
    long[] data = blockStates.getLongArray("data").orElse(null);
    if (bits == 0) return new int[4096];
    if (data == null) return new int[4096];
    return PaletteCodec.decode4096(bits, data);
  }
}

