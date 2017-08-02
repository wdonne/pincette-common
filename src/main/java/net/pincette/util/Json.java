package net.pincette.util;

import net.pincette.xml.stream.JsonEventReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createParserFactory;
import static javax.json.Json.createReader;
import static javax.json.Json.createWriterFactory;
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

import static javax.xml.stream.XMLOutputFactory.newInstance;
import static net.pincette.util.Collections.list;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.Util.pathSearch;
import static net.pincette.util.Util.takeWhile;
import static net.pincette.util.Util.tryToGetSilent;
import static net.pincette.util.Collections.difference;



/**
 * Some JSON-utilities.
 * @author Werner Donn\u00e9
 */

public class Json

{

  final public static Function<JsonObject,?> EVALUATOR =
    value ->
      value.getValueType() == JsonValue.ValueType.NUMBER ?
        asNumber(value).longValue() :
        (
          value.getValueType() == JsonValue.ValueType.STRING ?
            asString(value).getString() : value.toString()
        );



  public static JsonObject
  add(final JsonObject obj, final String name, final JsonArrayBuilder value)
  {
    return add(obj, builder -> builder.add(name, value));
  }



  public static JsonObject
  add(final JsonObject obj, final String name, final JsonObjectBuilder value)
  {
    return add(obj, builder -> builder.add(name, value));
  }



  public static JsonObject
  add(final JsonObject obj, final String name, final JsonValue value)
  {
    return add(obj, builder -> builder.add(name, value));
  }



  public static JsonObject
  add(final JsonObject obj, final String name, final String value)
  {
    return add(obj, builder -> builder.add(name, value));
  }



  public static JsonObject
  add(final JsonObject obj, final String name, final boolean value)
  {
    return add(obj, builder -> builder.add(name, value));
  }



  public static JsonObject
  add(final JsonObject obj, final String name, final double value)
  {
    return add(obj, builder -> builder.add(name, value));
  }



  public static JsonObject
  add(final JsonObject obj, final String name, final int value)
  {
    return add(obj, builder -> builder.add(name, value));
  }



  public static JsonObject
  add(final JsonObject obj, final String name, final long value)
  {
    return add(obj, builder -> builder.add(name, value));
  }



  public static JsonObject
  add(final JsonObject obj, final String name, final BigDecimal value)
  {
    return add(obj, builder -> builder.add(name, value));
  }



  public static JsonObject
  add(final JsonObject obj, final String name, final BigInteger value)
  {
    return add(obj, builder -> builder.add(name, value));
  }



  public static JsonObject
  add(final JsonObject obj, final Consumer<JsonObjectBuilder> add)
  {
    final JsonObjectBuilder builder = createObjectBuilder();

    copy(obj, builder);
    add.accept(builder);

    return builder.build();
  }



  public static JsonObject
  add(final JsonObject obj1, final JsonObject obj2)
  {
    return add(obj1, builder -> copy(obj2, builder));
  }



  public static JsonObject
  add(final JsonObject obj, final Map<String,?> fields)
  {
    return add(obj, builder -> add(builder, fields));
  }



  public static JsonObjectBuilder
  add
  (
    final JsonObjectBuilder builder,
    final Map<String,?> fields
  )
  {
    fields.
      entrySet().
      forEach(entry -> addJsonField(builder, entry.getKey(), entry.getValue()));

    return builder;
  }



  public static JsonObjectBuilder
  add(final JsonObjectBuilder builder, final JsonObject obj)
  {
    copy(obj, builder);

    return builder;
  }



  public static JsonArrayBuilder
  add(final JsonArrayBuilder builder, final JsonArray array)
  {
    array.forEach(builder::add);

    return builder;
  }



  /**
   * Returns a copy of the object where the values of the fields in
   * <code>values</code> are replaced. The first entry of a pair is the
   * name path and the second entry is the new value.
   * @param obj the given JSON object.
   * @param values the list of pairs, where the first entry is the
   *               name and the second entry is the new value.
   * @return The new object.
   */

  public static JsonObject
  add(final JsonObject obj, Collection<Pair<String,Object>> values)
  {
    final JsonObjectBuilder builder = createObjectBuilder();

    copy(obj, builder);
    values.forEach(pair -> addJsonField(builder, pair.first, pair.second));

    return builder.build();
  }



