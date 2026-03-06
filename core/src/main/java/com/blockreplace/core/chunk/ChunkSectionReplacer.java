package com.blockreplace.core.chunk;

import com.blockreplace.core.nbt.NbtCompound;
import com.blockreplace.core.nbt.NbtList;
import com.blockreplace.core.nbt.NbtString;
import com.blockreplace.core.task.BlockStateSpec;
import com.blockreplace.core.task.ReplaceTask;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ChunkSectionReplacer {
  private ChunkSectionReplacer() {}

  public static SectionReplaceResult applyTaskToSection(NbtCompound section, ReplaceTask task) {
    Objects.requireNonNull(section, "section");
    Objects.requireNonNull(task, "task");

    NbtCompound blockStates = section.getCompound("block_states").orElse(null);
    if (blockStates == null) return SectionReplaceResult.none();

    NbtList palette = blockStates.getList("palette").orElse(null);
    if (palette == null) return SectionReplaceResult.none();

    int paletteSize = palette.size();
    if (paletteSize <= 0) return SectionReplaceResult.none();

    // Determine which palette indices match, and update palette entries in-place.
    Set<Integer> changedIndices = new HashSet<>();
    Set<Integer> nameChangedIndices = new HashSet<>();

    for (int i = 0; i < paletteSize; i++) {
      if (!(palette.get(i) instanceof NbtCompound entry)) continue;
      BlockState current = BlockState.fromPaletteEntry(entry);
      if (!matches(current, task.from(), task.matchByNameOnly())) continue;

      BlockState target = buildTarget(current, task.to());
      if (current.equals(target)) continue;

      entry.put("Name", new NbtString(target.name));
      if (target.properties.isEmpty()) {
        entry.remove("Properties");
      } else {
        NbtCompound props = new NbtCompound();
        for (Map.Entry<String, String> e : target.properties.entrySet()) {
          props.put(e.getKey(), new NbtString(e.getValue()));
        }
        entry.put("Properties", props);
      }

      changedIndices.add(i);
      if (!current.name.equals(target.name)) nameChangedIndices.add(i);
    }

    if (changedIndices.isEmpty()) return SectionReplaceResult.none();

    long affectedBlocks = countBlocksWithPaletteIndices(blockStates, paletteSize, changedIndices);
    return new SectionReplaceResult(true, changedIndices.size(), affectedBlocks, Set.copyOf(nameChangedIndices));
  }

  private static boolean matches(BlockState current, BlockStateSpec from, boolean matchByNameOnly) {
    if (!current.name.equals(from.name())) return false;
    if (matchByNameOnly) {
      // Полное совпадение только по имени блока, состояния игнорируются.
      return true;
    }
    for (Map.Entry<String, String> e : from.properties().entrySet()) {
      String prop = e.getKey();
      String wanted = e.getValue();
      String have = current.properties.get(prop);
      if (have == null) return false;
      if (!BlockStateSpec.ALL.equalsIgnoreCase(wanted) && !have.equals(wanted)) return false;
    }
    return true;
  }

  private static BlockState buildTarget(BlockState source, BlockStateSpec to) {
    HashMap<String, String> props = new HashMap<>();
    for (Map.Entry<String, String> e : to.properties().entrySet()) {
      String key = e.getKey();
      String v = e.getValue();
      if (BlockStateSpec.ALL.equalsIgnoreCase(v)) {
        String src = source.properties.get(key);
        if (src != null) props.put(key, src);
      } else {
        props.put(key, v);
      }
    }
    return new BlockState(BlockStateSpec.normalizeName(to.name()), props);
  }

  static long countBlocksWithPaletteIndices(
      NbtCompound blockStates, int paletteSize, Set<Integer> indices) {
    long[] data = blockStates.getLongArray("data").orElse(null);
    int bits = PaletteCodec.bitsPerEntry(paletteSize);
    if (bits == 0) {
      return indices.contains(0) ? 4096L : 0L;
    }
    if (data == null) return 0L;
    int[] decoded = PaletteCodec.decode4096(bits, data);
    long count = 0;
    for (int v : decoded) {
      if (indices.contains(v)) count++;
    }
    return count;
  }

  private static final class BlockState {
    final String name;
    final Map<String, String> properties;

    BlockState(String name, Map<String, String> properties) {
      this.name = BlockStateSpec.normalizeName(name);
      this.properties = Map.copyOf(properties == null ? Map.of() : properties);
    }

    static BlockState fromPaletteEntry(NbtCompound entry) {
      String name =
          entry.getString("Name").orElseThrow(() -> new IllegalStateException("Palette entry missing Name"));

      Map<String, String> props = Map.of();
      NbtCompound p = entry.getCompound("Properties").orElse(null);
      if (p != null && p.size() > 0) {
        HashMap<String, String> m = new HashMap<>();
        for (var e : p.values().entrySet()) {
          if (e.getValue() instanceof NbtString s) {
            m.put(e.getKey(), s.value());
          } else {
            m.put(e.getKey(), e.getValue().toString());
          }
        }
        props = m;
      }
      return new BlockState(name, props);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof BlockState other)) return false;
      return name.equals(other.name) && properties.equals(other.properties);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, properties);
    }
  }
}

