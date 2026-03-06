package com.blockreplace.core.chunk;

public final class PaletteCodec {
  private PaletteCodec() {}

  public static int bitsPerEntry(int paletteSize) {
    if (paletteSize <= 1) return 0;
    int needed = 32 - Integer.numberOfLeadingZeros(paletteSize - 1);
    return Math.max(4, needed);
  }

  public static int[] decode4096(int bitsPerEntry, long[] data) {
    int[] out = new int[4096];
    if (bitsPerEntry == 0) {
      return out;
    }
    long mask = (1L << bitsPerEntry) - 1L;
    int valuesPerLong = 64 / bitsPerEntry;
    int idx = 0;
    for (long l : data) {
      for (int i = 0; i < valuesPerLong && idx < 4096; i++) {
        out[idx++] = (int) ((l >>> (i * bitsPerEntry)) & mask);
      }
      if (idx >= 4096) break;
    }
    return out;
  }
}

