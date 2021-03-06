package org.basex.core.cmd;

import static org.basex.core.Text.*;
import java.io.IOException;
import org.basex.core.Prop;
import org.basex.core.User;
import org.basex.data.Data;
import org.basex.index.IndexToken.IndexType;
import org.basex.io.IO;
import org.basex.util.Util;

/**
 * Evaluates the 'optimize' command and optimizes the data structures of
 * the currently opened database. Indexes and statistics are refreshed,
 * which is especially helpful after updates.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public final class Optimize extends ACreate {
  /** Current pre value. */
  private int pre;
  /** Data size. */
  private int size;

  /**
   * Default constructor.
   */
  public Optimize() {
    super(DATAREF | User.WRITE);
  }

  @Override
  protected boolean run() {
    final Data data = context.data;

    // refresh indexes
    data.pthindex.init();
    data.tags.init();
    data.atts.init();
    data.meta.dirty = true;

    final int[] parStack = new int[IO.MAXHEIGHT];
    final int[] tagStack = new int[IO.MAXHEIGHT];
    final boolean path = prop.is(Prop.PATHINDEX);
    int level = 0;
    int h = 0, d = 0;

    size = data.meta.size;
    for(pre = 0; pre < size; ++pre) {
      final byte kind = (byte) data.kind(pre);
      final int par = data.parent(pre, kind);
      while(level > 0 && parStack[level - 1] > par) --level;

      if(kind == Data.DOC) {
        parStack[level++] = pre;
        if(path) data.pthindex.add(0, level, kind);
        ++d;
      } else if(kind == Data.ELEM) {
        final int id = data.name(pre);
        data.tags.index(data.tags.key(id), null, true);
        if(path) data.pthindex.add(id, level, kind);
        tagStack[level] = id;
        parStack[level++] = pre;
      } else if(kind == Data.ATTR) {
        final int id = data.name(pre);
        data.atts.index(data.atts.key(id), data.text(pre, false), true);
        if(path) data.pthindex.add(id, level, kind);
      } else {
        final byte[] txt = data.text(pre, true);
        if(kind == Data.TEXT) data.tags.index(tagStack[level - 1], txt);
        if(path) data.pthindex.add(0, level, kind);
      }
      if(h < level) h = level;
    }
    data.meta.height = h;
    data.meta.ndocs = d;
    data.meta.pathindex = path;
    data.meta.uptodate = true;

    try {
      // global property check can be skipped as soon as id/pre mapping exists
      if(data.meta.textindex || prop.is(Prop.TEXTINDEX))
        index(IndexType.TEXT, data);
      if(data.meta.attrindex || prop.is(Prop.ATTRINDEX))
        index(IndexType.ATTRIBUTE, data);
      if(data.meta.ftindex || prop.is(Prop.FTINDEX))
        index(IndexType.FULLTEXT, data);
    } catch(final IOException ex) {
      Util.debug(ex);
    }
    data.flush();

    return info(DBOPTIMIZED, perf);
  }

  @Override
  public double prog() {
    return (double) pre / size;
  }

  @Override
  public boolean stoppable() {
    return false;
  }

  @Override
  public String det() {
    return INFOSTATS;
  }
}
