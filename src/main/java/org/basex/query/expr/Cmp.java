package org.basex.query.expr;

import org.basex.query.QueryException;
import org.basex.query.expr.CmpV.Op;
import org.basex.query.func.Fun;
import org.basex.query.func.FunDef;
import org.basex.query.item.Bln;
import org.basex.query.item.Item;
import org.basex.query.path.AxisPath;
import org.basex.util.InputInfo;

/**
 * Abstract comparison.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
abstract class Cmp extends Arr {
  /**
   * Constructor.
   * @param ii input info
   * @param e1 first expression
   * @param e2 second expression
   */
  public Cmp(final InputInfo ii, final Expr e1, final Expr e2) {
    super(ii, e1, e2);
  }

  /**
   * Swaps the operands of the expression, if better performance is expected.
   * The operator itself needs to be swapped by the calling expression.
   * @return resulting expression
   */
  protected boolean swap() {
    // move value or path without root to second position
    final boolean swap = expr[0].value() && !expr[1].value() ||
      expr[0] instanceof AxisPath && ((AxisPath) expr[0]).root != null &&
      expr[1] instanceof AxisPath && ((AxisPath) expr[1]).root == null;

    if(swap) {
      final Expr tmp = expr[0];
      expr[0] = expr[1];
      expr[1] = tmp;
    }
    return swap;
  }

  /**
   * Rewrites a {@code count()} function.
   * @param o comparison operator
   * @return resulting expression
   * @throws QueryException query exception
   */
  protected Expr count(final Op o) throws QueryException {
    // evaluate argument
    final Expr a = expr[1];
    if(!a.item()) return this;
    final Item it = (Item) a;
    if(!it.num() && !it.unt()) return this;

    final double v = it.dbl(input);
    // TRUE: c > (v<0), c != (v<0), c >= (v<=0), c != not-int(v)
    if((o == Op.GT || o == Op.NE) && v < 0 || o == Op.GE && v <= 0 ||
       o == Op.NE && v != (int) v) return Bln.TRUE;
    // FALSE: c < (v<=0), c <= (v<0), c = (v<0), c = not-int(v)
    if(o == Op.LT && v <= 0 || (o == Op.LE || o == Op.EQ) && v < 0 ||
       o == Op.EQ && v != (int) v) return Bln.FALSE;
    // EXISTS: c > (v<1), c >= (v<=1), c != (v=0)
    if(o == Op.GT && v < 1 || o == Op.GE && v <= 1 || o == Op.NE && v == 0)
      return FunDef.EXISTS.get(input, ((Fun) expr[0]).expr);
    // EMPTY: c < (v<=1), c <= (v<1), c = (v=0)
    if(o == Op.LT && v <= 1 || o == Op.LE && v < 1 || o == Op.EQ && v == 0)
      return FunDef.EMPTY.get(input, ((Fun) expr[0]).expr);

    return this;
  }
}
