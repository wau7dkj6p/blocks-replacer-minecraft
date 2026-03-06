package com.blockreplace.core.nbt;

public record NbtByte(byte value) implements NbtTag {
  @Override
  public NbtType type() {
    return NbtType.BYTE;
  }
}

