package com.blockreplace.core.chunk;

import com.blockreplace.core.blockdb.BlockDatabase;
import com.blockreplace.core.nbt.NbtCompound;
import com.blockreplace.core.nbt.NbtList;
import com.blockreplace.core.task.BlockStateSpec;
import com.blockreplace.core.world.WorldDimension;
import java.util.Objects;

/**
 * Recalculates block light and sky light for chunk sections so that lighting matches current
 * blocks (e.g. after mass block replacement). Uses emitLight/filterLight from BlockDatabase.
 * Light is computed per-section only (no cross-chunk or cross-section propagation).
 */
public final class LightFixer {
  private static final String TAG_BLOCK_LIGHT = "block_light";
  private static final String TAG_SKY_LIGHT = "sky_light";
  private static final int SECTION_SIZE = 16 * 16 * 16; // 4096
  private static final int NIBBLE_BYTES = SECTION_SIZE / 2; // 2048

  private LightFixer() {}

  /**
   * Recalculates and writes BlockLight and SkyLight for all sections in the chunk.
   * Only processes Anvil-style sections that have block_states; creates light arrays if missing.
   */
  public static void fixChunk(
      NbtCompound chunkRoot, BlockDatabase blockDb, WorldDimension dimension) {
    Objects.requireNonNull(chunkRoot, "chunkRoot");
    Objects.requireNonNull(blockDb, "blockDb");
    Objects.requireNonNull(dimension, "dimension");

    NbtList sections = chunkRoot.getList("sections").orElse(null);
    if (sections == null) return;

    boolean overworld = dimension == WorldDimension.OVERWORLD;

    for (int i = 0; i < sections.size(); i++) {
      if (!(sections.get(i) instanceof NbtCompound section)) continue;
      fixSection(section, blockDb, overworld);
    }
  }

  private static void fixSection(
      NbtCompound section, BlockDatabase blockDb, boolean doSkyLight) {
    NbtCompound blockStates = section.getCompound("block_states").orElse(null);
    if (blockStates == null) return;

    NbtList palette = blockStates.getList("palette").orElse(null);
    int paletteSize = palette == null ? 0 : palette.size();
    int bits = PaletteCodec.bitsPerEntry(paletteSize);
    long[] data = blockStates.getLongArray("data").orElse(null);

    int[] paletteIndices;
    if (bits == 0 || data == null) {
      paletteIndices = new int[SECTION_SIZE];
    } else {
      paletteIndices = PaletteCodec.decode4096(bits, data);
    }

    // Resolve block names and light properties per palette index
    int[] emitLight = new int[SECTION_SIZE];
    int[] filterLight = new int[SECTION_SIZE];
    for (int i = 0; i < SECTION_SIZE; i++) {
      final int idx = i;
      int pi = paletteIndices[i];
      if (pi < 0 || pi >= paletteSize) continue;
      if (!(palette.get(pi) instanceof NbtCompound entry)) continue;
      String name = entry.getString("Name").orElse(null);
      if (name == null || name.isBlank()) continue;
      name = BlockStateSpec.normalizeName(name);
      blockDb
          .block(name)
          .ifPresent(
              def -> {
                emitLight[idx] = Math.max(0, Math.min(15, def.emitLight()));
                filterLight[idx] = Math.max(0, Math.min(15, def.filterLight()));
              });
    }

    // Block light: sources from emitLight, then propagate
    int[] blockLight = new int[SECTION_SIZE];
    for (int i = 0; i < SECTION_SIZE; i++) {
      blockLight[i] = emitLight[i];
    }
    propagateBlockLight(blockLight, filterLight);

    // Sky light: only Overworld; top of section (y=15) = 15, propagate down
    int[] skyLight = new int[SECTION_SIZE];
    if (doSkyLight) {
      for (int z = 0; z < 16; z++) {
        for (int x = 0; x < 16; x++) {
          skyLight[15 * 256 + z * 16 + x] = 15;
        }
      }
      propagateSkyLight(skyLight, filterLight);
    }

    section.put(TAG_BLOCK_LIGHT, nibblesToBytes(blockLight));
    section.put(TAG_SKY_LIGHT, nibblesToBytes(skyLight));
  }

