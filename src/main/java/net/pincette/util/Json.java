package net.pincette.util;

import static java.lang.String.join;
import static java.time.Instant.ofEpochMilli;
import static java.time.Instant.parse;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createParserFactory;
import static javax.json.Json.createReader;
import static javax.json.Json.createWriterFactory;
import static javax.json.JsonValue.ValueType.FALSE;
import static javax.json.JsonValue.ValueType.TRUE;
import static javax.xml.stream.XMLOutputFactory.newInstance;
import static net.pincette.util.Collections.difference;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.last;
import static net.pincette.util.Util.allPaths;
import static net.pincette.util.Util.autoClose;
import static net.pincette.util.Util.getLastSegment;
import static net.pincette.util.Util.getSegments;
import static net.pincette.util.Util.pathSearch;
import static net.pincette.util.Util.tryToDoWith;
import static net.pincette.util.Util.tryToDoWithRethrow;
import static net.pincette.util.Util.tryToGetSilent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.xml.stream.XMLEventWriter;
import net.pincette.function.SideEffect;
import net.pincette.xml.JsonEventReader;

/**
 * Some JSON-utilities.
 *
 * @author Werner Donn\u00e9
 */
public class Json {
  public static final Function<JsonObject, ?> EVALUATOR =
      value ->
          value.getValueType() == JsonValue.ValueType.NUMBER
              ? asNumber(value).longValue()
              : toString(value);
  private static final String ADD = "add";
  private static final String COPY = "copy";
  private static final String ERROR = "_error";
  private static final String FROM = "from";
  private static final String MOVE = "move";
  private static final String OP = "op";
  private static final String PATH = "path";
  private static final String REMOVE = "remove";
  private static final String REPLACE = "replace";
  private static final Set<String> OPS =
      net.pincette.util.Collections.set(ADD, COPY, MOVE, REMOVE, REPLACE);
  private static final Set<String> SET_OPS =
      net.pincette.util.Collections.set(ADD, COPY, MOVE, REPLACE);
  private static final String VALUE = "/value";

  public static JsonObject add(
      final JsonObject obj, final String name, final JsonArrayBuilder value) {
    return add(obj, builder -> builder.add(name, value));
  }

  public static JsonObject add(
      final JsonObject obj, final String name, final JsonObjectBuilder value) {
    return add(obj, builder -> builder.add(name, value));
  }

  /**
   * Returns a new object in which the value of the field designated by the dot-separated <code>path
   * </code> is added or replaced with <code>value</code>.
   *
   * @param obj the given JSON object.
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The new object.
   */
  public static JsonObject add(final JsonObject obj, final String path, final JsonValue value) {
    return transform(obj, addTransformer(path, value));
  }

  /**
   * Returns a new object in which the value of the field designated by the dot-separated <code>path
   * </code> is added or replaced with <code>value</code>.
   *
   * @param obj the given JSON object.
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The new object.
   */
  public static JsonObject add(final JsonObject obj, final String path, final Object value) {
    return add(obj, path, createValue(value));
  }

  public static JsonObject add(final JsonObject obj, final UnaryOperator<JsonObjectBuilder> add) {
    return add.apply(copy(obj, createObjectBuilder())).build();
  }

  public static JsonObject add(final JsonObject obj1, final JsonObject obj2) {
    return add(obj1, builder -> copy(obj2, builder));
  }

  public static JsonObject add(final JsonObject obj, final Map<String, ?> fields) {
    return add(obj, builder -> add(builder, fields));
  }

  public static JsonObjectBuilder add(
      final JsonObjectBuilder builder, final Map<String, ?> fields) {
    return fields.entrySet().stream()
        .reduce(builder, (b, e) -> addJsonField(b, e.getKey(), e.getValue()), (b1, b2) -> b1);
  }

  public static JsonObjectBuilder add(final JsonObjectBuilder builder, final JsonObject obj) {
    return copy(obj, builder);
  }

  public static JsonArrayBuilder add(final JsonArrayBuilder builder, final JsonArray array) {
    return array.stream().reduce(builder, JsonArrayBuilder::add, (b1, b2) -> b1);
  }

  /**
   * Returns a copy of the object where the values of the fields in <code>values</code> are
   * replaced. The first entry of a pair is the name and the second entry is the new value.
   *
   * @param obj the given JSON object.
   * @param values the list of pairs, where the first entry is the name and the second entry is the
   *     new value.
   * @return The new object.
   */
  public static JsonObject add(final JsonObject obj, Collection<Pair<String, Object>> values) {
    return values.stream()
        .reduce(
            copy(obj, createObjectBuilder()),
            (b, p) -> addJsonField(b, p.first, p.second),
            (b1, b2) -> b1)
        .build();
  }

  public static boolean added(
      final JsonArray patch, final JsonObject original, final String jsonPointer) {
    return !getValue(original, jsonPointer).isPresent() && isSet(patch, jsonPointer);
  }

  /**
   * Executes <code>add</code> if <code>test</code> returns <code>true</code>.
   *
   * @param builder the builder
   * @param test the test function
   * @param add the add function
   * @return The builder.
   */
  public static JsonObjectBuilder addIf(
      final JsonObjectBuilder builder,
      final BooleanSupplier test,
      final UnaryOperator<JsonObjectBuilder> add) {
    return test.getAsBoolean() ? add.apply(builder) : builder;
  }

