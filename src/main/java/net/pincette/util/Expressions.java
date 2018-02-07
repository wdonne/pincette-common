package net.pincette.util;

import static java.util.stream.Collectors.toList;
import static net.pincette.util.Or.tryWith;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.stream;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pincette.function.SideEffect;

/**
 * Utilities to work with expressions.
 *
 * @author Werner Donn\u00e9
 */

public class Expressions

{

  private static final Pattern TOKENS =
      Pattern.compile(
          "(\\()|(\\))|(\\!)|([a-zA-Z][a-zA-Z0-9_]*)|(=)|(\\!=)|(<)|(>)|(<=)|" +
              "(>=)|(\\+)|(-)|(\\*)|(/)|(&&)|(\\|\\|)|(-?\\d+\\.?\\d*)|('[^']*')|" +
              "(\"[^\"]*\")|(\\s+)"
      );

  private static final int LEFT_BRACE = 1;
  private static final int RIGHT_BRACE = 2;
  private static final int NOT = 3;
  private static final int IDENTIFIER = 4;
  private static final int EQUAL = 5;
  private static final int NOT_EQUAL = 6;
  private static final int LESS_THAN = 7;
  private static final int GREATER_THAN = 8;
  private static final int LESS_THAN_EQUAL = 9;
  private static final int GREATER_THAN_EQUAL = 10;
  private static final int PLUS = 11;
  private static final int MINUS = 12;
  private static final int MULTIPLY = 13;
  private static final int DIVIDE = 14;
  private static final int AND = 15;
  private static final int OR = 16;
  private static final int NUMBER = 17;
  private static final int SINGLE_QUOTED = 18;
  private static final int DOUBLE_QUOTED = 19;
  private static final int WHITESPACE = 20;

  private static final int[] TOKEN_VALUES = {
      LEFT_BRACE, RIGHT_BRACE, NOT, IDENTIFIER, EQUAL, NOT_EQUAL, LESS_THAN, GREATER_THAN,
      LESS_THAN_EQUAL, GREATER_THAN_EQUAL, PLUS, MINUS, MULTIPLY, DIVIDE, AND, OR, NUMBER,
      SINGLE_QUOTED, DOUBLE_QUOTED, WHITESPACE
  };

  private static Expr
  braced(final Supplier<Value> get, final Runnable pushback) {
    final Function<Pair<Expr, Value>, Expr> ifRightBrace =
        pair ->
            pair.second.token == RIGHT_BRACE ?
                pair.first :
                SideEffect.<Expr>run(pushback).andThenGet(() -> null);

    return
        get.get().token == LEFT_BRACE ?
            Optional
                .ofNullable(expr(get, pushback))
                .map(expr -> pair(expr, get.get()))
                .map(ifRightBrace)
                .orElse(null) :
            SideEffect.<Expr>run(pushback).andThenGet(() -> null);
  }

  /**
   * expr -> ( expr ) |
   * ! expr |
   * expr operator expr |
   * identifier |
   * number |
   * string
   *
   * transformed to:
   *
   * expr -> rest exprPrime
   * rest -> ( expr ) |
   * ! expr |
   * identifier |
   * number |
   * string
   * exprPrime -> operator expr |
   * epsilon
   */

  private static Expr
  expr(final Supplier<Value> get, final Runnable pushback) {
    return
        Optional
            .ofNullable(rest(get, pushback))
            .map(rest -> pair(rest, exprPrime(get, pushback)))
            .map(
                pair ->
                    pair.second != null ?
                        new Operator(pair.first, pair.second.op, pair.second.operand2) : pair.first
            )
            .orElse(null);
  }

  private static Operator
  exprPrime(final Supplier<Value> get, final Runnable pushback) {
    final int token = get.get().token;

    return
        isBinaryOperator(token) ?
            Optional
                .ofNullable(expr(get, pushback))
                .map(expr -> new Operator(null, token, expr))
                .orElse(null) :
            SideEffect.<Operator>run(pushback).andThenGet(() -> null);
  }

