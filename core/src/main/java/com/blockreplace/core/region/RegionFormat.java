package com.blockreplace.core.region;

public enum RegionFormat {
  ANVIL,
  MCREGION;

  public static RegionFormat fromPath(java.nio.file.Path path) {
    String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
    if (name.endsWith(".mca")) {
      return ANVIL;
    } else if (name.endsWith(".mcr")) {
      return MCREGION;
    } else {
      throw new IllegalArgumentException("Unknown region file format: " + name);
    }
  }
}

