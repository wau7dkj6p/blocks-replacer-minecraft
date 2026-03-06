package com.blockreplace.core.world;

import com.blockreplace.core.task.BlockStateSpec;
import com.blockreplace.core.task.ReplaceTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JSON-serializable state of world processing, stored in the world root directory.
 *
 * <p>Allows resuming from the last fully processed region.
 */
final class WorldProcessState {
  public String worldRoot;
  public boolean allowUnknownBlocks;
  public boolean fixSnowyGround;
  public List<TaskState> tasks = new ArrayList<>();
  public Map<String, Integer> lastRegionIndexByDimension = new HashMap<>();

  static final class TaskState {
    public String from;
    public String to;
    public boolean enabled;
  }

  static WorldProcessState load(Path file) throws IOException {
    if (!Files.isRegularFile(file)) {
      return null;
    }
    ObjectMapper om = new ObjectMapper();
    return om.readValue(file.toFile(), WorldProcessState.class);
  }

  static void save(Path file, WorldProcessState state) throws IOException {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(state, "state");
    ObjectMapper om = new ObjectMapper();
    Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
    om.writeValue(tmp.toFile(), state);
    Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
  }

  static void deleteIfExists(Path file) throws IOException {
    if (Files.isRegularFile(file)) {
      Files.delete(file);
    }
  }

  boolean isCompatible(
      Path worldRoot, List<ReplaceTask> tasks, boolean allowUnknownBlocks, boolean fixSnowyGround) {
    if (worldRoot == null || tasks == null) return false;
    String rootStr = worldRoot.toAbsolutePath().normalize().toString();
    if (!rootStr.equals(this.worldRoot)) return false;
    if (this.allowUnknownBlocks != allowUnknownBlocks) return false;
    if (this.fixSnowyGround != fixSnowyGround) return false;
    if (this.tasks == null) return false;
    if (this.tasks.size() != tasks.size()) return false;
    for (int i = 0; i < tasks.size(); i++) {
      ReplaceTask t = tasks.get(i);
      TaskState ts = this.tasks.get(i);
      if (ts == null) return false;
      if (ts.enabled != t.enabled()) return false;
      String fromStr = t.from() == null ? null : t.from().toString();
      String toStr = t.to() == null ? null : t.to().toString();
      if (!Objects.equals(ts.from, fromStr) || !Objects.equals(ts.to, toStr)) {
        return false;
      }
    }
    return true;
  }

  static WorldProcessState create(
      Path worldRoot, List<ReplaceTask> tasks, boolean allowUnknownBlocks, boolean fixSnowyGround) {
    WorldProcessState s = new WorldProcessState();
    s.worldRoot = worldRoot.toAbsolutePath().normalize().toString();
    s.allowUnknownBlocks = allowUnknownBlocks;
    s.fixSnowyGround = fixSnowyGround;
    s.tasks = new ArrayList<>(tasks.size());
    for (ReplaceTask t : tasks) {
      TaskState ts = new TaskState();
      BlockStateSpec from = t.from();
      BlockStateSpec to = t.to();
      ts.from = from == null ? null : from.toString();
      ts.to = to == null ? null : to.toString();
      ts.enabled = t.enabled();
      s.tasks.add(ts);
    }
    s.lastRegionIndexByDimension = new HashMap<>();
    return s;
  }
}

