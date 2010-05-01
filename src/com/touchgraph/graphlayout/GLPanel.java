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

import com.touchgraph.graphlayout.interaction.*;
import com.touchgraph.graphlayout.graphelements.*;

import java.awt.*;
import java.awt.event.*; // import javax.swing.*;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Enumeration;
import java.net.URL;
import java.net.UnknownHostException;
import java.io.InputStreamReader;
import org.opencyc.api.*;
import org.opencyc.cycobject.*;
import org.opencyc.cyclobject.*;

import java.io.*;
import java.util.*; // import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.applet.Applet;
import java.applet.AppletContext;
import java.net.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

// import java.awt.event.*;
// import daxclr.modules.BrowserLauncher;

/**
 * GLPanel contains code for adding scrollbars and interfaces to the TGPanel The
 * "GL" prefix indicates that this class is GraphLayout specific, and will
 * probably need to be rewritten for other applications.
 * 
 * @author Alexander Shapiro
 * @version 1.22-jre1.1 $Id: GLPanel.java,v 1.3 2002/09/23 18:45:56 ldornbusch
 *          Exp $
 */
public class GLPanel extends Panel {

	public String zoomLabel = "Zoom"; // label for zoom menu item

	public String rotateLabel = "Rotate"; // label for rotate menu item

	public String localityLabel = "Radius"; // label for locality menu item

	public String hyperLabel = "Hyperbolic"; // label for Hyper menu item

	public HVScroll hvScroll;

	public ZoomScroll zoomScroll;

	public HyperScroll hyperScroll; // unused

	public RotateScroll rotateScroll;

	public LocalityScroll localityScroll;

	public PopupMenu glPopup;

	public Hashtable scrollBarHash; // = new Hashtable();

	public Choice History;

	public Choice VisList;

	protected TGPanel tgPanel;

	protected TGLensSet tgLensSet;

	protected TGUIManager tgUIManager;

	private Scrollbar currentSB = null;

	Vector selectedNodes; // stored a vector for each selected node
	// (found through the search text field)

	// each vector object is a vector of the node itself, node back color, node
	// border color, and node text color.

	TextField tfSearch;

	private Color defaultBackColor = new Color(0x01, 0x11, 0x44);

	private Color defaultBorderBackColor = new Color(0x02, 0x35, 0x81);

	private Color defaultForeColor = new Color((float) 0.95, (float) 0.85,
			(float) 0.55);

	public JTextPane tpWikiText;
	//public daxclr.ui.EditingPane tpWikiText;

	public String textPaneURL = null;

	protected JButton statusButton;

	private Node nodeMOHL; // MouseOverHyperLink

	Stack browseHistory = new Stack();

	public URLConnection url_con;

	public String mime_type;

	public static String WIKI_URL;

	public static String WIKI_FILE;

	public static String INITIAL_NODE = null;

	public static int INITIAL_RADIUS = -1;

	public static boolean INITIAL_SHOW_BACKLINKS = false;

	// ............

	/**
	 * Default constructor.
	 */
	public GLPanel() {
		try {
			this.setBackground(defaultBorderBackColor);
			this.setForeground(defaultForeColor);
			scrollBarHash = new Hashtable();
			tgLensSet = new TGLensSet();
			CycVariable variable = new CycVariable("WHAT");
			tgPanel = new TGPanel();
			tgPanel.glp = this;
			tgPanel.setBackColor(defaultBackColor);
			hvScroll = new HVScroll(tgPanel, tgLensSet);
			zoomScroll = new ZoomScroll(tgPanel);
			hyperScroll = new HyperScroll(tgPanel);
			rotateScroll = new RotateScroll(tgPanel);
			localityScroll = new LocalityScroll(tgPanel);
			selectedNodes = new Vector();
			initialize();
			setDefaultURL();
		} catch (Throwable e) {
			e.printStackTrace(System.out);
		}
	}

	static void resolveHost(final String host, final int port) {
		Thread t = new Thread() {
			public void run() {
				try {
					Socket s = new Socket(host, port);
					s.close();
				} catch (Throwable e) {
					e.printStackTrace(System.out);
				}
			}
		};
		t.start();
	}

	public void setDefaultURL() {
		new Thread() {
			public void run() {
				setWikiTextPane("http://www.google.com");
			}
		}.start();
	}

	/**
	 * Initialize panel, lens, and establish a random graph as a demonstration.
	 */
	public void initialize() {
		buildPanel();
		buildLens();
		tgPanel.setLensSet(tgLensSet);
		addUIs();
		// tgPanel.addNode(); //Add a starting node.
		try {
			// randomGraph();
			demoNet();
		} catch (TGException tge) {
			System.err.println(tge.getMessage());
			tge.printStackTrace(System.err);
		}
		setVisible(true);
	}

	/** Return the TGPanel used with this GLPanel. */
	public TGPanel getTGPanel() {
		return tgPanel;
	}

	// navigation .................

	/** Return the HVScroll used with this GLPanel. */
	public HVScroll getHVScroll() {
		return hvScroll;
	}

	/** Return the HyperScroll used with this GLPanel. */
	public HyperScroll getHyperScroll() {
		return hyperScroll;
	}

	/**
	 * Sets the horizontal offset to p.x, and the vertical offset to p.y given a
	 * Point <tt>p<tt>.
	 */
	public void setOffset(Point p) {
		hvScroll.setOffset(p);
	};