  public static JsonObjectBuilder addJsonField(
      final JsonObjectBuilder builder, final String name, final Object value) {
    return builder.add(name, createValue(value));
  }

  public static JsonArrayBuilder addJsonField(final JsonArrayBuilder builder, final Object value) {
    return builder.add(createValue(value));
  }

  /**
   * Returns a transformer that adds or replaces the value of the field designated by the
   * dot-separated <code>path</code> with <code>value</code>.
   *
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The transformer.
   */
  public static Transformer addTransformer(final String path, final JsonValue value) {
    final String parent = getParentPath(path);

    return new Transformer(
        e -> e.path.equals(parent) && isObject(e.value),
        e ->
            getLastSegment(path, ".")
                .map(
                    s ->
                        new JsonEntry(
                            e.path,
                            createObjectBuilder(e.value.asJsonObject()).add(s, value).build())));
  }

  /**
   * Returns a transformer that adds or replaces the value of the field designated by the
   * dot-separated <code>path</code> with <code>value</code>.
   *
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The transformer.
   */
  public static Transformer addTransformer(final String path, final Object value) {
    return addTransformer(path, createValue(value));
  }

  public static JsonArray asArray(final JsonValue value) {
    if (value.getValueType() != JsonValue.ValueType.ARRAY) {
      throw new JsonException("Not an array");
    }

    return (JsonArray) value;
  }

  public static Instant asInstant(final JsonValue value) {
    return parse(asString(value).getString());
  }

  public static JsonNumber asNumber(final JsonValue value) {
    if (value.getValueType() != JsonValue.ValueType.NUMBER) {
      throw new JsonException("Not a number");
    }

    return (JsonNumber) value;
  }

  public static JsonObject asObject(final JsonValue value) {
    if (value.getValueType() != JsonValue.ValueType.OBJECT) {
      throw new JsonException("Not an object");
    }

    return (JsonObject) value;
  }

  public static JsonString asString(final JsonValue value) {
    if (value.getValueType() != JsonValue.ValueType.STRING) {
      throw new JsonException("Not a string");
    }

    return (JsonString) value;
  }

  public static boolean changed(final JsonArray patch, final String jsonPointer) {
    return changes(patch, jsonPointer, false).findFirst().isPresent();
  }

  public static boolean changed(
      final JsonArray patch,
      final JsonObject original,
      final String jsonPointer,
      final JsonValue from,
      final JsonValue to) {
    final JsonObject[] changes = changes(patch, jsonPointer, true).toArray(JsonObject[]::new);

    return isRemoveThenAdd(changes, original, from, to)
        || isReplace(changes, original, from, to)
        || isMove(changes, original, from, to);
  }

  public static boolean changed(
      final JsonArray patch,
      final JsonObject original,
      final String jsonPointer,
      final JsonValue to) {
    final JsonObject[] changes = changes(patch, jsonPointer, true).toArray(JsonObject[]::new);

    return isRemoveThenAdd(changes, to) || isReplace(changes, to) || isMove(changes, original, to);
  }

  private static Stream<JsonObject> changes(final JsonArray patch) {
    return patch.stream()
        .filter(Json::isObject)
        .map(JsonValue::asJsonObject)
        .filter(json -> json.containsKey(OP));
  }

  private static Stream<JsonObject> changes(
      final JsonArray patch, final String jsonPointer, final boolean exact) {
    return changes(patch, jsonPointer, exact, OPS);
  }

  private static Stream<JsonObject> changes(
      final JsonArray patch, final String jsonPointer, final boolean exact, final Set<String> ops) {
    return changes(patch)
        .filter(json -> ops.contains(json.getString(OP)))
        .filter(json -> comparePaths(json.getString(PATH, ""), jsonPointer, exact));
  }

  private static boolean comparePaths(final String found, final String given, final boolean exact) {
    return exact ? found.equals(given) : found.startsWith(given);
  }

  private static Object convertNumber(final JsonNumber number) {
    return number.isIntegral() ? (Object) number.longValue() : (Object) number.doubleValue();
  }

  public static JsonObjectBuilder copy(final JsonObject obj, final JsonObjectBuilder builder) {
    return copy(obj, builder, key -> true);
  }

  public static JsonObjectBuilder copy(
      final JsonObject obj, final JsonObjectBuilder builder, final Predicate<String> retain) {
    return copy(obj, builder, (key, o) -> retain.test(key));
  }

  public static JsonObjectBuilder copy(
      final JsonObject obj,
      final JsonObjectBuilder builder,
      final BiPredicate<String, JsonObject> retain) {
    return obj.keySet().stream()
        .filter(key -> retain.test(key, obj))
        .reduce(builder, (b, key) -> b.add(key, obj.get(key)), (b1, b2) -> b1);
  }

  public static JsonArrayBuilder copy(
      final JsonArray array, final JsonArrayBuilder builder, final Predicate<JsonValue> retain) {
    return array.stream().filter(retain).reduce(builder, JsonArrayBuilder::add, (b1, b2) -> b1);
  }

