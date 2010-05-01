package com.touchgraph.graphlayout;

import java.util.HashSet;
import java.util.Set;

import org.opencyc.api.CycAccess;

public abstract class TGThread extends Thread {
	Set<Edge> edges = new HashSet<Edge>();
	public TGPanel targetPanel;
	public int radius;
	//public CycAccess cycAccess = null;

	abstract void runNow();

	@Override
	final public void run() {
		try {
			synchronized (targetPanel.workerThreads) {
				targetPanel.workerThreads.add(this);
			}
//			cycAccess = TGPanel.checkCycAccess();
			runNow();
		} finally {
			synchronized (targetPanel.workerThreads) {
				targetPanel.workerThreads.remove(this);
			}
		}
	}

	public void addEdge(Edge foundEdge) {
		edges.add(foundEdge);
	}

	public void remEdge(Edge foundEdge) {
		edges.remove(foundEdge);
		foundEdge.reverse();
	}

}
