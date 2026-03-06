package com.blockreplace.core.nbt;

import java.util.Arrays;

public record NbtLongArray(long[] value) implements NbtTag {
  public NbtLongArray {
    value = value == null ? new long[0] : value;
  }

  @Override
  public NbtType type() {
    return NbtType.LONG_ARRAY;
  }

  @Override
  public long[] value() {
    return Arrays.copyOf(value, value.length);
  }
}

