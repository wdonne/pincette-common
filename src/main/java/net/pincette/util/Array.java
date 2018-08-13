package net.pincette.util;

/** @author Werner Donn\u00e9 */
public class Array {
  private Array() {}

  public static <T> T[] append(final T[] array, final T object) {
    final T[] newArray = newArray(array, array.length + 1);

    System.arraycopy(array, 0, newArray, 0, array.length);
    newArray[newArray.length - 1] = object;

    return newArray;
  }

  public static boolean[] append(final boolean[] array, final boolean b) {
    final boolean[] newArray = new boolean[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, array.length);
    newArray[newArray.length - 1] = b;

    return newArray;
  }

  public static byte[] append(final byte[] array, final byte b) {
    final byte[] newArray = new byte[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, array.length);
    newArray[newArray.length - 1] = b;

    return newArray;
  }

  public static char[] append(final char[] array, final char c) {
    final char[] newArray = new char[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, array.length);
    newArray[newArray.length - 1] = c;

    return newArray;
  }

  public static short[] append(final short[] array, final short s) {
    final short[] newArray = new short[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, array.length);
    newArray[newArray.length - 1] = s;

    return newArray;
  }

  public static int[] append(final int[] array, final int i) {
    final int[] newArray = new int[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, array.length);
    newArray[newArray.length - 1] = i;

    return newArray;
  }

  public static long[] append(final long[] array, final long l) {
    final long[] newArray = new long[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, array.length);
    newArray[newArray.length - 1] = l;

    return newArray;
  }

  public static float[] append(final float[] array, final float f) {
    final float[] newArray = new float[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, array.length);
    newArray[newArray.length - 1] = f;

    return newArray;
  }

  public static double[] append(final double[] array, final double d) {
    final double[] newArray = new double[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, array.length);
    newArray[newArray.length - 1] = d;

    return newArray;
  }

  public static <T> T[] append(final T[] array, final T[] objects) {
    final T[] newArray = newArray(array, array.length + objects.length);

    System.arraycopy(array, 0, newArray, 0, array.length);
    System.arraycopy(objects, 0, newArray, array.length, objects.length);

    return newArray;
  }

  public static boolean[] append(final boolean[] array, final boolean[] booleans) {
    final boolean[] newArray = new boolean[array.length + booleans.length];

    System.arraycopy(array, 0, newArray, 0, array.length);
    System.arraycopy(booleans, 0, newArray, array.length, booleans.length);

    return newArray;
  }

  public static byte[] append(final byte[] array, final byte[] bytes) {
    final byte[] newArray = new byte[array.length + bytes.length];

    System.arraycopy(array, 0, newArray, 0, array.length);
    System.arraycopy(bytes, 0, newArray, array.length, bytes.length);

    return newArray;
  }

  public static char[] append(final char[] array, final char[] chars) {
    final char[] newArray = new char[array.length + chars.length];

    System.arraycopy(array, 0, newArray, 0, array.length);
    System.arraycopy(chars, 0, newArray, array.length, chars.length);

    return newArray;
  }

  public static short[] append(final short[] array, final short[] shorts) {
    final short[] newArray = new short[array.length + shorts.length];

    System.arraycopy(array, 0, newArray, 0, array.length);
    System.arraycopy(shorts, 0, newArray, array.length, shorts.length);

    return newArray;
  }

  public static int[] append(final int[] array, final int[] ints) {
    final int[] newArray = new int[array.length + ints.length];

    System.arraycopy(array, 0, newArray, 0, array.length);
    System.arraycopy(ints, 0, newArray, array.length, ints.length);

    return newArray;
  }

  public static long[] append(final long[] array, final long[] longs) {
    final long[] newArray = new long[array.length + longs.length];

    System.arraycopy(array, 0, newArray, 0, array.length);
    System.arraycopy(longs, 0, newArray, array.length, longs.length);

    return newArray;
  }

  public static float[] append(final float[] array, final float[] floats) {
    final float[] newArray = new float[array.length + floats.length];

    System.arraycopy(array, 0, newArray, 0, array.length);
    System.arraycopy(floats, 0, newArray, array.length, floats.length);

    return newArray;
  }

  public static double[] append(final double[] array, final double[] doubles) {
    final double[] newArray = new double[array.length + doubles.length];

    System.arraycopy(array, 0, newArray, 0, array.length);
    System.arraycopy(doubles, 0, newArray, array.length, doubles.length);

    return newArray;
  }

  public static boolean hasPrefix(final Object[] array1, final Object[] array2) {
    return hasSlice(array1, array2, 0, array2.length);
  }

