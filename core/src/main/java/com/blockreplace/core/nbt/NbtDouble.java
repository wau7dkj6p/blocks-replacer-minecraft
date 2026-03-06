package com.blockreplace.core.nbt;

public record NbtDouble(double value) implements NbtTag {
  @Override
  public NbtType type() {
    return NbtType.DOUBLE;
  }
}

