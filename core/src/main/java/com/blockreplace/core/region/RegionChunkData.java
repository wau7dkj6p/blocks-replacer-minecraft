package com.blockreplace.core.region;

public record RegionChunkData(
    RegionCompression compression,
    byte[] compressedPayload,
    int timestampSeconds) {}

