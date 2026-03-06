package com.blockreplace.core.region;

import com.blockreplace.core.task.BlockStateSpec;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapping between legacy Pre-Anvil block id/metadata pairs and modern block state specs.
 *
 * <p>This is intentionally minimal and focused on common vanilla blocks from Minecraft
 * Beta 1.3–1.7. Unknown IDs are mapped to synthetic names but preserve their legacy id/meta
 * values via auxiliary tags attached by {@link ChunkNbtCodecPreAnvil}, so they can still be
 * round-tripped even if not recognised here.
 */
public final class PreAnvilBlockMapper {
  private static final Map<Integer, BlockStateSpec> ID_META_TO_SPEC = new HashMap<>();
  private static final Map<String, int[]> SPEC_NAME_TO_ID_META = new HashMap<>();

  static {
    // Very small core subset; can be extended over time.
    registerSimple(1, "minecraft:stone");
    registerSimple(2, "minecraft:grass_block");
    registerSimple(3, "minecraft:dirt");
    registerSimple(8, "minecraft:water");
    registerSimple(9, "minecraft:water"); // flowing water
    registerSimple(10, "minecraft:lava");
    registerSimple(11, "minecraft:lava"); // flowing lava
    registerSimple(12, "minecraft:sand");
    registerSimple(13, "minecraft:gravel");
    registerSimple(78, "minecraft:snow");
    registerSimple(80, "minecraft:snow_block");
    registerSimple(81, "minecraft:cactus");
    registerSimple(17, "minecraft:oak_log");
    registerSimple(18, "minecraft:oak_leaves");
    registerSimple(35, "minecraft:white_wool");

    // Leaves (block id 18) use the low 2 bits of meta for type (oak/spruce/birch/jungle)
    // and higher bits for decay flags. Normalise all 16 meta values to one of the four
    // canonical leaf types, ignoring decay flags at the block-state level.
    for (int meta = 0; meta < 16; meta++) {
      int typeBits = meta & 0x3;
      String name;
      switch (typeBits) {
        case 0:
          name = "minecraft:oak_leaves";
          break;
        case 1:
          name = "minecraft:spruce_leaves";
          break;
        case 2:
          name = "minecraft:birch_leaves";
          break;
        case 3:
        default:
          name = "minecraft:jungle_leaves";
          break;
      }
      BlockStateSpec spec = new BlockStateSpec(name, Map.of());
      int key = (18 << 8) | (meta & 0xFF);
      ID_META_TO_SPEC.put(key, spec);
    }

    // Map canonical leaf names back to base legacy meta values (decay flags cleared).
    SPEC_NAME_TO_ID_META.put("minecraft:oak_leaves", new int[] {18, 0});
    SPEC_NAME_TO_ID_META.put("minecraft:spruce_leaves", new int[] {18, 1});
    SPEC_NAME_TO_ID_META.put("minecraft:birch_leaves", new int[] {18, 2});
    SPEC_NAME_TO_ID_META.put("minecraft:jungle_leaves", new int[] {18, 3});
  }

  private PreAnvilBlockMapper() {}

  private static void registerSimple(int id, String name) {
    BlockStateSpec spec = new BlockStateSpec(name, Map.of());
    int key = (id << 8);
    ID_META_TO_SPEC.put(key, spec);
    SPEC_NAME_TO_ID_META.put(spec.name(), new int[] {id, 0});
  }

  /**
   * Maps legacy id/meta to a modern {@link BlockStateSpec}.
   *
   * <p>For unknown ids a synthetic block name is returned. Downstream tools can still target those
   * by name if needed.
   */
  public static BlockStateSpec toCanonical(int id, int meta) {
    int key = (id << 8) | (meta & 0xFF);
    BlockStateSpec spec = ID_META_TO_SPEC.get(key);
    if (spec != null) return spec;

    BlockStateSpec byId = ID_META_TO_SPEC.get(id << 8);
    if (byId != null) return byId;

    // Fallback synthetic name that keeps the numeric id visible.
    String synthetic = "minecraft:legacy_id_" + id;
    return new BlockStateSpec(synthetic, Map.of("meta", Integer.toString(meta & 0xF)));
  }

  /**
   * Maps a canonical block state back to a legacy id/meta pair.
   *
   * @return {@code int[]{id, meta}} if known, or {@code null} if the name is not recognised.
   */
  public static int[] toLegacy(BlockStateSpec state) {
    if (state == null) return null;
    String name = state.name();
    int[] mapped = SPEC_NAME_TO_ID_META.get(name);
    if (mapped != null) return mapped.clone();

    // Best-effort: try to parse synthetic legacy names back.
    if (name.startsWith("minecraft:legacy_id_")) {
      try {
        int id = Integer.parseInt(name.substring("minecraft:legacy_id_".length()));
        String metaStr = state.properties().getOrDefault("meta", "0");
        int meta = Integer.parseInt(metaStr);
        return new int[] {id, meta & 0xF};
      } catch (NumberFormatException ignored) {
        return null;
      }
    }

    return null;
  }
}

