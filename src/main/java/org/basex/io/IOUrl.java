package org.basex.io;

import java.io.IOException;
import java.net.URL;
import org.xml.sax.InputSource;

/**
 * URL reference, wrapped into an IO representation.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
final class IOUrl extends IO {
  /**
   * Constructor.
   * @param u url
   */
  IOUrl(final String u) {
    super(u);
  }

  @Override
  public void cache() throws IOException {
    cache(new URL(path).openStream());
  }

  @Override
  public InputSource inputSource() {
    return new InputSource(path);
  }

  @Override
  public BufferInput buffer() throws IOException {
    return new BufferInput(new URL(path).openStream());
  }

  @Override
  public IO merge(final String f) {
    return this;
  }
}