  private static int
  findGroup(final Matcher matcher) {
    return
        Arrays
            .stream(TOKEN_VALUES)
            .filter(value -> matcher.start(value) != -1)
            .findFirst()
            .orElse(-1);
  }

  private static Object
  getValue(final int token, final String s) {
    switch (token) {
      case NUMBER:
        return Double.parseDouble(s);
      case IDENTIFIER:
        return s;
      case SINGLE_QUOTED:
      case DOUBLE_QUOTED:
        return s.substring(1, s.length() - 1);
      default:
        return null;
    }
  }

  private static Identifier
  identifier(final Supplier<Value> get, final Runnable pushback) {
    final Value value = get.get();

    return
        value.token == IDENTIFIER ?
            new Identifier((String) value.val) :
            SideEffect.<Identifier>run(pushback).andThenGet(() -> null);
  }

  private static boolean
  isBinaryOperator(final int token) {
    return
        token == PLUS || token == MINUS || token == AND || token == OR || token == EQUAL
            || token == NOT_EQUAL || token == LESS_THAN || token == GREATER_THAN
            || token == LESS_THAN_EQUAL || token == GREATER_THAN_EQUAL || token == MULTIPLY
            || token == DIVIDE;
  }

  private static Not
  not(final Supplier<Value> get, final Runnable pushback) {
    return
        get.get().token == NOT ?
            Optional
                .ofNullable(expr(get, pushback))
                .map(Not::new)
                .orElse(null) :
            SideEffect.<Not>run(pushback).andThenGet(() -> null);
  }

  private static NumberExpr
  number(final Supplier<Value> get, final Runnable pushback) {
    final Value value = get.get();

    return
        value.token == NUMBER ?
            new NumberExpr((Double) value.val) :
            SideEffect.<NumberExpr>run(pushback).andThenGet(() -> null);
  }

  public static Optional<Expr>
  parse(final String s) {
    final Tokens tokens = new Tokens(tokenize(s));

    return Optional.ofNullable(expr(tokens::get, tokens::pushback));
  }

  private static Expr
  rest(final Supplier<Value> get, final Runnable pushback) {
    return
        tryWith(() -> braced(get, pushback))
            .or(() -> not(get, pushback))
            .or(() -> identifier(get, pushback))
            .or(() -> number(get, pushback))
            .or(() -> string(get, pushback))
            .get()
            .orElse(null);
  }

  private static StringExpr
  string(final Supplier<Value> get, final Runnable pushback) {
    final Value value = get.get();

    return
        value.token == SINGLE_QUOTED || value.token == DOUBLE_QUOTED ?
            new StringExpr((String) value.val) :
            SideEffect.<StringExpr>run(pushback).andThenGet(() -> null);
  }

  private static List<Value>
  tokenize(final String s) {
    final Matcher matcher = TOKENS.matcher(s);

    return
        stream(
            new Iterator<MatchedToken>() {
              public boolean hasNext() {
                return matcher.find();
              }

              public MatchedToken next() {
                if (!hasNext()) {
                  throw new NoSuchElementException();
                }

                return new MatchedToken(findGroup(matcher), matcher);
              }
            }
        )
            .filter(token -> token.token != WHITESPACE)
            .map(
                token ->
                    new Value(
                        token.token,
                        getValue(token.token, s.substring(token.start, token.end))
                    )
            )
            .collect(toList());
  }

  public interface Expr

  {

    Object evaluate(Function<String, Object> evaluator);

  } // Expr


  private static class Identifier implements Expr

  {

    private final String name;


    private Identifier(final String name) {
      this.name = name;
    }


    public Object
    evaluate(final Function<String, Object> evaluator) {
      return evaluator.apply(name);
    }

  } // Identifier


  private static class MatchedToken

  {

    private final int end;
    private final int token;
    private final int start;


    private MatchedToken(final int token, final Matcher matcher) {
      this.token = token;
      this.start = matcher.start(token);
      this.end = matcher.end(token);
    }

  } // MatchedToken


