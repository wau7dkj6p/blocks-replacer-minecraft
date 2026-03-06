package com.blockreplace.core.chunk;

import com.blockreplace.core.nbt.NbtCompound;
import com.blockreplace.core.nbt.NbtList;
import com.blockreplace.core.task.BlockStateSpec;
import com.blockreplace.core.task.ReplaceTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ChunkReplacer {
  private ChunkReplacer() {}

  public static ChunkReplaceResult applyTasks(NbtCompound chunkRoot, List<ReplaceTask> tasks) {
    return applyTasks(chunkRoot, tasks, false);
  }

  public static ChunkReplaceResult applyTasks(
      NbtCompound chunkRoot, List<ReplaceTask> tasks, boolean fixSnowyGround) {
    Objects.requireNonNull(chunkRoot, "chunkRoot");
    Objects.requireNonNull(tasks, "tasks");

    NbtList sections = chunkRoot.getList("sections").orElse(null);
    if (sections == null) return ChunkReplaceResult.none();

    boolean modified = false;
    int sectionsModified = 0;
    int paletteEntriesChanged = 0;
    long blocksAffected = 0;
    Map<Integer, Set<Integer>> nameChangedIndicesBySection = new HashMap<>();

    boolean applySnowFix = fixSnowyGround && hasSnowRemovalTask(tasks);

    for (int i = 0; i < sections.size(); i++) {
      if (!(sections.get(i) instanceof NbtCompound section)) continue;

      boolean sectionModified = false;
      Set<Integer> nameChangedPaletteIndices = null;
      for (ReplaceTask t : tasks) {
        if (!t.enabled()) continue;
        SectionReplaceResult r = ChunkSectionReplacer.applyTaskToSection(section, t);
        if (!r.modified()) continue;
        sectionModified = true;
        paletteEntriesChanged += r.paletteEntriesChanged();
        blocksAffected += r.blocksAffected();
        if (!r.paletteIndicesWithNameChange().isEmpty()) {
          if (nameChangedPaletteIndices == null) nameChangedPaletteIndices = new HashSet<>();
          nameChangedPaletteIndices.addAll(r.paletteIndicesWithNameChange());
        }
      }

      if (applySnowFix) {
        SectionReplaceResult snowResult = SnowFixer.fixSectionSnowyGround(section);
        if (snowResult.modified()) {
          sectionModified = true;
          paletteEntriesChanged += snowResult.paletteEntriesChanged();
          blocksAffected += snowResult.blocksAffected();
        }
      }

      if (sectionModified) {
        modified = true;
        sectionsModified++;
        if (nameChangedPaletteIndices != null && !nameChangedPaletteIndices.isEmpty()) {
          nameChangedIndicesBySection.put(i, Set.copyOf(nameChangedPaletteIndices));
        }
      }
    }

    if (!modified) return ChunkReplaceResult.none();
    return new ChunkReplaceResult(
        modified, sectionsModified, paletteEntriesChanged, blocksAffected, Map.copyOf(nameChangedIndicesBySection));
  }

  private static boolean hasSnowRemovalTask(List<ReplaceTask> tasks) {
    for (ReplaceTask t : tasks) {
      if (!t.enabled()) continue;
      String fromName = t.from() != null ? t.from().name() : null;
      if (fromName == null) continue;
      String n = BlockStateSpec.normalizeName(fromName);
      if ("minecraft:snow".equals(n)
          || "minecraft:snow_block".equals(n)
          || "minecraft:powder_snow".equals(n)) {
        return true;
      }
    }
    return false;
  }

  public static List<ReplaceTask> enabledOnly(List<ReplaceTask> tasks) {
    ArrayList<ReplaceTask> out = new ArrayList<>();
    for (ReplaceTask t : tasks) if (t.enabled()) out.add(t);
    return out;
  }
}

