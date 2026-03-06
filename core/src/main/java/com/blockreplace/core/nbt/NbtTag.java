package com.blockreplace.core.nbt;

public sealed interface NbtTag permits
    NbtByte,
    NbtShort,
    NbtInt,
    NbtLong,
    NbtFloat,
    NbtDouble,
    NbtByteArray,
    NbtIntArray,
    NbtLongArray,
    NbtString,
    NbtList,
    NbtCompound {
  NbtType type();
}

