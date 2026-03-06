package com.blockreplace.core.chunk;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PaletteCodecTest {

  @Test
  void bitsPerEntryRespectsMinimum4() {
    assertEquals(4, PaletteCodec.bitsPerEntry(1));
    assertEquals(4, PaletteCodec.bitsPerEntry(2));
    assertEquals(4, PaletteCodec.bitsPerEntry(16));
    assertEquals(5, PaletteCodec.bitsPerEntry(17));
  }

  @Test
  void decodeSimplePattern() {
    int bpe = 4;
    long[] data = new long[16];
    // Fill with repeating 0..15 pattern.
    int idx = 0;
    for (int i = 0; i < data.length; i++) {
      long v = 0;
      for (int j = 0; j < 16; j++) {
        int val = j;
        v |= (long) val << (j * bpe);
        idx++;
        if (idx >= 4096) break;
      }
      data[i] = v;
      if (idx >= 4096) break;
    }
    int[] decoded = PaletteCodec.decode4096(bpe, data);
    for (int i = 0; i < decoded.length; i++) {
      assertEquals(i % 16, decoded[i]);
    }
  }
}

