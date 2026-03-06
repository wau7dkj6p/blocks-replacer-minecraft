package com.blockreplace.core.blockdb;

import com.blockreplace.core.task.BlockStateSpec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class BlockDatabase {
  private static final String BUNDLED_BLOCKS_JSON = "/minecraft-data/pc-1.21.1/blocks.json";

  private final Map<String, BlockDef> blocksByName;
  private final List<String> sortedBlockNames;

  private BlockDatabase(Map<String, BlockDef> blocksByName) {
    this.blocksByName = Map.copyOf(blocksByName);
    ArrayList<String> names = new ArrayList<>(blocksByName.keySet());
    names.sort(Comparator.naturalOrder());
    this.sortedBlockNames = Collections.unmodifiableList(names);
  }

  public static BlockDatabase loadBundled() throws IOException {
    ObjectMapper om =
        new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try (InputStream in = BlockDatabase.class.getResourceAsStream(BUNDLED_BLOCKS_JSON)) {
      if (in == null) {
        throw new IOException("Bundled blocks.json not found: " + BUNDLED_BLOCKS_JSON);
      }
      List<BlockJson> blocks = om.readValue(in, new TypeReference<>() {});
      HashMap<String, BlockDef> map = new HashMap<>(blocks.size() * 2);
      for (BlockJson b : blocks) {
        if (b == null || b.name == null || b.name.isBlank()) continue;
        String rl = BlockStateSpec.normalizeName(b.name);

        HashMap<String, PropertyDef> props = new HashMap<>();
        if (b.states != null) {
          for (StateJson s : b.states) {
            if (s == null || s.name == null || s.name.isBlank()) continue;
            ArrayList<String> values = new ArrayList<>();
            if ("bool".equalsIgnoreCase(s.type)) {
              values.add("false");
              values.add("true");
            } else if (s.values != null) {
              values.addAll(s.values);
            }
            props.put(s.name, new PropertyDef(s.name, s.type == null ? "" : s.type, List.copyOf(values)));
          }
        }

        int emit = b.emitLight;
        int filter = b.filterLight;
        if (filter < 0 || filter > 15) filter = 15;
        if (emit < 0 || emit > 15) emit = 0;
        map.put(
            rl,
            new BlockDef(
                rl,
                b.displayName == null ? b.name : b.displayName,
                Map.copyOf(props),
                emit,
                filter));
      }
      return new BlockDatabase(map);
    }
  }

  public List<String> blockNames() {
    return sortedBlockNames;
  }

  public Optional<BlockDef> block(String name) {
    return Optional.ofNullable(blocksByName.get(BlockStateSpec.normalizeName(name)));
  }

  public void validate(BlockStateSpec spec, boolean isToSpec) {
    Objects.requireNonNull(spec, "spec");
    BlockDef b =
        block(spec.name())
            .orElseThrow(() -> new IllegalArgumentException("Unknown block: " + spec.name()));

    for (Map.Entry<String, String> e : spec.properties().entrySet()) {
      String key = e.getKey();
      String val = e.getValue();

      PropertyDef pd =
          b.properties()
              .get(key);
      if (pd == null) {
        throw new IllegalArgumentException("Block " + b.name() + " has no property: " + key);
      }

      if (BlockStateSpec.ALL.equalsIgnoreCase(val)) {
        if (!isToSpec) continue;
        // TO-spec copy: allowed; if property exists in target it can be copied.
        continue;
      }

      if (!pd.allowedValues().isEmpty() && !pd.allowedValues().contains(val)) {
        throw new IllegalArgumentException(
            "Invalid value for " + b.name() + "[" + key + "]: " + val + " (allowed: " + pd.allowedValues() + ")");
      }
    }
  }

  // --- JSON DTOs (minecraft-data blocks.json)
  static final class BlockJson {
    public int id;
    public String name;
    public String displayName;
    public List<StateJson> states;
    public int emitLight = 0;
    public int filterLight = 15;
  }

  static final class StateJson {
    public String name;
    public String type;
    public int num_values;
    public List<String> values;
  }
}

