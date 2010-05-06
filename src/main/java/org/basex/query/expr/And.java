package org.basex.query.expr;

import static org.basex.query.QueryText.*;
import org.basex.query.IndexContext;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.QueryTokens;
import org.basex.query.item.Bln;
import org.basex.query.item.Item;
import org.basex.query.item.SeqType;
import org.basex.query.item.Type;
import org.basex.util.Array;

/**
 * And expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class And extends Arr {
  /**
   * Constructor.
   * @param e expression list
   */
  public And(final Expr[] e) {
    super(e);
  }

  @Override
  public Expr comp(final QueryContext ctx) throws QueryException {
    super.comp(ctx);

    for(int e = 0; e < expr.length; e++) {
      final Expr ex = expr[e];
      if(!ex.i()) continue;

      if(!((Item) ex).bool()) {
        // atomic items can be pre-evaluated
        ctx.compInfo(OPTFALSE, ex);
        return Bln.FALSE;
      }
      ctx.compInfo(OPTTRUE, ex);
      expr = Array.delete(expr, e--);
      if(expr.length == 0) return Bln.TRUE;
    }

    // merge predicates if possible
    Expr[] ex = {};
    Pos ps = null;
    CmpR cr = null;
    for(final Expr e : expr) {
      Expr tmp = null;
      if(e instanceof Pos) {
        // merge position predicates
        tmp = ps == null ? e : ps.intersect((Pos) e);
        if(!(tmp instanceof Pos)) return tmp;
        ps = (Pos) tmp;
      } else if(e instanceof CmpR) {
        // merge comparisons
        tmp = cr == null ? e : cr.intersect((CmpR) e);
        if(tmp instanceof CmpR) {
          cr = (CmpR) tmp;
        } else if(tmp != null) {
          return tmp;
        }
      }
      if(tmp == null) ex = Array.add(ex, e);
    }

    expr = ex;
    if(ps != null) expr = Array.add(expr, ps);
    if(cr != null) expr = Array.add(expr, cr);

    if(expr.length != 1) return this;
    final SeqType ret = expr[0].returned(ctx);
    return ret.type == Type.BLN && ret.occ == SeqType.OCC_1 ? expr[0] : this;
  }

  @Override
  public Bln atomic(final QueryContext ctx) throws QueryException {
    double s = 0;
    for(final Expr e : expr) {
      final Item it = e.ebv(ctx);
      if(!it.bool()) return Bln.FALSE;
      s = ctx.score.and(s, it.score());
    }
    // no scoring - return default boolean
    return s == 0 ? Bln.TRUE : Bln.get(s);
  }

  @Override
  public SeqType returned(final QueryContext ctx) {
    return SeqType.BLN;
  }

  @Override
  public boolean indexAccessible(final IndexContext ic) throws QueryException {
    int is = 0;
    boolean empty = false;
    final double[] ics = new double[expr.length];
    for(int e = 0; e < expr.length; e++) {
      if(!expr[e].indexAccessible(ic) || ic.seq) return false;
      ics[e] = ic.is;
      // evaluate empty result first
      if(ic.is == 0) {
        final Expr tmp = expr[e];
        expr[e] = expr[0];
        expr[0] = tmp;
        empty = true;
      }
      is += ic.is;
    }
    if(empty) {
      ic.is = 0;
    } else {
      // reorder arguments to speedup intersection
      final int[] ord = Array.createOrder(ics, false);
      final Expr[] ex = new Expr[ics.length];
      for(int e = 0; e < expr.length; e++) ex[e] = expr[ord[e]];
      expr = ex;
    }
    return true;
  }

  @Override
  public Expr indexEquivalent(final IndexContext ic) throws QueryException {
    super.indexEquivalent(ic);
    return new InterSect(expr);
  }

  @Override
  public String toString() {
    return toString(" " + QueryTokens.AND + " ");
  }
}