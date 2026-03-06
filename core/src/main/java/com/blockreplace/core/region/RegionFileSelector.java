package com.blockreplace.core.region;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public final class RegionFileSelector {
  private RegionFileSelector() {}

  public static boolean isRegionFile(Path path) {
    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
    return name.endsWith(".mca") || name.endsWith(".mcr");
  }

  public static RegionBackend open(Path path) throws IOException {
    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
    if (name.endsWith(".mca")) {
      return AnvilRegionBackend.open(path);
    } else if (name.endsWith(".mcr")) {
      return McRegionBackend.open(path);
    }
    throw new IllegalArgumentException("Not a supported region file: " + name);
  }

  public static RegionFormat detectFormat(Path path) {
    return RegionFormat.fromPath(path);
  }

  public static void rebuild(
      Path src, Path dest, RegionBackend.ChunkTransformer transformer) throws IOException {
    RegionFile.rebuild(
        src,
        dest,
        (localX, localZ, in) -> transformer.transform(localX, localZ, in));
  }
}

