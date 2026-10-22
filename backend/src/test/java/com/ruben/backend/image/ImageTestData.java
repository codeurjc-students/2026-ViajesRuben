package com.ruben.backend.image;

import java.util.Arrays;
import java.util.Random;

/** Byte arrays that start with the signature of each accepted image type. */
public final class ImageTestData {

  private static final byte[] PNG_SIGNATURE = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };
  private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

  private ImageTestData() {}

  public static byte[] png(int size) {
    return withSignature(PNG_SIGNATURE, size);
  }

  public static byte[] jpeg(int size) {
    return withSignature(JPEG_SIGNATURE, size);
  }

  /** "RIFF", a 4-byte size and "WEBP", followed by filler. */
  public static byte[] webp(int size) {
    byte[] data = new byte[size];
    byte[] header = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    System.arraycopy(header, 0, data, 0, header.length);
    return data;
  }

  public static byte[] text() {
    return "this is not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  private static byte[] withSignature(byte[] signature, int size) {
    byte[] data = new byte[Math.max(size, signature.length)];
    new Random(42).nextBytes(data);
    return concat(signature, Arrays.copyOfRange(data, signature.length, data.length));
  }

  private static byte[] concat(byte[] first, byte[] second) {
    byte[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }
}
