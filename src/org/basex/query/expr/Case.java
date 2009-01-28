package org.basex.query.expr;

import static org.basex.query.QueryTokens.*;
import org.basex.BaseX;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.iter.Iter;
import org.basex.query.iter.SeqIter;
import org.basex.query.util.Var;

/**
 * Case expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class Case extends Single {
  /** Variable. */
  private final Var var;

  /**
   * Constructor.
   * @param v variable
   * @param r return expression
   */
  public Case(final Var v, final Expr r) {
    super(r);
    var = v;
  }

  @Override
  public Expr comp(final QueryContext ctx) {
    return this;
  }

  @Override
  public Iter iter(final QueryContext ctx) {
    BaseX.notexpected();
    return null;
  }

  @Override
  public boolean usesVar(final Var v) {
    return v == null || v.visible(var) && super.usesVar(v);
  }

  @Override
  public Case removeVar(final Var v) {
    if(!v.eq(var)) expr = expr.removeVar(v);
    return this;
  }
  
  /**
   * Evaluates the given sequence.
   * @param ctx query context
   * @param seq sequence to be checked
   * @return resulting item
   * @throws QueryException evaluation exception
   */
  Iter iter(final QueryContext ctx, final SeqIter seq) throws QueryException {
    if(!var.type.instance(seq)) return null;
    if(var.name == null) return ctx.iter(expr);

    final int s = ctx.vars.size();
    ctx.vars.add(var.bind(seq.finish(), ctx));
    final Iter sb = ctx.iter(expr);
    ctx.vars.reset(s);
    return sb;
  }

  @Override
  public Return returned(final QueryContext ctx) {
    return expr.returned(ctx);
  }

  @Override
  public String toString() {
    return CASE + " " + var.type + " " + RETURN + " " + expr;
  }
}