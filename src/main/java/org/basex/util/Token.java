package org.basex.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * This class provides convenience operations for handling so-called
 * 'Tokens'. Tokens in this project are nothing else than UTF8 encoded strings,
 * stored in a byte array.
 *
 * Note that, to guarantee a consistent string representation, all string
 * conversions should be done via the methods of this class.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public final class Token {
  /** Maximum length for hash calculation and index terms. */
  public static final byte MAXLEN = 96;

  /** Empty token. */
  public static final byte[] EMPTY = {};
  /** XML token. */
  public static final byte[] XML = token("xml");
  /** XML token with colon. */
  public static final byte[] XMLC = token("xml:");
  /** XMLNS token. */
  public static final byte[] XMLNS = token("xmlns");
  /** XMLNS token with colon. */
  public static final byte[] XMLNSC = token("xmlns:");
  /** True token. */
  public static final byte[] TRUE = token("true");
  /** False token. */
  public static final byte[] FALSE = token("false");
  /** Not available number. */
  public static final byte[] NAN = token("NaN");
  /** Positive infinity. */
  public static final byte[] INF = token("INF");
  /** Negative infinity. */
  public static final byte[] NINF = token("-INF");
  /** Space token. */
  public static final byte[] SPACE = { ' ' };
  /** Zero token. */
  public static final byte[] ZERO = { '0' };
  /** Zero token. */
  public static final byte[] MZERO = { '-', '0' };
  /** One token. */
  public static final byte[] ONE = { '1' };

  /** Quote entity. */
  public static final byte[] QU = { '"' };
  /** Ampersand entity. */
  public static final byte[] AMP = { '&' };
  /** Apostrophe entity. */
  public static final byte[] APOS = { '\'' };
  /** GreaterThan entity. */
  public static final byte[] GT = { '>' };
  /** LessThan entity. */
  public static final byte[] LT = { '<' };

  /** Hex codes. */
  public static final byte[] HEX = token("0123456789ABCDEF");
  /** Reserved characters. */
  private static final byte[] IRIRES = token("!#$%&*'()+,-./:;=?@[]~_");
  /** Reserved characters. */
  private static final byte[] RES = token("-._~");

  /** UTF8 encoding string. */
  public static final String UTF8 = "UTF-8";
  /** UTF8 encoding string (variant). */
  public static final String UTF82 = "UTF8";
  /** UTF16 encoding string. */
  public static final String UTF16 = "UTF-16";
  /** UTF16 encoding string. */
  public static final String UTF162 = "UTF16";
  /** UTF16BE (=UTF16) encoding string. */
  public static final String UTF16BE = "UTF-16BE";
  /** UTF16 encoding string. */
  public static final String UTF16LE = "UTF-16LE";

  /** Hidden constructor. */
  private Token() { }

  /**
   * Returns the specified token as string.
   * @param token token
   * @return string
   */
  public static String string(final byte[] token) {
    return string(token, 0, token.length);
  }

  /**
   * Returns the specified token as string.
   * @param token token
   * @param start start position
   * @param length length
   * @return string
   */
  public static String string(final byte[] token, final int start,
      final int length) {

    if(length <= 0) return "";
    final char[] str = new char[length];
    for(int i = 0; i < length; ++i) {
      final byte b = token[start + i];
      if(b < 0) return utf8(token, start, length);
      str[i] = (char) b;
    }
    return new String(str);
  }

  /**
   * Returns a string of the specified UTF8 token.
   * @param token token
   * @param start start position
   * @param length length
   * @return string
   */
  private static String utf8(final byte[] token, final int start,
      final int length) {

    // input is assumed to be correct UTF8. if input contains codepoints
    // larger than Character.MAX_CODE_POINT, results might be unexpected.

    final StringBuilder sb = new StringBuilder(length);
    final int il = Math.min(start + length, token.length);
    for(int i = start; i < il; i += cl(token, i)) {
      final int cp = cp(token, i);
      if(cp < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
        sb.append((char) cp);
      } else {
        final int o = cp - Character.MIN_SUPPLEMENTARY_CODE_POINT;
        sb.append((char) ((o >>> 10) + Character.MIN_HIGH_SURROGATE));
        sb.append((char) ((o & 0x3ff) + Character.MIN_LOW_SURROGATE));
      }
    }
    return sb.toString();
  }

  /**
   * Checks if the specified token only consists of ASCII characters.
   * @param token token
   * @return result of check
   */
  public static boolean ascii(final byte[] token) {
    for(final byte t : token) if(t < 0) return false;
    return true;
  }

  /**
   * Converts a string to a byte array.
   * All strings should be converted by this function to guarantee
   * a consistent character conversion.
   * @param string string to be converted
   * @return byte array
   */
  public static byte[] token(final String string) {
    final int l = string.length();
    if(l == 0) return EMPTY;
    final byte[] b = new byte[l];
    for(int i = 0; i < l; ++i) {
      final char c = string.charAt(i);
      if(c > 0x7F) return utf8(string);
      b[i] = (byte) c;
    }
    return b;
  }

  /**
   * Converts the specified strings to tokens.
   * @param strings strings
   * @return tokens
   */
  public static byte[][] tokens(final String... strings) {
    final byte[][] t = new byte[strings.length][];
    for(int i = 0; i < t.length; ++i) t[i] = token(strings[i]);
    return t;
  }

  /**
   * Converts a string to a UTF8 byte array.
   * @param string string to be converted
   * @return byte array
   */
  private static byte[] utf8(final String string) {
    final char[] arr = string.toCharArray();
    final int al = arr.length;
    final TokenBuilder tb = new TokenBuilder(al << 1);
    for(int c = 0; c < al; ++c) {
      final char ch = arr[c];
      tb.add(Character.isHighSurrogate(ch) && c < al - 1
          && Character.isLowSurrogate(arr[c + 1])
          ? Character.toCodePoint(ch, arr[++c]) : ch);
    }
    return tb.finish();
  }

  /**
   * Converts a token from the input encoding to UTF8.
   * @param token token to be converted
   * @return byte array
   * @param encoding input encoding
   */
  public static byte[] utf8(final byte[] token, final String encoding) {
    // UTF8 (comparison by ref.) or no special characters: return input
    if(encoding == UTF8 || ascii(token)) return token;

    // convert to utf8. if errors occur while converting, an empty is returned.
    try {
      return token(new String(token, encoding));
    } catch(final Exception ex) {
      Util.debug(ex);
      return EMPTY;
    }
  }

  /**
   * Returns a unified representation of the specified encoding.
   * @param encoding input encoding
   * @param old (optional) old encoding
   * @return encoding
   */
  public static String normEncoding(final String encoding, final String old) {
    final String e = encoding.toUpperCase();
    if(e.equals(UTF8) || e.equals(UTF82)) return UTF8;
    if(e.equals(UTF16BE)) return UTF16BE;
    if(e.equals(UTF16LE)) return UTF16LE;
    if(e.equals(UTF16) || e.equals(UTF162))
      return old == UTF16BE || old == UTF16LE ? old : UTF16BE;
    return encoding;
  }

  /**
   * Checks if the specified encoding is supported.
   * @param encoding encoding
   * @return result of check
   */
  public static boolean supported(final String encoding) {
    try {
      return Charset.isSupported(encoding);
    } catch(final IllegalArgumentException ex) {
      return false;
    }
  }

  /**
   * Returns the codepoint (unicode value) of the specified token, starting at
   * the specified position. Returns a unicode replacement character for invalid
   * values.
   * @param token token
   * @param pos character position
   * @return current character
   */
  public static int cp(final byte[] token, final int pos) {
    // 0xxxxxxx
    final byte v = token[pos];
    if((v & 0xFF) < 192) return v & 0xFF;
    // number of bytes to be read
    final int vl = cl(v);
    if(pos + vl > token.length) return 0xFFFD;
    // 110xxxxx 10xxxxxx
    if(vl == 2) return (v & 0x1F) << 6 | token[pos + 1] & 0x3F;
    // 1110xxxx 10xxxxxx 10xxxxxx
    if(vl == 3) return (v & 0x0F) << 12 | (token[pos + 1] & 0x3F) << 6 |
      token[pos + 2] & 0x3F;
    // 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
    return (v & 0x07) << 18 | (token[pos + 1] & 0x3F) << 12 |
      (token[pos + 2] & 0x3F) << 6 | token[pos + 3] & 0x3F;
  }

  /*** Character lengths. */
  private static final int[] CHLEN = {
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 3, 4
  };

  /**
   * Returns the length of the specified UTF8 byte.
   * @param cp codepoint
   * @return character length
   */
  public static int cl(final byte cp) {
    return cp >= 0 ? 1 : CHLEN[cp >> 4 & 0xF];
  }

  /**
   * Returns the length of a UTF8 character at the specified position.
   * @param token token
   * @param pos position
   * @return character length
   */
  public static int cl(final byte[] token, final int pos) {
    return cl(token[pos]);
  }

  /**
   * Returns the codepoint (unicode value) at the specified position,
   * ignoring surrogates.
   * Returns a {@code 0} byte in the index is invalid.
   * @param string string
   * @param pos position
   * @return character
   */
  public static int cp(final String string, final int pos) {
    return pos >= 0 && pos < string.length() ? string.charAt(pos) : 0;
  }

  /**
   * Converts a token to a sequence of codepoints.
   * @param token token
   * @return codepoints
   */
  public static int[] cps(final byte[] token) {
    int pos = 0;
    final int len = token.length;
    final int[] cp = new int[len];
    for(int i = 0; i < len; i += cl(token, i)) cp[pos++] = cp(token, i);
    return pos < len ? Arrays.copyOf(cp, pos) : cp;
  }

  /**
   * Checks if the specified UTF-8 characters are valid.
   * @param token UTF-8 characters
   * @return result of check
   */
  public static boolean valid(final byte[] token) {
    final int l = token.length;
    int i = 0;
    while(i < l) {
      int cl = cl2(token[i]);
      if(cl <= 0 || cl > l - i++) return false;
      if(l == i) return true;
      final byte b = token[i];
      if(b >= 0 && b < ' ' && !ws(b)) return false; // control character
      while(--cl > 0) if(cl2(token[i++]) != 0) return false;
    }
    return true;
  }

  /**
   * Removes invalid characters from the UTF-8 sequence.
   * @param token the UTF-8 sequence to remove the invalid chars from
   * @param chop if true, all leading and trailing whitespaces are removed
   * @return the cleaned UTF-8 sequence
   */
  public static byte[] clean(final byte[] token, final boolean chop) {
    final int l = token.length;
    if(l == 0) return EMPTY;
    final byte[] t = new byte[l];
    int i = 0, p = 0;
    if(chop) while(i < l && ws(token[i])) ++i;
    if(i == l) return EMPTY;
    out: while(i < l) {
      final int cl = cl2(token[i]);
      if(cl <= 0) { ++i; continue; } // invalid ... ignore this one
      if(cl > l - i) break; // not enough bytes left, ignore everything behind
      final byte b = token[i];
      if(b >= 0 && b < ' ' && !ws(b)) { ++i; continue; } // ignore control chars
      t[p++] = token[i++]; // byte is valid .. copy to new array
      for(int j = 1; j < cl; ++j) { // process all following bytes
        // all following bytes must have a codepoint length of zero
        if(cl2(token[i]) != 0) {
          --p; // drop the already added first byte
          i += cl - j; // skip all bytes of this sequence
          continue out; // continue with the next UTF-8 character
        }
      }
      // all bytes are valid .. add them to the array
      for(int j = 1; j < cl; ++j) t[p++] = token[i++];
    }
    if(chop) while(p > 0 && ws(t[p - 1])) --p;
    return p == 0 ? EMPTY : Arrays.copyOf(t, p);
  }

  /**
   * Checks if the byte is part of a valid UTF-8 character. Returns the expected
   * codepoint length of the specified byte, if it is the first byte of the
   * sequence. If the given byte is the second, third or fourth byte of the
   * sequence, zero is returned. A return value of -1 indicates an invalid UTF-8
   * character.
   * @param first first character byte
   * @return character length, if the byte is the first byte;
   *          zero if not; -1 if invalid
   */
  private static int cl2(final byte first) {
    final int i = first & 0xFF;
    return i == 0xC0 || i == 0xC1 || i > 0xF4 ? -1 : CHLEN2[i >> 4];
  }

  /*** Character lengths. */
  private static final int[] CHLEN2 = {
    1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 2, 2, 3, 4
  };

  /**
   * Returns the token length.
   * @param token token
   * @return length
   */
  public static int len(final byte[] token) {
    int l = 0;
    for(int t = 0; t < token.length; t += cl(token, t)) ++l;
    return l;
  }

  /**
   * Creates a byte array representation of the specified boolean value.
   * @param bool boolean value to be converted
   * @return boolean value in byte array
   */
  public static byte[] token(final boolean bool) {
    return bool ? TRUE : FALSE;
  }

  /**
   * Creates a byte array representation of the specified integer value.
   * @param integer int value to be converted
   * @return integer value in byte array
   */
  public static byte[] token(final int integer) {
    if(integer == 0) return ZERO;
    if(integer == Integer.MIN_VALUE) return MININT;

    int n = integer;
    final boolean m = n < 0;
    if(m) n = -n;
    int j = numDigits(n);
    if(m) ++j;
    final byte[] num = new byte[j];

    // faster division by 10 for values < 81920 (see Integer.getChars)
    while(n > 81919) {
      final int q = n / 10;
      num[--j] = (byte) (n - (q << 3) - (q << 1) + '0');
      n = q;
    }
    while(n != 0) {
      final int q = n * 52429 >>> 19;
      num[--j] = (byte) (n - (q << 3) - (q << 1) + '0');
      n = q;
    }
    if(m) num[--j] = '-';
    return num;
  }

  /**
   * Checks number of digits of the specified integer.
   * @param integer number to be checked
   * @return number of digits
   */
  public static int numDigits(final int integer) {
    for(int i = 0;; ++i) if(integer <= INTSIZE[i]) return i + 1;
  }

  /** Minimum integer. */
  private static final byte[] MININT = token("-2147483648");
  /** Table with integer sizes. */
  private static final int[] INTSIZE = { 9, 99, 999, 9999, 99999, 999999,
      9999999, 99999999, 999999999, Integer.MAX_VALUE };

  /**
   * Creates a byte array representation from the specified long value,
   * using Java's standard method.
   * @param integer int value to be converted
   * @return byte array
   */
  public static byte[] token(final long integer) {
    return integer >= Integer.MIN_VALUE && integer <= Integer.MAX_VALUE ?
        token((int) integer) : token(Long.toString(integer));
  }

  /** US charset. */
  private static final java.text.DecimalFormatSymbols LOC =
    new java.text.DecimalFormatSymbols(java.util.Locale.US);
  /** Scientific double output. */
  private static final java.text.DecimalFormat SD =
    new java.text.DecimalFormat("0.0################E0##", LOC);
  /** Decimal double output. */
  private static final java.text.DecimalFormat DD =
    new java.text.DecimalFormat("#####0.0################", LOC);
  /** Scientific float output. */
  private static final java.text.DecimalFormat SF =
    new java.text.DecimalFormat("0.0####E0##", LOC);
  /** Decimal float output. */
  private static final java.text.DecimalFormat DF =
    new java.text.DecimalFormat("#####0.0######", LOC);

  /**
   * Creates a byte array representation from the specified double value;
   * inspired by Xavier Franc's Qizx.
   * @param dbl double value to be converted
   * @return byte array
   */
  public static byte[] token(final double dbl) {
    final byte[] b = tok(dbl);
    if(b != null) return b;

    final double a = Math.abs(dbl);
    return chopNumber(token(a >= 1e-6 && a < 1e6 ?
        DD.format(dbl) : SD.format(dbl)));
  }

  /**
   * Creates a byte array representation from the specified float value.
   * @param flt float value to be converted
   * @return byte array
   */
  public static byte[] token(final float flt) {
    final byte[] b = tok(flt);
    if(b != null) return b;

    // not that brilliant here.. no chance for elegant code either
    // due to the nifty differences between Java and XQuery
    for(int i = 0; i < FLT.length; ++i) if(flt == FLT[i]) return FLTSTR[i];
    final float a = Math.abs(flt);
    final boolean small = a >= 1e-6f && a < 1e6f;
    String s1 = small ? DF.format(flt) : SF.format(flt);
    final String s2 = Float.toString(flt);
    if(s2.length() < s1.length() && (!s2.contains("E") || !small)) s1 = s2;
    return chopNumber(token(s1));
  }

  /**
   * Checks if the specified value equals a constant token.
   * @param dbl value to be converted
   * @return byte array or zero, or {@code null}
   */
  private static byte[] tok(final double dbl) {
    if(dbl == 1 / 0d) return INF;
    if(dbl == -1 / 0d) return NINF;
    if(dbl == 0) return 1 / dbl > 0 ? ZERO : MZERO;
    if(Double.isNaN(dbl)) return NAN;
    final double a = Math.abs(dbl);
    if(a < 1e6) {
      final int i = (int) dbl;
      if(i == dbl) return token(i);
    }
    return null;
  }

  /**
   * Finishes the numeric token, removing trailing zeroes.
   * @param token token to be modified
   * @return token
   */
  public static byte[] chopNumber(final byte[] token) {
    if(!contains(token, '.') || contains(token, 'e') ||
        contains(token, 'E')) return token;
    // remove trailing zeroes
    int l = token.length;
    while(--l > 0 && token[l] == '0');
    return substring(token, 0, token[l] == '.' ? l : l + 1);
  }

  /** Constant float values. */
  private static final float[] FLT = { 1.0E17f, 1.0E15f, 1.0E13f, 1.0E11f,
    -1.0E17f, -1.0E15f, -1.0E13f, -1.0E11f };
  /** String representations of float values. */
  private static final byte[][] FLTSTR = tokens("1.0E17", "1.0E15",
    "1.0E13", "1.0E11", "-1.0E17", "-1.0E15", "-1.0E13", "-1.0E11");

  /**
   * Converts the specified token into a double value.
   * {@link Double#NaN} is returned if the input is invalid.
   * @param token token to be converted
   * @return resulting double value
   */
  public static double toDouble(final byte[] token) {
    final int tl = token.length;
    boolean f = false;
    for(final int t : token) {
      if(t >= 0 && t <= ' ' || digit(t)) continue;
      f = t == 'e' || t == 'E' || t == '.' || t == '-';
      if(!f) return Double.NaN;
    }
    if(f || tl > 9) return dbl(token);
    final int d = toInt(token);
    return d == Integer.MIN_VALUE ? Double.NaN : d;
  }

  /**
   * Converts the specified token into a double value.
   * {@link Double#NaN} is returned when the input is invalid.
   * @param token token to be converted
   * @return resulting double value
   */
  private static double dbl(final byte[] token) {
    try {
      return Double.parseDouble(string(token));
    } catch(final Exception ex) {
      return Double.NaN;
    }
  }

  /**
   * Converts the specified string into an long value.
   * {@link Long#MIN_VALUE} is returned when the input is invalid.
   * @param string string to be converted
   * @return resulting long value
   */
  public static long toLong(final String string) {
    return toLong(token(string));
  }

  /**
   * Converts the specified token into an long value.
   * {@link Long#MIN_VALUE} is returned when the input is invalid.
   * @param token token to be converted
   * @return resulting long value
   */
  public static long toLong(final byte[] token) {
    return toLong(token, 0, token.length);
  }

  /**
   * Converts the specified token into an long value.
   * {@link Long#MIN_VALUE} is returned when the input is invalid.
   * @param token token to be converted
   * @param start first byte to be parsed
   * @param end last byte to be parsed - exclusive
   * @return resulting long value
   */
  public static long toLong(final byte[] token, final int start,
      final int end) {
    int t = start;
    while(t < end && token[t] <= ' ') ++t;
    if(t == end) return Long.MIN_VALUE;
    boolean m = false;
    if(token[t] == '-' || token[t] == '+') m = token[t++] == '-';
    if(t == end) return Long.MIN_VALUE;
    long v = 0;
    for(; t < end; ++t) {
      final byte c = token[t];
      if(c < '0' || c > '9') break;
      final long w = (v << 3) + (v << 1) + c - '0';
      if(w < v) return Long.MIN_VALUE;
      v = w;
    }
    while(t < end && token[t] <= ' ') ++t;
    return t < end ? Long.MIN_VALUE : m ? -v : v;
  }

  /**
   * Converts the specified string into an integer value.
   * {@link Integer#MIN_VALUE} is returned when the input is invalid.
   * @param string string to be converted
   * @return resulting integer value
   */
  public static int toInt(final String string) {
    return toInt(token(string));
  }

  /**
   * Converts the specified token into an integer value.
   * {@link Integer#MIN_VALUE} is returned when the input is invalid.
   * @param token token to be converted
   * @return resulting integer value
   */
  public static int toInt(final byte[] token) {
    return toInt(token, 0, token.length);
  }

  /**
   * Converts the specified token into an integer value.
   * {@link Integer#MIN_VALUE} is returned when the input is invalid.
   * @param token token to be converted
   * @param start first byte to be parsed
   * @param end last byte to be parsed (exclusive)
   * @return resulting integer value
   */
  public static int toInt(final byte[] token, final int start, final int end) {
    int t = start;
    while(t < end && token[t] <= ' ') ++t;
    if(t == end) return Integer.MIN_VALUE;
    boolean m = false;
    if(token[t] == '-' || token[t] == '+') m = token[t++] == '-';
    if(t == end) return Integer.MIN_VALUE;
    int v = 0;
    for(; t < end; ++t) {
      final byte c = token[t];
      if(c < '0' || c > '9') break;
      v = (v << 3) + (v << 1) + c - '0';
    }
    while(t < end && token[t] <= ' ') ++t;
    return t < end ? Integer.MIN_VALUE : m ? -v : v;
  }

  /**
   * Converts the specified token into a positive integer value.
   * {@link Integer#MIN_VALUE} is returned if non-digits are found
   * or if the input is longer than nine characters.
   * @param token token to be converted
   * @return resulting integer value
   */
  public static int toSimpleInt(final byte[] token) {
    final int te = token.length;
    if(te >= 10 || te == 0) return Integer.MIN_VALUE;
    if(token[0] == '0') return te == 1 ? 0 : Integer.MIN_VALUE;

    int v = 0;
    for(int ts = 0; ts < te; ++ts) {
      final byte c = token[ts];
      if(c < '0' || c > '9') return Integer.MIN_VALUE;
      v = (v << 3) + (v << 1) + c - '0';
    }
    return v;
  }

  /**
   * Calculates a hash code for the specified token.
   * @param token specified token
   * @return hash code
   */
  public static int hash(final byte[] token) {
    int h = 0;
    final int l = Math.min(token.length, MAXLEN);
    for(int i = 0; i != l; ++i) h = (h << 5) - h + token[i];
    return h;
  }

  /**
   * Compares two tokens for equality.
   * @param token1 first token
   * @param token2 token to be compared
   * @return true if the arrays are equal
   */
  public static boolean eq(final byte[] token1, final byte[] token2) {
    final int tl = token2.length;
    if(tl != token1.length) return false;
    for(int t = 0; t != tl; ++t) if(token2[t] != token1[t]) return false;
    return true;
  }

  /**
   * Calculates the difference of two tokens.
   * @param token first token
   * @param compare token to be compared
   * @return 0 if tokens are equal, negative if first token is smaller,
   *   positive if first token is bigger
   */
  public static int diff(final byte[] token, final byte[] compare) {
    final int l = Math.min(token.length, compare.length);
    for(int i = 0; i != l; ++i) {
      final int c = (token[i] & 0xFF) - (compare[i] & 0xFF);
      if(c != 0) return c;
    }
    return token.length - compare.length;
  }

  /**
   * Calculates the difference of two characters.
   * @param char1 first character
   * @param char2 character to be compared
   * @return 0 if characters are equal, negative if first token is smaller,
   *   positive if first character is bigger
   */
  public static int diff(final byte char1, final byte char2) {
    return (char1 & 0xFF) - (char2 & 0xFF);
  }

  /**
   * Checks if the first token contains the second token.
   * @param token token
   * @param sub token to be found
   * @return result of test
   */
  public static boolean contains(final byte[] token, final byte[] sub) {
    return indexOf(token, sub) != -1;
  }

  /**
   * Checks if the first token contains the specified character.
   * @param token token
   * @param c character to be found
   * @return result of test
   */
  public static boolean contains(final byte[] token, final int c) {
    return indexOf(token, c) != -1;
  }

  /**
   * Returns the position of the specified character or -1.
   * @param token token
   * @param c character to be found
   * @return result of test
   */
  public static int indexOf(final byte[] token, final int c) {
    final int tl = token.length;
    for(int t = 0; t < tl; ++t) if(token[t] == c) return t;
    return -1;
  }

  /**
   * Returns the position of the specified token or -1.
   * @param token token
   * @param sub token to be found
   * @return result of test
   */
  public static int indexOf(final byte[] token, final byte[] sub) {
    return indexOf(token, sub, 0);
  }

  /**
   * Returns the position of the specified token or -1.
   * @param token token
   * @param sub token to be found
   * @param pos start position
   * @return result of test
   */
  public static int indexOf(final byte[] token, final byte[] sub,
      final int pos) {
    final int sl = sub.length;
    if(sl == 0) return 0;
    final int tl = token.length - sl;
    if(pos > tl) return -1;

    // compare tokens character wise
    for(int t = pos; t <= tl; ++t) {
      int s = 0;
      while(sub[s] == token[t + s]) if(++s == sl) return t;
    }
    return -1;
  }

  /**
   * Checks if the first token starts with the specified character.
   * @param token token
   * @param ch character to be found
   * @return result of test
   */
  public static boolean startsWith(final byte[] token, final int ch) {
    return token.length != 0 && token[0] == ch;
  }

  /**
   * Checks if the first token starts with the second token.
   * @param token token
   * @param sub token to be found
   * @return result of test
   */
  public static boolean startsWith(final byte[] token, final byte[] sub) {
    final int sl = sub.length;
    if(sl > token.length) return false;
    for(int s = 0; s < sl; ++s) if(sub[s] != token[s]) return false;
    return true;
  }

  /**
   * Checks if the first token starts with the specified character.
   * @param token token
   * @param ch character to be bound
   * @return result of test
   */
  public static boolean endsWith(final byte[] token, final int ch) {
    return token.length != 0 && token[token.length - 1] == ch;
  }

  /**
   * Checks if the first token ends with the second token.
   * @param token token
   * @param sub token to be found
   * @return result of test
   */
  public static boolean endsWith(final byte[] token, final byte[] sub) {
    final int sl = sub.length;
    final int tl = token.length;
    if(sl > tl) return false;
    for(int s = sl; s > 0; s--) if(sub[sl - s] != token[tl - s]) return false;
    return true;
  }

  /**
   * Returns a substring of the specified token.
   * Note that this method does not correctly split UTF8 character;
   * use {@link #subtoken} instead.
   * @param token input token
   * @param start start position
   * @return substring
   */
  public static byte[] substring(final byte[] token, final int start) {
    return substring(token, start, token.length);
  }

  /**
   * Returns a substring of the specified token.
   * Note that this method does not correctly split UTF8 character;
   * use {@link #subtoken} instead.
   * @param token input token
   * @param start start position
   * @param end end position
   * @return substring
   */
  public static byte[] substring(final byte[] token, final int start,
      final int end) {

    final int s = Math.max(0, start);
    final int e = Math.min(end, token.length);
    if(s == 0 && e == token.length) return token;
    return s >= e ? EMPTY : Arrays.copyOfRange(token, s, e);
  }

  /**
   * Returns a partial token.
   * @param token input token
   * @param start start position
   * @return resulting text
   */
  public static byte[] subtoken(final byte[] token, final int start) {
    return subtoken(token, start, token.length);
  }

  /**
   * Returns a partial token.
   * @param token input text
   * @param start start position
   * @param end end position
   * @return resulting text
   */
  public static byte[] subtoken(final byte[] token, final int start,
      final int end) {

    int s = Math.max(0, start);
    final int e = Math.min(end, token.length);
    if(s == 0 && e == token.length) return token;
    if(s >= e) return EMPTY;

    int t = Math.max(0, s - 4);
    for(; t != s && t < e; t += cl(token, t)) {
      if(t >= s) s = t;
    }
    for(; t < e; t += cl(token, t));
    return Arrays.copyOfRange(token, s, t);
  }

  /**
   * Splits the token at all whitespaces and returns a array with all tokens.
   * @param token token to be split
   * @param sep separation character
   * @return array
   */
  public static byte[][] split(final byte[] token, final int sep) {
    final int l = token.length;
    final byte[][] split = new byte[l][];

    int s = 0;
    final TokenBuilder tb = new TokenBuilder();
    for(int i = 0; i < l; i += cl(token, i)) {
      final int c = cp(token, i);
      if(c == sep) {
        if(tb.size() != 0) {
          split[s++] = tb.finish();
          tb.reset();
        }
      } else {
        tb.add(c);
      }
    }
    if(tb.size() != 0) split[s++] = tb.finish();
    return Array.copyOf(split, s);
  }

  /**
   * Checks if the specified token has only whitespaces.
   * @param token token
   * @return true if all characters are whitespaces
   */
  public static boolean ws(final byte[] token) {
    final int tl = token.length;
    for(int i = 0; i < tl; ++i) if(token[i] < 0 || token[i] > ' ') return false;
    return true;
  }

  /**
   * Replaces the specified character and returns the result token.
   * @param token token to be checked
   * @param search the character to be replaced
   * @param replace the new character
   * @return resulting token
   */
  public static byte[] replace(final byte[] token, final int search,
      final int replace) {

    final int tl = token.length;
    final byte[] tok = new byte[tl];
    for(int i = 0; i < tl; ++i) {
      tok[i] = token[i] == search ? (byte) replace : token[i];
    }
    return tok;
  }

  /**
   * Removes leading and trailing whitespaces from the specified token.
   * @param token token to be trimmed
   * @return trimmed token
   */
  public static byte[] trim(final byte[] token) {
    int s = -1;
    int e = token.length;
    while(++s < e) if(token[s] > ' ' || token[s] < 0) break;
    while(--e > s) if(token[e] > ' ' || token[e] < 0) break;
    if(++e == token.length && s == 0) return token;
    return s == e ? EMPTY : Arrays.copyOfRange(token, s, e);
  }

  /**
   * Chops a token to the specified length and adds dots.
   * @param token token to be chopped
   * @param max maximum length
   * @return chopped token
   */
  public static byte[] chop(final byte[] token, final int max) {
    if(token.length <= max) return token;
    final byte[] tt = Arrays.copyOf(token, max);
    if(max > 2) tt[max - 3] = '.';
    if(max > 1) tt[max - 2] = '.';
    if(max > 0) tt[max - 1] = '.';
    return tt;
  }

  /**
   * Concatenates two tokens.
   * @param token1 first token
   * @param token2 second token
   * @return resulting array
   */
  public static byte[] concat(final byte[] token1, final byte[] token2) {
    final int t1 = token1.length;
    final int t2 = token2.length;
    final byte[] tmp = new byte[t1 + t2];
    System.arraycopy(token1, 0, tmp, 0, t1);
    System.arraycopy(token2, 0, tmp, t1, t2);
    return tmp;
  }

  /**
   * Concatenates three tokens. A {@link TokenBuilder} instance can be used to
   * concatenate more than three tokens.
   * @param token1 first token
   * @param token2 second token
   * @param token3 third token
   * @return resulting array
   */
  public static byte[] concat(final byte[] token1, final byte[] token2,
      final byte[] token3) {

    final int t1 = token1.length;
    final int t2 = token2.length;
    final int t3 = token3.length;
    final byte[] tmp = new byte[t1 + t2 + t3];
    System.arraycopy(token1, 0, tmp, 0, t1);
    System.arraycopy(token2, 0, tmp, t1, t2);
    System.arraycopy(token3, 0, tmp, t1 + t2, t3);
    return tmp;
  }

  /**
   * Deletes the specified character from the token.
   * @param token token
   * @param ch character to be removed
   * @return resulting token
   */
  public static byte[] delete(final byte[] token, final int ch) {
    final TokenBuilder tb = new TokenBuilder(token.length);
    final int tl = token.length;
    for(int i = 0; i < tl; i += cl(token, i)) {
      final int c = cp(token, i);
      if(c != ch) tb.add(c);
    }
    return tb.finish();
  }

  /**
   * Normalizes all whitespace occurrences from the specified token.
   * @param token token
   * @return normalized token
   */
  public static byte[] norm(final byte[] token) {
    final int l = token.length;
    final byte[] tmp = new byte[l];
    int c = 0;
    boolean ws1 = true;
    for(int i = 0; i < l; ++i) {
      final boolean ws2 = ws(token[i]);
      if(ws2 && ws1) continue;
      tmp[c++] = ws2 ? (byte) ' ' : token[i];
      ws1 = ws2;
    }
    if(c > 0 && ws(tmp[c - 1])) --c;
    return c == l ? tmp : Arrays.copyOf(tmp, c);
  }

  /**
   * Checks if the specified character is a whitespace.
   * @param ch the letter to be checked
   * @return result of comparison
   */
  public static boolean ws(final int ch) {
    return ch == 0x09 || ch == 0x0A || ch == 0x0D || ch == 0x20;
  }

  /**
   * Checks if the specified character is a computer letter (A - Z, a - z, _).
   * @param ch the letter to be checked
   * @return result of comparison
   */
  public static boolean letter(final int ch) {
    return ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z' || ch == '_';
  }

  /**
   * Checks if the specified character is a digit (0 - 9).
   * @param ch the letter to be checked
   * @return result of comparison
   */
  public static boolean digit(final int ch) {
    return ch >= '0' && ch <= '9';
  }

  /**
   * Checks if the specified character is a computer letter or digit.
   * @param ch the letter to be checked
   * @return result of comparison
   */
  public static boolean letterOrDigit(final int ch) {
    return letter(ch) || digit(ch);
  }

  /**
   * Returns true if the specified character is a full-text letter or digit.
   * @param ch character to be tested
   * @return result of check
   */
  public static boolean ftChar(final int ch) {
    return ch >= '0' && (ch < 0x80 ? LOD[ch - '0'] :
      Character.isLetterOrDigit(ch));
  }

  /** Letter-or-digit table for ASCII codes larger than '0'. */
  private static final boolean[] LOD = {
    true,  true,  true,  true,  true,  true,  true,  true,
    true,  true,  false, false, false, false, false, false,
    false, true,  true,  true,  true,  true,  true,  true,
    true,  true,  true,  true,  true,  true,  true,  true,
    true,  true,  true,  true,  true,  true,  true,  true,
    true,  true,  true,  false, false, false, false, false,
    false, true,  true,  true,  true,  true,  true,  true,
    true,  true,  true,  true,  true,  true,  true,  true,
    true,  true,  true,  true,  true,  true,  true,  true,
    true,  true,  true,  false, false, false, false, false
  };

  /**
   * Converts the specified token to upper case.
   * @param token token to be converted
   * @return resulting token
   */
  public static byte[] uc(final byte[] token) {
    if(ascii(token)) {
      final byte[] tok = new byte[token.length];
      for(int i = 0; i < token.length; ++i) tok[i] = (byte) uc(token[i]);
      return tok;
    }
    return token(string(token).toUpperCase());
  }

  /**
   * Converts a character to upper case.
   * @param ch character to be converted
   * @return resulting character
   */
  public static int uc(final int ch) {
    return ch >= 'a' && ch <= 'z' ? ch - 0x20 :
      ch > 0x7F ? Character.toUpperCase(ch) : ch;
  }

  /**
   * Converts the specified token to lower case.
   * @param token token to be converted
   * @return resulting token
   */
  public static byte[] lc(final byte[] token) {
    if(ascii(token)) {
      final byte[] tok = new byte[token.length];
      for(int i = 0; i < token.length; ++i) tok[i] = (byte) lc(token[i]);
      return tok;
    }
    return token(string(token).toLowerCase());
  }

  /**
   * Converts a character to lower case.
   * @param ch character to be converted
   * @return resulting character
   */
  public static int lc(final int ch) {
    return ch >= 'A' && ch <= 'Z' ? ch | 0x20 :
      ch > 0x7F ? Character.toLowerCase(ch) : ch;
  }

  /**
   * Returns the prefix of the specified token.
   * @param name name
   * @return prefix or empty token if no prefix exists
   */
  public static byte[] pref(final byte[] name) {
    final int i = indexOf(name, ':');
    return i == -1 ? EMPTY : substring(name, 0, i);
  }

  /**
   * Returns the local name of the specified name.
   * @param name name
   * @return local name
   */
  public static byte[] ln(final byte[] name) {
    final int i = indexOf(name, ':');
    return i == -1 ? name : substring(name, i + 1);
  }

  /**
   * Returns a URI encoded token.
   * @param token token
   * @param iri input
   * @return encoded token
   */
  public static byte[] uri(final byte[] token, final boolean iri) {
    final int tl = token.length;
    final TokenBuilder tb = new TokenBuilder();
    for(int t = 0; t < tl; ++t) {
      final byte b = token[t];
      if(letterOrDigit(b) || contains(iri ? IRIRES : RES, b)) tb.addByte(b);
      else hex(tb, b);
    }
    return tb.finish();
  }

  /**
   * Escapes the specified token.
   * @param token token
   * @return escaped token
   */
  public static byte[] escape(final byte[] token) {
    final int tl = token.length;
    final TokenBuilder tb = new TokenBuilder();
    for(int t = 0; t < tl; ++t) {
      final byte b = token[t];
      if(b >= 0x20 && b <= 0x7e) tb.addByte(b);
      else hex(tb, b);
    }
    return tb.finish();
  }

  /**
   * Adds the specified byte in hex code.
   * @param tb token builder
   * @param b byte to be added
   */
  private static void hex(final TokenBuilder tb, final byte b) {
    tb.add('%');
    tb.addByte(HEX[(b & 0xFF) >> 4]);
    tb.addByte(HEX[b & 0xFF & 15]);
  }

  /**
   * Returns an MD5 hash.
   * @param string string to be hashed
   * @return hash
   */
  public static String md5(final String string) {
    try {
      final MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(token(string));
      final ByteList bl = new ByteList();
      for(final byte b : md.digest()) {
        final int h = b >> 4 & 0x0F;
        bl.add(h + (h > 9 ? 0x57 : 0x30));
        final int l = b & 0x0F;
        bl.add(l + (l > 9 ? 0x57 : 0x30));
      }
      return string(bl.toArray());
    } catch(final Exception ex) {
      Util.notexpected(ex);
      return null;
    }
  }

  /**
   * Returns a normalized character without diacritics.
   * This method supports all latin1 characters, including supplements.
   * @param ch character to be normalized
   * @return resulting character
   */
  public static int norm(final int ch) {
    return ch < 0x80 || ch >= 0x200 ? ch : ch()[ch];
  }

  /**
   * Initializes the array of normalized characters.
   * @return normalization array
   */
  private static synchronized char[] ch() {
    if(norm == null) {
      // will be only initialized if needed
      norm = new char[0x200];
      for(int n = 0; n < norm.length; ++n) norm[n] = (char) n;
      for(int n = 0; n < NC.length; ++n) norm[NC[n][0]] = NC[n][1];
    }
    return norm;
  }

  /** Mapping table for character normalization. */
  private static char[] norm;

  /** Normalized characters. */
  private static final char[][] NC = {
    { '\u00C0', 'A' }, { '\u00C1', 'A' }, { '\u00C2', 'A' }, { '\u00C3', 'A' },
    { '\u00C4', 'A' }, { '\u00C5', 'A' }, { '\u00C6', 'A' }, { '\u00C7', 'C' },
    { '\u00C8', 'E' }, { '\u00C9', 'E' }, { '\u00CA', 'E' }, { '\u00CB', 'E' },
    { '\u00CC', 'I' }, { '\u00CD', 'I' }, { '\u00CE', 'I' }, { '\u00CF', 'I' },
    { '\u00D0', 'D' }, { '\u00D1', 'N' }, { '\u00D2', 'O' }, { '\u00D3', 'O' },
    { '\u00D4', 'O' }, { '\u00D5', 'O' }, { '\u00D6', 'O' }, { '\u00D8', 'O' },
    { '\u00D9', 'U' }, { '\u00DA', 'U' }, { '\u00DB', 'U' }, { '\u00DC', 'U' },
    { '\u00DD', 'Y' }, { '\u00DE', 'd' }, { '\u00DF', 's' }, { '\u00E0', 'a' },
    { '\u00E1', 'a' }, { '\u00E2', 'a' }, { '\u00E3', 'a' }, { '\u00E4', 'a' },
    { '\u00E5', 'a' }, { '\u00E6', 'a' }, { '\u00E7', 'c' }, { '\u00E8', 'e' },
    { '\u00E9', 'e' }, { '\u00EA', 'e' }, { '\u00EB', 'e' }, { '\u00EC', 'i' },
    { '\u00ED', 'i' }, { '\u00EE', 'i' }, { '\u00EF', 'i' }, { '\u00F0', 'd' },
    { '\u00F1', 'n' }, { '\u00F2', 'o' }, { '\u00F3', 'o' }, { '\u00F4', 'o' },
    { '\u00F5', 'o' }, { '\u00F6', 'o' }, { '\u00F8', 'o' }, { '\u00F9', 'u' },
    { '\u00FA', 'u' }, { '\u00FB', 'u' }, { '\u00FC', 'u' }, { '\u00FD', 'y' },
    { '\u00FE', 'd' }, { '\u00FF', 'y' }, { '\u0100', 'A' }, { '\u0101', 'a' },
    { '\u0102', 'A' }, { '\u0103', 'a' }, { '\u0104', 'A' }, { '\u0105', 'a' },
    { '\u0106', 'C' }, { '\u0107', 'c' }, { '\u0108', 'C' }, { '\u0109', 'c' },
    { '\u010A', 'C' }, { '\u010B', 'c' }, { '\u010C', 'C' }, { '\u010D', 'c' },
    { '\u010E', 'D' }, { '\u010F', 'd' }, { '\u0110', 'D' }, { '\u0111', 'd' },
    { '\u0112', 'E' }, { '\u0113', 'e' }, { '\u0114', 'E' }, { '\u0115', 'e' },
    { '\u0116', 'E' }, { '\u0117', 'e' }, { '\u0118', 'E' }, { '\u0119', 'e' },
    { '\u011A', 'E' }, { '\u011B', 'e' }, { '\u011C', 'G' }, { '\u011D', 'g' },
    { '\u011E', 'G' }, { '\u011F', 'g' }, { '\u0120', 'G' }, { '\u0121', 'g' },
    { '\u0122', 'G' }, { '\u0123', 'g' }, { '\u0124', 'H' }, { '\u0125', 'h' },
    { '\u0126', 'H' }, { '\u0127', 'h' }, { '\u0128', 'I' }, { '\u0129', 'i' },
    { '\u012A', 'I' }, { '\u012B', 'i' }, { '\u012C', 'I' }, { '\u012D', 'i' },
    { '\u012E', 'I' }, { '\u012F', 'i' }, { '\u0130', 'I' }, { '\u0131', 'i' },
    { '\u0132', 'I' }, { '\u0133', 'i' }, { '\u0134', 'J' }, { '\u0135', 'j' },
    { '\u0136', 'K' }, { '\u0137', 'k' }, { '\u0138', 'k' }, { '\u0139', 'L' },
    { '\u013A', 'l' }, { '\u013B', 'L' }, { '\u013C', 'l' }, { '\u013D', 'L' },
    { '\u013E', 'l' }, { '\u013F', 'L' }, { '\u0140', 'l' }, { '\u0141', 'L' },
    { '\u0142', 'l' }, { '\u0143', 'N' }, { '\u0144', 'n' }, { '\u0145', 'N' },
    { '\u0146', 'n' }, { '\u0147', 'N' }, { '\u0148', 'n' }, { '\u0149', 'n' },
    { '\u014A', 'N' }, { '\u014B', 'n' }, { '\u014C', 'O' }, { '\u014D', 'o' },
    { '\u014E', 'O' }, { '\u014F', 'o' }, { '\u0150', 'O' }, { '\u0151', 'o' },
    { '\u0152', 'O' }, { '\u0153', 'o' }, { '\u0154', 'R' }, { '\u0155', 'r' },
    { '\u0156', 'R' }, { '\u0157', 'r' }, { '\u0158', 'R' }, { '\u0159', 'r' },
    { '\u015A', 'S' }, { '\u015B', 's' }, { '\u015C', 'S' }, { '\u015D', 's' },
    { '\u015E', 'S' }, { '\u015F', 's' }, { '\u0160', 'S' }, { '\u0161', 's' },
    { '\u0162', 'T' }, { '\u0163', 't' }, { '\u0164', 'T' }, { '\u0165', 't' },
    { '\u0166', 'T' }, { '\u0167', 't' }, { '\u0168', 'U' }, { '\u0169', 'u' },
    { '\u016A', 'U' }, { '\u016B', 'u' }, { '\u016C', 'U' }, { '\u016D', 'u' },
    { '\u016E', 'U' }, { '\u016F', 'u' }, { '\u0170', 'U' }, { '\u0171', 'u' },
    { '\u0172', 'U' }, { '\u0173', 'u' }, { '\u0174', 'W' }, { '\u0175', 'w' },
    { '\u0176', 'Y' }, { '\u0177', 'y' }, { '\u0178', 'Y' }, { '\u0179', 'Z' },
    { '\u017A', 'z' }, { '\u017B', 'Z' }, { '\u017C', 'z' }, { '\u017D', 'Z' },
    { '\u017E', 'z' }, { '\u01FA', 'A' }, { '\u01FB', 'a' }, { '\u01FC', 'A' },
    { '\u01FD', 'a' }, { '\u01FE', 'O' }, { '\u01FF', 'o' }
  };
}
