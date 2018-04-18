package net.pincette.cls;

import static java.util.Comparator.comparing;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.takeWhile;
import static net.pincette.util.Util.tryToGetRethrow;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.pincette.util.Pair;

public class ClassFile {

  private static final byte CONSTANT_CLASS = 7;
  private static final byte CONSTANT_FIELDREF = 9;
  private static final byte CONSTANT_METHODREF = 10;
  private static final byte CONSTANT_INTERFACE_METHODREF = 11;
  private static final byte CONSTANT_STRING = 8;
  private static final byte CONSTANT_INTEGER = 3;
  private static final byte CONSTANT_FLOAT = 4;
  private static final byte CONSTANT_LONG = 5;
  private static final byte CONSTANT_DOUBLE = 6;
  private static final byte CONSTANT_NAME_AND_TYPE = 12;
  private static final byte CONSTANT_UTF_8 = 1;
  private static final String DEPRECATED = "Deprecated";
  private static final Object EMPTY = new Object();

  private Attribute[] attributes;
  private Object[] constantPool;
  private Field[] fields;
  private String[] interfaces;
  private boolean isDeprecated;
  private Method[] methods;
  private int modifiers;
  private String name;
  private String sourceFile;
  private String superClass;
  private String version;

  private ClassFile() {}

  private static boolean isDoubleTag(final byte tag) {
    return tag == CONSTANT_DOUBLE || tag == CONSTANT_LONG;
  }

  private static ClassFile parse(final DataInputStream in) throws IOException {
    if (in.readInt() != 0xcafebabe) {
      throw new NotAClassException();
    }

    final ClassFile c = new ClassFile();

    c.version = String.valueOf(in.readShort());
    c.version = String.valueOf(in.readShort()) + "." + c.version;
    c.constantPool = readConstantPool(in, in.readShort());
    c.modifiers = in.readShort();
    c.name = c.getClassName(in.readShort());

    final short superClass = in.readShort();

    c.superClass = superClass == 0 ? null : c.getClassName(superClass);
    c.interfaces = c.readClassNames(in, in.readShort());
    c.fields = c.readFields(in, in.readShort());
    c.methods = c.readMethods(in, in.readShort());

    final short attLength = in.readShort();
    final List<Attribute> attributes = new ArrayList<>();

    for (short i = 0; i < attLength; ++i) {
      final String name = (String) c.constantPool[in.readShort()];
      final int length = in.readInt();

      if (name.equals(DEPRECATED)) {
        c.isDeprecated = true;
      } else if (name.equals("SourceFile")) {
        c.sourceFile = (String) c.constantPool[in.readShort()];
      } else {
        attributes.add(c.readAttribute(in, name, length));
      }
    }

    c.attributes = attributes.toArray(new Attribute[attributes.size()]);
    c.constantPool = null;

    return c;
  }

  public static ClassFile parse(final InputStream in) throws IOException {
    return parse(new DataInputStream(in));
  }

  public static ClassFile parse(final byte[] b) throws IOException {
    return parse(new ByteArrayInputStream(b));
  }

  private static Object[] readConstantPool(final DataInput in, final short size) {
    return takeWhile(
            readConstantPoolEntry(in, 1, size),
            pair -> readConstantPoolEntry(in, pair.first + 1, size),
            pair -> pair.first < size)
        .flatMap(pair -> pair.second)
        .map(o -> o == EMPTY ? null : 0)
        .toArray();
  }

  private static Pair<Integer, Stream<Object>> readConstantPoolEntry(
      final DataInput in, final int index, final short size) {
    final Function<Byte, Stream<Object>> addExtra =
        tag -> isDoubleTag(tag) ? Stream.of(EMPTY) : empty();
    final Function<Byte, Integer> moveExtra = tag -> isDoubleTag(tag) ? 1 : 0;

    return index < size
        ? tryToGetRethrow(in::readByte)
            .map(
                tag ->
                    pair(
                        index + 1 + moveExtra.apply(tag),
                        concat(
                            Stream.of(
                                tryToGetRethrow(() -> readConstantPoolEntry(in, tag)).orElse(null)),
                            addExtra.apply(tag))))
            .orElse(null)
        : pair(index + 1, null);
  }

