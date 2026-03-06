package com.blockreplace.core.nbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class NbtList implements NbtTag {
  private final NbtType elementType;
  private final ArrayList<NbtTag> elements;

  public NbtList(NbtType elementType, List<? extends NbtTag> elements) {
    this.elementType = Objects.requireNonNull(elementType, "elementType");
    this.elements = new ArrayList<>(elements == null ? List.of() : elements);
  }

  public static NbtList of(NbtType elementType) {
    return new NbtList(elementType, List.of());
  }

  @Override
  public NbtType type() {
    return NbtType.LIST;
  }

  public NbtType elementType() {
    return elementType;
  }

  public List<NbtTag> elements() {
    return Collections.unmodifiableList(elements);
  }

  public int size() {
    return elements.size();
  }

  public NbtTag get(int index) {
    return elements.get(index);
  }

  public void add(NbtTag tag) {
    elements.add(tag);
  }

  public void set(int index, NbtTag tag) {
    elements.set(index, tag);
  }

  public void clear() {
    elements.clear();
  }

  public NbtTag remove(int index) {
    return elements.remove(index);
  }

  public int removeIf(Predicate<NbtTag> predicate) {
    int before = elements.size();
    elements.removeIf(predicate);
    return before - elements.size();
  }
}

