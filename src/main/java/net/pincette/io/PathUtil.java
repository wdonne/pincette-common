package net.pincette.io;

import static java.lang.Integer.MAX_VALUE;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.walkFileTree;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Optional;

/**
 * Some path utilities.
 *
 * @author Werner Donné
 * @since 2.0.2
 */
public class PathUtil {
  private PathUtil() {}

  /**
   * Recusively copy <code>source</code> to <code>target</code>.
   *
   * @param source the source path.
   * @param target the target path.
   */
  public static void copy(final Path source, final Path target) {
    final Path realTarget =
        exists(target) && isDirectory(target) ? target.resolve(source.getFileName()) : target;

    tryToDoRethrow(
        () ->
            walkFileTree(
                source,
                EnumSet.of(FOLLOW_LINKS),
                MAX_VALUE,
                new SimpleFileVisitor<>() {
                  @Override
                  public FileVisitResult preVisitDirectory(
                      final Path dir, final BasicFileAttributes attrs) {
                    return Optional.of(realTarget.resolve(source.relativize(dir)))
                        .filter(d -> !exists(d) || isDirectory(d))
                        .flatMap(d -> tryToGetRethrow(() -> Files.copy(dir, d)))
                        .map(path -> CONTINUE)
                        .orElse(TERMINATE);
                  }

                  @Override
                  public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                      throws IOException {
                    Files.copy(file, realTarget.resolve(source.relativize(file)));

                    return CONTINUE;
                  }
                }));
  }

  /**
   * Recursively delete the path.
   *
   * @param path the path.
   */
  public static void delete(final Path path) {
    tryToDoRethrow(
        () ->
            walkFileTree(
                path,
                new SimpleFileVisitor<>() {
                  @Override
                  public FileVisitResult postVisitDirectory(final Path dir, final IOException e)
                      throws IOException {
                    if (e != null) {
                      throw e;
                    }

                    Files.delete(dir);

                    return CONTINUE;
                  }

                  @Override
                  public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                      throws IOException {
                    Files.delete(file);

                    return CONTINUE;
                  }
                }));
  }
}
