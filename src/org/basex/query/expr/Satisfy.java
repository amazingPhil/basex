package org.basex.query.expr;

import static org.basex.query.QueryTokens.*;
import static org.basex.query.QueryText.*;

import java.io.IOException;
import org.basex.data.Serializer;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.item.Bln;
import org.basex.query.iter.Iter;
import org.basex.query.iter.ResetIter;
import org.basex.query.util.Var;

/**
 * Some/Every Satisfier Clause.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class Satisfy extends Single {
  /** For/Let expressions. */
  private final For[] fl;
  /** Every flag. */
  private final boolean every;

  /**
   * Constructor.
   * @param f variable inputs
   * @param s satisfier
   * @param e every flag
   */
  public Satisfy(final For[] f, final Expr s, final boolean e) {
    super(s);
    fl = f;
    every = e;
  }

  @Override
  public Expr comp(final QueryContext ctx) throws QueryException {
    for(int f = 0; f != fl.length; f++) {
      final Expr e = fl[f].comp(ctx);
      if(e.e()) {
        ctx.compInfo(every ? OPTTRUE : OPTFALSE, fl[f]);
        return Bln.get(every);
      }
      fl[f] = (For) e;
    }
    return super.comp(ctx);
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    final ResetIter[] iter = new ResetIter[fl.length];
    // casting is safe, but should be removed
    for(int f = 0; f < fl.length; f++) iter[f] = (ResetIter) ctx.iter(fl[f]);
    return Bln.get(iter(ctx, iter, 0)).iter();
  }

  /**
   * Performs a recursive iteration on the specified variable position.
   * @param ctx root reference
   * @param it iterator
   * @param p variable position
   * @return satisfied flag
   * @throws QueryException evaluation exception
   */
  private boolean iter(final QueryContext ctx, final ResetIter[] it,
      final int p) throws QueryException {

    final boolean last = p + 1 == fl.length;
    while(it[p].next().bool()) {
      if(every ^ (last ? ctx.iter(expr).ebv().bool() : iter(ctx, it, p + 1))) {
        for(final ResetIter ri : it) ri.reset();
        return !every;
      }
    }
    return every;
  }

  @Override
  public boolean usesVar(final Var v) {
    if(v == null) return true;
    for(final ForLet f : fl) {
      if(f.usesVar(v)) return true;
      if(f.shadows(v)) return false;
    }
    return super.usesVar(v);
  }

  @Override
  public Expr removeVar(final Var v) {
    for(final ForLet f : fl) {
      f.removeVar(v);
      if(f.shadows(v)) return this;
    }
    return super.removeVar(v);
  }

  @Override
  public void plan(final Serializer ser) throws IOException {
    ser.openElement(this);
    for(final Expr f : fl) f.plan(ser);
    expr.plan(ser);
    ser.closeElement();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(every ? EVERY : SOME);
    for(int i = 0; i < fl.length; i++) sb.append(" " + fl[i]);
    return sb.append(" " + SATISFIES + " " + expr).toString();
  }
}