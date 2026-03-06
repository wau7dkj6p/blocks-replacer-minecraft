package com.blockreplace.core.blockdb;

import java.util.Map;

public record BlockDef(
    String name,
    String displayName,
    Map<String, PropertyDef> properties,
    int emitLight,
    int filterLight) {
  public BlockDef(String name, String displayName, Map<String, PropertyDef> properties) {
    this(name, displayName, properties, 0, 15);
  }
}

