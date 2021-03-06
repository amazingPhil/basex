package org.basex.query.func;

import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.expr.CmpV;
import org.basex.query.expr.Expr;
import org.basex.query.item.Empty;
import org.basex.query.item.Item;
import org.basex.query.item.Itr;
import org.basex.query.item.SeqType;
import org.basex.query.item.Type;
import org.basex.query.iter.Iter;
import org.basex.query.iter.ItemCache;
import org.basex.query.util.ItemSet;
import org.basex.util.InputInfo;

/**
 * Sequence functions.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
final class FNSeq extends Fun {
  /**
   * Constructor.
   * @param ii input info
   * @param f function definition
   * @param e arguments
   */
  protected FNSeq(final InputInfo ii, final FunDef f, final Expr... e) {
    super(ii, f, e);
  }

  @Override
  public Item item(final QueryContext ctx, final InputInfo ii)
      throws QueryException {
    switch(def) {
      case HEAD: return head(ctx);
      default:   return super.item(ctx, ii);
    }
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    switch(def) {
      case INDEXOF:  return indexOf(ctx);
      case DISTINCT: return distinctValues(ctx);
      case INSBEF:   return insertBefore(ctx);
      case REVERSE:  return reverse(ctx);
      case REMOVE:   return remove(ctx);
      case SUBSEQ:   return subsequence(ctx);
      case TAIL:     return tail(ctx);
      default:       return super.iter(ctx);
    }
  }

  @Override
  public Expr cmp(final QueryContext ctx) {
    // static typing:
    // index-of will create integers, insert-before might add new types
    if(def == FunDef.INDEXOF || def == FunDef.INSBEF) return this;

    // all other types will return existing types
    final Type t = expr[0].type().type;
    SeqType.Occ o = SeqType.Occ.ZM;

    // head will return at most one item
    if(def == FunDef.HEAD) o = SeqType.Occ.ZO;

    // at most one returned item
    if(def == FunDef.SUBSEQ && expr[0].type().one()) o = SeqType.Occ.ZO;

    type = SeqType.get(t, o);
    return this;
  }

  /**
   * Returns the first item in a sequence.
   * @param ctx query context
   * @return first item
   * @throws QueryException query exception
   */
  private Item head(final QueryContext ctx) throws QueryException {
    final Expr e = expr[0];
    return e.type().zeroOrOne() ? e.item(ctx, input) : e.iter(ctx).next();
  }

  /**
   * Returns all but the first item in a sequence.
   * @param ctx query context
   * @return iterator
   * @throws QueryException query exception
   */
  private Iter tail(final QueryContext ctx) throws QueryException {
    final Expr e = expr[0];
    if(e.type().zeroOrOne()) return Empty.ITER;

    final Iter ir = e.iter(ctx);
    if(ir.next() == null) return Empty.ITER;

    return new Iter() {
      @Override
      public Item next() throws QueryException {
        return ir.next();
      }
    };
  }

  /**
   * Returns the indexes of an item in a sequence.
   * @param ctx query context
   * @return position(s) of item
   * @throws QueryException query exception
   */
  private Iter indexOf(final QueryContext ctx) throws QueryException {
    final Item it = checkItem(expr[1], ctx);
    if(expr.length == 3) checkColl(expr[2], ctx);

    return new Iter() {
      final Iter ir = expr[0].iter(ctx);
      int c;

      @Override
      public Item next() throws QueryException {
        while(true) {
          final Item i = ir.next();
          if(i == null) return null;
          ++c;
          if(i.comparable(it) && CmpV.Op.EQ.e(input, i, it)) return Itr.get(c);
        }
      }
    };
  }

  /**
   * Returns all distinct values of a sequence.
   * @param ctx query context
   * @return distinct iterator
   * @throws QueryException query exception
   */
  private Iter distinctValues(final QueryContext ctx) throws QueryException {
    if(expr.length == 2) checkColl(expr[1], ctx);

    return new Iter() {
      final ItemSet map = new ItemSet();
      final Iter ir = expr[0].iter(ctx);

      @Override
      public Item next() throws QueryException {
        while(true) {
          Item i = ir.next();
          if(i == null) return null;
          ctx.checkStop();
          i = atom(i);
          if(map.index(input, i)) return i;
        }
      }
    };
  }

  /**
   * Inserts items before the specified position.
   * @param ctx query context
   * @return iterator
   * @throws QueryException query exception
   */
  private Iter insertBefore(final QueryContext ctx) throws QueryException {
    return new Iter() {
      final long pos = Math.max(1, checkItr(expr[1], ctx));
      final Iter iter = expr[0].iter(ctx);
      final Iter ins = expr[2].iter(ctx);
      long p = pos;
      boolean last;

      @Override
      public Item next() throws QueryException {
        if(last) return p > 0 ? ins.next() : null;
        final boolean sub = p == 0 || --p == 0;
        final Item i = (sub ? ins : iter).next();
        if(i != null) return i;
        if(sub) --p;
        else last = true;
        return next();
      }
    };
  }

  /**
   * Removes an item at a specified position in a sequence.
   * @param ctx query context
   * @return iterator without Item
   * @throws QueryException query exception
   */
  private Iter remove(final QueryContext ctx) throws QueryException {
    return new Iter() {
      final long pos = checkItr(expr[1], ctx);
      final Iter iter = expr[0].iter(ctx);
      long c;

      @Override
      public Item next() throws QueryException {
        return ++c != pos || iter.next() != null ? iter.next() : null;
      }
    };
  }

  /**
   * Creates a subsequence out of a sequence, starting with start and
   * ending with end.
   * @param ctx query context
   * @return subsequence
   * @throws QueryException query exception
   */
  private Iter subsequence(final QueryContext ctx) throws QueryException {
    final double ds = checkDbl(expr[1], ctx);
    if(Double.isNaN(ds)) return Empty.ITER;
    final long s = StrictMath.round(ds);

    long l = Long.MAX_VALUE;
    if(expr.length > 2) {
      final double dl = checkDbl(expr[2], ctx);
      if(Double.isNaN(dl)) return Empty.ITER;
      l = s + StrictMath.round(dl);
    }
    final long e = l;

    final Iter iter = ctx.iter(expr[0]);
    final long max = iter.size();
    return max != -1 ? new Iter() {
      // directly access specified items
      long c = Math.max(1, s);
      long m = Math.min(e, max + 1);

      @Override
      public Item next() throws QueryException {
        return c < m ? iter.get(c++ - 1) : null;
      }
      @Override
      public Item get(final long i) throws QueryException {
        return iter.get(c + i - 1);
      }
      @Override
      public long size() {
        return Math.max(0, m - c);
      }
      @Override
      public boolean reset() {
        c = Math.max(1, s);
        return true;
      }
    } : new Iter() {
      // run through all items
      long c;

      @Override
      public Item next() throws QueryException {
        while(true) {
          final Item i = iter.next();
          if(i == null || ++c >= e) return null;
          if(c >= s) return i;
        }
      }
    };
  }

  /**
   * Reverses a sequence.
   * @param ctx query context
   * @return iterator
   * @throws QueryException query exception
   */
  private Iter reverse(final QueryContext ctx) throws QueryException {
    final Iter iter = ctx.iter(expr[0]);
    // only one item found; no reversion necessary
    if(iter.size() == 1) return iter;

    // process any other iterator...
    return new Iter() {
      final Iter ir = iter.size() != -1 ? iter : ItemCache.get(iter);
      final long s = ir.size();
      long c = s;

      @Override
      public Item next() throws QueryException {
        return --c >= 0 ? ir.get(c) : null;
      }
      @Override
      public Item get(final long i) throws QueryException {
        return ir.get(s - i - 1);
      }
      @Override
      public long size() {
        return s;
      }
      @Override
      public boolean reset() {
        c = s;
        return true;
      }
    };
  }

  @Override
  public boolean uses(final Use u) {
    return u == Use.X30 && (def == FunDef.HEAD || def == FunDef.TAIL) ||
      super.uses(u);
  }
}