  private static Object readConstantPoolEntry(final DataInput in, final byte tag)
      throws IOException {
    switch (tag) {
      case CONSTANT_CLASS:
        return new ClassInfo(in.readShort());

      case CONSTANT_FIELDREF:
      case CONSTANT_METHODREF:
      case CONSTANT_INTERFACE_METHODREF:
        return new RefInfo(tag, in.readShort(), in.readShort());

      case CONSTANT_STRING:
        return new StringInfo(in.readShort());

      case CONSTANT_INTEGER:
        return in.readInt();

      case CONSTANT_FLOAT:
        return in.readFloat();

      case CONSTANT_LONG:
        return in.readLong();

      case CONSTANT_DOUBLE:
        return in.readDouble();

      case CONSTANT_NAME_AND_TYPE:
        return new NameAndType(in.readShort(), in.readShort());

      case CONSTANT_UTF_8:
        return readString(in);

      default:
        return null;
    }
  }

  private static String readString(final DataInput in) throws IOException {
    final byte[] b = new byte[in.readShort()];

    in.readFully(b);

    return tryToGetRethrow(() -> new String(b, "UTF-8")).orElse(null);
  }

  public Attribute[] getAttributes() {
    return attributes;
  }

  private String getClassName(final short index) {
    return (String) constantPool[((ClassInfo) constantPool[index]).name];
  }

  public Field[] getFields() {
    return fields;
  }

  public String[] getInterfaceNames() {
    return interfaces;
  }

  public String[] getInterfaceTypes() {
    final String[] result = new String[interfaces.length];

    for (int i = 0; i < interfaces.length; ++i) {
      result[i] = Util.getType(interfaces[i]);
    }

    return result;
  }

  public Method[] getMethods() {
    return methods;
  }

  public int getModifiers() {
    return modifiers;
  }

  public String getName() {
    return name;
  }

  public String getSourceFile() {
    return sourceFile;
  }

  public String getSuperClassName() {
    return superClass;
  }

  public String getSuperClassType() {
    return superClass == null ? null : Util.getType(superClass);
  }

  public String getType() {
    return Util.getType(name);
  }

  public String getVersion() {
    return version;
  }

  public boolean isArray() {
    return name.charAt(0) == '[';
  }

  public boolean isDeprecated() {
    return isDeprecated;
  }

  public boolean isInterface() {
    return Modifier.isInterface(getModifiers());
  }

  private Attribute readAttribute(final DataInput in, final String name, final int length)
      throws IOException {
    final Attribute attribute = new Attribute();

    attribute.name = name;
    attribute.value = new byte[length];
    in.readFully(attribute.value);

    return attribute;
  }

  private String[] readClassNames(final DataInput in, final short size) throws IOException {
    final String[] result = new String[size];

    for (short i = 0; i < size; ++i) {
      result[i] = getClassName(in.readShort());
    }

    return result;
  }

  private Code readCode(final DataInput in) throws IOException {
    final Code result = new Code();

    result.maxStack = in.readShort();
    result.maxLocals = in.readShort();
    result.theCode = new byte[in.readInt()];
    in.readFully(result.theCode);
    result.exceptions = readExceptionHandlers(in, in.readShort());

    final short attLength = in.readShort();
    final List<Attribute> atts = new ArrayList<>();
    final List<LocalVariable> localVariables = new ArrayList<>();

    for (short i = 0; i < attLength; ++i) {
      final String nam = (String) constantPool[in.readShort()];
      final int length = in.readInt(); // The attribute length.

      if (nam.equals("LocalVariableTable")) {
        localVariables.addAll(readLocalVariables(in, in.readShort()));
      } else {
        atts.add(readAttribute(in, nam, length));
      }
    }

    result.attributes = atts.toArray(new Attribute[atts.size()]);
    localVariables.sort(comparing(LocalVariable::getIndex));
    result.localVariables = localVariables.toArray(new LocalVariable[0]);

    return result;
  }

