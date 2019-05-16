package net.pincette.util;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Stream.empty;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.rangeInclusive;

import java.util.stream.Stream;

/**
 * Functions te generate a stream of shingles.
 *
 * @author Werner Donn\u00e9
 */
public class Shingles {
  private Shingles() {}

  /**
   * Transform a word in a stream of shingles.
   *
   * @param word the given word.
   * @param minSize the minimum length of the generated shingles.
   * @param maxSize the maximum length of the generated shingles. It is limited by the length of the
   *     word.
   * @return The stream of shingles.
   */
  public static Stream<String> generate(final String word, final int minSize, final int maxSize) {
    return rangeInclusive(minSize, min(word.length(), maxSize))
        .map(i -> pair(i, word))
        .flatMap(pair -> generate(pair.second, pair.first));
  }

  /**
   * Generates a stream of shingles.
   *
   * @param word the word that is transformed to shingles.
   * @param size the size of the shingles. If it is larger than the length of the given word then
   *     the stream will be empty.
   * @return The stream of shingles.
   */
  public static Stream<String> generate(final String word, final int size) {
    return size > word.length()
        ? empty()
        : rangeInclusive(0, max(word.length() - size, 0)).map(i -> word.substring(i, i + size));
  }

  /**
   * Expand the stream of words in a stream of shingles.
   *
   * @param words the given word stream.
   * @param minSize the minimum length of the generated shingles.
   * @param maxSize the maximum length of the generated shingles. It is limited by the length of an
   *     individual word.
   * @return The stream of shingles.
   */
  public static Stream<String> generate(
      final Stream<String> words, final int minSize, final int maxSize) {
    return words.flatMap(word -> generate(word, minSize, maxSize));
  }
}
