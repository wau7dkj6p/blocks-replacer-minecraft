package com.blockreplace.core.nbt;

import java.util.Arrays;

public record NbtIntArray(int[] value) implements NbtTag {
  public NbtIntArray {
    value = value == null ? new int[0] : value;
  }

  @Override
  public NbtType type() {
    return NbtType.INT_ARRAY;
  }

  @Override
  public int[] value() {
    return Arrays.copyOf(value, value.length);
  }
}

