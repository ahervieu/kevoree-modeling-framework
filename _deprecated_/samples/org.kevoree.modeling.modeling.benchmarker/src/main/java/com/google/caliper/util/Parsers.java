/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors:
 * Fouquet Francois
 * Nain Gregory
 */
package com.google.caliper.util;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Primitives;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.List;

public class Parsers {
  public static final Parser<String> IDENTITY = new Parser<String>() {
    @Override public String parse(CharSequence in) {
      return in.toString();
    }
  };

  private static final List<String> CONVERSION_METHOD_NAMES =
      ImmutableList.of("fromString", "decode", "valueOf");

  /**
   * Parser that tries, in this order:
   * <ul>
   * <li>ResultType.fromString(String)
   * <li>ResultType.decode(String)
   * <li>ResultType.valueOf(String)
   * <li>new ResultType(String)
   * </ul>
   */
  public static <T> Parser<T> conventionalParser(Class<T> resultType)
      throws NoSuchMethodException {
    if (resultType == String.class) {
      @SuppressWarnings("unchecked") // T == String
      Parser<T> identity = (Parser<T>) IDENTITY;
      return identity;
    }

    final Class<T> wrappedResultType = Primitives.wrap(resultType);

    for (String methodName : CONVERSION_METHOD_NAMES) {
      try {
        final Method method = wrappedResultType.getDeclaredMethod(methodName, String.class);

        if (Util.isStatic(method) && wrappedResultType.isAssignableFrom(method.getReturnType())) {
          method.setAccessible(true); // to permit inner enums, etc.
          return new InvokingParser<T>() {
            @Override protected T invoke(String input) throws Exception {
              return wrappedResultType.cast(method.invoke(null, input));
            }
          };
        }
      } catch (Exception tryAgain) {
      }
    }

    final Constructor<T> constr = wrappedResultType.getDeclaredConstructor(String.class);
    constr.setAccessible(true);
    return new InvokingParser<T>() {
      @Override protected T invoke(String input) throws Exception {
        return wrappedResultType.cast(constr.newInstance(input));
      }
    };
  }

  abstract static class InvokingParser<T> implements Parser<T> {
    @Override public T parse(CharSequence input) throws ParseException {
      try {
        return invoke(input.toString());
      } catch (InvocationTargetException e) {
        Throwable cause = e.getCause();
        String desc = firstNonNull(cause.getMessage(), cause.getClass().getSimpleName());
        throw newParseException(desc, cause);
      } catch (Exception e) {
        throw newParseException("Unknown parsing problem", e);
      }
    }

    protected abstract T invoke(String input) throws Exception;
  }

  public static ParseException newParseException(String message, Throwable cause) {
    ParseException pe = newParseException(message);
    pe.initCause(cause);
    return pe;
  }

  public static ParseException newParseException(String message) {
    return new ParseException(message, 0);
  }

  private static <T> T firstNonNull(T first, T second) {
    return (first != null) ? first : second;
  }
}
