package com.blockreplace.core.nbt;

public record NbtShort(short value) implements NbtTag {
  @Override
  public NbtType type() {
    return NbtType.SHORT;
  }
}