  public static JsonObject createErrorObject(final JsonValue value, final String message) {
    return Optional.of(createObjectBuilder())
        .map(builder -> builder.add(ERROR, true))
        .map(builder -> builder.add("message", message))
        .map(builder -> value != null ? builder.add("value", value) : builder)
        .map(JsonObjectBuilder::build)
        .orElse(emptyObject());
  }

  public static JsonValue createValue(final Object value) {
    if (value == null) {
      return JsonValue.NULL;
    }

    if (value instanceof Boolean) {
      return ((boolean) value) ? JsonValue.TRUE : JsonValue.FALSE;
    }

    if (value instanceof Integer) {
      return javax.json.Json.createValue((int) value);
    }

    if (value instanceof Long) {
      return javax.json.Json.createValue((long) value);
    }

    if (value instanceof BigInteger) {
      return javax.json.Json.createValue((BigInteger) value);
    }

    if (value instanceof BigDecimal) {
      return javax.json.Json.createValue((BigDecimal) value);
    }

    if (value instanceof Double || value instanceof Float) {
      return javax.json.Json.createValue(((Number) value).doubleValue());
    }

    if (value instanceof Date) {
      return javax.json.Json.createValue(ofEpochMilli(((Date) value).getTime()).toString());
    }

    if (value instanceof Map) {
      return from((Map) value);
    }

    if (value instanceof List) {
      return from((List) value);
    }

    return javax.json.Json.createValue(value.toString());
  }

  public static JsonArray emptyArray() {
    return createArrayBuilder().build();
  }

  public static JsonObject emptyObject() {
    return createObjectBuilder().build();
  }

  public static Object evaluate(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.NUMBER
        ? (Object) asNumber(value).longValue()
        : toString(value);
  }

  public static JsonObject from(final Map<String, ?> fields) {
    return add(createObjectBuilder(), fields).build();
  }

  public static JsonArray from(final List<?> values) {
    return values.stream().reduce(createArrayBuilder(), Json::addJsonField, (b1, b2) -> b1).build();
  }

  public static Optional<JsonStructure> from(final String json) {
    return tryToGetSilent(() -> createReader(new StringReader(json)).read());
  }

  public static Optional<JsonArray> getArray(final JsonStructure json, final String jsonPointer) {
    return getValue(json, jsonPointer).filter(Json::isArray).map(JsonValue::asJsonArray);
  }

  /**
   * Returns the value for <code>field</code>, which may be dot-separated.
   *
   * @param obj the given JSON object.
   * @param field the query field.
   * @return The optional result value.
   */
  public static Optional<JsonValue> get(final JsonObject obj, final String field) {
    return pathSearch(obj, field.split("\\."));
  }

  public static Optional<Boolean> getBoolean(final JsonStructure json, final String jsonPointer) {
    return getValue(json, jsonPointer)
        .filter(v -> v.getValueType() == TRUE || v.getValueType() == FALSE)
        .map(v -> v.getValueType() == TRUE);
  }

  private static Stream<String> getFieldVariants(final String field) {
    return allPaths(field, ".");
  }

  public static Optional<Instant> getInstant(final JsonStructure json, final String jsonPointer) {
    return getValue(json, jsonPointer).filter(Json::isInstant).map(Json::asInstant);
  }

  /**
   * Returns the last segment of a dot-separated path.
   *
   * @param path the given path.
   * @return The last segment.
   */
  public static String getKey(final String path) {
    return getKey(path, null);
  }

  private static String getKey(final String path, final String originalKey) {
    return Optional.ofNullable(originalKey)
        .map(path::lastIndexOf)
        .filter(index -> index != -1)
        .map(index -> originalKey)
        .orElse(
            Optional.of(path.split("\\."))
                .filter(parts -> parts.length > 0)
                .map(parts -> parts[parts.length - 1])
                .orElse(path));
  }

  private static Set<String> getMandatoryKeys(final Set<String> all, final String parent) {
    return parent == null
        ? all.stream().filter(key -> key.indexOf('.') == -1).collect(toSet())
        : all.stream()
            .filter(key -> key.startsWith(parent + "."))
            .map(key -> key.substring(parent.length() + 1))
            .filter(key -> key.indexOf('.') == -1)
            .collect(toSet());
  }

  private static String getMessage(final Map<String, String> messages, final String field) {
    return getFieldVariants(field)
        .filter(messages::containsKey)
        .map(messages::get)
        .findFirst()
        .orElse("Error");
  }

  public static Optional<Double> getNumber(final JsonStructure json, final String jsonPointer) {
    return getValue(json, jsonPointer)
        .filter(Json::isNumber)
        .map(Json::asNumber)
        .map(JsonNumber::doubleValue);
  }

  public static Stream<Double> getNumbers(final JsonObject json, final String array) {
    return getValues(json, array)
        .filter(Json::isNumber)
        .map(Json::asNumber)
        .map(JsonNumber::doubleValue);
  }

  public static Optional<JsonObject> getObject(final JsonStructure json, final String jsonPointer) {
    return getValue(json, jsonPointer).filter(Json::isObject).map(JsonValue::asJsonObject);
  }

