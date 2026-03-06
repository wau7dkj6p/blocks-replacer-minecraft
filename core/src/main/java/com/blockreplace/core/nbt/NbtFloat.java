package com.blockreplace.core.nbt;

public record NbtFloat(float value) implements NbtTag {
  @Override
  public NbtType type() {
    return NbtType.FLOAT;
  }
}