  public static boolean hasPrefix(final boolean[] array1, final boolean[] array2) {
    return hasSlice(array1, array2, 0, array2.length);
  }

  public static boolean hasPrefix(final byte[] array1, final byte[] array2) {
    return hasSlice(array1, array2, 0, array2.length);
  }

  public static boolean hasPrefix(final char[] array1, final char[] array2) {
    return hasSlice(array1, array2, 0, array2.length);
  }

  public static boolean hasPrefix(final short[] array1, final short[] array2) {
    return hasSlice(array1, array2, 0, array2.length);
  }

  public static boolean hasPrefix(final int[] array1, final int[] array2) {
    return hasSlice(array1, array2, 0, array2.length);
  }

  public static boolean hasPrefix(final long[] array1, final long[] array2) {
    return hasSlice(array1, array2, 0, array2.length);
  }

  public static boolean hasPrefix(final float[] array1, final float[] array2) {
    return hasSlice(array1, array2, 0, array2.length);
  }

  public static boolean hasPrefix(final double[] array1, final double[] array2) {
    return hasSlice(array1, array2, 0, array2.length);
  }

  public static boolean hasSlice(
      final Object[] array, final Object[] slice, final int position, final int len) {
    if (position + len > array.length || len > slice.length) {
      return false;
    }

    for (int i = 0; i < len; ++i) {
      if ((array[i + position] == null && slice[i] != null)
          || (array[i + position] != null && slice[i] == null)
          || !array[i + position].equals(slice[i])) {
        return false;
      }
    }

    return true;
  }

