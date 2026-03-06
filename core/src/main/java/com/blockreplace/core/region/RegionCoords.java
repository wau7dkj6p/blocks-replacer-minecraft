package com.blockreplace.core.region;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record RegionCoords(int regionX, int regionZ) {
  // Support both Anvil (.mca) and McRegion (.mcr) files.
  private static final Pattern REGION_NAME = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mc[ar]$");

  public static RegionCoords parse(Path regionFile) {
    String name = regionFile.getFileName().toString();
    Matcher m = REGION_NAME.matcher(name);
    if (!m.matches()) {
      throw new IllegalArgumentException("Not a region file name: " + name);
    }
    return new RegionCoords(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
  }

  public int chunkX(int localX) {
    return regionX * RegionFile.CHUNKS_PER_REGION_SIDE + localX;
  }

  public int chunkZ(int localZ) {
    return regionZ * RegionFile.CHUNKS_PER_REGION_SIDE + localZ;
  }
}

