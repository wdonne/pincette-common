package net.pincette.cls;

import static java.util.Arrays.sort;
import static java.util.Comparator.comparing;

import java.io.IOException;
import java.lang.reflect.Modifier;

public class Writer {
  private Writer() {}

  private static String getConstructors(final Method[] methods) {
    final StringBuilder result = new StringBuilder();

    for (final Method method : methods) {
      if (method.isConstructor() && !method.isSynthetic()) {
        result
            .append("  ")
            .append(getModifiers(method.getModifiers()))
            .append("\n  ")
            .append(method.getName())
            .append(getParameters(method))
            .append(
                (method.getExceptionTypes().length > 0)
                    ? ("\n    " + getThrows(method.getExceptionTypes()))
                    : "")
            .append("\n  {\n  }\n\n");
      }
    }

    return result.toString();
  }

  private static String getFields(final Field[] fields) {
    sort(fields, comparing(Field::getName));

    final StringBuilder result = new StringBuilder();

    for (final Field field : fields) {
      if (!field.isSynthetic()) {
        result
            .append("  ")
            .append(getModifiers(field.getModifiers()))
            .append(field.getType())
            .append(' ')
            .append(field.getName())
            .append(";\n");
      }
    }

    return result.toString();
  }

  private static String getMethods(final Method[] methods, final boolean isInterface) {
    sort(methods, comparing(Method::getName));

    final StringBuilder result = new StringBuilder();

    for (final Method method : methods) {
      if (!method.isConstructor() && !method.isInitializer() && !method.isSynthetic()) {
        result
            .append("  ")
            .append(getModifiers(method.getModifiers()))
            .append(method.getReturnType())
            .append("\n  ")
            .append(method.getName())
            .append(getParameters(method))
            .append(
                (method.getExceptionTypes().length > 0)
                    ? ("\n    " + getThrows(method.getExceptionTypes()))
                    : "")
            .append(
                isInterface || Modifier.isNative(method.getModifiers())
                    ? ";\n\n"
                    : "\n  {\n  }\n\n");
      }
    }

    return result.toString();
  }

  private static String getModifiers(final int modifiers) {
    return modifiers == 0 ? "" : (Modifier.toString(modifiers) + " ");
  }

  private static String getParameters(final Method method) {
    final int staticCode = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
    final StringBuilder result = new StringBuilder();
    final String[] types = method.getParameterTypes();

    result.append('(');

    for (int i = 0; i < types.length; ++i) {
      result
          .append(i > 0 ? ", " : "")
          .append(types[i])
          .append(' ')
          .append(
              method.getCode() != null
                      && method.getCode().getLocalVariables().length >= types.length
                  ? method.getCode().getLocalVariables()[i + staticCode].getName()
                  : ("p" + i));
    }

    result.append(')');

    return result.toString();
  }

  private static String getThrows(final String[] types) {
    if (types.length == 0) {
      return "";
    }

    final StringBuilder result = new StringBuilder();

    result.append("throws ");

    for (int i = 0; i < types.length; ++i) {
      result.append(i > 0 ? ", " : "").append(types[i]);
    }

    return result.toString();
  }

  public static void write(final java.io.Writer out, final ClassFile c) throws IOException {
    if (c.getType().indexOf('.') != -1) {
      out.write("package " + c.getType().substring(0, c.getType().lastIndexOf('.')) + ";\n\n");
    }

    final int modifiers = c.getModifiers() & ~Modifier.SYNCHRONIZED;
    // This also indicated ACC_SUPER.

    out.write(
        getModifiers(modifiers)
            + (c.isInterface() ? "" : "class ")
            + c.getType().substring(c.getType().lastIndexOf('.') + 1)
            + "\n");

    if (c.getSuperClassName() != null && !c.getSuperClassType().equals("java.lang.Object")) {
      out.write("  extends " + c.getSuperClassType() + "\n");
    }

    final String[] intfs = c.getInterfaceTypes();

    if (intfs.length > 0) {
      out.write("  " + (c.isInterface() ? "extends" : "implements") + "\n");
    }

    for (int i = 0; i < intfs.length; ++i) {
      out.write("    " + intfs[i] + (i < intfs.length - 1 ? ",\n" : "\n"));
    }

    out.write(
        "{\n"
            + getFields(c.getFields())
            + "\n"
            + getConstructors(c.getMethods())
            + "\n"
            + getMethods(c.getMethods(), c.isInterface())
            + "} // "
            + c.getType().substring(c.getType().lastIndexOf('.') + 1)
            + "\n");
  }
}
