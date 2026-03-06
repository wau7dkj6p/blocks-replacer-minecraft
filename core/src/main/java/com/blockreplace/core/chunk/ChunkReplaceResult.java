package com.blockreplace.core.chunk;

import java.util.Map;
import java.util.Set;

public record ChunkReplaceResult(
    boolean modified,
    int sectionsModified,
    int paletteEntriesChanged,
    long blocksAffected,
    Map<Integer, Set<Integer>> nameChangedPaletteIndicesBySection) {
  public static ChunkReplaceResult none() {
    return new ChunkReplaceResult(false, 0, 0, 0, Map.of());
  }
}

