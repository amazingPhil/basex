package org.basex.query.expr;

import static org.basex.query.QueryText.*;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.item.Item;
import org.basex.query.item.Nod;
import org.basex.query.item.SeqType;
import org.basex.query.item.Type;
import org.basex.query.iter.Iter;
import org.basex.query.iter.NodIter;
import org.basex.query.util.Err;

/**
 * Root node.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class Root extends Simple {
  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    final Iter iter = checkCtx(ctx).iter();
    final NodIter ni = new NodIter(true);
    Item i;
    while((i = iter.next()) != null) {
      final Nod n = root(i);
      if(n == null || n.type != Type.DOC) Err.or(CTXNODE, this);
      ni.add(n);
    }
    return ni;
  }

  /**
   * Returns the root node of the specified item.
   * @param i input node
   * @return root node
   */
  public Nod root(final Item i) {
    if(!i.node()) return null;
    Nod n = (Nod) i;
    while(true) {
      final Nod p = n.parent();
      if(p == null) return n;
      n = p;
    }
  }

  @Override
  public boolean uses(final Use u, final QueryContext ctx) {
    return u == Use.CTX;
  }

  @Override
  public SeqType returned(final QueryContext ctx) {
    return SeqType.NOD_0M;
  }

  @Override
  public boolean duplicates(final QueryContext ctx) {
    return false;
  }

  @Override
  public boolean sameAs(final Expr cmp) {
    return cmp instanceof Root;
  }

  @Override
  public String toString() {
    return "root()";
  }
}