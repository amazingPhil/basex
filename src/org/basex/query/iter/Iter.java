package org.basex.query.iter;

import static org.basex.query.QueryText.*;

import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.expr.Expr;
import org.basex.query.item.Bln;
import org.basex.query.item.Item;
import org.basex.query.item.Seq;
import org.basex.query.util.Err;

/**
 * Iterator interface.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public abstract class Iter {
  /** Empty iterator. */
  public static final Iter EMPTY = new ResetIter() {
    @Override
    public Item next() { return null; }
    @Override
    public long size() { return 0; }
    @Override
    public void reset() { }
    @Override
    public String toString() { return "()"; }
  };
  
  /**
   * Returns the next item or null if no other items are found.
   * @return resulting item
   * @throws QueryException evaluation exception
   */
  public abstract Item next() throws QueryException;

  /**
   * Returns the number of entries. Warning: -1 is returned if the number
   * cannot be evaluated, so each method has to check and react on the
   * returned value.
   * @return number of entries
   */
  public long size() {
    return -1;
  }

  /**
   * Returns a sequence from all iterator values. Should be called before
   * {@link #next}.
   * @return sequence
   * @throws QueryException evaluation exception
   */
  public Item finish() throws QueryException {
    Item i = next();
    if(i == null) return Seq.EMPTY;
    
    Item[] item = { i };
    int s = 1;
    while((i = next()) != null) {
      if(s == item.length) {
        final Item[] tmp = new Item[s << 2];
        System.arraycopy(item, 0, tmp, 0, s);
        item = tmp;
      }
      item[s++] = i;
    }
    return Seq.get(item, s);
  }

  /**
   * Checks if the iterator can be dissolved into an effective boolean value.
   * If not, returns an error. If yes, returns the first value - which can be
   * also be e.g. an integer, which is later evaluated as position predicate.
   * Must be called before {@link #next} was called.
   * @return item
   * @throws QueryException evaluation exception
   */
  public final Item ebv() throws QueryException {
    final Item it = next();
    if(it == null) return Bln.FALSE;
    if(!it.node() && next() != null) Err.or(FUNSEQ, this);
    return it;
  }

  /**
   * Performs a predicate test and returns the item if test was successful.
   * @param ctx query context
   * @return item
   * @throws QueryException evaluation exception
   */
  public final Item test(final QueryContext ctx) throws QueryException {
    final Item it = ebv();
    return (it.n() ? it.dbl() == ctx.pos : it.bool()) ? it : null;
  }

  /**
   * Checks if the specified iterator contains a single item.
   * Returns null, the first item or an exception.
   * @param expr calling expression
   * @param empty allow empty sequences
   * @return item
   * @throws QueryException evaluation exception
   */
  public final Item atomic(final Expr expr, final boolean empty)
      throws QueryException {
    
    final long s = size();
    if(s != -1) {
      if(s == 1) return next();
      if(s == 0) {
        if(!empty) Err.empty(expr);
        return null;
      }
      seqErr(next(), next(), next(), expr);
    }

    final Item it = next();
    if(it == null) {
      if(!empty) Err.empty(expr);
      return null;
    }

    final Item n = next();
    if(n != null) seqErr(it, n, next(), expr);
    return it;
  }
  
  /**
   * Throws a sequence error.
   * @param i1 first item
   * @param i2 first item
   * @param i3 first item
   * @param expr expression
   * @throws QueryException evaluation exception
   */
  private static void seqErr(final Item i1, final Item i2, final Item i3,
      final Expr expr) throws QueryException {
    Err.or(XPSEQ, "(" + i1 + "," + i2 + (i3 != null ? ",..." : "") + ")",
        expr.info());
  }
}