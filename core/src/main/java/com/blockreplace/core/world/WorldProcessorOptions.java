package com.blockreplace.core.world;

import java.nio.file.Path;
import java.util.EnumSet;

public record WorldProcessorOptions(
    Path worldRoot,
    EnumSet<WorldDimension> dimensions,
    boolean dryRun,
    boolean backup,
    boolean allowUnknownBlocks,
    boolean saveState,
    boolean resumeFromState,
    boolean fixSnowyGround,
    boolean fixLight) {}

