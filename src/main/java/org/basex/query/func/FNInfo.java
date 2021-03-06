package org.basex.query.func;

import static org.basex.query.util.Err.*;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.expr.Expr;
import org.basex.query.item.Item;
import org.basex.query.item.QNm;
import org.basex.query.item.Str;
import org.basex.query.item.Type;
import org.basex.query.item.Value;
import org.basex.query.iter.Iter;
import org.basex.query.iter.ItemCache;
import org.basex.util.InputInfo;
import org.basex.util.Token;

/**
 * Info functions.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
final class FNInfo extends Fun {
  /**
   * Constructor.
   * @param ii input info
   * @param f function definition
   * @param e arguments
   */
  protected FNInfo(final InputInfo ii, final FunDef f, final Expr... e) {
    super(ii, f, e);
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    switch(def) {
      case ERROR:
        final int al = expr.length;
        if(al == 0) FUNERR1.thrw(input);

        String code = FUNERR1.code();
        String msg = FUNERR1.desc;

        final Item it = expr[0].item(ctx, input);
        if(it == null) {
          if(al == 1) XPEMPTY.thrw(input, desc());
        } else {
          code = Token.string(((QNm) checkType(it, Type.QNM)).ln());
        }
        if(al > 1) msg = Token.string(checkEStr(expr[1], ctx));
        Value val = al > 2 ? expr[2].value(ctx) : null;
        final QueryException ex = new QueryException(input, code, val, msg);
        throw ex;
      case TRACE:
        val = expr[0].value(ctx);
        ctx.evalInfo(checkEStr(expr[1], ctx), val.toString());
        return val.iter();
      case ENVS:
        final ItemCache ir = new ItemCache();
        for(final Object k : System.getenv().keySet().toArray()) {
          ir.add(Str.get(k));
        }
        return ir;
      default:
        return super.iter(ctx);
    }
  }

  @Override
  public Item item(final QueryContext ctx, final InputInfo ii)
      throws QueryException {
    switch(def) {
      case ENV:
        final String e = System.getenv(Token.string(checkEStr(expr[0], ctx)));
        return e != null ? Str.get(e) : null;
      default:
        return super.item(ctx, ii);
    }
  }

  @Override
  public boolean vacuous() {
    return def == FunDef.ERROR;
  }

  @Override
  public boolean uses(final Use u) {
    return u == Use.X30 && (def == FunDef.ENV || def == FunDef.ENVS) ||
      u == Use.CTX && (def == FunDef.ERROR || def == FunDef.TRACE) ||
      super.uses(u);
  }
}