  public static Stream<JsonObject> getObjects(final JsonObject json, final String array) {
    return getValues(json, array).filter(Json::isObject).map(JsonValue::asJsonObject);
  }

  /**
   * Returns the parent path of a dot-separated path, or the empty string if the path has only one
   * segment.
   *
   * @param path the given dot-separated path.
   * @return The dot-separated parent path.
   */
  public static String getParentPath(final String path) {
    return Optional.of(path.lastIndexOf('.'))
        .filter(i -> i != -1)
        .map(i -> path.substring(0, i))
        .orElse("");
  }

  private static String getPath(final String parent, final String key) {
    return (parent != null && !"".equals(parent) ? (parent + ".") : "") + key;
  }

  public static Optional<String> getString(final JsonStructure json, final String jsonPointer) {
    return getValue(json, jsonPointer)
        .filter(Json::isString)
        .map(Json::asString)
        .map(JsonString::getString);
  }

  public static Stream<String> getStrings(final JsonObject json, final String array) {
    return getValues(json, array)
        .filter(Json::isString)
        .map(Json::asString)
        .map(JsonString::getString);
  }

  private static Validator getValidator(
      final Map<String, Validator> validators, final String field) {
    return getFieldVariants(field)
        .filter(validators::containsKey)
        .map(validators::get)
        .findFirst()
        .orElse(null);
  }

  public static Optional<JsonValue> getValue(final JsonStructure json, final String jsonPointer) {
    return tryToGetSilent(() -> json.getValue(jsonPointer));
  }

  public static Stream<JsonValue> getValues(final JsonObject json, final String array) {
    return Optional.ofNullable(json.getJsonArray(array))
        .map(JsonArray::stream)
        .orElseGet(Stream::empty);
  }

  /**
   * Returns <code>true</code> if <code>obj</code> contains an entry with the name _error and value
   * <code>true</code>.
   *
   * @param obj the given JSON object.
   * @return Whether the object contains errors or not.
   */
  public static boolean hasErrors(final JsonObject obj) {
    return obj.get(ERROR) != null && obj.getBoolean(ERROR, false);
  }

  /**
   * Returns <code>true</code> if any object in <code>array</code> contains an entry with the name
   * _error and value <code>true</code>.
   *
   * @param array the given JSON array.
   * @return Whether the array contains errors or not.
   */
  public static boolean hasErrors(final JsonArray array) {
    return array.stream()
        .anyMatch(value -> value instanceof JsonObject && hasErrors((JsonObject) value));
  }

  public static boolean hasErrors(final JsonStructure json) {
    return (json instanceof JsonObject && hasErrors((JsonObject) json))
        || (json instanceof JsonArray && hasErrors((JsonArray) json));
  }

  public static boolean isArray(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.ARRAY;
  }

  public static ValidationResult isArray(final ValidationContext context) {
    return new ValidationResult(isArray(context.value), null);
  }

  public static boolean isBoolean(final JsonValue value) {
    return value.getValueType() == TRUE || value.getValueType() == FALSE;
  }

  public static ValidationResult isBoolean(final ValidationContext context) {
    return new ValidationResult(isBoolean(context.value), null);
  }

  public static boolean isDate(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.STRING
        && net.pincette.util.Util.isDate(asString(value).getString());
  }

  public static ValidationResult isDate(final ValidationContext context) {
    return new ValidationResult(isDate(context.value), null);
  }

  public static boolean isEmail(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.STRING
        && net.pincette.util.Util.isEmail(asString(value).getString());
  }

  public static ValidationResult isEmail(final ValidationContext context) {
    return new ValidationResult(isEmail(context.value), null);
  }

  public static boolean isInstant(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.STRING
        && net.pincette.util.Util.isInstant(asString(value).getString());
  }

  public static ValidationResult isInstant(final ValidationContext context) {
    return new ValidationResult(isInstant(context.value), null);
  }

  private static boolean isMove(
      final JsonObject[] changes,
      final JsonObject original,
      final JsonValue from,
      final JsonValue to) {
    return changes.length == 1
        && changes[0].getString("op").equals("move")
        && isValue(changes[0], "path", original, from)
        && isValue(changes[0], "from", original, to);
  }

  private static boolean isMove(
      final JsonObject[] changes, final JsonObject original, final JsonValue to) {
    return changes.length == 1
        && changes[0].getString("op").equals("move")
        && isValue(changes[0], "from", original, to);
  }

  public static boolean isNull(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.NULL;
  }

  public static boolean isNumber(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.NUMBER;
  }

  public static ValidationResult isNumber(final ValidationContext context) {
    return new ValidationResult(isNumber(context.value), null);
  }

  public static boolean isObject(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.OBJECT;
  }

  public static ValidationResult isObject(final ValidationContext context) {
    return new ValidationResult(isObject(context.value), null);
  }

  private static boolean isRemove(final JsonObject change, final String jsonPointer) {
    return Optional.of(change.getString(OP))
        .map(
            op ->
                (op.equals(REMOVE) && isSubject(change, PATH, jsonPointer))
                    || (op.equals(MOVE) && isSubject(change, FROM, jsonPointer)))
        .orElse(false);
  }

