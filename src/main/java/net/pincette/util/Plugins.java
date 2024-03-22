package net.pincette.util;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.ModuleLayer.boot;
import static java.nio.file.Files.list;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static net.pincette.util.Util.tryToGetRethrow;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Loads plugins as Java 9 modules.
 *
 * @author Werner Donné
 */
public class Plugins {
  private static Set<String> alreadyLoaded() {
    return boot().configuration().modules().stream().map(ResolvedModule::name).collect(toSet());
  }

  private static ModuleLayer createPluginLayer(final Path directory) {
    final var boot = boot();
    final var finder = new Finder(ModuleFinder.of(directory), alreadyLoaded());

    return boot.defineModulesWithOneLoader(
        boot.configuration().resolve(finder, ModuleFinder.of(), moduleNames(finder)),
        getSystemClassLoader());
  }

  /**
   * Loads plugins from the subdirectories of <code>directory</code> as modules, each in its own
   * module layer.
   *
   * @param directory the place to find plugins as subdirectories.
   * @param loader the service loader.
   * @return The stream of loaded plugins.
   * @param <T> The plugin type.
   */
  public static <T> Stream<T> loadPlugins(
      final Path directory, final Function<ModuleLayer, ServiceLoader<T>> loader) {
    return Optional.of(directory)
        .filter(Files::isDirectory)
        .flatMap(d -> tryToGetRethrow(() -> list(d)))
        .map(
            children ->
                children
                    .map(Plugins::createPluginLayer)
                    .flatMap(layer -> stream(loader.apply(layer).spliterator(), false)))
        .orElseGet(Stream::empty);
  }

  private static Set<String> moduleNames(final ModuleFinder finder) {
    return finder.findAll().stream().map(ref -> ref.descriptor().name()).collect(toSet());
  }

  private record Finder(ModuleFinder delegate, Set<String> alreadyLoaded) implements ModuleFinder {
    public Optional<ModuleReference> find(final String name) {
      return Optional.of(name).filter(n -> !alreadyLoaded.contains(n)).flatMap(delegate::find);
    }

    public Set<ModuleReference> findAll() {
      return delegate.findAll().stream()
          .filter(r -> !alreadyLoaded.contains(r.descriptor().name()))
          .collect(toSet());
    }
  }
}
