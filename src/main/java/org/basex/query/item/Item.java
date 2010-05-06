package org.basex.query.item;

import static org.basex.query.QueryText.*;
import static org.basex.query.QueryTokens.*;
import java.io.IOException;
import java.math.BigDecimal;
import org.basex.core.Main;
import org.basex.data.Serializer;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.expr.Expr;
import org.basex.query.iter.Iter;
import org.basex.query.util.Err;
import org.basex.query.util.Var;
import org.basex.util.Token;

/**
 * Abstract item.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public abstract class Item extends Expr {
  /** Score value. */
  public double score;
  /** Data type. */
  public Type type;

  /**
   * Constructor.
   * @param t data type
   */
  protected Item(final Type t) {
    type = t;
  }

  @Override
  public Expr comp(final QueryContext ctx) {
    return this;
  }

  @Override
  public Iter iter(final QueryContext ctx) {
    return iter();
  }

  /**
   * Returns an item iterator.
   * @return iterator
   */
  public Iter iter() {
    return new Iter() {
      private boolean more;
      @Override
      public Item next() { return (more ^= true) ? Item.this : null; }
      @Override
      public int size() { return 1; }
      @Override
      public Item get(final long i) { return i == 0 ? Item.this : null; }
    };
  }

  @Override
  @SuppressWarnings("unused")
  public Item atomic(final QueryContext ctx) throws QueryException {
    return this;
  }

  @Override
  @SuppressWarnings("unused")
  public Item ebv(final QueryContext ctx) throws QueryException {
    return this;
  }

  @Override
  public Item test(final QueryContext ctx) throws QueryException {
    return bool() ? this : null;
  }

  @Override
  public boolean i() {
    return true;
  }

  @Override
  public long size(final QueryContext ctx) {
    return 1;
  }

  /**
   * Checks if this is a numeric item.
   * @return result of check
   */
  public final boolean n() {
    return type.num;
  }

  /**
   * Checks if this is an untyped item.
   * @return result of check
   */
  public final boolean u() {
    return type.unt;
  }

  /**
   * Checks if this is a string item.
   * @return result of check
   */
  public final boolean s() {
    return type.str;
  }

  /**
   * Checks if this is a duration.
   * @return result of check
   */
  public final boolean d() {
    return type.dur;
  }

  /**
   * Checks if this is a node.
   * @return result of check
   */
  public final boolean node() {
    return type.node();
  }

  /**
   * Returns an atomized string.
   * @return string representation
   */
  public byte[] str() {
    Main.notexpected();
    return null;
  }

  /**
   * Returns a Java object.
   * @return string representation
   */
  public Object java() {
    return Token.string(str());
  }

  /**
   * Returns a boolean representation of the item.
   * @return boolean value
   * @throws QueryException query exception
   */
  public boolean bool() throws QueryException {
    Err.or(CONDTYPE, type, this);
    return false;
  }

  /**
   * Returns a decimal representation of the item.
   * @return decimal value
   * @throws QueryException query exception
   */
  @SuppressWarnings("unused")
  public BigDecimal dec() throws QueryException {
    Main.notexpected();
    return null;
  }

  /**
   * Returns an integer (long) representation of the item.
   * @return long value
   * @throws QueryException query exception
   */
  @SuppressWarnings("unused")
  public long itr() throws QueryException {
    Main.notexpected();
    return 0;
  }

  /**
   * Returns a float representation of the item.
   * @return float value
   * @throws QueryException query exception
   */
  @SuppressWarnings("unused")
  public float flt() throws QueryException {
    Main.notexpected();
    return 0;
  }

  /**
   * Returns a double representation of the item.
   * @return double value
   * @throws QueryException query exception
   */
  @SuppressWarnings("unused")
  public double dbl() throws QueryException {
    Main.notexpected();
    return 0;
  }

  /**
   * Checks the items for equality.
   * @param it item to be compared
   * @return result of check
   * @throws QueryException query exception
   */
  public abstract boolean eq(Item it) throws QueryException;

  /**
   * Returns the difference between the current and the specified item.
   * @param it item to be compared
   * @return difference
   * @throws QueryException query exception
   */
  public int diff(final Item it) throws QueryException {
    Err.cmp(it, this);
    return 0;
  }

  /**
   * Returns a score value.
   * @return score value
   */
  public double score() {
    return score;
  }

  /**
   * Sets a new score value.
   * @param s score value
   */
  public final void score(final double s) {
    score = s;
  }

  /**
   * Throws a cast error.
   * @param val cast value
   * @throws QueryException query exception
   */
  protected final void castErr(final Object val) throws QueryException {
    Err.or(FUNCAST, type, Err.chop(val));
  }

  /**
   * Serializes the item.
   * @param ser serializer
   * @throws IOException I/O exception
   */
  public void serialize(final Serializer ser) throws IOException {
    ser.item(str());
  }

  @Override
  public boolean uses(final Use u, final QueryContext ctx) {
    return false;
  }

  @Override
  public boolean removable(final Var v, final QueryContext ctx) {
    return true;
  }

  @Override
  public SeqType returned(final QueryContext ctx) {
    return new SeqType(type, SeqType.OCC_1);
  }

  @Override
  public boolean duplicates(final QueryContext ctx) {
    return false;
  }

  @Override
  public final String color() {
    return "9999FF";
  }

  @Override
  public final String name() {
    return type.name;
  }

  @Override
  public void plan(final Serializer ser) throws IOException {
    ser.openElement(this);
    ser.attribute(VAL, str());
    ser.closeElement();
  }

  @Override
  public int hashCode() {
    return Token.hash(str());
  }

  @Override
  public String toString() {
    return Token.string(str());
  }
  
  /**
   * Returns an item array with double the size of the input array. 
   * @param it item array
   * @return resulting array
   */
  public static Item[] extend(final Item[] it) {
    final int s = it.length;
    final Item[] tmp = new Item[s << 1];
    System.arraycopy(it, 0, tmp, 0, s);
    return tmp;
  }
}