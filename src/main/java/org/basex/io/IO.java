package org.basex.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.basex.data.Data;
import org.basex.util.ByteList;
import org.basex.util.Token;
import org.basex.util.Util;
import org.xml.sax.InputSource;

/**
 * Generic representation for inputs and outputs. The underlying source can
 * be a local file ({@link IOFile}), a URL ({@link IOUrl}) or a byte array
 * ({@link IOContent}).
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public abstract class IO {
  /** Database file suffix. */
  public static final String BASEXSUFFIX = ".basex";
  /** XQuery file suffix. */
  public static final String XQSUFFIX = ".xq";
  /** XQuery file suffix. */
  public static final String XQUERYSUFFIX = ".xquery";
  /** XQuery file suffix. */
  public static final String XQMSUFFIX = ".xqm";
  /** XQuery file suffix. */
  public static final String XQYSUFFIX = ".xqy";
  /** XQuery file suffix. */
  public static final String XQLSUFFIX = ".xql";
  /** XQuery suffixes. */
  public static final String[] XQSUFFIXES = {
    XQSUFFIX, XQMSUFFIX, XQYSUFFIX, XQLSUFFIX, XQUERYSUFFIX
  };
  /** XML file suffix. */
  public static final String XMLSUFFIX = ".xml";
  /** ZIP file suffix. */
  public static final String ZIPSUFFIX = ".zip";
  /** GZIP file suffix. */
  public static final String GZSUFFIX = ".gz";

  /** Disk block/page size. */
  public static final int BLOCKSIZE = 1 << 12;
  /** Table node size power. */
  public static final int NODEPOWER = 4;
  /** Maximum supported tree height. */
  public static final int MAXHEIGHT = 1 << 8;
  /** Maximum number of attributes (see bit layout in {@link Data} class). */
  public static final int MAXATTS = 0x1F;
  /** Offset for inlining numbers (see bit layout in {@link Data} class). */
  public static final long NUMOFF = 0x8000000000L;
  /** Offset for compressing texts (see bit layout in {@link Data} class). */
  public static final long CPROFF = 0x4000000000L;

  /** File path and name. */
  protected String path;
  /** File contents. */
  protected byte[] cont;
  /** First call. */
  protected boolean more;
  /** File name. */
  protected String name;

  /**
   * Protected constructor.
   * @param p path
   */
  protected IO(final String p) {
    init(p);
  }

  /**
   * Sets the file path and name.
   * @param p file path
   */
  protected final void init(final String p) {
    path = p;
    // use timer if no name is given
    final String n = path.substring(path.lastIndexOf('/') + 1);
    name = n.isEmpty() ? Long.toString(System.currentTimeMillis()) +
        XMLSUFFIX : n;
  }

  /**
   * Constructor.
   * @param s source
   * @return IO reference
   */
  public static IO get(final String s) {
    if(s == null) return new IOFile("");
    if(s.startsWith("<")) return new IOContent(Token.token(s));
    if(!s.contains("://") || s.startsWith("file:")) return new IOFile(s);
    return new IOUrl(s);
  }

  /**
   * Returns the contents.
   * @return contents
   * @throws IOException I/O exception
   */
  public final byte[] content() throws IOException {
    if(cont == null) cache();
    return cont;
  }

  /**
   * Caches the contents.
   * @throws IOException I/O exception
   */
  public abstract void cache() throws IOException;

  /**
   * Tests if the file exists.
   * @return result of check
   */
  public boolean exists() {
    return true;
  }

  /**
   * Tests if this is a directory instance.
   * @return result of check
   */
  public boolean isDir() {
    return false;
  }

  /**
   * Returns the modification date of this file.
   * @return modification date
   */
  public long date() {
    return System.currentTimeMillis();
  }

  /**
   * Returns the file length.
   * @return file length
   */
  public long length() {
    return cont != null ? cont.length : 0;
  }

  /**
   * Checks if more input streams are found.
   * @return result of check
   * @throws IOException I/O exception
   */
  @SuppressWarnings("unused")
  public boolean more() throws IOException {
    return more ^= true;
  }

  /**
   * Returns the next input source.
   * @return input source
   */
  public abstract InputSource inputSource();

  /**
   * Returns a buffered reader for the input.
   * @return buffered reader
   * @throws IOException I/O exception
   */
  public abstract BufferInput buffer() throws IOException;

  /**
   * Merges two filenames.
   * @param fn file name/path to be merged
   * @return contents
   */
  public abstract IO merge(final String fn);

  /**
   * Creates the directory.
   * @return contents
   */
  public boolean md() {
    return false;
  }

  /**
   * Chops the path and the XML suffix of the specified filename
   * and returns the database name.
   * @return database name
   */
  public final String dbname() {
    final String n = name();
    final int i = n.lastIndexOf(".");
    return (i != -1 ? n.substring(0, i) : n).replaceAll("[^\\w-]", "");
  }

  /**
   * Returns the name of the resource.
   * @return file name
   */
  public final String name() {
    return name;
  }

  /**
   * Sets the name of the resource.
   * @param n file name
   */
  public final void name(final String n) {
    name = n;
  }

  /**
   * Returns the path.
   * @return path
   */
  public final String path() {
    return path;
  }

  /**
   * Creates a URL from the specified path.
   * @return URL
   */
  public String url() {
    return path;
  }

  /**
   * Returns the directory.
   * @return chopped filename
   */
  public String dir() {
    return isDir() ? path() : path.substring(0, path.lastIndexOf('/') + 1);
  }

  /**
   * Returns the children of a path.
   * @return children
   */
  public IO[] children() {
    return new IO[] {};
  }

  /**
   * Writes the specified file contents.
   * @param c contents
   * @throws IOException I/O exception
   */
  @SuppressWarnings("unused")
  public void write(final byte[] c) throws IOException {
    Util.notexpected();
  }

  /**
   * Deletes the IO reference.
   * @return success flag
   */
  public boolean delete() {
    return false;
  }

  /**
   * Renames the specified IO reference.
   * @param trg target reference
   * @return success flag
   */
  @SuppressWarnings("unused")
  public boolean rename(final IO trg) {
    return false;
  }

  /**
   * Compares the filename of the specified IO reference.
   * @param io io reference
   * @return result of check
   */
  public final boolean eq(final IO io) {
    return path.equals(io.path);
  }

  @Override
  public String toString() {
    return path;
  }

  /**
   * Caches the contents of the specified input stream.
   * @param i input stream
   * @return cached contents
   * @throws IOException I/O exception
   */
  protected final byte[] cache(final InputStream i) throws IOException {
    final ByteList bl = new ByteList();
    final InputStream bis = i instanceof BufferedInputStream ||
      i instanceof BufferInput ? i : new BufferedInputStream(i);
    int b;
    while((b = bis.read()) != -1) bl.add(b);
    bis.close();
    cont = bl.toArray();
    return cont;
  }
}
