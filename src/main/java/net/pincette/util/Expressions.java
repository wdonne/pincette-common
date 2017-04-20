package net.pincette.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.toList;
import net.pincette.function.SideEffect;
import static net.pincette.util.Or.tryWith;



/**
 * Utilities to work with expressions.
 * @author Werner Donn\u00e9
 */

public class Expressions

{

  final private static Pattern TOKENS =
    Pattern.compile
    (
      "(\\()|(\\))|(\\!)|([a-zA-Z][a-zA-Z0-9_]*)|(=)|(\\!=)|(<)|(>)|(<=)|" +
        "(>=)|(\\+)|(-)|(\\*)|(/)|(&&)|(\\|\\|)|(-?\\d+\\.?\\d*)|('[^']*')|" +
        "(\"[^\"]*\")|(\\s+)"
    );

  final private static int LEFT_BRACE = 1;
  final private static int RIGHT_BRACE = 2;
  final private static int NOT = 3;
  final private static int IDENTIFIER = 4;
  final private static int EQUAL = 5;
  final private static int NOT_EQUAL = 6;
  final private static int LESS_THAN = 7;
  final private static int GREATER_THAN = 8;
  final private static int LESS_THAN_EQUAL = 9;
  final private static int GREATER_THAN_EQUAL = 10;
  final private static int PLUS = 11;
  final private static int MINUS = 12;
  final private static int MULTIPLY = 13;
  final private static int DIVIDE = 14;
  final private static int AND = 15;
  final private static int OR = 16;
  final private static int NUMBER = 17;
  final private static int SINGLE_QUOTED = 18;
  final private static int DOUBLE_QUOTED = 19;
  final private static int WHITESPACE = 20;

  final private static int[] TOKEN_VALUES =
    {
      LEFT_BRACE, RIGHT_BRACE, NOT, IDENTIFIER, EQUAL, NOT_EQUAL, LESS_THAN,
        GREATER_THAN, LESS_THAN_EQUAL, GREATER_THAN_EQUAL, PLUS, MINUS,
        MULTIPLY, DIVIDE, AND, OR, NUMBER, SINGLE_QUOTED, DOUBLE_QUOTED,
        WHITESPACE
    };



  private static Expr
  braced(final Supplier<Value> get, final Runnable pushback)
  {
    return
      get.get().token == LEFT_BRACE ?
        Optional.ofNullable(expr(get, pushback)).
          map(expr -> new Pair<Expr,Value>(expr, get.get())).
          map
          (
            pair ->
              pair.second.token == RIGHT_BRACE ?
                pair.first :
                SideEffect.<Expr>run(pushback).andThenGet(() -> null)
          ).
          orElse(null) :
        SideEffect.<Expr>run(pushback).andThenGet(() -> null);
  }



  /**
   * expr -> ( expr ) |
   *         ! expr |
   *         expr operator expr |
   *         identifier |
   *         number |
   *         string
   *
   * transformed to:
   *
   * expr -> rest exprPrime
   * rest -> ( expr ) |
   *         ! expr |
   *         identifier |
   *         number |
   *         string
   * exprPrime -> operator expr |
   *              epsilon
   */

  private static Expr
  expr(final Supplier<Value> get, final Runnable pushback)
  {
    return
      Optional.ofNullable(rest(get, pushback)).
        map(rest -> new Pair<Expr,Operator>(rest, exprPrime(get, pushback))).
        map
        (
          pair ->
            pair.second != null ?
              new Operator
              (
                pair.first,
                pair.second.operator,
                pair.second.operand2
              ) : pair.first
        ).
        orElse(null);
  }



  private static Operator
  exprPrime(final Supplier<Value> get, final Runnable pushback)
  {
    final int token = get.get().token;

    return
      isBinaryOperator(token) ?
        Optional.ofNullable(expr(get, pushback)).
          map(expr -> new Operator(null, token, expr)).
          orElse(null) :
        SideEffect.<Operator>run(pushback).andThenGet(() -> null);
  }



