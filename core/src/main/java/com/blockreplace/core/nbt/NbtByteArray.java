package com.blockreplace.core.nbt;

import java.util.Arrays;

public record NbtByteArray(byte[] value) implements NbtTag {
  public NbtByteArray {
    value = value == null ? new byte[0] : value;
  }

  @Override
  public NbtType type() {
    return NbtType.BYTE_ARRAY;
  }

  @Override
  public byte[] value() {
    return Arrays.copyOf(value, value.length);
  }
}

