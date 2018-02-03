package net.pincette.cls;

import java.util.function.Supplier;

public class Util

{

  private static final String[][] basicTypes = {
      {"B", "byte"},
      {"C", "char"},
      {"D", "double"},
      {"F", "float"},
      {"I", "int"},
      {"J", "long"},
      {"S", "short"},
      {"V", "void"},
      {"Z", "boolean"}
  };

  private Util() {
  }

  private static String
  getBasicType(final String descriptor) {
    for (int i = 0; i < basicTypes.length; ++i) {
      if (basicTypes[i][0].equals(descriptor)) {
        return basicTypes[i][1];
      }
    }

    return null;
  }

  public static String
  getType(final String descriptor) {
    final Supplier<String> ifBasicOr =
        () ->
            getBasicType(descriptor) != null ?
                getBasicType(descriptor) : descriptor.replace('/', '.');
    final Supplier<String> ifLeftBracketOr =
        () ->
            descriptor.charAt(0) == '[' ?
                (getType(descriptor.substring(1)) + "[]") : ifBasicOr.get();

    return
        descriptor.charAt(0) == 'L' &&
            descriptor.charAt(descriptor.length() - 1) == ';' ?
            getType(descriptor.substring(1, descriptor.length() - 1)) :
            ifLeftBracketOr.get();
  }

} // Util