  private static int
  findGroup(final Matcher matcher)
  {
    return
      Arrays.stream(TOKEN_VALUES).
        filter(value -> matcher.start(value) != -1).
        findFirst().
        orElse(-1);
  }



  private static BiFunction<Object,Object,Object>
  getBinaryOperator(final int operator)
  {
    switch (operator)
    {
      case AND:
        return (left, right) -> (Boolean) left && (Boolean) right;
      case OR:
        return (left, right) -> (Boolean) left || (Boolean) right;
      case EQUAL: return Objects::equals;
      case NOT_EQUAL: return (left, right) -> !left.equals(right);
      case LESS_THAN:
        return (left, right) -> ((Comparable) left).compareTo(right) < 0;
      case GREATER_THAN:
        return (left, right) -> ((Comparable) left).compareTo(right) > 0;
      case LESS_THAN_EQUAL:
        return (left, right) -> ((Comparable) left).compareTo(right) <= 0;
      case GREATER_THAN_EQUAL:
        return (left, right) -> ((Comparable) left).compareTo(right) >= 0;
      case PLUS:
        return
          (left, right) ->
            ((Number) left).doubleValue() + ((Number) right).doubleValue();
      case MINUS:
        return
          (left, right) ->
            ((Number) left).doubleValue() - ((Number) right).doubleValue();
      case MULTIPLY:
        return
          (left, right) ->
            ((Number) left).doubleValue() * ((Number) right).doubleValue();
      case DIVIDE:
        return
          (left, right) ->
            ((Number) left).doubleValue() / ((Number) right).doubleValue();
      default: return (left, right) -> null;
    }
  }



  private static Object
  getValue(final int token, final String s)
  {
    switch (token)
    {
      case NUMBER: return Double.parseDouble(s);
      case IDENTIFIER: return s;
      case SINGLE_QUOTED: case DOUBLE_QUOTED:
        return s.substring(1, s.length() - 1);
      default: return null;
    }
  }



  private static Identifier
  identifier(final Supplier<Value> get, final Runnable pushback)
  {
    final Value value = get.get();

    return
      value.token == IDENTIFIER ?
        new Identifier((String) value.value) :
        SideEffect.<Identifier>run(pushback).andThenGet(() -> null);
  }



  private static boolean
  isBinaryOperator(final int token)
  {
    return
      token == PLUS || token == MINUS || token == AND ||
        token == OR || token == EQUAL || token == NOT_EQUAL ||
        token == LESS_THAN || token == GREATER_THAN ||
        token == LESS_THAN_EQUAL || token == GREATER_THAN_EQUAL ||
        token == MULTIPLY || token == DIVIDE;
  }



  private static boolean
  isCompatible(final int operator, final Object value)
  {
    switch (operator)
    {
      case AND: case OR: return value instanceof Boolean;
      case EQUAL: case NOT_EQUAL: return true;
      case LESS_THAN: case GREATER_THAN: case LESS_THAN_EQUAL:
        case GREATER_THAN_EQUAL: return value instanceof Comparable;
      case PLUS: case MINUS: case MULTIPLY: case DIVIDE:
        return value instanceof Number;
      default: return false;
    }
  }



  private static Not
  not(final Supplier<Value> get, final Runnable pushback)
  {
    return
      get.get().token == NOT ?
        Optional.ofNullable(expr(get, pushback)).
          map(Not::new).
          orElse(null) :
        SideEffect.<Not>run(pushback).andThenGet(() -> null);
  }



  private static NumberExpr
  number(final Supplier<Value> get, final Runnable pushback)
  {
    final Value value = get.get();

    return
      value.token == NUMBER ?
        new NumberExpr((Double) value.value) :
        SideEffect.<NumberExpr>run(pushback).andThenGet(() -> null);
  }



  public static Optional<Expr>
  parse(final String s)
  {
    final Tokens tokens = new Tokens(tokenize(s));

    return Optional.ofNullable(expr(tokens::get, tokens::pushback));
  }



