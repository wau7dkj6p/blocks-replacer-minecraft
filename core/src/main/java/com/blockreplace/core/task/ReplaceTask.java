package com.blockreplace.core.task;

import java.util.Objects;
import java.util.UUID;

public record ReplaceTask(
    String id,
    String title,
    BlockStateSpec from,
    BlockStateSpec to,
    boolean matchByNameOnly,
    boolean enabled) {
  public ReplaceTask {
    if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
    title = title == null ? "" : title;
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
  }
}

