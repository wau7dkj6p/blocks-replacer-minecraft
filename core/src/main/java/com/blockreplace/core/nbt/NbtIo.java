package com.blockreplace.core.nbt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class NbtIo {
  private NbtIo() {}

  public static NbtCompound readRootCompound(byte[] nbtBytes) throws IOException {
    try (var in = new DataInputStream(new ByteArrayInputStream(nbtBytes))) {
      NbtType type = NbtType.fromId(in.readUnsignedByte());
      if (type != NbtType.COMPOUND) {
        throw new IOException("Root tag is not a COMPOUND, got: " + type);
      }
      readString(in); // root name, ignored
      return readCompoundPayload(in);
    }
  }

  public static byte[] writeRootCompound(NbtCompound root) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
    try (var out = new DataOutputStream(baos)) {
      out.writeByte(NbtType.COMPOUND.id());
      writeString(out, "");
      writeCompoundPayload(out, root);
    }
    return baos.toByteArray();
  }

  public static NbtCompound readGzipFile(Path path) throws IOException {
    try (InputStream fis = Files.newInputStream(path);
        InputStream gis = new GZIPInputStream(fis);
        DataInputStream in = new DataInputStream(gis)) {
      NbtType type = NbtType.fromId(in.readUnsignedByte());
      if (type != NbtType.COMPOUND) {
        throw new IOException("Root tag is not a COMPOUND, got: " + type);
      }
      readString(in); // name ignored
      return readCompoundPayload(in);
    }
  }

  public static void writeGzipFile(Path path, NbtCompound root) throws IOException {
    Files.createDirectories(path.getParent());
    try (OutputStream fos = Files.newOutputStream(path);
        OutputStream gos = new GZIPOutputStream(fos);
        DataOutputStream out = new DataOutputStream(gos)) {
      out.writeByte(NbtType.COMPOUND.id());
      writeString(out, "");
      writeCompoundPayload(out, root);
    }
  }

  static NbtTag readUnnamedTag(DataInput in, NbtType type) throws IOException {
    return switch (type) {
      case END -> throw new IOException("Unexpected END in unnamed tag");
      case BYTE -> new NbtByte(in.readByte());
      case SHORT -> new NbtShort(in.readShort());
      case INT -> new NbtInt(in.readInt());
      case LONG -> new NbtLong(in.readLong());
      case FLOAT -> new NbtFloat(in.readFloat());
      case DOUBLE -> new NbtDouble(in.readDouble());
      case BYTE_ARRAY -> {
        int len = in.readInt();
        byte[] b = new byte[len];
        in.readFully(b);
        yield new NbtByteArray(b);
      }
      case STRING -> new NbtString(readString(in));
      case LIST -> {
        NbtType elementType = NbtType.fromId(in.readUnsignedByte());
        int len = in.readInt();
        ArrayList<NbtTag> elements = new ArrayList<>(Math.max(0, len));
        for (int i = 0; i < len; i++) {
          elements.add(readUnnamedTag(in, elementType));
        }
        yield new NbtList(elementType, elements);
      }
      case COMPOUND -> readCompoundPayload(in);
      case INT_ARRAY -> {
        int len = in.readInt();
        int[] arr = new int[len];
        for (int i = 0; i < len; i++) arr[i] = in.readInt();
        yield new NbtIntArray(arr);
      }
      case LONG_ARRAY -> {
        int len = in.readInt();
        long[] arr = new long[len];
        for (int i = 0; i < len; i++) arr[i] = in.readLong();
        yield new NbtLongArray(arr);
      }
    };
  }

  static void writeUnnamedTag(DataOutput out, NbtTag tag) throws IOException {
    switch (tag.type()) {
      case END -> throw new IOException("Unexpected END tag value");
      case BYTE -> out.writeByte(((NbtByte) tag).value());
      case SHORT -> out.writeShort(((NbtShort) tag).value());
      case INT -> out.writeInt(((NbtInt) tag).value());
      case LONG -> out.writeLong(((NbtLong) tag).value());
      case FLOAT -> out.writeFloat(((NbtFloat) tag).value());
      case DOUBLE -> out.writeDouble(((NbtDouble) tag).value());
      case BYTE_ARRAY -> {
        byte[] b = ((NbtByteArray) tag).value();
        out.writeInt(b.length);
        out.write(b);
      }
      case STRING -> writeString(out, ((NbtString) tag).value());
      case LIST -> {
        NbtList list = (NbtList) tag;
        out.writeByte(list.elementType().id());
        out.writeInt(list.size());
        for (NbtTag el : list.elements()) {
          writeUnnamedTag(out, el);
        }
      }
      case COMPOUND -> writeCompoundPayload(out, (NbtCompound) tag);
      case INT_ARRAY -> {
        int[] arr = ((NbtIntArray) tag).value();
        out.writeInt(arr.length);
        for (int v : arr) out.writeInt(v);
      }
      case LONG_ARRAY -> {
        long[] arr = ((NbtLongArray) tag).value();
        out.writeInt(arr.length);
        for (long v : arr) out.writeLong(v);
      }
    }
  }

  static NbtCompound readCompoundPayload(DataInput in) throws IOException {
    NbtCompound c = new NbtCompound();
    while (true) {
      int typeId;
      try {
        typeId = in.readUnsignedByte();
      } catch (EOFException e) {
        throw new IOException("Unexpected EOF while reading compound", e);
      }
      NbtType type = NbtType.fromId(typeId);
      if (type == NbtType.END) {
        return c;
      }
      String name = readString(in);
      NbtTag payload = readUnnamedTag(in, type);
      c.put(name, payload);
    }
  }

  static void writeCompoundPayload(DataOutput out, NbtCompound compound) throws IOException {
    for (Map.Entry<String, NbtTag> e : compound.values().entrySet()) {
      out.writeByte(e.getValue().type().id());
      writeString(out, e.getKey());
      writeUnnamedTag(out, e.getValue());
    }
    out.writeByte(NbtType.END.id());
  }

  static String readString(DataInput in) throws IOException {
    int len = in.readUnsignedShort();
    if (len == 0) return "";
    byte[] b = new byte[len];
    in.readFully(b);
    return new String(b, StandardCharsets.UTF_8);
  }

  static void writeString(DataOutput out, String s) throws IOException {
    if (s == null) s = "";
    byte[] b = s.getBytes(StandardCharsets.UTF_8);
    if (b.length > 65535) throw new IOException("NBT string too long: " + b.length);
    out.writeShort(b.length);
    out.write(b);
  }

  public static NbtCompound readRootCompound(InputStream in) throws IOException {
    try (var dis = new DataInputStream(in)) {
      NbtType type = NbtType.fromId(dis.readUnsignedByte());
      if (type != NbtType.COMPOUND) {
        throw new IOException("Root tag is not a COMPOUND, got: " + type);
      }
      readString(dis); // root name ignored
      return readCompoundPayload(dis);
    }
  }

  public static void writeRootCompound(OutputStream out, NbtCompound root) throws IOException {
    try (var dos = new DataOutputStream(out)) {
      dos.writeByte(NbtType.COMPOUND.id());
      writeString(dos, "");
      writeCompoundPayload(dos, root);
    }
  }
}

