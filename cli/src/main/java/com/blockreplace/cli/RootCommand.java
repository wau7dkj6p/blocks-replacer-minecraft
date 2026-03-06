package com.blockreplace.cli;

import com.blockreplace.core.blockdb.BlockDatabase;
import com.blockreplace.core.task.BlockStateSpec;
import com.blockreplace.core.task.ReplaceTask;
import com.blockreplace.core.world.WorldBlockScanner;
import com.blockreplace.core.world.WorldDimension;
import com.blockreplace.core.world.WorldProcessor;
import com.blockreplace.core.world.WorldProcessorOptions;
import com.blockreplace.core.world.WorldProgressListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(
    name = "block-replace",
    mixinStandardHelpOptions = true,
    description =
        "Mass block replacement tool for Minecraft Java Edition worlds (.mca, .mcr / Beta 1.3–1.7).")
public final class RootCommand implements Runnable {

  @Mixin private SharedOptions shared;

  @Option(
      names = {"--level-dat"},
      required = true,
      description = "Path to level.dat inside the world folder.")
  private Path levelDat;

  @Option(
      names = {"--dims"},
      split = ",",
      description = "Dimensions to process: overworld,nether,end (default: overworld).")
  private List<String> dims;

  @Option(
      names = {"--from"},
      description = "Single task FROM spec, e.g. minecraft:acacia_button[powered=true].")
  private String from;

  @Option(
      names = {"--to"},
      description = "Single task TO spec, e.g. air or minecraft:acacia_button[powered=false,face=all].")
  private String to;

  @Option(
      names = {"--task"},
      description = "Replacement task in format FROM->TO, can be repeated.")
  private List<String> taskSpecs;

  @Option(
      names = {"--dry-run"},
      description = "Dry run: do not modify files, just report what would change.")
  private boolean dryRun;

  @Option(
      names = {"--backup"},
      description = "Create .bak next to each modified region file.")
  private boolean backup;

  @Option(
      names = {"--allow-unknown-blocks"},
      description = "Allow unknown blocks and properties (mods mode).")
  private boolean allowUnknownBlocks;

  @Option(
      names = {"--scan-only"},
      description =
          "Scan-only mode: count occurrences of the given block name across the world without modifying files. "
              + "Requires --from to specify the block name (properties are ignored).")
  private boolean scanOnly;

   @Option(
      names = {"--save-state"},
      description = "Save progress state in world folder (.block-replace-state.json).")
   private boolean saveState;

   @Option(
      names = {"--resume"},
      description = "Resume from saved state if present.")
   private boolean resume;

  @Option(
      names = {"--no-fix-snowy-ground"},
      description = "Disable automatic fixing of snowy=true grass/podzol/mycelium under removed snow.")
  private boolean noFixSnowyGround;

  @Option(
      names = {"--fix-light"},
      description = "Recalculate block/sky light after replacement (or run light-only when no tasks).")
  private boolean fixLight;

  @Override
  public void run() {
    try {
      Path worldRoot = validateWorldRoot();
      EnumSet<WorldDimension> dimensions = parseDimensions();

      if (scanOnly) {
        runScanOnly(worldRoot, dimensions);
        return;
      }

      List<ReplaceTask> tasks = buildTasks();

      // High-level configuration summary for easier debugging.
      System.out.println("=== block-replace configuration ===");
      System.out.println("World root      : " + worldRoot);
      System.out.println("Dimensions      : " + dimensions);
      System.out.println(
          "Options         : dryRun="
              + dryRun
              + ", backup="
              + backup
              + ", allowUnknownBlocks="
              + allowUnknownBlocks
              + ", saveState="
              + saveState
              + ", resume="
              + resume
              + ", fixSnowyGround="
              + !noFixSnowyGround
              + ", fixLight="
              + fixLight);
      if (tasks.isEmpty()) {
        System.out.println("Tasks           : <none>");
      } else {
        System.out.println("Tasks:");
        int idx = 1;
        for (ReplaceTask t : tasks) {
          System.out.printf(
              "  %d) %s -> %s (title=\"%s\", enabled=%s)%n",
              idx++, t.from(), t.to(), t.title(), t.enabled());
        }
      }
      System.out.println("===================================");

      BlockDatabase db = BlockDatabase.loadBundled();
      WorldProcessorOptions opts =
          new WorldProcessorOptions(
              worldRoot,
              dimensions,
              dryRun,
              backup,
              allowUnknownBlocks,
              saveState,
              resume,
              !noFixSnowyGround,
              fixLight);
      WorldProcessor processor =
          new WorldProcessor(
              opts, tasks, db, new CliProgressListener(shared != null && shared.ansi));
      processor.run();
    } catch (IllegalArgumentException e) {
      System.err.println("Configuration/argument error: " + e.getMessage());
    } catch (java.io.IOException e) {
      System.err.println("I/O error while processing world: " + e.getMessage());
      e.printStackTrace(System.err);
    } catch (Throwable t) {
      System.err.println("Unexpected error:");
      t.printStackTrace(System.err);
    }
  }

