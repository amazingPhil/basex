package org.deepfs.fsml.parsers;

import java.io.IOException;
import org.basex.util.Util;
import org.deepfs.fsml.DeepFile;
import org.deepfs.fsml.FileType;
import org.deepfs.fsml.MimeType;
import org.deepfs.fsml.ParserRegistry;

/**
 * Parser for TIF files.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 * @author Bastian Lemke
 */
public final class TIFFParser implements IFileParser {

  static {
    ParserRegistry.register("tiff", TIFFParser.class);
    ParserRegistry.register("tif", TIFFParser.class);
  }

  /** Parser for Exif data. */
  private final ExifParser exifParser = new ExifParser();

  @Override
  public boolean check(final DeepFile df) throws IOException {
    return exifParser.check(df);
  }

  @Override
  public void extract(final DeepFile deepFile) throws IOException {
    if(deepFile.extractMeta()) {
      deepFile.setFileType(FileType.PICTURE);
      deepFile.setFileFormat(MimeType.TIFF);
      exifParser.extract(deepFile);
    }
  }

  @Override
  public void propagate(final DeepFile deepFile) {
    Util.notimplemented();
  }
}
