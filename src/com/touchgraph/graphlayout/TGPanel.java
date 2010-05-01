/*
 * TouchGraph LLC. Apache-Style Software License
 *
 *
 * Copyright (c) 2001-2002 Alexander Shapiro. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        TouchGraph LLC (http://www.touchgraph.com/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "TouchGraph" or "TouchGraph LLC" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission.  For written permission, please contact
 *    alex@touchgraph.com
 *
 * 5. Products derived from this software may not be called "TouchGraph",
 *    nor may "TouchGraph" appear in their name, without prior written
 *    permission of alex@touchgraph.com.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL TOUCHGRAPH OR ITS CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 */

package com.touchgraph.graphlayout;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.*;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;

import javax.jws.Oneway;

import org.omg.CORBA.ExceptionList;
import org.opencyc.api.*;
import org.opencyc.cyclobject.*;
import org.opencyc.cycobject.*;

//import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;

import com.touchgraph.graphlayout.graphelements.*;
import com.touchgraph.graphlayout.interaction.*;

/**
 * TGPanel contains code for drawing the graph, and storing which nodes are
 * selected, and which ones the mouse is over.
 * 
 * It houses methods to activate TGLayout, which performs dynamic layout.
 * Whenever the graph is moved, or repainted, TGPanel fires listner methods on
 * associated objects.
 * 
 * <p>
 * <b> Parts of this code build upon Sun's Graph Layout example.
 * http://java.sun.com/applets/jdk/1.1/demo/GraphLayout/Graph.java </b>
 * </p>
 * 
 * @author Alexander Shapiro
 * @author Murray Altheim (2001-11-06; 2002-01-14 cleanup)
 * @version 1.22-jre1.1 $Id: TGPanel.java,v 1.2 2002/09/20 14:26:17 ldornbusch
 *          Exp $
 */
public class TGPanel extends Panel {

	// static variables for use within the package

	private static final CycVariable WHAT = new CycVariable("WHAT");

	public static Color BACK_COLOR = Color.white;

	// ....

	private GraphEltSet completeEltSet;

	public VisibleLocality visibleLocality;

	private LocalityUtils localityUtils;

	public TGLayout tgLayout;

	protected BasicMouseMotionListener basicMML;

	public Choice tgHistory;

	public Choice tgVisList;

	public java.awt.List edgeFilterList;

	protected Edge mouseOverE; // mouseOverE is the edge the mouse is over

	protected Node mouseOverN; // mouseOverN is the node the mouse is over

	protected boolean maintainMouseOver = false; // If true, then don't
	// change mouseOverN or
	// mouseOverE

	protected Node select;

	GLPanel glp;

	Node dragNode; // Node currently being dragged

	protected Point mousePos; // Mouse location, updated in the
	// mouseMotionListener

	Image offscreen;

	CycSymbol NIL = new CycSymbol("NIL");
	Dimension offscreensize;

	Graphics offgraphics;

	private Vector graphListeners;

	private Vector paintListeners;

	TGLensSet tgLensSet; // Converts between a nodes visual position (drawx,
	// drawy),

	// and its absolute position (x,y).
	AdjustOriginLens adjustOriginLens;

	SwitchSelectUI switchSelectUI;

	public static CycAccess cycAccess = null;

	public boolean showEdgeLables = true;

	public boolean censorTachyons = false;

	public boolean showShadow = true;

	public Vector<Thread> workerThreads = new Vector<Thread>();
	public TGThread onlyThread = null;
	public Node currentNode = null;

	/** Required to get an image from a URL. */
	// protected Toolkit tk;
	/** URL of the background image of the linkbrowser. */
	// protected URL imageURL;
	/** Background image of the linkbrowser. */
	// protected Image BG_IMAGE;
	/** Used to turn background image of the linkbrowser on and off. */
	// protected boolean bgImageOn = false; //set to On
	static Vector lastSelectedNodes; // supplied by TGWikiBrowser ...........

	// H.A.

	/**
	 * Default constructor.
	 */
	public TGPanel() {
		lastSelectedNodes = new Vector(); // H.A.

		setLayout(null);

		setGraphEltSet(new GraphEltSet());
		addMouseListener(new BasicMouseListener());
		basicMML = new BasicMouseMotionListener();
		addMouseMotionListener(basicMML);

		graphListeners = new Vector();
		paintListeners = new Vector();

		adjustOriginLens = new AdjustOriginLens();
		switchSelectUI = new SwitchSelectUI();

		TGLayout tgLayout = new TGLayout(this);
		setTGLayout(tgLayout);
		tgLayout.start();
		setGraphEltSet(new GraphEltSet());

	}

	public void setLensSet(TGLensSet lensSet) {
		tgLensSet = lensSet;
	}

	public void setTGLayout(TGLayout tgl) {
		tgLayout = tgl;
	}

	public void setGraphEltSet(GraphEltSet ges) {
		completeEltSet = ges;
		visibleLocality = new VisibleLocality(completeEltSet);
		localityUtils = new LocalityUtils(visibleLocality, this);
	}

	public AdjustOriginLens getAdjustOriginLens() {
		return adjustOriginLens;
	}

	public SwitchSelectUI getSwitchSelectUI() {
		return switchSelectUI;
	}

	// color and font setters ......................

	public void setBackColor(Color color) {
		BACK_COLOR = color;
	}

	// Node manipulation ...........................

	/** Returns an Iterator over all nodes in the complete graph. */
	/*
	 * public Iterator getAllNodes() { return completeEltSet.getNodes(); }
	 */

	/** Return the current visible locality. */
	public ImmutableGraphEltSet getGES() {
		return visibleLocality;
	}

	/** Returns the current node count. */
	public int getNodeCount() {
		return completeEltSet.nodeCount();
	}

	/**
	 * Returns the current node count within the VisibleLocality.
	 * 
	 * @deprecated this method has been replaced by the
	 *             <tt>visibleNodeCount()</tt> method.
	 */
	public int nodeNum() {
		return visibleLocality.nodeCount();
	}

	/** Returns the current node count within the VisibleLocality. */
	public int visibleNodeCount() {
		return visibleLocality.nodeCount();
	}

	/**
	 * Return the Node whose ID matches the String <tt>id</tt>, null if no
	 * match is found.
	 * 
	 * @param id
	 *            The ID identifier used as a query.
	 * @return The Node whose ID matches the provided 'id', null if no match is
	 *         found.
	 */
	public Node findNode(String id) {
		if (id == null)
			return null; // ignore
		return completeEltSet.findNode(id);
	}

	/**
	 * Return the Node whose URL matches the String <tt>strURL</tt>, null if
	 * no match is found.
	 * 
	 * @param strURL
	 *            The URL identifier used as a query.
	 * @return The Node whose URL matches the provided 'URL', null if no match
	 *         is found.
	 */
	public Node findNodeByURL(String strURL) {
		if (strURL == null)
			return null; // ignore
		return completeEltSet.findNodeByURL(strURL);
	}

	/**
	 * Return a Collection of all Nodes whose label matches the String
	 * <tt>label</tt>, null if no match is found.
	 */
	public Collection findNodesByLabel(String label) {
		if (label == null)
			return null; // ignore
		return completeEltSet.findNodesByLabel(label);
	}

	/**
	 * Return the first Nodes whose label contains the String <tt>substring</tt>,
	 * null if no match is found.
	 * 
	 * @param substring
	 *            The Substring used as a query.
	 */
	public Node findNodeLabelContaining(String substring) {
		if (substring == null)
			return null; // ignore
		return completeEltSet.findNodeLabelContaining(substring);
	}

	/**
	 * Adds a Node, with its ID and label being the current node count plus 1.
	 * 
	 * @see com.touchgraph.graphlayout.Node
	 */
	public Node addNode() throws TGException {
		String id = String.valueOf(getNodeCount() + 1);
		return addNode(id, null);
	}

	/**
	 * Adds a Node, provided its label. The node is assigned a unique ID.
	 * 
	 * @see com.touchgraph.graphlayout.graphelements.GraphEltSet
	 */
	public Node addNode(String label) throws TGException {
		return addNode(null, label);
	}

	/**
	 * Adds a Node, provided its ID and label.
	 * 
	 * @see com.touchgraph.graphlayout.Node
	 */
	public Node addNode(String id, String label) throws TGException {
		Node node;
		if (label == null)
			node = new Node(id);
		else
			node = new Node(id, label);

		updateDrawPos(node); // The addNode() call should probably take a
		// position, this just sets it at 0,0
		addNode(node);
		return node;
	}

	/**
	 * Add the Node <tt>node</tt> to the visibleLocality, checking for ID
	 * uniqueness.
	 */
	public void addNode(final Node node) throws TGException {
		synchronized (localityUtils) {
			visibleLocality.addNode(node);
			resetDamper();
		}
	}

	/**
	 * Remove the Node object matching the ID <code>id</code>, returning true
	 * if the deletion occurred, false if a Node matching the ID does not exist
	 * (or if the ID value was null).
	 * 
	 * @param id
	 *            The ID identifier used as a query.
	 * @return true if the deletion occurred.
	 */
	public boolean deleteNodeById(String id) {
		if (id == null)
			return false; // ignore
		Node node = findNode(id);
		if (node == null)
			return false;
		else
			return deleteNode(node);
	}

	public boolean deleteNode(Node node) {
		synchronized (localityUtils) {
			if (visibleLocality.deleteNode(node)) { // delete from
				// visibleLocality, *AND
				// completeEltSet
				if (node == select)
					clearSelect();
				resetDamper();
				return true;
			}
			return false;
		}
	}

	public void clearAll() {
		synchronized (localityUtils) {
			synchronized (workerThreads) {
				for (Thread workerThread : workerThreads) {
					if (workerThread.isAlive()) {
						//workerThread.interrupt();
					}
				}
			}
			workerThreads.clear();
			visibleLocality.clearAll();
		}
	}

	public Node getSelect() {
		return select;
	}

	public Node getMouseOverN() {
		return mouseOverN;
	}

	public synchronized void setMouseOverN(Node node) {
		if (dragNode != null || maintainMouseOver)
			return; // So you don't accidentally switch nodes while dragging
		if (mouseOverN != node) {
			Node oldMouseOverN = mouseOverN;
			mouseOverN = node;
		}
		if (mouseOverN == null)
			setCursor(new Cursor(Cursor.MOVE_CURSOR));
		else {
			setCursor(new Cursor(Cursor.HAND_CURSOR));
			// println("MouseOver"+node.getID() );
		}
	}

	// Edge manipulation ...........................

	/** Returns an Iterator over all edges in the complete graph. */
	/*
	 * public Iterator getAllEdges() { return completeEltSet.getEdges(); }
	 */
	public void deleteEdge(Edge edge) {
		synchronized (localityUtils) {
			visibleLocality.deleteEdge(edge);
			resetDamper();
		}
	}

	public void deleteEdge(Node from, Node to) {
		synchronized (localityUtils) {
			visibleLocality.deleteEdge(from, to);
		}
	}

	/**
	 * Returns the current edge count in the complete graph.
	 */
	public int getEdgeCount() {
		return completeEltSet.edgeCount();
	}

	/**
	 * Return the number of Edges in the Locality.
	 * 
	 * @deprecated this method has been replaced by the
	 *             <tt>visibleEdgeCount()</tt> method.
	 */
	public int edgeNum() {
		return visibleLocality.edgeCount();
	}

	/**
	 * Return the number of Edges in the Locality.
	 */
	public int visibleEdgeCount() {
		return visibleLocality.edgeCount();
	}

	public Edge findEdge(Node f, Node t) {
		return visibleLocality.findEdge(f, t);
	}

	public void addEdge(Edge e) {
		synchronized (localityUtils) {
			visibleLocality.addEdge(e);
			resetDamper();
		}
	}

	public Edge addEdge(Node f, Node t, int tens) {
		synchronized (localityUtils) {
			return visibleLocality.addEdge(f, t, tens);
		}
	}

	public Edge getMouseOverE() {
		return mouseOverE;
	}

	public synchronized void setMouseOverE(Edge edge) {
		if (dragNode != null || maintainMouseOver)
			return; // No funny business while dragging
		if (mouseOverE != edge) {
			Edge oldMouseOverE = mouseOverE;
			mouseOverE = edge;
		}
	}

	// relationships

	public Node findOrCreateNode(String ID) throws TGException {
		Node n1 = findNode(ID);
		if (n1 == null)
			n1 = addNode(ID);
		return n1;
	}

	public void addRelationKeyed(String Key, String pred, String arg1, String arg2)
			throws TGException {
		if ((Key.equalsIgnoreCase(arg1)) || (Key.equalsIgnoreCase(arg2)))
			addRelation(pred, arg1, arg2);
	}

	public synchronized void addRelation(String pred, String arg1, String arg2)
			throws TGException {
		if (!isThread())
			return;

		Node NDEF;

		// some nodes that are in error to place on the map
		if ((arg1.trim() == "#<") || (arg1.trim() == ">"))
			return;
		if ((arg2.trim() == "#<") || (arg2.trim() == ">"))
			return;

		Node.DEFAULT_TYPE = Node.TYPE_RECTANGLE;
		if (arg1 == arg2) {
			arg2 = "{[" + arg1 + "]}";
		}
		Node n1 = findOrCreateNode(arg1);
		Node n2 = findOrCreateNode(arg2);

		// Eliminates duplicate direct links
		Edge FoundEdge;
		FoundEdge = findEdge(n1, n2);
		if (FoundEdge != null) {
			if (onlyThread!=null) {
                            onlyThread.addEdge(FoundEdge);
                        }
			return;
		}

		// p1.setNodeBackDefaultColor( Color.gray );

		// How do we want to cluster. Have a shorter length for stronger
		// relations and clustering
		// maybe you want it to be the opposite, so the properties of
		// individuals are strong
		// and the genls/isa tree is weaker and thus more spread out

		int ELen = Edge.DEFAULT_LENGTH;
		// if ((pred!="genls")&&(pred!="isa")) ELen=ELen/2;
		Edge e1;
		Edge e2;

		// if ((pred=="isa")||(pred=="genls"))
		{
			if ("genls".equals(pred))
				ELen = ELen / 2;
			if ("isa".equals(pred))
				ELen = ELen / 3;
			// e1.lbl=pred;
			e1 = addEdge(n1, n2, ELen);
			e1.setLbl(pred);
			if (visibleLocality.contains(e1)) {
				n1.setVisible(true);
				n2.setVisible(true);
			}
			if (onlyThread != null)
				onlyThread.addEdge(e1);
			if ("genls".equals(pred))
				e1.setColor(Color.green);
			if ("isa".equals(pred))
				e1.setColor(Color.yellow);
			if ("disjoint".equals(pred))
				e1.setColor(Color.red);
			if ("isa".equals(pred))
				n1.setType(Node.TYPE_ROUNDRECT);
		}
		/*
		 * else { //String pid = String.valueOf(getNodeCount()+1); String
		 * pid=pred+arg1+arg2; Node p1; p1=findNode(pid); if (p1!=null) return;
		 * 
		 * Node.DEFAULT_TYPE =Node.TYPE_ELLIPSE; p1=addNode(pid,pred);
		 * e1=addEdge(n1,p1,ELen); e2=addEdge(p1,n2,ELen); if (pred=="disjoint") {
		 * e1.setColor( Color.red); e2.setColor( Color.red); p1.setBackColor(
		 * Color.red); } e1.setLbl(pred); e2.setLbl(pred);
		 *  }
		 */

		Node.DEFAULT_TYPE = Node.TYPE_RECTANGLE;
	}

	// miscellany ..................................

	protected class AdjustOriginLens extends TGAbstractLens {
		protected void applyLens(TGPoint2D p) {
			p.x = p.x + TGPanel.this.getSize().width / 2;
			p.y = p.y + TGPanel.this.getSize().height / 2;
		}

		protected void undoLens(TGPoint2D p) {
			p.x = p.x - TGPanel.this.getSize().width / 2;
			p.y = p.y - TGPanel.this.getSize().height / 2;
		}
	}

	public class SwitchSelectUI extends TGAbstractClickUI {
		public void mouseClicked(MouseEvent e) {
			if (mouseOverN != null) {
				if (mouseOverN != select)
					setSelect(mouseOverN);
				else
					clearSelect();
			}
		}
	}

	void fireMovedEvent() {
		Vector listeners;

		synchronized (this) {
			listeners = (Vector) graphListeners.clone();
		}

		for (int i = 0; i < listeners.size(); i++) {
			GraphListener gl = (GraphListener) listeners.elementAt(i);
			gl.graphMoved();
		}
	}

	public void fireResetEvent() {
		Vector listeners;

		synchronized (this) {
			listeners = (Vector) graphListeners.clone();
		}

		for (int i = 0; i < listeners.size(); i++) {
			GraphListener gl = (GraphListener) listeners.elementAt(i);
			gl.graphReset();
		}
	}

	public synchronized void addGraphListener(GraphListener gl) {
		graphListeners.addElement(gl);
	}

	public synchronized void removeGraphListener(GraphListener gl) {
		graphListeners.removeElement(gl);
	}

	public synchronized void addPaintListener(TGPaintListener pl) {
		paintListeners.addElement(pl);
	}

	public synchronized void removePaintListener(TGPaintListener pl) {
		paintListeners.removeElement(pl);
	}

	private void redraw() {
		resetDamper();
	}

	public void setMaintainMouseOver(boolean maintain) {
		maintainMouseOver = maintain;
	}

	public void clearSelect() {
		if (select != null) {
			select = null;
			repaint();
		}
	}

	/**
	 * A convenience method that selects the first node of a graph, so that
	 * hiding works.
	 */
	public void selectFirstNode() {
		setSelect(getGES().getFirstNode());
	}

