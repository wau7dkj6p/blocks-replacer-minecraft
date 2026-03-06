package com.blockreplace.core.region;

import com.blockreplace.core.nbt.NbtByteArray;
import com.blockreplace.core.nbt.NbtCompound;
import com.blockreplace.core.nbt.NbtInt;
import com.blockreplace.core.nbt.NbtList;
import com.blockreplace.core.nbt.NbtLongArray;
import com.blockreplace.core.nbt.NbtType;
import com.blockreplace.core.nbt.NbtIo;
import com.blockreplace.core.task.BlockStateSpec;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Codec for legacy McRegion (Pre-Anvil) chunks.
 *
 * <p>Pre-Anvil chunks store blocks as flat {@code Blocks/Data/Add} arrays. This codec translates
 * that layout to the canonical Anvil-like representation used by the rest of the pipeline:
 *
 * <ul>
 *   <li>root compound with {@code sections} list of section compounds
 *   <li>each section has {@code Y} and {@code block_states{palette,data}}
 * </ul>
 *
 * <p>During decoding we also attach auxiliary legacy fields ({@code _legacy_id} and
 * {@code _legacy_meta}) to palette entries so that unknown/mod blocks can be round-tripped without
 * losing information when re-encoding back to Pre-Anvil.
 */
public final class ChunkNbtCodecPreAnvil {
  private static final int CHUNK_WIDTH = 16;
  private static final int CHUNK_LENGTH = 16;
  private static final int CHUNK_HEIGHT = 128; // Beta 1.3–1.7
  private static final int BLOCKS_PER_SECTION = CHUNK_WIDTH * CHUNK_LENGTH * 16; // 4096

  private ChunkNbtCodecPreAnvil() {}

  public static NbtCompound decodeToCanonical(RegionChunkData in) throws IOException {
    byte[] uncompressed = in.compression().decompress(in.compressedPayload());
    NbtCompound root = NbtIo.readRootCompound(uncompressed);
    NbtCompound level = root.getCompound("Level").orElseThrow(() ->
        new IOException("Pre-Anvil chunk missing Level compound"));

    byte[] blocks = asByteArray(level, "Blocks", CHUNK_WIDTH * CHUNK_LENGTH * CHUNK_HEIGHT);
    byte[] data = asByteArray(level, "Data", (CHUNK_WIDTH * CHUNK_LENGTH * CHUNK_HEIGHT) / 2);
    byte[] add = level.get("Add")
        .filter(t -> t instanceof NbtByteArray)
        .map(t -> ((NbtByteArray) t).value())
        .orElse(null);

    NbtCompound canonical = new NbtCompound();

    // Copy some basic fields commonly used by downstream logic (positions, heightmap, etc.).
    copyIfPresent(level, canonical, "xPos");
    copyIfPresent(level, canonical, "zPos");
    copyIfPresent(level, canonical, "HeightMap");

    // Convert TileEntities -> block_entities for BlockEntityFixer.
    level.getList("TileEntities")
        .ifPresent(list -> canonical.put("block_entities", list));

    NbtList sections = NbtList.of(NbtType.COMPOUND);

    for (int sectionY = 0; sectionY < CHUNK_HEIGHT / 16; sectionY++) {
      // Build palette per section: key is (id, meta) packed into int.
      Map<Integer, Integer> paletteIndexByKey = new HashMap<>();
      NbtList palette = NbtList.of(NbtType.COMPOUND);
      int[] indices = new int[BLOCKS_PER_SECTION];
      int paletteSize = 0;

      int baseY = sectionY * 16;
      for (int y = 0; y < 16; y++) {
        int globalY = baseY + y;
        if (globalY >= CHUNK_HEIGHT) continue;
        for (int z = 0; z < CHUNK_LENGTH; z++) {
          for (int x = 0; x < CHUNK_WIDTH; x++) {
            int globalIndex = index(x, globalY, z);
            int id = blocks[globalIndex] & 0xFF;
            int meta = getNibble(data, globalIndex);
            if (add != null) {
              int high = getNibble(add, globalIndex);
              id |= (high << 8);
            }
            int key = (id << 8) | (meta & 0xFF);
            Integer paletteIndex = paletteIndexByKey.get(key);
            if (paletteIndex == null) {
              BlockStateSpec spec = PreAnvilBlockMapper.toCanonical(id, meta);
              NbtCompound entry = new NbtCompound();
              entry.put("Name", new com.blockreplace.core.nbt.NbtString(spec.name()));
              if (!spec.properties().isEmpty()) {
                NbtCompound props = new NbtCompound();
                for (Map.Entry<String, String> e : spec.properties().entrySet()) {
                  props.put(e.getKey(), new com.blockreplace.core.nbt.NbtString(e.getValue()));
                }
                entry.put("Properties", props);
              }
              entry.put("_legacy_id", new NbtInt(id));
              entry.put("_legacy_meta", new NbtInt(meta));
              palette.add(entry);
              paletteIndex = paletteSize;
              paletteIndexByKey.put(key, paletteSize);
              paletteSize++;
            }
            int localIndex = y * (CHUNK_WIDTH * CHUNK_LENGTH) + z * CHUNK_WIDTH + x;
            indices[localIndex] = paletteIndex;
          }
        }
      }

      if (paletteSize == 0) {
        continue; // section is entirely empty, skip
      }

      int bits = com.blockreplace.core.chunk.PaletteCodec.bitsPerEntry(paletteSize);
      long[] packed;
      if (bits == 0) {
        // All entries are the same, encode as zero with implicit palette index 0.
        packed = new long[0];
      } else {
        packed = encode4096(bits, indices);
      }

      NbtCompound blockStates = new NbtCompound();
      blockStates.put("palette", palette);
      blockStates.put("data", new NbtLongArray(packed));

      NbtCompound section = new NbtCompound();
      section.put("Y", new NbtInt(sectionY));
      section.put("block_states", blockStates);

      sections.add(section);
    }

    canonical.put("sections", sections);
    return canonical;
  }