  private static Expr
  rest(final Supplier<Value> get, final Runnable pushback)
  {
    return
      tryWith(() -> braced(get, pushback)).
        or(() -> not(get, pushback)).
        or(() -> identifier(get, pushback)).
        or(() -> number(get, pushback)).
        or(() -> string(get, pushback)).
        get().
        orElse(null);
  }



  private static StringExpr
  string(final Supplier<Value> get, final Runnable pushback)
  {
    final Value value = get.get();

    return
      value.token == SINGLE_QUOTED || value.token == DOUBLE_QUOTED ?
        new StringExpr((String) value.value) :
        SideEffect.<StringExpr>run(pushback).andThenGet(() -> null);
  }



  private static List<Value>
  tokenize(final String s)
  {
    final Matcher matcher = TOKENS.matcher(s);

    return
      Util.stream
      (
        new Iterator<MatchedToken>()
        {
          public boolean hasNext()
          {
            return matcher.find();
          }

          public MatchedToken next()
          {
            return new MatchedToken(findGroup(matcher), matcher);
          }
        }
      ).
      filter(token -> token.token != WHITESPACE).
      map
      (
        token ->
          new Value
          (
            token.token,
            getValue(token.token, s.substring(token.start, token.end))
          )
      ).
      collect(toList());
  }



  public interface Expr

  {

    Object evaluate (Function<String,Object> evaluator);

  } // Expr



  private static class Identifier implements Expr

  {

    final private String name;



    private
    Identifier(final String name)
    {
      this.name = name;
    }



    public Object
    evaluate(final Function<String,Object> evaluator)
    {
      return evaluator.apply(name);
    }

  } // Identifier



  private static class MatchedToken

  {

    final private int end;
    final private int token;
    final private int start;



    private
    MatchedToken(final int token, final Matcher matcher)
    {
      this.token = token;
      this.start = matcher.start(token);
      this.end = matcher.end(token);
    }

  } // MatchedToken



  private static class Not implements Expr

  {

    final private Expr operand;



    private
    Not(final Expr operand)
    {
      this.operand = operand;
    }



    public Object
    evaluate(final Function<String,Object> evaluator)
    {
      final Object value = operand.evaluate(evaluator);

      return value instanceof Boolean ? !((Boolean) value) : null;
    }

  } // Not



  private static class NumberExpr implements Expr

  {

    final private Double value;



    private
    NumberExpr(final Double value)
    {
      this.value = value;
    }



    public Object
    evaluate(final Function<String,Object> evaluator)
    {
      return value;
    }

  } // NumberExpr



  private static class Operator implements Expr

  {

    final private Expr operand1;
    final private Expr operand2;
    final private int operator;



    private
    Operator(final Expr operand1, final int operator, final Expr operand2)
    {
      this.operand1 = operand1;
      this.operator = operator;
      this.operand2 = operand2;
    }



    public Object
    evaluate(final Function<String,Object> evaluator)
    {
      final Object left = operand1.evaluate(evaluator);
      final Object right = operand2.evaluate(evaluator);

      return
        left != null && right != null &&
          left.getClass().isAssignableFrom(right.getClass()) &&
          isCompatible(operator, left) && isCompatible(operator, right) ?
          getBinaryOperator(operator).apply(left, right) : null;
    }

  } // Operator



  private static class StringExpr implements Expr

  {

    final private String value;



    private
    StringExpr(final String value)
    {
      this.value = value;
    }



    public Object
    evaluate(final Function<String,Object> evaluator)
    {
      return value;
    }

  } // StringExpr



  private static class Tokens

  {

    private int position;
    final private List<Value> tokens;



    private
    Tokens(final List<Value> tokens)
    {
      this.tokens = tokens;
    }



    private Value
    get()
    {
      return
        position >= tokens.size() ?
          new Value(-1, null) : tokens.get(position++);
    }



    private void
    pushback()
    {
      --position;
    }

  } // Tokens



  private static class Value

  {

    final private int token;
    final private Object value;



    private
    Value(final int token, final Object value)
    {
      this.token = token;
      this.value = value;
    }

  } // Value

} // Expressions
