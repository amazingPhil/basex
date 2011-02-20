package org.basex.build.xml;

import static org.basex.build.BuildText.*;
import static org.basex.util.Token.*;
import java.io.IOException;
import org.basex.build.BuildException;
import org.basex.build.FileParser;
import org.basex.build.BuildText.Type;
import org.basex.core.Prop;
import org.basex.io.IO;

/**
 * This class parses the tokens that are delivered by the {@link XMLScanner} and
 * sends them to the specified database builder.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public class XMLParser extends FileParser {
  /** Scanner reference. */
  private final XMLScanner scanner;

  /**
   * Creates a new XMLParser instance for collection creation.
   * The length of the rootPath is passed in to correctly chop
   * the relative path inside the collection.
   * @param io parser input
   * @param tar target for collection adding
   * @param pr database properties
   * @throws IOException I/O exception
   */
  public XMLParser(final IO io, final String tar, final Prop pr)
      throws IOException {
    super(io, tar);
    scanner = new XMLScanner(io, pr);
  }

  @Override
  public void parse() throws IOException {
    // loop until all tokens have been processed
    scanner.more();
    while(true) {
      if(scanner.type == Type.TEXT) {
        builder.text(scanner.token);
      } else if(scanner.type == Type.COMMENT) {
        builder.comment(scanner.token);
      } else if(scanner.type == Type.PI) {
        builder.pi(scanner.token);
      } else if(scanner.type == Type.EOF) {
        break;
      } else if(scanner.type != Type.DTD) {
        if(!parseTag()) break;
        continue;
      }
      if(!scanner.more()) break;
    }
    scanner.close();
    builder.encoding(scanner.encoding);
  }

  /**
   * Parses an XML tag.
   * @throws IOException I/O exception
   * @return result of scanner step
   */
  private boolean parseTag() throws IOException {
    // find opening tag
    if(scanner.type == Type.L_BR_CLOSE) {
      scanner.more();

      // get tag name
      final byte[] tag = consumeToken(Type.TAGNAME);
      skipSpace();

      builder.endElem(tag);
      return consume(Type.R_BR);
    }

    consume(Type.L_BR);
    atts.reset();

    // get tag name
    final byte[] tag = consumeToken(Type.TAGNAME);
    skipSpace();

    // parse optional attributes
    while(scanner.type != Type.R_BR && scanner.type != Type.CLOSE_R_BR) {
      final byte[] attName = consumeToken(Type.ATTNAME);
      skipSpace();
      consume(Type.EQ);
      skipSpace();
      consume(Type.QUOTE);
      byte[] attValue = EMPTY;
      if(scanner.type == Type.ATTVALUE) {
        attValue = scanner.token.finish();
        scanner.more();
      }
      consume(Type.QUOTE);

      if(startsWith(attName, XMLNSC)) {
        // open namespace...
        builder.startNS(ln(attName), attValue);
      } else if(eq(attName, XMLNS)) {
        // open namespace...
        builder.startNS(EMPTY, attValue);
      } else {
        // add attribute
        atts.add(attName, attValue);
      }

      if(scanner.type != Type.R_BR && scanner.type != Type.CLOSE_R_BR) {
        consume(Type.WS);
      }
    }

    // send start tag to the xml builder
    if(scanner.type == Type.CLOSE_R_BR) {
      builder.emptyElem(tag, atts);
      return scanner.more();
    }
    builder.startElem(tag, atts);
    return consume(Type.R_BR);
  }

  /**
   * Checks if the current token matches the specified type.
   * @param t token type to be checked
   * @return result of scanner step
   * @throws IOException I/O exception
   */
  private boolean consume(final Type t) throws IOException {
    if(scanner.type != t) throw new BuildException(PARSEINVALID, det(),
        t.string, scanner.type.string);
    return scanner.more();
  }

  /**
   * Returns the token for the specified token type. If the current token type
   * is wrong, a {@code null} reference is returned.
   * @param t token type
   * @return token or {@code null} if the token type is wrong
   * @throws IOException I/O exception
   */
  private byte[] consumeToken(final Type t) throws IOException {
    if(scanner.type != t) throw new BuildException(PARSEINVALID, det(),
        t.string, scanner.type.string);
    final byte[] tok = scanner.token.finish();
    scanner.more();
    return tok;
  }

  /**
   * Skips optional whitespaces.
   * @throws IOException I/O exception
   */
  private void skipSpace() throws IOException {
    if(scanner.type == Type.WS) scanner.more();
  }

  @Override
  public String det() {
    return scanner.det();
  }

  @Override
  public double prog() {
    return scanner.prog();
  }
}
