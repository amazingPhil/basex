package org.basex.query.expr;

import static org.basex.query.QueryText.*;

import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.item.FPI;
import org.basex.query.item.Item;
import org.basex.query.item.QNm;
import org.basex.query.item.Type;
import org.basex.query.iter.Iter;
import org.basex.query.util.Err;
import org.basex.util.Token;
import org.basex.util.TokenBuilder;
import org.basex.util.XMLToken;

/**
 * PI fragment.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class CPI extends Arr {
  /** Closing processing instruction. */
  private static final byte[] CLOSE = { '?', '>' };

  /**
   * Constructor.
   * @param n name
   * @param v value
   */
  public CPI(final Expr n, final Expr v) {
    super(n, v);
  }


  @Override
  public Expr comp(final QueryContext ctx) throws QueryException {
    super.comp(ctx);
    if(expr[0].e()) Err.empty(this);
    return this;
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    final Item it = atomic(ctx, expr[0], false);
    if(!it.u() && !it.s() && it.type != Type.QNM) Err.or(CPIWRONG, it.type, it);

    final byte[] nm = Token.trim(it.str());
    if(Token.eq(Token.lc(nm), Token.XML)) Err.or(CPIXML, nm);
    if(!XMLToken.isNCName(nm)) Err.or(CPIINVAL, nm);

    final Iter iter = ctx.iter(expr[1]);
    final TokenBuilder tb = new TokenBuilder();
    CText.add(tb, iter);
    byte[] v = tb.finish();
    
    int i = -1;
    while(++i != v.length && v[i] >= 0 && v[i] <= ' ');
    v = Token.substring(v, i);
    if(Token.contains(v, CLOSE)) Err.or(CPICONT, v);

    return new FPI(new QNm(nm), v, null).iter();
  }
  
  @Override
  public Return returned(final QueryContext ctx) {
    return Return.NOD;
  }

  @Override
  public String info() {
    return "PI constructor";
  }

  @Override
  public String toString() {
    return "<?" + expr[0] + ' ' + expr[1] + "?>";
  }
}