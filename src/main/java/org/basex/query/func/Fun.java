package org.basex.query.func;

import static org.basex.query.QueryTokens.*;
import java.io.IOException;
import org.basex.data.Serializer;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.expr.Arr;
import org.basex.query.expr.Expr;
import org.basex.query.item.Atm;
import org.basex.query.item.Item;
import org.basex.query.item.Str;
import org.basex.query.item.Type;
import org.basex.query.util.Err;
import org.basex.util.InputInfo;
import org.basex.util.Token;
import org.basex.util.TokenBuilder;

/**
 * Abstract function definition.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public abstract class Fun extends Arr {
  /** Function definition. */
  public FunDef def;

  /**
   * Constructor.
   * @param ii input info
   * @param d function definition
   * @param e arguments
   */
  protected Fun(final InputInfo ii, final FunDef d, final Expr... e) {
    super(ii, e);
    def = d;
    type = def.ret;
  }

  @Override
  public Expr comp(final QueryContext ctx) throws QueryException {
    // compile all arguments
    super.comp(ctx);
    // skip functions based on context or with non-values as arguments
    if(uses(Use.CTX) || !values()) return optPre(cmp(ctx), ctx);
    // pre-evaluate function
    return optPre(def.ret.zeroOrOne() ? item(ctx, input) : value(ctx), ctx);
  }

  /**
   * Performs function specific compilations.
   * @param ctx query context
   * @return evaluated item
   * @throws QueryException query exception
   */
  @SuppressWarnings("unused")
  public Expr cmp(final QueryContext ctx) throws QueryException {
    return this;
  }

  /**
   * Atomizes the specified item.
   * @param it input item
   * @return atomized item
   */
  protected Item atom(final Item it) {
    return it.node() ? it.type == Type.PI || it.type == Type.COM ?
        Str.get(it.atom()) : new Atm(it.atom()) : it;
  }

  @Override
  public boolean isFun(final FunDef f) {
    return def == f;
  }

  /**
   * Checks the data type and throws an exception, if necessary.
   * @param it item to be checked
   * @param t type to be checked
   * @return specified item
   * @throws QueryException query exception
   */
  public final Item checkType(final Item it, final Type t)
      throws QueryException {

    if(checkEmpty(it).type != t) Err.type(this, t, it);
    return it;
  }

  @Override
  public final String desc() {
    return def.toString();
  }

  @Override
  public final void plan(final Serializer ser) throws IOException {
    ser.openElement(this, NAM, Token.token(def.desc));
    for(final Expr arg : expr) arg.plan(ser);
    ser.closeElement();
  }

  @Override
  public final String toString() {
    return new TokenBuilder().add(def.toString().replaceAll(
        "\\(.*\\)", "") + PAR1).addSep(expr, SEP).add(PAR2).toString();
  }
}