  public static RegionChunkData encodeFromCanonical(NbtCompound canonical, RegionChunkData original)
      throws IOException {
    // Rebuild Blocks/Data/Add arrays from canonical sections.
    byte[] blocks = new byte[CHUNK_WIDTH * CHUNK_LENGTH * CHUNK_HEIGHT];
    byte[] data = new byte[(CHUNK_WIDTH * CHUNK_LENGTH * CHUNK_HEIGHT) / 2];
    byte[] add = null; // lazily allocated when needed

    NbtList sections = canonical.getList("sections").orElse(NbtList.of(NbtType.COMPOUND));

    for (int i = 0; i < sections.size(); i++) {
      if (!(sections.get(i) instanceof NbtCompound section)) continue;
      int sectionY = section.getInt("Y").orElse(0);
      if (sectionY < 0 || sectionY >= CHUNK_HEIGHT / 16) continue;

      NbtCompound blockStates = section.getCompound("block_states").orElse(null);
      if (blockStates == null) continue;
      NbtList palette = blockStates.getList("palette").orElse(null);
      if (palette == null || palette.size() == 0) continue;
      long[] packed = blockStates.getLongArray("data").orElse(new long[0]);

      int paletteSize = palette.size();
      int bits = com.blockreplace.core.chunk.PaletteCodec.bitsPerEntry(paletteSize);
      int[] indices;
      if (bits == 0) {
        indices = new int[BLOCKS_PER_SECTION];
      } else {
        indices = com.blockreplace.core.chunk.PaletteCodec.decode4096(bits, packed);
      }

      int baseY = sectionY * 16;
      for (int y = 0; y < 16; y++) {
        int globalY = baseY + y;
        if (globalY >= CHUNK_HEIGHT) continue;
        for (int z = 0; z < CHUNK_LENGTH; z++) {
          for (int x = 0; x < CHUNK_WIDTH; x++) {
            int localIndex = y * (CHUNK_WIDTH * CHUNK_LENGTH) + z * CHUNK_WIDTH + x;
            int paletteIndex = indices[localIndex];
            if (paletteIndex < 0 || paletteIndex >= paletteSize) continue;
            if (!(palette.get(paletteIndex) instanceof NbtCompound entry)) continue;

            // Prefer mapping by Name/Properties, but allow fallback to stored legacy id/meta.
            String name =
                entry
                    .getString("Name")
                    .orElseThrow(() -> new IOException("Palette entry missing Name"));
            Map<String, String> props = Map.of();
            NbtCompound propsTag = entry.getCompound("Properties").orElse(null);
            if (propsTag != null && propsTag.size() > 0) {
              HashMap<String, String> m = new HashMap<>();
              for (var e : propsTag.values().entrySet()) {
                if (e.getValue() instanceof com.blockreplace.core.nbt.NbtString s) {
                  m.put(e.getKey(), s.value());
                } else {
                  m.put(e.getKey(), e.getValue().toString());
                }
              }
              props = m;
            }
            BlockStateSpec spec = new BlockStateSpec(name, props);
            int id;
            int meta;
            int[] legacy = PreAnvilBlockMapper.toLegacy(spec);
            if (legacy != null) {
              id = legacy[0];
              meta = legacy[1];
            } else {
              id = entry.getInt("_legacy_id").orElse(0);
              meta = entry.getInt("_legacy_meta").orElse(0);
            }

            int globalIndex = index(x, globalY, z);
            blocks[globalIndex] = (byte) (id & 0xFF);
            setNibble(data, globalIndex, meta & 0xF);
            int high = (id >>> 8) & 0xF;
            if (high != 0) {
              if (add == null) {
                add = new byte[data.length];
              }
              setNibble(add, globalIndex, high);
            }
          }
        }
      }
    }

    // Rebuild Level compound, preserving as much as possible from original.
    byte[] originalUncompressed = original.compression().decompress(original.compressedPayload());
    NbtCompound originalRoot = NbtIo.readRootCompound(originalUncompressed);
    NbtCompound originalLevel =
        originalRoot.getCompound("Level").orElseGet(NbtCompound::new);

    NbtCompound newLevel = new NbtCompound();
    // Copy all existing tags except those we overwrite.
    for (var e : originalLevel.values().entrySet()) {
      String key = e.getKey();
      if (key.equals("Blocks")
          || key.equals("Data")
          || key.equals("Add")
          || key.equals("TileEntities")) {
        continue;
      }
      newLevel.put(key, e.getValue());
    }

    newLevel.put("Blocks", new NbtByteArray(blocks));
    newLevel.put("Data", new NbtByteArray(data));
    if (add != null) {
      newLevel.put("Add", new NbtByteArray(add));
    }

    // Map canonical block_entities back to TileEntities.
    canonical
        .getList("block_entities")
        .ifPresent(list -> newLevel.put("TileEntities", list));

    NbtCompound newRoot = new NbtCompound();
    newRoot.put("Level", newLevel);

    byte[] rebuilt = NbtIo.writeRootCompound(newRoot);
    byte[] compressed = original.compression().compress(rebuilt);
    return new RegionChunkData(original.compression(), compressed, RegionFile.nowUnixSeconds());
  }

