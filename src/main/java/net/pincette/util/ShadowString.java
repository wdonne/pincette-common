package net.pincette.util;

import static java.lang.CharSequence.compare;

/**
 * A representation of a string that doesn't copy the underlying string.
 *
 * @author Werner Donn\u00e9
 * @since 2.0.4
 */
public class ShadowString implements CharSequence {
  private final int end;
  private final String s;
  private final int start;

  public ShadowString(final String s) {
    this(s, 0, s.length());
  }

  public ShadowString(final String s, final int start, final int end) {
    this.s = s;
    this.start = start;
    this.end = end;
  }

  public static ShadowString shadow(final String s) {
    return new ShadowString(s);
  }

  public static ShadowString shadow(final String s, final int start, final int end) {
    return new ShadowString(s, start, end);
  }

  @Override
  public char charAt(final int index) {
    return s.charAt(start + index);
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof CharSequence && compare(this, (CharSequence) o) == 0;
  }

  @Override
  public int hashCode() {
    int h = 0;

    for (int i = start; i < end; ++i) {
      h = 31 * h + s.charAt(i);
    }

    return h;
  }

  @Override
  public int length() {
    return end - start;
  }

  @Override
  public CharSequence subSequence(final int start, final int end) {
    return shadow(s, this.start + start, this.start + end);
  }

  @Override
  public String toString() {
    return start == 0 && end == s.length() ? s : s.substring(start, end);
  }
}
