package com.blockreplace.core.world;

import com.blockreplace.core.blockdb.BlockDatabase;
import com.blockreplace.core.chunk.BlockEntityFixer;
import com.blockreplace.core.chunk.ChunkReplaceResult;
import com.blockreplace.core.chunk.ChunkReplacer;
import com.blockreplace.core.chunk.LightFixer;
import com.blockreplace.core.nbt.NbtCompound;
import com.blockreplace.core.region.RegionBackend;
import com.blockreplace.core.region.RegionChunkData;
import com.blockreplace.core.region.RegionCoords;
import com.blockreplace.core.region.ChunkCodecs;
import com.blockreplace.core.region.RegionFile;
import com.blockreplace.core.region.RegionFileSelector;
import com.blockreplace.core.region.RegionFormat;
import com.blockreplace.core.task.ReplaceTask;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public final class WorldProcessor {
  private final WorldProcessorOptions options;
  private final List<ReplaceTask> tasks;
  private final BlockDatabase blockDb;
  private final WorldProgressListener listener;
  private volatile boolean cancelled;
  private volatile boolean completedNormally;

  public WorldProcessor(
      WorldProcessorOptions options,
      List<ReplaceTask> tasks,
      BlockDatabase blockDb,
      WorldProgressListener listener) {
    this.options = Objects.requireNonNull(options, "options");
    this.tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks"));
    this.blockDb = Objects.requireNonNull(blockDb, "blockDb");
    this.listener = listener == null ? new WorldProgressListener() {} : listener;
  }

  public void cancel() {
    this.cancelled = true;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public boolean isCompletedNormally() {
    return completedNormally;
  }

  public void run() throws IOException {
    cancelled = false;
    completedNormally = false;
    boolean lightOnly = tasks.isEmpty() && options.fixLight();

    if (!lightOnly) {
      // Validate tasks against block database.
      for (ReplaceTask t : tasks) {
        if (!t.enabled()) continue;
        if (options.allowUnknownBlocks()) {
          try {
            blockDb.validate(t.from(), false);
          } catch (IllegalArgumentException ex) {
            listener.onWarn("FROM spec warning: " + ex.getMessage());
          }
          try {
            blockDb.validate(t.to(), true);
          } catch (IllegalArgumentException ex) {
            listener.onWarn("TO spec warning: " + ex.getMessage());
          }
        } else {
          blockDb.validate(t.from(), false);
          blockDb.validate(t.to(), true);
        }
      }
    }

    EnumSet<WorldDimension> dims =
        options.dimensions() == null || options.dimensions().isEmpty()
            ? EnumSet.of(WorldDimension.OVERWORLD)
            : options.dimensions();

    Path stateFile = options.worldRoot().resolve(".block-replace-state.json");
    WorldProcessState state = null;

    if (!lightOnly && options.resumeFromState()) {
      try {
        WorldProcessState loaded = WorldProcessState.load(stateFile);
        if (loaded != null
            && loaded.isCompatible(
                options.worldRoot(), tasks, options.allowUnknownBlocks(), options.fixSnowyGround())) {
          state = loaded;
        }
      } catch (IOException ex) {
        listener.onWarn("Failed to load state file: " + ex.getMessage());
      }
    }

    if (!lightOnly && state == null && options.saveState()) {
      state =
          WorldProcessState.create(
              options.worldRoot(), tasks, options.allowUnknownBlocks(), options.fixSnowyGround());
    }

    List<DimRegionBatch> batches = new ArrayList<>();
    int totalRegions = 0;
    long totalBlocksAffected = 0L;
    int totalChunksModified = 0;
    int totalChunksVisited = 0;

    for (WorldDimension dim : dims) {
      Path regionDir = dim.regionDir(options.worldRoot());
      if (!Files.isDirectory(regionDir)) {
        listener.onWarn("Region dir missing for " + dim + ": " + regionDir);
        continue;
      }
      try (var files = Files.list(regionDir)) {
        List<Path> regionFiles = new ArrayList<>();
        files
            .filter(RegionFileSelector::isRegionFile)
            .sorted()
            .forEach(regionFiles::add);

        int startIndex = 0;
        if (state != null && state.lastRegionIndexByDimension != null) {
          Integer idx = state.lastRegionIndexByDimension.get(dim.name());
          if (idx != null && idx >= 0 && idx + 1 < regionFiles.size()) {
            startIndex = idx + 1;
          }
        }

        if (startIndex < regionFiles.size()) {
          batches.add(new DimRegionBatch(dim, regionFiles, startIndex));
          totalRegions += regionFiles.size() - startIndex;
        }
      }
    }

    listener.onInfo("World root: " + options.worldRoot());
    listener.onInfo("Dimensions: " + dims);
    listener.onInfo("Total regions to process: " + totalRegions);

    listener.onWorldStart(totalRegions);

    boolean finishedAllRegions = true;

    outer:
    for (DimRegionBatch batch : batches) {
      if (cancelled) {
        finishedAllRegions = false;
        break;
      }
      WorldDimension dim = batch.dim();
      List<Path> regionFiles = batch.regionFiles();
      int startIndex = batch.startIndex();

      listener.onInfo("Start dimension: " + dim);

      for (int i = startIndex; i < regionFiles.size(); i++) {
        if (cancelled) {
          finishedAllRegions = false;
          break outer;
        }
        Path regionFile = regionFiles.get(i);
        try {
          RegionStats stats = processRegion(dim, regionFile);
          totalBlocksAffected += stats.blocksAffected();
          totalChunksModified += stats.chunksModified();
          totalChunksVisited += stats.chunksVisited();
        } catch (IOException | RuntimeException ex) {
          listener.onError(
              "Failed to process region " + regionFile + ": " + ex.getMessage(), ex);
        } finally {
          if (state != null && options.saveState()) {
            state.lastRegionIndexByDimension.put(dim.name(), i);
            try {
              WorldProcessState.save(stateFile, state);
            } catch (IOException ex) {
              listener.onWarn("Failed to save state file: " + ex.getMessage());
            }
          }
        }
      }
    }

    completedNormally = finishedAllRegions;

    if (options.saveState() && finishedAllRegions) {
      try {
        WorldProcessState.deleteIfExists(stateFile);
      } catch (IOException ex) {
        listener.onWarn("Failed to delete state file: " + ex.getMessage());
      }
    }

    listener.onWorldDone(totalRegions, totalChunksVisited, totalChunksModified, totalBlocksAffected);
  }

  private RegionStats processRegion(WorldDimension dim, Path regionFile) throws IOException {
    RegionFormat format = RegionFormat.fromPath(regionFile);
    RegionCoords coords = RegionCoords.parse(regionFile);
    int presentChunks = 0;
    int visitedChunks = 0;
    try (RegionBackend backend = RegionFileSelector.open(regionFile)) {
      for (int z = 0; z < RegionFile.CHUNKS_PER_REGION_SIDE; z++) {
        for (int x = 0; x < RegionFile.CHUNKS_PER_REGION_SIDE; x++) {
          if (backend.hasChunk(x, z)) presentChunks++;
        }
      }
    }

    boolean lightOnly = tasks.isEmpty() && options.fixLight();

    listener.onRegionStart(dim, regionFile, presentChunks);
    listener.onInfo(
        "Processing region "
            + dim
            + " "
            + regionFile.getFileName()
            + " (presentChunks="
            + presentChunks
            + ", mode="
            + (options.dryRun() ? "dry-run" : "real")
            + (lightOnly ? ", light-only" : "")
            + ")");
    if (presentChunks == 0) {
      listener.onRegionDone(dim, regionFile, false, 0, 0L, 0, 0);
      return new RegionStats(0, 0, 0L);
    }

    if (options.dryRun()) {
      // Dry-run: just walk chunks and compute stats, no writes.
      int regionChunksModified = 0;
      long regionBlocksAffected = 0L;
      int regionPaletteChanges = 0;

      try (RegionBackend backend = RegionFileSelector.open(regionFile)) {
        for (int localZ = 0; localZ < RegionFile.CHUNKS_PER_REGION_SIDE; localZ++) {
          for (int localX = 0; localX < RegionFile.CHUNKS_PER_REGION_SIDE; localX++) {
            if (!backend.hasChunk(localX, localZ)) continue;
            visitedChunks++;
            if (lightOnly) {
              regionChunksModified++;
              int chunkX = coords.chunkX(localX);
              int chunkZ = coords.chunkZ(localZ);
              listener.onChunkDone(dim, regionFile, chunkX, chunkZ, 0L, 0, 0);
              continue;
            }
            RegionChunkData data = backend.readChunk(localX, localZ).orElse(null);
            if (data == null) continue;
            NbtCompound root = ChunkCodecs.decode(data, format);
            ChunkReplaceResult r = ChunkReplacer.applyTasks(root, tasks, options.fixSnowyGround());
            if (!r.modified()) continue;

            regionChunksModified++;
            regionBlocksAffected += r.blocksAffected();
            regionPaletteChanges += r.paletteEntriesChanged();

            int chunkX = coords.chunkX(localX);
            int chunkZ = coords.chunkZ(localZ);
            listener.onChunkDone(
                dim,
                regionFile,
                chunkX,
                chunkZ,
                r.blocksAffected(),
                r.paletteEntriesChanged(),
                0);
          }
        }
      }

      if (visitedChunks != presentChunks) {
        listener.onWarn(
            "Dry-run mismatch: presentChunks="
                + presentChunks
                + " visitedChunks="
                + visitedChunks
                + " in "
                + regionFile);
      }

      boolean modified = regionChunksModified > 0;
      listener.onRegionDone(
          dim,
          regionFile,
          modified,
          regionChunksModified,
          regionBlocksAffected,
          regionPaletteChanges,
          0);

      return new RegionStats(visitedChunks, regionChunksModified, regionBlocksAffected);
    }

    // Real run: rebuild region file with transformations.
    Path target = regionFile;
    if (options.backup()) {
      Path regionDir = regionFile.getParent();
      if (regionDir != null) {
        Path backupDir = regionDir.resolveSibling(regionDir.getFileName().toString() + "_bak");
        try {
          Files.createDirectories(backupDir);
          Path backup = backupDir.resolve(regionFile.getFileName());
          if (!Files.exists(backup)) {
            Files.copy(regionFile, backup, StandardCopyOption.COPY_ATTRIBUTES);
            listener.onInfo("Created backup: " + backup);
          }
        } catch (IOException ex) {
          listener.onWarn(
              "Failed to create backup for " + regionFile + ": " + ex.getMessage());
        }
      }
    }

    final long[] regionBlocksAffected = {0L};
    final int[] regionPaletteChanges = {0};
    final int[] regionChunksModified = {0};
    final int[] regionBlockEntitiesRemoved = {0};
    final int[] regionChunksVisited = {0};

    RegionBackend.ChunkTransformer transformer =
        (localX, localZ, original) -> {
          regionChunksVisited[0]++;
          NbtCompound root = ChunkCodecs.decode(original, format);

          if (lightOnly) {
            LightFixer.fixChunk(root, blockDb, dim);
            regionChunksModified[0]++;
            RegionCoords c = coords;
            listener.onChunkDone(dim, regionFile, c.chunkX(localX), c.chunkZ(localZ), 0L, 0, 0);
            return ChunkCodecs.encode(root, original, format);
          }

          ChunkReplaceResult r = ChunkReplacer.applyTasks(root, tasks, options.fixSnowyGround());
          if (!r.modified()) {
            return original;
          }

          RegionCoords c = coords;
          int chunkX = c.chunkX(localX);
          int chunkZ = c.chunkZ(localZ);

          int beRemoved =
              BlockEntityFixer.removeBlockEntitiesForNameChangedBlocks(
                  root, chunkX, chunkZ, r.nameChangedPaletteIndicesBySection());

          if (options.fixLight()) {
            LightFixer.fixChunk(root, blockDb, dim);
          }

          RegionChunkData encoded = ChunkCodecs.encode(root, original, format);

          regionBlocksAffected[0] += r.blocksAffected();
          regionPaletteChanges[0] += r.paletteEntriesChanged();
          regionChunksModified[0]++;
          regionBlockEntitiesRemoved[0] += beRemoved;

          listener.onChunkDone(
              dim,
              regionFile,
              chunkX,
              chunkZ,
              r.blocksAffected(),
              r.paletteEntriesChanged(),
              beRemoved);

          return encoded;
        };

    RegionFileSelector.rebuild(regionFile, target, transformer);

    if (regionChunksVisited[0] != presentChunks) {
      listener.onWarn(
          "Rebuild mismatch: presentChunks="
              + presentChunks
              + " visitedChunks="
              + regionChunksVisited[0]
              + " in "
              + regionFile);
    }

    listener.onRegionDone(
        dim,
        regionFile,
        regionChunksModified[0] > 0,
        regionChunksModified[0],
        regionBlocksAffected[0],
        regionPaletteChanges[0],
        regionBlockEntitiesRemoved[0]);

    return new RegionStats(regionChunksVisited[0], regionChunksModified[0], regionBlocksAffected[0]);
  }

  private record DimRegionBatch(WorldDimension dim, List<Path> regionFiles, int startIndex) {}

  private record RegionStats(int chunksVisited, int chunksModified, long blocksAffected) {}
}

