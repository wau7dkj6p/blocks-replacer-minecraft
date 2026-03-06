package com.blockreplace.core.chunk;

import com.blockreplace.core.nbt.NbtCompound;
import com.blockreplace.core.nbt.NbtList;
import com.blockreplace.core.nbt.NbtString;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Helpers for fixing {@code snowy=true} dirt-like blocks (grass, podzol, mycelium)
 * after snow layers have been removed.
 *
 * <p>This works at the palette level: for each palette entry corresponding to a
 * snowy dirt block, the {@code snowy} property is forced to {@code false}.
 *
 * <p>The caller is responsible for deciding when it is appropriate to run this
 * (e.g. only when there are tasks that remove snow blocks).
 */
final class SnowFixer {
  private static final String GRASS = "minecraft:grass_block";
  private static final String PODZOL = "minecraft:podzol";
  private static final String MYCELIUM = "minecraft:mycelium";

  private SnowFixer() {}

  static SectionReplaceResult fixSectionSnowyGround(NbtCompound section) {
    Objects.requireNonNull(section, "section");

    NbtCompound blockStates = section.getCompound("block_states").orElse(null);
    if (blockStates == null) return SectionReplaceResult.none();

    NbtList palette = blockStates.getList("palette").orElse(null);
    if (palette == null || palette.size() == 0) return SectionReplaceResult.none();

    int paletteSize = palette.size();
    Set<Integer> changed = new HashSet<>();

    for (int i = 0; i < paletteSize; i++) {
      if (!(palette.get(i) instanceof NbtCompound entry)) continue;

      String name =
          entry
              .getString("Name")
              .orElseThrow(() -> new IllegalStateException("Palette entry missing Name"));

      // Normalize simple names like "grass_block" to "minecraft:grass_block".
      String normalized = normalizeName(name);
      if (!isSnowyGroundCandidate(normalized)) continue;

      NbtCompound props = entry.getCompound("Properties").orElse(null);
      if (props == null || props.size() == 0) continue;

      var snowyTagOpt = props.get("snowy");
      if (snowyTagOpt.isEmpty()) continue;
      if (!(snowyTagOpt.get() instanceof NbtString snowyVal)) continue;
      if (!"true".equals(snowyVal.value())) continue;

      // Force snowy=false.
      props.put("snowy", new NbtString("false"));
      changed.add(i);
    }

    if (changed.isEmpty()) return SectionReplaceResult.none();

    long affectedBlocks =
        ChunkSectionReplacer.countBlocksWithPaletteIndices(blockStates, paletteSize, changed);

    return new SectionReplaceResult(true, changed.size(), affectedBlocks, Set.of());
  }

  private static boolean isSnowyGroundCandidate(String name) {
    return GRASS.equals(name) || PODZOL.equals(name) || MYCELIUM.equals(name);
  }

  private static String normalizeName(String name) {
    String n = Objects.requireNonNull(name, "name").trim();
    if (!n.contains(":")) {
      n = "minecraft:" + n;
    }
    return n;
  }
}

