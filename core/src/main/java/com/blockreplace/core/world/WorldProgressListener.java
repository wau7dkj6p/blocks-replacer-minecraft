package com.blockreplace.core.world;

import java.nio.file.Path;

public interface WorldProgressListener {
  default void onWorldStart(int totalRegionFiles) {}

  /**
   * Called after all regions of the world have been processed.
   *
   * @param totalRegionFiles Total number of region files that were scheduled for processing.
   * @param totalChunksVisited Total number of chunks that were visited (present and decoded).
   * @param totalChunksModified Total number of chunks that contained at least one change (real or
   *     dry-run).
   * @param totalBlocksAffected Total number of blocks that were affected (real or dry-run).
   */
  default void onWorldDone(
      int totalRegionFiles,
      int totalChunksVisited,
      int totalChunksModified,
      long totalBlocksAffected) {}

  default void onInfo(String message) {}

  default void onWarn(String message) {}

  default void onError(String message, Throwable t) {}

  default void onRegionStart(WorldDimension dim, Path regionFile, int presentChunks) {}

  default void onChunkDone(
      WorldDimension dim,
      Path regionFile,
      int chunkX,
      int chunkZ,
      long blocksAffected,
      int paletteChanges,
      int blockEntitiesRemoved) {}

  default void onRegionDone(
      WorldDimension dim,
      Path regionFile,
      boolean modified,
      int chunksModified,
      long blocksAffected,
      int paletteChanges,
      int blockEntitiesRemoved) {}
}