  private ExceptionHandler[] readExceptionHandlers(final DataInput in, final short size)
      throws IOException {
    final ExceptionHandler[] result = new ExceptionHandler[size];

    for (short i = 0; i < size; ++i) {
      result[i] = new ExceptionHandler();
      result[i].startPC = in.readShort();
      result[i].endPC = in.readShort();
      result[i].handlerPC = in.readShort();

      final short index = in.readShort();

      result[i].type = index == 0 ? null : getClassName(index);
    }

    return result;
  }

  private Field[] readFields(final DataInput in, final short size) throws IOException {
    final Field[] result = new Field[size];

    for (int i = 0; i < size; ++i) {
      result[i] = new Field();
      result[i].modifiers = in.readShort();
      result[i].name = (String) constantPool[in.readShort()];
      result[i].descriptor = (String) constantPool[in.readShort()];

      final short attLength = in.readShort();
      final List<Attribute> atts = new ArrayList<>();

      for (short j = 0; j < attLength; ++j) {
        final String nam = (String) constantPool[in.readShort()];
        final int length = in.readInt(); // The attribute length.

        if (nam.equals("ConstantValue")) {
          result[i].value = constantPool[in.readShort()];
        } else if (nam.equals("Synthetic")) {
          result[i].isSynthetic = true;
        } else if (nam.equals(DEPRECATED)) {
          result[i].isDeprecated = true;
        } else {
          atts.add(readAttribute(in, nam, length));
        }
      }

      result[i].attributes = atts.toArray(new Attribute[atts.size()]);
    }

    return result;
  }

  private List<LocalVariable> readLocalVariables(final DataInput in, final short size)
      throws IOException {
    final List<LocalVariable> result = new ArrayList<>();

    for (short i = 0; i < size; ++i) {
      final LocalVariable localVariable = new LocalVariable();

      localVariable.startPC = in.readShort();
      localVariable.length = in.readShort();
      localVariable.name = (String) constantPool[in.readShort()];
      localVariable.descriptor = (String) constantPool[in.readShort()];
      localVariable.index = in.readShort();
      result.add(localVariable);
    }

    return result;
  }

  private Method[] readMethods(final DataInput in, final short size) throws IOException {
    final Method[] result = new Method[size];

    for (short i = 0; i < size; ++i) {
      result[i] = new Method();
      result[i].modifiers = in.readShort();
      result[i].name = (String) constantPool[in.readShort()];
      result[i].descriptor = (String) constantPool[in.readShort()];
      result[i].exceptions = new String[0];
      result[i].className = name;

      final short attLength = in.readShort();
      final List<Attribute> atts = new ArrayList<>();

      for (short j = 0; j < attLength; ++j) {
        final String nam = (String) constantPool[in.readShort()];
        final int length = in.readInt(); // The attribute length.

        if (nam.equals("Code")) {
          result[i].code = readCode(in);
        } else if (nam.equals("Exceptions")) {
          result[i].exceptions = readClassNames(in, in.readShort());
        } else if (nam.equals("Synthetic")) {
          result[i].isSynthetic = true;
        } else if (nam.equals(DEPRECATED)) {
          result[i].isDeprecated = true;
        } else {
          atts.add(readAttribute(in, nam, length));
        }
      }

      result[i].attributes = atts.toArray(new Attribute[atts.size()]);
    }

    return result;
  }

  private static class ClassInfo {

    private short name;

    private ClassInfo(final short name) {
      this.name = name;
    }
  }

  private static class NameAndType {

    short name;
    short type;

    private NameAndType(final short name, final short type) {
      this.name = name;
      this.type = type;
    }
  }

  private static class RefInfo {

    short classInfo;
    short nameAndType;
    byte tag;

    private RefInfo(final byte tag, final short classInfo, final short nameAndType) {
      this.tag = tag;
      this.classInfo = classInfo;
      this.nameAndType = nameAndType;
    }
  }

  private static class StringInfo {

    short name;

    private StringInfo(final short name) {
      this.name = name;
    }
  }
}