	public void setSelect(Node node) {
		if (node != null) {
			demoDB(node.getID(), false);
			select = node;
			updateVisList();
			repaint();
			println("SeletedNode:" + node.getID());
			try {
				fireMovedEvent();
				//((GraphListener)gl).graphMoved();
			} catch (Throwable e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			tgHistory.addItem(node.getID());

		} else if (node == null)
			clearSelect();
	}

	public void updateVisList() {
		visibleLocality.fillBox(tgVisList);

	}

	// H.A.
	public void setSelectedNodesVector(Vector v) {
		// if (select!=null) select.displayFullLabel(false);
		if (v != null) {
			// changeNodesToOriginalColors(lastSelectedNodes);
			lastSelectedNodes = v;
			// changeNodesToSelectedColors(lastSelectedNodes);
		}
	}

	public void multiSelect(TGPoint2D from, TGPoint2D to) {
		final double minX, minY, maxX, maxY;

		if (from.x > to.x) {
			maxX = from.x;
			minX = to.x;
		} else {
			minX = from.x;
			maxX = to.x;
		}
		if (from.y > to.y) {
			maxY = from.y;
			minY = to.y;
		} else {
			minY = from.y;
			maxY = to.y;
		}

		final Vector selectedNodes = new Vector();

		TGForEachNode fen = new TGForEachNode() {
			public void forEachNode(Node node) {
				double x = node.drawx;
				double y = node.drawy;
				if (x > minX && x < maxX && y > minY && y < maxY) {
					selectedNodes.addElement(node);
				}
			}
		};

		visibleLocality.forAllNodes(fen);

		if (selectedNodes.size() > 0) {
			int r = (int) (Math.random() * selectedNodes.size());
			setSelect((Node) selectedNodes.elementAt(r));
		} else {
			clearSelect();
		}
	}

	public void updateLocalityFromVisibility() throws TGException {
		visibleLocality.updateLocalityFromVisibility();
	}

	public void setLocale(Node node, int radius, int maxAddEdgeCount,
			int maxExpandEdgeCount, boolean unidirectional) throws TGException {
		localityUtils.setLocale(node, radius, maxAddEdgeCount, maxExpandEdgeCount,
				unidirectional);
	}

	public void fastFinishAnimation() { // Quickly wraps up the add node
		// animation
		localityUtils.fastFinishAnimation();
	}

	public void setLocale(Node node, int radius) throws TGException {
		localityUtils.setLocale(node, radius);
	}

	public void expandNode(Node node) {
		// KHC: expand the knowledge frontier
		// demoDB(node.getID());

		localityUtils.expandNode(node);
	}

	public void inspectNode(Node node) {
		// KHC: expand the knowledge frontier
		// demoDB(node.getID());

		expandNode(node);
		glp.setWikiTextPane(node);
	}

	public void inspectNode(Node node, String urlType) {
		// KHC: expand the knowledge frontier
		// demoDB(node.getID());

		String url = node.getURL(urlType);
		expandNode(node);
		glp.setWikiTextPane(url);
		println("inspectNode " + node.getID() + " urlType=" + urlType + " URL="
				+ url);
	}

	public void hideNode(Node hideNode) {
		localityUtils.hideNode(hideNode);
	}

	public void collapseNode(Node collapseNode) {
		localityUtils.collapseNode(collapseNode);
	}

	public void hideEdge(Edge hideEdge) {
		visibleLocality.removeEdge(hideEdge);
		if (mouseOverE == hideEdge)
			setMouseOverE(null);
		resetDamper();
	}

	public void hideEdgesWithLabel(String hideEdge) {
		Object Edge = visibleLocality.removeEdgesWithLabel(hideEdge, mouseOverE);
		if (mouseOverE == Edge)
			setMouseOverE(null);
		resetDamper();
	}

	public void setDragNode(Node node) {
		dragNode = node;
		tgLayout.setDragNode(node);
	}

	public Node getDragNode() {
		return dragNode;
	}

	void setMousePos(Point p) {
		mousePos = p;
	}

	public Point getMousePos() {
		return mousePos;
	}

	/** Start and stop the damper. Should be placed in the TGPanel too. */
	public void startDamper() {
		if (tgLayout != null)
			tgLayout.startDamper();
	}

	public void stopDamper() {
		if (tgLayout != null)
			tgLayout.stopDamper();
	}

	/** Makes the graph mobile, and slowly slows it down. */
	public void resetDamper() {
		if (tgLayout != null)
			tgLayout.resetDamper();
	}

	/** Gently stops the graph from moving */
	public void stopMotion() {
		if (tgLayout != null)
			tgLayout.stopMotion();
	}

	class BasicMouseListener extends MouseAdapter {

		public void mouseEntered(MouseEvent e) {
			addMouseMotionListener(basicMML);
		}

		public void mouseExited(MouseEvent e) {
			removeMouseMotionListener(basicMML);
			mousePos = null;
			setMouseOverN(null);
			setMouseOverE(null);
			repaint();
		}
	}

	class BasicMouseMotionListener implements MouseMotionListener {
		public void mouseDragged(MouseEvent e) {
			mousePos = e.getPoint();
			findMouseOver();
			try {
				Thread.currentThread().sleep(6); // An attempt to make the
				// cursor flicker less
			} catch (InterruptedException ex) {
				// break;
			}
		}

		public void mouseMoved(MouseEvent e) {
			mousePos = e.getPoint();
			synchronized (this) {
				Edge oldMouseOverE = mouseOverE;
				Node oldMouseOverN = mouseOverN;
				findMouseOver();
				if (oldMouseOverE != mouseOverE || oldMouseOverN != mouseOverN) {
					repaint();
				}
				// Replace the above lines with the commented portion below to
				// prevent whole graph
				// from being repainted simply to highlight a node On mouseOver.
				// This causes some annoying flickering though.
				/*
				 * if(oldMouseOverE!=mouseOverE) { if (oldMouseOverE!=null) {
				 * synchronized(oldMouseOverE) {
				 * oldMouseOverE.paint(TGPanel.this.getGraphics(),TGPanel.this);
				 * oldMouseOverE.from.paint(TGPanel.this.getGraphics(),TGPanel.this);
				 * oldMouseOverE.to.paint(TGPanel.this.getGraphics(),TGPanel.this);
				 *  } }
				 * 
				 * if (mouseOverE!=null) { synchronized(mouseOverE) {
				 * mouseOverE.paint(TGPanel.this.getGraphics(),TGPanel.this);
				 * mouseOverE.from.paint(TGPanel.this.getGraphics(),TGPanel.this);
				 * mouseOverE.to.paint(TGPanel.this.getGraphics(),TGPanel.this); } } }
				 * 
				 * if(oldMouseOverN!=mouseOverN) { if (oldMouseOverN!=null)
				 * oldMouseOverN.paint(TGPanel.this.getGraphics(),TGPanel.this);
				 * if (mouseOverN!=null)
				 * mouseOverN.paint(TGPanel.this.getGraphics(),TGPanel.this); }
				 */
			}
		}
	}

	protected synchronized void findMouseOver() {

		if (mousePos == null) {
			setMouseOverN(null);
			setMouseOverE(null);
			return;
		}

		final int mpx = mousePos.x;
		final int mpy = mousePos.y;

		final Node[] monA = new Node[1];
		final Edge[] moeA = new Edge[1];

		TGForEachNode fen = new TGForEachNode() {

			double minoverdist = 100; // Kind of a hack (see second if

			// statement)

			// Nodes can be as wide as 200 (=2*100)
			public void forEachNode(Node node) {
				double x = node.drawx;
				double y = node.drawy;

				double dist = Math.sqrt((mpx - x) * (mpx - x) + (mpy - y) * (mpy - y));

				if ((dist < minoverdist) && node.containsPoint(mpx, mpy)) {
					minoverdist = dist;
					monA[0] = node;
				}
			}
		};
		visibleLocality.forAllNodes(fen);

		TGForEachEdge fee = new TGForEachEdge() {

			double minDist = 8; // Tangential distance to the edge

			double minFromDist = 1000; // Distance to the edge's "from" node

			public void forEachEdge(Edge edge) {
				double x = edge.from.drawx;
				double y = edge.from.drawy;
				double dist = edge.distFromPoint(mpx, mpy);
				if (dist < minDist) { // Set the over edge to the edge with
					// the minimun tangential distance
					minDist = dist;
					minFromDist = Math
							.sqrt((mpx - x) * (mpx - x) + (mpy - y) * (mpy - y));
					moeA[0] = edge;
				} else if (dist == minDist) { // If tangential distances are
					// identical, chose
					// the edge whose "from" node is closest.
					double fromDist = Math.sqrt((mpx - x) * (mpx - x) + (mpy - y)
							* (mpy - y));
					if (fromDist < minFromDist) {
						minFromDist = fromDist;
						moeA[0] = edge;
					}
				}
			}
		};
		visibleLocality.forAllEdges(fee);

		setMouseOverN(monA[0]);
		if (monA[0] == null)
			setMouseOverE(moeA[0]);
		else
			setMouseOverE(null);
	}

	TGPoint2D topLeftDraw = null;

	TGPoint2D bottomRightDraw = null;

	public TGPoint2D getTopLeftDraw() {
		return new TGPoint2D(topLeftDraw);
	}

	public TGPoint2D getBottomRightDraw() {
		return new TGPoint2D(bottomRightDraw);
	}

	public TGPoint2D getCenter() {
		return tgLensSet.convDrawToReal(getSize().width / 2, getSize().height / 2);
	}

	public TGPoint2D getDrawCenter() {
		return new TGPoint2D(getSize().width / 2, getSize().height / 2);
	}

	public void updateGraphSize() {
		if (topLeftDraw == null)
			topLeftDraw = new TGPoint2D(0, 0);
		if (bottomRightDraw == null)
			bottomRightDraw = new TGPoint2D(0, 0);

		TGForEachNode fen = new TGForEachNode() {
			boolean firstNode = true;

			public void forEachNode(Node node) {
				if (firstNode) { // initialize topRight + bottomLeft
					topLeftDraw.setLocation(node.drawx, node.drawy);
					bottomRightDraw.setLocation(node.drawx, node.drawy);
					firstNode = false;
				} else { // Standard max and min finding
					topLeftDraw.setLocation(Math.min(node.drawx, topLeftDraw.x), Math
							.min(node.drawy, topLeftDraw.y));
					bottomRightDraw.setLocation(Math.max(node.drawx, bottomRightDraw.x),
							Math.max(node.drawy, bottomRightDraw.y));
				}
			}
		};

		visibleLocality.forAllNodes(fen);
	}

	public synchronized void processGraphMove() {
		updateDrawPositions();
		updateGraphSize();
	}

	public synchronized void repaintAfterMove() { // Called by TGLayout +
		// others to indicate that
		// graph has moved
		processGraphMove();
		findMouseOver();
		fireMovedEvent();
		repaint();
	}

	public void updateDrawPos(Node node) { // sets the visual position from the
		// real position
		TGPoint2D p = tgLensSet.convRealToDraw(node.x, node.y);
		node.drawx = p.x;
		node.drawy = p.y;
	}

	public void updatePosFromDraw(Node node) { // sets the real position from
		// the visual position
		TGPoint2D p = tgLensSet.convDrawToReal(node.drawx, node.drawy);
		node.x = p.x;
		node.y = p.y;
	}

	public void updateDrawPositions() {
		TGForEachNode fen = new TGForEachNode() {
			public void forEachNode(Node node) {
				updateDrawPos(node);
			}
		};
		visibleLocality.forAllNodes(fen);
	}

	Color myBrighter(Color c) {
		int r = c.getRed();
		int g = c.getGreen();
		int b = c.getBlue();

		r = Math.min(r + 96, 255);
		g = Math.min(g + 96, 255);
		b = Math.min(b + 96, 255);

		return new Color(r, g, b);
	}

	public synchronized void paint(Graphics g) {
		update(g);
	}

	public synchronized void update(Graphics g) {
		Dimension d = getSize();
		if ((offscreen == null) || (d.width != offscreensize.width)
				|| (d.height != offscreensize.height)) {
			offscreen = createImage(d.width, d.height);
			offscreensize = d;
			offgraphics = offscreen.getGraphics();

			processGraphMove();
			findMouseOver();
			fireMovedEvent();
		}

		offgraphics.setColor(BACK_COLOR);
		offgraphics.fillRect(0, 0, d.width, d.height);

		// if you want a logo, then define the image and run the commented code
		// below
		// offgraphics.drawImage(logoimage, x, y, null);

		// if you want a logo, then define the image and run the commented code
		// below
		// if (BG_IMAGE!=null) offgraphics.drawImage(BG_IMAGE,x, y, null);

		synchronized (this) {
			paintListeners = (Vector) paintListeners.clone();
		}

		for (int i = 0; i < paintListeners.size(); i++) {
			TGPaintListener pl = (TGPaintListener) paintListeners.elementAt(i);
			pl.paintFirst(offgraphics);
		}

		TGForEachEdge fee = new TGForEachEdge() {
			public void forEachEdge(Edge edge) {
				edge.paint(offgraphics, TGPanel.this);
			}
		};

		visibleLocality.forAllEdges(fee);

		for (int i = 0; i < paintListeners.size(); i++) {
			TGPaintListener pl = (TGPaintListener) paintListeners.elementAt(i);
			pl.paintAfterEdges(offgraphics);
		}

		TGForEachNode fen = new TGForEachNode() {
			public void forEachNode(Node node) {
				node.paint(offgraphics, TGPanel.this);
			}
		};

		visibleLocality.forAllNodes(fen);

		if (mouseOverE != null) { // Make the edge the mouse is over appear on
			// top.
			mouseOverE.paint(offgraphics, this);
			mouseOverE.from.paint(offgraphics, this);
			mouseOverE.to.paint(offgraphics, this);
		}

		if (select != null) { // Make the selected node appear on top.
			select.paint(offgraphics, this);
		}

		if (mouseOverN != null) { // Make the node the mouse is over appear on
			// top.
			mouseOverN.paint(offgraphics, this);
		}

		for (int i = 0; i < paintListeners.size(); i++) {
			TGPaintListener pl = (TGPaintListener) paintListeners.elementAt(i);
			pl.paintLast(offgraphics);
		}

		paintComponents(offgraphics); // Paint any components that have been
		// added to this panel
		g.drawImage(offscreen, 0, 0, null);

	}

	public void updateFilterList() {
		visibleLocality.filterList(edgeFilterList);
		repaint();
	}

	/**
	 * Used to set the background image of the touchgraph panel that is
	 * specified by the given URL as a String. The URL must be a completely
	 * qualified URL.
	 * 
	 * @see URL
	 */
	// public void setBGImage( String url ) {
	// tk = Toolkit.getDefaultToolkit();
	// try {
	// imageURL = new URL(url);
	// } catch (MalformedURLException e) {
	// println("URL Error!");
	// e.printStackTrace();
	// }
	// BG_IMAGE = tk.getImage(imageURL);
	// BGImageON();
	// repaint();
	// }
	/**
	 * The background image <tt>BG_IMAGE</tt> will be turned on for the
	 * background of the touchgraph panel if the URL <tt>imageURL</tt> is
	 * valid and the Image <tt>BG_IMAGE</tt> exists.
	 */
	// public void BGImageON() {
	// if (imageURL == null) println("Error: No URL to Background!");
	// if (BG_IMAGE == null) println("Error: No Background Image
	// Found!");
	// if ((imageURL != null) && (BG_IMAGE != null)) {
	// this.bgImageOn = true;
	// repaint();
	// }
	// }
	/** Turns off the background image of the touchgraph panel. */
	// public void BGImageOFF() {
	// bgImageOn = false;
	// repaint();
	// }

	/**
	 * Convenience method to check to see if the backgroud image is set to
	 * display.
	 */
	// public boolean isBGImageON() { return bgImageOn; }

	public static void main(String[] args) {

		Frame frame;
		frame = new Frame("TGPanel");
		TGPanel tgPanel = new TGPanel();

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		TGLensSet tgls = new TGLensSet();
		tgls.addLens(tgPanel.getAdjustOriginLens());
		tgPanel.setLensSet(tgls);
		try {
			tgPanel.addNode(); // Add a starting node.
		} catch (TGException tge) {
			System.err.println(tge.getMessage());
		}
		tgPanel.setVisible(true);
		new GLEditUI(tgPanel).activate();
		frame.add("Center", tgPanel);
		frame.setSize(500, 500);
		frame.setVisible(true);
	}

	public synchronized void addAllRelation(String relation, String key,
			CycList ResultList) {
		try {
			ArrayList addList = new ArrayList();
			addList.addAll(ResultList);
			for (int i = 0; i < addList.size(); i++) {
				Object value = addList.get(i);
				addRelation(relation, key, value.toString());
			}
			Thread.currentThread().sleep(5);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

	}

	public synchronized void addRelationAll(String relation, String key,
			CycList ResultList) {
		try {
			ArrayList addList = new ArrayList();
			addList.addAll(ResultList);
			for (int i = 0; i < addList.size(); i++) {
				Object value = addList.get(i);
				addRelation(relation, value.toString(), key);
			}
			Thread.currentThread().sleep(5);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

	}

	public void expandPred(String pred, String key) {
		CycList response;

		try {

			response = cycAccess.getArg2s(pred, key, "EverythingPSC");
			println("expandPred " + pred + "[" + key + "]:" + response.cyclify());
			addAllRelation(pred, key, response);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		// expandPredThread("PRED",pred,key);

	}

	public void expandRole(String pred, String key) {

		CycList response;

		try {
			response = cycAccess.getArg1s(pred, key, "EverythingPSC");
			println("expandRole " + pred + "[" + key + "]:" + response.cyclify());

			addRelationAll(pred, key, response);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		// expandPredThread("ROLE",pred,key);
	}

	public void expandKBLink(String pred, String key) {
		expandRole(pred, key);
		expandPred(pred, key);
	}

	public void expandPredThread(String task, String pred, String key) {
		ExpandPredThread expander = new ExpandPredThread();
		expander.key = key;
		expander.pred = pred;
		expander.task = task;
		expander.targetPanel = this;
		expander.radius = glp.getLocalityRadius();
		expander.start();

	}

	public void queryKB1(String key) {

		println("\n**** test QueryKB1****");
		try {
			checkCycAccess();
			CycConstant mt = cycAccess.getKnownConstantByName("EverythingPSC");
			Object keyTerm = getKnownConstantByName(key);
			CycList query = cycAccess.makeCycList("(#$genls #$" + key + " ?WHAT)");
			if (true || cycAccess.isFormulaWellFormed(query, mt)) {
				CycList response;
				/*
				 * // (key) is the source response =
				 * cycAccess.getGenls(keyTerm); println("Genls
				 * Response:"+response.cyclify());
				 * addAllRelation("genls",key,response);
				 * 
				 * response = cycAccess.getIsas(keyTerm);
				 * println("Isas Response:"+response.cyclify());
				 * addAllRelation("isa",key,response);
				 * 
				 * 
				 * response = cycAccess.getLocalDisjointWith(keyTerm);
				 * println("Disjoint Response:"+response.cyclify());
				 * addAllRelation("disjoint",key,response);
				 * 
				 * response = cycAccess.getGenlPreds(keyTerm);
				 * println("GenlPreds Response:"+response.cyclify());
				 * addAllRelation("genlPred",key,response);
				 * 
				 *  // (key) is the target response =
				 * cycAccess.getSpecs(keyTerm); println("Specs
				 * Response:"+response.cyclify());
				 * addRelationAll("genls",key,response);
				 * 
				 * response = cycAccess.getInstances(keyTerm);
				 * println("Instances Response:"+response.cyclify());
				 * addRelationAll("isa",key,response);
				 */
				expandKBLink("isa", key);
				expandKBLink("genls", key);
				expandKBLink("disjointWith", key);
				expandKBLink("genlPreds", key);

			}
			println("**** QueryKB OK ****");
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	@SuppressWarnings("deprecation")
	synchronized static public void checkCycAccess() {
		// TODO Auto-generated method stub
		if (cycAccess != null) {
			if (!cycAccess.isClosed())
				return;
		}
		try {
			cycAccess = CycAccess.current();
		} catch (Exception e) {
			try {
				cycAccess = new CycAccess(CycConnection.DEFAULT_HOSTNAME,
						CycConnection.DEFAULT_BASE_PORT);
				cycAccess.persistentConnection = 10;
				CycAccess.setSharedCycAccessInstance(cycAccess);
			} catch (Exception e1) {
			}
		}
		//return cycAccess;
	}

	public void queryKB2(String key) {

		println("\n**** test QueryKB2****");
		checkCycAccess();
		try {

			CycConstant mt = cycAccess.getKnownConstantByName("EverythingPSC");
			Object keyTerm = getKnownConstantByName(key);
			//	CycList query = cycAccess.makeCycList("(#$genls #$" + key + " ?WHAT)");

			if (keyTerm instanceof CycObject) {
				if (cycAccess.isa((CycObject) keyTerm, "SocialBeing")) {
					// see PersonalAssociationPredicate
					expandKBLink("acquaintedWith", key);
					expandKBLink("affiliatedWith", key);
					expandKBLink("friends", key);
					expandKBLink("relatives", key);
					expandKBLink("mother", key);
					expandKBLink("father", key);
					expandKBLink("sisters", key);
					expandKBLink("brothers", key);
					expandKBLink("sons", key);
					// expandPred("biologicalSons",key);
					expandKBLink("daughters", key);
					// expandPred("biologicalDaughters",key);
					// expandPred("classmates",key);
					expandKBLink("mate", key);
					expandKBLink("spouse", key);
					expandKBLink("husband", key);
					expandKBLink("wife", key);
					expandKBLink("romanticInterest", key);
					expandKBLink("sexualPartners", key);
					expandKBLink("businessPartners", key);
					expandKBLink("employees", key);
					expandPred("residesInRegion", key);
					expandPred("objectFoundInLocation", key);
					expandRole("performedBy", key);
					expandRole("doneBy", key);
					expandRole("objectActedOn", key);
					expandRole("maleficiary", key);

				}
				if (cycAccess.isa((CycObject) keyTerm, "Action")) {
					expandKBLink("firstSubEvents", key);
					expandKBLink("subEvents", key);
					expandPred("eventPosesThreat", key);
					expandKBLink("temporallyStartedBy", key);
					expandPred("performedBy", key);
					expandPred("doneBy", key);
					expandPred("objectActedOn", key);
					expandPred("maleficiary", key);

				}
				expandPred("properSubEventTypes", key);

				if (cycAccess.isa((CycObject) keyTerm, "Agent-Generic")) {
					// links we expand
					expandPred("possesses", key);
					expandPred("owns", key);
					// expandPred("linked",key);

					expandPred("hasBeliefSystems", key);
					// expandPred("knowsAbout",key); // long
					expandPred("hasLeaders", key);
					expandPred("hasMembers", key);
					expandPred("accused", key);
					expandRole("directingAgent", key);
					expandRole("performedBy", key);
					expandRole("recipientOfInfo", key);
					expandRole("senderOfInfo", key);
					expandRole("doneBy", key);
					expandRole("buyer", key);
					expandRole("seller", key);

					// expandRole("deviceUsed",key); // long
					// expandRole("knowsAbout",key); // long
				}
				if (cycAccess.isa((CycObject) keyTerm, "GeopoliticalEntity")) {
					expandPred("imports", key);
					expandPred("exports", key);
					expandPred("hasAsHeadOfGovernment", key);
					expandPred("governmentType", key);
					expandPred("legalSystem", key);
					expandPred("capitalCity", key);
					expandPred("majorReligions", key);
				}
			}
			expandKBLink("imports", key);
			expandKBLink("exports", key);
			expandKBLink("conceptuallyRelated", key);
			expandKBLink("genlPreds", key);
			expandKBLink("negationPreds", key);
			expandKBLink("typeGenls", key);

			expandKBLink("termOfUnit", key);
			expandKBLink("posForms", key);
			expandKBLink("placeName-Standard", key);
			expandKBLink("definingMt", key);
			expandKBLink("nameString", key);
			expandKBLink("oldConstantName", key);
			expandKBLink("familyName", key);
			expandKBLink("givenNames", key);
			expandKBLink("commonNickname", key);
			expandKBLink("scientificName", key);

			expandKBLink("countryOfCity", key);
			expandKBLink("geographicalSubRegionsOfUSState", key);
			expandKBLink("cityInState", key);
			expandKBLink("airportServicesCity", key);
			expandKBLink("synonymousExternalConcept", key);
			expandKBLink("geographicalSubRegions", key);
			// expandKBLink("airportHasIATACodea",key);
			expandKBLink("dateOfEvent", key);
			expandKBLink("residesInRegion", key);
			expandKBLink("bordersOn", key);
			expandKBLink("inRegion", key);
			expandKBLink("populationOfRegion", key);
			expandKBLink("ethnicGroupsHere", key);
			expandKBLink("inhabitantTypes", key);
			expandKBLink("politiesBorderEachOther", key);
			expandKBLink("geographicalSubRegionsOfContinent", key);
			expandKBLink("genlMt", key);
			expandKBLink("genlPreds", key);
			expandKBLink("eventOccursAt", key);
			expandKBLink("perpetrator", key);
			expandKBLink("groupMemberType", key);
			expandKBLink("hasMembers", key);
			expandKBLink("territoryOf", key);
			expandKBLink("genlInverse", key);
			expandKBLink("goals", key);
			expandKBLink("agreeingAgents", key);
			expandKBLink("armedWithWeaponType", key);
			expandKBLink("obligatedAgents", key);
			// expandRole("goalCategoryForAgent",key);
			// expandRole("organismKilled",key);
			expandKBLink("customers", key);
			expandKBLink("duties", key);

			println("**** QueryKB OK ****");
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

	}

	private Object getKnownConstantByName(String key) {
		key = key.trim();
		if (key.startsWith("#$")) key = key.substring(2);
		Object ret = null;
		checkCycAccess();
		if (ret == null && key.startsWith("(") && key.endsWith(")")) {
			ret = new CycListParser(cycAccess).read(key);
		}
		if (ret == null && key.startsWith("\"") && key.endsWith("\"")) {
			ret = key.substring(1, key.length() - 2).replace("\\\n", "\n").replace(
					"\\", "");
		}
		try {			
			ret = cycAccess.getKnownConstantByName(key);
		} catch (Exception e) {
		}
		if (ret != null)
			return ret;
		return key;
	}

	public void queryKB3(String key, boolean clearNext) {
		println("\n**** test QueryKB 3**** " + key);
		synchronized (localityUtils) {
			if (!isThread())
				return;
		}
		try {
			Object keyTerm = getKnownConstantByName(key);
			if (clearNext) {
				clearAll();
			}
			CycList query = allTermAssertions(keyTerm);
			if (query.size() == 0) {
				println("No term assertions: " + keyTerm);
				return;
			}
			for (Iterator iterator = query.iterator(); iterator.hasNext();) {
				Object object = (Object) iterator.next();
				if (onlyThread != null && onlyThread != Thread.currentThread())
					return;
				displayAssertion(object);
			}

			println("**** QueryKB OK **** " + key);
		} catch (org.opencyc.util.OpenCycTaskInterruptedException e) {
			println("**** QueryKB Early **** " + key);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			println("**** QueryKB Early **** " + key);
		}
	}

	private boolean isFiltered(Object keyTerm) {
		CycList query = CycList.list(CycAccess.isa, keyTerm, CycAccess.collection);
		try {
			return cycAccess.askNewCycQuery(query,CycAccess.universalVocabularyMt, new HashMap()).size()!=0;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CycApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	private void displayAssertion(final Object objectIn) throws TGException,
			UnknownHostException, CycApiException, IOException {
		if (!isThread())
			return;
		Object object = objectIn;
		if (object instanceof CycAssertion) {
			CycAssertion ass = (CycAssertion) object;
			object = getFormula(ass);
		}
		try {
			if (object instanceof CycList) {
				CycList list = (CycList) object;
				if (list.size() == 0) {
					println("CantGet: " + objectIn);
					return;
				}
				if (NIL.equals(list.first())) {
					list = (CycList) (((CycList) list.second()).first());
				}
				String fisrt = toNodeString(list.first());
				if (fisrt.equals("not")) {
					return;
				}
				if (fisrt.equals("not")) {
					return;
				}
				if (fisrt.equals("termOfUnit")) {
					return;
				}
				if (fisrt.equals("implies")) {
					return;
				}
				switch (list.size()) {
				case 3: {
					addRelation(fisrt, toNodeString(list.second()), toNodeString(list
							.third()));
					return;
				}
				case 2: {
					addRelation("->", fisrt, toNodeString(list.second()));
					return;
				}
				default:
					println("arity " + list.size() + ": " + list);
				}
				return;
			}
		} catch (DontUseException e) {
		}
		println("CantGet: " + object);
	}

	private Object getFormula(CycAssertion ass) throws UnknownHostException,
			CycApiException, IOException {
		checkCycAccess();
		return cycAccess.converseList(CycList.list(new CycSymbol(
				"ASSERTION-EL-FORMULA"), ass));
	}

	private String toNodeString(Object first) throws DontUseException {
		if (first == null)
			throw new DontUseException();
		String ret = first.toString();
		if (first instanceof String) {
			if (false)
				return "\"" + ret.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
			throw new DontUseException();
		}
		int len = ret.length();
		if (len < 3)
			return ret;
		if (ret.substring(2).contains("("))
			throw new DontUseException();
		return ret;
	}

	private CycList allTermAssertions(Object keyTerm)
			throws UnknownHostException, CycApiException, IOException {
		checkCycAccess();
		CycObject ata = new CycSymbol("ALL-TERM-ASSERTIONS");
		if (cycAccess==null) return new CycList();
		return cycAccess.converseList(CycList.list(ata, keyTerm));
	}

	static public void println(String string) {
		System.err.println(string);
	}

	public void demoDB(String key, boolean clearNext) {
		ExpandThread expander = new ExpandThread();
		expander.expandKey = key;
		expander.targetPanel = this;
		expander.clearNext = clearNext;
		expander.radius = glp.getLocalityRadius();
		onlyThread = expander;
		expander.start();
	}

	public void demoDB2(String key) {
		// queryKB(key);
		try {
			addRelationKeyed(key, "genls", "SpaceRegion", "Region");
			addRelationKeyed(key, "disjoint", "SpaceRegion", "GeographicArea");
			addRelationKeyed(key, "genls", "OuterSpaceRegion", "SpaceRegion");
			addRelationKeyed(key, "subrelation", "subRegion", "part");
			addRelationKeyed(key, "genls", "StormFront", "AtmosphericRegion");
			addRelationKeyed(key, "isa", "Outside", "Region");
			addRelationKeyed(key, "genls", "MilitaryFront", "GeographicArea");
			addRelationKeyed(key, "genls", "Field", "LandArea");
			addRelationKeyed(key, "genls", "Lawn", "Field");
			addRelationKeyed(key, "genls", "Park", "LandArea");
			addRelationKeyed(key, "genls", "CityDistrict", "GeopoliticalArea");
			addRelationKeyed(key, "genls", "Downtown", "CityDistrict");
			addRelationKeyed(key, "genls", "County", "GeopoliticalArea");
			addRelationKeyed(key, "genls", "County", "LandArea");
			addRelationKeyed(key, "isa", "RedRiver", "River");
			addRelationKeyed(key, "part", "RedRiver", "UnitedStates");
			addRelationKeyed(key, "isa", "Laos", "Nation");
			addRelationKeyed(key, "isa", "VirginIslands", "Collection");
			addRelationKeyed(key, "isa", "Guam", "Island");
			addRelationKeyed(key, "isa", "Cuba", "Nation");
			addRelationKeyed(key, "isa", "Cuba", "Island");
			addRelationKeyed(key, "isa", "Mexico", "Nation");
			addRelationKeyed(key, "meetsSpatially", "Mexico", "UnitedStates");
			addRelationKeyed(key, "isa", "Canada", "Nation");
			addRelationKeyed(key, "meetsSpatially", "Canada", "UnitedStates");
			addRelationKeyed(key, "isa", "UnitedStates", "Nation");
			addRelationKeyed(key, "genls", "AmericanState", "StateOrProvince");
			addRelationKeyed(key, "isa", "Alaska", "AmericanState");
			addRelationKeyed(key, "isa", "California", "AmericanState");
			addRelationKeyed(key, "isa", "Hawaii", "AmericanState");
			addRelationKeyed(key, "meetsSpatially", "Hawaii", "PacificOcean");
			addRelationKeyed(key, "isa", "NewYorkState", "AmericanState");
			addRelationKeyed(key, "isa", "Pennsylvania", "AmericanState");
			addRelationKeyed(key, "isa", "Ohio", "AmericanState");
			addRelationKeyed(key, "isa", "RhodeIsland", "AmericanState");
			addRelationKeyed(key, "part", "RhodeIsland", "NewEngland");
			addRelationKeyed(key, "isa", "Texas", "AmericanState");
			addRelationKeyed(key, "isa", "Virginia", "AmericanState");
			addRelationKeyed(key, "meetsSpatially", "Virginia", "WashingtonDC");
			addRelationKeyed(key, "isa", "Georgia", "AmericanState");
			addRelationKeyed(key, "isa", "NewEngland", "GeographicArea");
			addRelationKeyed(key, "part", "NewEngland", "UnitedStates");
			addRelationKeyed(key, "genls", "AmericanCity", "City");
			addRelationKeyed(key, "isa", "NewYorkCity", "AmericanCity");
			addRelationKeyed(key, "part", "NewYorkCity", "NewYorkState");
			addRelationKeyed(key, "isa", "WashingtonDC", "AmericanCity");
			addRelationKeyed(key, "isa", "Chicago", "AmericanCity");
			addRelationKeyed(key, "isa", "Dallas", "AmericanCity");
			addRelationKeyed(key, "isa", "KansasCityMissouri", "AmericanCity");
			addRelationKeyed(key, "isa", "SaintLouis", "AmericanCity");
			addRelationKeyed(key, "isa", "Boston", "AmericanCity");
			addRelationKeyed(key, "isa", "LosAngeles", "AmericanCity");
			addRelationKeyed(key, "part", "LosAngeles", "California");
			addRelationKeyed(key, "isa", "SanFrancisco", "AmericanCity");
			addRelationKeyed(key, "part", "SanFrancisco", "California");
			addRelationKeyed(key, "isa", "ManchesterNewHampshire", "AmericanCity");
			addRelationKeyed(key, "isa", "Philadelphia", "AmericanCity");
			addRelationKeyed(key, "part", "Philadelphia", "Pennsylvania");
			addRelationKeyed(key, "isa", "Detroit", "AmericanCity");
			addRelationKeyed(key, "isa", "PuertoRico", "Island");
			addRelationKeyed(key, "part", "PuertoRico", "UnitedStates");
			addRelationKeyed(key, "isa", "Russia", "Nation");
			addRelationKeyed(key, "isa", "Europe", "Continent");
			addRelationKeyed(key, "genls", "EuropeanCity", "City");
			addRelationKeyed(key, "isa", "Paris", "EuropeanCity");
			addRelationKeyed(key, "part", "Paris", "France");
			addRelationKeyed(key, "isa", "Warsaw", "EuropeanCity");
			addRelationKeyed(key, "isa", "France", "Nation");
			addRelationKeyed(key, "isa",
					"UnitedKingdomOfGreatBritainAndNorthernIreland", "Nation");
			addRelationKeyed(key, "isa", "London", "City");
			addRelationKeyed(key, "part", "London",
					"UnitedKingdomOfGreatBritainAndNorthernIreland");
			addRelationKeyed(key, "isa", "Ireland", "Nation");
			addRelationKeyed(key, "isa", "SovietUnion", "Nation");
			addRelationKeyed(key, "isa", "Russia", "Nation");
			addRelationKeyed(key, "isa", "Greece", "Nation");
			addRelationKeyed(key, "part", "Greece", "Europe");
			addRelationKeyed(key, "part", "Germany", "Europe");
			addRelationKeyed(key, "isa", "Sweden", "Nation");
			addRelationKeyed(key, "part", "Sweden", "Europe");
			addRelationKeyed(key, "isa", "Japan", "Nation");
			addRelationKeyed(key, "isa", "Japan", "Island");
			addRelationKeyed(key, "genls", "Industry", "Collection");
			addRelationKeyed(key, "genls", "GroupOfAnimals", "Group");
			addRelationKeyed(key, "genls", "Brood", "GroupOfAnimals");
			addRelationKeyed(key, "genls", "AnimalTeam", "GroupOfAnimals");
			addRelationKeyed(key, "genls", "DramaticCast", "GroupOfPeople");
			addRelationKeyed(key, "genls", "Orchestra", "GroupOfPeople");
			addRelationKeyed(key, "isa", "Jury", "GroupOfPeople");
			addRelationKeyed(key, "isa", "Antisemitism", "BeliefGroup");
			addRelationKeyed(key, "subrelation", "groupMember", "member");
			addRelationKeyed(key, "genls", "Proprietorship", "CommercialAgent");
			addRelationKeyed(key, "genls", "Restaurant", "CommercialAgent");
			addRelationKeyed(key, "genls", "Cafeteria", "Restaurant");
			addRelationKeyed(key, "genls", "Tavern", "Restaurant");
			addRelationKeyed(key, "genls", "TransportationCompany", "CommercialAgent");
			addRelationKeyed(key, "genls", "RailroadCompany", "TransportationCompany");
			addRelationKeyed(key, "genls", "WholesaleStore", "MercantileOrganization");
			addRelationKeyed(key, "genls", "RetailStore", "MercantileOrganization");
			addRelationKeyed(key, "disjoint", "RetailStore", "WholesaleStore");
			addRelationKeyed(key, "genls", "GroceryStore", "RetailStore");
			addRelationKeyed(key, "genls", "ShoppingMall", "MercantileOrganization");
			addRelationKeyed(key, "genls", "MissionOrganization",
					"ReligiousOrganization");
			addRelationKeyed(key, "genls", "CareOrganization", "Organization");
			addRelationKeyed(key, "genls", "Hospital", "CareOrganization");
			addRelationKeyed(key, "genls", "Hospital", "TemporaryResidence");
			addRelationKeyed(key, "genls", "MedicalClinic", "CareOrganization");
			addRelationKeyed(key, "genls", "School", "EducationalOrganization");
			addRelationKeyed(key, "genls", "HighSchool", "School");
			addRelationKeyed(key, "genls", "PostSecondarySchool", "School");
			addRelationKeyed(key, "genls", "JuniorCollege", "PostSecondarySchool");
			addRelationKeyed(key, "genls", "College", "PostSecondarySchool");
			addRelationKeyed(key, "genls", "University", "PostSecondarySchool");
			addRelationKeyed(key, "subrelation", "student", "member");
			addRelationKeyed(key, "subrelation", "teacher", "member");
			addRelationKeyed(key, "genls", "SportsLeague", "Organization");
			addRelationKeyed(key, "genls", "SportsTeam", "Organization");
			addRelationKeyed(key, "genls", "BaseballTeam", "SportsTeam");
			addRelationKeyed(key, "genls", "StateGovernment", "Government");
			addRelationKeyed(key, "isa", "UnitedStatesCongress",
					"LegislativeOrganization");
			addRelationKeyed(key, "isa", "UnitedStatesDepartmentOfState",
					"GovernmentOrganization");
			addRelationKeyed(key, "isa", "UnitedStatesDepartmentOfInterior",
					"GovernmentOrganization");
			addRelationKeyed(key, "genls", "MilitaryService", "MilitaryOrganization");
			addRelationKeyed(key, "genls", "Army", "MilitaryService");
			addRelationKeyed(key, "genls", "Navy", "MilitaryService");
			addRelationKeyed(key, "genls", "MilitaryUnit", "MilitaryOrganization");
			addRelationKeyed(key, "genls", "InfantryUnit", "MilitaryUnit");
			addRelationKeyed(key, "genls", "MilitaryDivision", "MilitaryUnit");
			addRelationKeyed(key, "genls", "MilitaryBrigade", "MilitaryUnit");
			addRelationKeyed(key, "genls", "MilitaryCompany", "MilitaryUnit");
			addRelationKeyed(key, "genls", "MilitaryRegiment", "MilitaryUnit");
			addRelationKeyed(key, "genls", "MilitarySquad", "MilitaryUnit");
			addRelationKeyed(key, "genls", "Commission", "Organization");
			addRelationKeyed(key, "genls", "ServiceOrganization", "Organization");
			addRelationKeyed(key, "genls", "OrganizationalBoard", "Organization");
			addRelationKeyed(key, "genls", "SecurityUnit", "Organization");
			addRelationKeyed(key, "genls", "UnionOrganization", "Organization");
			addRelationKeyed(key, "genls", "Nest", "CorpuscularObject");
			addRelationKeyed(key, "genls", "Tumor", "AbnormalAnatomicalStructure");
			addRelationKeyed(key, "genls", "CellNucleus", "BodyPart");
			addRelationKeyed(key, "genls", "PlantLeaf", "PlantAnatomicalStructure");
			addRelationKeyed(key, "genls", "PlantLeaf", "Organ");
			addRelationKeyed(key, "genls", "PlantBranch", "PlantAnatomicalStructure");
			addRelationKeyed(key, "genls", "PlantBranch", "BodyPart");
			addRelationKeyed(key, "genls", "PlantRoot", "PlantAnatomicalStructure");
			addRelationKeyed(key, "genls", "PlantRoot", "Organ");
			addRelationKeyed(key, "genls", "Radish", "PlantRoot");
			addRelationKeyed(key, "genls", "Radish", "Food");
			addRelationKeyed(key, "genls", "Flower", "PlantAnatomicalStructure");
			addRelationKeyed(key, "genls", "Flower", "Organ");
			addRelationKeyed(key, "genls", "DateFruit", "FruitOrVegetable");
			addRelationKeyed(key, "genls", "DateFruit", "Food");
			addRelationKeyed(key, "genls", "Avocado", "FruitOrVegetable");
			addRelationKeyed(key, "genls", "Avocado", "Food");
			addRelationKeyed(key, "genls", "RawFood", "Food");
			addRelationKeyed(key, "genls", "PreparedFood", "Food");
			addRelationKeyed(key, "genls", "SoupStock", "PreparedFood");
			addRelationKeyed(key, "genls", "Coffee", "Beverage");
			addRelationKeyed(key, "genls", "Coffee", "PreparedFood");
			addRelationKeyed(key, "genls", "AlcoholicBeverage", "Beverage");
			addRelationKeyed(key, "genls", "DistilledAlcoholicBeverage",
					"AlcoholicBeverage");
			addRelationKeyed(key, "genls", "Whiskey", "DistilledAlcoholicBeverage");
			addRelationKeyed(key, "genls", "LiquorShot", "DistilledAlcoholicBeverage");
			addRelationKeyed(key, "genls", "Beer", "AlcoholicBeverage");
			addRelationKeyed(key, "genls", "Wine", "AlcoholicBeverage");
			addRelationKeyed(key, "genls", "ChickenMeat", "Meat");
			addRelationKeyed(key, "genls", "Hay", "Fodder");
			addRelationKeyed(key, "genls", "Grass", "FloweringPlant");
			addRelationKeyed(key, "genls", "WillowTree", "BotanicalTree");
			addRelationKeyed(key, "genls", "SpinalColumn",
					"AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Skin", "BodyCovering");
			addRelationKeyed(key, "genls", "Skin", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "BronchialDuct", "BodyVessel");
			addRelationKeyed(key, "genls", "BronchialDuct",
					"AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "BloodVessel", "BodyVessel");
			addRelationKeyed(key, "genls", "BloodVessel", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Artery", "BloodVessel");
			addRelationKeyed(key, "genls", "PulmonaryArtery", "Artery");
			addRelationKeyed(key, "genls", "Vein", "BloodVessel");
			addRelationKeyed(key, "relatedInternalConcept", "Vein", "Artery");
			addRelationKeyed(key, "genls", "PulmonaryVein", "Vein");
			addRelationKeyed(key, "genls", "Gland", "Organ");
			addRelationKeyed(key, "genls", "Gland", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "ThyroidGland", "Gland");
			addRelationKeyed(key, "genls", "Lung", "Organ");
			addRelationKeyed(key, "genls", "Lung", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Heart", "Organ");
			addRelationKeyed(key, "genls", "Heart", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Liver", "Organ");
			addRelationKeyed(key, "genls", "Liver", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Kidney", "Organ");
			addRelationKeyed(key, "genls", "Kidney", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Mouth", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Mouth", "BodyPart");
			addRelationKeyed(key, "genls", "Tongue", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Tongue", "BodyPart");
			addRelationKeyed(key, "genls", "Lip", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Lip", "BodyPart");
			addRelationKeyed(key, "genls", "Tooth", "Bone");
			addRelationKeyed(key, "genls", "Skeleton", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Skeleton", "BodyPart");
			addRelationKeyed(key, "genls", "Throat", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Throat", "BodyVessel");
			addRelationKeyed(key, "genls", "Hair", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "NervousSystem", "Organ");
			addRelationKeyed(key, "genls", "NervousSystem",
					"AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Brain", "Organ");
			addRelationKeyed(key, "genls", "Brain", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Stomach", "Organ");
			addRelationKeyed(key, "genls", "Stomach", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Hypothalamus", "BodyPart");
			addRelationKeyed(key, "genls", "Hypothalamus",
					"AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Eye", "Organ");
			addRelationKeyed(key, "genls", "Eye", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Ear", "Organ");
			addRelationKeyed(key, "genls", "Ear", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Nose", "Organ");
			addRelationKeyed(key, "genls", "Nose", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Arm", "Limb");
			addRelationKeyed(key, "genls", "Hand", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Hand", "BodyPart");
			addRelationKeyed(key, "genls", "Finger", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Finger", "BodyPart");
			addRelationKeyed(key, "genls", "Limb", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Limb", "BodyPart");
			addRelationKeyed(key, "genls", "Leg", "Limb");
			addRelationKeyed(key, "genls", "Foot", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Foot", "BodyPart");
			addRelationKeyed(key, "genls", "Toe", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Toe", "BodyPart");
			addRelationKeyed(key, "genls", "Knee", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Knee", "BodyJunction");
			addRelationKeyed(key, "genls", "Elbow", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Elbow", "BodyJunction");
			addRelationKeyed(key, "genls", "Wrist", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Wrist", "BodyJunction");
			addRelationKeyed(key, "genls", "Shoulder", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Shoulder", "BodyPart");
			addRelationKeyed(key, "genls", "Torso", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Torso", "BodyPart");
			addRelationKeyed(key, "genls", "Head", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Head", "BodyPart");
			addRelationKeyed(key, "genls", "Neck", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Neck", "BodyPart");
			addRelationKeyed(key, "genls", "Face", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Face", "BodyPart");
			addRelationKeyed(key, "genls", "Chin", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Chin", "BodyPart");
			addRelationKeyed(key, "genls", "Wing", "Limb");
			addRelationKeyed(key, "genls", "Snake", "Reptile");
			addRelationKeyed(key, "genls", "ConstrictorSnake", "Snake");
			addRelationKeyed(key, "genls", "Anaconda", "ConstrictorSnake");
			addRelationKeyed(key, "genls", "Bee", "Insect");
			addRelationKeyed(key, "genls", "BumbleBee", "Bee");
			addRelationKeyed(key, "genls", "QueenInsect", "Insect");
			addRelationKeyed(key, "genls", "DomesticAnimal", "Animal");
			addRelationKeyed(key, "disjoint", "DomesticAnimal", "Human");
			addRelationKeyed(key, "genls", "Horse", "HoofedMammal");
			addRelationKeyed(key, "genls", "Horse", "DomesticAnimal");
			addRelationKeyed(key, "genls", "Donkey", "HoofedMammal");
			addRelationKeyed(key, "genls", "Donkey", "DomesticAnimal");
			addRelationKeyed(key, "genls", "Mule", "HoofedMammal");
			addRelationKeyed(key, "genls", "Mule", "DomesticAnimal");
			addRelationKeyed(key, "genls", "Sheep", "HoofedMammal");
			addRelationKeyed(key, "genls", "Sheep", "DomesticAnimal");
			addRelationKeyed(key, "genls", "Cow", "HoofedMammal");
			addRelationKeyed(key, "genls", "Cow", "DomesticAnimal");
			addRelationKeyed(key, "genls", "FemaleCow", "Cow");
			addRelationKeyed(key, "genls", "Chicken", "Bird");
			addRelationKeyed(key, "genls", "Chicken", "DomesticAnimal");
			addRelationKeyed(key, "genls", "Hen", "Chicken");
			addRelationKeyed(key, "genls", "Mouse", "Rodent");
			addRelationKeyed(key, "genls", "Rabbit", "Rodent");
			addRelationKeyed(key, "genls", "HumanCorpse", "Human");
			addRelationKeyed(key, "genls", "HumanSlave", "Human");
			addRelationKeyed(key, "genls", "HumanAdult", "Human");
			addRelationKeyed(key, "genls", "HumanYouth", "Human");
			addRelationKeyed(key, "genls", "HumanChild", "HumanYouth");
			addRelationKeyed(key, "genls", "Boy", "HumanChild");
			addRelationKeyed(key, "genls", "Boy", "Man");
			addRelationKeyed(key, "genls", "Girl", "HumanChild");
			addRelationKeyed(key, "genls", "Girl", "Woman");
			addRelationKeyed(key, "genls", "HumanBaby", "HumanChild");
			addRelationKeyed(key, "genls", "Teenager", "HumanYouth");
			addRelationKeyed(key, "genls", "Syllable", "SymbolicString");
			addRelationKeyed(key, "genls", "DigitCharacter", "Character");
			addRelationKeyed(key, "isa", "EnglishLanguage", "NaturalLanguage");
			addRelationKeyed(key, "isa", "speaksLanguage", "BinaryPredicate");
			addRelationKeyed(key, "isa", "grammaticalRelation", "BinaryPredicate");
			addRelationKeyed(key, "subrelation", "sententialSubject",
					"grammaticalRelation");
			addRelationKeyed(key, "subrelation", "sententialObject",
					"grammaticalRelation");
			addRelationKeyed(key, "genls", "Statement", "Sentence");
			addRelationKeyed(key, "genls", "Fact", "Statement");
			addRelationKeyed(key, "genls", "Question", "Sentence");
			addRelationKeyed(key, "genls", "Supposition", "Sentence");
			addRelationKeyed(key, "genls", "Request", "Sentence");
			addRelationKeyed(key, "genls", "Order", "Sentence");
			addRelationKeyed(key, "genls", "MotionPicture", "Text");
			addRelationKeyed(key, "genls", "MotionPictureShot", "MotionPicture");
			addRelationKeyed(key, "genls", "MotionPictureScene", "MotionPicture");
			addRelationKeyed(key, "genls", "SoundRecording", "Text");
			addRelationKeyed(key, "genls", "RecordAlbum", "SoundRecording");
			addRelationKeyed(key, "genls", "AcademicDegree", "Certificate");
			addRelationKeyed(key, "genls", "InsurancePolicy", "Certificate");
			addRelationKeyed(key, "genls", "Label", "Text");
			addRelationKeyed(key, "genls", "Form", "Text");
			addRelationKeyed(key, "genls", "TaxReturn", "Form");
			addRelationKeyed(key, "genls", "Application", "Form");
			addRelationKeyed(key, "genls", "BroadcastProgram", "Series");
			addRelationKeyed(key, "genls", "NewsProgram", "BroadcastProgram");
			addRelationKeyed(key, "genls", "Chapter", "Article");
			addRelationKeyed(key, "subrelation", "titles", "names");
			addRelationKeyed(key, "domainSubclass", "2", "Text");
			addRelationKeyed(key, "genls", "Report", "FactualText");
			addRelationKeyed(key, "genls", "Report", "Article");
			addRelationKeyed(key, "genls", "FinancialText", "Report");
			addRelationKeyed(key, "genls", "FinancialBill", "FinancialText");
			addRelationKeyed(key, "genls", "PerformanceProgram", "FactualText");
			addRelationKeyed(key, "genls", "Newspaper", "Periodical");
			addRelationKeyed(key, "genls", "Magazine", "Periodical");
			addRelationKeyed(key, "isa", "subscriber", "TernaryPredicate");
			addRelationKeyed(key, "genls", "Letter", "FactualText");
			addRelationKeyed(key, "genls", "HistoricalAccount", "FactualText");
			addRelationKeyed(key, "genls", "ReferenceBook", "Book");
			addRelationKeyed(key, "genls", "ReferenceBook", "FactualText");
			addRelationKeyed(key, "genls", "Dictionary", "ReferenceBook");
			addRelationKeyed(key, "genls", "NarrativeText", "Text");
			addRelationKeyed(key, "genls", "ShortStory", "FictionalText");
			addRelationKeyed(key, "genls", "ShortStory", "Article");
			addRelationKeyed(key, "genls", "Novel", "FictionalText");
			addRelationKeyed(key, "genls", "Novel", "Book");
			addRelationKeyed(key, "genls", "MysteryStory", "FictionalText");
			addRelationKeyed(key, "genls", "DramaticPlay", "FictionalText");
			addRelationKeyed(key, "genls", "Opera", "DramaticPlay");
			addRelationKeyed(key, "genls", "MusicalComposition", "Text");
			addRelationKeyed(key, "genls", "Lyrics", "MusicalComposition");
			addRelationKeyed(key, "genls", "PartyPlatform", "FactualText");
			addRelationKeyed(key, "genls", "HolyBible", "Book");
			addRelationKeyed(key, "genls", "Blueprint", "Icon");
			addRelationKeyed(key, "genls", "GraphDiagram", "Icon");
			addRelationKeyed(key, "genls", "Flag", "Icon");
			addRelationKeyed(key, "genls", "ArrowIcon", "Icon");
			addRelationKeyed(key, "genls", "Photograph", "Icon");
			addRelationKeyed(key, "genls", "PaintedPicture", "ArtWork");
			addRelationKeyed(key, "genls", "WaterColor", "PaintedPicture");
			addRelationKeyed(key, "genls", "Sketch", "ArtWork");
			addRelationKeyed(key, "genls", "Sculpture", "ArtWork");
			addRelationKeyed(key, "genls", "Collage", "ArtWork");
			addRelationKeyed(key, "genls", "OutdoorClothing", "Clothing");
			addRelationKeyed(key, "genls", "Hat", "OutdoorClothing");
			addRelationKeyed(key, "genls", "Coat", "OutdoorClothing");
			addRelationKeyed(key, "genls", "Shoe", "Clothing");
			addRelationKeyed(key, "genls", "Shirt", "Clothing");
			addRelationKeyed(key, "genls", "Dress", "Clothing");
			addRelationKeyed(key, "genls", "ClothingSuit", "Collection");
			addRelationKeyed(key, "genls", "Leather", "Fabric");
			addRelationKeyed(key, "genls", "Pocket", "Fabric");
			addRelationKeyed(key, "genls", "Blanket", "Fabric");
			addRelationKeyed(key, "genls", "Door", "Artifact");
			addRelationKeyed(key, "genls", "DisplayBoard", "Artifact");
			addRelationKeyed(key, "genls", "BoardOrBlock", "Artifact");
			addRelationKeyed(key, "genls", "Pillow", "Artifact");
			addRelationKeyed(key, "genls", "GameArtifact", "Artifact");
			addRelationKeyed(key, "genls", "GameBoard", "GameArtifact");
			addRelationKeyed(key, "genls", "GamePiece", "GameArtifact");
			addRelationKeyed(key, "genls", "GameDie", "GamePiece");
			addRelationKeyed(key, "genls", "Ball", "GamePiece");
			addRelationKeyed(key, "genls", "ShotBall", "Ball");
			addRelationKeyed(key, "genls", "GameGoal", "GameArtifact");
			addRelationKeyed(key, "genls", "Wheel", "Artifact");
			addRelationKeyed(key, "genls", "Tire", "Artifact");
			addRelationKeyed(key, "genls", "Lumber", "Artifact");
			addRelationKeyed(key, "genls", "Paper", "Artifact");
			addRelationKeyed(key, "genls", "Page", "Artifact");
			addRelationKeyed(key, "genls", "Wire", "Artifact");
			addRelationKeyed(key, "genls", "WireLine", "Wire");
			addRelationKeyed(key, "genls", "WireLine", "EngineeringComponent");
			addRelationKeyed(key, "genls", "Plug", "Artifact");
			addRelationKeyed(key, "genls", "CigarOrCigarette", "Artifact");
			addRelationKeyed(key, "genls", "Pottery", "Artifact");
			addRelationKeyed(key, "genls", "Shelf", "Artifact");
			addRelationKeyed(key, "genls", "Furniture", "Artifact");
			addRelationKeyed(key, "disjoint", "Furniture", "Device");
			addRelationKeyed(key, "genls", "Seat", "Furniture");
			addRelationKeyed(key, "genls", "Chair", "Seat");
			addRelationKeyed(key, "genls", "AuditoriumSeat", "Seat");
			addRelationKeyed(key, "genls", "Bed", "Furniture");
			addRelationKeyed(key, "genls", "Table", "Furniture");
			addRelationKeyed(key, "genls", "Desk", "Table");
			addRelationKeyed(key, "genls", "Steps", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Doorway", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Window", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Wall", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Floor", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Roof", "StationaryArtifact");
			addRelationKeyed(key, "genls", "BuildingLevel", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Basement", "BuildingLevel");
			addRelationKeyed(key, "genls", "Garage", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Porch", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Sidewalk", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Fence", "StationaryArtifact");
			addRelationKeyed(key, "genls", "SportsGround", "StationaryArtifact");
			addRelationKeyed(key, "genls", "IndustrialPlant", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Laboratory", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Farm", "StationaryArtifact");
			addRelationKeyed(key, "genls", "PerformanceStage", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Steeple", "StationaryArtifact");
			addRelationKeyed(key, "genls", "ArtStudio", "StationaryArtifact");
			addRelationKeyed(key, "genls", "MineOrWell", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Chimney", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Chimney", "Device");
			addRelationKeyed(key, "genls", "PlaceOfCommerce", "StationaryArtifact");
			addRelationKeyed(key, "genls", "CommercialBuilding", "Building");
			addRelationKeyed(key, "genls", "CommercialBuilding", "PlaceOfCommerce");
			addRelationKeyed(key, "genls", "CommercialUnit", "PlaceOfCommerce");
			addRelationKeyed(key, "genls", "Auditorium", "Building");
			addRelationKeyed(key, "genls", "ReligiousBuilding", "Building");
			addRelationKeyed(key, "genls", "Barn", "Building");
			addRelationKeyed(key, "disjoint", "Barn", "ResidentialBuilding");
			addRelationKeyed(key, "genls", "ExecutiveResidence", "PermanentResidence");
			addRelationKeyed(key, "genls", "ApartmentUnit", "SingleFamilyResidence");
			addRelationKeyed(key, "genls", "CondominiumUnit", "SingleFamilyResidence");
			addRelationKeyed(key, "genls", "House", "ResidentialBuilding");
			addRelationKeyed(key, "genls", "House", "SingleFamilyResidence");
			addRelationKeyed(key, "genls", "ApartmentBuilding", "ResidentialBuilding");
			addRelationKeyed(key, "disjoint", "ApartmentBuilding",
					"SingleFamilyResidence");
			addRelationKeyed(key, "genls", "CondominiumBuilding",
					"ResidentialBuilding");
			addRelationKeyed(key, "disjoint", "CondominiumBuilding",
					"SingleFamilyResidence");
			addRelationKeyed(key, "genls", "Kitchen", "Room");
			addRelationKeyed(key, "genls", "Bedroom", "Room");
			addRelationKeyed(key, "genls", "CourtRoom", "Room");
			addRelationKeyed(key, "genls", "MobileResidence", "Artifact");
			addRelationKeyed(key, "disjoint", "MobileResidence", "Residence");
			addRelationKeyed(key, "genls", "Camp", "MobileResidence");
			addRelationKeyed(key, "genls", "Tent", "MobileResidence");
			addRelationKeyed(key, "genls", "AnimalResidence", "Artifact");
			addRelationKeyed(key, "genls", "ExplosiveDevice", "Device");
			addRelationKeyed(key, "genls", "Camera", "Device");
			addRelationKeyed(key, "genls", "Filter", "Device");
			addRelationKeyed(key, "genls", "FileDevice", "Device");
			addRelationKeyed(key, "genls", "WritingDevice", "Device");
			addRelationKeyed(key, "genls", "ElectricDevice", "Device");
			addRelationKeyed(key, "genls", "Radar", "ElectricDevice");
			addRelationKeyed(key, "genls", "SecurityDevice", "Device");
			addRelationKeyed(key, "genls", "Lock", "SecurityDevice");
			addRelationKeyed(key, "genls", "Key", "SecurityDevice");
			addRelationKeyed(key, "genls", "SecurityAlarm", "SecurityDevice");
			addRelationKeyed(key, "genls", "SecurityAlarm", "ElectricDevice");
			addRelationKeyed(key, "genls", "Clock", "MeasuringDevice");
			addRelationKeyed(key, "genls", "WatchClock", "Clock");
			addRelationKeyed(key, "genls", "Thermometer", "MeasuringDevice");
			addRelationKeyed(key, "genls", "Screw", "AttachingDevice");
			addRelationKeyed(key, "genls", "Tape", "AttachingDevice");
			addRelationKeyed(key, "genls", "Holder", "Device");
			addRelationKeyed(key, "genls", "Dish", "Holder");
			addRelationKeyed(key, "genls", "Saddle", "Holder");
			addRelationKeyed(key, "genls", "Tray", "Holder");
			addRelationKeyed(key, "genls", "Container", "Holder");
			addRelationKeyed(key, "genls", "Box", "Container");
			addRelationKeyed(key, "genls", "TravelContainer", "Container");
			addRelationKeyed(key, "genls", "FluidContainer", "Container");
			addRelationKeyed(key, "genls", "Bottle", "FluidContainer");
			addRelationKeyed(key, "genls", "Cup", "FluidContainer");
			addRelationKeyed(key, "genls", "Envelope", "Container");
			addRelationKeyed(key, "genls", "ProjectileShell", "Container");
			addRelationKeyed(key, "genls", "Antenna", "CommunicationDevice");
			addRelationKeyed(key, "genls", "Telephone", "ElectricDevice");
			addRelationKeyed(key, "genls", "Telephone", "CommunicationDevice");
			addRelationKeyed(key, "subrelation", "telephoneNumber",
					"uniqueIdentifier");
			addRelationKeyed(key, "relatedInternalConcept", "telephoneNumber",
					"address");
			addRelationKeyed(key, "genls", "LightFixture", "Device");
			addRelationKeyed(key, "genls", "BirthControlDevice", "Device");
			addRelationKeyed(key, "genls", "Transducer", "Device");
			addRelationKeyed(key, "genls", "Aerator", "Device");
			addRelationKeyed(key, "genls", "Piano", "MusicalInstrument");
			addRelationKeyed(key, "genls", "Bell", "MusicalInstrument");
			addRelationKeyed(key, "genls", "Projectile", "Weapon");
			addRelationKeyed(key, "genls", "Gun", "Weapon");
			addRelationKeyed(key, "genls", "Sword", "Weapon");
			addRelationKeyed(key, "genls", "ArtilleryGun", "Gun");
			addRelationKeyed(key, "genls", "Firearm", "Gun");
			addRelationKeyed(key, "genls", "Rifle", "Firearm");
			addRelationKeyed(key, "genls", "Pistol", "Firearm");
			addRelationKeyed(key, "genls", "GunStock", "EngineeringComponent");
			addRelationKeyed(key, "genls", "Manifold", "EngineeringComponent");
			addRelationKeyed(key, "genls", "SwitchDevice", "EngineeringComponent");
			addRelationKeyed(key, "genls", "Helicopter", "Aircraft");
			addRelationKeyed(key, "genls", "Bomber", "Aircraft");
			addRelationKeyed(key, "genls", "StageCoach", "Wagon");
			addRelationKeyed(key, "genls", "Spacecraft", "Vehicle");
			addRelationKeyed(key, "genls", "Missile", "Spacecraft");
			addRelationKeyed(key, "genls", "Powder", "Substance");
			addRelationKeyed(key, "genls", "Fallout", "Powder");
			addRelationKeyed(key, "genls", "LiquidBodySubstance", "BodySubstance");
			addRelationKeyed(key, "genls", "Serum", "LiquidBodySubstance");
			addRelationKeyed(key, "disjoint", "Serum", "Blood");
			addRelationKeyed(key, "genls", "Milk", "LiquidBodySubstance");
			addRelationKeyed(key, "genls", "Milk", "Beverage");
			addRelationKeyed(key, "genls", "Antibody", "Protein");
			addRelationKeyed(key, "genls", "Enzyme", "Protein");
			addRelationKeyed(key, "genls", "Hormone", "BodySubstance");
			addRelationKeyed(key, "genls", "Hormone", "BiologicallyActiveSubstance");
			addRelationKeyed(key, "genls", "ThyroidHormone", "Hormone");
			addRelationKeyed(key, "genls", "Wood", "Tissue");
			addRelationKeyed(key, "genls", "Wood", "PlantSubstance");
			addRelationKeyed(key, "genls", "Opium", "BiologicallyActiveSubstance");
			addRelationKeyed(key, "genls", "Opium", "PlantSubstance");
			addRelationKeyed(key, "genls", "Antigen", "BiologicallyActiveSubstance");
			addRelationKeyed(key, "genls", "Sweat", "AnimalSubstance");
			addRelationKeyed(key, "genls", "Honey", "AnimalSubstance");
			addRelationKeyed(key, "genls", "Honey", "Food");
			addRelationKeyed(key, "isa", "protonNumber", "BinaryPredicate");
			addRelationKeyed(key, "isa", "protonNumber", "SingleValuedRelation");
			addRelationKeyed(key, "isa", "protonNumber", "TotalValuedRelation");
			addRelationKeyed(key, "isa", "electronNumber", "BinaryPredicate");
			addRelationKeyed(key, "isa", "electronNumber", "SingleValuedRelation");
			addRelationKeyed(key, "isa", "electronNumber", "TotalValuedRelation");
			addRelationKeyed(key, "genls", "AtomicGroup", "CompoundSubstance");
			addRelationKeyed(key, "genls", "OrganicCompound", "CompoundSubstance");
			addRelationKeyed(key, "genls", "Alcohol", "OrganicCompound");
			addRelationKeyed(key, "genls", "ChemicalAcid", "CompoundSubstance");
			addRelationKeyed(key, "genls", "ChemicalBase", "CompoundSubstance");
			addRelationKeyed(key, "genls", "ChemicalSalt", "CompoundSubstance");
			addRelationKeyed(key, "genls", "SodiumChloride", "CompoundSubstance");
			addRelationKeyed(key, "genls", "Ice", "Water");
			addRelationKeyed(key, "genls", "Glass", "Mixture");
			addRelationKeyed(key, "genls", "MetallicAlloy", "Mixture");
			addRelationKeyed(key, "genls", "Steel", "MetallicAlloy");
			addRelationKeyed(key, "genls", "Brass", "MetallicAlloy");
			addRelationKeyed(key, "genls", "Detergent", "Mixture");
			addRelationKeyed(key, "genls", "Fog", "WaterCloud");
			addRelationKeyed(key, "genls", "SalineSolution", "Solution");
			addRelationKeyed(key, "genls", "Oil", "Solution");
			addRelationKeyed(key, "genls", "Paint", "Solution");
			addRelationKeyed(key, "genls", "Glue", "Solution");
			addRelationKeyed(key, "isa", "conjugate", "BinaryPredicate");
			addRelationKeyed(key, "isa", "conjugate", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "conjugate", "SymmetricRelation");
			addRelationKeyed(key, "isa", "conjugate", "TransitiveRelation");
			addRelationKeyed(key, "genls", "Broadening", "Increasing");
			addRelationKeyed(key, "genls", "Narrowing", "Decreasing");
			addRelationKeyed(key, "genls", "CriminalAction", "IntentionalProcess");
			addRelationKeyed(key, "genls", "SocialParty", "Meeting");
			addRelationKeyed(key, "genls", "SocialParty", "RecreationOrExercise");
			addRelationKeyed(key, "genls", "FormalMeeting", "Meeting");
			addRelationKeyed(key, "disjoint", "FormalMeeting", "SocialParty");
			addRelationKeyed(key, "genls", "DramaticActing", "Pretending");
			addRelationKeyed(key, "genls", "Resolution", "Deciding");
			addRelationKeyed(key, "genls", "LegalAward", "LegalDecision");
			addRelationKeyed(key, "genls", "LegalConviction", "LegalDecision");
			addRelationKeyed(key, "genls", "LegalAquittal", "LegalDecision");
			addRelationKeyed(key, "genls", "GameCall", "Deciding");
			addRelationKeyed(key, "genls", "GameCall", "Declaring");
			addRelationKeyed(key, "genls", "Smoking", "RecreationOrExercise");
			addRelationKeyed(key, "genls", "Digging", "IntentionalProcess");
			addRelationKeyed(key, "genls", "Digging", "SurfaceChange");
			addRelationKeyed(key, "genls", "Tilling", "Digging");
			addRelationKeyed(key, "genls", "Drilling", "IntentionalProcess");
			addRelationKeyed(key, "genls", "Drilling", "SurfaceChange");
			addRelationKeyed(key, "genls", "Copying", "Making");
			addRelationKeyed(key, "genls", "Sharing", "ChangeOfPossession");
			addRelationKeyed(key, "genls", "Stealing", "UnilateralGetting");
			addRelationKeyed(key, "genls", "Stealing", "CriminalAction");
			addRelationKeyed(key, "genls", "Inheriting", "UnilateralGetting");
			addRelationKeyed(key, "genls", "Renting", "FinancialTransaction");
			addRelationKeyed(key, "genls", "Renting", "Borrowing");
			addRelationKeyed(key, "genls", "Working", "FinancialTransaction");
			addRelationKeyed(key, "isa", "monetaryWage", "QuaternaryPredicate");
			addRelationKeyed(key, "genls", "Farming", "Working");
			addRelationKeyed(key, "genls", "Serving", "Working");
			addRelationKeyed(key, "genls", "Sales", "Working");
			addRelationKeyed(key, "isa", "hasOccupation", "BinaryPredicate");
			addRelationKeyed(key, "genls", "Vacationing", "RecreationOrExercise");
			addRelationKeyed(key, "genls", "OfferingForSale", "Offering");
			addRelationKeyed(key, "genls", "BargainSale", "Offering");
			addRelationKeyed(key, "genls", "Biting", "Grabbing");
			addRelationKeyed(key, "genls", "Spitting", "Impelling");
			addRelationKeyed(key, "genls", "Kicking", "Impelling");
			addRelationKeyed(key, "genls", "Throwing", "Impelling");
			addRelationKeyed(key, "genls", "Throwing", "BodyMotion");
			addRelationKeyed(key, "genls", "Pitching", "Throwing");
			addRelationKeyed(key, "genls", "Pitching", "GameShot");
			addRelationKeyed(key, "subrelation", "contestParticipant", "agent");
			addRelationKeyed(key, "isa", "contestParticipant", "TotalValuedRelation");
			addRelationKeyed(key, "subrelation", "contestAward", "patient");
			addRelationKeyed(key, "genls", "CivilWar", "War");
			addRelationKeyed(key, "genls", "LegalCharge", "LegalAction");
			addRelationKeyed(key, "genls", "Debating", "Contest");
			addRelationKeyed(key, "genls", "Debating", "LinguisticCommunication");
			addRelationKeyed(key, "genls", "Negotiating", "Contest");
			addRelationKeyed(key, "genls", "Negotiating", "LinguisticCommunication");
			addRelationKeyed(key, "genls", "BusinessCompetition", "Contest");
			addRelationKeyed(key, "genls", "TeamSport", "Sport");
			addRelationKeyed(key, "genls", "Boxing", "ViolentContest");
			addRelationKeyed(key, "genls", "Boxing", "Sport");
			addRelationKeyed(key, "genls", "Golf", "Sport");
			addRelationKeyed(key, "genls", "Gymnastics", "Sport");
			addRelationKeyed(key, "genls", "Football", "Sport");
			addRelationKeyed(key, "genls", "Baseball", "Sport");
			addRelationKeyed(key, "genls", "BaseballInning", "Maneuver");
			addRelationKeyed(key, "genls", "GameShot", "Impelling");
			addRelationKeyed(key, "genls", "GameShot", "Maneuver");
			addRelationKeyed(key, "genls", "Score", "GameShot");
			addRelationKeyed(key, "genls", "BaseballManeuver", "Maneuver");
			addRelationKeyed(key, "genls", "BaseballWalk", "BaseballManeuver");
			addRelationKeyed(key, "genls", "BaseballHit", "GameShot");
			addRelationKeyed(key, "genls", "BaseballHit", "BaseballManeuver");
			addRelationKeyed(key, "genls", "BaseballRun", "BaseballHit");
			addRelationKeyed(key, "genls", "BaseballRun", "Score");
			addRelationKeyed(key, "genls", "HomeRun", "BaseballRun");
			addRelationKeyed(key, "disjoint", "Untying", "Tying");
			addRelationKeyed(key, "genls", "RelievingPain", "TherapeuticProcess");
			addRelationKeyed(key, "genls", "Diluting", "Combining");
			addRelationKeyed(key, "genls", "Aerating", "Combining");
			addRelationKeyed(key, "genls", "Dialysis", "Separating");
			addRelationKeyed(key, "genls", "Oxidation", "ChemicalProcess");
			addRelationKeyed(key, "genls", "Stretching", "Motion");
			addRelationKeyed(key, "genls", "Rotating", "Motion");
			addRelationKeyed(key, "genls", "Tremor", "Rotating");
			addRelationKeyed(key, "genls", "Reversing", "Motion");
			addRelationKeyed(key, "genls", "Dripping", "LiquidMotion");
			addRelationKeyed(key, "genls", "Pouring", "LiquidMotion");
			addRelationKeyed(key, "genls", "Pouring", "Transfer");
			addRelationKeyed(key, "genls", "WaterMotion", "LiquidMotion");
			addRelationKeyed(key, "genls", "WaterWave", "LiquidMotion");
			addRelationKeyed(key, "genls", "Flying", "Translocation");
			addRelationKeyed(key, "genls", "TakingOff", "Translocation");
			addRelationKeyed(key, "genls", "Landing", "Translocation");
			addRelationKeyed(key, "genls", "Returning", "Translocation");
			addRelationKeyed(key, "genls", "Escaping", "Translocation");
			addRelationKeyed(key, "genls", "Leaving", "Translocation");
			addRelationKeyed(key, "genls", "Arriving", "Translocation");
			addRelationKeyed(key, "genls", "MotionUpward", "Translocation");
			addRelationKeyed(key, "disjoint", "MotionUpward", "MotionDownward");
			addRelationKeyed(key, "genls", "MotionDownward", "Translocation");
			addRelationKeyed(key, "genls", "Accelerating", "Translocation");
			addRelationKeyed(key, "genls", "Accelerating", "Increasing");
			addRelationKeyed(key, "genls", "Decelerating", "Translocation");
			addRelationKeyed(key, "genls", "Decelerating", "Decreasing");
			addRelationKeyed(key, "genls", "Stepping", "BodyMotion");
			addRelationKeyed(key, "genls", "Chewing", "BodyMotion");
			addRelationKeyed(key, "genls", "LyingDown", "BodyMotion");
			addRelationKeyed(key, "genls", "SittingDown", "BodyMotion");
			addRelationKeyed(key, "genls", "StandingUp", "BodyMotion");
			addRelationKeyed(key, "genls", "Trembling", "BodyMotion");
			addRelationKeyed(key, "genls", "Trembling", "Tremor");
			addRelationKeyed(key, "genls", "Swimming", "BodyMotion");
			addRelationKeyed(key, "genls", "Dancing", "BodyMotion");
			addRelationKeyed(key, "genls", "Inclining", "BodyMotion");
			addRelationKeyed(key, "genls", "Inclining", "MotionDownward");
			addRelationKeyed(key, "genls", "Bowing", "Inclining");
			addRelationKeyed(key, "genls", "Bowing", "Gesture");
			addRelationKeyed(key, "genls", "Ducking", "Inclining");
			addRelationKeyed(key, "genls", "Ducking", "IntentionalProcess");
			addRelationKeyed(key, "genls", "OpeningEyes", "BodyMotion");
			addRelationKeyed(key, "disjoint", "OpeningEyes", "ClosingEyes");
			addRelationKeyed(key, "genls", "ClosingEyes", "BodyMotion");
			addRelationKeyed(key, "genls", "Winking", "ClosingEyes");
			addRelationKeyed(key, "genls", "Winking", "Gesture");
			addRelationKeyed(key, "genls", "Shrugging", "Gesture");
			addRelationKeyed(key, "genls", "Mailing", "Transfer");
			addRelationKeyed(key, "genls", "MovingResidence", "Transfer");
			addRelationKeyed(key, "genls", "Kissing", "Touching");
			addRelationKeyed(key, "genls", "Catching", "Touching");
			addRelationKeyed(key, "genls", "Catching", "Maneuver");
			addRelationKeyed(key, "genls", "Pulling", "Transportation");
			addRelationKeyed(key, "genls", "LandTransportation", "Transportation");
			addRelationKeyed(key, "genls", "WaterTransportation", "Transportation");
			addRelationKeyed(key, "genls", "AirTransportation", "Transportation");
			addRelationKeyed(key, "genls", "SpaceTransportation", "Transportation");
			addRelationKeyed(key, "genls", "Washing", "Removing");
			addRelationKeyed(key, "genls", "HairRemoval", "Removing");
			addRelationKeyed(key, "genls", "Installing", "Putting");
			addRelationKeyed(key, "genls", "Explosion", "Radiating");
			addRelationKeyed(key, "genls", "ReflectingLight", "RadiatingLight");
			addRelationKeyed(key, "genls", "Sunlight", "RadiatingLight");
			addRelationKeyed(key, "genls", "RadiatingSoundUltrasonic",
					"RadiatingSound");
			addRelationKeyed(key, "genls", "Magnetism", "RadiatingElectromagnetic");
			addRelationKeyed(key, "genls", "RadioEmission",
					"RadiatingElectromagnetic");
			addRelationKeyed(key, "genls", "Broadcasting", "Disseminating");
			addRelationKeyed(key, "genls", "Broadcasting", "RadioEmission");
			addRelationKeyed(key, "genls", "RadioBroadcasting", "Broadcasting");
			addRelationKeyed(key, "genls", "TelevisionBroadcasting", "Broadcasting");
			addRelationKeyed(key, "genls", "Advertising", "Disseminating");
			addRelationKeyed(key, "genls", "DramaticDirecting", "Guiding");
			addRelationKeyed(key, "genls", "OrchestralConducting", "Guiding");
			addRelationKeyed(key, "genls", "Seating", "Guiding");
			addRelationKeyed(key, "genls", "Murder", "Killing");
			addRelationKeyed(key, "genls", "Murder", "CriminalAction");
			addRelationKeyed(key, "genls", "Hanging", "Killing");
			addRelationKeyed(key, "genls", "InstrumentalMusic", "Music");
			addRelationKeyed(key, "genls", "MonophonicMusic", "Music");
			addRelationKeyed(key, "genls", "PolyphonicMusic", "Music");
			addRelationKeyed(key, "genls", "Reminding", "Requesting");
			addRelationKeyed(key, "genls", "Threatening", "Committing");
			addRelationKeyed(key, "genls", "ClosingContract", "Committing");
			addRelationKeyed(key, "genls", "SigningADocument", "Committing");
			addRelationKeyed(key, "genls", "Registering", "Stating");
			addRelationKeyed(key, "genls", "Registering", "PoliticalProcess");
			addRelationKeyed(key, "genls", "Answering", "Stating");
			addRelationKeyed(key, "genls", "Arguing", "Stating");
			addRelationKeyed(key, "genls", "TellingALie", "Stating");
			addRelationKeyed(key, "genls", "Testifying", "Stating");
			addRelationKeyed(key, "genls", "Founding", "Declaring");
			addRelationKeyed(key, "genls", "Founding", "OrganizationalProcess");
			addRelationKeyed(key, "genls", "BeginningOperations",
					"OrganizationalProcess");
			addRelationKeyed(key, "disjoint", "BeginningOperations",
					"CeasingOperations");
			addRelationKeyed(key, "genls", "CeasingOperations",
					"OrganizationalProcess");
			addRelationKeyed(key, "genls", "LaborStriking", "OrganizationalProcess");
			addRelationKeyed(key, "genls", "Resigning", "TerminatingEmployment");
			addRelationKeyed(key, "genls", "Retiring", "Resigning");
			addRelationKeyed(key, "genls", "PassingABill", "PoliticalProcess");
			addRelationKeyed(key, "genls", "PassingABill", "Declaring");
			addRelationKeyed(key, "genls", "Painting", "Covering");
			addRelationKeyed(key, "genls", "Painting", "Coloring");
			addRelationKeyed(key, "genls", "ArtPainting", "ContentDevelopment");
			addRelationKeyed(key, "genls", "ArtPainting", "Painting");
			addRelationKeyed(key, "genls", "Drawing", "ContentDevelopment");
			addRelationKeyed(key, "genls", "Drawing", "SurfaceChange");
			addRelationKeyed(key, "genls", "Tracing", "ContentDevelopment");
			addRelationKeyed(key, "genls", "Tracing", "SurfaceChange");
			addRelationKeyed(key, "genls", "Photographing", "ContentDevelopment");
			addRelationKeyed(key, "genls", "Composing", "ContentDevelopment");
			addRelationKeyed(key, "genls", "Indicating", "Communication");
			addRelationKeyed(key, "genls", "Indicating", "BodyMotion");
			addRelationKeyed(key, "genls", "ExpressingApproval", "Expressing");
			addRelationKeyed(key, "genls", "ExpressingDisapproval", "Expressing");
			addRelationKeyed(key, "genls", "Greeting", "Expressing");
			addRelationKeyed(key, "genls", "Thanking", "ExpressingInLanguage");
			addRelationKeyed(key, "genls", "Thanking", "ExpressingApproval");
			addRelationKeyed(key, "genls", "Regretting", "ExpressingInLanguage");
			addRelationKeyed(key, "genls", "Regretting", "ExpressingDisapproval");
			addRelationKeyed(key, "genls", "FacialExpression", "Gesture");
			addRelationKeyed(key, "genls", "Smiling", "FacialExpression");
			addRelationKeyed(key, "genls", "Frowning", "FacialExpression");
			addRelationKeyed(key, "genls", "Laughing", "Vocalizing");
			addRelationKeyed(key, "genls", "Laughing", "FacialExpression");
			addRelationKeyed(key, "genls", "Weeping", "FacialExpression");
			addRelationKeyed(key, "genls", "Nodding", "Gesture");
			addRelationKeyed(key, "genls", "HandGesture", "Gesture");
			addRelationKeyed(key, "genls", "Waving", "HandGesture");
			addRelationKeyed(key, "genls", "Clapping", "HandGesture");
			addRelationKeyed(key, "genls", "Clapping", "RadiatingSound");
			addRelationKeyed(key, "genls", "WrittenCommunication",
					"LinguisticCommunication");
			addRelationKeyed(key, "genls", "Telephoning", "Speaking");
			addRelationKeyed(key, "genls", "Punishing", "RegulatoryProcess");
			addRelationKeyed(key, "genls", "ReligiousService", "ReligiousProcess");
			addRelationKeyed(key, "genls", "ReligiousService", "Demonstrating");
			addRelationKeyed(key, "genls", "ChristianService", "ReligiousService");
			addRelationKeyed(key, "genls", "Praying", "ReligiousProcess");
			addRelationKeyed(key, "genls", "Praying", "Requesting");
			addRelationKeyed(key, "genls", "Performance", "Demonstrating");
			addRelationKeyed(key, "genls", "PerformanceAct", "Performance");
			addRelationKeyed(key, "genls", "Lecture", "Demonstrating");
			addRelationKeyed(key, "genls", "Lecture", "Speaking");
			addRelationKeyed(key, "genls", "Sermon", "Lecture");
			addRelationKeyed(key, "genls", "Bleeding", "AutonomicProcess");
			addRelationKeyed(key, "genls", "Blushing", "AutonomicProcess");
			addRelationKeyed(key, "genls", "Imagining", "PsychologicalProcess");
			addRelationKeyed(key, "genls", "Dreaming", "Imagining");
			addRelationKeyed(key, "disjoint", "Dreaming", "IntentionalProcess");
			addRelationKeyed(key, "genls", "Frightening", "PsychologicalProcess");
			addRelationKeyed(key, "genls", "FallingAsleep", "PsychologicalProcess");
			addRelationKeyed(key, "disjoint", "FallingAsleep", "WakingUp");
			addRelationKeyed(key, "genls", "WakingUp", "PsychologicalProcess");
			addRelationKeyed(key, "genls", "Poisoning", "Injuring");
			addRelationKeyed(key, "genls", "LegalOpinion", "Argument");
			addRelationKeyed(key, "genls", "EducationalProgram", "Plan");
			addRelationKeyed(key, "genls", "EducationalCourse", "EducationalProgram");
			addRelationKeyed(key, "genls", "SportsPlay", "Plan");
			addRelationKeyed(key, "genls", "NightTime", "TimeInterval");
			addRelationKeyed(key, "genls", "DayTime", "TimeInterval");
			addRelationKeyed(key, "genls", "Morning", "DayTime");
			addRelationKeyed(key, "genls", "Afternoon", "DayTime");
			addRelationKeyed(key, "genls", "Sunrise", "TimeInterval");
			addRelationKeyed(key, "genls", "Sunset", "TimeInterval");
			addRelationKeyed(key, "genls", "Weekend", "TimeInterval");
			addRelationKeyed(key, "genls", "SeasonOfYear", "TimeInterval");
			addRelationKeyed(key, "genls", "WinterSeason", "SeasonOfYear");
			addRelationKeyed(key, "genls", "SpringSeason", "SeasonOfYear");
			addRelationKeyed(key, "genls", "SummerSeason", "SeasonOfYear");
			addRelationKeyed(key, "genls", "FallSeason", "SeasonOfYear");
			addRelationKeyed(key, "isa", "ChemicalEquilibrium", "InternalAttribute");
			addRelationKeyed(key, "genls", "BreakabilityAttribute",
					"InternalAttribute");
			addRelationKeyed(key, "isa", "Fragile", "BreakabilityAttribute");
			addRelationKeyed(key, "isa", "Unbreakable", "BreakabilityAttribute");
			addRelationKeyed(key, "contraryAttribute", "Unbreakable", "Fragile");
			addRelationKeyed(key, "genls", "DeviceAttribute", "ObjectiveNorm");
			addRelationKeyed(key, "isa", "Functioning", "DeviceAttribute");
			addRelationKeyed(key, "contraryAttribute", "Functioning",
					"Malfunctioning");
			addRelationKeyed(key, "isa", "Malfunctioning", "DeviceAttribute");
			addRelationKeyed(key, "genls", "SecondaryColor", "ColorAttribute");
			addRelationKeyed(key, "isa", "Gray", "SecondaryColor");
			addRelationKeyed(key, "isa", "Pink", "SecondaryColor");
			addRelationKeyed(key, "isa", "Purple", "SecondaryColor");
			addRelationKeyed(key, "isa", "Brown", "SecondaryColor");
			addRelationKeyed(key, "isa", "Green", "SecondaryColor");
			addRelationKeyed(key, "isa", "LineFormation", "ShapeAttribute");
			addRelationKeyed(key, "isa", "Stressed", "SoundAttribute");
			addRelationKeyed(key, "genls", "Address", "RelationalAttribute");
			addRelationKeyed(key, "isa", "address", "BinaryPredicate");
			addRelationKeyed(key, "subrelation", "postalCode", "uniqueIdentifier");
			addRelationKeyed(key, "subrelation", "postalBoxNumber",
					"uniqueIdentifier");
			addRelationKeyed(key, "subrelation", "cityAddress", "address");
			addRelationKeyed(key, "subrelation", "streetAddress", "address");
			addRelationKeyed(key, "subrelation", "streetNumber", "address");
			addRelationKeyed(key, "subrelation", "unitNumber", "address");
			addRelationKeyed(key, "isa", "LegislativeBill", "DeonticAttribute");
			addRelationKeyed(key, "subAttribute", "InternationalLaw", "Law");
			addRelationKeyed(key, "isa", "Lost", "ContestAttribute");
			addRelationKeyed(key, "contraryAttribute", "Lost", "Won");
			addRelationKeyed(key, "isa", "Won", "ContestAttribute");
			addRelationKeyed(key, "genls", "GameAttribute", "ContestAttribute");
			addRelationKeyed(key, "genls", "SportAttribute", "GameAttribute");
			addRelationKeyed(key, "isa", "BaseballStrike", "SportAttribute");
			addRelationKeyed(key, "isa", "PacificTimeZone", "TimeZone");
			addRelationKeyed(key, "isa", "MountainTimeZone", "TimeZone");
			addRelationKeyed(key, "isa", "CentralTimeZone", "TimeZone");
			addRelationKeyed(key, "isa", "EasternTimeZone", "TimeZone");
			addRelationKeyed(key, "isa", "Upstairs", "PositionalAttribute");
			addRelationKeyed(key, "isa", "Downstairs", "PositionalAttribute");
			addRelationKeyed(key, "genls", "PoliticoEconomicAttribute",
					"RelationalAttribute");
			addRelationKeyed(key, "isa", "Blind", "BiologicalAttribute");
			addRelationKeyed(key, "isa", "Hungry", "BiologicalAttribute");
			addRelationKeyed(key, "isa", "Drunk", "ConsciousnessAttribute");
			addRelationKeyed(key, "isa", "Happiness", "EmotionalState");
			addRelationKeyed(key, "subAttribute", "Tranquility", "Happiness");
			addRelationKeyed(key, "contraryAttribute", "Tranquility", "Anxiety");
			addRelationKeyed(key, "contraryAttribute", "Unhappiness", "Happiness");
			addRelationKeyed(key, "subAttribute", "Anxiety", "Unhappiness");
			addRelationKeyed(key, "subAttribute", "Anger", "Unhappiness");
			addRelationKeyed(key, "subAttribute", "Pain", "Unhappiness");
			addRelationKeyed(key, "isa", "Surprise", "EmotionalState");
			addRelationKeyed(key, "isa", "Kneeling", "BodyPosition");
			addRelationKeyed(key, "subAttribute", "Squatting", "Sitting");
			addRelationKeyed(key, "subAttribute", "Retired", "Unemployed");
			addRelationKeyed(key, "genls", "FullTimePosition", "Position");
			addRelationKeyed(key, "genls", "PartTimePosition", "Position");
			addRelationKeyed(key, "genls", "ManualLabor", "Position");
			addRelationKeyed(key, "genls", "SkilledOccupation", "Position");
			addRelationKeyed(key, "genls", "Banker", "SkilledOccupation");
			addRelationKeyed(key, "genls", "TheaterProfession", "SkilledOccupation");
			addRelationKeyed(key, "genls", "Coach", "SkilledOccupation");
			addRelationKeyed(key, "genls", "ClericalSecretary", "SkilledOccupation");
			addRelationKeyed(key, "genls", "SportsPosition", "SkilledOccupation");
			addRelationKeyed(key, "genls", "GovernmentSecretary", "SkilledOccupation");
			addRelationKeyed(key, "isa", "SecretaryOfTheInterior",
					"GovernmentSecretary");
			addRelationKeyed(key, "isa", "SecretaryOfTheTreasury",
					"GovernmentSecretary");
			addRelationKeyed(key, "genls", "Soldier", "SkilledOccupation");
			addRelationKeyed(key, "genls", "RedcoatSoldier", "Soldier");
			addRelationKeyed(key, "genls", "ConfederateSoldier", "Soldier");
			addRelationKeyed(key, "genls", "MilitaryPrivate", "Soldier");
			addRelationKeyed(key, "genls", "MilitaryOfficer", "Soldier");
			addRelationKeyed(key, "genls", "Corporal", "MilitaryOfficer");
			addRelationKeyed(key, "genls", "Lieutenant", "MilitaryOfficer");
			addRelationKeyed(key, "genls", "CaptainOfficer", "MilitaryOfficer");
			addRelationKeyed(key, "genls", "Colonel", "MilitaryOfficer");
			addRelationKeyed(key, "genls", "OccupationalTrade", "SkilledOccupation");
			addRelationKeyed(key, "genls", "OccupationalTrade", "ManualLabor");
			addRelationKeyed(key, "genls", "FarmHand", "OccupationalTrade");
			addRelationKeyed(key, "genls", "Profession", "SkilledOccupation");
			addRelationKeyed(key, "disjoint", "Profession", "OccupationalTrade");
			addRelationKeyed(key, "genls", "Cleric", "Profession");
			addRelationKeyed(key, "genls", "Architect", "Profession");
			addRelationKeyed(key, "genls", "PoliceOfficer", "Profession");
			addRelationKeyed(key, "genls", "Deputy", "PoliceOfficer");
			addRelationKeyed(key, "genls", "PoliceDetective", "PoliceOfficer");
			addRelationKeyed(key, "genls", "PrivateDetective", "Profession");
			addRelationKeyed(key, "disjoint", "PrivateDetective", "PoliceDetective");
			addRelationKeyed(key, "genls", "Teacher", "Profession");
			addRelationKeyed(key, "genls", "Professor", "Teacher");
			addRelationKeyed(key, "genls", "MedicalDoctor", "Profession");
			addRelationKeyed(key, "genls", "Surgeon", "MedicalDoctor");
			addRelationKeyed(key, "genls", "NewsReporter", "Profession");
			addRelationKeyed(key, "genls", "UnskilledOccupation", "ManualLabor");
			addRelationKeyed(key, "disjoint", "UnskilledOccupation",
					"SkilledOccupation");
			addRelationKeyed(key, "isa", "Maid", "UnskilledOccupation");
			addRelationKeyed(key, "isa", "Mathematics", "FieldOfStudy");
			addRelationKeyed(key, "genls", "Science", "FieldOfStudy");
			addRelationKeyed(key, "genls", "SocialScience", "Science");
			addRelationKeyed(key, "isa", "Economics", "SocialScience");
			addRelationKeyed(key, "isa", "Linguistics", "SocialScience");
			addRelationKeyed(key, "subField", "Phonology", "Linguistics");
			addRelationKeyed(key, "isa", "Psychology", "SocialScience");
			addRelationKeyed(key, "isa", "Biology", "Science");
			addRelationKeyed(key, "subField", "Physiology", "Biology");
			addRelationKeyed(key, "subField", "MedicalScience", "Biology");
			addRelationKeyed(key, "isa", "Chemistry", "Science");
			addRelationKeyed(key, "isa", "Physics", "Science");
			addRelationKeyed(key, "isa", "Engineering", "Science");
			addRelationKeyed(key, "subField", "Electronics", "Physics");
			addRelationKeyed(key, "subField", "Electronics", "Engineering");
			addRelationKeyed(key, "isa", "Theology", "FieldOfStudy");
			addRelationKeyed(key, "isa", "MilitaryScience", "FieldOfStudy");
			addRelationKeyed(key, "isa", "History", "FieldOfStudy");
			addRelationKeyed(key, "isa", "Philosophy", "FieldOfStudy");
			addRelationKeyed(key, "isa", "FieldOfLaw", "FieldOfStudy");
			addRelationKeyed(key, "subrelation", "subField", "subProposition");
			addRelationKeyed(key, "isa", "subField", "TransitiveRelation");
			addRelationKeyed(key, "isa", "subField", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "hasExpertise", "BinaryPredicate");
			addRelationKeyed(key, "genls", "Infection", "DiseaseOrSyndrome");
			addRelationKeyed(key, "genls", "NonspecificDisease", "Infection");
			addRelationKeyed(key, "isa", "Cancer", "DiseaseOrSyndrome");
			addRelationKeyed(key, "isa", "Diarrhea", "DiseaseOrSyndrome");
			addRelationKeyed(key, "genls", "BiologicalSpecies", "Class");
			addRelationKeyed(key, "genls", "EconomicRelation", "BinaryRelation");
			addRelationKeyed(key, "isa", "patientMedical", "BinaryPredicate");
			addRelationKeyed(key, "isa", "DescendantsFn", "UnaryFunction");
			addRelationKeyed(key, "range", "DescendantsFn", "FamilyGroup");
			addRelationKeyed(key, "isa", "neighbor", "BinaryPredicate");
			addRelationKeyed(key, "isa", "neighbor", "SymmetricRelation");
			addRelationKeyed(key, "isa", "neighbor", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "ResidentFn", "UnaryFunction");
			addRelationKeyed(key, "range", "ResidentFn", "GroupOfPeople");
			addRelationKeyed(key, "subrelation", "CitizenryFn", "ResidentFn");
			addRelationKeyed(key, "range", "CitizenryFn", "GroupOfPeople");
			addRelationKeyed(key, "isa", "PerCapitaFn", "BinaryFunction");
			addRelationKeyed(key, "range", "PerCapitaFn", "Quantity");
			addRelationKeyed(key, "isa", "capacity", "BinaryPredicate");
			addRelationKeyed(key, "relatedInternalConcept", "capacity",
					"humanCapacity");
			addRelationKeyed(key, "isa", "humanCapacity", "BinaryPredicate");
			addRelationKeyed(key, "isa", "humanCapacity", "SingleValuedRelation");
			addRelationKeyed(key, "isa", "LastFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "FirstFn", "UnaryFunction");
			addRelationKeyed(key, "subrelation", "half", "part");
			addRelationKeyed(key, "subrelation", "most", "part");
			addRelationKeyed(key, "subrelation", "enjoys", "inScopeOfInterest");
			addRelationKeyed(key, "subrelation", "expects", "believes");
			addRelationKeyed(key, "subrelation", "fears", "expects");
			addRelationKeyed(key, "subrelation", "hopes", "expects");
			addRelationKeyed(key, "isa", "doubts", "PropositionalAttitude");
			addRelationKeyed(key, "isa", "dislikes", "ObjectAttitude");
			addRelationKeyed(key, "subrelation", "dislikes", "inScopeOfInterest");
			addRelationKeyed(key, "disjointRelation", "dislikes", "wants");
			addRelationKeyed(key, "relatedInternalConcept", "dislikes", "disapproves");
			addRelationKeyed(key, "isa", "disapproves", "PropositionalAttitude");
			addRelationKeyed(key, "subrelation", "disapproves", "inScopeOfInterest");
			addRelationKeyed(key, "disjointRelation", "disapproves", "desires");
			addRelationKeyed(key, "subrelation", "lacks", "needs");
			addRelationKeyed(key, "genls", "Graph", "Abstract");
			addRelationKeyed(key, "genls", "DirectedGraph", "Graph");
			addRelationKeyed(key, "genls", "Tree", "Graph");
			addRelationKeyed(key, "genls", "GraphPath", "DirectedGraph");
			addRelationKeyed(key, "genls", "GraphCircuit", "GraphPath");
			addRelationKeyed(key, "genls", "MultiGraph", "Graph");
			addRelationKeyed(key, "genls", "PseudoGraph", "Graph");
			addRelationKeyed(key, "genls", "GraphElement", "Abstract");
			addRelationKeyed(key, "genls", "GraphNode", "GraphElement");
			addRelationKeyed(key, "genls", "GraphArc", "GraphElement");
			addRelationKeyed(key, "genls", "GraphLoop", "GraphArc");
			addRelationKeyed(key, "isa", "links", "TernaryPredicate");
			addRelationKeyed(key, "isa", "graphPart", "BinaryPredicate");
			addRelationKeyed(key, "isa", "graphPart", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "graphPart", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "subGraph", "BinaryPredicate");
			addRelationKeyed(key, "isa", "subGraph", "ReflexiveRelation");
			addRelationKeyed(key, "isa", "subGraph", "TransitiveRelation");
			addRelationKeyed(key, "isa", "pathLength", "BinaryPredicate");
			addRelationKeyed(key, "isa", "pathLength", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "pathLength", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "InitialNodeFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "InitialNodeFn", "PartialValuedRelation");
			addRelationKeyed(key, "range", "InitialNodeFn", "GraphNode");
			addRelationKeyed(key, "isa", "TerminalNodeFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "TerminalNodeFn", "PartialValuedRelation");
			addRelationKeyed(key, "range", "TerminalNodeFn", "GraphNode");
			addRelationKeyed(key, "isa", "BeginNodeFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "BeginNodeFn", "TotalValuedRelation");
			addRelationKeyed(key, "relatedInternalConcept", "BeginNodeFn",
					"InitialNodeFn");
			addRelationKeyed(key, "isa", "EndNodeFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "EndNodeFn", "TotalValuedRelation");
			addRelationKeyed(key, "relatedInternalConcept", "EndNodeFn",
					"TerminalNodeFn");
			addRelationKeyed(key, "isa", "arcWeight", "BinaryPredicate");
			addRelationKeyed(key, "isa", "arcWeight", "SingleValuedRelation");
			addRelationKeyed(key, "isa", "PathWeightFn", "UnaryFunction");
			addRelationKeyed(key, "range", "PathWeightFn", "RealNumber");
			addRelationKeyed(key, "isa", "MinimalWeightedPathFn", "BinaryFunction");
			addRelationKeyed(key, "range", "MinimalWeightedPathFn", "GraphPath");
			addRelationKeyed(key, "isa", "MaximalWeightedPathFn", "BinaryFunction");
			addRelationKeyed(key, "range", "MaximalWeightedPathFn", "GraphPath");
			addRelationKeyed(key, "isa", "GraphPathFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "GraphPathFn", "TotalValuedRelation");
			addRelationKeyed(key, "rangeSubclass", "GraphPathFn", "GraphPath");
			addRelationKeyed(key, "isa", "CutSetFn", "UnaryFunction");
			addRelationKeyed(key, "rangeSubclass", "CutSetFn", "GraphPath");
			addRelationKeyed(key, "isa", "MinimalCutSetFn", "UnaryFunction");
			addRelationKeyed(key, "rangeSubclass", "MinimalCutSetFn", "GraphPath");
			addRelationKeyed(key, "relatedInternalConcept", "MinimalCutSetFn",
					"CutSetFn");
			addRelationKeyed(key, "genls", "TonMass", "MassMeasure");
			addRelationKeyed(key, "isa", "TonMass", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Acre", "AreaMeasure");
			addRelationKeyed(key, "isa", "Acre", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Century", "TimeDuration");
			addRelationKeyed(key, "isa", "Century", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Decade", "TimeDuration");
			addRelationKeyed(key, "isa", "Decade", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "YardLength", "LengthMeasure");
			addRelationKeyed(key, "isa", "YardLength", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Ampere", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Ampere", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Mole", "MassMeasure");
			addRelationKeyed(key, "isa", "Mole", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Candela", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Candela", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Newton", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Newton", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Pascal", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Pascal", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Joule", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Joule", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Watt", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Watt", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Coulomb", "TimeDependentQuantity");
			addRelationKeyed(key, "isa", "Coulomb", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Volt", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Volt", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Farad", "FunctionQuantity");
			addRelationKeyed(key, "genls", "Ohm", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Ohm", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Siemens", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Siemens", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Weber", "FunctionQuantity");
			addRelationKeyed(key, "genls", "Tesla", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Tesla", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Henry", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Henry", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Lumen", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Lumen", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Lux", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Lux", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Becquerel", "TimeDependentQuantity");
			addRelationKeyed(key, "isa", "Becquerel", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Gray", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Gray", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Sievert", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Sievert", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Amu", "MassMeasure");
			addRelationKeyed(key, "isa", "Amu", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "ElectronVolt", "FunctionQuantity");
			addRelationKeyed(key, "isa", "ElectronVolt", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Angstrom", "LengthMeasure");
			addRelationKeyed(key, "isa", "Angstrom", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "AtomGram", "MassMeasure");
			addRelationKeyed(key, "isa", "AtomGram", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Slug", "MassMeasure");
			addRelationKeyed(key, "isa", "Slug", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "PoundForce", "FunctionQuantity");
			addRelationKeyed(key, "isa", "PoundForce", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "UnitedKingdomGallon", "VolumeMeasure");
			addRelationKeyed(key, "isa", "UnitedKingdomGallon", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Chlorine", "ElementalSubstance");
			addRelationKeyed(key, "atomicNumber", "Chlorine", "17");
			addRelationKeyed(key, "genls", "Sodium", "ElementalSubstance");
			addRelationKeyed(key, "atomicNumber", "Sodium", "11");
			addRelationKeyed(key, "genls", "Hydrogen", "ElementalSubstance");
			addRelationKeyed(key, "atomicNumber", "Hydrogen", "1");
			addRelationKeyed(key, "genls", "Carbon", "ElementalSubstance");
			addRelationKeyed(key, "atomicNumber", "Carbon", "6");
			addRelationKeyed(key, "genls", "Iodine", "ElementalSubstance");
			addRelationKeyed(key, "atomicNumber", "Iodine", "53");
			addRelationKeyed(key, "genls", "Iron", "ElementalSubstance");
			addRelationKeyed(key, "atomicNumber", "Iron", "26");
			addRelationKeyed(key, "genls", "Helium", "ElementalSubstance");
			addRelationKeyed(key, "atomicNumber", "Helium", "2");
			addRelationKeyed(key, "genls", "Nitrogen", "ElementalSubstance");
			addRelationKeyed(key, "atomicNumber", "Nitrogen", "7");
			addRelationKeyed(key, "genls", "Copper", "ElementalSubstance");
			addRelationKeyed(key, "atomicNumber", "Copper", "29");
			addRelationKeyed(key, "genls", "Zinc", "ElementalSubstance");
			addRelationKeyed(key, "atomicNumber", "Zinc", "30");
			addRelationKeyed(key, "genls", "Stock", "FinancialInstrument");
			addRelationKeyed(key, "genls", "Share", "CurrencyMeasure");
			addRelationKeyed(key, "isa", "Share", "UnitOfMeasure");
			addRelationKeyed(key, "subrelation", "stockOf", "BinaryPredicate");
			addRelationKeyed(key, "isa", "stockHolder", "BinaryPredicate");
			addRelationKeyed(key, "genls", "FinancialCompany", "CommercialAgent");
			addRelationKeyed(key, "genls", "FinancialBank", "FinancialCompany");
			addRelationKeyed(key, "genls", "CreditUnion", "FinancialCompany");
			addRelationKeyed(key, "genls", "SavingsAndLoan", "FinancialCompany");
			addRelationKeyed(key, "genls", "FinancialAccount", "Contract");
			addRelationKeyed(key, "subAttribute", "PurchaseContract", "Contract");
			addRelationKeyed(key, "subAttribute", "ServiceContract", "Contract");
			addRelationKeyed(key, "subAttribute", "Warranty", "ServiceContract");
			addRelationKeyed(key, "isa", "agreementMember", "BinaryPredicate");
			addRelationKeyed(key, "isa", "agreementMember", "TotalValuedRelation");
			addRelationKeyed(key, "subrelation", "financialAccount",
					"agreementMember");
			addRelationKeyed(key, "isa", "price", "TernaryPredicate");
			addRelationKeyed(key, "isa", "profit", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "loss", "AsymmetricRelation");
			addRelationKeyed(key, "subrelation", "financialAsset", "possesses");
			addRelationKeyed(key, "genls", "FillingAnOrder", "FinancialTransaction");
			addRelationKeyed(key, "genls", "Security", "FinancialInstrument");
			addRelationKeyed(key, "isa", "currentAccountBalance", "TernaryPredicate");
			addRelationKeyed(key, "isa", "interestEarned", "TernaryPredicate");
			addRelationKeyed(key, "genls", "FinancialService", "CommercialService");
			addRelationKeyed(key, "isa", "customer", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "customer", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "customer", "BinaryPredicate");
			addRelationKeyed(key, "subrelation", "accountHolder", "agreementMember");
			addRelationKeyed(key, "isa", "income", "TernaryPredicate");
			addRelationKeyed(key, "isa", "incomeEarned", "TernaryPredicate");
			addRelationKeyed(key, "genls", "ChargingAFee", "FinancialTransaction");
			addRelationKeyed(key, "genls", "Tax", "ChargingAFee");
			addRelationKeyed(key, "genls", "DutyTax", "Tax");
			addRelationKeyed(key, "genls", "Payment", "FinancialTransaction");
			addRelationKeyed(key, "genls", "PayCheck", "Check");
			addRelationKeyed(key, "isa", "checkAccount", "BinaryPredicate");
			addRelationKeyed(key, "genls", "DrawingACheck", "UsingAnAccount");
			addRelationKeyed(key, "genls", "DepositingACheck", "UsingAnAccount");
			addRelationKeyed(key, "genls", "ProcessingACheck",
					"AuthorizationOfTransaction");
			addRelationKeyed(key, "genls", "DepositAccount", "FinancialAccount");
			addRelationKeyed(key, "genls", "CheckingAccount", "DepositAccount");
			addRelationKeyed(key, "genls", "AuthorizationOfTransaction",
					"FinancialService");
			addRelationKeyed(key, "genls", "AuthorizationOfTransaction",
					"RegulatoryProcess");
			addRelationKeyed(key, "genls", "Deposit", "FinancialTransaction");
			addRelationKeyed(key, "disjoint", "Deposit", "Withdrawal");
			addRelationKeyed(key, "genls", "Withdrawal", "FinancialTransaction");
			addRelationKeyed(key, "isa", "issuedBy", "BinaryPredicate");
			addRelationKeyed(key, "isa", "signedBy", "BinaryPredicate");
			addRelationKeyed(key, "genls", "Investing", "FinancialTransaction");
			addRelationKeyed(key, "genls", "Bond", "FinancialInstrument");
			addRelationKeyed(key, "isa", "couponInterest", "BinaryPredicate");
			addRelationKeyed(key, "isa", "maturityDate", "BinaryPredicate");
			addRelationKeyed(key, "isa", "principalAmount", "BinaryPredicate");
			addRelationKeyed(key, "isa", "periodicPayment", "TernaryPredicate");
			addRelationKeyed(key, "isa", "amountDue", "TernaryPredicate");
			addRelationKeyed(key, "isa", "securedBy", "BinaryPredicate");
			addRelationKeyed(key, "genls", "Fishing", "Hunting");
			addRelationKeyed(key, "genls", "Hunting", "Pursuing");
			addRelationKeyed(key, "isa", "SquareMeter", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "SquareMeter", "AreaMeasure");
			addRelationKeyed(key, "genls", "Earthquake", "GeologicalProcess");
			addRelationKeyed(key, "genls", "EarthTremor", "Tremor");
			addRelationKeyed(key, "genls", "EarthTremor", "GeologicalProcess");
			addRelationKeyed(key, "genls", "Planting", "Putting");
			addRelationKeyed(key, "isa", "DocumentFn", "UnaryFunction");
			addRelationKeyed(key, "rangeSubclass", "DocumentFn", "Text");
			addRelationKeyed(key, "genls", "Star", "AstronomicalBody");
			addRelationKeyed(key, "isa", "Sol", "Star");
			addRelationKeyed(key, "genls", "SolarSystem", "Collection");
			addRelationKeyed(key, "genls", "Meteoroid", "AstronomicalBody");
			addRelationKeyed(key, "genls", "Meteorite", "Meteoroid");
			addRelationKeyed(key, "isa", "orbits", "BinaryPredicate");
			addRelationKeyed(key, "isa", "orbits", "AsymmetricRelation");
			addRelationKeyed(key, "genls", "NaturalSatellite", "Satellite");
			addRelationKeyed(key, "genls", "NaturalSatellite", "AstronomicalBody");
			addRelationKeyed(key, "disjoint", "NaturalSatellite", "Artifact");
			addRelationKeyed(key, "genls", "Moon", "NaturalSatellite");
			addRelationKeyed(key, "isa", "EarthsMoon", "Moon");
			addRelationKeyed(key, "orbits", "EarthsMoon", "PlanetEarth");
			addRelationKeyed(key, "genls", "Planet", "NaturalSatellite");
			addRelationKeyed(key, "isa", "PlanetEarth", "Planet");
			addRelationKeyed(key, "orbits", "PlanetEarth", "Sol");
			addRelationKeyed(key, "isa", "PlanetMercury", "Planet");
			addRelationKeyed(key, "orbits", "PlanetEarth", "Sol");
			addRelationKeyed(key, "isa", "PlanetVenus", "Planet");
			addRelationKeyed(key, "orbits", "PlanetVenus", "Sol");
			addRelationKeyed(key, "isa", "PlanetMars", "Planet");
			addRelationKeyed(key, "orbits", "PlanetMars", "Sol");
			addRelationKeyed(key, "isa", "PlanetJupiter", "Planet");
			addRelationKeyed(key, "orbits", "PlanetJupiter", "Sol");
			addRelationKeyed(key, "isa", "PlanetSaturn", "Planet");
			addRelationKeyed(key, "orbits", "PlanetSaturn", "Sol");
			addRelationKeyed(key, "isa", "PlanetNeptune", "Planet");
			addRelationKeyed(key, "orbits", "PlanetNeptune", "Sol");
			addRelationKeyed(key, "isa", "PlanetUranus", "Planet");
			addRelationKeyed(key, "orbits", "PlanetUranus", "Sol");
			addRelationKeyed(key, "isa", "PlanetPluto", "Planet");
			addRelationKeyed(key, "orbits", "PlanetPluto", "Sol");
			addRelationKeyed(key, "genls", "WeatherFront", "WeatherProcess");
			addRelationKeyed(key, "isa", "barometricPressure", "BinaryPredicate");
			addRelationKeyed(key, "isa", "barometricPressure", "AsymmetricRelation");
			addRelationKeyed(key, "subrelation", "barometricPressure", "measure");
			addRelationKeyed(key, "genls", "PressureMeasure", "ConstantQuantity");
			addRelationKeyed(key, "genls", "InchMercury", "PressureMeasure");
			addRelationKeyed(key, "isa", "InchMercury", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "MmMercury", "PressureMeasure");
			addRelationKeyed(key, "isa", "MmMercury", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Atmosphere", "Region");
			addRelationKeyed(key, "isa", "EarthsAtmosphere", "Atmosphere");
			addRelationKeyed(key, "genls", "AtmosphericRegion", "GeographicArea");
			addRelationKeyed(key, "genls", "FlowRegion", "SelfConnectedObject");
			addRelationKeyed(key, "genls", "AirStream", "FlowRegion	");
			addRelationKeyed(key, "genls", "AirStream", "Air");
			addRelationKeyed(key, "genls", "Wind", "AirStream");
			addRelationKeyed(key, "genls", "Falling", "Translocation");
			addRelationKeyed(key, "genls", "Falling", "MotionDownward");
			addRelationKeyed(key, "genls", "Raining", "Precipitation");
			addRelationKeyed(key, "genls", "Snowing", "Precipitation");
			addRelationKeyed(key, "genls", "BotanicalTree", "FloweringPlant");
			addRelationKeyed(key, "genls", "Shrub", "FloweringPlant");
			addRelationKeyed(key, "genls", "Forest", "LandArea");
			addRelationKeyed(key, "disjoint", "Forest", "Field");
			addRelationKeyed(key, "genls", "Agriculture", "Maintaining");
			addRelationKeyed(key, "genls", "Inlet", "BodyOfWater");
			addRelationKeyed(key, "genls", "BodyOfWater", "WaterArea");
			addRelationKeyed(key, "genls", "BodyOfWater", "SelfConnectedObject");
			addRelationKeyed(key, "genls", "River", "BodyOfWater");
			addRelationKeyed(key, "genls", "River", "StreamWaterArea");
			addRelationKeyed(key, "genls", "River", "FreshWaterArea");
			addRelationKeyed(key, "isa", "WorldOcean", "SaltWaterArea");
			addRelationKeyed(key, "isa", "WorldOcean", "BodyOfWater");
			addRelationKeyed(key, "genls", "Ocean", "SaltWaterArea");
			addRelationKeyed(key, "genls", "Ocean", "BodyOfWater");
			addRelationKeyed(key, "isa", "AtlanticOcean", "Ocean");
			addRelationKeyed(key, "isa", "PacificOcean", "Ocean");
			addRelationKeyed(key, "genls", "LandForm", "LandArea");
			addRelationKeyed(key, "genls", "SlopedArea", "LandForm");
			addRelationKeyed(key, "genls", "Soil", "Mixture");
			addRelationKeyed(key, "genls", "Humus", "Mixture");
			addRelationKeyed(key, "genls", "Clay", "Soil");
			addRelationKeyed(key, "genls", "Sand", "Soil");
			addRelationKeyed(key, "isa", "Africa", "Continent");
			addRelationKeyed(key, "isa", "NorthAmerica", "Continent");
			addRelationKeyed(key, "isa", "SouthAmerica", "Continent");
			addRelationKeyed(key, "isa", "Antarctica", "Continent");
			addRelationKeyed(key, "isa", "Europe", "Continent");
			addRelationKeyed(key, "isa", "Asia", "Continent");
			addRelationKeyed(key, "isa", "Oceania", "Continent");
			addRelationKeyed(key, "isa", "ArcticRegion", "GeographicArea");
			addRelationKeyed(key, "genls", "Rock", "Substance");
			addRelationKeyed(key, "relatedInternalConcept",
					"dependentGeopoliticalArea", "primaryGeopoliticalSubdivision");
			addRelationKeyed(key, "genls", "NationalGovernment", "Government");
			addRelationKeyed(key, "isa", "governmentType", "BinaryPredicate");
			addRelationKeyed(key, "subrelation", "governmentType", "attribute");
			addRelationKeyed(key, "genls", "FormOfGovernment",
					"PoliticoEconomicAttribute");
			addRelationKeyed(key, "isa", "Monarchy", "FormOfGovernment");
			addRelationKeyed(key, "isa", "HereditaryMonarchy", "FormOfGovernment");
			addRelationKeyed(key, "isa", "Chiefdom", "FormOfGovernment");
			addRelationKeyed(key, "isa", "ConstitutionalMonarchy", "FormOfGovernment");
			addRelationKeyed(key, "isa", "Coprincipality", "FormOfGovernment");
			addRelationKeyed(key, "isa", "Republic", "FormOfGovernment");
			addRelationKeyed(key, "isa", "FederalRepublic", "FormOfGovernment");
			addRelationKeyed(key, "isa", "FederalDemocraticRepublic",
					"FormOfGovernment");
			addRelationKeyed(key, "isa", "ParliamentaryGovernment",
					"FormOfGovernment");
			addRelationKeyed(key, "isa", "ParliamentaryRepublic", "FormOfGovernment");
			addRelationKeyed(key, "isa", "ParliamentaryDemocracy", "FormOfGovernment");
			addRelationKeyed(key, "isa", "ParliamentaryDemocraticRepublic",
					"FormOfGovernment");
			addRelationKeyed(key, "isa", "FederalParliamentaryDemocracy",
					"FormOfGovernment");
			addRelationKeyed(key, "isa", "PresidentialGovernment", "FormOfGovernment");
			addRelationKeyed(key, "isa", "ConstitutionalGovernment",
					"FormOfGovernment");
			addRelationKeyed(key, "isa", "ConstitutionalRepublic", "FormOfGovernment");
			addRelationKeyed(key, "isa", "ConstitutionalParliamentaryDemocracy",
					"FormOfGovernment");
			addRelationKeyed(key, "isa", "ConstitutionalDemocraticRepublic",
					"FormOfGovernment");
			addRelationKeyed(key, "isa", "FederalGovernment", "FormOfGovernment");
			addRelationKeyed(key, "isa", "Federation", "FormOfGovernment");
			addRelationKeyed(key, "isa", "Commonwealth", "FormOfGovernment");
			addRelationKeyed(key, "isa", "Democracy", "FormOfGovernment");
			addRelationKeyed(key, "isa", "MultipartyDemocracy", "FormOfGovernment");
			addRelationKeyed(key, "isa", "TransitionalGovernment", "FormOfGovernment");
			addRelationKeyed(key, "isa", "EmergingDemocracy", "FormOfGovernment");
			addRelationKeyed(key, "isa", "Factionalism", "FormOfGovernment");
			addRelationKeyed(key, "isa", "Anarchy", "FormOfGovernment");
			addRelationKeyed(key, "isa", "AuthoritarianRegime", "FormOfGovernment");
			addRelationKeyed(key, "isa", "MilitaryDictatorship", "FormOfGovernment");
			addRelationKeyed(key, "isa", "Dictatorship", "FormOfGovernment");
			addRelationKeyed(key, "isa", "CommunistState", "FormOfGovernment");
			addRelationKeyed(key, "isa", "AuthoritarianSocialist", "FormOfGovernment");
			addRelationKeyed(key, "isa", "TheocraticGovernment", "FormOfGovernment");
			addRelationKeyed(key, "isa", "TheocraticRepublic", "FormOfGovernment");
			addRelationKeyed(key, "isa", "EcclesiasticalGovernment",
					"FormOfGovernment");
			addRelationKeyed(key, "isa", "IslamicGovernment", "FormOfGovernment");
			addRelationKeyed(key, "isa", "CompactOfFreeAssociationWithUnitedStates",
					"FormOfGovernment");
			addRelationKeyed(key, "isa", "CompactOfFreeAssociationWithNewZealand",
					"FormOfGovernment");
			addRelationKeyed(key, "isa", "UnitaryRule", "FormOfGovernment");
			addRelationKeyed(key, "subAttribute", "HereditaryMonarchy", "Monarchy");
			addRelationKeyed(key, "subAttribute", "Chiefdom", "Monarchy");
			addRelationKeyed(key, "subAttribute", "ConstitutionalMonarchy",
					"Monarchy");
			addRelationKeyed(key, "subAttribute", "AbsoluteMonarchy", "Monarchy");
			addRelationKeyed(key, "contraryAttribute", "Monarchy", "Republic");
			addRelationKeyed(key, "contraryAttribute", "Monarchy", "Federation");
			addRelationKeyed(key, "subAttribute", "ParliamentaryRepublic",
					"ParliamentaryGovernment");
			addRelationKeyed(key, "subAttribute", "ParliamentaryDemocracy",
					"ParliamentaryGovernment");
			addRelationKeyed(key, "subAttribute", "FederalParliamentaryDemocracy",
					"ParliamentaryGovernment");
			addRelationKeyed(key, "subAttribute",
					"ConstitutionalParliamentaryDemocracy", "ParliamentaryGovernment");
			addRelationKeyed(key, "subAttribute", "FederalRepublic",
					"FederalGovernment");
			addRelationKeyed(key, "subAttribute", "FederalDemocraticRepublic",
					"FederalGovernment");
			addRelationKeyed(key, "subAttribute", "FederalParliamentaryDemocracy",
					"FederalGovernment");
			addRelationKeyed(key, "subAttribute", "Federation", "FederalGovernment");
			addRelationKeyed(key, "contraryAttribute", "FederalGovernment",
					"UnitaryRule");
			addRelationKeyed(key, "contraryAttribute", "FederalGovernment",
					"AuthoritarianRegime");
			addRelationKeyed(key, "subAttribute", "ConstitutionalRepublic",
					"Republic");
			addRelationKeyed(key, "subAttribute", "ConstitutionalDemocraticRepublic",
					"Republic");
			addRelationKeyed(key, "subAttribute", "FederalRepublic", "Republic");
			addRelationKeyed(key, "subAttribute", "ParliamentaryRepublic", "Republic");
			addRelationKeyed(key, "subAttribute", "ParliamentaryDemocraticRepublic",
					"Republic");
			addRelationKeyed(key, "subAttribute", "FederalDemocraticRepublic",
					"Republic");
			addRelationKeyed(key, "subAttribute", "FederalDemocraticRepublic",
					"Democracy");
			addRelationKeyed(key, "subAttribute", "ParliamentaryDemocracy",
					"Democracy");
			addRelationKeyed(key, "subAttribute", "ParliamentaryDemocraticRepublic",
					"Democracy");
			addRelationKeyed(key, "subAttribute", "FederalParliamentaryDemocracy",
					"Democracy");
			addRelationKeyed(key, "subAttribute",
					"ConstitutionalParliamentaryDemocracy", "Democracy");
			addRelationKeyed(key, "subAttribute", "ConstitutionalDemocraticRepublic",
					"Democracy");
			addRelationKeyed(key, "subAttribute", "MultipartyDemocracy", "Democracy");
			addRelationKeyed(key, "subAttribute", "EmergingDemocracy", "Democracy");
			addRelationKeyed(key, "subAttribute", "ConstitutionalDemocracy",
					"ConstitutionalGovernment");
			addRelationKeyed(key, "subAttribute", "ConstitutionalMonarchy",
					"ConstitutionalGovernment");
			addRelationKeyed(key, "subAttribute",
					"ConstitutionalParliamentaryDemocracy", "ConstitutionalDemocracy");
			addRelationKeyed(key, "subAttribute", "ConstitutionalRepublic",
					"ConstitutionalGovernment");
			addRelationKeyed(key, "subAttribute", "Dictatorship",
					"AuthoritarianRegime");
			addRelationKeyed(key, "subAttribute", "MilitaryDictatorship",
					"Dictatorship");
			addRelationKeyed(key, "subAttribute", "AbsoluteMonarchy",
					"AuthoritarianRegime");
			addRelationKeyed(key, "subAttribute", "CommunistState",
					"AuthoritarianRegime");
			addRelationKeyed(key, "subAttribute", "AuthoritarianSocialist",
					"AuthoritarianRegime");
			addRelationKeyed(key, "subAttribute", "TheocraticGovernment",
					"AuthoritarianRegime");
			addRelationKeyed(key, "contraryAttribute", "AuthoritarianRegime",
					"MultipartyDemocracy");
			addRelationKeyed(key, "subAttribute", "TheocraticRepublic",
					"TheocraticGovernment");
			addRelationKeyed(key, "subAttribute", "EcclesiasticalGovernment",
					"TheocraticGovernment");
			addRelationKeyed(key, "subAttribute", "IslamicGovernment",
					"TheocraticGovernment");
			addRelationKeyed(key, "subAttribute", "EmergingDemocracy",
					"TransitionalGovernment");
			addRelationKeyed(key, "subAttribute", "Factionalism",
					"TransitionalGovernment");
			addRelationKeyed(key, "isa", "capitalCity", "BinaryPredicate");
			addRelationKeyed(key, "subrelation", "capitalCity",
					"administrativeCenter");
			addRelationKeyed(key, "isa", "administrativeCenter", "BinaryPredicate");
			addRelationKeyed(key, "subrelation", "administrativeCenter",
					"geopoliticalSubdivision");
			addRelationKeyed(key, "isa", "primaryGeopoliticalSubdivision",
					"BinaryPredicate");
			addRelationKeyed(key, "isa", "primaryGeopoliticalSubdivision",
					"AsymmetricRelation");
			addRelationKeyed(key, "subrelation", "primaryGeopoliticalSubdivision",
					"geopoliticalSubdivision");
			addRelationKeyed(key, "documentation", "primaryGeopoliticalSubdivision",
					"");
			addRelationKeyed(key, "genls", "Holiday", "TimeInterval");
			addRelationKeyed(key, "genls", "FixedHoliday", "Holiday");
			addRelationKeyed(key, "genls", "MoveableHoliday", "Holiday");
			addRelationKeyed(key, "isa", "ExecutiveBranchFn", "UnaryFunction");
			addRelationKeyed(key, "range", "ExecutiveBranchFn", "Organization");
			addRelationKeyed(key, "isa", "leaderPosition", "BinaryPredicate");
			addRelationKeyed(key, "isa", "leaderPosition", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "representativeAgentToAgent",
					"TernaryPredicate");
			addRelationKeyed(key, "isa", "President", "Position");
			addRelationKeyed(key, "isa", "PrimeMinister", "Position");
			addRelationKeyed(key, "isa", "VicePresident", "Position");
			addRelationKeyed(key, "isa", "GovernmentDeputy", "Position");
			addRelationKeyed(key, "isa", "Chairman", "Position");
			addRelationKeyed(key, "isa", "ViceChairman", "Position");
			addRelationKeyed(key, "isa", "MilitaryCommander", "Position");
			addRelationKeyed(key, "isa", "Monarch", "Position");
			addRelationKeyed(key, "subAttribute", "Queen", "Monarch");
			addRelationKeyed(key, "subAttribute", "King", "Monarch");
			addRelationKeyed(key, "isa", "Mayor", "Position");
			addRelationKeyed(key, "isa", "Governor", "Position");
			addRelationKeyed(key, "isa", "chiefOfState", "TernaryPredicate");
			addRelationKeyed(key, "isa", "headOfGovernment", "TernaryPredicate");
			addRelationKeyed(key, "isa", "electionForPosition", "BinaryPredicate");
			addRelationKeyed(key, "isa", "candidateForPosition", "TernaryPredicate");
			addRelationKeyed(key, "isa", "voteFractionReceived",
					"QuaternaryPredicate");
			addRelationKeyed(key, "isa", "electionWinner", "TernaryPredicate");
			addRelationKeyed(key, "subrelation", "electionWinner",
					"candidateForPosition");
			addRelationKeyed(key, "genls", "GeneralElection", "Election");
			addRelationKeyed(key, "genls", "PopularElection", "GeneralElection");
			addRelationKeyed(key, "genls", "ElectoralCollegeElection",
					"GeneralElection");
			addRelationKeyed(key, "genls", "LegislativeOrganization", "Organization");
			addRelationKeyed(key, "genls", "Parliament", "LegislativeOrganization");
			addRelationKeyed(key, "isa", "LegislatureFn", "UnaryFunction");
			addRelationKeyed(key, "range", "LegislatureFn", "LegislativeOrganization");
			addRelationKeyed(key, "isa", "JudiciaryFn", "UnaryFunction");
			addRelationKeyed(key, "range", "JudiciaryFn", "GovernmentOrganization");
			addRelationKeyed(key, "range", "JudiciaryFn", "JudicialOrganization");
			addRelationKeyed(key, "genls", "PoliticalParty", "PoliticalOrganization");
			addRelationKeyed(key, "isa", "RepublicanParty", "PoliticalParty");
			addRelationKeyed(key, "isa", "DemocraticParty", "PoliticalParty");
			addRelationKeyed(key, "isa", "MilitaryGeneral", "MilitaryOfficer");
			addRelationKeyed(key, "genls", "Airport", "TransitTerminal");
			addRelationKeyed(key, "genls", "Airport", "LandTransitway");
			addRelationKeyed(key, "genls", "Pipeline", "Transitway");
			addRelationKeyed(key, "genls", "Waterway", "Transitway");
			addRelationKeyed(key, "genls", "Waterway", "WaterArea");
			addRelationKeyed(key, "genls", "Canal", "Waterway");
			addRelationKeyed(key, "genls", "Canal", "StationaryArtifact");
			addRelationKeyed(key, "genls", "SurfacedRoadway", "Roadway");
			addRelationKeyed(key, "disjoint", "SurfacedRoadway", "UnsurfacedRoadway");
			addRelationKeyed(key, "genls", "Expressway", "SurfacedRoadway");
			addRelationKeyed(key, "genls", "UnsurfacedRoadway", "Roadway");
			addRelationKeyed(key, "genls", "Railway", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Tunnel", "LandTransitway");
			addRelationKeyed(key, "genls", "Tunnel", "StationaryArtifact");
			addRelationKeyed(key, "genls", "RailroadTrack", "StationaryArtifact");
			addRelationKeyed(key, "genls", "TransitwayJunction", "Transitway");
			addRelationKeyed(key, "genls", "RoadJunction", "TransitwayJunction");
			addRelationKeyed(key, "genls", "RoadJunction", "Roadway");
			addRelationKeyed(key, "genls", "LandVehicle", "Vehicle");
			addRelationKeyed(key, "genls", "RoadVehicle", "LandVehicle");
			addRelationKeyed(key, "genls", "Automobile", "RoadVehicle");
			addRelationKeyed(key, "genls", "Truck", "RoadVehicle");
			addRelationKeyed(key, "genls", "Trailer", "RoadVehicle	");
			addRelationKeyed(key, "genls", "Cycle", "LandVehicle");
			addRelationKeyed(key, "genls", "Cycle", "UserPoweredDevice");
			addRelationKeyed(key, "genls", "Bicycle", "Cycle");
			addRelationKeyed(key, "genls", "Motorcycle", "RoadVehicle	");
			addRelationKeyed(key, "genls", "RailVehicle", "LandVehicle");
			addRelationKeyed(key, "genls", "Wagon", "LandVehicle");
			addRelationKeyed(key, "genls", "Train", "RailVehicle");
			addRelationKeyed(key, "genls", "Watercraft", "Vehicle");
			addRelationKeyed(key, "genls", "Aircraft", "Vehicle");
			addRelationKeyed(key, "genls", "Airplane", "Aircraft");
			addRelationKeyed(key, "genls", "TransitTerminal", "StationaryArtifact");
			addRelationKeyed(key, "genls", "TrainStation", "TransitTerminal");
			addRelationKeyed(key, "genls", "CommonCarrier", "TransportationCompany");
			addRelationKeyed(key, "disjoint", "CommonCarrier", "ContractCarrier");
			addRelationKeyed(key, "genls", "ContractCarrier", "TransportationCompany");
			addRelationKeyed(key, "isa", "OperatingFn", "UnaryFunction");
			addRelationKeyed(key, "rangeSubclass", "OperatingFn", "Process");
			addRelationKeyed(key, "isa", "subordinateInOrganization",
					"TernaryPredicate");
			addRelationKeyed(key, "documentation", "subordinateInOrganization", "");
			addRelationKeyed(key, "isa", "lowerRankPositionInOrganization",
					"TernaryPredicate");
			addRelationKeyed(key, "documentation", "lowerRankPositionInOrganization",
					"");
			addRelationKeyed(key, "genls", "MaterialHandlingEquipment", "Device");
			addRelationKeyed(key, "genls", "LiquefiedPetroleumGas",
					"PetroleumProduct");
			addRelationKeyed(key, "isa", "memberType", "BinaryPredicate");
			addRelationKeyed(key, "isa", "memberTypeCount", "TernaryPredicate");
			addRelationKeyed(key, "genls", "Signalling", "Guiding");
			addRelationKeyed(key, "genls", "ElectricalSignalling", "Signalling");
			addRelationKeyed(key, "genls", "ElectronicSignalling", "Signalling");
			addRelationKeyed(key, "subrelation", "detainee", "patient");
			addRelationKeyed(key, "isa", "detainee", "CaseRole");
			addRelationKeyed(key, "isa", "equipmentCount", "TernaryPredicate");
			addRelationKeyed(key, "genls", "Crane", "MaterialHandlingEquipment");
			addRelationKeyed(key, "genls", "HoistingDevice",
					"MaterialHandlingEquipment");
			addRelationKeyed(key, "isa", "capableAtLocation", "QuaternaryPredicate");
			addRelationKeyed(key, "documentation", "capableAtLocation", "");
			addRelationKeyed(key, "isa", "registeredItem", "BinaryPredicate");
			addRelationKeyed(key, "subrelation", "registeredItem", "refers");
			addRelationKeyed(key, "genls", "License", "Certificate");
			addRelationKeyed(key, "isa", "inventory", "BinaryPredicate");
			addRelationKeyed(key, "genls", "MilitaryWatercraft", "Watercraft");
			addRelationKeyed(key, "isa", "powerPlant", "BinaryPredicate");
			addRelationKeyed(key, "isa", "powerPlant", "AsymmetricPredicate");
			addRelationKeyed(key, "isa", "powerPlant", "IrreflexivePredicate");
			addRelationKeyed(key, "subrelation", "powerPlant", "component");
			addRelationKeyed(key, "genls", "Engine", "Machine");
			addRelationKeyed(key, "genls", "InternalCombustionEngine", "Engine");
			addRelationKeyed(key, "genls", "GasolineEngine",
					"InternalCombustionEngine");
			addRelationKeyed(key, "genls", "JetEngine", "InternalCombustionEngine");
			addRelationKeyed(key, "genls", "TurbojetEngine",
					"InternalCombustionEngine");
			addRelationKeyed(key, "genls", "RocketEngine", "Engine");
			addRelationKeyed(key, "genls", "DieselEngine", "InternalCombustionEngine");
			addRelationKeyed(key, "genls", "SteamEngine", "Engine");
			addRelationKeyed(key, "genls", "Windmill", "Engine");
			addRelationKeyed(key, "genls", "ElectricMotor", "Engine");
			addRelationKeyed(key, "genls", "ElectricMotor", "ElectricDevice");
			addRelationKeyed(key, "genls", "Battery", "Device");
			addRelationKeyed(key, "genls", "SelfPoweredDevice",
					"TransportationDevice");
			addRelationKeyed(key, "documentation", "SelfPoweredDevice", "");
			addRelationKeyed(key, "genls", "AnimalPoweredDevice", "Device");
			addRelationKeyed(key, "genls", "UserPoweredDevice", "Device");
			addRelationKeyed(key, "genls", "Fuel", "Substance");
			addRelationKeyed(key, "genls", "FossilFuel", "Fuel");
			addRelationKeyed(key, "genls", "FossilFuel", "OrganicCompound");
			addRelationKeyed(key, "genls", "DieselFuel", "FossilFuel");
			addRelationKeyed(key, "genls", "Gasoline", "FossilFuel");
			addRelationKeyed(key, "genls", "Ramp", "Object");
			addRelationKeyed(key, "genls", "RefrigeratedCompartment", "Container");
			addRelationKeyed(key, "genls", "Urea", "OrganicCompound");
			addRelationKeyed(key, "isa", "DeviceOpen", "DeviceStateAttribute");
			addRelationKeyed(key, "isa", "DeviceOpen", "RelationalAttribute");
			addRelationKeyed(key, "isa", "DeviceClosed", "DeviceStateAttribute");
			addRelationKeyed(key, "isa", "DeviceClosed", "RelationalAttribute");
			addRelationKeyed(key, "isa", "DeviceOn", "DeviceStateAttribute");
			addRelationKeyed(key, "isa", "DeviceOn", "InternalAttribute");
			addRelationKeyed(key, "isa", "DeviceOff", "DeviceStateAttribute");
			addRelationKeyed(key, "isa", "DeviceOff", "InternalAttribute");
			addRelationKeyed(key, "isa", "deviceState", "BinaryPredicate");
			addRelationKeyed(key, "isa", "MetricTon", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "MetricTon", "MassMeasure");
			addRelationKeyed(key, "isa", "CubicFoot", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "CubicFoot", "VolumeMeasure");
			addRelationKeyed(key, "isa", "ContainerFull", "RelationalAttribute");
			addRelationKeyed(key, "isa", "ContainerEmpty", "RelationalAttribute");
			addRelationKeyed(key, "genls", "HumanPoweredDevice", "Device");
			addRelationKeyed(key, "genls", "WindPoweredDevice", "Device");
			addRelationKeyed(key, "genls", "CommunicationDevice",
					"EngineeringComponent");
			addRelationKeyed(key, "relatedInternalConcept", "Communication",
					"CommunicationDevice");
			addRelationKeyed(key, "genls", "ArtificialSatellite", "Satellite");
			addRelationKeyed(key, "genls", "ArtificialSatellite", "Device");
			addRelationKeyed(key, "isa", "BroadcastingStation", "StationaryArtifact");
			addRelationKeyed(key, "genls", "RadioStation", "BroadcastingStation");
			addRelationKeyed(key, "genls", "Radio", "CommunicationDevice");
			addRelationKeyed(key, "genls", "Radio", "ElectricDevice");
			addRelationKeyed(key, "genls", "TelevisionStation", "BroadcastingStation");
			addRelationKeyed(key, "genls", "Television", "CommunicationDevice");
			addRelationKeyed(key, "genls", "Television", "ElectricDevice");
			addRelationKeyed(key, "isa", "economyType", "BinaryPredicate");
			addRelationKeyed(key, "subrelation", "economyType", "attribute");
			addRelationKeyed(key, "genls", "EconomicAttribute",
					"PoliticoEconomicAttribute");
			addRelationKeyed(key, "genls", "EconomicSystemAttribute",
					"EconomicAttribute");
			addRelationKeyed(key, "isa", "CapitalistEconomy",
					"EconomicSystemAttribute");
			addRelationKeyed(key, "subAttribute", "CapitalistEconomy",
					"PrivateEnterpriseEconomy");
			addRelationKeyed(key, "isa", "PureCapitalistEconomy",
					"EconomicSystemAttribute");
			addRelationKeyed(key, "subAttribute", "PureCapitalistEconomy",
					"CapitalistEconomy");
			addRelationKeyed(key, "contraryAttribute", "PureCapitalistEconomy",
					"MixedEconomy");
			addRelationKeyed(key, "isa", "PrivateEnterpriseEconomy",
					"EconomicSystemAttribute");
			addRelationKeyed(key, "subAttribute", "PrivateEnterpriseEconomy",
					"CapitalistEconomy");
			addRelationKeyed(key, "isa", "MarketEconomy", "EconomicSystemAttribute");
			addRelationKeyed(key, "isa", "SocialistEconomy",
					"EconomicSystemAttribute");
			addRelationKeyed(key, "isa", "PureSocialistEconomy",
					"EconomicSystemAttribute");
			addRelationKeyed(key, "subAttribute", "PureSocialistEconomy",
					"SocialistEconomy");
			addRelationKeyed(key, "contraryAttribute", "PureSocialistEconomy",
					"MixedEconomy");
			addRelationKeyed(key, "contraryAttribute", "PureSocialistEconomy",
					"PureCapitalistEconomy");
			addRelationKeyed(key, "isa", "DemocraticSocialism",
					"EconomicSystemAttribute");
			addRelationKeyed(key, "subAttribute", "DemocraticSocialism",
					"SocialistEconomy");
			addRelationKeyed(key, "isa", "MarketSocialism", "EconomicSystemAttribute");
			addRelationKeyed(key, "subAttribute", "MarketSocialism",
					"PartialMarketEconomy");
			addRelationKeyed(key, "isa", "CommunalLandOwnershipEconomy",
					"EconomicSystemAttribute");
			addRelationKeyed(key, "subAttribute", "CommunalLandOwnershipEconomy",
					"SocialistEconomy");
			addRelationKeyed(key, "isa", "MixedEconomy", "EconomicSystemAttribute	");
			addRelationKeyed(key, "isa", "PartialMarketEconomy",
					"EconomicSystemAttribute");
			addRelationKeyed(key, "subAttribute", "PartialMarketEconomy",
					"MixedEconomy");
			addRelationKeyed(key, "isa", "GovernmentRegulatedEconomy",
					"EconomicSystemAttribute");
			addRelationKeyed(key, "subAttribute", "CentrallyPlannedEconomy",
					"GovernmentRegulatedEconomy");
			addRelationKeyed(key, "isa", "PrivatizingEconomy",
					"EconomicSystemAttribute");
			addRelationKeyed(key, "subAttribute", "PrivatizingEconomy",
					"MixedEconomy");
			addRelationKeyed(key, "isa", "NationalizedIndustryEconomy",
					"EconomicSystemAttribute");
			addRelationKeyed(key, "subAttribute", "NationalizedIndustryEconomy",
					"GovernmentRegulatedEconomy");
			addRelationKeyed(key, "isa", "GovernmentSubsidizedEconomy",
					"EconomicSystemAttribute");
			addRelationKeyed(key, "isa", "WelfareCapitalism",
					"EconomicSystemAttribute");
			addRelationKeyed(key, "subAttribute", "WelfareCapitalism", "MixedEconomy");
			addRelationKeyed(key, "subAttribute", "WelfareCapitalism",
					"GovernmentSubsidizedEconomy");
			addRelationKeyed(key, "isa", "industryOfArea", "BinaryPredicate");
			addRelationKeyed(key, "isa", "industryProductType", "BinaryPredicate");
			addRelationKeyed(key, "documentation", "industryProductType", "");
			addRelationKeyed(key, "isa", "KilowattHour", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "KilowattHour", "FunctionQuantity");
			addRelationKeyed(key, "isa", "currencyType", "BinaryPredicate");
			addRelationKeyed(key, "isa", "fiscalYearPeriod", "BinaryPredicate");
			addRelationKeyed(key, "genls", "Narcotic", "ControlledSubstance");
			addRelationKeyed(key, "genls", "ControlledSubstance",
					"BiologicallyActiveSubstance");
			addRelationKeyed(key, "genls", "Fodder", "Food");
			addRelationKeyed(key, "genls", "Cement", "Mixture");
			addRelationKeyed(key, "genls", "Concrete", "Mixture");
			addRelationKeyed(key, "isa", "PopulationFn", "UnaryFunction");
			addRelationKeyed(key, "domain", "PopulationFn", "GeopoliticalArea");
			addRelationKeyed(key, "range", "PopulationFn", "Integer");
			addRelationKeyed(key, "equal", "(PopulationFn", "?AREA");
			addRelationKeyed(key, "isa", "average", "BinaryPredicate");
			addRelationKeyed(key, "isa", "average", "PartialValuedRelation");
			addRelationKeyed(key, "isa", "average", "SingleValuedRelation");
			addRelationKeyed(key, "genls", "RacialEthnicGroup", "EthnicGroup");
			addRelationKeyed(key, "isa", "BlackEthnicity", "RacialEthnicGroup");
			addRelationKeyed(key, "isa", "WhiteEthnicity", "EthnicGroup");
			addRelationKeyed(key, "isa", "AmerindianEthnicity", "EthnicGroup");
			addRelationKeyed(key, "isa", "Judaism", "BeliefGroup");
			addRelationKeyed(key, "isa", "Christianity", "BeliefGroup");
			addRelationKeyed(key, "subCollection", "Protestantism", "Christianity");
			addRelationKeyed(key, "subCollection", "RomanCatholicism", "Christianity");
			addRelationKeyed(key, "isa", "RomanCatholicChurch",
					"ReligiousOrganization");
			addRelationKeyed(key, "genls", "DeafSignLanguage", "ManualHumanLanguage");
			addRelationKeyed(key, "genls", "CreoleLanguage", "SpokenHumanLanguage");
			addRelationKeyed(key, "genls", "PidginLanguage", "SpokenHumanLanguage");
			addRelationKeyed(key, "genls", "MixedLanguage", "SpokenHumanLanguage");
			addRelationKeyed(key, "genls", "LiteracyAttribute", "TraitAttribute");
			addRelationKeyed(key, "isa", "instance", "BinaryPredicate");
			addRelationKeyed(key, "subrelation", "immediateInstance", "instance");
			addRelationKeyed(key, "isa", "immediateInstance", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "immediateInstance", "IntransitiveRelation");
			addRelationKeyed(key, "isa", "inverse", "BinaryPredicate");
			addRelationKeyed(key, "isa", "inverse", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "inverse", "IntransitiveRelation");
			addRelationKeyed(key, "isa", "inverse", "SymmetricRelation");
			addRelationKeyed(key, "isa", "subclass", "BinaryPredicate");
			addRelationKeyed(key, "isa", "subclass", "PartialOrderingRelation");
			addRelationKeyed(key, "subrelation", "immediateSubclass", "subclass");
			addRelationKeyed(key, "isa", "immediateSubclass", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "immediateSubclass", "IntransitiveRelation");
			addRelationKeyed(key, "isa", "subrelation", "BinaryPredicate");
			addRelationKeyed(key, "isa", "subrelation", "PartialOrderingRelation");
			addRelationKeyed(key, "isa", "domain", "TernaryPredicate");
			addRelationKeyed(key, "isa", "domainSubclass", "TernaryPredicate");
			addRelationKeyed(key, "isa", "equal", "BinaryPredicate");
			addRelationKeyed(key, "isa", "equal", "EquivalenceRelation");
			addRelationKeyed(key, "isa", "equal", "RelationExtendedToQuantities");
			addRelationKeyed(key, "isa", "range", "BinaryPredicate");
			addRelationKeyed(key, "isa", "range", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "rangeSubclass", "BinaryPredicate");
			addRelationKeyed(key, "isa", "rangeSubclass", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "valence", "BinaryPredicate");
			addRelationKeyed(key, "isa", "valence", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "valence", "SingleValuedRelation");
			addRelationKeyed(key, "isa", "documentation", "BinaryPredicate");
			addRelationKeyed(key, "isa", "documentation", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "disjoint", "BinaryPredicate");
			addRelationKeyed(key, "isa", "disjoint", "SymmetricRelation");
			addRelationKeyed(key, "isa", "disjointRelation", "Predicate");
			addRelationKeyed(key, "isa", "disjointRelation", "VariableArityRelation");
			addRelationKeyed(key, "relatedInternalConcept", "disjointRelation",
					"disjoint");
			addRelationKeyed(key, "isa", "contraryAttribute", "Predicate");
			addRelationKeyed(key, "isa", "contraryAttribute", "VariableArityRelation");
			addRelationKeyed(key, "isa", "exhaustiveAttribute", "Predicate");
			addRelationKeyed(key, "isa", "exhaustiveAttribute",
					"VariableArityRelation");
			addRelationKeyed(key, "isa", "exhaustiveDecomposition", "Predicate");
			addRelationKeyed(key, "isa", "exhaustiveDecomposition",
					"VariableArityRelation");
			addRelationKeyed(key, "relatedInternalConcept",
					"exhaustiveDecomposition", "partition");
			addRelationKeyed(key, "isa", "disjointDecomposition", "Predicate");
			addRelationKeyed(key, "isa", "disjointDecomposition",
					"VariableArityRelation");
			addRelationKeyed(key, "relatedInternalConcept", "disjointDecomposition",
					"exhaustiveDecomposition");
			addRelationKeyed(key, "relatedInternalConcept", "disjointDecomposition",
					"disjoint");
			addRelationKeyed(key, "isa", "partition", "Predicate");
			addRelationKeyed(key, "isa", "partition", "VariableArityRelation");
			addRelationKeyed(key, "isa", "relatedInternalConcept", "BinaryPredicate");
			addRelationKeyed(key, "isa", "relatedInternalConcept",
					"EquivalenceRelation");
			addRelationKeyed(key, "isa", "relatedExternalConcept", "TernaryPredicate");
			addRelationKeyed(key, "relatedInternalConcept", "relatedExternalConcept",
					"relatedInternalConcept");
			addRelationKeyed(key, "subrelation", "synonymousExternalConcept",
					"relatedExternalConcept");
			addRelationKeyed(key, "subrelation", "subsumingExternalConcept",
					"relatedExternalConcept");
			addRelationKeyed(key, "subrelation", "subsumedExternalConcept",
					"relatedExternalConcept");
			addRelationKeyed(key, "isa", "subAttribute", "BinaryPredicate");
			addRelationKeyed(key, "isa", "subAttribute", "PartialOrderingRelation");
			addRelationKeyed(key, "disjointRelation", "subAttribute",
					"successorAttribute");
			addRelationKeyed(key, "isa", "successorAttribute", "BinaryPredicate");
			addRelationKeyed(key, "isa", "successorAttribute", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "successorAttributeClosure",
					"BinaryPredicate");
			addRelationKeyed(key, "isa", "successorAttributeClosure",
					"TransitiveRelation");
			addRelationKeyed(key, "isa", "successorAttributeClosure",
					"IrreflexiveRelation");
			addRelationKeyed(key, "relatedInternalConcept",
					"successorAttributeClosure", "successorAttribute");
			addRelationKeyed(key, "isa", "entails", "BinaryPredicate");
			addRelationKeyed(key, "isa", "AssignmentFn", "Function");
			addRelationKeyed(key, "isa", "AssignmentFn", "VariableArityRelation");
			addRelationKeyed(key, "range", "AssignmentFn", "Entity");
			addRelationKeyed(key, "isa", "holds", "Predicate");
			addRelationKeyed(key, "isa", "holds", "VariableArityRelation");
			addRelationKeyed(key, "isa", "PowerSetFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "PowerSetFn", "TotalValuedRelation");
			addRelationKeyed(key, "rangeSubclass", "PowerSetFn", "SetOrClass");
			addRelationKeyed(key, "isa", "?THING", "Entity");
			addRelationKeyed(key, "genls", "Physical", "Entity");
			addRelationKeyed(key, "genls", "Object", "Physical");
			addRelationKeyed(key, "genls", "SelfConnectedObject", "Object");
			addRelationKeyed(key, "isa", "FrontFn", "SpatialRelation");
			addRelationKeyed(key, "isa", "FrontFn", "PartialValuedRelation");
			addRelationKeyed(key, "isa", "FrontFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "FrontFn", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "FrontFn", "IrreflexiveRelation");
			addRelationKeyed(key, "range", "FrontFn", "SelfConnectedObject");
			addRelationKeyed(key, "isa", "BackFn", "SpatialRelation");
			addRelationKeyed(key, "isa", "BackFn", "PartialValuedRelation");
			addRelationKeyed(key, "isa", "BackFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "BackFn", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "BackFn", "IrreflexiveRelation");
			addRelationKeyed(key, "range", "BackFn", "SelfConnectedObject");
			addRelationKeyed(key, "isa", "part", "SpatialRelation");
			addRelationKeyed(key, "isa", "part", "PartialOrderingRelation");
			addRelationKeyed(key, "isa", "properPart", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "properPart", "TransitiveRelation");
			addRelationKeyed(key, "subrelation", "properPart", "part");
			addRelationKeyed(key, "subrelation", "piece", "part");
			addRelationKeyed(key, "subrelation", "component", "part");
			addRelationKeyed(key, "isa", "material", "BinaryPredicate");
			addRelationKeyed(key, "subrelation", "contains", "partlyLocated");
			addRelationKeyed(key, "isa", "contains", "SpatialRelation");
			addRelationKeyed(key, "isa", "contains", "AsymmetricRelation");
			addRelationKeyed(key, "disjointRelation", "contains", "part");
			addRelationKeyed(key, "genls", "Substance", "SelfConnectedObject");
			addRelationKeyed(key, "genls", "SyntheticSubstance", "Substance");
			addRelationKeyed(key, "genls", "NaturalSubstance", "Substance");
			addRelationKeyed(key, "genls", "Atom", "ElementalSubstance");
			addRelationKeyed(key, "genls", "SubatomicParticle", "ElementalSubstance");
			addRelationKeyed(key, "genls", "AtomicNucleus", "SubatomicParticle");
			addRelationKeyed(key, "genls", "Electron", "SubatomicParticle");
			addRelationKeyed(key, "genls", "Proton", "SubatomicParticle");
			addRelationKeyed(key, "genls", "Neutron", "SubatomicParticle");
			addRelationKeyed(key, "genls", "CorpuscularObject", "SelfConnectedObject");
			addRelationKeyed(key, "disjoint", "CorpuscularObject", "Substance");
			addRelationKeyed(key, "genls", "Region", "Object");
			addRelationKeyed(key, "genls", "Collection", "Object");
			addRelationKeyed(key, "disjoint", "Collection", "SelfConnectedObject");
			addRelationKeyed(key, "subrelation", "member", "part");
			addRelationKeyed(key, "isa", "member", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "member", "IntransitiveRelation");
			addRelationKeyed(key, "relatedInternalConcept", "member", "instance");
			addRelationKeyed(key, "relatedInternalConcept", "member", "element");
			addRelationKeyed(key, "isa", "subCollection", "BinaryPredicate");
			addRelationKeyed(key, "isa", "subCollection", "PartialOrderingRelation");
			addRelationKeyed(key, "genls", "ContentBearingObject",
					"CorpuscularObject");
			addRelationKeyed(key, "relatedInternalConcept", "ContentBearingObject",
					"containsInformation");
			addRelationKeyed(key, "genls", "SymbolicString", "ContentBearingObject");
			addRelationKeyed(key, "genls", "Character", "SymbolicString");
			addRelationKeyed(key, "isa", "containsInformation", "BinaryPredicate");
			addRelationKeyed(key, "isa", "containsInformation", "AsymmetricRelation");
			addRelationKeyed(key, "subrelation", "containsInformation", "represents");
			addRelationKeyed(key, "genls", "Icon", "ContentBearingObject");
			addRelationKeyed(key, "genls", "LinguisticExpression",
					"ContentBearingObject");
			addRelationKeyed(key, "disjoint", "LinguisticExpression", "Icon");
			addRelationKeyed(key, "genls", "Language", "LinguisticExpression");
			addRelationKeyed(key, "genls", "AnimalLanguage", "Language");
			addRelationKeyed(key, "genls", "ArtificialLanguage", "Language");
			addRelationKeyed(key, "genls", "ComputerLanguage", "ArtificialLanguage");
			addRelationKeyed(key, "genls", "HumanLanguage", "Language");
			addRelationKeyed(key, "genls", "ConstructedLanguage", "HumanLanguage");
			addRelationKeyed(key, "genls", "ConstructedLanguage",
					"ArtificialLanguage");
			addRelationKeyed(key, "genls", "NaturalLanguage", "HumanLanguage");
			addRelationKeyed(key, "genls", "ManualHumanLanguage", "HumanLanguage");
			addRelationKeyed(key, "genls", "SpokenHumanLanguage", "HumanLanguage");
			addRelationKeyed(key, "genls", "Word", "LinguisticExpression");
			addRelationKeyed(key, "genls", "Formula", "Sentence");
			addRelationKeyed(key, "genls", "Agent", "Object");
			addRelationKeyed(key, "genls", "SentientAgent", "Agent");
			addRelationKeyed(key, "genls", "CognitiveAgent", "SentientAgent");
			addRelationKeyed(key, "isa", "leader", "BinaryPredicate");
			addRelationKeyed(key, "isa", "leader", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "leader", "SingleValuedRelation");
			addRelationKeyed(key, "genls", "Process", "Physical");
			addRelationKeyed(key, "genls", "DualObjectProcess", "Process");
			addRelationKeyed(key, "genls", "Abstract", "Entity");
			addRelationKeyed(key, "genls", "Quantity", "Abstract");
			addRelationKeyed(key, "genls", "Attribute", "Abstract");
			addRelationKeyed(key, "isa", "property", "BinaryPredicate");
			addRelationKeyed(key, "isa", "attribute", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "attribute", "IrreflexiveRelation");
			addRelationKeyed(key, "subrelation", "attribute", "property");
			addRelationKeyed(key, "isa", "manner", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "manner", "IrreflexiveRelation");
			addRelationKeyed(key, "subrelation", "manner", "property");
			addRelationKeyed(key, "disjointRelation", "manner", "attribute");
			addRelationKeyed(key, "isa", "AbstractionFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "AbstractionFn", "PartialValuedRelation");
			addRelationKeyed(key, "range", "AbstractionFn", "Attribute");
			addRelationKeyed(key, "isa", "ExtensionFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "ExtensionFn", "PartialValuedRelation");
			addRelationKeyed(key, "range", "ExtensionFn", "Class");
			addRelationKeyed(key, "genls", "InternalAttribute", "Attribute");
			addRelationKeyed(key, "genls", "RelationalAttribute", "Attribute");
			addRelationKeyed(key, "genls", "Number", "Quantity");
			addRelationKeyed(key, "isa", "lessThan", "BinaryPredicate");
			addRelationKeyed(key, "isa", "lessThan", "TransitiveRelation");
			addRelationKeyed(key, "isa", "lessThan", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "lessThan", "RelationExtendedToQuantities");
			addRelationKeyed(key, "trichotomizingOn", "lessThan", "RealNumber");
			addRelationKeyed(key, "isa", "greaterThan", "BinaryPredicate");
			addRelationKeyed(key, "isa", "greaterThan", "TransitiveRelation");
			addRelationKeyed(key, "isa", "greaterThan", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "greaterThan",
					"RelationExtendedToQuantities");
			addRelationKeyed(key, "trichotomizingOn", "greaterThan", "RealNumber");
			addRelationKeyed(key, "inverse", "greaterThan", "lessThan");
			addRelationKeyed(key, "isa", "lessThanOrEqualTo", "BinaryPredicate");
			addRelationKeyed(key, "isa", "lessThanOrEqualTo",
					"PartialOrderingRelation");
			addRelationKeyed(key, "isa", "lessThanOrEqualTo",
					"RelationExtendedToQuantities");
			addRelationKeyed(key, "trichotomizingOn", "lessThanOrEqualTo",
					"RealNumber");
			addRelationKeyed(key, "isa", "greaterThanOrEqualTo", "BinaryPredicate");
			addRelationKeyed(key, "isa", "greaterThanOrEqualTo",
					"PartialOrderingRelation");
			addRelationKeyed(key, "isa", "greaterThanOrEqualTo",
					"RelationExtendedToQuantities");
			addRelationKeyed(key, "trichotomizingOn", "greaterThanOrEqualTo",
					"RealNumber");
			addRelationKeyed(key, "inverse", "greaterThanOrEqualTo",
					"lessThanOrEqualTo");
			addRelationKeyed(key, "genls", "RealNumber", "Number");
			addRelationKeyed(key, "genls", "RationalNumber", "RealNumber");
			addRelationKeyed(key, "genls", "IrrationalNumber", "RealNumber");
			addRelationKeyed(key, "genls", "NonnegativeRealNumber", "RealNumber");
			addRelationKeyed(key, "genls", "PositiveRealNumber",
					"NonnegativeRealNumber");
			addRelationKeyed(key, "genls", "NegativeRealNumber", "RealNumber");
			addRelationKeyed(key, "genls", "Integer", "RationalNumber");
			addRelationKeyed(key, "genls", "EvenInteger", "Integer");
			addRelationKeyed(key, "genls", "OddInteger", "Integer");
			addRelationKeyed(key, "genls", "PrimeNumber", "Integer");
			addRelationKeyed(key, "genls", "NonnegativeInteger", "Integer");
			addRelationKeyed(key, "genls", "NonnegativeInteger",
					"NonnegativeRealNumber");
			addRelationKeyed(key, "genls", "NegativeInteger", "Integer");
			addRelationKeyed(key, "genls", "NegativeInteger", "NegativeRealNumber");
			addRelationKeyed(key, "genls", "PositiveInteger", "NonnegativeInteger");
			addRelationKeyed(key, "genls", "PositiveInteger", "PositiveRealNumber");
			addRelationKeyed(key, "genls", "BinaryNumber", "RealNumber");
			addRelationKeyed(key, "genls", "ComplexNumber", "Number");
			addRelationKeyed(key, "disjoint", "ComplexNumber", "RealNumber");
			addRelationKeyed(key, "genls", "PhysicalQuantity", "Quantity");
			addRelationKeyed(key, "genls", "ConstantQuantity", "PhysicalQuantity");
			addRelationKeyed(key, "genls", "TimeMeasure", "ConstantQuantity");
			addRelationKeyed(key, "genls", "TimeDuration", "TimeMeasure");
			addRelationKeyed(key, "genls", "TimePosition", "TimeMeasure");
			addRelationKeyed(key, "genls", "TimeInterval", "TimePosition");
			addRelationKeyed(key, "genls", "TimePoint", "TimePosition");
			addRelationKeyed(key, "genls", "FunctionQuantity", "PhysicalQuantity");
			addRelationKeyed(key, "genls", "FunctionQuantity", "Function");
			addRelationKeyed(key, "genls", "UnaryConstantFunctionQuantity",
					"FunctionQuantity");
			addRelationKeyed(key, "genls", "UnaryConstantFunctionQuantity",
					"UnaryFunction");
			addRelationKeyed(key, "genls", "TimeDependentQuantity",
					"UnaryConstantFunctionQuantity");
			addRelationKeyed(key, "genls", "TimeDependentQuantity",
					"ContinuousFunction");
			addRelationKeyed(key, "genls", "SetOrClass", "Abstract");
			addRelationKeyed(key, "genls", "Class", "SetOrClass");
			addRelationKeyed(key, "genls", "Set", "SetOrClass");
			addRelationKeyed(key, "genls", "Relation", "Abstract");
			addRelationKeyed(key, "genls", "SingleValuedRelation", "Relation");
			addRelationKeyed(key, "isa", "SingleValuedRelation",
					"InheritableRelation");
			addRelationKeyed(key, "genls", "TotalValuedRelation", "Relation");
			addRelationKeyed(key, "isa", "TotalValuedRelation", "InheritableRelation");
			addRelationKeyed(key, "genls", "PartialValuedRelation", "Relation");
			addRelationKeyed(key, "genls", "BinaryRelation", "Relation");
			addRelationKeyed(key, "isa", "BinaryRelation", "InheritableRelation");
			addRelationKeyed(key, "genls", "ReflexiveRelation", "BinaryRelation");
			addRelationKeyed(key, "genls", "IrreflexiveRelation", "BinaryRelation");
			addRelationKeyed(key, "genls", "SymmetricRelation", "BinaryRelation");
			addRelationKeyed(key, "genls", "AsymmetricRelation",
					"IrreflexiveRelation");
			addRelationKeyed(key, "genls", "AsymmetricRelation",
					"AntisymmetricRelation");
			addRelationKeyed(key, "genls", "AntisymmetricRelation", "BinaryRelation");
			addRelationKeyed(key, "genls", "TrichotomizingRelation", "BinaryRelation");
			addRelationKeyed(key, "genls", "TransitiveRelation", "BinaryRelation");
			addRelationKeyed(key, "genls", "IntransitiveRelation", "BinaryRelation");
			addRelationKeyed(key, "genls", "PartialOrderingRelation",
					"TransitiveRelation");
			addRelationKeyed(key, "genls", "PartialOrderingRelation",
					"AntisymmetricRelation");
			addRelationKeyed(key, "genls", "PartialOrderingRelation",
					"ReflexiveRelation");
			addRelationKeyed(key, "genls", "TotalOrderingRelation",
					"PartialOrderingRelation");
			addRelationKeyed(key, "genls", "TotalOrderingRelation",
					"TrichotomizingRelation");
			addRelationKeyed(key, "genls", "EquivalenceRelation",
					"TransitiveRelation");
			addRelationKeyed(key, "genls", "EquivalenceRelation", "SymmetricRelation");
			addRelationKeyed(key, "genls", "EquivalenceRelation", "ReflexiveRelation");
			addRelationKeyed(key, "genls", "CaseRole", "BinaryPredicate");
			addRelationKeyed(key, "isa", "CaseRole", "InheritableRelation");
			addRelationKeyed(key, "genls", "CaseRole", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "agent", "CaseRole");
			addRelationKeyed(key, "isa", "destination", "CaseRole");
			addRelationKeyed(key, "isa", "experiencer", "CaseRole");
			addRelationKeyed(key, "subrelation", "instrument", "patient");
			addRelationKeyed(key, "isa", "origin", "CaseRole");
			addRelationKeyed(key, "isa", "patient", "CaseRole");
			addRelationKeyed(key, "subrelation", "resource", "patient");
			addRelationKeyed(key, "subrelation", "result", "patient");
			addRelationKeyed(key, "isa", "InheritableRelation", "Class");
			addRelationKeyed(key, "genls", "ProbabilityRelation", "Relation");
			addRelationKeyed(key, "isa", "ProbabilityRelation", "InheritableRelation");
			addRelationKeyed(key, "isa", "ProbabilityFn", "ProbabilityRelation");
			addRelationKeyed(key, "isa", "ProbabilityFn", "TotalValuedRelation");
			addRelationKeyed(key, "isa", "ProbabilityFn", "UnaryFunction");
			addRelationKeyed(key, "range", "ProbabilityFn", "RealNumber");
			addRelationKeyed(key, "isa", "ProbabilityFn", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "conditionalProbability",
					"ProbabilityRelation");
			addRelationKeyed(key, "isa", "conditionalProbability", "TernaryPredicate");
			addRelationKeyed(key, "isa", "increasesLikelihood", "ProbabilityRelation");
			addRelationKeyed(key, "isa", "increasesLikelihood", "BinaryPredicate");
			addRelationKeyed(key, "isa", "increasesLikelihood", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "decreasesLikelihood", "ProbabilityRelation");
			addRelationKeyed(key, "isa", "decreasesLikelihood", "BinaryPredicate");
			addRelationKeyed(key, "isa", "decreasesLikelihood", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "independentProbability",
					"ProbabilityRelation");
			addRelationKeyed(key, "isa", "independentProbability", "BinaryPredicate");
			addRelationKeyed(key, "isa", "independentProbability",
					"SymmetricRelation");
			addRelationKeyed(key, "genls", "SpatialRelation", "Relation");
			addRelationKeyed(key, "isa", "SpatialRelation", "InheritableRelation");
			addRelationKeyed(key, "genls", "TemporalRelation", "Relation");
			addRelationKeyed(key, "isa", "TemporalRelation", "InheritableRelation");
			addRelationKeyed(key, "isa", "IntentionalRelation", "InheritableRelation");
			addRelationKeyed(key, "isa", "prefers", "TernaryPredicate");
			addRelationKeyed(key, "isa", "prefers", "IntentionalRelation");
			addRelationKeyed(key, "genls", "PropositionalAttitude",
					"IntentionalRelation");
			addRelationKeyed(key, "genls", "PropositionalAttitude",
					"AsymmetricRelation");
			addRelationKeyed(key, "isa", "PropositionalAttitude",
					"InheritableRelation");
			addRelationKeyed(key, "genls", "ObjectAttitude", "IntentionalRelation");
			addRelationKeyed(key, "isa", "ObjectAttitude", "InheritableRelation");
			addRelationKeyed(key, "disjoint", "ObjectAttitude",
					"PropositionalAttitude");
			addRelationKeyed(key, "isa", "inScopeOfInterest", "BinaryPredicate");
			addRelationKeyed(key, "isa", "inScopeOfInterest", "IntentionalRelation");
			addRelationKeyed(key, "isa", "needs", "ObjectAttitude");
			addRelationKeyed(key, "subrelation", "needs", "inScopeOfInterest");
			addRelationKeyed(key, "isa", "wants", "ObjectAttitude");
			addRelationKeyed(key, "subrelation", "wants", "inScopeOfInterest");
			addRelationKeyed(key, "relatedInternalConcept", "wants", "desires");
			addRelationKeyed(key, "isa", "desires", "PropositionalAttitude");
			addRelationKeyed(key, "subrelation", "desires", "inScopeOfInterest");
			addRelationKeyed(key, "relatedInternalConcept", "desires", "wants");
			addRelationKeyed(key, "isa", "considers", "PropositionalAttitude");
			addRelationKeyed(key, "subrelation", "considers", "inScopeOfInterest");
			addRelationKeyed(key, "isa", "believes", "PropositionalAttitude");
			addRelationKeyed(key, "subrelation", "believes", "inScopeOfInterest");
			addRelationKeyed(key, "isa", "knows", "PropositionalAttitude");
			addRelationKeyed(key, "subrelation", "knows", "inScopeOfInterest");
			addRelationKeyed(key, "genls", "TernaryRelation", "Relation");
			addRelationKeyed(key, "isa", "TernaryRelation", "InheritableRelation");
			addRelationKeyed(key, "genls", "QuaternaryRelation", "Relation");
			addRelationKeyed(key, "isa", "QuaternaryRelation", "InheritableRelation");
			addRelationKeyed(key, "genls", "QuintaryRelation", "Relation");
			addRelationKeyed(key, "isa", "QuintaryRelation", "InheritableRelation");
			addRelationKeyed(key, "genls", "List", "Relation");
			addRelationKeyed(key, "genls", "UniqueList", "List");
			addRelationKeyed(key, "isa", "NullList", "List");
			addRelationKeyed(key, "isa", "ListFn", "Function");
			addRelationKeyed(key, "isa", "ListFn", "VariableArityRelation");
			addRelationKeyed(key, "isa", "ListFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "ListFn", "List");
			addRelationKeyed(key, "isa", "ListOrderFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "ListOrderFn", "PartialValuedRelation");
			addRelationKeyed(key, "range", "ListOrderFn", "Entity");
			addRelationKeyed(key, "isa", "ListLengthFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "ListLengthFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "ListLengthFn", "NonnegativeInteger");
			addRelationKeyed(key, "isa", "ListConcatenateFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "ListConcatenateFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "ListConcatenateFn", "List");
			addRelationKeyed(key, "isa", "inList", "BinaryPredicate");
			addRelationKeyed(key, "isa", "inList", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "inList", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "subList", "BinaryPredicate");
			addRelationKeyed(key, "isa", "subList", "PartialOrderingRelation");
			addRelationKeyed(key, "isa", "initialList", "BinaryPredicate");
			addRelationKeyed(key, "isa", "initialList", "PartialOrderingRelation");
			addRelationKeyed(key, "subrelation", "initialList", "subList");
			addRelationKeyed(key, "genls", "Predicate", "Relation");
			addRelationKeyed(key, "isa", "Predicate", "InheritableRelation");
			addRelationKeyed(key, "genls", "Function", "SingleValuedRelation");
			addRelationKeyed(key, "isa", "Function", "InheritableRelation");
			addRelationKeyed(key, "genls", "UnaryFunction", "Function");
			addRelationKeyed(key, "genls", "UnaryFunction", "BinaryRelation");
			addRelationKeyed(key, "isa", "UnaryFunction", "InheritableRelation");
			addRelationKeyed(key, "genls", "OneToOneFunction", "UnaryFunction");
			addRelationKeyed(key, "genls", "SequenceFunction", "OneToOneFunction");
			addRelationKeyed(key, "genls", "BinaryFunction", "Function");
			addRelationKeyed(key, "genls", "BinaryFunction", "TernaryRelation");
			addRelationKeyed(key, "isa", "BinaryFunction", "InheritableRelation");
			addRelationKeyed(key, "genls", "AssociativeFunction", "BinaryFunction");
			addRelationKeyed(key, "genls", "CommutativeFunction", "BinaryFunction");
			addRelationKeyed(key, "genls", "TernaryFunction", "Function");
			addRelationKeyed(key, "genls", "TernaryFunction", "QuaternaryRelation");
			addRelationKeyed(key, "isa", "TernaryFunction", "InheritableRelation");
			addRelationKeyed(key, "genls", "QuaternaryFunction", "Function");
			addRelationKeyed(key, "genls", "QuaternaryFunction", "QuintaryRelation");
			addRelationKeyed(key, "isa", "QuaternaryFunction", "InheritableRelation");
			addRelationKeyed(key, "genls", "ContinuousFunction", "Function");
			addRelationKeyed(key, "genls", "BinaryPredicate", "Predicate");
			addRelationKeyed(key, "genls", "BinaryPredicate", "BinaryRelation");
			addRelationKeyed(key, "isa", "BinaryPredicate", "InheritableRelation");
			addRelationKeyed(key, "genls", "TernaryPredicate", "Predicate");
			addRelationKeyed(key, "genls", "TernaryPredicate", "TernaryRelation");
			addRelationKeyed(key, "isa", "TernaryPredicate", "InheritableRelation");
			addRelationKeyed(key, "genls", "QuaternaryPredicate", "Predicate");
			addRelationKeyed(key, "genls", "QuaternaryPredicate",
					"QuaternaryRelation");
			addRelationKeyed(key, "isa", "QuaternaryPredicate", "InheritableRelation");
			addRelationKeyed(key, "genls", "QuintaryPredicate", "Predicate");
			addRelationKeyed(key, "genls", "QuintaryPredicate", "QuintaryRelation");
			addRelationKeyed(key, "isa", "QuintaryPredicate", "InheritableRelation");
			addRelationKeyed(key, "genls", "VariableArityRelation", "Relation");
			addRelationKeyed(key, "genls", "RelationExtendedToQuantities", "Relation");
			addRelationKeyed(key, "isa", "RelationExtendedToQuantities",
					"InheritableRelation");
			addRelationKeyed(key, "genls", "Proposition", "Abstract");
			addRelationKeyed(key, "isa", "closedOn", "BinaryPredicate");
			addRelationKeyed(key, "isa", "closedOn", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "reflexiveOn", "BinaryPredicate");
			addRelationKeyed(key, "isa", "reflexiveOn", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "irreflexiveOn", "BinaryPredicate");
			addRelationKeyed(key, "isa", "irreflexiveOn", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "partialOrderingOn", "BinaryPredicate");
			addRelationKeyed(key, "isa", "partialOrderingOn", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "totalOrderingOn", "BinaryPredicate");
			addRelationKeyed(key, "isa", "totalOrderingOn", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "trichotomizingOn", "BinaryPredicate");
			addRelationKeyed(key, "isa", "trichotomizingOn", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "equivalenceRelationOn", "BinaryPredicate");
			addRelationKeyed(key, "isa", "equivalenceRelationOn",
					"AsymmetricRelation");
			addRelationKeyed(key, "isa", "distributes", "BinaryPredicate");
			addRelationKeyed(key, "isa", "distributes", "BinaryRelation");
			addRelationKeyed(key, "isa", "causes", "BinaryPredicate");
			addRelationKeyed(key, "isa", "causes", "AsymmetricRelation");
			addRelationKeyed(key, "relatedInternalConcept", "causes",
					"causesSubclass");
			addRelationKeyed(key, "isa", "causesSubclass", "BinaryPredicate");
			addRelationKeyed(key, "isa", "causesSubclass", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "copy", "BinaryPredicate");
			addRelationKeyed(key, "isa", "copy", "EquivalenceRelation");
			addRelationKeyed(key, "isa", "time", "BinaryPredicate");
			addRelationKeyed(key, "isa", "time", "TemporalRelation");
			addRelationKeyed(key, "isa", "time", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "holdsDuring", "BinaryPredicate");
			addRelationKeyed(key, "isa", "holdsDuring", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "capability", "TernaryPredicate");
			addRelationKeyed(key, "isa", "exploits", "BinaryPredicate");
			addRelationKeyed(key, "isa", "exploits", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "hasPurpose", "BinaryPredicate");
			addRelationKeyed(key, "isa", "hasPurpose", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "hasPurposeForAgent", "TernaryPredicate");
			addRelationKeyed(key, "isa", "hasSkill", "BinaryPredicate");
			addRelationKeyed(key, "isa", "hasSkill", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "holdsRight", "BinaryPredicate");
			addRelationKeyed(key, "isa", "holdsRight", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "confersRight", "TernaryPredicate");
			addRelationKeyed(key, "isa", "holdsObligation", "BinaryPredicate");
			addRelationKeyed(key, "isa", "holdsObligation", "AsymmetricRelation");
			addRelationKeyed(key, "relatedInternalConcept", "holdsObligation",
					"holdsRight");
			addRelationKeyed(key, "isa", "confersObligation", "TernaryPredicate");
			addRelationKeyed(key, "relatedInternalConcept", "confersObligation",
					"confersRight");
			addRelationKeyed(key, "isa", "partlyLocated", "SpatialRelation");
			addRelationKeyed(key, "isa", "partlyLocated", "AntisymmetricRelation");
			addRelationKeyed(key, "isa", "partlyLocated", "BinaryPredicate");
			addRelationKeyed(key, "isa", "located", "AntisymmetricRelation");
			addRelationKeyed(key, "isa", "located", "TransitiveRelation");
			addRelationKeyed(key, "subrelation", "located", "partlyLocated");
			addRelationKeyed(key, "subrelation", "exactlyLocated", "located");
			addRelationKeyed(key, "isa", "between", "SpatialRelation");
			addRelationKeyed(key, "isa", "between", "TernaryPredicate");
			addRelationKeyed(key, "isa", "traverses", "BinaryPredicate");
			addRelationKeyed(key, "isa", "traverses", "SpatialRelation");
			addRelationKeyed(key, "subrelation", "crosses", "traverses");
			addRelationKeyed(key, "isa", "crosses", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "crosses", "TransitiveRelation");
			addRelationKeyed(key, "disjointRelation", "crosses", "connected");
			addRelationKeyed(key, "subrelation", "penetrates", "traverses");
			addRelationKeyed(key, "subrelation", "penetrates", "meetsSpatially");
			addRelationKeyed(key, "isa", "penetrates", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "penetrates", "IntransitiveRelation");
			addRelationKeyed(key, "isa", "WhereFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "WhereFn", "SpatialRelation");
			addRelationKeyed(key, "isa", "WhereFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "WhereFn", "Region");
			addRelationKeyed(key, "relatedInternalConcept", "WhereFn", "WhenFn");
			addRelationKeyed(key, "isa", "possesses", "BinaryPredicate");
			addRelationKeyed(key, "isa", "possesses", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "PropertyFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "PropertyFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "PropertyFn", "Set");
			addRelationKeyed(key, "isa", "precondition", "BinaryPredicate");
			addRelationKeyed(key, "isa", "precondition", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "precondition", "TransitiveRelation");
			addRelationKeyed(key, "isa", "inhibits", "BinaryPredicate");
			addRelationKeyed(key, "isa", "inhibits", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "prevents", "BinaryPredicate");
			addRelationKeyed(key, "isa", "prevents", "IrreflexiveRelation");
			addRelationKeyed(key, "relatedInternalConcept", "prevents", "inhibits");
			addRelationKeyed(key, "isa", "refers", "BinaryPredicate");
			addRelationKeyed(key, "subrelation", "names", "refers");
			addRelationKeyed(key, "subrelation", "uniqueIdentifier", "names");
			addRelationKeyed(key, "isa", "uniqueIdentifier", "SingleValuedRelation");
			addRelationKeyed(key, "subrelation", "represents", "refers");
			addRelationKeyed(key, "isa", "representsForAgent", "TernaryPredicate");
			addRelationKeyed(key, "isa", "representsInLanguage", "TernaryPredicate");
			addRelationKeyed(key, "subrelation", "equivalentContentClass",
					"subsumesContentClass");
			addRelationKeyed(key, "isa", "equivalentContentClass",
					"EquivalenceRelation");
			addRelationKeyed(key, "isa", "subsumesContentClass", "BinaryPredicate");
			addRelationKeyed(key, "isa", "subsumesContentClass",
					"PartialOrderingRelation");
			addRelationKeyed(key, "subrelation", "equivalentContentInstance",
					"subsumesContentInstance");
			addRelationKeyed(key, "isa", "equivalentContentInstance",
					"EquivalenceRelation");
			addRelationKeyed(key, "relatedInternalConcept",
					"equivalentContentInstance", "equivalentContentClass");
			addRelationKeyed(key, "isa", "subsumesContentInstance", "BinaryPredicate");
			addRelationKeyed(key, "isa", "subsumesContentInstance",
					"PartialOrderingRelation");
			addRelationKeyed(key, "relatedInternalConcept",
					"subsumesContentInstance", "subsumesContentClass");
			addRelationKeyed(key, "subrelation", "realization", "represents");
			addRelationKeyed(key, "isa", "realization", "AsymmetricRelation");
			addRelationKeyed(key, "relatedInternalConcept", "realization",
					"equivalentContentInstance");
			addRelationKeyed(key, "relatedInternalConcept", "realization",
					"containsInformation");
			addRelationKeyed(key, "isa", "expressedInLanguage", "BinaryPredicate");
			addRelationKeyed(key, "isa", "expressedInLanguage", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "subProposition", "BinaryPredicate");
			addRelationKeyed(key, "isa", "subProposition", "TransitiveRelation");
			addRelationKeyed(key, "isa", "subProposition", "IrreflexiveRelation");
			addRelationKeyed(key, "subrelation", "subPlan", "subProposition");
			addRelationKeyed(key, "isa", "subPlan", "TransitiveRelation");
			addRelationKeyed(key, "isa", "subPlan", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "uses", "BinaryPredicate");
			addRelationKeyed(key, "isa", "uses", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "MultiplicationFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "MultiplicationFn", "AssociativeFunction");
			addRelationKeyed(key, "isa", "MultiplicationFn", "CommutativeFunction");
			addRelationKeyed(key, "isa", "MultiplicationFn",
					"RelationExtendedToQuantities");
			addRelationKeyed(key, "isa", "MultiplicationFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "MultiplicationFn", "Quantity");
			addRelationKeyed(key, "isa", "AdditionFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "AdditionFn", "AssociativeFunction");
			addRelationKeyed(key, "isa", "AdditionFn", "CommutativeFunction");
			addRelationKeyed(key, "isa", "AdditionFn", "RelationExtendedToQuantities");
			addRelationKeyed(key, "isa", "AdditionFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "AdditionFn", "Quantity");
			addRelationKeyed(key, "isa", "SubtractionFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "SubtractionFn", "AssociativeFunction");
			addRelationKeyed(key, "isa", "SubtractionFn",
					"RelationExtendedToQuantities");
			addRelationKeyed(key, "isa", "SubtractionFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "SubtractionFn", "Quantity");
			addRelationKeyed(key, "isa", "DivisionFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "DivisionFn", "AssociativeFunction");
			addRelationKeyed(key, "isa", "DivisionFn", "RelationExtendedToQuantities");
			addRelationKeyed(key, "isa", "DivisionFn", "PartialValuedRelation");
			addRelationKeyed(key, "range", "DivisionFn", "Quantity");
			addRelationKeyed(key, "isa", "AbsoluteValueFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "AbsoluteValueFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "AbsoluteValueFn", "NonnegativeRealNumber");
			addRelationKeyed(key, "isa", "CeilingFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "CeilingFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "CeilingFn", "Integer");
			addRelationKeyed(key, "isa", "CosineFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "CosineFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "CosineFn", "RealNumber");
			addRelationKeyed(key, "isa", "DenominatorFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "DenominatorFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "DenominatorFn", "Integer");
			addRelationKeyed(key, "isa", "ExponentiationFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "ExponentiationFn",
					"RelationExtendedToQuantities");
			addRelationKeyed(key, "isa", "ExponentiationFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "ExponentiationFn", "Quantity");
			addRelationKeyed(key, "isa", "FloorFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "FloorFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "FloorFn", "Integer");
			addRelationKeyed(key, "isa", "GreatestCommonDivisorFn", "Function");
			addRelationKeyed(key, "isa", "GreatestCommonDivisorFn",
					"VariableArityRelation");
			addRelationKeyed(key, "isa", "GreatestCommonDivisorFn",
					"PartialValuedRelation");
			addRelationKeyed(key, "range", "GreatestCommonDivisorFn", "Integer");
			addRelationKeyed(key, "isa", "ImaginaryPartFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "ImaginaryPartFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "ImaginaryPartFn", "ImaginaryNumber");
			addRelationKeyed(key, "isa", "IntegerSquareRootFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "IntegerSquareRootFn",
					"PartialValuedRelation");
			addRelationKeyed(key, "range", "IntegerSquareRootFn",
					"NonnegativeInteger");
			addRelationKeyed(key, "isa", "LeastCommonMultipleFn", "Function");
			addRelationKeyed(key, "isa", "LeastCommonMultipleFn",
					"VariableArityRelation");
			addRelationKeyed(key, "range", "LeastCommonMultipleFn", "Integer");
			addRelationKeyed(key, "isa", "LogFn", "BinaryFunction");
			addRelationKeyed(key, "range", "LogFn", "RealNumber");
			addRelationKeyed(key, "isa", "MaxFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "MaxFn", "AssociativeFunction");
			addRelationKeyed(key, "isa", "MaxFn", "CommutativeFunction");
			addRelationKeyed(key, "isa", "MaxFn", "RelationExtendedToQuantities");
			addRelationKeyed(key, "isa", "MaxFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "MaxFn", "Quantity");
			addRelationKeyed(key, "isa", "MinFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "MinFn", "AssociativeFunction");
			addRelationKeyed(key, "isa", "MinFn", "CommutativeFunction");
			addRelationKeyed(key, "isa", "MinFn", "RelationExtendedToQuantities");
			addRelationKeyed(key, "isa", "MinFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "MinFn", "Quantity");
			addRelationKeyed(key, "isa", "NumeratorFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "NumeratorFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "NumeratorFn", "Integer");
			addRelationKeyed(key, "isa", "Pi", "PositiveRealNumber");
			addRelationKeyed(key, "isa", "NumberE", "PositiveRealNumber");
			addRelationKeyed(key, "isa", "RationalNumberFn", "UnaryFunction");
			addRelationKeyed(key, "range", "RationalNumberFn", "RationalNumber");
			addRelationKeyed(key, "isa", "RealNumberFn", "UnaryFunction");
			addRelationKeyed(key, "range", "RealNumberFn", "RealNumber");
			addRelationKeyed(key, "isa", "ReciprocalFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "ReciprocalFn",
					"RelationExtendedToQuantities");
			addRelationKeyed(key, "isa", "ReciprocalFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "ReciprocalFn", "Quantity");
			addRelationKeyed(key, "isa", "RemainderFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "RemainderFn",
					"RelationExtendedToQuantities");
			addRelationKeyed(key, "isa", "RemainderFn", "PartialValuedRelation");
			addRelationKeyed(key, "range", "RemainderFn", "Quantity");
			addRelationKeyed(key, "isa", "RoundFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "RoundFn", "RelationExtendedToQuantities");
			addRelationKeyed(key, "isa", "RoundFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "RoundFn", "Quantity");
			addRelationKeyed(key, "isa", "SignumFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "SignumFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "SignumFn", "Integer");
			addRelationKeyed(key, "isa", "SineFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "SineFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "SineFn", "RealNumber");
			addRelationKeyed(key, "isa", "SquareRootFn", "UnaryFunction");
			addRelationKeyed(key, "range", "SquareRootFn", "Number");
			addRelationKeyed(key, "isa", "TangentFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "TangenFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "TangentFn", "RealNumber");
			addRelationKeyed(key, "isa", "identityElement", "BinaryPredicate");
			addRelationKeyed(key, "isa", "identityElement", "AsymmetricRelation");
			addRelationKeyed(key, "identityElement", "MultiplicationFn", "1");
			addRelationKeyed(key, "identityElement", "AdditionFn", "0");
			addRelationKeyed(key, "identityElement", "SubtractionFn", "0");
			addRelationKeyed(key, "identityElement", "DivisionFn", "1");
			addRelationKeyed(key, "isa", "SuccessorFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "SuccessorFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "SuccessorFn", "Integer");
			addRelationKeyed(key, "isa", "PredecessorFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "PredecessorFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "PredecessorFn", "Integer");
			addRelationKeyed(key, "subrelation", "subset", "subclass");
			addRelationKeyed(key, "isa", "element", "BinaryPredicate");
			addRelationKeyed(key, "isa", "element", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "element", "IntransitiveRelation");
			addRelationKeyed(key, "subrelation", "element", "instance");
			addRelationKeyed(key, "isa", "UnionFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "UnionFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "UnionFn", "SetOrClass");
			addRelationKeyed(key, "isa", "IntersectionFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "IntersectionFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "IntersectionFn", "SetOrClass");
			addRelationKeyed(key, "isa", "RelativeComplementFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "RelativeComplementFn",
					"TotalValuedRelation");
			addRelationKeyed(key, "range", "RelativeComplementFn", "SetOrClass");
			addRelationKeyed(key, "isa", "ComplementFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "ComplementFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "ComplementFn", "SetOrClass");
			addRelationKeyed(key, "isa", "GeneralizedUnionFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "GeneralizedUnionFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "GeneralizedUnionFn", "SetOrClass");
			addRelationKeyed(key, "isa", "GeneralizedIntersectionFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "GeneralizedIntersectionFn",
					"TotalValuedRelation");
			addRelationKeyed(key, "range", "GeneralizedIntersectionFn", "SetOrClass");
			addRelationKeyed(key, "isa", "CardinalityFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "CardinalityFn", "TotalValuedRelation");
			addRelationKeyed(key, "isa", "CardinalityFn", "AsymmetricRelation");
			addRelationKeyed(key, "range", "CardinalityFn", "Number");
			addRelationKeyed(key, "genls", "NullSet", "SetOrClass");
			addRelationKeyed(key, "genls", "NonNullSet", "SetOrClass");
			addRelationKeyed(key, "genls", "FiniteSet", "Set");
			addRelationKeyed(key, "genls", "PairwiseDisjointClass", "SetOrClass");
			addRelationKeyed(key, "genls", "MutuallyDisjointClass", "SetOrClass");
			addRelationKeyed(key, "isa", "KappaFn", "BinaryFunction");
			addRelationKeyed(key, "range", "KappaFn", "Class");
			addRelationKeyed(key, "genls", "UnitOfMeasure", "PhysicalQuantity");
			addRelationKeyed(key, "genls", "SystemeInternationalUnit",
					"UnitOfMeasure");
			addRelationKeyed(key, "genls", "LengthMeasure", "ConstantQuantity");
			addRelationKeyed(key, "genls", "MassMeasure", "ConstantQuantity");
			addRelationKeyed(key, "genls", "AreaMeasure", "ConstantQuantity");
			addRelationKeyed(key, "genls", "VolumeMeasure", "ConstantQuantity");
			addRelationKeyed(key, "genls", "TemperatureMeasure", "ConstantQuantity");
			addRelationKeyed(key, "genls", "CurrencyMeasure", "ConstantQuantity");
			addRelationKeyed(key, "genls", "AngleMeasure", "ConstantQuantity");
			addRelationKeyed(key, "genls", "PlaneAngleMeasure", "AngleMeasure");
			addRelationKeyed(key, "genls", "SolidAngleMeasure", "AngleMeasure");
			addRelationKeyed(key, "disjoint", "SolidAngleMeasure",
					"PlaneAngleMeasure");
			addRelationKeyed(key, "isa", "MeasureFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "MeasureFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "MeasureFn", "ConstantQuantity");
			addRelationKeyed(key, "isa", "KiloFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "KiloFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "KiloFn", "UnitOfMeasure");
			addRelationKeyed(key, "isa", "MegaFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "MegaFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "MegaFn", "UnitOfMeasure");
			addRelationKeyed(key, "isa", "GigaFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "GigaFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "GigaFn", "UnitOfMeasure");
			addRelationKeyed(key, "isa", "TeraFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "TeraFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "TeraFn", "UnitOfMeasure");
			addRelationKeyed(key, "isa", "MilliFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "MilliFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "MilliFn", "UnitOfMeasure");
			addRelationKeyed(key, "isa", "MicroFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "MicroFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "MicroFn", "UnitOfMeasure");
			addRelationKeyed(key, "isa", "NanoFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "NanoFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "NanoFn", "UnitOfMeasure");
			addRelationKeyed(key, "isa", "PicoFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "PicoFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "PicoFn", "UnitOfMeasure");
			addRelationKeyed(key, "isa", "IntervalFn", "BinaryFunction");
			addRelationKeyed(key, "rangeSubclass", "IntervalFn", "ConstantQuantity");
			addRelationKeyed(key, "relatedInternalConcept", "IntervalFn",
					"RecurrentTimeIntervalFn");
			addRelationKeyed(key, "isa", "MagnitudeFn", "UnaryFunction");
			addRelationKeyed(key, "range", "MagnitudeFn", "RealNumber");
			addRelationKeyed(key, "isa", "PerFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "PerFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "PerFn", "FunctionQuantity");
			addRelationKeyed(key, "subrelation", "DensityFn", "PerFn");
			addRelationKeyed(key, "isa", "DensityFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "DensityFn", "FunctionQuantity");
			addRelationKeyed(key, "subrelation", "SpeedFn", "PerFn");
			addRelationKeyed(key, "isa", "SpeedFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "SpeedFn", "FunctionQuantity");
			addRelationKeyed(key, "isa", "VelocityFn", "QuaternaryFunction");
			addRelationKeyed(key, "isa", "VelocityFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "VelocityFn", "FunctionQuantity");
			addRelationKeyed(key, "genls", "Meter", "LengthMeasure");
			addRelationKeyed(key, "isa", "Meter", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Gram", "MassMeasure");
			addRelationKeyed(key, "isa", "Gram", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "SecondDuration", "TimeDuration");
			addRelationKeyed(key, "isa", "SecondDuration", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "KelvinDegree", "TemperatureMeasure");
			addRelationKeyed(key, "isa", "KelvinDegree", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Liter", "VolumeMeasure");
			addRelationKeyed(key, "isa", "Liter", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Centimeter", "LengthMeasure");
			addRelationKeyed(key, "isa", "Centimeter", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Radian", "PlaneAngleMeasure");
			addRelationKeyed(key, "isa", "Radian", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Steradian", "SolidAngleMeasure");
			addRelationKeyed(key, "isa", "Steradian", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "Hertz", "TimeDependentQuantity");
			addRelationKeyed(key, "isa", "Hertz", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "CelsiusDegree", "TemperatureMeasure");
			addRelationKeyed(key, "isa", "CelsiusDegree", "SystemeInternationalUnit");
			addRelationKeyed(key, "genls", "DayDuration", "TimeDuration");
			addRelationKeyed(key, "isa", "DayDuration", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "HourDuration", "TimeDuration");
			addRelationKeyed(key, "isa", "HourDuration", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "MinuteDuration", "TimeDuration");
			addRelationKeyed(key, "isa", "MinuteDuration", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "WeekDuration", "TimeDuration");
			addRelationKeyed(key, "isa", "WeekDuration", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "YearDuration", "TimeDuration");
			addRelationKeyed(key, "isa", "YearDuration", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "FootLength", "LengthMeasure");
			addRelationKeyed(key, "isa", "FootLength", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Inch", "LengthMeasure");
			addRelationKeyed(key, "isa", "Inch", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Mile", "LengthMeasure");
			addRelationKeyed(key, "isa", "Mile", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "UnitedStatesGallon", "VolumeMeasure");
			addRelationKeyed(key, "isa", "UnitedStatesGallon", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Quart", "VolumeMeasure");
			addRelationKeyed(key, "isa", "Quart", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Pint", "VolumeMeasure");
			addRelationKeyed(key, "isa", "Pint", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Cup", "VolumeMeasure");
			addRelationKeyed(key, "isa", "Cup", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Ounce", "VolumeMeasure");
			addRelationKeyed(key, "isa", "Ounce", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "PoundMass", "MassMeasure");
			addRelationKeyed(key, "isa", "PoundMass", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "RankineDegree", "TemperatureMeasure");
			addRelationKeyed(key, "isa", "RankineDegree", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "FahrenheitDegree", "TemperatureMeasure");
			addRelationKeyed(key, "isa", "FahrenheitDegree", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Calorie", "FunctionQuantity");
			addRelationKeyed(key, "isa", "Calorie", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "BritishThermalUnit", "FunctionQuantity");
			addRelationKeyed(key, "isa", "BritishThermalUnit", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "AngularDegree", "PlaneAngleMeasure");
			addRelationKeyed(key, "isa", "AngularDegree", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "UnitedStatesDollar", "CurrencyMeasure");
			addRelationKeyed(key, "isa", "UnitedStatesDollar", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "UnitedStatesCent", "CurrencyMeasure");
			addRelationKeyed(key, "isa", "UnitedStatesCent", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "EuroDollar", "CurrencyMeasure");
			addRelationKeyed(key, "isa", "EuroDollar", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "EuroCent", "CurrencyMeasure");
			addRelationKeyed(key, "isa", "EuroCent", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "InformationMeasure", "ConstantQuantity");
			addRelationKeyed(key, "genls", "Bit", "InformationMeasure");
			addRelationKeyed(key, "isa", "Bit", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "Byte", "InformationMeasure");
			addRelationKeyed(key, "isa", "Byte", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "KiloByte", "InformationMeasure");
			addRelationKeyed(key, "isa", "KiloByte", "UnitOfMeasure");
			addRelationKeyed(key, "genls", "MegaByte", "InformationMeasure");
			addRelationKeyed(key, "isa", "MegaByte", "UnitOfMeasure");
			addRelationKeyed(key, "isa", "measure", "BinaryPredicate");
			addRelationKeyed(key, "isa", "measure", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "age", "SingleValuedRelation");
			addRelationKeyed(key, "subrelation", "age", "measure");
			addRelationKeyed(key, "subrelation", "length", "measure");
			addRelationKeyed(key, "isa", "width", "SingleValuedRelation");
			addRelationKeyed(key, "subrelation", "width", "length");
			addRelationKeyed(key, "subrelation", "height", "length");
			addRelationKeyed(key, "subrelation", "diameter", "width");
			addRelationKeyed(key, "isa", "distance", "SingleValuedRelation");
			addRelationKeyed(key, "isa", "distance", "SpatialRelation");
			addRelationKeyed(key, "isa", "distance", "TernaryPredicate");
			addRelationKeyed(key, "subrelation", "altitude", "distance");
			addRelationKeyed(key, "isa", "altitude", "SingleValuedRelation");
			addRelationKeyed(key, "subrelation", "depth", "distance");
			addRelationKeyed(key, "isa", "depth", "SingleValuedRelation");
			addRelationKeyed(key, "isa", "larger", "BinaryPredicate");
			addRelationKeyed(key, "isa", "larger", "SpatialRelation");
			addRelationKeyed(key, "isa", "larger", "TransitiveRelation");
			addRelationKeyed(key, "isa", "larger", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "smaller", "BinaryPredicate");
			addRelationKeyed(key, "isa", "smaller", "SpatialRelation");
			addRelationKeyed(key, "isa", "smaller", "TransitiveRelation");
			addRelationKeyed(key, "isa", "smaller", "IrreflexiveRelation");
			addRelationKeyed(key, "inverse", "smaller", "larger");
			addRelationKeyed(key, "isa", "monetaryValue", "SingleValuedRelation");
			addRelationKeyed(key, "subrelation", "monetaryValue", "measure");
			addRelationKeyed(key, "isa", "WealthFn", "UnaryFunction");
			addRelationKeyed(key, "range", "WealthFn", "CurrencyMeasure");
			addRelationKeyed(key, "isa", "PositiveInfinity", "TimePoint");
			addRelationKeyed(key, "isa", "NegativeInfinity", "TimePoint");
			addRelationKeyed(key, "isa", "duration", "BinaryPredicate");
			addRelationKeyed(key, "isa", "duration", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "duration", "TotalValuedRelation");
			addRelationKeyed(key, "isa", "frequency", "BinaryPredicate");
			addRelationKeyed(key, "isa", "frequency", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "temporalPart", "BinaryPredicate");
			addRelationKeyed(key, "isa", "temporalPart", "TemporalRelation");
			addRelationKeyed(key, "isa", "temporalPart", "PartialOrderingRelation");
			addRelationKeyed(key, "isa", "BeginFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "BeginFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "BeginFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "BeginFn", "TimePoint");
			addRelationKeyed(key, "isa", "EndFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "EndFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "EndFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "EndFn", "TimePoint");
			addRelationKeyed(key, "subrelation", "starts", "temporalPart");
			addRelationKeyed(key, "isa", "starts", "TemporalRelation");
			addRelationKeyed(key, "isa", "starts", "TransitiveRelation");
			addRelationKeyed(key, "isa", "starts", "IrreflexiveRelation");
			addRelationKeyed(key, "subrelation", "finishes", "temporalPart");
			addRelationKeyed(key, "isa", "finishes", "TemporalRelation");
			addRelationKeyed(key, "isa", "finishes", "TransitiveRelation");
			addRelationKeyed(key, "isa", "finishes", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "before", "TemporalRelation");
			addRelationKeyed(key, "isa", "before", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "before", "TransitiveRelation");
			addRelationKeyed(key, "subrelation", "before", "beforeOrEqual");
			addRelationKeyed(key, "relatedInternalConcept", "before", "earlier");
			addRelationKeyed(key, "isa", "beforeOrEqual", "BinaryPredicate");
			addRelationKeyed(key, "isa", "beforeOrEqual", "TemporalRelation");
			addRelationKeyed(key, "isa", "beforeOrEqual", "PartialOrderingRelation");
			addRelationKeyed(key, "isa", "temporallyBetween", "TemporalRelation");
			addRelationKeyed(key, "isa", "temporallyBetween", "TernaryPredicate");
			addRelationKeyed(key, "subrelation", "temporallyBetween",
					"temporallyBetweenOrEqual");
			addRelationKeyed(key, "isa", "temporallyBetweenOrEqual",
					"TemporalRelation");
			addRelationKeyed(key, "isa", "temporallyBetweenOrEqual",
					"TernaryPredicate");
			addRelationKeyed(key, "isa", "overlapsTemporally", "BinaryPredicate");
			addRelationKeyed(key, "isa", "overlapsTemporally", "TemporalRelation");
			addRelationKeyed(key, "isa", "overlapsTemporally", "ReflexiveRelation");
			addRelationKeyed(key, "isa", "overlapsTemporally", "SymmetricRelation");
			addRelationKeyed(key, "subrelation", "during", "temporalPart");
			addRelationKeyed(key, "isa", "during", "TransitiveRelation");
			addRelationKeyed(key, "isa", "during", "IrreflexiveRelation");
			addRelationKeyed(key, "subrelation", "during", "overlapsTemporally");
			addRelationKeyed(key, "isa", "meetsTemporally", "BinaryPredicate");
			addRelationKeyed(key, "isa", "meetsTemporally", "TemporalRelation");
			addRelationKeyed(key, "isa", "meetsTemporally", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "meetsTemporally", "IntransitiveRelation");
			addRelationKeyed(key, "isa", "earlier", "BinaryPredicate");
			addRelationKeyed(key, "isa", "earlier", "TemporalRelation");
			addRelationKeyed(key, "isa", "earlier", "TransitiveRelation");
			addRelationKeyed(key, "isa", "earlier", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "cooccur", "BinaryPredicate");
			addRelationKeyed(key, "isa", "cooccur", "TemporalRelation");
			addRelationKeyed(key, "isa", "cooccur", "EquivalenceRelation");
			addRelationKeyed(key, "isa", "TimeIntervalFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "TimeIntervalFn", "TemporalRelation");
			addRelationKeyed(key, "range", "TimeIntervalFn", "TimeInterval");
			addRelationKeyed(key, "isa", "RecurrentTimeIntervalFn",
					"TemporalRelation");
			addRelationKeyed(key, "isa", "RecurrentTimeIntervalFn", "BinaryFunction");
			addRelationKeyed(key, "rangeSubclass", "RecurrentTimeIntervalFn",
					"TimeInterval");
			addRelationKeyed(key, "isa", "WhenFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "WhenFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "WhenFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "WhenFn", "TimeInterval");
			addRelationKeyed(key, "isa", "PastFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "PastFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "PastFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "PastFn", "TimeInterval");
			addRelationKeyed(key, "isa", "ImmediatePastFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "ImmediatePastFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "ImmediatePastFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "ImmediatePastFn", "TimeInterval");
			addRelationKeyed(key, "isa", "FutureFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "FutureFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "FutureFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "FutureFn", "TimeInterval");
			addRelationKeyed(key, "isa", "ImmediateFutureFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "ImmediateFutureFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "ImmediateFutureFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "ImmediateFutureFn", "TimeInterval");
			addRelationKeyed(key, "isa", "date", "BinaryPredicate");
			addRelationKeyed(key, "isa", "date", "SingleValuedRelation");
			addRelationKeyed(key, "isa", "date", "AsymmetricRelation");
			addRelationKeyed(key, "subrelation", "date", "time");
			addRelationKeyed(key, "isa", "YearFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "YearFn", "UnaryFunction");
			addRelationKeyed(key, "rangeSubclass", "YearFn", "Year");
			addRelationKeyed(key, "isa", "MonthFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "MonthFn", "BinaryFunction");
			addRelationKeyed(key, "rangeSubclass", "MonthFn", "Month");
			addRelationKeyed(key, "isa", "DayFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "DayFn", "BinaryFunction");
			addRelationKeyed(key, "rangeSubclass", "DayFn", "Day");
			addRelationKeyed(key, "isa", "HourFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "HourFn", "BinaryFunction");
			addRelationKeyed(key, "rangeSubclass", "HourFn", "Hour");
			addRelationKeyed(key, "isa", "MinuteFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "MinuteFn", "BinaryFunction");
			addRelationKeyed(key, "rangeSubclass", "MinuteFn", "Minute");
			addRelationKeyed(key, "isa", "SecondFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "SecondFn", "BinaryFunction");
			addRelationKeyed(key, "rangeSubclass", "SecondFn", "Second");
			addRelationKeyed(key, "genls", "Year", "TimeInterval");
			addRelationKeyed(key, "relatedInternalConcept", "Year", "YearFn");
			addRelationKeyed(key, "relatedInternalConcept", "Year", "YearDuration");
			addRelationKeyed(key, "genls", "LeapYear", "Year");
			addRelationKeyed(key, "genls", "Month", "TimeInterval");
			addRelationKeyed(key, "relatedInternalConcept", "Month", "MonthFn");
			addRelationKeyed(key, "genls", "January", "Month");
			addRelationKeyed(key, "genls", "February", "Month");
			addRelationKeyed(key, "genls", "March", "Month");
			addRelationKeyed(key, "genls", "April", "Month");
			addRelationKeyed(key, "genls", "May", "Month");
			addRelationKeyed(key, "genls", "June", "Month");
			addRelationKeyed(key, "genls", "July", "Month");
			addRelationKeyed(key, "genls", "August", "Month");
			addRelationKeyed(key, "genls", "September", "Month");
			addRelationKeyed(key, "genls", "October", "Month");
			addRelationKeyed(key, "genls", "November", "Month");
			addRelationKeyed(key, "genls", "December", "Month");
			addRelationKeyed(key, "genls", "Day", "TimeInterval");
			addRelationKeyed(key, "relatedInternalConcept", "Day", "DayFn");
			addRelationKeyed(key, "relatedInternalConcept", "Day", "DayDuration");
			addRelationKeyed(key, "genls", "Monday", "Day");
			addRelationKeyed(key, "genls", "Tuesday", "Day");
			addRelationKeyed(key, "genls", "Wednesday", "Day");
			addRelationKeyed(key, "genls", "Thursday", "Day");
			addRelationKeyed(key, "genls", "Friday", "Day");
			addRelationKeyed(key, "genls", "Saturday", "Day");
			addRelationKeyed(key, "genls", "Sunday", "Day");
			addRelationKeyed(key, "genls", "Week", "TimeInterval");
			addRelationKeyed(key, "genls", "Hour", "TimeInterval");
			addRelationKeyed(key, "relatedInternalConcept", "Hour", "HourFn");
			addRelationKeyed(key, "relatedInternalConcept", "Hour", "HourDuration");
			addRelationKeyed(key, "genls", "Minute", "TimeInterval");
			addRelationKeyed(key, "relatedInternalConcept", "Minute", "MinuteFn");
			addRelationKeyed(key, "relatedInternalConcept", "Minute",
					"MinuteDuration");
			addRelationKeyed(key, "genls", "Second", "TimeInterval");
			addRelationKeyed(key, "relatedInternalConcept", "Second",
					"SecondDuration");
			addRelationKeyed(key, "relatedInternalConcept", "Second", "SecondFn");
			addRelationKeyed(key, "isa", "TemporalCompositionFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "TemporalCompositionFn", "BinaryFunction");
			addRelationKeyed(key, "rangeSubclass", "TemporalCompositionFn",
					"TimeInterval");
			addRelationKeyed(key, "isa", "connected", "BinaryPredicate");
			addRelationKeyed(key, "isa", "connected", "SpatialRelation");
			addRelationKeyed(key, "isa", "connected", "ReflexiveRelation");
			addRelationKeyed(key, "isa", "connected", "SymmetricRelation");
			addRelationKeyed(key, "isa", "connects", "SpatialRelation");
			addRelationKeyed(key, "isa", "connects", "TernaryPredicate");
			addRelationKeyed(key, "subrelation", "meetsSpatially", "connected");
			addRelationKeyed(key, "isa", "meetsSpatially", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "meetsSpatially", "SymmetricRelation");
			addRelationKeyed(key, "disjointRelation", "meetsSpatially",
					"overlapsSpatially");
			addRelationKeyed(key, "subrelation", "overlapsSpatially", "connected");
			addRelationKeyed(key, "isa", "overlapsSpatially", "ReflexiveRelation");
			addRelationKeyed(key, "isa", "overlapsSpatially", "SymmetricRelation");
			addRelationKeyed(key, "isa", "overlapsPartially", "SymmetricRelation");
			addRelationKeyed(key, "isa", "overlapsPartially", "IrreflexiveRelation");
			addRelationKeyed(key, "subrelation", "overlapsPartially",
					"overlapsSpatially");
			addRelationKeyed(key, "subrelation", "superficialPart", "part");
			addRelationKeyed(key, "isa", "superficialPart", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "superficialPart", "TransitiveRelation");
			addRelationKeyed(key, "isa", "surface", "AsymmetricRelation");
			addRelationKeyed(key, "subrelation", "surface", "superficialPart");
			addRelationKeyed(key, "subrelation", "interiorPart", "part");
			addRelationKeyed(key, "isa", "interiorPart", "AsymmetricRelation");
			addRelationKeyed(key, "subrelation", "bottom", "superficialPart");
			addRelationKeyed(key, "subrelation", "top", "superficialPart");
			addRelationKeyed(key, "subrelation", "side", "superficialPart");
			addRelationKeyed(key, "isa", "MereologicalSumFn", "SpatialRelation");
			addRelationKeyed(key, "isa", "MereologicalSumFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "MereologicalSumFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "MereologicalSumFn", "Object");
			addRelationKeyed(key, "relatedInternalConcept", "MereologicalSumFn",
					"MereologicalProductFn");
			addRelationKeyed(key, "relatedInternalConcept", "MereologicalSumFn",
					"MereologicalDifferenceFn");
			addRelationKeyed(key, "isa", "MereologicalProductFn", "SpatialRelation");
			addRelationKeyed(key, "isa", "MereologicalProductFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "MereologicalProductFn",
					"TotalValuedRelation");
			addRelationKeyed(key, "range", "MereologicalProductFn", "Object");
			addRelationKeyed(key, "relatedInternalConcept", "MereologicalProductFn",
					"MereologicalDifferenceFn");
			addRelationKeyed(key, "isa", "MereologicalDifferenceFn",
					"SpatialRelation");
			addRelationKeyed(key, "isa", "MereologicalDifferenceFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "MereologicalDifferenceFn",
					"TotalValuedRelation");
			addRelationKeyed(key, "range", "MereologicalDifferenceFn", "Object");
			addRelationKeyed(key, "isa", "hole", "BinaryPredicate");
			addRelationKeyed(key, "isa", "hole", "SpatialRelation");
			addRelationKeyed(key, "isa", "hole", "AsymmetricRelation");
			addRelationKeyed(key, "genls", "Hole", "Region");
			addRelationKeyed(key, "isa", "HoleHostFn", "SpatialRelation");
			addRelationKeyed(key, "isa", "HoleHostFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "HoleHostFn", "TotalValuedRelation");
			addRelationKeyed(key, "isa", "HoleHostFn", "AsymmetricRelation");
			addRelationKeyed(key, "range", "HoleHostFn", "Object");
			addRelationKeyed(key, "isa", "Fillable", "ShapeAttribute");
			addRelationKeyed(key, "subrelation", "partiallyFills", "located");
			addRelationKeyed(key, "isa", "partiallyFills", "SpatialRelation");
			addRelationKeyed(key, "isa", "partiallyFills", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "properlyFills", "AsymmetricRelation");
			addRelationKeyed(key, "subrelation", "properlyFills", "partiallyFills");
			addRelationKeyed(key, "isa", "completelyFills", "AsymmetricRelation");
			addRelationKeyed(key, "subrelation", "completelyFills", "partiallyFills");
			addRelationKeyed(key, "isa", "fills", "AsymmetricRelation");
			addRelationKeyed(key, "subrelation", "fills", "completelyFills");
			addRelationKeyed(key, "subrelation", "fills", "properlyFills");
			addRelationKeyed(key, "relatedInternalConcept", "fills", "Fillable");
			addRelationKeyed(key, "isa", "HoleSkinFn", "SpatialRelation");
			addRelationKeyed(key, "isa", "HoleSkinFn", "UnaryFunction");
			addRelationKeyed(key, "isa", "HoleSkinFn", "TotalValuedRelation");
			addRelationKeyed(key, "isa", "HoleSkinFn", "AsymmetricRelation");
			addRelationKeyed(key, "range", "HoleSkinFn", "Object");
			addRelationKeyed(key, "isa", "subProcess", "BinaryPredicate");
			addRelationKeyed(key, "isa", "subProcess", "PartialOrderingRelation");
			addRelationKeyed(key, "genls", "BiologicalProcess", "InternalChange");
			addRelationKeyed(key, "genls", "PhysiologicProcess", "BiologicalProcess");
			addRelationKeyed(key, "genls", "AutonomicProcess", "PhysiologicProcess");
			addRelationKeyed(key, "disjoint", "AutonomicProcess",
					"IntentionalProcess");
			addRelationKeyed(key, "genls", "OrganOrTissueProcess", "AutonomicProcess");
			addRelationKeyed(key, "disjoint", "OrganOrTissueProcess",
					"OrganismProcess");
			addRelationKeyed(key, "genls", "OrganismProcess", "PhysiologicProcess");
			addRelationKeyed(key, "genls", "Birth", "OrganismProcess");
			addRelationKeyed(key, "genls", "Death", "OrganismProcess");
			addRelationKeyed(key, "genls", "Breathing", "OrganismProcess");
			addRelationKeyed(key, "genls", "Breathing", "AutonomicProcess");
			addRelationKeyed(key, "genls", "Ingesting", "OrganismProcess");
			addRelationKeyed(key, "genls", "Eating", "Ingesting");
			addRelationKeyed(key, "genls", "Drinking", "Ingesting");
			addRelationKeyed(key, "genls", "Digesting", "OrganismProcess");
			addRelationKeyed(key, "genls", "Digesting", "AutonomicProcess");
			addRelationKeyed(key, "genls", "Growth", "AutonomicProcess");
			addRelationKeyed(key, "genls", "Replication", "OrganismProcess");
			addRelationKeyed(key, "genls", "SexualReproduction", "Replication");
			addRelationKeyed(key, "disjoint", "SexualReproduction",
					"AsexualReproduction");
			addRelationKeyed(key, "genls", "AsexualReproduction", "Replication");
			addRelationKeyed(key, "genls", "PsychologicalProcess",
					"BiologicalProcess");
			addRelationKeyed(key, "genls", "PathologicProcess", "BiologicalProcess");
			addRelationKeyed(key, "disjoint", "PathologicProcess",
					"PhysiologicProcess");
			addRelationKeyed(key, "genls", "Injuring", "PathologicProcess");
			addRelationKeyed(key, "genls", "Injuring", "Damaging");
			addRelationKeyed(key, "genls", "IntentionalProcess", "Process");
			addRelationKeyed(key, "genls", "IntentionalPsychologicalProcess",
					"IntentionalProcess");
			addRelationKeyed(key, "genls", "IntentionalPsychologicalProcess",
					"PsychologicalProcess");
			addRelationKeyed(key, "genls", "RecreationOrExercise",
					"IntentionalProcess");
			addRelationKeyed(key, "genls", "OrganizationalProcess",
					"IntentionalProcess");
			addRelationKeyed(key, "genls", "Election", "OrganizationalProcess");
			addRelationKeyed(key, "genls", "ReligiousProcess",
					"OrganizationalProcess");
			addRelationKeyed(key, "genls", "JoiningAnOrganization",
					"OrganizationalProcess");
			addRelationKeyed(key, "genls", "LeavingAnOrganization",
					"OrganizationalProcess");
			addRelationKeyed(key, "disjoint", "LeavingAnOrganization",
					"JoiningAnOrganization");
			addRelationKeyed(key, "genls", "Graduation", "LeavingAnOrganization");
			addRelationKeyed(key, "genls", "Matriculation", "JoiningAnOrganization");
			addRelationKeyed(key, "genls", "Hiring", "JoiningAnOrganization");
			addRelationKeyed(key, "genls", "TerminatingEmployment",
					"LeavingAnOrganization");
			addRelationKeyed(key, "genls", "PoliticalProcess",
					"OrganizationalProcess");
			addRelationKeyed(key, "genls", "JudicialProcess", "PoliticalProcess");
			addRelationKeyed(key, "genls", "LegalDecision", "JudicialProcess");
			addRelationKeyed(key, "genls", "LegalDecision", "Declaring");
			addRelationKeyed(key, "genls", "MilitaryProcess", "PoliticalProcess");
			addRelationKeyed(key, "genls", "RegulatoryProcess", "Guiding");
			addRelationKeyed(key, "genls", "Managing", "OrganizationalProcess");
			addRelationKeyed(key, "genls", "Managing", "Guiding");
			addRelationKeyed(key, "genls", "Planning",
					"IntentionalPsychologicalProcess");
			addRelationKeyed(key, "genls", "Designing",
					"IntentionalPsychologicalProcess");
			addRelationKeyed(key, "genls", "Interpreting",
					"IntentionalPsychologicalProcess");
			addRelationKeyed(key, "genls", "QuantityChange", "InternalChange");
			addRelationKeyed(key, "genls", "Increasing", "QuantityChange");
			addRelationKeyed(key, "relatedInternalConcept", "Increasing", "Putting");
			addRelationKeyed(key, "genls", "Heating", "Increasing");
			addRelationKeyed(key, "disjoint", "Heating", "Cooling");
			addRelationKeyed(key, "genls", "Decreasing", "QuantityChange");
			addRelationKeyed(key, "relatedInternalConcept", "Decreasing", "Removing");
			addRelationKeyed(key, "genls", "Cooling", "Decreasing");
			addRelationKeyed(key, "genls", "Motion", "Process");
			addRelationKeyed(key, "isa", "path", "CaseRole");
			addRelationKeyed(key, "genls", "BodyMotion", "Motion");
			addRelationKeyed(key, "genls", "Vocalizing", "RadiatingSound");
			addRelationKeyed(key, "genls", "Vocalizing", "BodyMotion");
			addRelationKeyed(key, "genls", "Speaking", "LinguisticCommunication");
			addRelationKeyed(key, "genls", "Speaking", "Vocalizing");
			addRelationKeyed(key, "genls", "Singing", "Speaking");
			addRelationKeyed(key, "genls", "Singing", "Music");
			addRelationKeyed(key, "genls", "Ambulating", "BodyMotion");
			addRelationKeyed(key, "genls", "Ambulating", "Translocation");
			addRelationKeyed(key, "genls", "Walking", "Ambulating");
			addRelationKeyed(key, "genls", "Running", "Ambulating");
			addRelationKeyed(key, "genls", "GeologicalProcess", "Motion");
			addRelationKeyed(key, "disjoint", "GeologicalProcess",
					"IntentionalProcess");
			addRelationKeyed(key, "genls", "WeatherProcess", "Motion");
			addRelationKeyed(key, "disjoint", "WeatherProcess", "IntentionalProcess");
			addRelationKeyed(key, "genls", "Precipitation", "WeatherProcess");
			addRelationKeyed(key, "genls", "LiquidMotion", "Motion");
			addRelationKeyed(key, "genls", "GasMotion", "Motion");
			addRelationKeyed(key, "genls", "Wind", "GasMotion");
			addRelationKeyed(key, "genls", "DirectionChange", "Motion");
			addRelationKeyed(key, "genls", "Transfer", "Translocation");
			addRelationKeyed(key, "genls", "Carrying", "Transfer");
			addRelationKeyed(key, "genls", "Removing", "Transfer");
			addRelationKeyed(key, "genls", "Putting", "Transfer");
			addRelationKeyed(key, "genls", "Dressing", "Covering");
			addRelationKeyed(key, "genls", "Inserting", "Putting");
			addRelationKeyed(key, "genls", "Injecting", "Inserting");
			addRelationKeyed(key, "genls", "Substituting", "Transfer");
			addRelationKeyed(key, "genls", "Substituting", "DualObjectProcess");
			addRelationKeyed(key, "genls", "Impelling", "Transfer");
			addRelationKeyed(key, "genls", "Shooting", "Impelling");
			addRelationKeyed(key, "genls", "Touching", "Transfer");
			addRelationKeyed(key, "subrelation", "grasps", "meetsSpatially");
			addRelationKeyed(key, "genls", "Grabbing", "Touching");
			addRelationKeyed(key, "genls", "Grabbing", "Attaching");
			addRelationKeyed(key, "genls", "Impacting", "Touching");
			addRelationKeyed(key, "genls", "Translocation", "Motion");
			addRelationKeyed(key, "genls", "Transportation", "Translocation");
			addRelationKeyed(key, "relatedInternalConcept", "Transportation",
					"TransportationDevice");
			addRelationKeyed(key, "genls", "Guiding", "IntentionalProcess");
			addRelationKeyed(key, "genls", "Driving", "Guiding");
			addRelationKeyed(key, "genls", "EducationalProcess", "Guiding");
			addRelationKeyed(key, "genls", "ChangeOfPossession", "SocialInteraction");
			addRelationKeyed(key, "relatedInternalConcept", "ChangeOfPossession",
					"possesses");
			addRelationKeyed(key, "genls", "Giving", "ChangeOfPossession");
			addRelationKeyed(key, "genls", "Funding", "Giving");
			addRelationKeyed(key, "genls", "UnilateralGiving", "Giving");
			addRelationKeyed(key, "genls", "Lending", "Giving");
			addRelationKeyed(key, "genls", "GivingBack", "Giving");
			addRelationKeyed(key, "genls", "Getting", "ChangeOfPossession");
			addRelationKeyed(key, "genls", "UnilateralGetting", "Getting");
			addRelationKeyed(key, "relatedInternalConcept", "UnilateralGetting",
					"UnilateralGiving");
			addRelationKeyed(key, "genls", "Borrowing", "Getting");
			addRelationKeyed(key, "genls", "Transaction", "ChangeOfPossession");
			addRelationKeyed(key, "genls", "Transaction", "DualObjectProcess");
			addRelationKeyed(key, "genls", "FinancialTransaction", "Transaction");
			addRelationKeyed(key, "genls", "CommercialService",
					"FinancialTransaction");
			addRelationKeyed(key, "genls", "Betting", "FinancialTransaction");
			addRelationKeyed(key, "genls", "Buying", "FinancialTransaction");
			addRelationKeyed(key, "relatedInternalConcept", "Buying", "Selling");
			addRelationKeyed(key, "genls", "Selling", "FinancialTransaction");
			addRelationKeyed(key, "genls", "Learning",
					"IntentionalPsychologicalProcess");
			addRelationKeyed(key, "genls", "Discovering",
					"IntentionalPsychologicalProcess");
			addRelationKeyed(key, "genls", "Classifying",
					"IntentionalPsychologicalProcess");
			addRelationKeyed(key, "genls", "Reasoning",
					"IntentionalPsychologicalProcess");
			addRelationKeyed(key, "genls", "Selecting",
					"IntentionalPsychologicalProcess");
			addRelationKeyed(key, "genls", "Deciding", "Selecting");
			addRelationKeyed(key, "genls", "Judging", "Selecting");
			addRelationKeyed(key, "genls", "Voting", "Selecting");
			addRelationKeyed(key, "genls", "Comparing",
					"IntentionalPsychologicalProcess");
			addRelationKeyed(key, "genls", "Comparing", "DualObjectProcess");
			addRelationKeyed(key, "genls", "Calculating",
					"IntentionalPsychologicalProcess");
			addRelationKeyed(key, "genls", "Measuring", "Calculating");
			addRelationKeyed(key, "genls", "Counting", "Calculating");
			addRelationKeyed(key, "genls", "Predicting",
					"IntentionalPsychologicalProcess");
			addRelationKeyed(key, "genls", "Remembering", "PsychologicalProcess");
			addRelationKeyed(key, "genls", "Keeping", "IntentionalProcess");
			addRelationKeyed(key, "genls", "Confining", "Keeping");
			addRelationKeyed(key, "genls", "Maintaining", "IntentionalProcess");
			addRelationKeyed(key, "genls", "Repairing", "IntentionalProcess");
			addRelationKeyed(key, "relatedInternalConcept", "Repairing",
					"Maintaining");
			addRelationKeyed(key, "genls", "TherapeuticProcess", "Repairing");
			addRelationKeyed(key, "genls", "Surgery", "TherapeuticProcess");
			addRelationKeyed(key, "genls", "Damaging", "InternalChange");
			addRelationKeyed(key, "disjoint", "Damaging", "Repairing");
			addRelationKeyed(key, "genls", "Destruction", "Damaging");
			addRelationKeyed(key, "genls", "Killing", "Destruction");
			addRelationKeyed(key, "genls", "Poking", "IntentionalProcess");
			addRelationKeyed(key, "genls", "Cutting", "Poking");
			addRelationKeyed(key, "genls", "Attaching", "DualObjectProcess");
			addRelationKeyed(key, "disjoint", "Attaching", "Detaching");
			addRelationKeyed(key, "relatedInternalConcept", "Attaching", "Putting");
			addRelationKeyed(key, "genls", "Detaching", "DualObjectProcess");
			addRelationKeyed(key, "genls", "Ungrasping", "Detaching");
			addRelationKeyed(key, "genls", "Combining", "DualObjectProcess");
			addRelationKeyed(key, "genls", "Separating", "DualObjectProcess");
			addRelationKeyed(key, "disjoint", "Separating", "Combining");
			addRelationKeyed(key, "genls", "ChemicalSynthesis", "ChemicalProcess");
			addRelationKeyed(key, "genls", "ChemicalDecomposition", "Separating");
			addRelationKeyed(key, "genls", "Combustion", "ChemicalDecomposition");
			addRelationKeyed(key, "genls", "InternalChange", "Process");
			addRelationKeyed(key, "genls", "SurfaceChange", "InternalChange");
			addRelationKeyed(key, "genls", "Coloring", "SurfaceChange");
			addRelationKeyed(key, "genls", "ContentDevelopment", "IntentionalProcess");
			addRelationKeyed(key, "genls", "Reading", "ContentDevelopment");
			addRelationKeyed(key, "relatedInternalConcept", "Reading", "Interpreting");
			addRelationKeyed(key, "genls", "Writing", "ContentDevelopment");
			addRelationKeyed(key, "genls", "Encoding", "Writing");
			addRelationKeyed(key, "genls", "Decoding", "Writing");
			addRelationKeyed(key, "disjoint", "Decoding", "Encoding");
			addRelationKeyed(key, "genls", "Translating", "ContentDevelopment");
			addRelationKeyed(key, "genls", "Translating", "DualObjectProcess");
			addRelationKeyed(key, "genls", "Wetting", "Putting");
			addRelationKeyed(key, "genls", "Drying", "Removing");
			addRelationKeyed(key, "genls", "Creation", "InternalChange");
			addRelationKeyed(key, "relatedInternalConcept", "Creation", "Destruction");
			addRelationKeyed(key, "genls", "Making", "Creation");
			addRelationKeyed(key, "genls", "Making", "IntentionalProcess");
			addRelationKeyed(key, "genls", "Constructing", "Making");
			addRelationKeyed(key, "genls", "Manufacture", "Making");
			addRelationKeyed(key, "genls", "Publication", "Manufacture");
			addRelationKeyed(key, "genls", "Publication", "ContentDevelopment");
			addRelationKeyed(key, "genls", "Cooking", "Making");
			addRelationKeyed(key, "genls", "Pursuing", "IntentionalProcess");
			addRelationKeyed(key, "genls", "Investigating",
					"IntentionalPsychologicalProcess");
			addRelationKeyed(key, "genls", "Experimenting", "Investigating");
			addRelationKeyed(key, "genls", "DiagnosticProcess", "Investigating");
			addRelationKeyed(key, "genls", "SocialInteraction", "IntentionalProcess");
			addRelationKeyed(key, "genls", "Pretending", "SocialInteraction");
			addRelationKeyed(key, "genls", "Communication", "SocialInteraction");
			addRelationKeyed(key, "relatedInternalConcept", "Communication",
					"ContentDevelopment");
			addRelationKeyed(key, "genls", "Disseminating", "Communication");
			addRelationKeyed(key, "genls", "Demonstrating", "Disseminating");
			addRelationKeyed(key, "subrelation", "attends", "experiencer");
			addRelationKeyed(key, "genls", "Gesture", "Communication");
			addRelationKeyed(key, "genls", "Gesture", "BodyMotion");
			addRelationKeyed(key, "genls", "Expressing", "Communication");
			addRelationKeyed(key, "genls", "LinguisticCommunication", "Communication");
			addRelationKeyed(key, "genls", "Stating", "LinguisticCommunication");
			addRelationKeyed(key, "genls", "Supposing", "LinguisticCommunication");
			addRelationKeyed(key, "genls", "Directing", "LinguisticCommunication");
			addRelationKeyed(key, "genls", "Ordering", "Directing");
			addRelationKeyed(key, "genls", "Requesting", "Directing");
			addRelationKeyed(key, "genls", "Questioning", "Directing");
			addRelationKeyed(key, "genls", "Committing", "LinguisticCommunication");
			addRelationKeyed(key, "genls", "Offering", "Committing");
			addRelationKeyed(key, "genls", "Declaring", "LinguisticCommunication");
			addRelationKeyed(key, "genls", "Wedding", "Declaring");
			addRelationKeyed(key, "genls", "Naming", "Declaring");
			addRelationKeyed(key, "genls", "Cooperation", "SocialInteraction");
			addRelationKeyed(key, "genls", "Meeting", "SocialInteraction");
			addRelationKeyed(key, "genls", "Contest", "SocialInteraction");
			addRelationKeyed(key, "genls", "ViolentContest", "Contest");
			addRelationKeyed(key, "genls", "War", "ViolentContest");
			addRelationKeyed(key, "genls", "Battle", "ViolentContest");
			addRelationKeyed(key, "genls", "Game", "Contest");
			addRelationKeyed(key, "genls", "Game", "RecreationOrExercise");
			addRelationKeyed(key, "genls", "Sport", "Game");
			addRelationKeyed(key, "genls", "LegalAction", "Contest");
			addRelationKeyed(key, "genls", "Maneuver", "IntentionalProcess");
			addRelationKeyed(key, "genls", "Attack", "Maneuver");
			addRelationKeyed(key, "genls", "DefensiveManeuver", "Maneuver");
			addRelationKeyed(key, "genls", "Perception", "PsychologicalProcess");
			addRelationKeyed(key, "genls", "Seeing", "Perception");
			addRelationKeyed(key, "genls", "Looking", "Seeing");
			addRelationKeyed(key, "genls", "Looking", "IntentionalProcess");
			addRelationKeyed(key, "genls", "Smelling", "Perception");
			addRelationKeyed(key, "genls", "Tasting", "Perception");
			addRelationKeyed(key, "genls", "Hearing", "Perception");
			addRelationKeyed(key, "genls", "Listening", "Hearing");
			addRelationKeyed(key, "genls", "Listening", "IntentionalProcess");
			addRelationKeyed(key, "genls", "TactilePerception", "Perception");
			addRelationKeyed(key, "genls", "RadiatingLight", "Radiating");
			addRelationKeyed(key, "genls", "RadiatingSound", "Radiating");
			addRelationKeyed(key, "genls", "Music", "RadiatingSound");
			addRelationKeyed(key, "genls", "RadiatingElectromagnetic", "Radiating");
			addRelationKeyed(key, "genls", "RadiatingNuclear", "Radiating");
			addRelationKeyed(key, "genls", "StateChange", "InternalChange");
			addRelationKeyed(key, "genls", "Melting", "StateChange");
			addRelationKeyed(key, "genls", "Boiling", "StateChange");
			addRelationKeyed(key, "genls", "Condensing", "StateChange");
			addRelationKeyed(key, "genls", "Freezing", "StateChange");
			addRelationKeyed(key, "genls", "AstronomicalBody", "Region");
			addRelationKeyed(key, "disjoint", "AstronomicalBody", "GeographicArea");
			addRelationKeyed(key, "genls", "GeographicArea", "Region");
			addRelationKeyed(key, "isa", "geographicSubregion", "BinaryPredicate");
			addRelationKeyed(key, "isa", "geographicSubregion", "TransitiveRelation");
			addRelationKeyed(key, "isa", "geographicSubregion", "AsymmetricRelation");
			addRelationKeyed(key, "subrelation", "geographicSubregion", "properPart");
			addRelationKeyed(key, "subrelation", "geographicSubregion", "located");
			addRelationKeyed(key, "genls", "GeopoliticalArea", "GeographicArea");
			addRelationKeyed(key, "genls", "GeopoliticalArea", "Agent");
			addRelationKeyed(key, "genls", "WaterArea", "GeographicArea");
			addRelationKeyed(key, "genls", "SaltWaterArea", "WaterArea");
			addRelationKeyed(key, "disjoint", "SaltWaterArea", "FreshWaterArea");
			addRelationKeyed(key, "genls", "FreshWaterArea", "WaterArea");
			addRelationKeyed(key, "genls", "StreamWaterArea", "WaterArea");
			addRelationKeyed(key, "disjoint", "StreamWaterArea", "StaticWaterArea");
			addRelationKeyed(key, "genls", "StaticWaterArea", "WaterArea");
			addRelationKeyed(key, "genls", "LandArea", "GeographicArea");
			addRelationKeyed(key, "genls", "ShoreArea", "LandArea");
			addRelationKeyed(key, "genls", "Continent", "LandArea");
			addRelationKeyed(key, "genls", "Island", "LandArea");
			addRelationKeyed(key, "genls", "Nation", "GeopoliticalArea");
			addRelationKeyed(key, "genls", "Nation", "LandArea");
			addRelationKeyed(key, "genls", "StateOrProvince", "GeopoliticalArea");
			addRelationKeyed(key, "genls", "StateOrProvince", "LandArea");
			addRelationKeyed(key, "genls", "City", "GeopoliticalArea");
			addRelationKeyed(key, "genls", "City", "LandArea");
			addRelationKeyed(key, "genls", "Transitway", "Region");
			addRelationKeyed(key, "genls", "Transitway", "SelfConnectedObject");
			addRelationKeyed(key, "genls", "LandTransitway", "Transitway");
			addRelationKeyed(key, "genls", "LandTransitway", "LandArea");
			addRelationKeyed(key, "genls", "Roadway", "LandTransitway");
			addRelationKeyed(key, "genls", "Water", "CompoundSubstance");
			addRelationKeyed(key, "genls", "Mineral", "Mixture");
			addRelationKeyed(key, "isa", "developmentalForm", "BinaryPredicate");
			addRelationKeyed(key, "isa", "developmentalForm", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "developmentalForm", "TransitiveRelation");
			addRelationKeyed(key, "subrelation", "developmentalForm", "attribute");
			addRelationKeyed(key, "genls", "OrganicObject", "CorpuscularObject");
			addRelationKeyed(key, "genls", "Organism", "OrganicObject");
			addRelationKeyed(key, "genls", "Organism", "Agent");
			addRelationKeyed(key, "isa", "inhabits", "BinaryPredicate");
			addRelationKeyed(key, "isa", "inhabits", "AsymmetricRelation");
			addRelationKeyed(key, "subrelation", "home", "inhabits");
			addRelationKeyed(key, "genls", "Plant", "Organism");
			addRelationKeyed(key, "genls", "FloweringPlant", "Plant");
			addRelationKeyed(key, "genls", "NonFloweringPlant", "Plant");
			addRelationKeyed(key, "disjoint", "NonFloweringPlant", "FloweringPlant");
			addRelationKeyed(key, "genls", "Alga", "NonFloweringPlant");
			addRelationKeyed(key, "genls", "Fungus", "NonFloweringPlant");
			addRelationKeyed(key, "genls", "Moss", "NonFloweringPlant");
			addRelationKeyed(key, "genls", "Fern", "NonFloweringPlant");
			addRelationKeyed(key, "genls", "Animal", "Organism");
			addRelationKeyed(key, "genls", "Microorganism", "Organism");
			addRelationKeyed(key, "genls", "Bacterium", "Microorganism");
			addRelationKeyed(key, "genls", "Virus", "Microorganism");
			addRelationKeyed(key, "genls", "Vertebrate", "Animal");
			addRelationKeyed(key, "genls", "Invertebrate", "Animal");
			addRelationKeyed(key, "genls", "Worm", "Invertebrate");
			addRelationKeyed(key, "genls", "Mollusk", "Invertebrate");
			addRelationKeyed(key, "genls", "Arthropod", "Invertebrate");
			addRelationKeyed(key, "genls", "Arachnid", "Arthropod");
			addRelationKeyed(key, "genls", "Myriapod", "Arthropod");
			addRelationKeyed(key, "genls", "Insect", "Arthropod");
			addRelationKeyed(key, "genls", "Crustacean", "Arthropod");
			addRelationKeyed(key, "genls", "ColdBloodedVertebrate", "Vertebrate");
			addRelationKeyed(key, "genls", "WarmBloodedVertebrate", "Vertebrate");
			addRelationKeyed(key, "disjoint", "WarmBloodedVertebrate",
					"ColdBloodedVertebrate");
			addRelationKeyed(key, "genls", "Amphibian", "ColdBloodedVertebrate");
			addRelationKeyed(key, "genls", "Bird", "WarmBloodedVertebrate");
			addRelationKeyed(key, "disjoint", "Bird", "Mammal");
			addRelationKeyed(key, "genls", "Fish", "ColdBloodedVertebrate");
			addRelationKeyed(key, "genls", "Mammal", "WarmBloodedVertebrate");
			addRelationKeyed(key, "genls", "AquaticMammal", "Mammal");
			addRelationKeyed(key, "genls", "HoofedMammal", "Mammal");
			addRelationKeyed(key, "genls", "Marsupial", "Mammal");
			addRelationKeyed(key, "genls", "Carnivore", "Mammal");
			addRelationKeyed(key, "genls", "Canine", "Carnivore");
			addRelationKeyed(key, "disjoint", "Canine", "Feline");
			addRelationKeyed(key, "genls", "Feline", "Carnivore");
			addRelationKeyed(key, "genls", "Rodent", "Mammal");
			addRelationKeyed(key, "genls", "Primate", "Mammal");
			addRelationKeyed(key, "genls", "Ape", "Primate");
			addRelationKeyed(key, "genls", "Monkey", "Primate");
			addRelationKeyed(key, "genls", "Hominid", "Primate");
			addRelationKeyed(key, "genls", "Human", "Hominid");
			addRelationKeyed(key, "genls", "Human", "CognitiveAgent");
			addRelationKeyed(key, "genls", "Man", "Human");
			addRelationKeyed(key, "genls", "Woman", "Human");
			addRelationKeyed(key, "genls", "Reptile", "ColdBloodedVertebrate");
			addRelationKeyed(key, "genls", "BiologicallyActiveSubstance", "Substance");
			addRelationKeyed(key, "genls", "Nutrient", "BiologicallyActiveSubstance");
			addRelationKeyed(key, "genls", "Protein", "Nutrient");
			addRelationKeyed(key, "genls", "Carbohydrate", "Nutrient");
			addRelationKeyed(key, "genls", "Vitamin", "Nutrient");
			addRelationKeyed(key, "genls", "LiquidMixture", "Mixture");
			addRelationKeyed(key, "=>", "", "");
			addRelationKeyed(key, "genls", "Suspension", "LiquidMixture");
			addRelationKeyed(key, "genls", "GasMixture", "Mixture");
			addRelationKeyed(key, "disjoint", "GasMixture", "LiquidMixture");
			addRelationKeyed(key, "=>", "", "");
			addRelationKeyed(key, "genls", "Cloud", "GasMixture");
			addRelationKeyed(key, "genls", "Smoke", "Cloud");
			addRelationKeyed(key, "genls", "WaterCloud", "Cloud");
			addRelationKeyed(key, "genls", "Air", "GasMixture");
			addRelationKeyed(key, "genls", "BodySubstance", "Mixture");
			addRelationKeyed(key, "genls", "AnimalSubstance", "BodySubstance");
			addRelationKeyed(key, "genls", "PlantSubstance", "BodySubstance");
			addRelationKeyed(key, "genls", "Blood", "BodySubstance");
			addRelationKeyed(key, "genls", "Food", "SelfConnectedObject");
			addRelationKeyed(key, "genls", "Meat", "Food");
			addRelationKeyed(key, "genls", "Beverage", "Food");
			addRelationKeyed(key, "genls", "AnatomicalStructure", "OrganicObject");
			addRelationKeyed(key, "genls", "AbnormalAnatomicalStructure",
					"AnatomicalStructure");
			addRelationKeyed(key, "genls", "BodyPart", "AnatomicalStructure");
			addRelationKeyed(key, "genls", "AnimalAnatomicalStructure",
					"AnatomicalStructure");
			addRelationKeyed(key, "genls", "PlantAnatomicalStructure",
					"AnatomicalStructure");
			addRelationKeyed(key, "genls", "ReproductiveBody", "BodyPart");
			addRelationKeyed(key, "genls", "Egg", "ReproductiveBody");
			addRelationKeyed(key, "genls", "Egg", "AnimalAnatomicalStructure");
			addRelationKeyed(key, "genls", "Seed", "ReproductiveBody");
			addRelationKeyed(key, "genls", "Seed", "PlantAnatomicalStructure");
			addRelationKeyed(key, "genls", "Pollen", "ReproductiveBody");
			addRelationKeyed(key, "genls", "Pollen", "PlantAnatomicalStructure");
			addRelationKeyed(key, "genls", "FruitOrVegetable",
					"PlantAnatomicalStructure");
			addRelationKeyed(key, "genls", "FruitOrVegetable", "ReproductiveBody");
			addRelationKeyed(key, "genls", "Spore", "ReproductiveBody");
			addRelationKeyed(key, "genls", "Spore", "PlantAnatomicalStructure");
			addRelationKeyed(key, "genls", "BodyCovering", "BodyPart");
			addRelationKeyed(key, "genls", "BodyJunction", "BodyPart");
			addRelationKeyed(key, "genls", "BodyVessel", "BodyPart");
			addRelationKeyed(key, "genls", "Cell", "BodyPart");
			addRelationKeyed(key, "genls", "Organ", "BodyPart");
			addRelationKeyed(key, "genls", "Tissue", "BodySubstance");
			addRelationKeyed(key, "genls", "Bone", "Tissue");
			addRelationKeyed(key, "genls", "Muscle", "Tissue");
			addRelationKeyed(key, "genls", "FatTissue", "Tissue");
			addRelationKeyed(key, "genls", "Noun", "Word");
			addRelationKeyed(key, "genls", "Verb", "Word");
			addRelationKeyed(key, "genls", "Adjective", "Word");
			addRelationKeyed(key, "genls", "Adverb", "Word");
			addRelationKeyed(key, "genls", "ParticleWord", "Word");
			addRelationKeyed(key, "genls", "Morpheme", "LinguisticExpression");
			addRelationKeyed(key, "genls", "Phrase", "LinguisticExpression");
			addRelationKeyed(key, "genls", "VerbPhrase", "Phrase");
			addRelationKeyed(key, "genls", "NounPhrase", "Phrase");
			addRelationKeyed(key, "disjoint", "NounPhrase", "VerbPhrase");
			addRelationKeyed(key, "genls", "PrepositionalPhrase", "Phrase");
			addRelationKeyed(key, "genls", "Text", "LinguisticExpression");
			addRelationKeyed(key, "genls", "Text", "Artifact");
			addRelationKeyed(key, "genls", "FactualText", "Text");
			addRelationKeyed(key, "disjoint", "FactualText", "FictionalText");
			addRelationKeyed(key, "genls", "FictionalText", "Text");
			addRelationKeyed(key, "genls", "Sentence", "LinguisticExpression");
			addRelationKeyed(key, "isa", "authors", "BinaryPredicate");
			addRelationKeyed(key, "isa", "authors", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "editor", "BinaryPredicate");
			addRelationKeyed(key, "isa", "editor", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "publishes", "BinaryPredicate");
			addRelationKeyed(key, "isa", "publishes", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "EditionFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "EditionFn", "PartialValuedRelation");
			addRelationKeyed(key, "rangeSubclass", "EditionFn",
					"ContentBearingObject");
			addRelationKeyed(key, "isa", "SeriesVolumeFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "SeriesVolumeFn", "PartialValuedRelation");
			addRelationKeyed(key, "rangeSubclass", "SeriesVolumeFn", "Text");
			addRelationKeyed(key, "isa", "PeriodicalIssueFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "PeriodicalIssueFn", "PartialValuedRelation");
			addRelationKeyed(key, "rangeSubclass", "PeriodicalIssueFn", "Periodical");
			addRelationKeyed(key, "genls", "Book", "Text");
			addRelationKeyed(key, "genls", "Summary", "Text");
			addRelationKeyed(key, "genls", "Series", "Text");
			addRelationKeyed(key, "genls", "Periodical", "Series");
			addRelationKeyed(key, "genls", "Article", "Text");
			addRelationKeyed(key, "disjoint", "Article", "Book");
			addRelationKeyed(key, "genls", "Certificate", "Text");
			addRelationKeyed(key, "genls", "FinancialInstrument", "Certificate");
			addRelationKeyed(key, "genls", "Currency", "FinancialInstrument");
			addRelationKeyed(key, "genls", "CurrencyBill", "Currency");
			addRelationKeyed(key, "genls", "CurrencyCoin", "Currency");
			addRelationKeyed(key, "genls", "Patent", "Certificate");
			addRelationKeyed(key, "genls", "Molecule", "CompoundSubstance");
			addRelationKeyed(key, "genls", "Artifact", "CorpuscularObject");
			addRelationKeyed(key, "genls", "Product", "Artifact");
			addRelationKeyed(key, "isa", "version", "BinaryPredicate");
			addRelationKeyed(key, "isa", "version", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "version", "TransitiveRelation");
			addRelationKeyed(key, "genls", "StationaryArtifact", "Artifact");
			addRelationKeyed(key, "genls", "Building", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Room", "StationaryArtifact");
			addRelationKeyed(key, "disjoint", "Room", "Building");
			addRelationKeyed(key, "genls", "Residence", "StationaryArtifact");
			addRelationKeyed(key, "genls", "PermanentResidence", "Residence");
			addRelationKeyed(key, "genls", "TemporaryResidence", "Residence");
			addRelationKeyed(key, "genls", "ResidentialBuilding", "Building");
			addRelationKeyed(key, "genls", "ResidentialBuilding", "Residence");
			addRelationKeyed(key, "genls", "Hotel", "ResidentialBuilding");
			addRelationKeyed(key, "genls", "Hotel", "TemporaryResidence");
			addRelationKeyed(key, "genls", "Hotel", "CommercialAgent");
			addRelationKeyed(key, "genls", "SingleFamilyResidence",
					"PermanentResidence");
			addRelationKeyed(key, "genls", "ArtWork", "Artifact");
			addRelationKeyed(key, "genls", "RepresentationalArtWork", "ArtWork");
			addRelationKeyed(key, "genls", "RepresentationalArtWork", "Icon");
			addRelationKeyed(key, "genls", "Fabric", "Artifact");
			addRelationKeyed(key, "disjoint", "Fabric", "StationaryArtifact");
			addRelationKeyed(key, "genls", "Clothing", "Artifact");
			addRelationKeyed(key, "disjoint", "Clothing", "StationaryArtifact");
			addRelationKeyed(key, "isa", "wears", "BinaryPredicate");
			addRelationKeyed(key, "genls", "Device", "Artifact");
			addRelationKeyed(key, "genls", "MusicalInstrument", "Device");
			addRelationKeyed(key, "genls", "TransportationDevice", "Device");
			addRelationKeyed(key, "genls", "Vehicle", "TransportationDevice");
			addRelationKeyed(key, "genls", "MeasuringDevice", "Device");
			addRelationKeyed(key, "genls", "AttachingDevice", "Device");
			addRelationKeyed(key, "genls", "Weapon", "Device");
			addRelationKeyed(key, "genls", "Machine", "Device");
			addRelationKeyed(key, "genls", "EngineeringComponent", "Device");
			addRelationKeyed(key, "subrelation", "engineeringSubcomponent",
					"properPart");
			addRelationKeyed(key, "isa", "connectedEngineeringComponents",
					"SymmetricRelation");
			addRelationKeyed(key, "isa", "connectedEngineeringComponents",
					"IrreflexiveRelation");
			addRelationKeyed(key, "subrelation", "connectedEngineeringComponents",
					"connected");
			addRelationKeyed(key, "genls", "EngineeringConnection",
					"EngineeringComponent");
			addRelationKeyed(key, "subrelation", "connectsEngineeringComponents",
					"connects");
			addRelationKeyed(key, "genls", "Corporation", "CommercialAgent");
			addRelationKeyed(key, "genls", "Corporation", "Organization");
			addRelationKeyed(key, "genls", "Manufacturer", "Corporation");
			addRelationKeyed(key, "genls", "MercantileOrganization", "Corporation");
			addRelationKeyed(key, "genls", "GroupOfPeople", "Group");
			addRelationKeyed(key, "genls", "SocialUnit", "Group");
			addRelationKeyed(key, "isa", "ImmediateFamilyFn", "UnaryFunction");
			addRelationKeyed(key, "range", "ImmediateFamilyFn", "FamilyGroup");
			addRelationKeyed(key, "isa", "familyRelation", "BinaryPredicate");
			addRelationKeyed(key, "subrelation", "ancestor", "familyRelation");
			addRelationKeyed(key, "isa", "ancestor", "TransitiveRelation");
			addRelationKeyed(key, "isa", "ancestor", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "parent", "BinaryPredicate");
			addRelationKeyed(key, "subrelation", "parent", "familyRelation");
			addRelationKeyed(key, "isa", "parent", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "parent", "IntransitiveRelation");
			addRelationKeyed(key, "isa", "mother", "SingleValuedRelation");
			addRelationKeyed(key, "subrelation", "mother", "parent");
			addRelationKeyed(key, "isa", "father", "SingleValuedRelation");
			addRelationKeyed(key, "subrelation", "father", "parent");
			addRelationKeyed(key, "subrelation", "daughter", "parent");
			addRelationKeyed(key, "subrelation", "son", "parent");
			addRelationKeyed(key, "isa", "sibling", "BinaryPredicate");
			addRelationKeyed(key, "subrelation", "sibling", "familyRelation");
			addRelationKeyed(key, "isa", "sibling", "SymmetricRelation");
			addRelationKeyed(key, "isa", "sibling", "IrreflexiveRelation");
			addRelationKeyed(key, "subrelation", "brother", "sibling");
			addRelationKeyed(key, "isa", "brother", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "brother", "TransitiveRelation");
			addRelationKeyed(key, "subrelation", "sister", "sibling");
			addRelationKeyed(key, "isa", "sister", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "sister", "TransitiveRelation");
			addRelationKeyed(key, "isa", "legalRelation", "BinaryPredicate");
			addRelationKeyed(key, "isa", "legalRelation", "SymmetricRelation");
			addRelationKeyed(key, "subrelation", "spouse", "legalRelation");
			addRelationKeyed(key, "isa", "spouse", "IrreflexiveRelation");
			addRelationKeyed(key, "isa", "spouse", "SymmetricRelation");
			addRelationKeyed(key, "subrelation", "husband", "spouse");
			addRelationKeyed(key, "isa", "husband", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "husband", "IrreflexiveRelation");
			addRelationKeyed(key, "inverse", "husband", "wife");
			addRelationKeyed(key, "subrelation", "wife", "spouse");
			addRelationKeyed(key, "isa", "wife", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "wife", "IrreflexiveRelation");
			addRelationKeyed(key, "genls", "BeliefGroup", "Group");
			addRelationKeyed(key, "genls", "Organization", "CognitiveAgent");
			addRelationKeyed(key, "isa", "employs", "BinaryPredicate");
			addRelationKeyed(key, "genls", "PoliticalOrganization", "Organization");
			addRelationKeyed(key, "genls", "GovernmentOrganization", "Organization");
			addRelationKeyed(key, "genls", "Government", "GovernmentOrganization");
			addRelationKeyed(key, "isa", "GovernmentFn", "UnaryFunction");
			addRelationKeyed(key, "range", "GovernmentFn", "Government");
			addRelationKeyed(key, "genls", "MilitaryOrganization",
					"GovernmentOrganization");
			addRelationKeyed(key, "genls", "PoliceOrganization",
					"GovernmentOrganization");
			addRelationKeyed(key, "genls", "JudicialOrganization", "Organization");
			addRelationKeyed(key, "genls", "ReligiousOrganization", "BeliefGroup");
			addRelationKeyed(key, "subrelation", "subOrganization", "subCollection");
			addRelationKeyed(key, "isa", "subOrganization", "PartialOrderingRelation");
			addRelationKeyed(key, "isa", "citizen", "BinaryPredicate");
			addRelationKeyed(key, "genls", "FieldOfStudy", "Proposition");
			addRelationKeyed(key, "genls", "Procedure", "Proposition");
			addRelationKeyed(key, "genls", "ComputerProgram", "Procedure");
			addRelationKeyed(key, "genls", "Plan", "Procedure");
			addRelationKeyed(key, "genls", "Argument", "Proposition");
			addRelationKeyed(key, "genls", "DeductiveArgument", "Argument");
			addRelationKeyed(key, "genls", "ValidDeductiveArgument",
					"DeductiveArgument");
			addRelationKeyed(key, "genls", "InvalidDeductiveArgument",
					"DeductiveArgument");
			addRelationKeyed(key, "genls", "Explanation", "DeductiveArgument");
			addRelationKeyed(key, "genls", "InductiveArgument", "Argument");
			addRelationKeyed(key, "isa", "premise", "BinaryPredicate");
			addRelationKeyed(key, "isa", "premise", "TotalValuedRelation");
			addRelationKeyed(key, "isa", "PremisesFn", "UnaryFunction");
			addRelationKeyed(key, "domain", "PremisesFn", "Argument");
			addRelationKeyed(key, "range", "PremisesFn", "Proposition");
			addRelationKeyed(key, "isa", "conclusion", "BinaryPredicate");
			addRelationKeyed(key, "isa", "conclusion", "SingleValuedRelation");
			addRelationKeyed(key, "isa", "conclusion", "TotalValuedRelation");
			addRelationKeyed(key, "isa", "consistent", "BinaryPredicate");
			addRelationKeyed(key, "isa", "consistent", "SymmetricRelation");
			addRelationKeyed(key, "isa", "orientation", "SpatialRelation");
			addRelationKeyed(key, "isa", "orientation", "TernaryPredicate");
			addRelationKeyed(key, "isa", "direction", "CaseRole");
			addRelationKeyed(key, "isa", "faces", "BinaryPredicate");
			addRelationKeyed(key, "genls", "TruthValue", "RelationalAttribute");
			addRelationKeyed(key, "isa", "True", "TruthValue");
			addRelationKeyed(key, "isa", "False", "TruthValue");
			addRelationKeyed(key, "contraryAttribute", "False", "True");
			addRelationKeyed(key, "subrelation", "true", "property");
			addRelationKeyed(key, "genls", "PositionalAttribute",
					"RelationalAttribute");
			addRelationKeyed(key, "genls", "DirectionalAttribute",
					"PositionalAttribute");
			addRelationKeyed(key, "isa", "North", "DirectionalAttribute");
			addRelationKeyed(key, "isa", "South", "DirectionalAttribute");
			addRelationKeyed(key, "isa", "East", "DirectionalAttribute");
			addRelationKeyed(key, "isa", "West", "DirectionalAttribute");
			addRelationKeyed(key, "isa", "Vertical", "PositionalAttribute");
			addRelationKeyed(key, "isa", "Horizontal", "PositionalAttribute");
			addRelationKeyed(key, "contraryAttribute", "Horizontal", "Vertical");
			addRelationKeyed(key, "isa", "Above", "PositionalAttribute");
			addRelationKeyed(key, "contraryAttribute", "Above", "Below");
			addRelationKeyed(key, "isa", "Below", "PositionalAttribute");
			addRelationKeyed(key, "isa", "Adjacent", "PositionalAttribute");
			addRelationKeyed(key, "isa", "Left", "PositionalAttribute");
			addRelationKeyed(key, "isa", "Right", "PositionalAttribute");
			addRelationKeyed(key, "contraryAttribute", "Right", "Left");
			addRelationKeyed(key, "isa", "Near", "PositionalAttribute");
			addRelationKeyed(key, "isa", "On", "PositionalAttribute");
			addRelationKeyed(key, "genls", "TimeZone", "RelationalAttribute");
			addRelationKeyed(key, "isa", "CoordinatedUniversalTimeZone", "TimeZone");
			addRelationKeyed(key, "isa", "RelativeTimeFn", "BinaryFunction");
			addRelationKeyed(key, "isa", "RelativeTimeFn", "TemporalRelation");
			addRelationKeyed(key, "isa", "RelativeTimeFn", "TotalValuedRelation");
			addRelationKeyed(key, "range", "RelativeTimeFn", "TimePosition");
			addRelationKeyed(key, "isa", "Unemployed", "SocialRole");
			addRelationKeyed(key, "genls", "NormativeAttribute",
					"RelationalAttribute");
			addRelationKeyed(key, "isa", "modalAttribute", "BinaryPredicate");
			addRelationKeyed(key, "isa", "modalAttribute", "AsymmetricRelation");
			addRelationKeyed(key, "isa", "modalAttribute", "IrreflexiveRelation");
			addRelationKeyed(key, "subrelation", "modalAttribute", "property");
			addRelationKeyed(key, "genls", "SubjectiveAssessmentAttribute",
					"NormativeAttribute");
			addRelationKeyed(key, "disjoint", "SubjectiveAssessmentAttribute",
					"ObjectiveNorm");
			addRelationKeyed(key, "genls", "ObjectiveNorm", "NormativeAttribute");
			addRelationKeyed(key, "genls", "ContestAttribute", "ObjectiveNorm");
			addRelationKeyed(key, "genls", "AlethicAttribute", "ObjectiveNorm");
			addRelationKeyed(key, "isa", "Possibility", "AlethicAttribute");
			addRelationKeyed(key, "isa", "Necessity", "AlethicAttribute");
			addRelationKeyed(key, "genls", "DeonticAttribute", "ObjectiveNorm");
			addRelationKeyed(key, "isa", "Permission", "DeonticAttribute");
			addRelationKeyed(key, "isa", "Obligation", "DeonticAttribute");
			addRelationKeyed(key, "subAttribute", "Law", "Obligation");
			addRelationKeyed(key, "subAttribute", "Promise", "Obligation");
			addRelationKeyed(key, "subAttribute", "Contract", "Promise");
			addRelationKeyed(key, "subAttribute", "NakedPromise", "Promise");
			addRelationKeyed(key, "contraryAttribute", "NakedPromise", "Contract");
			addRelationKeyed(key, "isa", "Prohibition", "DeonticAttribute");
			addRelationKeyed(key, "genls", "ProbabilityAttribute", "ObjectiveNorm");
			addRelationKeyed(key, "isa", "Likely", "ProbabilityAttribute");
			addRelationKeyed(key, "contraryAttribute", "Likely", "Unlikely");
			addRelationKeyed(key, "isa", "Unlikely", "ProbabilityAttribute");
			addRelationKeyed(key, "genls", "PhysicalState", "InternalAttribute");
			addRelationKeyed(key, "isa", "Solid", "PhysicalState");
			addRelationKeyed(key, "isa", "Fluid", "PhysicalState");
			addRelationKeyed(key, "isa", "Liquid", "PhysicalState");
			addRelationKeyed(key, "subAttribute", "Liquid", "Fluid");
			addRelationKeyed(key, "isa", "Gas", "PhysicalState");
			addRelationKeyed(key, "subAttribute", "Gas", "Fluid");
			addRelationKeyed(key, "isa", "Plasma", "PhysicalState");
			addRelationKeyed(key, "subAttribute", "Plasma", "Fluid");
			addRelationKeyed(key, "genls", "PerceptualAttribute", "InternalAttribute");
			addRelationKeyed(key, "genls", "TasteAttribute", "PerceptualAttribute");
			addRelationKeyed(key, "genls", "OlfactoryAttribute",
					"PerceptualAttribute");
			addRelationKeyed(key, "genls", "VisualAttribute", "PerceptualAttribute");
			addRelationKeyed(key, "isa", "Illuminated", "VisualAttribute");
			addRelationKeyed(key, "isa", "Unilluminated", "VisualAttribute");
			addRelationKeyed(key, "contraryAttribute", "Unilluminated", "Illuminated");
			addRelationKeyed(key, "genls", "ColorAttribute", "VisualAttribute");
			addRelationKeyed(key, "genls", "PrimaryColor", "ColorAttribute");
			addRelationKeyed(key, "isa", "Red", "PrimaryColor");
			addRelationKeyed(key, "isa", "Blue", "PrimaryColor");
			addRelationKeyed(key, "isa", "Yellow", "PrimaryColor");
			addRelationKeyed(key, "isa", "White", "PrimaryColor");
			addRelationKeyed(key, "isa", "Black", "PrimaryColor");
			addRelationKeyed(key, "isa", "Monochromatic", "ColorAttribute");
			addRelationKeyed(key, "isa", "Polychromatic", "ColorAttribute");
			addRelationKeyed(key, "contraryAttribute", "Polychromatic",
					"Monochromatic");
			addRelationKeyed(key, "genls", "ShapeAttribute", "InternalAttribute");
			addRelationKeyed(key, "isa", "Pliable", "ShapeAttribute");
			addRelationKeyed(key, "isa", "Rigid", "ShapeAttribute");
			addRelationKeyed(key, "contraryAttribute", "Rigid", "Pliable");
			addRelationKeyed(key, "genls", "GeometricFigure", "ShapeAttribute");
			addRelationKeyed(key, "genls", "GeometricPoint", "GeometricFigure");
			addRelationKeyed(key, "genls", "OneDimensionalFigure", "GeometricFigure");
			addRelationKeyed(key, "genls", "TwoDimensionalFigure", "GeometricFigure");
			addRelationKeyed(key, "genls", "OpenTwoDimensionalFigure",
					"TwoDimensionalFigure");
			addRelationKeyed(key, "genls", "TwoDimensionalAngle",
					"OpenTwoDimensionalFigure");
			addRelationKeyed(key, "genls", "RightAngle", "TwoDimensionalAngle");
			addRelationKeyed(key, "genls", "ClosedTwoDimensionalFigure",
					"TwoDimensionalFigure");
			addRelationKeyed(key, "genls", "Polygon", "ClosedTwoDimensionalFigure");
			addRelationKeyed(key, "genls", "Triangle", "Polygon");
			addRelationKeyed(key, "genls", "Quadrilateral", "Polygon");
			addRelationKeyed(key, "genls", "Rectangle", "Quadrilateral");
			addRelationKeyed(key, "genls", "Square", "Rectangle");
			addRelationKeyed(key, "genls", "Circle", "ClosedTwoDimensionalFigure");
			addRelationKeyed(key, "genls", "ThreeDimensionalFigure",
					"GeometricFigure");
			addRelationKeyed(key, "genls", "Sphere", "ThreeDimensionalFigure");
			addRelationKeyed(key, "isa", "geometricPart", "BinaryPredicate");
			addRelationKeyed(key, "isa", "geometricPart", "PartialOrderingRelation");
			addRelationKeyed(key, "subrelation", "pointOfFigure", "geometricPart");
			addRelationKeyed(key, "subrelation", "sideOfFigure", "geometricPart");
			addRelationKeyed(key, "subrelation", "angleOfFigure", "geometricPart");
			addRelationKeyed(key, "isa", "pointOfIntersection", "TernaryPredicate");
			addRelationKeyed(key, "isa", "parallel", "BinaryPredicate");
			addRelationKeyed(key, "isa", "angularMeasure", "BinaryPredicate");
			addRelationKeyed(key, "isa", "angularMeasure", "TotalValuedRelation");
			addRelationKeyed(key, "isa", "lineMeasure", "BinaryPredicate");
			addRelationKeyed(key, "isa", "lineMeasure", "TotalValuedRelation");
			addRelationKeyed(key, "isa", "geometricDistance", "TernaryPredicate");
			addRelationKeyed(key, "isa", "geometricDistance", "SingleValuedRelation");
			addRelationKeyed(key, "isa", "geometricDistance", "TotalValuedRelation");
			addRelationKeyed(key, "isa", "radius", "BinaryPredicate");
			addRelationKeyed(key, "isa", "radius", "SingleValuedRelation");
			addRelationKeyed(key, "isa", "radius", "TotalValuedRelation");
			addRelationKeyed(key, "isa", "diameter", "BinaryPredicate");
			addRelationKeyed(key, "isa", "diameter", "SingleValuedRelation");
			addRelationKeyed(key, "isa", "diameter", "TotalValuedRelation");
			addRelationKeyed(key, "isa", "tangent", "BinaryPredicate");
			addRelationKeyed(key, "genls", "TextureAttribute", "PerceptualAttribute");
			addRelationKeyed(key, "genls", "TextureAttribute", "ShapeAttribute");
			addRelationKeyed(key, "genls", "SoundAttribute", "PerceptualAttribute");
			addRelationKeyed(key, "genls", "SaturationAttribute", "InternalAttribute");
			addRelationKeyed(key, "isa", "Dry", "SaturationAttribute");
			addRelationKeyed(key, "contraryAttribute", "Dry", "Damp");
			addRelationKeyed(key, "isa", "Damp", "SaturationAttribute");
			addRelationKeyed(key, "isa", "Wet", "SaturationAttribute");
			addRelationKeyed(key, "subAttribute", "Wet", "Damp");
			addRelationKeyed(key, "genls", "BiologicalAttribute", "InternalAttribute");
			addRelationKeyed(key, "genls", "BodyPosition", "BiologicalAttribute");
			addRelationKeyed(key, "isa", "Standing", "BodyPosition");
			addRelationKeyed(key, "isa", "Sitting", "BodyPosition");
			addRelationKeyed(key, "isa", "Prostrate", "BodyPosition");
			addRelationKeyed(key, "genls", "AnimacyAttribute", "BiologicalAttribute");
			addRelationKeyed(key, "isa", "Living", "AnimacyAttribute");
			addRelationKeyed(key, "isa", "Dead", "AnimacyAttribute");
			addRelationKeyed(key, "subAttribute", "Dead", "Unconscious");
			addRelationKeyed(key, "contraryAttribute", "Dead", "Living");
			addRelationKeyed(key, "genls", "SexAttribute", "BiologicalAttribute");
			addRelationKeyed(key, "isa", "Female", "SexAttribute");
			addRelationKeyed(key, "attribute", "?ORG", "Female");
			addRelationKeyed(key, "isa", "Male", "SexAttribute");
			addRelationKeyed(key, "contraryAttribute", "Male", "Female");
			addRelationKeyed(key, "genls", "DevelopmentalAttribute",
					"BiologicalAttribute");
			addRelationKeyed(key, "isa", "FullyFormed", "DevelopmentalAttribute");
			addRelationKeyed(key, "isa", "NonFullyFormed", "DevelopmentalAttribute");
			addRelationKeyed(key, "contraryAttribute", "NonFullyFormed",
					"FullyFormed");
			addRelationKeyed(key, "successorAttribute", "NonFullyFormed",
					"FullyFormed");
			addRelationKeyed(key, "isa", "Larval", "DevelopmentalAttribute");
			addRelationKeyed(key, "subAttribute", "Larval", "NonFullyFormed");
			addRelationKeyed(key, "isa", "Embryonic", "DevelopmentalAttribute");
			addRelationKeyed(key, "subAttribute", "Embryonic", "NonFullyFormed");
			addRelationKeyed(key, "contraryAttribute", "Embryonic", "Larval");
			addRelationKeyed(key, "genls", "DiseaseOrSyndrome", "BiologicalAttribute");
			addRelationKeyed(key, "genls", "PsychologicalAttribute",
					"BiologicalAttribute");
			addRelationKeyed(key, "genls", "StateOfMind", "PsychologicalAttribute");
			addRelationKeyed(key, "genls", "EmotionalState", "StateOfMind");
			addRelationKeyed(key, "genls", "ConsciousnessAttribute", "StateOfMind");
			addRelationKeyed(key, "isa", "Asleep", "ConsciousnessAttribute");
			addRelationKeyed(key, "isa", "Unconscious", "ConsciousnessAttribute");
			addRelationKeyed(key, "contraryAttribute", "Unconscious", "Awake");
			addRelationKeyed(key, "isa", "Awake", "ConsciousnessAttribute");
			addRelationKeyed(key, "genls", "TraitAttribute", "PsychologicalAttribute");
			addRelationKeyed(key, "genls", "PsychologicalDysfunction",
					"PsychologicalAttribute");
			addRelationKeyed(key, "genls", "PsychologicalDysfunction",
					"DiseaseOrSyndrome");
			addRelationKeyed(key, "genls", "TerroristOrganization",
					"PoliticalOrganization");
			addRelationKeyed(key, "genls", "ForeignTerroristOrganization",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "USStateDepartment", "Government");
			addRelationKeyed(key, "isa", "ImmigrationAndNationalityAct-US",
					"Proposition");
			addRelationKeyed(key, "modalAttribute",
					"ImmigrationAndNationalityAct-US", "Law");
			addRelationKeyed(key, "subsumesContentInstance",
					"ImmigrationAndNationalityAct-US",
					"ImmigrationAndNationalityAct-Section219");
			addRelationKeyed(key, "isa", "ImmigrationAndNationalityAct-Section219",
					"Proposition");
			addRelationKeyed(key, "modalAttribute",
					"ImmigrationAndNationalityAct-Section219", "Law");
			addRelationKeyed(key, "isa", "AntiterrorismAndEffectiveDeathPenaltyAct",
					"Proposition");
			addRelationKeyed(key, "modalAttribute",
					"AntiterrorismAndEffectiveDeathPenaltyAct", "Law");
			addRelationKeyed(key, "isa", "membersCount", "BinaryPredicate");
			addRelationKeyed(key, "isa", "AbuNidalOrganization",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "AbuNidalOrganization", "Iraq");
			addRelationKeyed(key, "isa", "SabriAlBanna", "Human");
			addRelationKeyed(key, "agentOperatesInArea", "AbuNidalOrganization",
					"UnitedStates");
			addRelationKeyed(key, "agentOperatesInArea", "AbuNidalOrganization",
					"UnitedKingdomOfGreatBritainAndNorthernIreland");
			addRelationKeyed(key, "agentOperatesInArea", "AbuNidalOrganization",
					"France");
			addRelationKeyed(key, "agentOperatesInArea", "AbuNidalOrganization",
					"Israel");
			addRelationKeyed(key, "isa", "AbuSayyafGroup",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "AbuSayyafGroup", "Philippines");
			addRelationKeyed(key, "agentOperatesInArea", "AbuSayyafGroup", "Malaysia");
			addRelationKeyed(key, "isa", "KhadafiJanjalani", "Human");
			addRelationKeyed(key, "membersCount", "AbuSayyafGroup", "200");
			addRelationKeyed(key, "isa", "ArmedIslamicGroup",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "ArmedIslamicGroup", "Algeria");
			addRelationKeyed(key, "isa", "AumSupremeTruth",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "isa", "AumSupremeTruth", "ReligiousOrganization");
			addRelationKeyed(key, "located", "AumSupremeTruth", "Japan");
			addRelationKeyed(key, "agentOperatesInArea", "AumSupremeTruth", "Russia");
			addRelationKeyed(key, "isa", "FumihiroJoyu", "Human");
			addRelationKeyed(key, "isa", "BasqueFatherlandAndLiberty",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "BasqueFatherlandAndLiberty", "Spain");
			addRelationKeyed(key, "located", "BasqueFatherlandAndLiberty", "France");
			addRelationKeyed(key, "isa", "AlGamaaAlIslamiyya",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "AlGamaaAlIslamiyya", "Egypt");
			addRelationKeyed(key, "agentOperatesInArea", "AlGamaaAlIslamiyya",
					"UnitedKingdomOfGreatBritainAndNorthernIreland");
			addRelationKeyed(key, "agentOperatesInArea", "AlGamaaAlIslamiyya",
					"Yemen");
			addRelationKeyed(key, "agentOperatesInArea", "AlGamaaAlIslamiyya",
					"Sudan");
			addRelationKeyed(key, "agentOperatesInArea", "AlGamaaAlIslamiyya",
					"Afghanistan");
			addRelationKeyed(key, "agentOperatesInArea", "AlGamaaAlIslamiyya",
					"Austria");
			addRelationKeyed(key, "isa", "ShaykhUmarAbdAlRahman", "Human");
			addRelationKeyed(key, "isa", "HAMAS", "ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "HAMAS", "Palestine");
			addRelationKeyed(key, "isa", "HarakatUlMujahidin",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "HarakatUlMujahidin", "Pakistan");
			addRelationKeyed(key, "located", "HarakatUlMujahidin", "Afghanistan");
			addRelationKeyed(key, "isa", "FarooqKashmiri", "Human");
			addRelationKeyed(key, "isa", "Hizballah", "ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "Hizballah", "Lebanon");
			addRelationKeyed(key, "agentOperatesInArea", "Hizballah", "Europe");
			addRelationKeyed(key, "agentOperatesInArea", "Hizballah", "Africa");
			addRelationKeyed(key, "agentOperatesInArea", "Hizballah", "SouthAmerica");
			addRelationKeyed(key, "agentOperatesInArea", "Hizballah", "NorthAmerica");
			addRelationKeyed(key, "agentOperatesInArea", "Hizballah", "Asia");
			addRelationKeyed(key, "isa", "IslamicMovementOfUzbekistan",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "IslamicMovementOfUzbekistan",
					"Afghanistan");
			addRelationKeyed(key, "located", "IslamicMovementOfUzbekistan",
					"Tajikistan");
			addRelationKeyed(key, "agentOperatesInArea",
					"IslamicMovementOfUzbekistan", "Uzbekistan");
			addRelationKeyed(key, "agentOperatesInArea",
					"IslamicMovementOfUzbekistan", "Kyrgyzstan");
			addRelationKeyed(key, "isa", "JapaneseRedArmy",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "JapaneseRedArmy", "Asia");
			addRelationKeyed(key, "located", "JapaneseRedArmy", "Lebanon");
			addRelationKeyed(key, "isa", "FusakoShigenobu", "Human");
			addRelationKeyed(key, "membersCount", "JapaneseRedArmy", "6");
			addRelationKeyed(key, "isa", "AlJihad", "ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "AlJihad", "Egypt");
			addRelationKeyed(key, "agentOperatesInArea", "AlJihad", "Yemen");
			addRelationKeyed(key, "agentOperatesInArea", "AlJihad", "Afghanistan");
			addRelationKeyed(key, "agentOperatesInArea", "AlJihad", "Pakistan");
			addRelationKeyed(key, "agentOperatesInArea", "AlJihad", "Sudan");
			addRelationKeyed(key, "agentOperatesInArea", "AlJihad", "Lebanon");
			addRelationKeyed(key, "agentOperatesInArea", "AlJihad",
					"UnitedKingdomOfGreatBritainAndNorthernIreland");
			addRelationKeyed(key, "isa", "KachAndKahaneChai",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "KachAndKahaneChai", "Israel");
			addRelationKeyed(key, "isa", "BinyaminKahane", "Human");
			addRelationKeyed(key, "isa", "KurdistanWorkersParty",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "KurdistanWorkersParty", "Turkey");
			addRelationKeyed(key, "located", "KurdistanWorkersParty", "Europe");
			addRelationKeyed(key, "located", "KurdistanWorkersParty",
					"MiddleEastRegion");
			addRelationKeyed(key, "isa", "AbdullahOcalan", "Human");
			addRelationKeyed(key, "isa", "LiberationTigersOfTamilEelam",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "LiberationTigersOfTamilEelam",
					"SriLanka");
			addRelationKeyed(key, "isa", "VelupillaiPrabhakaran", "Human");
			addRelationKeyed(key, "isa", "MujahedinEKhalqOrganization",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "MujahedinEKhalqOrganization", "Iraq");
			addRelationKeyed(key, "located", "MujahedinEKhalqOrganization", "Iran");
			addRelationKeyed(key, "isa", "NationalLiberationArmyColombia",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "NationalLiberationArmyColombia",
					"Colombia");
			addRelationKeyed(key, "located", "NationalLiberationArmyColombia",
					"Venezuela");
			addRelationKeyed(key, "isa", "PalestineIslamicJihad",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "PalestineIslamicJihad", "Israel");
			addRelationKeyed(key, "located", "PalestineIslamicJihad", "Palestine");
			addRelationKeyed(key, "located", "PalestineIslamicJihad", "Jordan");
			addRelationKeyed(key, "located", "PalestineIslamicJihad", "Lebanon");
			addRelationKeyed(key, "located", "PalestineIslamicJihad", "Syria");
			addRelationKeyed(key, "isa", "PalestineLiberationFront",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "PalestineLiberationFront", "Iraq");
			addRelationKeyed(key, "isa", "MuhammadAbbas", "Human");
			addRelationKeyed(key, "isa", "PopularFrontForTheLiberationOfPalestine",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located",
					"PopularFrontForTheLiberationOfPalestine", "Syria");
			addRelationKeyed(key, "located",
					"PopularFrontForTheLiberationOfPalestine", "Lebanon");
			addRelationKeyed(key, "located",
					"PopularFrontForTheLiberationOfPalestine", "Israel");
			addRelationKeyed(key, "located",
					"PopularFrontForTheLiberationOfPalestine", "Palestine");
			addRelationKeyed(key, "membersCount",
					"PopularFrontForTheLiberationOfPalestine", "800");
			addRelationKeyed(key, "isa",
					"PopularFrontForTheLiberationOfPalestineGeneralCommand",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located",
					"PopularFrontForTheLiberationOfPalestineGeneralCommand", "Syria");
			addRelationKeyed(key, "located",
					"PopularFrontForTheLiberationOfPalestineGeneralCommand", "Lebanon");
			addRelationKeyed(key, "documentation",
					"PopularFrontForTheLiberationOfPalestineGeneralCommand", "");
			addRelationKeyed(key, "isa", "AhmadJabril", "Human");
			addRelationKeyed(key, "documentation",
					"PopularFrontForTheLiberationOfPalestineGeneralCommand", "");
			addRelationKeyed(key, "isa", "AlQaida", "ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "AlQaida", "Afghanistan");
			addRelationKeyed(key, "located", "AlQaida", "Pakistan");
			addRelationKeyed(key, "isa", "UsamaBinLadin", "Human");
			addRelationKeyed(key, "isa", "RevolutionaryArmedForcesOfColombia",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "RevolutionaryArmedForcesOfColombia",
					"Colombia");
			addRelationKeyed(key, "agentOperatesInArea",
					"RevolutionaryArmedForcesOfColombia", "Venezuela");
			addRelationKeyed(key, "agentOperatesInArea",
					"RevolutionaryArmedForcesOfColombia", "Panama");
			addRelationKeyed(key, "agentOperatesInArea",
					"RevolutionaryArmedForcesOfColombia", "Ecuador");
			addRelationKeyed(key, "isa", "ManuelMarulanda", "Human");
			addRelationKeyed(key, "isa", "RevolutionaryOrganization17November",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "RevolutionaryOrganization17November",
					"Greece");
			addRelationKeyed(key, "located",
					"RevolutionaryPeoplesLiberationPartyFront", "Turkey");
			addRelationKeyed(key, "agentOperatesInArea",
					"RevolutionaryPeoplesLiberationPartyFront", "Europe");
			addRelationKeyed(key, "isa", "RevolutionaryPeoplesStruggle",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "RevolutionaryPeoplesStruggle", "Greece");
			addRelationKeyed(key, "isa", "SenderoLuminoso",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "SenderoLuminoso", "Peru");
			addRelationKeyed(key, "isa", "AbimaelGuzman", "Human");
			addRelationKeyed(key, "isa", "TupacAmaruRevolutionaryMovement",
					"ForeignTerroristOrganization");
			addRelationKeyed(key, "located", "TupacAmaruRevolutionaryMovement",
					"Peru");
			addRelationKeyed(key, "isa", "AlexBoncayaoBrigade",
					"TerroristOrganization");
			addRelationKeyed(key, "located", "AlexBoncayaoBrigade", "Philippines");
			addRelationKeyed(key, "membersCount", "AlexBoncayaoBrigade", "500");
			addRelationKeyed(key, "isa", "ArmyForTheLiberationOfRwanda",
					"TerroristOrganization");
			addRelationKeyed(key, "located", "ArmyForTheLiberationOfRwanda",
					"DemocraticRepublicOfTheCongo");
			addRelationKeyed(key, "located", "ArmyForTheLiberationOfRwanda", "Rwanda");
			addRelationKeyed(key, "agentOperatesInArea",
					"ArmyForTheLiberationOfRwanda", "Burundi");
			addRelationKeyed(key, "isa", "ContinuityIrishRepublicanArmy",
					"TerroristOrganization");
			addRelationKeyed(key, "located", "ContinuityIrishRepublicanArmy",
					"Ireland");
			addRelationKeyed(key, "located", "ContinuityIrishRepublicanArmy",
					"NorthernIreland");
			addRelationKeyed(key, "isa", "FirstOfOctoberAntifascistResistanceGroup",
					"TerroristOrganization");
			addRelationKeyed(key, "located",
					"FirstOfOctoberAntifascistResistanceGroup", "Spain");
			addRelationKeyed(key, "isa", "IrishRepublicanArmy",
					"TerroristOrganization");
			addRelationKeyed(key, "located", "IrishRepublicanArmy", "NorthernIreland");
			addRelationKeyed(key, "located", "IrishRepublicanArmy", "Ireland");
			addRelationKeyed(key, "located", "IrishRepublicanArmy",
					"UnitedKingdomOfGreatBritainAndNorthernIreland");
			addRelationKeyed(key, "located", "IrishRepublicanArmy", "Europe");
			addRelationKeyed(key, "isa", "JaishEMohammed", "TerroristOrganization");
			addRelationKeyed(key, "located", "JaishEMohammed", "Pakistan");
			addRelationKeyed(key, "agentOperatesInArea", "JaishEMohammed",
					"Afghanistan");
			addRelationKeyed(key, "isa", "MaulanaMasoodAzhar", "Human");
			addRelationKeyed(key, "isa", "LashkarETayyiba", "TerroristOrganization");
			addRelationKeyed(key, "located", "LashkarETayyiba", "Pakistan");
			addRelationKeyed(key, "agentOperatesInArea", "LashkarETayyiba",
					"Afghanistan");
			addRelationKeyed(key, "isa", "HafizMohammedSaeed", "Human");
			addRelationKeyed(key, "isa", "LoyalistVolunteerForce",
					"TerroristOrganization");
			addRelationKeyed(key, "located", "LoyalistVolunteerForce",
					"NorthernIreland");
			addRelationKeyed(key, "located", "LoyalistVolunteerForce", "Ireland");
			addRelationKeyed(key, "membersCount", "LoyalistVolunteerForce", "150");
			addRelationKeyed(key, "isa", "NewPeoplesArmy", "TerroristOrganization");
			addRelationKeyed(key, "located", "NewPeoplesArmy", "Philippines");
			addRelationKeyed(key, "isa", "OrangeVolunteers", "TerroristOrganization");
			addRelationKeyed(key, "located", "OrangeVolunteers", "NorthernIreland");
			addRelationKeyed(key, "membersCount", "OrangeVolunteers", "20");
			addRelationKeyed(key, "isa", "PeopleAgainstGangsterismAndDrugs",
					"TerroristOrganization");
			addRelationKeyed(key, "located", "PeopleAgainstGangsterismAndDrugs",
					"SouthAfrica");
			addRelationKeyed(key, "isa", "AbdusSalaamEbrahim", "Human");
			addRelationKeyed(key, "isa", "RealIRA", "TerroristOrganization");
			addRelationKeyed(key, "located", "RealIRA", "NorthernIreland");
			addRelationKeyed(key, "located", "RealIRA", "Ireland");
			addRelationKeyed(key, "located", "RealIRA",
					"UnitedKingdomOfGreatBritainAndNorthernIreland");
			addRelationKeyed(key, "isa", "MickeyMcKevitt", "Human");
			addRelationKeyed(key, "isa", "RedHandDefenders", "TerroristOrganization");
			addRelationKeyed(key, "located", "RedHandDefenders", "NorthernIreland");
			addRelationKeyed(key, "isa", "RevolutionaryUnitedFront",
					"TerroristOrganization");
			addRelationKeyed(key, "located", "RevolutionaryUnitedFront",
					"SierraLeone");
			addRelationKeyed(key, "located", "RevolutionaryUnitedFront", "Liberia");
			addRelationKeyed(key, "located", "RevolutionaryUnitedFront", "Guinea");
			addRelationKeyed(key, "isa", "CharlesTaylor", "Human");
			addRelationKeyed(key, "isa", "UnitedSelfDefenseForcesGroupOfColombia",
					"TerroristOrganization");
			addRelationKeyed(key, "located",
					"UnitedSelfDefenseForcesGroupOfColombia", "Colombia");
			addRelationKeyed(key, "isa", "CarlosCastano", "Human");
			addRelationKeyed(key, "membersCount",
					"UnitedSelfDefenseForcesGroupOfColombia", "8000");
			addRelationKeyed(key, "isa", "ArmataCorsa", "TerroristOrganization");
			addRelationKeyed(key, "located", "ArmataCorsa", "France");
			addRelationKeyed(key, "isa", "ChukakuHa", "TerroristOrganization");
			addRelationKeyed(key, "located", "ChukakuHa", "Japan");
			addRelationKeyed(key, "isa",
					"DemocraticFrontForTheLiberationOfPalestine", "TerroristOrganization");
			addRelationKeyed(key, "located",
					"DemocraticFrontForTheLiberationOfPalestine", "Palestine");
			addRelationKeyed(key, "isa", "AlFaran", "TerroristOrganization");
			addRelationKeyed(key, "located", "AlFaran", "Pakistan");
			addRelationKeyed(key, "isa", "AlgetiWolves", "TerroristOrganization");
			addRelationKeyed(key, "isa", "Amal", "TerroristOrganization");
			addRelationKeyed(key, "isa", "BavarianLiberationArmy",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "BretonRevolutionaryArmy",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "ChechenRebelResistence",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "DjiboutiYouthMovement",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "DukhtaranEMillat", "TerroristOrganization");
			addRelationKeyed(key, "isa", "EjercitoPopularDeLiberation",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "FarabundoMartiNationalLiberationFront",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "Fatah", "TerroristOrganization");
			addRelationKeyed(key, "isa", "YassirArafat", "Human");
			addRelationKeyed(key, "isa", "FatahTanzim", "TerroristOrganization");
			addRelationKeyed(key, "located", "FatahTanzim", "Palestine");
			addRelationKeyed(key, "isa", "Force17", "TerroristOrganization");
			addRelationKeyed(key, "located", "Force17", "Palestine");
			addRelationKeyed(key, "isa", "HizbUlMujehideen", "TerroristOrganization");
			addRelationKeyed(key, "isa", "InternationalJusticeGroup",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "IslamicMovementForChange",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "JammuAndKashmir", "TerroristOrganization");
			addRelationKeyed(key, "isa", "ManuelRodriquezPatrioticFront",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "AlAqsaMartyrsBrigade",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "MoranzanistPatrioticFront",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "NationalLiberationFrontOfCorsica",
					"TerroristOrganization");
			addRelationKeyed(key, "isa",
					"NationalUnionForTheTotalIndependenceOfAngola",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "NestorPazZamoraCommission",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "KhmerRouge", "TerroristOrganization");
			addRelationKeyed(key, "isa", "PeoplesLiberationArmy",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "Recontra380", "TerroristOrganization");
			addRelationKeyed(key, "isa", "RedArmyFaction", "TerroristOrganization");
			addRelationKeyed(key, "isa", "RedBrigades", "TerroristOrganization");
			addRelationKeyed(key, "isa", "RevolutionaryPeoplesLiberationPartyFront",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "SikhTerrorism", "TerroristOrganization");
			addRelationKeyed(key, "isa", "SipahESahabaPakistan",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "TheIslamicGreatEasternRaidersFront",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "TupacKatariGuerillaArmy",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "TurkishWorkersAndPeasantsLiberationArmy",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "UlsterVolunteerForce",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "UnitedLiberationFrontOfAssam",
					"TerroristOrganization");
			addRelationKeyed(key, "isa", "UnitedPopularActionMovement",
					"TerroristOrganization");
			addRelationKeyed(key, "genls", "Arson", "Combustion");
			addRelationKeyed(key, "genls", "Arson", "Destruction");
			addRelationKeyed(key, "genls", "Bombing", "Destruction");
			addRelationKeyed(key, "genls", "Bomb", "Weapon");
			addRelationKeyed(key, "genls", "CarBombing", "Bombing");
			addRelationKeyed(key, "genls", "ChemicalAttack", "ViolentContest");
			addRelationKeyed(key, "genls", "HandgrenadeAttack", "Bombing");
			addRelationKeyed(key, "genls", "HandGrenade", "Bomb");
			addRelationKeyed(key, "genls", "Hijacking", "UnilateralGetting");
			addRelationKeyed(key, "genls", "HostageTaking", "UnilateralGetting");
			addRelationKeyed(key, "genls", "IncendiaryDeviceAttack", "Combustion");
			addRelationKeyed(key, "genls", "IncendiaryDeviceAttack", "ViolentContest");
			addRelationKeyed(key, "genls", "Infiltration", "JoiningAnOrganization");
			addRelationKeyed(key, "genls", "Kidnapping", "UnilateralGetting");
			addRelationKeyed(key, "genls", "KnifeAttack", "ViolentContest");
			addRelationKeyed(key, "genls", "Knife", "Device");
			addRelationKeyed(key, "genls", "LetterBombAttack", "Bombing");
			addRelationKeyed(key, "genls", "Lynching", "Killing");
			addRelationKeyed(key, "genls", "MortarAttack", "Bombing");
			addRelationKeyed(key, "genls", "Mortar", "Weapon");
			addRelationKeyed(key, "genls", "RocketMissileAttack", "Bombing");
			addRelationKeyed(key, "genls", "Rocket", "Weapon");
			addRelationKeyed(key, "genls", "Stoning", "ViolentContest");
			addRelationKeyed(key, "genls", "SuicideBombing", "Bombing");
			addRelationKeyed(key, "genls", "SuicideBombing", "Killing");
			addRelationKeyed(key, "genls", "Vandalism", "Destruction");
			addRelationKeyed(key, "genls", "VehicleAttack", "ViolentContest");
			addRelationKeyed(key, "genls", "Bus", "Vehicle");
			addRelationKeyed(key, "genls", "BusStop", "GeographicArea");
			addRelationKeyed(key, "genls", "BusinessPerson", "OccupationalRole");
			addRelationKeyed(key, "genls", "CargoVehicle", "Vehicle");
			addRelationKeyed(key, "genls", "Celebrity", "Human");
			addRelationKeyed(key, "genls", "Checkpoint", "GeographicArea");
			addRelationKeyed(key, "genls", "Road", "Region");
			addRelationKeyed(key, "genls", "LandVehicle", "Vehicle");
			addRelationKeyed(key, "genls", "Civilian", "OccupationalRole");
			addRelationKeyed(key, "genls", "CivilianHuman", "Human");
			addRelationKeyed(key, "genls", "Convoy", "Group");
			addRelationKeyed(key, "genls", "Diplomat", "GovernmentPerson");
			addRelationKeyed(key, "genls", "Dissident", "Civilian");
			addRelationKeyed(key, "genls", "Embassy", "GovernmentBuilding");
			addRelationKeyed(key, "genls", "EntertainmentBuilding", "Building");
			addRelationKeyed(key, "genls", "Garage", "Building");
			addRelationKeyed(key, "genls", "GovernmentBuilding", "Building");
			addRelationKeyed(key, "genls", "GovernmentPerson", "OccupationalRole");
			addRelationKeyed(key, "genls", "Hotel", "Building");
			addRelationKeyed(key, "genls", "Marketplace", "GeographicArea");
			addRelationKeyed(key, "genls", "StoreOwner", "OccupationalRole");
			addRelationKeyed(key, "genls", "Militant", "Civilian");
			addRelationKeyed(key, "genls", "MilitaryPerson", "OccupationalRole");
			addRelationKeyed(key, "genls", "OfficeBuilding", "Building");
			addRelationKeyed(key, "genls", "PeaceKeepingMission",
					"ModernMilitaryOrganization");
			addRelationKeyed(key, "genls", "PlaceOfWorship", "Building");
			addRelationKeyed(key, "genls", "Factory", "Building");
			addRelationKeyed(key, "genls", "PolicePerson", "GovernmentPerson");
			addRelationKeyed(key, "genls", "PoliticalFigure", "Celebrity");
			addRelationKeyed(key, "genls", "ReligiousFigure", "Celebrity");
			addRelationKeyed(key, "genls", "Restaurant", "Building");
			addRelationKeyed(key, "genls", "EducationalFacility",
					"StationaryArtifact");
			addRelationKeyed(key, "genls", "Store", "Building");
			addRelationKeyed(key, "genls", "Student", "SocialRole");
			addRelationKeyed(key, "genls", "Subway", "Hole");
			addRelationKeyed(key, "genls", "Tourist", "SocialRole");
			addRelationKeyed(key, "genls", "TouristSite", "GeographicArea");
			addRelationKeyed(key, "isa", "targetTypeInAttack", "BinaryPredicate");
			addRelationKeyed(key, "isa", "victimDeathCount", "BinaryPredicate");
			addRelationKeyed(key, "isa", "victimInjuryCount", "BinaryPredicate");
			addRelationKeyed(key, "isa", "victimCasualtyCount", "BinaryPredicate");
			addRelationKeyed(key, "isa", "agentCountInAttack", "BinaryPredicate");
			addRelationKeyed(key, "isa", "dayOfEvent", "BinaryPredicate");
			addRelationKeyed(key, "isa", "monthOfEvent", "BinaryPredicate");
			addRelationKeyed(key, "isa", "yearOfEvent", "BinaryPredicate");
		} catch (TGException tge) {
			System.err.println(tge.getMessage());
		}

	}

	public Iterator getAllNodes() {
		// TODO Auto-generated method stub
		return completeEltSet.getNodes().iterator();
	}

	public boolean isThread(TGThread expandThread) {
		return onlyThread==null || expandThread == onlyThread;
	}
	public boolean isThread() {
		Thread expandThread = Thread.currentThread();		
		return onlyThread==null || expandThread == onlyThread;
	}

} // end com.touchgraph.graphlayout.TGPanel

