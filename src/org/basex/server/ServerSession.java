package org.basex.server;

import static org.basex.core.Text.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import org.basex.BaseXServer;
import org.basex.core.CommandParser;
import org.basex.core.Context;
import org.basex.core.Main;
import org.basex.core.Process;
import org.basex.core.Prop;
import org.basex.core.proc.Close;
import org.basex.core.proc.Exit;
import org.basex.core.proc.IntError;
import org.basex.core.proc.IntInfo;
import org.basex.core.proc.IntStop;
import org.basex.data.Data;
import org.basex.io.BufferedOutput;
import org.basex.io.IO;
import org.basex.io.PrintOutput;
import org.basex.query.QueryException;
import org.basex.util.Performance;

/**
 * Single session for a client-server connection.
 * 
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Andreas Weiler
 */
public final class ServerSession extends Thread {
  /** Database context. */
  private final Context context;
  /** Socket reference. */
  private final Socket socket;
  /** Server reference. */
  private final BaseXServer server;
  /** Info flag. */
  private final boolean info;
  /** Core. */
  private Process core;
  /** Timeout thread. */
  private Thread timeout;
  /** ClientProcess. */
  private ClientProcess cp;
  /** Thread blocker. */
  private String block = "";

  /**
   * Constructor.
   * @param s socket
   * @param b server reference
   * @param i info flag
   */
  public ServerSession(final Socket s, final BaseXServer b, final boolean i) {
    context = new Context(b.context);
    socket = s;
    server = b;
    info = i;
    start();
  }

  @Override
  public void run() {
    try {
      // get command and arguments
      final DataInputStream dis = new DataInputStream(socket.getInputStream());
      final PrintOutput out = new PrintOutput(new BufferedOutput(
          socket.getOutputStream()));

      // handle unknown users and wrong passwords
      final String us = dis.readUTF();
      final String pw = dis.readUTF();
      context.user = context.users.get(us, pw);
      final boolean ok = context.user != null;
      send(out, ok);

      if(ok) {
        if(info) Main.outln(this + (ok ? " Login: " : " Failed: ") + us);
        process(dis, out);
        if(info) Main.outln(this + " Logout: " + us);
      } else {
        if(info) Main.outln(this + " Failed: " + us);
      }
    } catch(final IOException ex) {
      Main.error(ex, false);
    }
  }

  /**
   * Processes all incoming client commands.
   * @param dis input stream
   * @param out output stream
   * @throws IOException I/O exception
   */
  void process(final DataInputStream dis, final PrintOutput out)
      throws IOException {

    while(true) {
      String in = null;
      try {
        in = dis.readUTF();
      } catch(final IOException ex) {
        // this exception is thrown for each session if the server is stopped
        exit();
        return;
      }

      // parse input and create process instance
      final Performance perf = new Performance();
      Process proc = null;
      try {
        proc = new CommandParser(in, context, true).parse()[0];

        if(proc instanceof IntInfo) {
          String inf = core.info();
          if(inf.equals(PROGERR)) inf = SERVERTIME;
          new DataOutputStream(out).writeUTF(inf);
          out.flush();
        } else {
          core = proc;
          boolean updating = proc.updating();
          cp = new ClientProcess(this, updating);
          server.cp.add(cp);
          startTimer(proc);

          // wait for updating processes
          if(server.cp.size() > 1) {
            for(int i = 0; !updating && i < server.cp.size(); i++) {
              updating = server.cp.get(i).updating;
            }
            if(updating) {
              while(server.cp.indexOf(cp) != 0) {
                synchronized(block) {
                  try {
                    block.wait();
                  } catch(InterruptedException e) {
                    e.printStackTrace();
                  }
                }
              }
            }
          }

          final boolean ok = proc.execute(context, out);
          server.cp.remove(cp);
          if(updating) {
            synchronized(block) {
            block.notifyAll();
            }
          }
          stopTimer();
          out.write(new byte[IO.BLOCKSIZE]);
          send(out, ok);

          if(proc instanceof IntStop || proc instanceof Exit) {
            server.cp.remove(cp);
            exit();
            if(proc instanceof IntStop) server.quit(false);
            return;
          }
        }
      } catch(final QueryException ex) {
        // invalid command was sent by a client; create error feedback
        proc = new IntError(ex.extended());
        core = proc;
        out.write(new byte[IO.BLOCKSIZE]);
        send(out, false);
      }

      if(info) Main.outln(this + " " + in + ": " + perf.getTimer());
    }
  }

  /*
          // wait for updating processes
          boolean updating = false;
          for(int i = 0; !updating && i < server.cp.size(); i++) {
            updating = server.cp.get(i).updating;
          }
          if(updating) {
            while(server.cp.indexOf(cp) != 0) {
              System.out.println("!");
              Performance.sleep(50);
            }
          }

   */
  
  /**
   * Sends the success flag to the client.
   * @param out output stream
   * @param ok success flag
   * @throws IOException I/O exception
   */
  private void send(final PrintOutput out, final boolean ok)
      throws IOException {
    out.write(ok ? 0 : 1);
    out.flush();
  }

  /**
   * Starts a timeout thread for the specified process.
   * @param proc process reference
   */
  private void startTimer(final Process proc) {
    final long to = context.prop.num(Prop.TIMEOUT);
    if(to == 0) return;

    timeout = new Thread() {
      @Override
      public void run() {
        Performance.sleep(to * 1000);
        proc.stop();
      }
    };
    timeout.start();
  }

  /**
   * Stops the current timeout thread.
   */
  private void stopTimer() {
    if(timeout != null) timeout.interrupt();
  }

  /**
   * Exits the session.
   */
  public void exit() {
    if(core != null) core.stop();
    stopTimer();

    new Close().execute(context);
    context.delete(this);

    try {
      socket.close();
    } catch(final IOException ex) {
      Main.error(ex, false);
    }
  }

  /**
   * Returns session information.
   * @return database information
   */
  public String info() {
    final Data data = context.data();
    return this + (data != null ? ": " + data.meta.name : "");
  }

  @Override
  public String toString() {
    final String host = socket.getInetAddress().getHostAddress();
    final int port = socket.getPort();
    return Main.info("[%:%]", host, port);
  }
}
