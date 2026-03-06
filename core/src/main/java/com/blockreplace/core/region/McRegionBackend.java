package com.blockreplace.core.region;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Backend for McRegion (.mcr) files.
 *
 * <p>The underlying file structure (header + chunk layout) is identical to Anvil region files, so
 * this implementation reuses {@link RegionFile} for low-level IO, but exposes a distinct
 * {@link RegionFormat#MCREGION} to allow higher layers to choose an appropriate chunk codec.
 */
public final class McRegionBackend implements RegionBackend {
  private final RegionFile regionFile;
  private final RegionCoords coords;

  private McRegionBackend(RegionFile regionFile, RegionCoords coords) {
    this.regionFile = regionFile;
    this.coords = coords;
  }

  public static McRegionBackend open(Path path) throws IOException {
    RegionFile rf = RegionFile.open(path);
    RegionCoords coords = RegionCoords.parse(path);
    return new McRegionBackend(rf, coords);
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
    return RegionFormat.MCREGION;
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

