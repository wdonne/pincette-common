package net.pincette.cls;

public class Code {
  Attribute[] attributes;
  byte[] theCode;
  ExceptionHandler[] exceptions;
  LocalVariable[] localVariables;
  short maxLocals;
  short maxStack;

  public Attribute[] getAttributes() {
    return attributes;
  }

  public byte[] getCode() {
    return theCode;
  }

  public ExceptionHandler[] getExceptionHandler() {
    return exceptions;
  }

  public LocalVariable[] getLocalVariables() {
    return localVariables;
  }

  public short getMaxLocals() {
    return maxLocals;
  }

  public short getMaxStack() {
    return maxStack;
  }
}