  public static boolean hasSlice(
      final boolean[] array, final boolean[] slice, final int position, final int len) {
    if (position + len > array.length || len > slice.length) {
      return false;
    }

    for (int i = 0; i < len; ++i) {
      if (array[i + position] != slice[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean hasSlice(
      final byte[] array, final byte[] slice, final int position, final int len) {
    if (position + len > array.length || len > slice.length) {
      return false;
    }

    for (int i = 0; i < len; ++i) {
      if (array[i + position] != slice[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean hasSlice(
      final char[] array, final char[] slice, final int position, final int len) {
    if (position + len > array.length || len > slice.length) {
      return false;
    }

    for (int i = 0; i < len; ++i) {
      if (array[i + position] != slice[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean hasSlice(
      final short[] array, final short[] slice, final int position, final int len) {
    if (position + len > array.length || len > slice.length) {
      return false;
    }

    for (int i = 0; i < len; ++i) {
      if (array[i + position] != slice[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean hasSlice(
      final int[] array, final int[] slice, final int position, final int len) {
    if (position + len > array.length || len > slice.length) {
      return false;
    }

    for (int i = 0; i < len; ++i) {
      if (array[i + position] != slice[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean hasSlice(
      final long[] array, final long[] slice, final int position, final int len) {
    if (position + len > array.length || len > slice.length) {
      return false;
    }

    for (int i = 0; i < len; ++i) {
      if (array[i + position] != slice[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean hasSlice(
      final float[] array, final float[] slice, final int position, final int len) {
    if (position + len > array.length || len > slice.length) {
      return false;
    }

    for (int i = 0; i < len; ++i) {
      if (array[i + position] != slice[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean hasSlice(
      final double[] array, final double[] slice, final int position, final int len) {
    if (position + len > array.length || len > slice.length) {
      return false;
    }

    for (int i = 0; i < len; ++i) {
      if (array[i + position] != slice[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean hasSuffix(final Object[] array1, final Object[] array2) {
    return hasSlice(array1, array2, array1.length - array2.length, array2.length);
  }

  public static boolean hasSuffix(final boolean[] array1, final boolean[] array2) {
    return hasSlice(array1, array2, array1.length - array2.length, array2.length);
  }

  public static boolean hasSuffix(final byte[] array1, final byte[] array2) {
    return hasSlice(array1, array2, array1.length - array2.length, array2.length);
  }

  public static boolean hasSuffix(final char[] array1, final char[] array2) {
    return hasSlice(array1, array2, array1.length - array2.length, array2.length);
  }

  public static boolean hasSuffix(final short[] array1, final short[] array2) {
    return hasSlice(array1, array2, array1.length - array2.length, array2.length);
  }

  public static boolean hasSuffix(final int[] array1, final int[] array2) {
    return hasSlice(array1, array2, array1.length - array2.length, array2.length);
  }

  public static boolean hasSuffix(final long[] array1, final long[] array2) {
    return hasSlice(array1, array2, array1.length - array2.length, array2.length);
  }

  public static boolean hasSuffix(final float[] array1, final float[] array2) {
    return hasSlice(array1, array2, array1.length - array2.length, array2.length);
  }

  public static boolean hasSuffix(final double[] array1, final double[] array2) {
    return hasSlice(array1, array2, array1.length - array2.length, array2.length);
  }

  public static boolean inArray(final Object[] array, final Object object) {
    return indexOf(array, object) != -1;
  }

  public static boolean inArray(final boolean[] array, final boolean b) {
    return indexOf(array, b) != -1;
  }

  public static boolean inArray(final byte[] array, final byte b) {
    return indexOf(array, b) != -1;
  }

  public static boolean inArray(final char[] array, final char c) {
    return indexOf(array, c) != -1;
  }

  public static boolean inArray(final short[] array, final short s) {
    return indexOf(array, s) != -1;
  }

  public static boolean inArray(final int[] array, final int i) {
    return indexOf(array, i) != -1;
  }

  public static boolean inArray(final long[] array, final long l) {
    return indexOf(array, l) != -1;
  }

  public static boolean inArray(final float[] array, final float f) {
    return indexOf(array, f) != -1;
  }

  public static boolean inArray(final double[] array, final double d) {
    return indexOf(array, d) != -1;
  }

  public static int indexOf(final Object[] array, final Object object) {
    for (int i = 0; i < array.length; ++i) {
      if (array[i].equals(object)) {
        return i;
      }
    }

    return -1;
  }

  public static int indexOf(final boolean[] array, final boolean b) {
    for (int i = 0; i < array.length; ++i) {
      if (array[i] == b) {
        return i;
      }
    }

    return -1;
  }

  public static int indexOf(final byte[] array, final byte b) {
    for (int i = 0; i < array.length; ++i) {
      if (array[i] == b) {
        return i;
      }
    }

    return -1;
  }

  public static int indexOf(final char[] array, final char c) {
    for (int i = 0; i < array.length; ++i) {
      if (array[i] == c) {
        return i;
      }
    }

    return -1;
  }

  public static int indexOf(final short[] array, final short s) {
    for (int i = 0; i < array.length; ++i) {
      if (array[i] == s) {
        return i;
      }
    }

    return -1;
  }

  public static int indexOf(final int[] array, final int i) {
    for (int j = 0; j < array.length; ++j) {
      if (array[j] == i) {
        return j;
      }
    }

    return -1;
  }

  public static int indexOf(final long[] array, final long l) {
    for (int i = 0; i < array.length; ++i) {
      if (array[i] == l) {
        return i;
      }
    }

    return -1;
  }

  public static int indexOf(final float[] array, final float f) {
    for (int i = 0; i < array.length; ++i) {
      if (array[i] == f) {
        return i;
      }
    }

    return -1;
  }

  public static int indexOf(final double[] array, final double d) {
    for (int i = 0; i < array.length; ++i) {
      if (array[i] == d) {
        return i;
      }
    }

    return -1;
  }

  public static <T> T[] insert(final T[] array, final int pos, final T object) {
    final T[] newArray = newArray(array, array.length + 1);

    System.arraycopy(array, 0, newArray, 0, pos);
    newArray[pos] = object;
    System.arraycopy(array, pos, newArray, pos + 1, array.length - pos);

    return newArray;
  }

  public static boolean[] insert(final boolean[] array, final int pos, final boolean b) {
    final boolean[] newArray = new boolean[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    newArray[pos] = b;
    System.arraycopy(array, pos, newArray, pos + 1, array.length - pos);

    return newArray;
  }

  public static byte[] insert(final byte[] array, final int pos, final byte b) {
    final byte[] newArray = new byte[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    newArray[pos] = b;
    System.arraycopy(array, pos, newArray, pos + 1, array.length - pos);

    return newArray;
  }

  public static char[] insert(final char[] array, final int pos, final char c) {
    final char[] newArray = new char[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    newArray[pos] = c;
    System.arraycopy(array, pos, newArray, pos + 1, array.length - pos);

    return newArray;
  }

  public static short[] insert(final short[] array, final int pos, final short s) {
    final short[] newArray = new short[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    newArray[pos] = s;
    System.arraycopy(array, pos, newArray, pos + 1, array.length - pos);

    return newArray;
  }

  public static int[] insert(final int[] array, final int pos, final int i) {
    final int[] newArray = new int[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    newArray[pos] = i;
    System.arraycopy(array, pos, newArray, pos + 1, array.length - pos);

    return newArray;
  }

  public static long[] insert(final long[] array, final int pos, final long l) {
    final long[] newArray = new long[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    newArray[pos] = l;
    System.arraycopy(array, pos, newArray, pos + 1, array.length - pos);

    return newArray;
  }

  public static float[] insert(final float[] array, final int pos, final float f) {
    final float[] newArray = new float[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    newArray[pos] = f;
    System.arraycopy(array, pos, newArray, pos + 1, array.length - pos);

    return newArray;
  }

  public static double[] insert(final double[] array, final int pos, final double d) {
    final double[] newArray = new double[array.length + 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    newArray[pos] = d;
    System.arraycopy(array, pos, newArray, pos + 1, array.length - pos);

    return newArray;
  }

  public static <T> T[] insert(final T[] array, final int pos, final T[] objects) {
    final T[] newArray = newArray(array, array.length + objects.length);

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(objects, 0, newArray, pos, objects.length);
    System.arraycopy(array, pos, newArray, pos + objects.length, array.length - pos);

    return newArray;
  }

  public static byte[] insert(final byte[] array, final int pos, final byte[] bytes) {
    final byte[] newArray = new byte[array.length + bytes.length];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(bytes, 0, newArray, pos, bytes.length);
    System.arraycopy(array, pos, newArray, pos + bytes.length, array.length - pos);

    return newArray;
  }

  public static char[] insert(final char[] array, final int pos, final char[] chars) {
    final char[] newArray = new char[array.length + chars.length];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(chars, 0, newArray, pos, chars.length);
    System.arraycopy(array, pos, newArray, pos + chars.length, array.length - pos);

    return newArray;
  }

  public static short[] insert(final short[] array, final int pos, final short[] shorts) {
    final short[] newArray = new short[array.length + shorts.length];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(shorts, 0, newArray, pos, shorts.length);
    System.arraycopy(array, pos, newArray, pos + shorts.length, array.length - pos);

    return newArray;
  }

  public static int[] insert(final int[] array, final int pos, final int[] ints) {
    final int[] newArray = new int[array.length + ints.length];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(ints, 0, newArray, pos, ints.length);
    System.arraycopy(array, pos, newArray, pos + ints.length, array.length - pos);

    return newArray;
  }

  public static long[] insert(final long[] array, final int pos, final long[] longs) {
    final long[] newArray = new long[array.length + longs.length];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(longs, 0, newArray, pos, longs.length);
    System.arraycopy(array, pos, newArray, pos + longs.length, array.length - pos);

    return newArray;
  }

  public static float[] insert(final float[] array, final int pos, final float[] floats) {
    final float[] newArray = new float[array.length + floats.length];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(floats, 0, newArray, pos, floats.length);
    System.arraycopy(array, pos, newArray, pos + floats.length, array.length - pos);

    return newArray;
  }

  public static double[] insert(final double[] array, final int pos, final double[] doubles) {
    final double[] newArray = new double[array.length + doubles.length];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(doubles, 0, newArray, pos, doubles.length);
    System.arraycopy(array, pos, newArray, pos + doubles.length, array.length - pos);

    return newArray;
  }

  @SuppressWarnings("unchecked")
  private static <T> T[] newArray(final T[] array, final int length) {
    return (T[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), length);
  }

  public static <T> T[] remove(final T[] array, final int pos) {
    final T[] newArray = newArray(array, array.length - 1);

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(array, pos + 1, newArray, pos, array.length - pos - 1);

    return newArray;
  }

  public static boolean[] remove(final boolean[] array, final int pos) {
    final boolean[] newArray = new boolean[array.length - 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(array, pos + 1, newArray, pos, array.length - pos - 1);

    return newArray;
  }

  public static byte[] remove(final byte[] array, final int pos) {
    final byte[] newArray = new byte[array.length - 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(array, pos + 1, newArray, pos, array.length - pos - 1);

    return newArray;
  }

  public static char[] remove(final char[] array, final int pos) {
    final char[] newArray = new char[array.length - 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(array, pos + 1, newArray, pos, array.length - pos - 1);

    return newArray;
  }

  public static short[] remove(final short[] array, final int pos) {
    final short[] newArray = new short[array.length - 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(array, pos + 1, newArray, pos, array.length - pos - 1);

    return newArray;
  }

  public static int[] remove(final int[] array, final int pos) {
    final int[] newArray = new int[array.length - 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(array, pos + 1, newArray, pos, array.length - pos - 1);

    return newArray;
  }

  public static long[] remove(final long[] array, final int pos) {
    final long[] newArray = new long[array.length - 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(array, pos + 1, newArray, pos, array.length - pos - 1);

    return newArray;
  }

  public static float[] remove(final float[] array, final int pos) {
    final float[] newArray = new float[array.length - 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(array, pos + 1, newArray, pos, array.length - pos - 1);

    return newArray;
  }

  public static double[] remove(final double[] array, final int pos) {
    final double[] newArray = new double[array.length - 1];

    System.arraycopy(array, 0, newArray, 0, pos);
    System.arraycopy(array, pos + 1, newArray, pos, array.length - pos - 1);

    return newArray;
  }

  public static <T> T[] reverse(final T[] array) {
    final T[] newArray = newArray(array, array.length);

    for (int i = 0; i < newArray.length; ++i) {
      newArray[i] = array[array.length - 1 - i];
    }

    return newArray;
  }

  public static boolean[] reverse(final boolean[] array) {
    final boolean[] newArray = new boolean[array.length];

    for (int i = 0; i < newArray.length; ++i) {
      newArray[i] = array[array.length - 1 - i];
    }

    return newArray;
  }

  public static byte[] reverse(final byte[] array) {
    final byte[] newArray = new byte[array.length];

    for (int i = 0; i < newArray.length; ++i) {
      newArray[i] = array[array.length - 1 - i];
    }

    return newArray;
  }

  public static char[] reverse(final char[] array) {
    final char[] newArray = new char[array.length];

    for (int i = 0; i < newArray.length; ++i) {
      newArray[i] = array[array.length - 1 - i];
    }

    return newArray;
  }

  public static short[] reverse(final short[] array) {
    final short[] newArray = new short[array.length];

    for (int i = 0; i < newArray.length; ++i) {
      newArray[i] = array[array.length - 1 - i];
    }

    return newArray;
  }

  public static int[] reverse(final int[] array) {
    final int[] newArray = new int[array.length];

    for (int i = 0; i < newArray.length; ++i) {
      newArray[i] = array[array.length - 1 - i];
    }

    return newArray;
  }

  public static long[] reverse(final long[] array) {
    final long[] newArray = new long[array.length];

    for (int i = 0; i < newArray.length; ++i) {
      newArray[i] = array[array.length - 1 - i];
    }

    return newArray;
  }

  public static float[] reverse(final float[] array) {
    final float[] newArray = new float[array.length];

    for (int i = 0; i < newArray.length; ++i) {
      newArray[i] = array[array.length - 1 - i];
    }

    return newArray;
  }

  public static double[] reverse(final double[] array) {
    final double[] newArray = new double[array.length];

    for (int i = 0; i < newArray.length; ++i) {
      newArray[i] = array[array.length - 1 - i];
    }

    return newArray;
  }

  public static <T> T[] truncate(final T[] array, final int pos) {
    if (pos == array.length) {
      return array;
    }

    final T[] newArray = newArray(array, pos);

    System.arraycopy(array, 0, newArray, 0, pos);

    return newArray;
  }

  public static boolean[] truncate(final boolean[] array, final int pos) {
    if (pos == array.length) {
      return array;
    }

    final boolean[] newArray = new boolean[pos];

    System.arraycopy(array, 0, newArray, 0, pos);

    return newArray;
  }

  public static byte[] truncate(final byte[] array, final int pos) {
    if (pos == array.length) {
      return array;
    }

    final byte[] newArray = new byte[pos];

    System.arraycopy(array, 0, newArray, 0, pos);

    return newArray;
  }

  public static char[] truncate(final char[] array, final int pos) {
    if (pos == array.length) {
      return array;
    }

    final char[] newArray = new char[pos];

    System.arraycopy(array, 0, newArray, 0, pos);

    return newArray;
  }

  public static short[] truncate(final short[] array, final int pos) {
    if (pos == array.length) {
      return array;
    }

    final short[] newArray = new short[pos];

    System.arraycopy(array, 0, newArray, 0, pos);

    return newArray;
  }

  public static int[] truncate(final int[] array, final int pos) {
    if (pos == array.length) {
      return array;
    }

    final int[] newArray = new int[pos];

    System.arraycopy(array, 0, newArray, 0, pos);

    return newArray;
  }

  public static long[] truncate(final long[] array, final int pos) {
    if (pos == array.length) {
      return array;
    }

    final long[] newArray = new long[pos];

    System.arraycopy(array, 0, newArray, 0, pos);

    return newArray;
  }

  public static float[] truncate(final float[] array, final int pos) {
    if (pos == array.length) {
      return array;
    }

    final float[] newArray = new float[pos];

    System.arraycopy(array, 0, newArray, 0, pos);

    return newArray;
  }

  public static double[] truncate(final double[] array, final int pos) {
    if (pos == array.length) {
      return array;
    }

    final double[] newArray = new double[pos];

    System.arraycopy(array, 0, newArray, 0, pos);

    return newArray;
  }
}
