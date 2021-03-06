package org.deepfs.fsml.parsers;

import static org.basex.util.Token.*;
import java.io.EOFException;
import java.io.IOException;
import org.basex.util.Util;
import org.deepfs.fsml.BufferedFileChannel;
import org.deepfs.fsml.DeepFile;
import org.deepfs.fsml.FileType;
import org.deepfs.fsml.MetaElem;
import org.deepfs.fsml.MimeType;
import org.deepfs.fsml.ParserRegistry;

/**
 * Parser for BMP files.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 * @author Bastian Lemke
 */
public final class BMPParser implements IFileParser {

  /** BMP header info. */
  private static final byte[] HEADERBMP = token("BM");

  static {
    ParserRegistry.register("bmp", BMPParser.class);
  }

  @Override
  public boolean check(final DeepFile deepFile) throws IOException {
    final BufferedFileChannel bfc = deepFile.getBufferedFileChannel();
    if(bfc.size() < 2) return false;
    final byte[] header = bfc.get(new byte[2]);
    return eq(header, HEADERBMP);
  }

  @Override
  public void extract(final DeepFile deepFile) throws IOException {
    if(!deepFile.extractMeta()) return; // no content to extract
    if(!check(deepFile)) return;

    final BufferedFileChannel f = deepFile.getBufferedFileChannel();
    f.skip(16);
    try {
      f.buffer(8);
    } catch(final EOFException ex) {
      return;
    }

    deepFile.setFileType(FileType.PICTURE);
    deepFile.setFileFormat(MimeType.BMP);

    // extract image dimensions
    final int w = f.get() + (f.get() << 8) + (f.get() << 16) + (f.get() << 24);
    final int h = f.get() + (f.get() << 8) + (f.get() << 16) + (f.get() << 24);
    deepFile.addMeta(MetaElem.PIXEL_WIDTH, w);
    deepFile.addMeta(MetaElem.PIXEL_HEIGHT, h);
  }

  @Override
  public void propagate(final DeepFile deepFile) {
    Util.notimplemented();
  }
}
