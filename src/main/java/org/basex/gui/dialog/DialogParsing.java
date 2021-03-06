package org.basex.gui.dialog;

import static org.basex.core.Text.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import org.basex.build.ParserProp;
import org.basex.build.file.CSVParser;
import org.basex.build.file.HTMLParser;
import org.basex.build.xml.CatalogResolverWrapper;
import org.basex.core.Prop;
import org.basex.data.DataText;
import org.basex.gui.GUIConstants;
import org.basex.gui.GUIProp;
import org.basex.gui.layout.BaseXBack;
import org.basex.gui.layout.BaseXButton;
import org.basex.gui.layout.BaseXCheckBox;
import org.basex.gui.layout.BaseXCombo;
import org.basex.gui.layout.BaseXFileChooser;
import org.basex.gui.layout.BaseXLabel;
import org.basex.gui.layout.BaseXTextField;
import org.basex.gui.layout.TableLayout;
import org.basex.io.IO;
import org.basex.util.StringList;

/**
 * Parsing options dialog.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public final class DialogParsing extends BaseXBack {
  /** Parser. */
  public final BaseXCombo parser;
  /** Internal XML parsing. */
  private final BaseXCheckBox intparse;
  /** Entities mode. */
  private final BaseXCheckBox entities;
  /** DTD mode. */
  private final BaseXCheckBox dtd;
  /** Whitespace chopping. */
  private final BaseXCheckBox chop;
  /** Use XML Catalog. */
  private final BaseXCheckBox usecat;
  /** Use CSV Header. */
  private final BaseXCheckBox header;
  /** Use TEXT Lines. */
  private final BaseXCheckBox lines;
  /** CSV Separator. */
  private final BaseXCombo separator;
  /** CSV Format. */
  private final BaseXCombo format;
  /** Catalog file. */
  private final BaseXTextField cfile;
  /** Browse Catalog file. */
  private final BaseXButton browsec;
  /** Main window reference. */
  private final Dialog dialog;
  /** Options panel. */
  private BaseXBack parseropts;
  /** XML options panel. */
  private final BaseXBack xmlopts;
  /** CSV options panel. */
  private final BaseXBack csvopts;
  /** Text options panel. */
  private final BaseXBack textopts;
  /** Main panel. */
  private final BaseXBack main;
  /** ParserProps. */
  private ParserProp props;

  /**
   * Default constructor.
   * @param d dialog reference
   */
  public DialogParsing(final Dialog d) {
    dialog = d;
    main = new BaseXBack(new TableLayout(3, 1)).border(4);

    try {
      props = new ParserProp(d.gui.context.prop.get(Prop.PARSEROPT));
    } catch(final IOException ex) {
      props = new ParserProp();
    }

    final StringList parsers = new StringList();
    parsers.add(DataText.M_XML);
    if(HTMLParser.available()) parsers.add(DataText.M_HTML);
    parsers.add(DataText.M_CSV);
    parsers.add(DataText.M_TEXT);

    parser = new BaseXCombo(d, parsers.toArray());
    parser.setSelectedItem(dialog.gui.context.prop.get(Prop.PARSER));

    intparse = new BaseXCheckBox(CREATEINTPARSE,
        dialog.gui.context.prop.is(Prop.INTPARSE), 0, dialog);
    entities = new BaseXCheckBox(CREATEENTITIES,
        dialog.gui.context.prop.is(Prop.ENTITY), dialog);
    dtd = new BaseXCheckBox(CREATEDTD,
        dialog.gui.context.prop.is(Prop.DTD), 12, dialog);
    chop = new BaseXCheckBox(CREATECHOP,
        dialog.gui.context.prop.is(Prop.CHOP), 0, dialog);
    usecat = new BaseXCheckBox(USECATFILE,
        !dialog.gui.context.prop.get(Prop.CATFILE).isEmpty(), 0, dialog);
    cfile = new BaseXTextField(
        dialog.gui.context.prop.get(Prop.CATFILE), dialog);
    browsec = new BaseXButton(BUTTONBROWSE, dialog);

    lines = new BaseXCheckBox("Lines", props.is(ParserProp.LINES), 0, dialog);
    header = new BaseXCheckBox("Header", props.is(ParserProp.HEADER),
        0, dialog);
    separator = new BaseXCombo(d, CSVParser.SEPARATORS);
    separator.setSelectedItem(props.get(ParserProp.SEPARATOR));
    format = new BaseXCombo(d, CSVParser.FORMATS);
    format.setSelectedItem(props.get(ParserProp.FORMAT));

    xmlopts = new BaseXBack(new TableLayout(9, 1));
    csvopts = new BaseXBack(new TableLayout(6, 1));
    textopts = new BaseXBack(new TableLayout(3, 1));
    createOptionsPanels();

    setLayout(new TableLayout(1, 1));
    options(parser.getSelectedItem().toString());
    add(main);
  }

  /**
   * Options panels.
   */
  void createOptionsPanels() {
    xmlopts.add(intparse);
    xmlopts.add(new BaseXLabel(INTPARSEINFO, true, false));
    xmlopts.add(entities);
    xmlopts.add(dtd);
    xmlopts.add(chop);
    xmlopts.add(new BaseXLabel(CHOPPINGINFO, false, false).border(0, 0, 8, 0));
    xmlopts.add(new BaseXLabel());

    // CatalogResolving
    final boolean rsen = CatalogResolverWrapper.available();
    final BaseXBack fl = new BaseXBack(new TableLayout(2, 2, 6, 0));
    usecat.setEnabled(rsen);
    fl.add(usecat);
    fl.add(new BaseXLabel());
    cfile.setEnabled(rsen);
    fl.add(cfile);
    browsec.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) { catchoose(); }
    });
    browsec.setEnabled(rsen);
    fl.add(browsec);
    xmlopts.add(fl);
    if(!rsen) {
      final BaseXBack rs = new BaseXBack(new TableLayout(2, 1));
      rs.add(new BaseXLabel(USECATHLP).color(GUIConstants.COLORDARK));
      rs.add(new BaseXLabel(USECATHLP2).color(GUIConstants.COLORDARK));
      xmlopts.add(rs);
    }

    BaseXBack p = new BaseXBack(new TableLayout(2, 1, 6, 0));
    p.add(header);
    p.add(new BaseXLabel(HEADERINFO, true, false));
    csvopts.add(p);
    p = new BaseXBack(new TableLayout(1, 2, 6, 0));
    p.add(new BaseXLabel(SEPARATORINFO, true, false));
    p.add(separator);
    csvopts.add(p);
    p = new BaseXBack(new TableLayout(1, 2, 6, 0)).border(4, 0, 0, 0);
    p.add(new BaseXLabel(FORMINFO, true, false));
    p.add(format);
    csvopts.add(p);

    textopts.add(lines);
    textopts.add(new BaseXLabel(LINESINFO, true, false));
  }

  /**
   * Refreshes the options panel.
   * @param type format type
   */
  void options(final String type) {
    main.removeAll();

    final BaseXBack p = new BaseXBack(new TableLayout(1, 2, 6, 0));
    p.add(new BaseXLabel(CREATEFORMAT, true, true));
    p.add(parser);
    main.add(p);
    main.add(new BaseXLabel(FORMATINFO, true, false));

    if(type.equals(DataText.M_XML)) {
      parseropts = xmlopts;
    } else if(type.equals(DataText.M_HTML)) {
      parseropts = new BaseXBack();
    } else if(type.equals(DataText.M_CSV)) {
      parseropts = csvopts;
    } else if(type.equals(DataText.M_TEXT)) {
      parseropts = textopts;
    }

    main.add(parseropts);
    main.revalidate();
    parser.requestFocusInWindow();
  }

  /**
   * Opens a file dialog to choose an XML catalog or directory.
   */
  void catchoose() {
    final GUIProp gprop = dialog.gui.gprop;
    final BaseXFileChooser fc = new BaseXFileChooser(CREATETITLE,
        gprop.get(GUIProp.CREATEPATH), dialog.gui);
    fc.addFilter(CREATEXMLDESC, IO.XMLSUFFIX);

    final IO file = fc.select(BaseXFileChooser.Mode.FDOPEN);
    if(file != null) cfile.setText(file.path());
  }

  /**
   * Reacts on user input.
   * @param cmp component
   */
  void action(final Object cmp) {
    final String type = parser.getSelectedItem().toString();
    if(type.equals(DataText.M_XML)) {
      final boolean ip = intparse.isSelected();
      final boolean uc = usecat.isSelected();
      intparse.setEnabled(!uc);
      entities.setEnabled(ip);
      dtd.setEnabled(ip);
      usecat.setEnabled(!ip && CatalogResolverWrapper.available());
      cfile.setEnabled(uc);
      browsec.setEnabled(uc);
    }
    if(cmp == parser) options(type);
  }

  /**
   * Closes the tab.
   */
  public void close() {
    dialog.gui.set(Prop.CHOP, chop.isSelected());
    dialog.gui.set(Prop.ENTITY, entities.isSelected());
    dialog.gui.set(Prop.DTD, dtd.isSelected());
    dialog.gui.set(Prop.INTPARSE, intparse.isSelected());
    dialog.gui.set(Prop.PARSER, parser.getSelectedItem().toString());
    dialog.gui.set(Prop.CATFILE, usecat.isSelected() ? cfile.getText() : "");
    props.set(ParserProp.FORMAT, format.getSelectedItem().toString());
    props.set(ParserProp.HEADER, header.isSelected());
    props.set(ParserProp.SEPARATOR, separator.getSelectedItem().toString());
    props.set(ParserProp.LINES, lines.isSelected());
    dialog.gui.set(Prop.PARSEROPT, props.toString());
    }
}
