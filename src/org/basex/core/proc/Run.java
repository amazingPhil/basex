package org.basex.core.proc;

import static org.basex.Text.*;
import java.io.IOException;
import org.basex.BaseX;
import org.basex.core.Prop;
import org.basex.io.IO;
import org.basex.io.PrintOutput;
import org.basex.util.Token;

/**
 * Evaluates the 'run' command.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
public final class Run extends AQuery {
  /**
   * Constructor.
   * @param file query file
   */
  public Run(final String file) {
    super(PRINTING, file);
  }

  @Override
  protected boolean exec() {
    final IO io = IO.get(args[0]);
    if(!io.exists()) return error(FILEWHICH, io);
    try {
      return query(Token.string(io.content()));
    } catch(final IOException ex) {
      BaseX.debug(ex);
      final String msg = ex.getMessage();
      return error(msg != null ? msg : args[0]);
    }
  }

  @Override
  protected void out(final PrintOutput o) throws IOException {
    out(o, Prop.xqformat);
  }
}