package com.blockreplace.core.nbt;

public record NbtString(String value) implements NbtTag {
  public NbtString {
    if (value == null) value = "";
  }

  @Override
  public NbtType type() {
    return NbtType.STRING;
  }
}

