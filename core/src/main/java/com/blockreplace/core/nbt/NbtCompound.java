package com.blockreplace.core.nbt;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class NbtCompound implements NbtTag, Iterable<Map.Entry<String, NbtTag>> {
  private final LinkedHashMap<String, NbtTag> values = new LinkedHashMap<>();

  @Override
  public NbtType type() {
    return NbtType.COMPOUND;
  }

  public Map<String, NbtTag> values() {
    return Collections.unmodifiableMap(values);
  }

  public boolean containsKey(String key) {
    return values.containsKey(key);
  }

  public Optional<NbtTag> get(String key) {
    return Optional.ofNullable(values.get(key));
  }

  public NbtTag getOrThrow(String key) {
    NbtTag t = values.get(key);
    if (t == null) throw new IllegalStateException("Missing NBT key: " + key);
    return t;
  }

  public void put(String key, NbtTag tag) {
    values.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(tag, "tag"));
  }

  public NbtTag remove(String key) {
    return values.remove(key);
  }

  public int size() {
    return values.size();
  }

  @Override
  public Iterator<Map.Entry<String, NbtTag>> iterator() {
    return values.entrySet().iterator();
  }

  public Optional<String> getString(String key) {
    return get(key).filter(t -> t instanceof NbtString).map(t -> ((NbtString) t).value());
  }

  public Optional<Integer> getInt(String key) {
    return get(key)
        .map(
            t -> {
              if (t instanceof NbtInt n) return n.value();
              if (t instanceof NbtByte n) return (int) n.value();
              if (t instanceof NbtShort n) return (int) n.value();
              return null;
            })
        .map(Optional::ofNullable)
        .orElse(Optional.empty());
  }

  public Optional<Long> getLong(String key) {
    return get(key)
        .map(
            t -> {
              if (t instanceof NbtLong n) return n.value();
              if (t instanceof NbtInt n) return (long) n.value();
              if (t instanceof NbtByte n) return (long) n.value();
              if (t instanceof NbtShort n) return (long) n.value();
              return null;
            })
        .map(Optional::ofNullable)
        .orElse(Optional.empty());
  }

  public Optional<NbtCompound> getCompound(String key) {
    return get(key).filter(t -> t instanceof NbtCompound).map(t -> (NbtCompound) t);
  }

  public Optional<NbtList> getList(String key) {
    return get(key).filter(t -> t instanceof NbtList).map(t -> (NbtList) t);
  }

  public Optional<long[]> getLongArray(String key) {
    return get(key).filter(t -> t instanceof NbtLongArray).map(t -> ((NbtLongArray) t).value());
  }

  public Optional<byte[]> getByteArray(String key) {
    return get(key).filter(t -> t instanceof NbtByteArray).map(t -> ((NbtByteArray) t).value());
  }

  public void put(String key, byte[] value) {
    put(key, new NbtByteArray(value));
  }
}

