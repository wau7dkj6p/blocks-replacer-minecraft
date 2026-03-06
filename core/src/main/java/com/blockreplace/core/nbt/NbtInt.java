package com.blockreplace.core.nbt;

public record NbtInt(int value) implements NbtTag {
  @Override
  public NbtType type() {
    return NbtType.INT;
  }
}

