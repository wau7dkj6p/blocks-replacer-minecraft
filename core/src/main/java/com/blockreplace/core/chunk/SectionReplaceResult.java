package com.blockreplace.core.chunk;

import java.util.Set;

public record SectionReplaceResult(
    boolean modified,
    int paletteEntriesChanged,
    long blocksAffected,
    Set<Integer> paletteIndicesWithNameChange) {
  public static SectionReplaceResult none() {
    return new SectionReplaceResult(false, 0, 0, Set.of());
  }
}