  private Path validateWorldRoot() {
    if (levelDat == null) {
      throw new IllegalArgumentException("--level-dat is required");
    }
    Path p = levelDat;
    if (!Files.isRegularFile(p)) {
      throw new IllegalArgumentException("level.dat not found: " + p);
    }
    return p.toAbsolutePath().getParent();
  }

  private EnumSet<WorldDimension> parseDimensions() {
    if (dims == null || dims.isEmpty()) {
      return EnumSet.of(WorldDimension.OVERWORLD);
    }
    EnumSet<WorldDimension> out = EnumSet.noneOf(WorldDimension.class);
    for (String d : dims) {
      String s = d.toLowerCase(Locale.ROOT).trim();
      switch (s) {
        case "overworld" -> out.add(WorldDimension.OVERWORLD);
        case "nether" -> out.add(WorldDimension.NETHER);
        case "end" -> out.add(WorldDimension.END);
        default -> throw new IllegalArgumentException("Unknown dimension: " + d);
      }
    }
    if (out.isEmpty()) out.add(WorldDimension.OVERWORLD);
    return out;
  }

  private List<ReplaceTask> buildTasks() {
    ArrayList<ReplaceTask> list = new ArrayList<>();
    if (taskSpecs != null && !taskSpecs.isEmpty()) {
      int idx = 1;
      for (String spec : taskSpecs) {
        if (spec == null || spec.isBlank()) {
          throw new IllegalArgumentException("--task entries must not be empty");
        }
        BlockStateSpec[] parsed = parseTaskSpec(spec);
        BlockStateSpec fromSpec = parsed[0];
        BlockStateSpec toSpec = parsed[1];
        String title = "cli-task-" + idx + ": " + fromSpec + " -> " + toSpec;
        list.add(new ReplaceTask(null, title, fromSpec, toSpec, false, true));
        idx++;
      }
    } else if (from != null && to != null) {
      BlockStateSpec fromSpec = BlockStateSpec.parse(from);
      BlockStateSpec toSpec = BlockStateSpec.parse(to);
      list.add(new ReplaceTask(null, "cli-task: " + fromSpec + " -> " + toSpec, fromSpec, toSpec, false, true));
    } else if (fixLight) {
      // Light-only run: no tasks, just recalculate lighting.
    } else {
      throw new IllegalArgumentException(
          "No tasks specified. Use either --from and --to for a single task, or one/more --task FROM->TO entries (or --fix-light for light-only run).");
    }
    return list;
  }

  private BlockStateSpec[] parseTaskSpec(String spec) {
    String trimmed = spec.trim();
    int arrow = trimmed.indexOf("->");
    if (arrow <= 0 || arrow + 2 >= trimmed.length()) {
      throw new IllegalArgumentException(
          "Invalid --task format: \""
              + spec
              + "\". Expected \"FROM->TO\", e.g. minecraft:snow->air or minecraft:snow_block->air");
    }
    String fromPart = trimmed.substring(0, arrow).trim();
    String toPart = trimmed.substring(arrow + 2).trim();
    if (fromPart.isEmpty() || toPart.isEmpty()) {
      throw new IllegalArgumentException(
          "Invalid --task format: \""
              + spec
              + "\". FROM and TO must be non-empty around \"->\".");
    }
    BlockStateSpec fromSpec = BlockStateSpec.parse(fromPart);
    BlockStateSpec toSpec = BlockStateSpec.parse(toPart);
    return new BlockStateSpec[] {fromSpec, toSpec};
  }

