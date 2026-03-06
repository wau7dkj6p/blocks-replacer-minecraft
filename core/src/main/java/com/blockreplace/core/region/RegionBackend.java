package com.blockreplace.core.region;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public interface RegionBackend extends Closeable {
  Path path();

  RegionCoords coords();

  RegionFormat format();

  boolean hasChunk(int localX, int localZ) throws IOException;

  Optional<RegionChunkData> readChunk(int localX, int localZ) throws IOException;

  @Override
  void close() throws IOException;

  @FunctionalInterface
  interface ChunkTransformer {
    RegionChunkData transform(int localX, int localZ, RegionChunkData in) throws IOException;
  }
}

