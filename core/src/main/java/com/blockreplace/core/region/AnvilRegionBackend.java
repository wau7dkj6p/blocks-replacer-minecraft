package com.blockreplace.core.region;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public final class AnvilRegionBackend implements RegionBackend {
  private final RegionFile regionFile;
  private final RegionCoords coords;

  private AnvilRegionBackend(RegionFile regionFile, RegionCoords coords) {
    this.regionFile = regionFile;
    this.coords = coords;
  }

  public static AnvilRegionBackend open(Path path) throws IOException {
    RegionFile rf = RegionFile.open(path);
    RegionCoords coords = RegionCoords.parse(path);
    return new AnvilRegionBackend(rf, coords);
  }

  @Override
  public Path path() {
    return regionFile.path();
  }

  @Override
  public RegionCoords coords() {
    return coords;
  }

  @Override
  public RegionFormat format() {
    return RegionFormat.ANVIL;
  }

  @Override
  public boolean hasChunk(int localX, int localZ) {
    return regionFile.hasChunk(localX, localZ);
  }

  @Override
  public Optional<RegionChunkData> readChunk(int localX, int localZ) throws IOException {
    return regionFile.readChunk(localX, localZ);
  }

  @Override
  public void close() throws IOException {
    regionFile.close();
  }
}

