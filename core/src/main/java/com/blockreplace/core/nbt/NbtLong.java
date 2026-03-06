package com.blockreplace.core.nbt;

public record NbtLong(long value) implements NbtTag {
  @Override
  public NbtType type() {
    return NbtType.LONG;
  }
}

