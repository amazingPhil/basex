package org.basex.gui.dialog;

import static org.basex.core.Text.*;
import static org.basex.data.DataText.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.SortedMap;
import org.basex.core.Prop;
import org.basex.data.SerializerProp;
import org.basex.gui.GUI;
import org.basex.gui.GUIConstants.Msg;
import org.basex.gui.layout.BaseXBack;
import org.basex.gui.layout.BaseXButton;
import org.basex.gui.layout.BaseXCheckBox;
import org.basex.gui.layout.BaseXCombo;
import org.basex.gui.layout.BaseXFileChooser;
import org.basex.gui.layout.BaseXLabel;
import org.basex.gui.layout.BaseXLayout;
import org.basex.gui.layout.BaseXTextField;
import org.basex.gui.layout.TableLayout;
import org.basex.io.IO;
import org.basex.io.IOFile;

/**
 * Dialog window for changing some project's preferences.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public final class DialogExport extends Dialog {
  /** Available encodings. */
  private static final String[] ENCODINGS;
  /** Directory path. */
  private final BaseXTextField path;
  /** Database info. */
  private final BaseXLabel info;
  /** Output label. */
  private final BaseXLabel out;
  /** XML formatting. */
  private final BaseXCheckBox format;
  /** Encoding. */
  private final BaseXCombo encoding;
  /** Buttons. */
  private final BaseXBack buttons;

  // initialize encodings
  static {
    final SortedMap<String, Charset> cs = Charset.availableCharsets();
    ENCODINGS = cs.keySet().toArray(new String[cs.size()]);
  }

  /**
   * Default constructor.
   * @param main reference to the main window
   */
  public DialogExport(final GUI main) {
    super(main, GUIEXPORT);

    // create checkboxes
    final BaseXBack pp = new BaseXBack(new TableLayout(3, 1, 0, 4));

    BaseXBack p = new BaseXBack(new TableLayout(2, 2, 6, 0));
    out = new BaseXLabel(OUTDIR + COL, true, true).border(0, 0, 4, 0);
    p.add(out);
    p.add(new BaseXLabel());

    final IO io = gui.context.data.meta.path;
    final String fn = io.dir();
    path = new BaseXTextField(fn, this);
    path.addKeyListener(keys);
    p.add(path);

    final BaseXButton browse = new BaseXButton(BUTTONBROWSE, this);
    browse.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) { choose(); }
    });
    p.add(browse);
    pp.add(p);

    p = new BaseXBack(new TableLayout(2, 1));
    p.add(new BaseXLabel(INFOENCODING + COL, true, true).border(0, 0, 4, 0));

    final Prop prop = gui.context.prop;
    SerializerProp sp = null;
    try {
      sp = new SerializerProp(prop.get(Prop.EXPORTER));
    } catch(final IOException ex) {
      // ignore invalid serialization parameters
      sp = new SerializerProp();
    }

    encoding = new BaseXCombo(this, ENCODINGS);
    String enc = gui.context.data.meta.encoding;
    boolean f = false;
    for(final String s : ENCODINGS) f |= s.equals(enc);
    if(!f) {
      enc = enc.toUpperCase();
      for(final String s : ENCODINGS) f |= s.equals(enc);
    }
    encoding.setSelectedItem(f ? enc : sp.get(SerializerProp.S_ENCODING));
    encoding.addKeyListener(keys);
    BaseXLayout.setWidth(encoding, BaseXTextField.DWIDTH);
    p.add(encoding);
    pp.add(p);

    format = new BaseXCheckBox(OUTINDENT,
        sp.get(SerializerProp.S_INDENT).equals(YES), 0, this);
    pp.add(format);
    set(pp, BorderLayout.CENTER);

    // create buttons
    p = new BaseXBack(new BorderLayout());
    info = new BaseXLabel(" ").border(18, 0, 0, 0);
    p.add(info, BorderLayout.WEST);
    buttons = okCancel(this);
    p.add(buttons, BorderLayout.EAST);
    set(p, BorderLayout.SOUTH);

    action(null);
    finish(null);
  }

  /**
   * Opens a file dialog to choose an XML document or directory.
   */
  void choose() {
    final IO io = new BaseXFileChooser(DIALOGFC, path.getText(), gui).
      select(BaseXFileChooser.Mode.DOPEN);
    if(io != null) path.setText(io.path());
  }

  /**
   * Returns the chosen XML file or directory path.
   * @return file or directory
   */
  public String path() {
    return path.getText().trim();
  }

  @Override
  public void action(final Object cmp) {
    final IO io = IO.get(path());
    final boolean file = io instanceof IOFile;
    ok = !path().isEmpty() && file;
    info.setText(!ok && !file ? INVPATH : io.children().length > 0 ? OVERFILE
        : null, ok ? Msg.WARN : Msg.ERROR);
    enableOK(buttons, BUTTONOK, ok);
  }

  @Override
  public void close() {
    if(!ok) return;
    super.close();
    final boolean indent = format.isSelected();
    gui.set(Prop.EXPORTER,
        SerializerProp.S_INDENT[0] + "=" + (indent ? YES : NO) + "," +
        SerializerProp.S_ENCODING[0] + "=" + encoding.getSelectedItem() + "," +
        SerializerProp.S_OMIT_XML_DECLARATION[0] + "=" + NO);
  }
}