  private static class Not implements Expr

  {

    private final Expr operand;


    private Not(final Expr operand) {
      this.operand = operand;
    }


    public Object
    evaluate(final Function<String, Object> evaluator) {
      final Object value = operand.evaluate(evaluator);

      return value instanceof Boolean ? !((Boolean) value) : null;
    }

  } // Not


  private static class NumberExpr implements Expr

  {

    private final Double value;


    private NumberExpr(final Double value) {
      this.value = value;
    }


    public Object
    evaluate(final Function<String, Object> evaluator) {
      return value;
    }

  } // NumberExpr


  private static class Operator implements Expr

  {

    private final Expr operand1;
    private final Expr operand2;
    private final int op;


    private Operator(final Expr operand1, final int op, final Expr operand2) {
      this.operand1 = operand1;
      this.op= op;
      this.operand2 = operand2;
    }


    public Object
    evaluate(final Function<String, Object> evaluator) {
      final Object left = operand1.evaluate(evaluator);
      final Object right = operand2.evaluate(evaluator);

      return
          left != null && right != null &&
              left.getClass().isAssignableFrom(right.getClass()) &&
              isCompatible(op, left) && isCompatible(op, right) ?
              getBinaryOperator(op).apply(left, right) : null;
    }

    private static BiFunction<Object, Object, Object>
    getBinaryOperator(final int operator) {
      switch (operator) {
        case AND:
          return (left, right) -> (Boolean) left && (Boolean) right;
        case OR:
          return (left, right) -> (Boolean) left || (Boolean) right;
        case EQUAL:
          return Objects::equals;
        case NOT_EQUAL:
          return (left, right) -> !left.equals(right);
        case LESS_THAN:
          return (left, right) -> ((Comparable) left).compareTo(right) < 0;
        case GREATER_THAN:
          return (left, right) -> ((Comparable) left).compareTo(right) > 0;
        case LESS_THAN_EQUAL:
          return (left, right) -> ((Comparable) left).compareTo(right) <= 0;
        case GREATER_THAN_EQUAL:
          return (left, right) -> ((Comparable) left).compareTo(right) >= 0;
        case PLUS:
          return (left, right) -> ((Number) left).doubleValue() + ((Number) right).doubleValue();
        case MINUS:
          return (left, right) -> ((Number) left).doubleValue() - ((Number) right).doubleValue();
        case MULTIPLY:
          return (left, right) -> ((Number) left).doubleValue() * ((Number) right).doubleValue();
        case DIVIDE:
          return (left, right) -> ((Number) left).doubleValue() / ((Number) right).doubleValue();
        default:
          return (left, right) -> null;
      }
    }

    private static boolean
    isCompatible(final int operator, final Object value) {
      switch (operator) {
        case AND:
        case OR:
          return value instanceof Boolean;
        case EQUAL:
        case NOT_EQUAL:
          return true;
        case LESS_THAN:
        case GREATER_THAN:
        case LESS_THAN_EQUAL:
        case GREATER_THAN_EQUAL:
          return value instanceof Comparable;
        case PLUS:
        case MINUS:
        case MULTIPLY:
        case DIVIDE:
          return value instanceof Number;
        default:
          return false;
      }
    }

  } // Operator


  private static class StringExpr implements Expr

  {

    private final String value;


    private StringExpr(final String value) {
      this.value = value;
    }


    public Object
    evaluate(final Function<String, Object> evaluator) {
      return value;
    }

  } // StringExpr


  private static class Tokens

  {

    private final List<Value> toks;
    private int position;


    private Tokens(final List<Value> toks) {
      this.toks = toks;
    }


    private Value
    get() {
      return position >= toks.size() ? new Value(-1, null) : toks.get(position++);
    }


    private void
    pushback() {
      --position;
    }

  } // Tokens


  private static class Value

  {

    private final int token;
    private final Object val;


    private Value(final int token, final Object val) {
      this.token = token;
      this.val = val;
    }

  } // Value

} // Expressions