  private static boolean isRemoveThenAdd(
      final JsonObject[] changes,
      final JsonObject original,
      final JsonValue from,
      final JsonValue to) {
    return changes.length == 2
        && changes[0].getString(OP).equals(REMOVE)
        && changes[1].getString(OP).equals(ADD)
        && changes[1].getValue(VALUE).equals(to)
        && isValue(changes[0], PATH, original, from);
  }

  private static boolean isRemoveThenAdd(final JsonObject[] changes, final JsonValue to) {
    return changes.length == 2
        && changes[0].getString(OP).equals(REMOVE)
        && changes[1].getString(OP).equals(ADD)
        && changes[1].getValue(VALUE).equals(to);
  }

  private static boolean isReplace(
      final JsonObject[] changes,
      final JsonObject original,
      final JsonValue from,
      final JsonValue to) {
    return changes.length == 1
        && changes[0].getString(OP).equals(REPLACE)
        && changes[0].getValue(VALUE).equals(to)
        && isValue(changes[0], PATH, original, from);
  }

  private static boolean isReplace(final JsonObject[] changes, final JsonValue to) {
    return changes.length == 1
        && changes[0].getString(OP).equals(REPLACE)
        && changes[0].getValue(VALUE).equals(to);
  }

  public static boolean isSet(final JsonArray patch, final String jsonPointer) {
    return changes(patch, jsonPointer, true, SET_OPS).findFirst().isPresent();
  }

  private static boolean isSet(final JsonObject change, final String jsonPointer) {
    return Optional.of(change.getString(OP))
        .map(op -> SET_OPS.contains(op) && isSubject(change, PATH, jsonPointer))
        .orElse(false);
  }

  public static boolean isString(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.STRING;
  }

  public static ValidationResult isString(final ValidationContext context) {
    return new ValidationResult(isString(context.value), null);
  }

  private static boolean isSubject(
      final JsonObject change, final String attribute, final String jsonPointer) {
    return Optional.ofNullable(change.getString(attribute, ""))
        .map(value -> value.equals(jsonPointer))
        .orElse(false);
  }

  public static boolean isUri(final JsonValue value) {
    return isUri(asString(value).getString());
  }

  public static boolean isUri(final String s) {
    return s.startsWith("/") || net.pincette.util.Util.isUri(s);
  }

  public static ValidationResult isUri(final ValidationContext context) {
    return new ValidationResult(isUri(context.value), null);
  }

  private static boolean isValue(
      final JsonObject change,
      final String attribute,
      final JsonObject original,
      final JsonValue value) {
    return getValue(original, change.getString(attribute)).filter(v -> v.equals(value)).isPresent();
  }

  /**
   * Returns a stream of nested objects in document order.
   *
   * @param json the structure to navigate.
   * @return The stream of found objects.
   */
  public static Stream<JsonObject> nestedObjects(final JsonStructure json) {
    return isArray(json) ? nestedObjects(json.asJsonArray()) : nestedObjects(json.asJsonObject());
  }

  public static Stream<JsonObject> nestedObjects(final JsonObject json) {
    return nestedObjectsAndSelf(json.entrySet().stream().map(Map.Entry::getValue));
  }

  public static Stream<JsonObject> nestedObjects(final JsonArray json) {
    return nestedObjectsAndSelf(json.stream());
  }

  private static Stream<JsonObject> nestedObjectsAndSelf(final Stream<JsonValue> stream) {
    return stream
        .filter(j -> isObject(j) || isArray(j))
        .flatMap(
            j ->
                isObject(j)
                    ? concat(of(j.asJsonObject()), nestedObjects(j.asJsonObject()))
                    : nestedObjects(j.asJsonArray()));
  }

  /**
   * Returns a transformer that does nothing.
   *
   * @return The transformer.
   */
  public static Transformer nopTransformer() {
    return new Transformer(e -> false, Optional::of);
  }

  public static JsonObject remove(final JsonObject obj, final Set<String> fields) {
    return remove(obj, fields::contains);
  }

  public static JsonObject remove(final JsonObject obj, final Predicate<String> pred) {
    return copy(obj, createObjectBuilder(), key -> !pred.test(key)).build();
  }

  public static JsonArray remove(final JsonArray array, final Predicate<JsonValue> pred) {
    return copy(array, createArrayBuilder(), value -> !pred.test(value)).build();
  }

  /**
   * Removes fields with a name that starts with an underscore.
   *
   * @param obj the given JSON object.
   * @return The new JSON object without the technical fields.
   */
  public static JsonObject removeTechnical(final JsonObject obj) {
    return copy(obj, createObjectBuilder(), key -> !key.startsWith("_")).build();
  }

  public static Transformer removeTransformer(final String path) {
    return new Transformer(e -> e.path.equals(path), e -> Optional.empty());
  }

  public static boolean removed(final JsonArray patch, final String jsonPointer) {
    return last(removedOrSet(patch, jsonPointer))
        .map(change -> isRemove(change, jsonPointer))
        .orElse(false);
  }

  private static Stream<JsonObject> removedOrSet(final JsonArray patch, final String jsonPointer) {
    return changes(patch)
        .filter(change -> isRemove(change, jsonPointer) || isSet(change, jsonPointer));
  }

