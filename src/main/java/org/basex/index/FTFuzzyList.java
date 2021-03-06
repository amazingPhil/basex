package org.basex.index;

import static org.basex.data.DataText.*;
import static org.basex.util.Token.*;
import java.io.File;
import java.io.IOException;
import org.basex.data.Data;
import org.basex.io.DataAccess;

/**
 * This class provides access to a sorted token list.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Sebastian Gath
 */
final class FTFuzzyList extends FTList {
  /** Size file. */
  private final File sizes;
  /** Token positions. */
  private final int[] tp = new int[MAXLEN + 3];
  /** Pointer on current token length. */
  private int ctl;
  /** Pointer on next token length. */
  private int ntl;
  /** Number of written bytes for tokens. */
  private int ptok;

  /**
   * Constructor, initializing the index structure.
   * @param d data reference
   * @param cf current file
   * @throws IOException IO Exception
   */
  protected FTFuzzyList(final Data d, final int cf) throws IOException {
    super(d, cf, 'y', 'z');
    for(int i = 0; i < tp.length; ++i) tp[i] = -1;
    sizes = d.meta.file(DATAFTX + cf + 'x');
    final DataAccess li = new DataAccess(sizes);
    int is = li.read1();
    while(--is >= 0) {
      final int p = li.read1();
      tp[p] = li.read4();
    }
    tp[tp.length - 1] = (int) str.length();
    li.close();
    next();
  }

  @Override
  void close() throws IOException {
    super.close();
    sizes.delete();
  }

  @Override
  protected byte[] token() {
    if(tp[tp.length - 1] == ptok) return EMPTY;
    if(tp[ntl] == ptok || ntl == 0) {
      ++ctl;
      while(tp[ctl] == -1) ++ctl;
      ntl = ctl + 1;
      while(tp[ntl] == -1) ++ntl;
    }
    if(ctl == tp.length) return EMPTY;

    final byte[] t = str.readBytes(ptok, ctl);
    // skip pointer
    size = str.read4(str.pos() + 5);
    // position will always fit in an integer...
    ptok = (int) str.pos();
    return t;
  }
}
