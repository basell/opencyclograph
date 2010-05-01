package com.touchgraph.graphlayout;

import com.touchgraph.graphlayout.interaction.*;
import com.touchgraph.graphlayout.graphelements.*;

import java.awt.*;
import java.awt.event.*; //import  javax.swing.*;
import java.util.*;
import java.lang.*;

import org.opencyc.api.*;
import org.opencyc.cycobject.*;
import org.opencyc.cyclobject.*;

public class ExpandThread extends TGThread {
	public String expandKey;
	public Node focusNode;
	public boolean clearNext;

	public void runNow() {
			TGPanel.checkCycAccess();
			//targetPanel.demoDB2(expandKey);
			try {
				if (false)
					targetPanel.queryKB1(expandKey);
				focusNode = (Node) targetPanel.findNodeLabelContaining(expandKey);
				targetPanel.setLocale(focusNode, radius);
				targetPanel.queryKB3(expandKey, clearNext);
				if (false) {
					targetPanel.queryKB2(expandKey);
					Thread.sleep(600);
					targetPanel.updateVisList();
					Thread.sleep(60);
				}
				targetPanel.setLocale(focusNode, radius);
				Thread.sleep(5000);
				if (!targetPanel.isThread(this)) return;
				targetPanel.stopMotion();
				
			} catch (Exception ex) {
				//break;
			}
	}
}