  /**
   * Returns a new object in which the value of the existing field designated by the dot-separated
   * <code>path</code> is replaced with <code>value</code>.
   *
   * @param obj the given JSON object.
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The new object.
   */
  public static JsonObject set(final JsonObject obj, final String path, final JsonValue value) {
    return transform(obj, setTransformer(path, value));
  }

  /**
   * Returns a new object in which the value of the existing field designated by the dot-separated
   * <code>path</code> is replaced with <code>value</code>.
   *
   * @param obj the given JSON object.
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The new object.
   */
  public static JsonObject set(final JsonObject obj, final String path, final Object value) {
    return set(obj, path, createValue(value));
  }

  /**
   * Returns a transformer that replaces the value of the existing field designated by the
   * dot-separated <code>path</code> with <code>value</code>.
   *
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The transformer.
   */
  public static Transformer setTransformer(final String path, final JsonValue value) {
    return new Transformer(
        e -> e.path.equals(path), e -> Optional.of(new JsonEntry(e.path, value)));
  }

  /**
   * Returns a transformer that replaces the value of the existing field designated by the
   * dot-separated <code>path</code> with <code>value</code>.
   *
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The transformer.
   */
  public static Transformer setTransformer(final String path, final Object value) {
    return setTransformer(path, createValue(value));
  }

  public static String string(final JsonStructure json) {
    return string(json, false);
  }

  public static String string(final JsonStructure json, final boolean pretty) {
    final Map<String, Object> config = new HashMap<>();
    final StringWriter writer = new StringWriter();

    if (pretty) {
      config.put(JsonGenerator.PRETTY_PRINTING, true);
    }

    tryToDoWith(() -> createWriterFactory(config).createWriter(writer), w -> w.write(json));

    return writer.toString();
  }

  /**
   * Transforms a JSON pointer in the form "/a/b/c" into a dot-separated field in the form "a.b.c".
   *
   * @param jsonPointer the given pointer.
   * @return The dot-separated field.
   */
  public static String toDotSeparated(final String jsonPointer) {
    return getSegments(jsonPointer, "/").collect(joining("."));
  }

  /**
   * Transforms a dot-separated field in the form "a.b.c" into a JSON pointer in the form "/a/b/c".
   *
   * @param dotSeparatedField the given field.
   * @return The JSON pointer.
   */
  public static String toJsonPointer(final String dotSeparatedField) {
    return "/" + join("/", dotSeparatedField.split("\\."));
  }

  /**
   * Converts <code>value</code> recursively to a Java value.
   *
   * @param value the given value.
   * @return The converted value.
   */
  public static Object toNative(final JsonValue value) {
    switch (value.getValueType()) {
      case ARRAY:
        return toNative(asArray(value));
      case FALSE:
        return false;
      case TRUE:
        return true;
      case NUMBER:
        return convertNumber(asNumber(value));
      case OBJECT:
        return toNative(asObject(value));
      case STRING:
        return asString(value).getString();
      case NULL:
        return null;
      default:
        return value;
    }
  }

  /**
   * Converts <code>array</code> recursively to a list with Java values.
   *
   * @param array the given array.
   * @return The generated list.
   */
  public static List<Object> toNative(final JsonArray array) {
    return array.stream().map(Json::toNative).collect(toList());
  }

  /**
   * Converts <code>object</code> recursively to a map with Java values.
   *
   * @param object the given object.
   * @return The generated map.
   */
  public static Map<String, Object> toNative(final JsonObject object) {
    return object.entrySet().stream()
        .collect(toMap(Map.Entry::getKey, e -> toNative(e.getValue())));
  }

  private static String toString(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.STRING
        ? asString(value).getString()
        : value.toString();
  }

  /**
   * Returns a new value where recursively entries that <code>match</code> are transformed by <code>
   * transformer</code>. If the latter is empty the entry is removed from the result.
   *
   * @param json the given JSON value.
   * @param transformer the applied transformer.
   * @return The new JSON value.
   */
  public static JsonValue transform(final JsonValue json, final Transformer transformer) {
    return transform(json, null, transformer);
  }

  private static JsonValue transform(
      final JsonValue json, final String parent, final Transformer transformer) {
    return json instanceof JsonStructure
        ? transform((JsonStructure) json, parent, transformer)
        : json;
  }

  /**
   * Returns a new structure where recursively entries that <code>match</code> are transformed by
   * <code>transformer</code>. If the latter is empty the entry is removed from the result.
   *
   * @param json the given JSON structure.
   * @param transformer the applied transformer.
   * @return The new JSON structure.
   */
  public static JsonStructure transform(final JsonStructure json, final Transformer transformer) {
    return transform(json, null, transformer);
  }

  private static JsonStructure transform(
      final JsonStructure json, final String parent, final Transformer transformer) {
    return json instanceof JsonArray
        ? transform((JsonArray) json, parent, transformer)
        : transform((JsonObject) json, parent, transformer);
  }

