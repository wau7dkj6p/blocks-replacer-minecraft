package com.blockreplace.core.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class McRegionBackendTest {

  @Test
  void selectorRecognisesMcrAndOpensBackend() throws IOException {
    Path tmp = Files.createTempFile("r.0.0", ".mcr");
    try {
      // Minimal valid region file: header only, no chunks.
      Files.write(tmp, new byte[RegionFile.HEADER_BYTES]);

      assertTrue(RegionFileSelector.isRegionFile(tmp));

      try (RegionBackend backend = RegionFileSelector.open(tmp)) {
        assertEquals(RegionFormat.MCREGION, backend.format());
        assertFalse(backend.hasChunk(0, 0));
      }
    } finally {
      Files.deleteIfExists(tmp);
    }
  }
}