	/** Return the horizontal and vertical offset position as a Point. */
	public Point getOffset() {
		return hvScroll.getOffset();
	};

	// rotation ...................

	/** Return the RotateScroll used with this GLPanel. */
	public RotateScroll getRotateScroll() {
		return rotateScroll;
	}

	/**
	 * Set the rotation angle of this GLPanel (allowable values between 0 to
	 * 359).
	 */
	public void setRotationAngle(int angle) {
		rotateScroll.setRotationAngle(angle);
	}

	/** Return the rotation angle of this GLPanel. */
	public int getRotationAngle() {
		return rotateScroll.getRotationAngle();
	}

	// locality ...................

	/** Return the LocalityScroll used with this GLPanel. */
	public LocalityScroll getLocalityScroll() {
		return localityScroll;
	}

	/**
	 * Set the locality radius of this TGScrollPane (allowable values between 0
	 * to 4, or LocalityUtils.INFINITE_LOCALITY_RADIUS).
	 */
	public void setLocalityRadius(int radius) {
		localityScroll.setLocalityRadius(radius);
	}

	/** Return the locality radius of this GLPanel. */
	public int getLocalityRadius() {
		return localityScroll.getLocalityRadius();
	}

	// zoom .......................

	/** Return the ZoomScroll used with this GLPanel. */
	public ZoomScroll getZoomScroll() {
		return zoomScroll;
	}

	/**
	 * Set the zoom value of this GLPanel (allowable values between -100 to
	 * 100).
	 */
	public void setZoomValue(int zoomValue) {
		zoomScroll.setZoomValue(zoomValue);
	}

	/** Return the zoom value of this GLPanel. */
	public int getZoomValue() {
		return zoomScroll.getZoomValue();
	}

	// ....

	public PopupMenu getGLPopup() {
		return glPopup;
	}

	public void buildLens() {
		tgLensSet.addLens(hvScroll.getLens());
		tgLensSet.addLens(zoomScroll.getLens());
		tgLensSet.addLens(hyperScroll.getLens());
		tgLensSet.addLens(rotateScroll.getLens());
		tgLensSet.addLens(tgPanel.getAdjustOriginLens());
	}

