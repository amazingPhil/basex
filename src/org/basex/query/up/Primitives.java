package org.basex.query.up;

import static org.basex.query.up.UpdateFunctions.*;


import java.util.LinkedList;
import java.util.List;

import org.basex.data.Data;
import org.basex.data.Nodes;
import org.basex.query.item.DBNode;
import org.basex.util.IntList;

/**
 * Holds all update primitives for a specific data reference. The distinct 
 * primitives are hold seperately to support fast checking of update 
 * constraints.
 * 
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Lukas Kircher
 */
public class Primitives {
  /** Update primitives. */
  List<DeletePrimitive> deletes;
  /** Update primitives. */
  List<RenamePrimitive> renames;
  /** Database reference. */
  Data data;
  
  /**
   * Constructor.
   */
  public Primitives() {
    deletes = new LinkedList<DeletePrimitive>();
    renames = new LinkedList<RenamePrimitive>();
  }

  /**
   * Adds a primitive to a primitive list depending on its type.
   * @param p update primitive
   */
  public void addPrimitive(final UpdatePrimitive p) {
    if(p instanceof DeletePrimitive) deletes.add((DeletePrimitive) p);
    if(p instanceof RenamePrimitive) renames.add((RenamePrimitive) p);
    if(p.node instanceof DBNode && data == null) data = ((DBNode) p.node).data;
  }
  
  /**
   * Applies all updates to the data reference.
   */
  public void apply() {
    if(data != null) {
      for(final RenamePrimitive p : renames) {
        final DBNode n = (DBNode) p.node;
        rename(n.pre, p.newName, n.data);
      }
      
      final IntList pres = new IntList();
      for(final DeletePrimitive p : deletes) pres.add(((DBNode) p.node).pre);
      deleteDBNodes(new Nodes(pres.finish(), data));
    }
  }
}