  /**
   * Returns a new array where entries of objects that <code>match</code> are transformed by <code>
   * transformer</code>. If the latter is empty the entry is removed from the result.
   *
   * @param array the given JSON array.
   * @param transformer the applied transformer.
   * @return The new JSON array.
   */
  public static JsonArray transform(final JsonArray array, final Transformer transformer) {
    return transform(array, null, transformer);
  }

  private static JsonArray transform(
      final JsonArray array, final String parent, final Transformer transformer) {
    return array.stream()
        .filter(Objects::nonNull)
        .reduce(
            createArrayBuilder(),
            (b, v) -> b.add(transform(v, parent, transformer)),
            (b1, b2) -> b1)
        .build();
  }

  /**
   * Returns a new object where entries that <code>match</code> are transformed by <code>transformer
   * </code>. If the latter is empty the entry is removed from the result.
   *
   * @param obj the given JSON object.
   * @param transformer the applied transformer.
   * @return The new JSON object.
   */
  public static JsonObject transform(final JsonObject obj, final Transformer transformer) {
    return transform(obj, null, transformer);
  }

  private static JsonObject transform(
      final JsonObject obj, final String parent, final Transformer transformer) {
    return concat(parent == null ? of("") : empty(), obj.keySet().stream())
        .reduce(
            createObjectBuilder(),
            (b, k) ->
                transformer
                    .run(
                        "".equals(k)
                            ? new JsonEntry("", obj)
                            : new JsonEntry(getPath(parent, k), obj.get(k)))
                    .map(
                        entry ->
                            new JsonEntry(
                                getPath(parent, getKey(entry.path, k)),
                                transform(
                                    entry.value,
                                    getPath(parent, getKey(entry.path, k)),
                                    transformer)))
                    .map(
                        entry ->
                            "".equals(k)
                                ? copy(entry.value.asJsonObject(), b)
                                : b.add(getKey(entry.path, k), entry.value))
                    .orElse(b),
            (b1, b2) -> b1)
        .build();
  }

  public static InputStream transformToXML(final JsonObject json) {
    return Optional.of(new ByteArrayOutputStream())
        .map(
            out ->
                SideEffect.<ByteArrayOutputStream>run(
                        () ->
                            tryToDoWithRethrow(
                                autoClose(
                                    () -> newInstance().createXMLEventWriter(out),
                                    XMLEventWriter::close),
                                writer ->
                                    writer.add(
                                        new JsonEventReader(
                                            createParserFactory(null).createParser(json)))))
                    .andThenGet(() -> out))
        .map(ByteArrayOutputStream::toByteArray)
        .map(ByteArrayInputStream::new)
        .orElse(null);
  }

  /**
   * The method validates <code>value</code>. If there are no errors <code>value</code> is returned
   * unchanged. Otherwise the fields for which there is an error are replaced with an object
   * containing the field "value" with the original value, the field "message" with an error message
   * and the field "_error", which is set to <code>true</code>. All ancestor objects will also have
   * the field _error set to <code>true</code>.
   *
   * @param value the object or array that is to be validated.
   * @param context the context information which is passed to each validator.
   * @param validators maps dot-separated fields to validator functions.
   * @param messages maps dot-separated fields to error messages.
   * @param mandatory the set of dot-separated fields that are must appear in <code>value</code>.
   *     When a field has several segments this only says something about the lowest level. For
   *     example, when the field "a.b" is present that "b" must be present when "a" is, but "a" as
   *     such doesn't have to be present. If that is also required then the field "a" should also be
   *     in the set.
   * @param missingMessage a general error message for missing fields.
   * @return An annotated copy of <code>value</code> when there is at least one error, just <code>
   *     value</code> otherwise.
   */
  public static JsonStructure validate(
      final JsonStructure value,
      final ValidationContext context,
      final Map<String, Validator> validators,
      final Map<String, String> messages,
      final Set<String> mandatory,
      final String missingMessage) {
    return (JsonStructure)
        validate(null, value, context, validators, messages, mandatory, missingMessage).first;
  }

  public static JsonObject validate(
      final JsonObject obj,
      final ValidationContext context,
      final Map<String, Validator> validators,
      final Map<String, String> messages,
      final Set<String> mandatory,
      final String missingMessage) {
    return (JsonObject)
        validate(null, obj, context, validators, messages, mandatory, missingMessage).first;
  }

  public static JsonArray validate(
      final JsonArray array,
      final ValidationContext context,
      final Map<String, Validator> validators,
      final Map<String, String> messages,
      final Set<String> mandatory,
      final String missingMessage) {
    return validate(null, array, context, validators, messages, mandatory, missingMessage).first;
  }

  private static Pair<? extends JsonValue, Boolean> validate(
      final String field,
      final JsonValue value,
      final ValidationContext context,
      final Map<String, Validator> validators,
      final Map<String, String> messages,
      final Set<String> mandatory,
      final String missingMessage) {
    final Function<JsonValue, Pair<? extends JsonValue, Boolean>> ifArrayOr =
        v ->
            v instanceof JsonArray
                ? validate(
                    field,
                    v.asJsonArray(),
                    context,
                    validators,
                    messages,
                    mandatory,
                    missingMessage)
                : pair(v, false);

    return value instanceof JsonObject
        ? validate(
            field, value.asJsonObject(), context, validators, messages, mandatory, missingMessage)
        : ifArrayOr.apply(value);
  }