  private static void propagateBlockLight(int[] light, int[] opacity) {
    boolean changed = true;
    int iters = 0;
    int maxIters = 32;
    while (changed && iters < maxIters) {
      changed = false;
      iters++;
      for (int y = 0; y < 16; y++) {
        for (int z = 0; z < 16; z++) {
          for (int x = 0; x < 16; x++) {
            int pos = y * 256 + z * 16 + x;
            int current = light[pos];
            if (current <= 0) continue;
            // +X, -X, +Y, -Y, +Z, -Z; spread = current - 1 - opacity[neighbor]
            if (x < 15) {
              int n = y * 256 + z * 16 + (x + 1);
              int spread = current - 1 - opacity[n];
              if (spread > 0 && light[n] < spread) {
                light[n] = spread;
                changed = true;
              }
            }
            if (x > 0) {
              int n = y * 256 + z * 16 + (x - 1);
              int spread = current - 1 - opacity[n];
              if (spread > 0 && light[n] < spread) {
                light[n] = spread;
                changed = true;
              }
            }
            if (y < 15) {
              int n = (y + 1) * 256 + z * 16 + x;
              int spread = current - 1 - opacity[n];
              if (spread > 0 && light[n] < spread) {
                light[n] = spread;
                changed = true;
              }
            }
            if (y > 0) {
              int n = (y - 1) * 256 + z * 16 + x;
              int spread = current - 1 - opacity[n];
              if (spread > 0 && light[n] < spread) {
                light[n] = spread;
                changed = true;
              }
            }
            if (z < 15) {
              int n = y * 256 + (z + 1) * 16 + x;
              int spread = current - 1 - opacity[n];
              if (spread > 0 && light[n] < spread) {
                light[n] = spread;
                changed = true;
              }
            }
            if (z > 0) {
              int n = y * 256 + (z - 1) * 16 + x;
              int spread = current - 1 - opacity[n];
              if (spread > 0 && light[n] < spread) {
                light[n] = spread;
                changed = true;
              }
            }
          }
        }
      }
    }
  }

  private static void propagateSkyLight(int[] light, int[] opacity) {
    boolean changed = true;
    int iters = 0;
    int maxIters = 32;
    while (changed && iters < maxIters) {
      changed = false;
      iters++;
      for (int y = 15; y >= 0; y--) {
        for (int z = 0; z < 16; z++) {
          for (int x = 0; x < 16; x++) {
            int pos = y * 256 + z * 16 + x;
            int current = light[pos];
            if (current <= 0) continue;
            if (x < 15) {
              int n = y * 256 + z * 16 + (x + 1);
              int spread = current - 1 - opacity[n];
              if (spread > 0 && light[n] < spread) {
                light[n] = spread;
                changed = true;
              }
            }
            if (x > 0) {
              int n = y * 256 + z * 16 + (x - 1);
              int spread = current - 1 - opacity[n];
              if (spread > 0 && light[n] < spread) {
                light[n] = spread;
                changed = true;
              }
            }
            if (y < 15) {
              int n = (y + 1) * 256 + z * 16 + x;
              int spread = current - 1 - opacity[n];
              if (spread > 0 && light[n] < spread) {
                light[n] = spread;
                changed = true;
              }
            }
            if (y > 0) {
              int n = (y - 1) * 256 + z * 16 + x;
              int spread = current - 1 - opacity[n];
              if (spread > 0 && light[n] < spread) {
                light[n] = spread;
                changed = true;
              }
            }
            if (z < 15) {
              int n = y * 256 + (z + 1) * 16 + x;
              int spread = current - 1 - opacity[n];
              if (spread > 0 && light[n] < spread) {
                light[n] = spread;
                changed = true;
              }
            }
            if (z > 0) {
              int n = y * 256 + (z - 1) * 16 + x;
              int spread = current - 1 - opacity[n];
              if (spread > 0 && light[n] < spread) {
                light[n] = spread;
                changed = true;
              }
            }
          }
        }
      }
    }
  }

  private static byte[] nibblesToBytes(int[] light) {
    byte[] out = new byte[NIBBLE_BYTES];
    for (int i = 0; i < NIBBLE_BYTES; i++) {
      int lo = light[2 * i] & 0x0F;
      int hi = light[2 * i + 1] & 0x0F;
      out[i] = (byte) (lo | (hi << 4));
    }
    return out;
  }
}
