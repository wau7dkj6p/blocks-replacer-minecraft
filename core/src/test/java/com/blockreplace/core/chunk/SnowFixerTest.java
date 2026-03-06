package com.blockreplace.core.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blockreplace.core.nbt.NbtCompound;
import com.blockreplace.core.nbt.NbtList;
import com.blockreplace.core.nbt.NbtLongArray;
import com.blockreplace.core.nbt.NbtString;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class SnowFixerTest {

  @Test
  void fixesSnowyGrassInPalette() {
    NbtCompound section = new NbtCompound();
    NbtCompound blockStates = new NbtCompound();
    section.put("block_states", blockStates);

    NbtList palette = new NbtList();
    NbtCompound grassEntry = new NbtCompound();
    grassEntry.put("Name", new NbtString("minecraft:grass_block"));
    NbtCompound props = new NbtCompound();
    props.put("snowy", new NbtString("true"));
    grassEntry.put("Properties", props);
    palette.add(grassEntry);
    blockStates.put("palette", palette);

    // Single palette entry used for all 4096 blocks.
    blockStates.put("data", new NbtLongArray(new long[] {0L}));

    SectionReplaceResult r = SnowFixer.fixSectionSnowyGround(section);
    assertTrue(r.modified());
    assertEquals(1, r.paletteEntriesChanged());
    assertEquals(4096L, r.blocksAffected());

    NbtCompound updatedProps =
        ((NbtCompound) ((NbtCompound) ((NbtList) blockStates.getList("palette").orElseThrow()).get(0))
            .getCompound("Properties")
            .orElseThrow());
    assertTrue(updatedProps.get("snowy").isPresent());
    assertEquals("false", ((NbtString) updatedProps.get("snowy").get()).value());
  }

  @Test
  void doesNothingWhenNoSnowyProperty() {
    NbtCompound section = new NbtCompound();
    NbtCompound blockStates = new NbtCompound();
    section.put("block_states", blockStates);

    NbtList palette = new NbtList();
    NbtCompound grassEntry = new NbtCompound();
    grassEntry.put("Name", new NbtString("minecraft:grass_block"));
    palette.add(grassEntry);
    blockStates.put("palette", palette);

    blockStates.put("data", new NbtLongArray(new long[] {0L}));

    SectionReplaceResult r = SnowFixer.fixSectionSnowyGround(section);
    assertFalse(r.modified());
    assertEquals(0, r.paletteEntriesChanged());
    assertEquals(0L, r.blocksAffected());
  }
}

