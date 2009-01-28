package org.basex.query.expr;

import static org.basex.query.QueryTokens.*;
import static org.basex.query.QueryText.*;

import java.io.IOException;
import org.basex.data.Serializer;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.item.Item;
import org.basex.query.item.Seq;
import org.basex.query.iter.Iter;
import org.basex.query.iter.SeqIter;
import org.basex.query.util.Var;

/**
 * FLWOR Clause.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public class FLWOR extends Single {
  /** For/Let expressions. */
  protected ForLet[] fl;
  /** Where Expression. */
  protected Expr where;
  /** Order Expressions. */
  protected Order order;

  /**
   * Constructor.
   * @param f variable inputs
   * @param w where clause
   * @param o order expressions
   * @param r return expression
   */
  public FLWOR(final ForLet[] f, final Expr w, final Order o, final Expr r) {
    super(r);
    fl = f;
    where = w;
    order = o;
  }

  @Override
  public Expr comp(final QueryContext ctx) throws QueryException {
    final int vs = ctx.vars.size();
    
    for(int f = 0; f != fl.length; f++) {
      final Expr e = fl[f].comp(ctx);
      if(e.e()) {
        ctx.vars.reset(vs);
        ctx.compInfo(OPTFLWOR);
        return e;
      }
      fl[f] = (ForLet) e;
    }
    
    if(where != null) {
      where = where.comp(ctx);
      final boolean e = where.e();
      if(e || where.i()) {
        // test is always false: no results
        if(e || !((Item) where).bool()) {
          ctx.compInfo(OPTFALSE, where);
          ctx.vars.reset(vs);
          return Seq.EMPTY;
        }
        // always true: test can be skipped
        ctx.compInfo(OPTTRUE, where);
        where = null;
      }
    }

    if(order != null) order.comp(ctx);
    expr = expr.comp(ctx);
    
    ctx.vars.reset(vs);
    return this;
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    final SeqIter seq = new SeqIter();
    final Iter[] iter = new Iter[fl.length];
    for(int f = 0; f < fl.length; f++) iter[f] = ctx.iter(fl[f]);
    iter(ctx, seq, iter, 0);
    return order.iter(seq);
  }

  /**
   * Performs a recursive iteration on the specified variable position.
   * @param ctx root reference
   * @param seq result sequence
   * @param it iterator
   * @param p variable position
   * @throws QueryException evaluation exception
   */
  private void iter(final QueryContext ctx, final SeqIter seq,
      final Iter[] it, final int p) throws QueryException {

    final boolean more = p + 1 != fl.length;
    while(it[p].next().bool()) {
      if(more) {
        iter(ctx, seq, it, p + 1);
      } else {
        if(where == null || ctx.iter(where).ebv().bool()) {
          order.add(ctx);
          seq.add(ctx.iter(expr).finish());
        }
      }
    }
  }

  @Override
  public final boolean usesVar(final Var v) {
    if(v == null) return true;
    for(final ForLet f : fl) {
      if(f.usesVar(v)) return true;
      if(f.shadows(v)) return false;
    }
    return where != null && where.usesVar(v) || order != null &&
      order.usesVar(v) || super.usesVar(v);
  }

  @Override
  public Expr removeVar(final Var v) {
    for(final ForLet f : fl) {
      f.removeVar(v);
      if(f.shadows(v)) return this;
    }
    if(where != null) where = where.removeVar(v);
    if(order != null) order = order.removeVar(v);
    return super.removeVar(v);
  }

  @Override
  public final String color() {
    return "66FF66";
  }

  @Override
  public final void plan(final Serializer ser) throws IOException {
    ser.openElement(this, EVAL, ITER);
    for(final ForLet f : fl) f.plan(ser);
    if(where != null) {
      ser.openElement(WHR);
      where.plan(ser);
      ser.closeElement();
    }
    if(order != null) order.plan(ser);
    ser.openElement(RET);
    expr.plan(ser);
    ser.closeElement();
    ser.closeElement();
  }

  @Override
  public final String toString() {
    final StringBuilder sb = new StringBuilder();
    for(int i = 0; i != fl.length; i++) sb.append((i != 0 ? " " : "") + fl[i]);
    if(where != null) sb.append(" where " + where);
    if(order != null) sb.append(order);
    return sb.append(" return " + expr).toString();
  }
}