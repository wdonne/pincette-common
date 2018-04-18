package net.pincette.cls;

import java.util.regex.Pattern;

public class Method {

  private static final Pattern METHOD_PARAMETERS = Pattern.compile("\\[*(L[^;]+;|[ZBCSIFDJ])");

  Attribute[] attributes;
  String className;
  Code code;
  String descriptor;
  String[] exceptions;
  boolean isDeprecated;
  boolean isSynthetic;
  int modifiers;
  String name;

  public Attribute[] getAttribtutes() {
    return attributes;
  }

  public Code getCode() {
    return code;
  }

  public String getDescriptor() {
    return descriptor;
  }

  public String[] getExceptionNames() {
    return exceptions;
  }

  public String[] getExceptionTypes() {
    final String[] result = new String[exceptions.length];

    for (int i = 0; i < exceptions.length; ++i) {
      result[i] = Util.getType(exceptions[i]);
    }

    return result;
  }

  public int getModifiers() {
    return modifiers;
  }

  public String getName() {
    return isConstructor()
        ? Util.getType(className).substring(Util.getType(className).lastIndexOf('.') + 1)
        : name;
  }

  public String[] getParameterTypes() {
    return METHOD_PARAMETERS
        .splitAsStream(descriptor.substring(1, descriptor.indexOf(')')))
        .map(Util::getType)
        .toArray(String[]::new);
  }

  public String getReturnType() {
    return Util.getType(descriptor.substring(descriptor.lastIndexOf(')') + 1));
  }

  public boolean isConstructor() {
    return name.equals("<init>");
  }

  public boolean isDeprecated() {
    return isDeprecated;
  }

  public boolean isInitializer() {
    return name.equals("<clinit>");
  }

  public boolean isSynthetic() {
    return isSynthetic;
  }
}
