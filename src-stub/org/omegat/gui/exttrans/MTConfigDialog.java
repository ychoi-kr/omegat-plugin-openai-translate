package org.omegat.gui.exttrans;

import java.awt.Window;

public abstract class MTConfigDialog {

	public final MTConfigPanel panel;
	
	public MTConfigDialog(Window parent, String name) {
        panel = new MTConfigPanel();
	}

	public void show() {
		// TODO Auto-generated method stub
		
	}
	
    protected abstract void onConfirm();

}