  private static long[] encode4096(int bitsPerEntry, int[] indices) {
    if (bitsPerEntry == 0) {
      return new long[0];
    }
    long mask = (1L << bitsPerEntry) - 1L;
    int valuesPerLong = 64 / bitsPerEntry;
    int longCount = (BLOCKS_PER_SECTION + valuesPerLong - 1) / valuesPerLong;
    long[] data = new long[longCount];
    for (int i = 0; i < BLOCKS_PER_SECTION; i++) {
      int value = indices[i];
      int longIndex = i / valuesPerLong;
      int offset = (i % valuesPerLong) * bitsPerEntry;
      data[longIndex] |= ((long) value & mask) << offset;
    }
    return data;
  }

  private static byte[] asByteArray(NbtCompound level, String key, int expectedLength)
      throws IOException {
    return level
        .get(key)
        .filter(t -> t instanceof NbtByteArray)
        .map(t -> ((NbtByteArray) t).value())
        .filter(arr -> arr.length == expectedLength)
        .orElseThrow(
            () ->
                new IOException(
                    "Pre-Anvil chunk missing or invalid " + key + " array, expected length "
                        + expectedLength));
  }

  private static int index(int x, int y, int z) {
    return (y * CHUNK_LENGTH + z) * CHUNK_WIDTH + x;
  }

  private static int getNibble(byte[] arr, int index) {
    int i = index >> 1;
    int b = arr[i] & 0xFF;
    if ((index & 1) == 0) {
      return b & 0x0F;
    } else {
      return (b >>> 4) & 0x0F;
    }
  }

  private static void setNibble(byte[] arr, int index, int value) {
    int i = index >> 1;
    int b = arr[i] & 0xFF;
    if ((index & 1) == 0) {
      b = (b & 0xF0) | (value & 0x0F);
    } else {
      b = (b & 0x0F) | ((value & 0x0F) << 4);
    }
    arr[i] = (byte) b;
  }

  private static void copyIfPresent(NbtCompound src, NbtCompound dst, String key) {
    src.get(key).ifPresent(tag -> dst.put(key, tag));
  }
}