  private static Pair<JsonValue, Boolean> validate(
      final String parent,
      final JsonObject obj,
      final ValidationContext context,
      final Map<String, Validator> validators,
      final Map<String, String> messages,
      final Set<String> mandatory,
      final String missingMessage) {
    final JsonObjectBuilder builder = createObjectBuilder();
    final Set<String> found = new HashSet<>();
    boolean errors =
        obj.keySet().stream()
            .map(
                key -> {
                  final String field = parent != null ? (parent + "." + key) : key;
                  final JsonValue value = obj.get(key);
                  final ValidationResult result =
                      Optional.ofNullable(getValidator(validators, field))
                          .map(validator -> validator.apply(context.with(field).with(value)))
                          .orElse(new ValidationResult(true, null));
                  final Supplier<String> message =
                      () -> result.message != null ? result.message : getMessage(messages, field);
                  final Pair<? extends JsonValue, Boolean> entry =
                      result.status
                          ? validate(
                              field,
                              value,
                              context,
                              validators,
                              messages,
                              mandatory,
                              missingMessage)
                          : pair(createErrorObject(value, message.get()), true);

                  found.add(key);
                  builder.add(key, entry.first);

                  return entry.second;
                })
            .reduce(false, (e1, e2) -> e1 || e2);

    errors |=
        difference(getMandatoryKeys(mandatory, parent), found).stream()
            .map(
                key -> {
                  builder.add(key, createErrorObject(null, missingMessage));

                  return true;
                })
            .reduce(false, (e1, e2) -> e1 || e2);

    if (errors) {
      builder.add(ERROR, true);
    }

    return pair(builder.build(), errors);
  }

  private static Pair<JsonArray, Boolean> validate(
      final String parent,
      final JsonArray array,
      final ValidationContext context,
      final Map<String, Validator> validators,
      final Map<String, String> messages,
      final Set<String> mandatory,
      final String missingMessage) {
    final JsonArrayBuilder builder = createArrayBuilder();
    final boolean errors =
        array.stream()
            .map(
                value -> {
                  final Pair<? extends JsonValue, Boolean> entry =
                      validate(
                          parent, value, context, validators, messages, mandatory, missingMessage);

                  builder.add(entry.first);

                  return entry.second;
                })
            .reduce(false, (e1, e2) -> e1 || e2);

    return pair(builder.build(), errors);
  }

  public interface Validator extends Function<ValidationContext, ValidationResult> {} // Validator

  public static class JsonEntry {
    /** A dot-separated key path. */
    public final String path;

    /** A JSON value. */
    public final JsonValue value;

    /**
     * @param path a dot-separated key path.
     * @param value a JSON value.
     */
    public JsonEntry(final String path, final JsonValue value) {
      this.path = path;
      this.value = value;
    }
  }

  public static class Transformer {
    public final Predicate<JsonEntry> match;
    public final Transformer next;
    public final Function<JsonEntry, Optional<JsonEntry>> transform;

    /**
     * If the transform function returns an empty <code>Optional</code> the entry is removed from
     * the result.
     *
     * @param match the function to test an entry. When <code>true</code> is returned the <code>
     *     transform</code> function will be executed.
     * @param transform the transform function.
     */
    public Transformer(
        final Predicate<JsonEntry> match,
        final Function<JsonEntry, Optional<JsonEntry>> transform) {
      this.match = match;
      this.next = null;
      this.transform = transform;
    }

    private Transformer(final Transformer me, final Transformer next) {
      this.match = me.match;
      this.transform = me.transform;
      this.next = me.next != null ? new Transformer(me.next, next) : next;
    }

    public Optional<JsonEntry> run(final JsonEntry entry) {
      return runNext(!match.test(entry) ? Optional.of(entry) : transform.apply(entry));
    }

    private Optional<JsonEntry> runNext(final Optional<JsonEntry> entry) {
      return next != null ? entry.flatMap(next::run) : entry;
    }

    public Transformer thenApply(final Transformer transformer) {
      return new Transformer(this, transformer);
    }
  }

  public static class ValidationContext {
    public final String field;
    public final JsonStructure newJson;
    public final JsonStructure oldJson;
    public final JsonValue value;

    public ValidationContext(final JsonStructure oldJson, final JsonStructure newJson) {
      this(oldJson, newJson, null, null);
    }

    private ValidationContext(
        final JsonStructure oldJson,
        final JsonStructure newJson,
        final String field,
        final JsonValue value) {
      this.oldJson = oldJson;
      this.newJson = newJson;
      this.field = field;
      this.value = value;
    }

    private ValidationContext with(final String field) {
      return new ValidationContext(oldJson, newJson, field, value);
    }

    private ValidationContext with(final JsonValue value) {
      return new ValidationContext(oldJson, newJson, field, value);
    }
  }

  public static class ValidationResult {
    public final String message;
    public final boolean status;

    public ValidationResult(final boolean status) {
      this(status, null);
    }

    public ValidationResult(final boolean status, final String message) {
      this.status = status;
      this.message = message;
    }
  }
}
