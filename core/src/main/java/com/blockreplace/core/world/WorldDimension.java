package com.blockreplace.core.world;

import java.nio.file.Path;

public enum WorldDimension {
  OVERWORLD(""),
  NETHER("DIM-1"),
  END("DIM1");

  private final String folder;

  WorldDimension(String folder) {
    this.folder = folder;
  }

  public String folder() {
    return folder;
  }

  public Path dimensionRoot(Path worldRoot) {
    return folder.isEmpty() ? worldRoot : worldRoot.resolve(folder);
  }

  public Path regionDir(Path worldRoot) {
    return dimensionRoot(worldRoot).resolve("region");
  }
}