	public void buildPanel() {
		final Scrollbar horizontalSB = hvScroll.getHorizontalSB();
		final Scrollbar verticalSB = hvScroll.getVerticalSB();
		final Scrollbar zoomSB = zoomScroll.getZoomSB();
		final Scrollbar rotateSB = rotateScroll.getRotateSB();
		final Scrollbar localitySB = localityScroll.getLocalitySB();
		final Scrollbar hyperSB = hyperScroll.getHyperSB();

		setLayout(new BorderLayout());

		Panel scrollPanel = new Panel();
		scrollPanel.setBackground(defaultBackColor);
		scrollPanel.setForeground(defaultForeColor);
		scrollPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		Panel modeSelectPanel = new Panel();
		modeSelectPanel.setBackground(defaultBackColor);
		modeSelectPanel.setForeground(defaultForeColor);
		modeSelectPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

		final Panel topPanel = new Panel();
		topPanel.setBackground(defaultBorderBackColor);
		topPanel.setForeground(defaultForeColor);
		topPanel.setLayout(new GridBagLayout());
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;

		c.gridx = 0;
		c.weightx = 0;

		c.insets = new Insets(0, 0, 0, 0);
		c.gridy = 0;
		c.weightx = 1;

		scrollBarHash.put(zoomLabel, zoomSB);
		scrollBarHash.put(rotateLabel, rotateSB);
		scrollBarHash.put(localityLabel, localitySB);
		scrollBarHash.put(hyperLabel, hyperSB);
		// System.out.println("before x1");

		Panel scrollselect = scrollSelectPanel(new String[] { zoomLabel,
				rotateLabel, localityLabel, hyperLabel });
		// System.out.println("after x1");

		scrollselect.setBackground(defaultBorderBackColor);
		scrollselect.setForeground(defaultForeColor);
		topPanel.add(scrollselect, c);

		add(topPanel, BorderLayout.SOUTH);

		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.weighty = 1;
		scrollPanel.add(tgPanel, c);

		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 0;
		c.weighty = 0;
		// scrollPanel.add(verticalSB,c); // For WDR We do not need scrollbars

		c.gridx = 0;
		c.gridy = 2;
		// scrollPanel.add(horizontalSB,c); // For WDR We do not need scrollbars

		add(scrollPanel, BorderLayout.CENTER);

		glPopup = new PopupMenu();
		add(glPopup); // needed by JDK11 Popupmenu..
		// Panel Popup "Stabilize"
		MenuItem menuItem2 = new MenuItem("Stabilize");
		ActionListener stabilizeAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tgPanel.stopMotion();
			}
		};
		menuItem2.addActionListener(stabilizeAction);
		glPopup.add(menuItem2);

		// Panel Popup "Toggle Controls"

		MenuItem menuItem = new MenuItem("Toggle Controls");
		ActionListener toggleControlsAction = new ActionListener() {
			boolean controlsVisible = true;

			public void actionPerformed(ActionEvent e) {
				controlsVisible = !controlsVisible;
				horizontalSB.setVisible(controlsVisible);
				verticalSB.setVisible(controlsVisible);
				topPanel.setVisible(controlsVisible);
				GLPanel.this.doLayout();
			}
		};
		menuItem.addActionListener(toggleControlsAction);
		glPopup.add(menuItem);

		// Panel Popup "Toggle Edge Labels"
		MenuItem menuItem3 = new MenuItem("Toggle Edge Labels");
		ActionListener labelEdgesAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tgPanel.showEdgeLables = !tgPanel.showEdgeLables;
			}
		};
		menuItem3.addActionListener(labelEdgesAction);
		glPopup.add(menuItem3);

		// Panel Popup "Toggle Tachyons"
		MenuItem menuItem4 = new MenuItem("Toggle Tachyons");
		ActionListener censorTachyonsAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tgPanel.censorTachyons = !tgPanel.censorTachyons;
			}
		};
		menuItem4.addActionListener(censorTachyonsAction);
		glPopup.add(menuItem4);

		// Panel Popup "Toggle Shadow"
		MenuItem menuItem5 = new MenuItem("Toggle Shadow");
		ActionListener shadowAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tgPanel.showShadow = !tgPanel.showShadow;
			}
		};
		menuItem5.addActionListener(shadowAction);
		glPopup.add(menuItem5);

		// WIKI Text Panels

		JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.weighty = 1;
		scrollPanel.add(tgPanel, c);

		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 0;
		c.weighty = 0;
		scrollPanel.add(verticalSB, c);

		c.gridx = 0;
		c.gridy = 2;
		scrollPanel.add(horizontalSB, c);

		jsp.setRightComponent(scrollPanel);

		JPanel wikiTextPanel = new JPanel();
		wikiTextPanel.setLayout(new BorderLayout());
		tpWikiText = new JTextPane();
		//tpWikiText = new daxclr.ui.EditingPane();
		tpWikiText.setEditable(false);

		tpWikiText.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				processHyperlinkEvent(e);
			}
		});
		tpWikiText.setEditorKit(new HTMLEditorKit());

		JScrollPane spWikiText = new JScrollPane(tpWikiText);
		spWikiText
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		statusButton = new JButton(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				try {
					BrowserLauncher.openURL(textPaneURL);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		statusButton.setToolTipText("Click to show page in external browser");
		statusButton.setHorizontalAlignment(SwingConstants.LEFT);
		statusButton.setMargin(new java.awt.Insets(2, 0, 2, 0));
		statusButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

		wikiTextPanel.add(spWikiText, BorderLayout.CENTER);
		wikiTextPanel.add(statusButton, BorderLayout.SOUTH);
		wikiTextPanel.setPreferredSize(new Dimension(400, 100));
		jsp.setLeftComponent(wikiTextPanel);
		jsp.setDividerLocation(350);
		jsp.setOneTouchExpandable(true);
		add(jsp, BorderLayout.CENTER);

	}

	final static String u_agent = "Lynx 2.8.4rel.1 on Linux: Lynx/2.8.4rel.1 libwww-FM/2.14 ";

	protected void setPanePage(URL url) throws IOException {
		if (tpWikiText == null) {
			//tpWikiText = new daxclr.ui.EditingPane();
			tpWikiText = new JTextPane();
		}
		/*
		 * String html=""; url_con = url.openConnection();
		 * 
		 * url_con.setRequestProperty("User-Agent",u_agent);
		 *  // mime_type = url_con.getContentType();
		 * 
		 *  // URL url = new URL("www.codetoad.com"); //URLConnection conn =
		 * url.openConnection(); //conn.setRequestProperty("User-Agent","");
		 * 
		 * BufferedReader in = new BufferedReader(new
		 * InputStreamReader(url_con.getInputStream())); String line="";
		 * 
		 * while((line=in.readLine())!=null) { html=html+line; } in.close();
		 * 
		 * //html=url.getFile(); System.out.println("HTML DATA="+html);
		 * 
		 * tpWikiText.setContentType("text/html"); tpWikiText.setText(html);
		 * 
		 */
		// tpWikiText.setPage(html);
		/*
		 * // Assume (like browsers) that missing mime-type indicates text/html.
		 * if(mime_type==null || mime_type.indexOf("text") != -1)
		 * tpWikiText.setPage(url); else { String path =
		 * "TWU_image_message.html"; URL u =
		 * getClass().getClassLoader().getResource(path); tpWikiText.setPage(u); }
		 */

		tpWikiText.setPage(url);
	}

	static Thread browserThread = null;

	static String browserThreadURL = null;

	public void setWikiTextPane(String url) {
		/*
		 * try { if (browserThread!=null && browserThread.isAlive()) { if (false &&
		 * browserThreadURL!=null && browserThreadURL.equals(url)) return; try {
		 * browserThread.interrupt(); } catch (Exception e) { } try {
		 * browserThread.stop(); } catch (Exception e) { } browserThread = null; } }
		 * catch (Exception eee) { }
		 */
		browserThreadURL = url;
		System.out.println("setWikiTextPane=" + url);
		textPaneURL = url;
		browserThread = new Thread() {
			public void run() {
				try {
					// tpWikiText.setPage(new URL(textPaneURL));
					setPanePage(new URL(textPaneURL));
					statusButton.setText(textPaneURL);
				} catch (Exception ex) {
					// System.out.println( "Could not load page:" + url + "\n" +
					// "Error:" + ex.getMessage() );
					ex.printStackTrace(System.out);
				}
			}
		};
		browserThread.start();
	}

	public void setWikiTextPane(Node n) {
		System.out.println("setWikiTextPane(Node " + n.id + ") url=" + n.url
				+ " strUrl=" + n.strUrl);
		if (n.getURL() == null || n.getURL().trim().equals(""))
			return;

		// System.out.println(n.getID());
		browseHistory.push(n.getID());
		setWikiTextPane(n.getURL());
	}

	private class BackButton extends JButton {
		BackButton() {
			super("< Back");
			setPreferredSize(new Dimension(80, 20));
			setMargin(new java.awt.Insets(2, 0, 2, 0));
			this.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (browseHistory.size() > 1) {
						try {
							browseHistory.pop();
							String nodeId = (String) browseHistory.peek();
							// System.out.println(nodeId);
							Node n = (Node) tgPanel.findNode(nodeId);

							// tgPanel.setSelect(n);
							processURL(new URL(n.getURL()));
							// Counteract the previous node being pushed as the
							// next node
							browseHistory.pop();
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			});
		}
	}

	Node findNodeByLabel(String label) {
		Node foundNode = null;
		Collection foundNodes = tgPanel.findNodesByLabel(label);
		if (foundNodes != null && !foundNodes.isEmpty()) {
			foundNode = (Node) foundNodes.iterator().next();
		}
		return foundNode;
	}

	public void setLocale(Node n) {
		try {
			// if(maxAddCombo!=null && maxExpandCombo!=null) {
			// int localityRadius = Integer.parseInt((String)
			// localityRadiusCombo.getSelectedItem());
			// int maxAddEdgeCount = Integer.parseInt((String)
			// maxAddCombo.getSelectedItem());
			// int maxExpandEdgeCount = Integer.parseInt((String)
			// maxExpandCombo.getSelectedItem());
			// boolean unidirectional = !showBackLinksCheckBox.isSelected();
			// tgPanel.setLocale(n,localityRadius,maxAddEdgeCount,maxExpandEdgeCount,unidirectional);
			tgPanel.setLocale(n, getLocalityRadius());
			// }
		} catch (TGException tge) {
			tge.printStackTrace();
		}
	}

	private String generateNodeLabel(String labelOrUrl) {
		if (!labelOrUrl.startsWith("http://")) {
			return labelOrUrl; // It's a label
		} else { // Create a label from the URL
			String urlString = labelOrUrl;

			if (urlString.length() < 25 && !labelOrUrl.startsWith(WIKI_URL)) {
				String str = urlString.substring(urlString.indexOf("//") + 2);
				if (str.length() > 40)
					str = str.substring(0, 40);
				return str;
			}
			if (urlString.indexOf("?") > -1) {
				return urlString.substring(urlString.indexOf("?") + 1);
			}

			if (urlString.lastIndexOf("/") != urlString.length() - 1) {
				return urlString.substring(urlString.lastIndexOf("/") + 1);
			}

			return urlString.substring(urlString.lastIndexOf("/",
					urlString.length() - 2) + 1, urlString.length() - 1);
		}
	}

	/** Called by WikiNodeHintUI when the user clicks a link. */
	public void processURL(URL url) {
		String urlString = url.toString();

		// String
		// nodeName=urlString.substring(urlString.indexOf("?")+1,urlString.length());
		String nodeName = generateNodeLabel(urlString);
		System.out.println(nodeName);
		Node n = findNodeByLabel(nodeName);
		if (n != null) {
			if (!n.isVisible()) {
				if (n.edgeCount() < 100) {
					setLocale(n);
				} else {
					try {
						tgPanel.setLocale(n, getLocalityRadius());
					} catch (TGException tge) {
					}
				}
			}
			tgPanel.setSelect(n);

			setWikiTextPane(n);
		} else {
			setWikiTextPane(urlString);
		}
	}

	public void processHyperlinkEvent(HyperlinkEvent hyperlinkEvent) {
		URL url = hyperlinkEvent.getURL();
		if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			processURL(url);
			return;
		}
		if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ENTERED) {
			String urlString = url.toString();
			String nodeName = generateNodeLabel(urlString);
			Node n = findNodeByLabel(nodeName);

			if (n != null) {
				if (n.isVisible()) {
					n.setMouseOverHyperlink(true);
					nodeMOHL = n;
					tgPanel.repaint();
				}
				statusButton.setText("IN GRAPH: " + nodeName + " (" + n.edgeCount()
						+ ")");
				if (n.edgeCount() > 50) {
					statusButton.setForeground(Color.red);
				} else {
					statusButton.setForeground(Color.black);
				}
			} else {
				statusButton.setText(urlString);
			}
		} else {
			statusButton.setText(textPaneURL);
			statusButton.setForeground(Color.black);
			if (nodeMOHL != null) {
				nodeMOHL.setMouseOverHyperlink(false);
				nodeMOHL = null;
				tgPanel.repaint();
			}
		}
	}

	public void setSelected(String searchString) {
		if (searchString != null && !searchString.trim().equals("")) {
			searchString = searchString.trim();
			if (searchString.startsWith("#$")) {
				searchString = searchString.substring(2);
			}
			tgPanel.demoDB(searchString, false);
			Node foundNode = (Node) tgPanel.findNodeLabelContaining(searchString);
			if (foundNode != null) {
				try {
					tgPanel.expandNode(foundNode);
					tgPanel.setSelect(foundNode);
					tgPanel.setLocale(foundNode, getLocalityRadius());
					tgPanel.setSelect(foundNode);
					getHVScroll().slowScrollToCenter(foundNode);
					setWikiTextPane(foundNode);
				} catch (TGException tge) {
					System.err.println(tge.getMessage());
					tge.printStackTrace(System.out);
				}
			}
		}
	}

	protected Panel scrollSelectPanel(final String[] scrollBarNames) {
		final Panel sbp = new Panel(new GridBagLayout());

		// UI: Scrollbarselector via
		// Radiobuttons.................................

		sbp.setBackground(defaultBorderBackColor);
		sbp.setForeground(defaultForeColor);

		Panel firstRow = new Panel(new GridBagLayout());

		final CheckboxGroup bg = new CheckboxGroup();

		int cbNumber = scrollBarNames.length;
		Checkbox checkboxes[] = new Checkbox[cbNumber];

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;

		for (int i = 0; i < cbNumber; i++) {
			checkboxes[i] = new Checkbox(scrollBarNames[i], true, bg);
			c.gridx = i;
			firstRow.add(checkboxes[i], c);
			// System.out.println(i);
			// System.out.println(scrollBarNames[i]);
		}
		checkboxes[0].setState(true);

		// c.gridx=cbNumber;c.weightx=1;
		// Label lbl = new Label(" KHC:Right-click nodes and background for more
		// options");
		// firstRow.add(lbl,c);

		// ---- KHC Selection history and visible nodes
		History = new Choice();
		c.gridx = cbNumber + 1;
		c.weightx = 1;// c.gridwidth=30;
		History.setForeground(Color.black);
		firstRow.add(History, c);
		History.addItem("Automobile");
		History.addItem("Entity");
		History.addItem("BillClinton");
		History.addItem("GeorgeWBush");
		History.addItem("EthnicGroupOfJapanese");
		History.addItem("(#$DoomItemFn \"cyc_bot_1\")");
		History.addItem("(#$DoomItemFn \"player1\")");
		History.addItem("CommunicationAct-Single");
		ItemListener historyAction = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				String searchString = History.getSelectedItem();
				// tfSearch.setText("");
				setSelected(searchString);
			}
		};
		History.addItemListener(historyAction);
		tgPanel.tgHistory = History;

		VisList = new Choice();
		c.gridx = cbNumber + 2;
		c.weightx = 1;// c.gridwidth=30;
		VisList.setForeground(Color.black);
		firstRow.add(VisList, c);
		VisList.addItem("CommunicationAct-Single");
		VisList.addItem("Automobile");
		VisList.addItem("Communicating");
		VisList.addItem("Man");
		ItemListener visAction = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				String searchString = VisList.getSelectedItem();
				// tfSearch.setText("");
				if (!searchString.trim().equals("")) {
					tgPanel.demoDB(searchString.trim(), false);
					Node foundNode = (Node) tgPanel.findNodeLabelContaining(searchString);
					if (foundNode != null) {
						try {
							tgPanel.expandNode(foundNode);
							tgPanel.setSelect(foundNode);
							tgPanel.setLocale(foundNode, getLocalityRadius());
							setWikiTextPane(foundNode);
							getHVScroll().slowScrollToCenter(foundNode);
						} catch (TGException tge) {
							System.err.println(tge.getMessage());
							tge.printStackTrace(System.err);
						}
					}
				}
			}
		};
		VisList.addItemListener(visAction);
		tgPanel.tgVisList = VisList;

		// --- Filter List
		c.gridx = cbNumber + 3;
		c.weightx = 1;
		// c.gridx=0;c.gridy = 1; c.weightx = 1; c.weighty = 1;
		java.awt.List edgeFilterList = new java.awt.List(2, true);
		edgeFilterList.addItem("genls");
		edgeFilterList.addItem("isa");
		edgeFilterList.addItem("owns");

		// topPanel.add(lbl, c);
		edgeFilterList.setForeground(Color.black);
		firstRow.add(edgeFilterList, c);
		tgPanel.edgeFilterList = edgeFilterList;
		ItemListener edgeFilterAction = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {

				tgPanel.updateFilterList();
			}
		};

		edgeFilterList.addItemListener(edgeFilterAction);

		// --- KHC Search box ???
		c.gridx = cbNumber + 4;
		c.weightx = 1;
		// c.gridx=0;c.gridy = 1; c.weightx = 1; c.weighty = 1;
		Label lbl = new Label("Search", Label.RIGHT);
		// topPanel.add(lbl, c);
		firstRow.add(lbl, c);
		c.gridx = cbNumber + 5;
		c.weightx = 1;
		c.gridwidth = 100; // c.gridy = 1; c.weightx = 1; c.weighty = 1;
		tfSearch = new TextField(40);
		tfSearch.setForeground(Color.black);
		// tfSearch.setToolTipText("Search the graph for a node containing the
		// given substring");
		// topPanel.add(tfSearch,c);
		firstRow.add(tfSearch, c);

		ActionListener findAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectedNodes = new Vector(); // reinitialise the vector
				tgPanel.setSelectedNodesVector(selectedNodes);
				String searchString = tfSearch.getText();
				tfSearch.setText("");
				if (!searchString.trim().equals("")) {
					tgPanel.demoDB(searchString.trim(), true);
					Node foundNode = (Node) tgPanel.findNodeLabelContaining(searchString);
					if (foundNode != null) {
						try {
							tgPanel.expandNode(foundNode);
							tgPanel.setLocale(foundNode, getLocalityRadius());
							tgPanel.setSelect(foundNode);
							setWikiTextPane(foundNode);
							getHVScroll().slowScrollToCenter(foundNode);
						} catch (TGException tge) {
							System.err.println(tge.getMessage());
							tge.printStackTrace(System.err);
						}
					}
				}
			}
		};
		tfSearch.addActionListener(findAction);

		// ---

		class radioItemListener implements ItemListener {
			private String scrollBarName;

			public radioItemListener(String str2Act) {
				this.scrollBarName = str2Act;
			}

			public void itemStateChanged(ItemEvent e) {
				Scrollbar selectedSB = (Scrollbar) scrollBarHash.get((String) bg
						.getSelectedCheckbox().getLabel());
				if (e.getStateChange() == ItemEvent.SELECTED) {
					for (int i = 0; i < scrollBarNames.length; i++) {
						Scrollbar sb = (Scrollbar) scrollBarHash.get(scrollBarNames[i]);
						sb.setVisible(false);
					}
					selectedSB.setBounds(currentSB.getBounds());
					if (selectedSB != null)
						selectedSB.setVisible(true);
					currentSB = selectedSB;
					sbp.invalidate();
				}
			}
		}
		;

		for (int i = 0; i < cbNumber; i++) {
			checkboxes[i].addItemListener(new radioItemListener(scrollBarNames[0]));
		}

		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(1, 5, 1, 5);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 10;
		c.gridwidth = 4; // Radiobutton UI
		c.gridheight = 1;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		sbp.add(firstRow, c);

		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		for (int i = 0; i < scrollBarNames.length; i++) {
			Scrollbar sb = (Scrollbar) scrollBarHash.get(scrollBarNames[i]);
			if (sb == null)
				continue;
			if (currentSB == null)
				currentSB = sb;
			sbp.add(sb, c);
		}

		return sbp;
	}

	public void addUIs() {
		tgUIManager = new TGUIManager();
		GLEditUI editUI = new GLEditUI(this);
		GLNavigateUI navigateUI = new GLNavigateUI(this);
		tgUIManager.addUI(editUI, "Edit");
		tgUIManager.addUI(navigateUI, "Navigate");
		tgUIManager.activate("Navigate");
	}

	public void demoNet() throws TGException {
		Node n1 = tgPanel.addNode("GraphRoot");
		n1.setType(0);

		tgPanel.addRelation("conceptuallyRelated", "GraphRoot", "HumanBeings");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings", "Person");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings", "Food");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings", "Language");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings",
				"Clothing-Generic");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings", "ConceptualWork");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings",
				"DevisedPracticeOrWork");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings", "HumanBody");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings", "ProductType");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings", "Product");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings",
				"TemporalThingTypeFrequentlyForSale");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings",
				"InformationStore");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings", "CodeOfConduct");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings", "Action");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings",
				"FeelingAttribute");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings", "Satisfaction");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings", "BeliefSystem");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings", "ActorSlot");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings", "Artifact");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings",
				"SensoryReactionType");
		tgPanel.addRelation("conceptuallyRelated", "HumanBeings",
				"PhysicalUrgeType");

		tgPanel.addRelation("conceptuallyRelated", "GraphRoot",
				"ConstructionArtifact");
		tgPanel.addRelation("conceptuallyRelated", "GraphRoot",
				"Physical Stuff and Objects");
		tgPanel.addRelation("conceptuallyRelated", "Physical Stuff and Objects",
				"Artifact");
		tgPanel.addRelation("conceptuallyRelated", "Physical Stuff and Objects",
				"ExistingStuffType");
		tgPanel.addRelation("conceptuallyRelated", "Physical Stuff and Objects",
				"ObjectType");
		tgPanel.addRelation("conceptuallyRelated", "Physical Stuff and Objects",
				"StuffType");
		tgPanel.addRelation("conceptuallyRelated", "Physical Stuff and Objects",
				"ChemicalSubstanceType");
		tgPanel.addRelation("conceptuallyRelated", "Physical Stuff and Objects",
				"PhysicalPartOfObject");

		tgPanel.addRelation("conceptuallyRelated", "GraphRoot", "SpatialPredicate");
		tgPanel.addRelation("conceptuallyRelated", "GraphRoot", "Nature");
		tgPanel.addRelation("conceptuallyRelated", "Nature", "WeatherEvent");
		tgPanel.addRelation("conceptuallyRelated", "Nature", "AstronomicalObject");
		tgPanel
				.addRelation("conceptuallyRelated", "Nature", "NaturalTangibleStuff");
		tgPanel.addRelation("conceptuallyRelated", "Nature",
				"BiologicalLivingObject");
		tgPanel.addRelation("conceptuallyRelated", "Nature", "Animal");
		tgPanel.addRelation("conceptuallyRelated", "Nature", "Plant");
		tgPanel.addRelation("conceptuallyRelated", "Nature", "OutdoorActivity");
		tgPanel
				.addRelation("conceptuallyRelated", "HumanBeings", "OutdoorActivity");

		tgPanel.addRelation("conceptuallyRelated", "GraphRoot", "HumanActivities");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"SocialOccurrence");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"CulturalThing");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"HumanSocialLifeMt");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"HumanActivitiesMt  ");

		tgPanel
				.addRelation("conceptuallyRelated", "HumanActivities", "Transaction");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities", "Game");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities", "Sport");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"EntertainmentEvent");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"MediaProduct");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"FinancialOrganization");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"EconomicEvent");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"BusinessEvent");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"CommercialActivity");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities", "Politics");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"ConflictEvent");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"ViolentAction");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"FinancialAsset");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"PersonTypeBy*");
		tgPanel.addRelation("conceptuallyRelated", "PersonTypeBy*",
				"PersonTypeByActivity");
		tgPanel.addRelation("conceptuallyRelated", "PersonTypeBy*",
				"PersonTypeByOccupation");
		tgPanel.addRelation("conceptuallyRelated", "PersonTypeBy*",
				"PersonTypeByBeliefSystem");
		tgPanel.addRelation("conceptuallyRelated", "PersonTypeBy*",
				"PersonTypeByCulture");
		tgPanel.addRelation("conceptuallyRelated", "PersonTypeBy*",
				"PersonTypeByEthnicity");
		tgPanel.addRelation("conceptuallyRelated", "PersonTypeBy*",
				"PersonTypeByGender");
		tgPanel.addRelation("conceptuallyRelated", "PersonTypeBy*",
				"PersonTypeByNationality");
		tgPanel.addRelation("conceptuallyRelated", "PersonTypeBy*",
				"PersonTypeByNationality");
		tgPanel.addRelation("conceptuallyRelated", "PersonTypeBy*",
				"PersonTypeByReligion");
		tgPanel.addRelation("conceptuallyRelated", "PersonTypeBy*",
				"PersonTypeByMaritalStatus");
		tgPanel.addRelation("conceptuallyRelated", "PersonTypeBy*",
				"PersonTypeByMentalFeature");
		tgPanel.addRelation("conceptuallyRelated", "PersonTypeBy*",
				"PersonTypeBySocialClass");
		tgPanel.addRelation("conceptuallyRelated", "PersonTypeBy*",
				"PersonTypeBySocialFeature");
		tgPanel.addRelation("conceptuallyRelated", "PersonTypeBy*",
				"PersonTypeByCothingState");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"TransportationEvent");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"Communicating");
		tgPanel.addRelation("conceptuallyRelated", "HumanActivities",
				"NonVerbalCommunicating");

		tgPanel.addRelation("conceptuallyRelated", "GraphRoot", "Agents");
		tgPanel.addRelation("conceptuallyRelated", "Agents", "Agent-Generic");
		tgPanel.addRelation("conceptuallyRelated", "Agents", "Organization");
		tgPanel.addRelation("conceptuallyRelated", "Agents", "Obligation");
		tgPanel.addRelation("conceptuallyRelated", "Agents", "CodeOfConduct");
		tgPanel.addRelation("conceptuallyRelated", "Agents", "Agreement");
		tgPanel.addRelation("conceptuallyRelated", "Agents", "Obligation");
		tgPanel.addRelation("conceptuallyRelated", "Agents", "Planning");
		tgPanel.addRelation("conceptuallyRelated", "Agents", "GoalTypeByCategory");
		tgPanel.addRelation("conceptuallyRelated", "Agents", "Goal");

		tgPanel.addRelation("conceptuallyRelated", "GraphRoot", "TimeAndDates");
		tgPanel.addRelation("conceptuallyRelated", "TimeAndDates", "TemporalThing");
		tgPanel.addRelation("conceptuallyRelated", "TimeAndDates", "Date");
		tgPanel.addRelation("conceptuallyRelated", "TimeAndDates", "TimeInterval");

		tgPanel.addRelation("conceptuallyRelated", "GraphRoot", "EventsAndScripts");
		tgPanel.addRelation("conceptuallyRelated", "EventsAndScripts", "Event");
		tgPanel.addRelation("conceptuallyRelated", "EventsAndScripts",
				"MovementEvent");
		tgPanel.addRelation("conceptuallyRelated", "EventsAndScripts", "Role");
		tgPanel.addRelation("conceptuallyRelated", "EventsAndScripts",
				"GeneralizedTransfer");
		tgPanel.addRelation("conceptuallyRelated", "EventsAndScripts", "ActorSlot");
		tgPanel.addRelation("conceptuallyRelated", "EventsAndScripts",
				"PhysicalTransformationEvent");
		tgPanel.addRelation("conceptuallyRelated", "EventsAndScripts",
				"CreationOrDestructionEvent");
		tgPanel
				.addRelation("conceptuallyRelated", "EventsAndScripts", "Perceiving");
		tgPanel.addRelation("conceptuallyRelated", "EventsAndScripts",
				"AnimateActivity");

		tgPanel.addRelation("conceptuallyRelated", "GraphRoot", "CycMetaPhysics");
		tgPanel.addRelation("conceptuallyRelated", "CycMetaPhysics",
				"SetOrCollection");
		tgPanel.addRelation("conceptuallyRelated", "CycMetaPhysics", "Relation");
		tgPanel.addRelation("conceptuallyRelated", "CycMetaPhysics",
				"MathematicalObject");
		tgPanel.addRelation("conceptuallyRelated", "CycMetaPhysics", "Group");
		tgPanel.addRelation("conceptuallyRelated", "CycMetaPhysics", "Group");
		tgPanel.addRelation("conceptuallyRelated", "CycMetaPhysics", "Group");
		tgPanel.addRelation("conceptuallyRelated", "CycMetaPhysics", "Group");

		tgPanel.addRelation("conceptuallyRelated", "GraphRoot",
				"SpecilizedKnowledge");
		tgPanel.addRelation("conceptuallyRelated", "SpecilizedKnowledge",
				"FieldOfStudy ");
		tgPanel.addRelation("conceptuallyRelated", "SpecilizedKnowledge",
				"TerroristAct");
		tgPanel.addRelation("conceptuallyRelated", "SpecilizedKnowledge",
				"MilitaryAgent ");
		tgPanel.addRelation("conceptuallyRelated", "SpecilizedKnowledge",
				"DangerousTangibleThing");
		tgPanel.addRelation("conceptuallyRelated", "SpecilizedKnowledge",
				"TakingCareOfSomething");
		tgPanel.addRelation("conceptuallyRelated", "SpecilizedKnowledge",
				"DrugSubstance");
		tgPanel.addRelation("conceptuallyRelated", "SpecilizedKnowledge",
				"MedicalTreatmentEvent");
		tgPanel.addRelation("conceptuallyRelated", "SpecilizedKnowledge",
				"HealthCare");

		tgPanel.addRelation("conceptuallyRelated", "GraphRoot",
				"ApplicationSpecificKnowledge");
		tgPanel.addRelation("conceptuallyRelated", "ApplicationSpecificKnowledge",
				"NLPredicate");
		tgPanel.addRelation("conceptuallyRelated", "ApplicationSpecificKnowledge",
				"CycProgramModule-CW");
		tgPanel.addRelation("conceptuallyRelated", "ApplicationSpecificKnowledge",
				"EvaluatableRelation");

		/*
		 * tgPanel.addRelation("isa","john","Person");
		 * tgPanel.addRelation("isa","jim","Person");
		 * tgPanel.addRelation("isa","jane","Person");
		 * tgPanel.addRelation("isa","mary","Person");
		 * tgPanel.addRelation("hasGender","jane","Woman");
		 * tgPanel.addRelation("hasGender","mary","Woman");
		 * tgPanel.addRelation("hasGender","john","Man");
		 * tgPanel.addRelation("hasGender","jim","Man");
		 * 
		 * tgPanel.addRelation("genls","Man","gender");
		 * tgPanel.addRelation("genls","Woman","gender");
		 * tgPanel.addRelation("genls","Person","mammal");
		 * tgPanel.addRelation("genls","Person","Human");
		 * tgPanel.addRelation("genls","Cat","Mammal");
		 * tgPanel.addRelation("genls","Dog","Mammal");
		 * tgPanel.addRelation("genls","Mammal","Animal");
		 * tgPanel.addRelation("genls","Animal","Entity");
		 * tgPanel.addRelation("owns","john","Automobile");
		 * tgPanel.addRelation("genls","Automobile","Device");
		 */

		tgPanel.setLocale(n1, getLocalityRadius());
		tgPanel.setSelect(n1);
		try {
			Thread.currentThread().sleep(2000);
		} catch (InterruptedException ex) {
		}

		getHVScroll().slowScrollToCenter(n1);

	}

	public void randomGraph() throws TGException {
		Node n1 = tgPanel.addNode();
		n1.setType(0);
		for (int i = 0; i < 249; i++) {
			tgPanel.addNode();
		}

		TGForEachNode fen = new TGForEachNode() {
			public void forEachNode(Node n) {
				for (int i = 0; i < 3; i++) {
					Node r = tgPanel.getGES().getRandomNode();
					if (r != n && tgPanel.findEdge(r, n) == null)
						tgPanel.addEdge(r, n, Edge.DEFAULT_LENGTH);
				}
			}
		};
		tgPanel.getGES().forAllNodes(fen);

		tgPanel.setLocale(n1, getLocalityRadius());
		tgPanel.setSelect(n1);
		try {
			Thread.currentThread().sleep(2000);
		} catch (InterruptedException ex) {
		}

		getHVScroll().slowScrollToCenter(n1);
	}

	public static void main(String[] args) {

		final Frame frame;
		CycConnection.DEFAULT_HOSTNAME = "CycServer";
		final GLPanel glPanel = new GLPanel();
		frame = new Frame("OpenCycloGraph");
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				frame.remove(glPanel);
				frame.dispose();
				System.exit(0);
			}
		});

		Node.GDS_KEY = "";
		if (args.length > 0)
			CycConnection.DEFAULT_HOSTNAME = args[0];
		//if (args.length > 1)
		//CycConnection.DEFAULT_BASE_PORT = (Integer.parseInt(args[1]));
		if (args.length > 2)
			Node.GDS_KEY = args[2];
		glPanel.tgPanel.checkCycAccess();

		frame.add("Center", glPanel);
		frame.setSize(800, 600);
		frame.setVisible(true);
	}

} // end com.touchgraph.graphlayout.GLPanel