  public static JsonObjectBuilder
  addJsonField
  (
    final JsonObjectBuilder builder,
    final String name,
    final Object value
  )
  {
    if (value instanceof Boolean)
    {
      builder.add(name, (boolean) value);
    }
    else
    {
      if (value instanceof Integer)
      {
        builder.add(name, (int) value);
      }
      else
      {
        if (value instanceof Long)
        {
          builder.add(name, (long) value);
        }
        else
        {
          if (value instanceof BigInteger)
          {
            builder.add(name, (BigInteger) value);
          }
          else
          {
            if (value instanceof BigDecimal)
            {
              builder.add(name, (BigDecimal) value);
            }
            else
            {
              if (value instanceof Double)
              {
                builder.add(name, (double) value);
              }
              else
              {
                if (value instanceof Float)
                {
                  builder.add(name, (float) value);
                }
                else
                {
                  if (value instanceof Date)
                  {
                    builder.add
                    (
                      name,
                      Instant.ofEpochMilli(((Date) value).getTime()).toString()
                    );
                  }
                  else
                  {
                    if (value instanceof Map)
                    {
                      builder.add(name, from((Map) value));
                    }
                    else
                    {
                      if (value instanceof List)
                      {
                        builder.add(name, from((List) value));
                      }
                      else
                      {
                        builder.add(name, value.toString());
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    return builder;
  }



  public static JsonArrayBuilder
  addJsonField(final JsonArrayBuilder builder, final Object value)
  {
    if (value instanceof Boolean)
    {
      builder.add((boolean) value);
    }
    else
    {
      if (value instanceof Integer)
      {
        builder.add((int) value);
      }
      else
      {
        if (value instanceof Long)
        {
          builder.add((long) value);
        }
        else
        {
          if (value instanceof BigInteger)
          {
            builder.add((BigInteger) value);
          }
          else
          {
            if (value instanceof BigDecimal)
            {
              builder.add((BigDecimal) value);
            }
            else
            {
              if (value instanceof Double)
              {
                builder.add((double) value);
              }
              else
              {
                if (value instanceof Float)
                {
                  builder.add((float) value);
                }
                else
                {
                  if (value instanceof Date)
                  {
                    builder.add
                    (
                      Instant.ofEpochMilli(((Date) value).getTime()).toString()
                    );
                  }
                  else
                  {
                    if (value instanceof Map)
                    {
                      builder.add(from((Map) value));
                    }
                    else
                    {
                      if (value instanceof List)
                      {
                        builder.add(from((List) value));
                      }
                      else
                      {
                        builder.add(value.toString());
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    return builder;
  }



  public static JsonArray
  asArray(final JsonValue value)
  {
    if (value.getValueType() != JsonValue.ValueType.ARRAY)
    {
      throw new JsonException("Not an array");
    }

    return (JsonArray) value;
  }



  public static JsonNumber
  asNumber(final JsonValue value)
  {
    if (value.getValueType() != JsonValue.ValueType.NUMBER)
    {
      throw new JsonException("Not a number");
    }

    return (JsonNumber) value;
  }



  public static JsonObject
  asObject(final JsonValue value)
  {
    if (value.getValueType() != JsonValue.ValueType.OBJECT)
    {
      throw new JsonException("Not an object");
    }

    return (JsonObject) value;
  }



  public static JsonString
  asString(final JsonValue value)
  {
    if (value.getValueType() != JsonValue.ValueType.STRING)
    {
      throw new JsonException("Not a string");
    }

    return (JsonString) value;
  }



  private static Transformer
  chain(final Stream<Transformer> stream)
  {
    return
      stream.
        reduce((t1, t2) -> t1.thenApply(t2)).
        orElse(nopTransformer());
  }



  public static void
  copy(final JsonObject obj, final JsonObjectBuilder builder)
  {
    copy(obj, builder, key -> true);
  }



  public static void
  copy
  (
    final JsonObject obj,
    final JsonObjectBuilder builder,
    final Predicate<String> retain
  )
  {
    copy(obj, builder, (key, o) -> retain.test(key));
  }



  public static void
  copy
  (
    final JsonObject obj,
    final JsonObjectBuilder builder,
    final BiPredicate<String,JsonObject> retain
  )
  {
    obj.
      keySet().
      stream().
      filter(key -> retain.test(key, obj)).
      forEach(key -> builder.add(key, obj.get(key)));
  }



  public static void
  copy
  (
    final JsonArray array,
    final JsonArrayBuilder builder,
    final Predicate<JsonValue> retain
  )
  {
    array.stream().filter(retain).forEach(builder::add);
  }



  public static JsonObject
  createErrorObject(final JsonValue value, final String message)
  {
    final JsonObjectBuilder builder = createObjectBuilder();

    if (value != null)
    {
      builder.add("value", value);
    }

    builder.add("error", true);
    builder.add("message", message);

    return builder.build();
  }



  public static JsonValue
  createValue(final Object value)
  {
    final JsonObjectBuilder builder = createObjectBuilder();

    addJsonField(builder, "__synthetic_key__", value);

    return builder.build().get("__synthetic_key__");
  }



  public static JsonArray
  emptyArray()
  {
    return createArrayBuilder().build();
  }



  public static JsonObject
  emptyObject()
  {
    return createObjectBuilder().build();
  }



  public static Object
  evaluator(final JsonValue value)
  {
    return
      value.getValueType() == JsonValue.ValueType.NUMBER ?
        (Object) asNumber(value).longValue() :
        (
          value.getValueType() == JsonValue.ValueType.STRING ?
            asString(value).getString() : value.toString()
        );
  }



  public static JsonObject
  from(final Map<String,?> fields)
  {
    final JsonObjectBuilder builder = createObjectBuilder();

    add(builder, fields);

    return builder.build();
  }



  public static JsonArray
  from(final List<?> values)
  {
    final JsonArrayBuilder builder = createArrayBuilder();

    values.forEach(value -> addJsonField(builder, value));

    return builder.build();
  }



  public static Optional<JsonStructure>
  from(final String json)
  {
    return tryToGetSilent(() -> createReader(new StringReader(json)).read());
  }



  /**
   * Returns the value for <code>field</code>, which may be dot-separated.
   * @param obj the given JSON object.
   * @param field the query field.
   * @return The optional result value.
   */

  public static Optional<JsonValue>
  get(final JsonObject obj, final String field)
  {
    return pathSearch(obj, field.split("\\."));
  }



  private static Stream<String>
  getFieldVariants(final String field)
  {
    final String[] parts = field.split("\\.");

    return
      takeWhile
      (
        0,
        i -> i + 1,
        i -> i < parts.length
      ).
      map(i -> Arrays.stream(parts, i, parts.length).collect(joining(".")));
  }



  /**
   * Returns the last segment of a dot-separated path.
   * @param path the given path.
   * @return The last segment.
   */

  public static String
  getKey(final String path)
  {
    final String[] parts = path.split("\\.");

    return parts.length > 0 ? parts[parts.length - 1] : path;
  }



  private static Set<String>
  getMandatoryKeys(final Set<String> all, final String parent)
  {
    return
      parent == null ?
        all.
          stream().
          filter(key -> key.indexOf('.') == -1).
          collect(toSet()) :
        all.
          stream().
          filter(key -> key.startsWith(parent + ".")).
          map(key -> key.substring(parent.length() + 1)).
          filter(key -> key.indexOf('.') == -1).
          collect(toSet());
  }



  private static String
  getMessage(final Map<String,String> messages, final String field)
  {
    return
      getFieldVariants(field).
        filter(messages::containsKey).
        map(messages::get).
        findFirst().
        orElse("Error");
  }



  private static String
  getPath(final String parent, final String key)
  {
    return (parent != null ? (parent + ".") : "") + key;
  }



  private static Validator
  getValidator
  (
    final Map<String,Validator> validators,
    final String field
  )
  {
    return
      getFieldVariants(field).
        filter(validators::containsKey).
        map(validators::get).
        findFirst().
        orElse(null);
  }



  /**
   * Returns <code>true</code> if <code>obj</code> contains an entry with the
   * name "error" and value <code>true</code>.
   * @param obj the given JSON object.
   * @return Whether the object contains errors or not.
   */

  public static boolean
  hasErrors(final JsonObject obj)
  {
    return obj.get("error") != null && obj.getBoolean("error", false);
  }



  /**
   * Returns <code>true</code> if any object in <code>array</code> contains an
   * entry with the name "error" and value <code>true</code>.
   * @param array the given JSON array.
   * @return Whether the array contains errors or not.
   */

  public static boolean
  hasErrors(final JsonArray array)
  {
    return
      array.
        stream().
        anyMatch
        (
          value -> value instanceof JsonObject && hasErrors((JsonObject) value)
        );
  }



  public static boolean
  hasErrors(final JsonStructure json)
  {
    return
      (json instanceof JsonObject && hasErrors((JsonObject) json)) ||
        (json instanceof JsonArray && hasErrors((JsonArray) json));
  }



  public static boolean
  isArray(final JsonValue value)
  {
    return value.getValueType() == JsonValue.ValueType.ARRAY;
  }



  public static boolean
  isBoolean(final JsonValue value)
  {
    return
      value.getValueType() == JsonValue.ValueType.TRUE ||
        value.getValueType() == JsonValue.ValueType.FALSE;
  }



  public static ValidationResult
  isBoolean(final ValidationContext context)
  {
    return new ValidationResult(isBoolean(context.value), null);
  }



  public static boolean
  isDate(final JsonValue value)
  {
    return
      value.getValueType() == JsonValue.ValueType.STRING &&
        net.pincette.util.Util.isDate(asString(value).getString());
  }



  public static ValidationResult
  isDate(final ValidationContext context)
  {
    return new ValidationResult(isDate(context.value), null);
  }



  public static boolean
  isEmail(final JsonValue value)
  {
    return
      value.getValueType() == JsonValue.ValueType.STRING &&
        net.pincette.util.Util.isEmail(asString(value).getString());
  }



  public static ValidationResult
  isEmail(final ValidationContext context)
  {
    return new ValidationResult(isEmail(context.value), null);
  }



  public static boolean
  isInstant(final JsonValue value)
  {
    return
      value.getValueType() == JsonValue.ValueType.STRING &&
        net.pincette.util.Util.isInstant(asString(value).getString());
  }



  public static ValidationResult
  isInstant(final ValidationContext context)
  {
    return new ValidationResult(isInstant(context.value), null);
  }



  public static boolean
  isNull(final JsonValue value)
  {
    return value.getValueType() == JsonValue.ValueType.NULL;
  }



  public static boolean
  isNumber(final JsonValue value)
  {
    return value.getValueType() == JsonValue.ValueType.NUMBER;
  }



  public static ValidationResult
  isNumber(final ValidationContext context)
  {
    return new ValidationResult(isNumber(context.value), null);
  }



  public static boolean
  isObject(final JsonValue value)
  {
    return value.getValueType() == JsonValue.ValueType.OBJECT;
  }



  public static boolean
  isString(final JsonValue value)
  {
    return value.getValueType() == JsonValue.ValueType.STRING;
  }



  public static ValidationResult
  isString(final ValidationContext context)
  {
    return new ValidationResult(isString(context.value), null);
  }



  public static boolean
  isUri(final JsonValue value)
  {
    return isUri(asString(value).getString());
  }



  public static boolean
  isUri(final String s)
  {
    return s.startsWith("/") || net.pincette.util.Util.isUri(s);
  }



  public static ValidationResult
  isUri(final ValidationContext context)
  {
    return new ValidationResult(isUri(context.value), null);
  }



  /**
   * Returns a transformer that does nothing.
   * @return The transformer.
   */

  public static Transformer
  nopTransformer()
  {
    return new Transformer(e -> false, Optional::of);
  }



  public static JsonObject
  remove(final JsonObject obj, final Set<String> fields)
  {
    return remove(obj, fields::contains);
  }



  public static JsonObject
  remove(final JsonObject obj, final Predicate<String> pred)
  {
    final JsonObjectBuilder builder = createObjectBuilder();

    copy(obj, builder, key -> !pred.test(key));

    return builder.build();
  }



  public static JsonArray
  remove(final JsonArray array, final Predicate<JsonValue> pred)
  {
    final JsonArrayBuilder builder = createArrayBuilder();

    copy(array, builder, value -> !pred.test(value));

    return builder.build();
  }



  /**
   * Removes fields with a name that starts with an underscore.
   * @param obj the given JSON object.
   * @return The new JSON object without the technical fields.
   */

  public static JsonObject
  removeTechnical(final JsonObject obj)
  {
    final JsonObjectBuilder builder = createObjectBuilder();

    copy(obj, builder, key -> !key.startsWith("_"));

    return builder.build();
  }



  /**
   * Returns a transformer that replaces the value of the field designated by
   * the dot-separated <code>path</code> with <code>value</code>.
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The transformer.
   */

  public static Transformer
  setTransformer(final String path, final Object value)
  {
    return
      new Transformer
      (
        e -> e.path.equals(path),
        e -> Optional.of(new JsonEntry(e.path, createValue(value)))
      );
  }




  public static String
  string(final JsonStructure json)
  {
    return string(json, false);
  }



  public static String
  string(final JsonStructure json, final boolean pretty)
  {
    final Map<String,Object> config = new HashMap<String,Object>();
    final StringWriter writer = new StringWriter();

    if (pretty)
    {
      config.put(JsonGenerator.PRETTY_PRINTING, true);
    }

    createWriterFactory(config).createWriter(writer).write(json);

    return writer.toString();
  }



  /**
   * Converts <code>value</code> recursively to a Java value.
   * @param value the given value.
   * @return The converted value.
   */

  public static Object
  toNative(final JsonValue value)
  {
    switch (value.getValueType())
    {
      case ARRAY: return toNative(asArray(value));
      case FALSE: return false;
      case TRUE: return true;
      case NUMBER: return asNumber(value).doubleValue();
      case OBJECT: return toNative(asObject(value));
      case STRING: return asString(value).getString();
      default: return value;
    }
  }



  /**
   * Converts <code>array</code> recursively to a list with Java values.
   * @param array the given array.
   * @return The generated list.
   */

  public static List<?>
  toNative(final JsonArray array)
  {
    return array.stream().map(Json::toNative).collect(toList());
  }



  /**
   * Converts <code>object</code> recursively to a map with Java values.
   * @param object the given object.
   * @return The generated map.
   */

  public static Map<String,?>
  toNative(final JsonObject object)
  {
    return
      object.
        entrySet().
        stream().
        collect(toMap(Map.Entry::getKey, e -> toNative(e.getValue())));
  }



  /**
   * Returns a new value where recursively entries that <code>match</code>
   * are transformed by <code>transformer</code>. If the latter is empty
   * the entry is removed from the result.
   * @param json the given JSON value.
   * @param transformer the applied transformer.
   * @return The new JSON value.
   */

  public static JsonValue
  transform(final JsonValue json, final Transformer transformer)
  {
    return transform(json, null, transformer);
  }



  private static JsonValue
  transform
  (
    final JsonValue json,
    final String parent,
    final Transformer transformer
  )
  {
    return
      json instanceof JsonStructure ?
        transform((JsonStructure) json, parent, transformer) : json;
  }



  /**
   * Returns a new structure where recursively entries that <code>match</code>
   * are transformed by <code>transformer</code>. If the latter is empty
   * the entry is removed from the result.
   * @param json the given JSON structure.
   * @param transformer the applied transformer.
   * @return The new JSON structure.
   */

  public static JsonStructure
  transform(final JsonStructure json, final Transformer transformer)
  {
    return transform(json, null, transformer);
  }



  private static JsonStructure
  transform
  (
    final JsonStructure json,
    final String parent,
    final Transformer transformer
  )
  {
    return
      json instanceof JsonArray ?
        transform((JsonArray) json, parent, transformer) :
        transform((JsonObject) json, parent, transformer);
  }



  /**
   * Returns a new array where entries of objects that <code>match</code>
   * are transformed by <code>transformer</code>. If the latter is empty
   * the entry is removed from the result.
   * @param array the given JSON array.
   * @param transformer the applied transformer.
   * @return The new JSON array.
   */

  public static JsonArray
  transform(final JsonArray array, final Transformer transformer)
  {
    return transform(array, null, transformer);
  }



  private static JsonArray
  transform
  (
    final JsonArray array,
    final String parent,
    final Transformer transformer
  )
  {
    final JsonArrayBuilder builder = createArrayBuilder();

    array.
      stream().
      filter(Objects::nonNull).
      forEach(value -> builder.add(transform(value, parent, transformer)));

    return builder.build();
  }



  /**
   * Returns a new object where entries that <code>match</code> are transformed
   * by <code>transformer</code>. If the latter is empty the
   * entry is removed from the result.
   * @param obj the given JSON object.
   * @param transformer the applied transformer.
   * @return The new JSON object.
   */

  public static JsonObject
  transform(final JsonObject obj, final Transformer transformer)
  {
    return transform(obj, null, transformer);
  }



  private static JsonObject
  transform
  (
    final JsonObject obj,
    final String parent,
    final Transformer transformer
  )
  {
    final JsonObjectBuilder builder = createObjectBuilder();

    obj.
      keySet().
      forEach
      (
        key ->
          transformer.run(new JsonEntry(getPath(parent, key), obj.get(key))).
          map
          (
            entry ->
              new JsonEntry
              (
                getPath(parent, getKey(entry.path)),
                transform
                (
                  entry.value,
                  getPath(parent, getKey(entry.path)),
                  transformer
                )
              )
          ).
          ifPresent(entry -> builder.add(getKey(entry.path), entry.value))
      );

    return builder.build();
  }



  public static InputStream
  transformToXML(final JsonObject json)
  {
    try
    {
      final ByteArrayOutputStream out = new ByteArrayOutputStream();

      newInstance().createXMLEventWriter(out).
        add(new JsonEventReader(createParserFactory(null).createParser(json)));

      return new ByteArrayInputStream(out.toByteArray());
    }

    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }



  public static JsonObject
  validate
  (
    final JsonObject obj,
    final ValidationContext context,
    final Map<String,Validator> validators,
    final Map<String,String> messages,
    final Set<String> mandatory,
    final String missingMessage
  )
  {
    return
      (JsonObject)
        validate
        (
          null,
          obj,
          context,
          validators,
          messages,
          mandatory,
          missingMessage
        ).first;
  }



  public static JsonArray
  validate
  (
    final JsonArray array,
    final ValidationContext context,
    final Map<String,Validator> validators,
    final Map<String,String> messages,
    final Set<String> mandatory,
    final String missingMessage
  )
  {
    return
      validate
      (
        null,
        array,
        context,
        validators,
        messages,
        mandatory,
        missingMessage
      ).first;
  }



  public static JsonStructure
  validate
  (
    final JsonStructure value,
    final ValidationContext context,
    final Map<String,Validator> validators,
    final Map<String,String> messages,
    final Set<String> mandatory,
    final String missingMessage
  )
  {
    return
      (JsonStructure)
        validate
        (
          null,
          value,
          context,
          validators,
          messages,
          mandatory,
          missingMessage
        ).first;
  }



  private static Pair<? extends JsonValue,Boolean>
  validate
  (
    final String field,
    final JsonValue value,
    final ValidationContext context,
    final Map<String,Validator> validators,
    final Map<String,String> messages,
    final Set<String> mandatory,
    final String missingMessage
  )
  {
    return
      value instanceof JsonObject ?
        validate
        (
          field,
          (JsonObject) value,
          context,
          validators,
          messages,
          mandatory,
          missingMessage
        ) :
        (
          value instanceof JsonArray ?
            validate
            (
              field,
              (JsonArray) value,
              context,
              validators,
              messages,
              mandatory,
              missingMessage
            ) : new Pair<>(value, false)
        );
  }



  private static Pair<JsonValue,Boolean>
  validate
  (
    final String parent,
    final JsonObject obj,
    final ValidationContext context,
    final Map<String,Validator> validators,
    final Map<String,String> messages,
    final Set<String> mandatory,
    final String missingMessage
  )
  {
    final JsonObjectBuilder builder = createObjectBuilder();
    final Set<String> found = new HashSet<String>();
    boolean errors =
      obj.keySet().stream().map
      (
        key ->
          {
            final String field = parent != null ? (parent + "." + key) : key;
            final JsonValue value = obj.get(key);
            final ValidationResult result =
              Optional.ofNullable(getValidator(validators, field)).
                map
                (
                  validator -> validator.apply(context.with(field).with(value))
                ).
                orElse(new ValidationResult(true, null));
            final Pair<? extends JsonValue,Boolean> entry =
              result.status ?
                validate
                (
                  field,
                  value,
                  context,
                  validators,
                  messages,
                  mandatory,
                  missingMessage
                ) :
                new Pair<JsonValue,Boolean>
                (
                  createErrorObject
                  (
                    value,
                    result.message != null ?
                      result.message : getMessage(messages, field)
                  ),
                  true
                );

            found.add(key);
            builder.add(key, entry.first);

            return entry.second;
          }
      ).
      reduce(false, (e1, e2) -> e1 || e2);

    errors |=
      difference(getMandatoryKeys(mandatory, parent), found).
        stream().
        map
        (
          key ->
            {
              builder.add(key, createErrorObject(null, missingMessage));

              return true;
            }
        ).
        reduce(false, (e1, e2) -> e1 || e2);

    if (errors)
    {
      builder.add("error", true);
    }

    return new Pair<>(builder.build(), errors);
  }



  private static Pair<JsonArray,Boolean>
  validate
  (
    final String parent,
    final JsonArray array,
    final ValidationContext context,
    final Map<String,Validator> validators,
    final Map<String,String> messages,
    final Set<String> mandatory,
    final String missingMessage
  )
  {
    final JsonArrayBuilder builder = createArrayBuilder();
    final boolean errors =
      array.stream().map
      (
        value ->
          {
            final Pair<? extends JsonValue,Boolean> entry =
              validate
              (
                parent,
                value,
                context,
                validators,
                messages,
                mandatory,
                missingMessage
              );

            builder.add(entry.first);

            return entry.second;
          }
      ).
      reduce(false, (e1, e2) -> e1 || e2);

    return new Pair<>(builder.build(), errors);
  }



  public static class JsonEntry

  {

    /**
     * A dot-separated key path.
     */

    final public String path;

    /**
     * A JSON value.
     */

    final public JsonValue value;



    /**
     * @param path a dot-separated key path.
     * @param value a JSON value.
     */

    public
    JsonEntry(final String path, final JsonValue value)
    {
      this.path = path;
      this.value = value;
    }

  } // JsonEntry



  public static class Transformer

  {

    final public Predicate<JsonEntry> match;
    final public Transformer next;
    final public Function<JsonEntry,Optional<JsonEntry>> transform;



    public
    Transformer
    (
      final Predicate<JsonEntry> match,
      final Function<JsonEntry,Optional<JsonEntry>> transform
    )
    {
      this.match = match;
      this.next = null;
      this.transform = transform;
    }



    private
    Transformer(final Transformer me, final Transformer next)
    {
      this.match = me.match;
      this.transform = me.transform;
      this.next = next;
    }



    public Optional<JsonEntry>
    run(final JsonEntry entry)
    {
      return
        runNext
        (
          !match.test(entry) ? Optional.of(entry) : transform.apply(entry)
        );
    }



    private Optional<JsonEntry>
    runNext(final Optional<JsonEntry> entry)
    {
      return next != null ? entry.flatMap(next::run) : entry;
    }



    public Transformer
    thenApply(final Transformer transformer)
    {
      return new Transformer(this, transformer);
    }

  } // Transformer



  public static class ValidationContext

  {

    public final String field;
    public final JsonStructure newJson;
    public final JsonStructure oldJson;
    public final JsonValue value;



    public
    ValidationContext(final JsonStructure oldJson, final JsonStructure newJson)
    {
      this(oldJson, newJson, null, null);
    }



    private
    ValidationContext
    (
      final JsonStructure oldJson,
      final JsonStructure newJson,
      final String field,
      final JsonValue value
    )
    {
      this.oldJson = oldJson;
      this.newJson = newJson;
      this.field = field;
      this.value = value;
    }



    private ValidationContext
    with(final String field)
    {
      return new ValidationContext(oldJson, newJson, field, value);
    }



    private ValidationContext
    with(final JsonValue value)
    {
      return new ValidationContext(oldJson, newJson, field, value);
    }

  } // ValidationContext



  public static class ValidationResult

  {

    public final String message;
    public final boolean status;



    public
    ValidationResult(final boolean status)
    {
      this(status, null);
    }



    public
    ValidationResult(final boolean status, final String message)
    {
      this.status = status;
      this.message = message;
    }

  } // ValidationResult



  public interface Validator extends
    Function<ValidationContext,ValidationResult>

  {

  } // Validator

} // Json