  private void runScanOnly(Path worldRoot, EnumSet<WorldDimension> dimensions) throws java.io.IOException {
    if (from == null || from.isBlank()) {
      throw new IllegalArgumentException("--scan-only requires --from with block name");
    }
    BlockStateSpec fromSpec = BlockStateSpec.parse(from);
    String blockName = fromSpec.name();

    WorldBlockScanner scanner = new WorldBlockScanner(worldRoot, dimensions, blockName);
    WorldBlockScanner.Result result = scanner.run();

    System.out.printf(
        "Scan-only for block %s: regions=%d, visitedChunks=%d, blocksFound=%d%n",
        blockName, result.totalRegionFiles(), result.totalChunksVisited(), result.totalBlocksFound());
  }

  static final class CliProgressListener implements WorldProgressListener {
    private final boolean ansi;

    private int worldTotalRegions;
    private int worldChunksVisited;
    private int worldChunksModified;
    private long worldBlocksAffected;

    CliProgressListener(boolean ansi) {
      this.ansi = ansi;
    }

    @Override
    public void onInfo(String message) {
      System.out.println(message);
    }

    @Override
    public void onWarn(String message) {
      printlnColored("[WARN] " + message, 33);
    }

    @Override
    public void onError(String message, Throwable t) {
      printlnColored("[ERROR] " + message, 31);
      if (t != null) {
        t.printStackTrace(System.err);
      }
    }

    @Override
    public void onRegionStart(WorldDimension dim, Path regionFile, int presentChunks) {
      System.out.printf(
          "Region %s %s (%d chunks present)%n", dim, regionFile.getFileName(), presentChunks);
    }

    @Override
    public void onChunkDone(
        WorldDimension dim,
        Path regionFile,
        int chunkX,
        int chunkZ,
        long blocksAffected,
        int paletteChanges,
        int blockEntitiesRemoved) {
      if (blocksAffected == 0 && paletteChanges == 0 && blockEntitiesRemoved == 0) return;
      System.out.printf(
          "  chunk (%d,%d): blocks=%d, palette=%d, blockEntitiesRemoved=%d%n",
          chunkX, chunkZ, blocksAffected, paletteChanges, blockEntitiesRemoved);
    }

    @Override
    public void onRegionDone(
        WorldDimension dim,
        Path regionFile,
        boolean modified,
        int chunksModified,
        long blocksAffected,
        int paletteChanges,
        int blockEntitiesRemoved) {
      String status = modified ? "MODIFIED" : "unchanged";
      System.out.printf(
          "Region done %s: %s, chunks=%d, blocks=%d, palette=%d, blockEntitiesRemoved=%d%n",
          dim, status, chunksModified, blocksAffected, paletteChanges, blockEntitiesRemoved);
    }

    @Override
    public void onWorldStart(int totalRegionFiles) {
      this.worldTotalRegions = totalRegionFiles;
      this.worldChunksVisited = 0;
      this.worldChunksModified = 0;
      this.worldBlocksAffected = 0L;
    }

    @Override
    public void onWorldDone(
        int totalRegionFiles,
        int totalChunksVisited,
        int totalChunksModified,
        long totalBlocksAffected) {
      this.worldTotalRegions = totalRegionFiles;
      this.worldChunksVisited = totalChunksVisited;
      this.worldChunksModified = totalChunksModified;
      this.worldBlocksAffected = totalBlocksAffected;

      System.out.printf(
          "World summary: regions=%d, visitedChunks=%d, modifiedChunks=%d, blocksAffected=%d%n",
          worldTotalRegions, worldChunksVisited, worldChunksModified, worldBlocksAffected);
    }

    private void printlnColored(String msg, int colorCode) {
      if (!ansi) {
        System.out.println(msg);
        return;
      }
      System.out.println("\u001B[" + colorCode + "m" + msg + "\u001B[0m");
    }
  }
}

