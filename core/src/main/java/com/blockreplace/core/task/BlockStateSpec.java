package com.blockreplace.core.task;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A block state spec like: minecraft:acacia_button[powered=true,facing=all]
 *
 * <p>Value {@code all} has special meaning:
 *
 * <ul>
 *   <li>in FROM spec: wildcard for matching
 *   <li>in TO spec: copy the matched value from the source state (if present)
 * </ul>
 */
public final class BlockStateSpec {
  public static final String ALL = "all";

  private final String name;
  private final LinkedHashMap<String, String> properties;

  public BlockStateSpec(String name, Map<String, String> properties) {
    this.name = normalizeName(Objects.requireNonNull(name, "name"));
    this.properties = new LinkedHashMap<>();
    if (properties != null) {
      for (Map.Entry<String, String> e : properties.entrySet()) {
        this.properties.put(
            Objects.requireNonNull(e.getKey(), "propertyKey"),
            Objects.requireNonNull(e.getValue(), "propertyValue"));
      }
    }
  }

  public String name() {
    return name;
  }

  public Map<String, String> properties() {
    return Map.copyOf(properties);
  }

  public static String normalizeName(String name) {
    String n = name.trim();
    if (!n.contains(":")) n = "minecraft:" + n;
    return n;
  }

  public static BlockStateSpec parse(String s) {
    String in = Objects.requireNonNull(s, "s").trim();
    if (in.isEmpty()) throw new IllegalArgumentException("Empty block spec");

    int open = in.indexOf('[');
    if (open < 0) {
      return new BlockStateSpec(in, Map.of());
    }
    if (!in.endsWith("]")) {
      throw new IllegalArgumentException("Invalid block spec (missing ']'): " + s);
    }
    String name = in.substring(0, open).trim();
    String inside = in.substring(open + 1, in.length() - 1).trim();

    LinkedHashMap<String, String> props = new LinkedHashMap<>();
    if (!inside.isEmpty()) {
      String[] parts = inside.split(",");
      for (String part : parts) {
        String p = part.trim();
        if (p.isEmpty()) continue;
        int eq = p.indexOf('=');
        if (eq <= 0 || eq == p.length() - 1) {
          throw new IllegalArgumentException("Invalid property assignment: " + p);
        }
        String key = p.substring(0, eq).trim();
        String val = p.substring(eq + 1).trim();
        props.put(key, val);
      }
    }

    return new BlockStateSpec(name, props);
  }

  @Override
  public String toString() {
    if (properties.isEmpty()) return name;
    StringBuilder sb = new StringBuilder();
    sb.append(name).append('[');
    boolean first = true;
    for (Map.Entry<String, String> e : properties.entrySet()) {
      if (!first) sb.append(',');
      first = false;
      sb.append(e.getKey()).append('=').append(e.getValue());
    }
    sb.append(']');
    return sb.toString();
  }
}

