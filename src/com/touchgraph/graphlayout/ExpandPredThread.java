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

public class ExpandPredThread extends TGThread {

	public String key;
	public String pred;
	public String task;

	public void runNow() {
		try {

			if ("ROLE".equals(task)) {
				CycList response;

				try {
					TGPanel.checkCycAccess();
					response = TGPanel.cycAccess.getArg1s(pred, key, "EverythingPSC");
					System.out.println("expandRole " + pred + "[" + key + "]:"
							+ response.cyclify());

					targetPanel.addRelationAll(pred, key, response);
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}

			if ("PRED".equals(task)) {
				CycList response;

				try {

					response = TGPanel.cycAccess.getArg2s(pred, key, "EverythingPSC");
					System.out.println("expandPred " + pred + "[" + key + "]:"
							+ response.cyclify());
					targetPanel.addAllRelation(pred, key, response);
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}

			}

			try {

				Node focusNode = (Node) targetPanel.findNodeLabelContaining(key);
				targetPanel.setLocale(focusNode, radius);
			} catch (Exception ex) {
				// break;
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}
