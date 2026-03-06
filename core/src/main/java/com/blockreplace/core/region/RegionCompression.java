package com.blockreplace.core.region;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;

public enum RegionCompression {
  GZIP(1),
  ZLIB(2),
  UNCOMPRESSED(3),
  LZ4(4);

  private final int id;

  RegionCompression(int id) {
    this.id = id;
  }

  public int id() {
    return id;
  }

  public static RegionCompression fromId(int id) {
    for (RegionCompression c : values()) {
      if (c.id == id) return c;
    }
    throw new IllegalArgumentException("Unknown region compression id: " + id);
  }

  public byte[] decompress(byte[] payload) throws IOException {
    if (this == UNCOMPRESSED) return payload;
    try (InputStream in = wrapDecompressing(new ByteArrayInputStream(payload))) {
      return in.readAllBytes();
    }
  }

  public byte[] compress(byte[] uncompressed) throws IOException {
    if (this == UNCOMPRESSED) return uncompressed;
    ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(1024, uncompressed.length / 2));
    try (OutputStream out = wrapCompressing(baos)) {
      out.write(uncompressed);
    }
    return baos.toByteArray();
  }

  public InputStream wrapDecompressing(InputStream in) throws IOException {
    return switch (this) {
      case GZIP -> new GZIPInputStream(in);
      case ZLIB -> new InflaterInputStream(in);
      case UNCOMPRESSED -> in;
      case LZ4 -> new LZ4FrameInputStream(in);
    };
  }

  public OutputStream wrapCompressing(OutputStream out) throws IOException {
    return switch (this) {
      case GZIP -> new GZIPOutputStream(out);
      case ZLIB -> new DeflaterOutputStream(out);
      case UNCOMPRESSED -> out;
      case LZ4 -> new LZ4FrameOutputStream(out);
    };
  }
}

