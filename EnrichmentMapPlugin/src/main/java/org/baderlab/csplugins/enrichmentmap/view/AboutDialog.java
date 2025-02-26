/**
 **                       EnrichmentMap Cytoscape Plugin
 **
 ** Copyright (c) 2008-2009 Bader Lab, Donnelly Centre for Cellular and Biomolecular 
 ** Research, University of Toronto
 **
 ** Contact: http://www.baderlab.org
 **
 ** Code written by: Ruth Isserlin
 ** Authors: Daniele Merico, Ruth Isserlin, Oliver Stueker, Gary D. Bader
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** (at your option) any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** University of Toronto
 ** has no obligations to provide maintenance, support, updates, 
 ** enhancements or modifications.  In no event shall the
 ** University of Toronto
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** University of Toronto
 ** has been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **
 **/

// $Id$
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
// $HeadURL$

package org.baderlab.csplugins.enrichmentmap.view;

import static org.baderlab.csplugins.enrichmentmap.EMBuildProps.APP_URL;
import static org.baderlab.csplugins.enrichmentmap.EMBuildProps.APP_VERSION;

import java.awt.Insets;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.util.swing.OpenBrowser;

import com.google.inject.Inject;

@SuppressWarnings("serial")
public class AboutDialog extends JDialog {

	private OpenBrowser browser;

	@Inject
	public AboutDialog(CySwingApplication application, OpenBrowser browser) {
		super(application.getJFrame(), "About EnrichmentMap", false);
		this.browser = browser;
		setResizable(false);

		JEditorPane editorPane = new JEditorPane();
		editorPane.setMargin(new Insets(10, 10, 10, 10));
		editorPane.setEditable(false);
		editorPane.setEditorKit(new HTMLEditorKit());
		editorPane.addHyperlinkListener(new HyperlinkAction());

		URL logoURL = getClass().getClassLoader().getResource("images/enrichmentmap_logo.png");

		editorPane.setText("<html><body style='font-family:Arial,Helvetica,sans-serif;'>" +
				"<table border='0'><tr>"
				+ "<td width='92' valign='top'><img height='46' width='92' src='" + logoURL.toString() + "'></td>"
				+ "<td width='209'>"
				+ "<p align=center>"
				+ "<b style='font-size:1.5em;'><span style='color:#1f78b4;'>Enrichment</span><span style='color:#33a02c;'>Map</span></b>"
				+ "<BR><span style='font-size:small;'><b>version " + APP_VERSION + "</b>"
				+ "<BR><BR>A Cytoscape App</span>"
				+ "</p>"
				+ "</td>"
				+ "<td width='92'> </td>"
				+ "</tr></table>"
				+ "<hr size='4' noshade>"
				+ "<p align=center>EnrichmentMap is a network-based method to visualize<BR>"
				+ "and interpret gene-set enrichment results.<BR><BR>"
				+ "by Gary Bader, Daniele Merico, Ruth Isserlin and Oliver Stueker<BR>"
				+ "(<a href='http://www.baderlab.org/'>Bader Lab</a>, University of Toronto)<BR><BR>"
				+ "App Homepage:<BR><a href='" + APP_URL + "'>" + APP_URL + "</a></p><BR>"
				+ "<hr size='4' noshade>"
				+ "<p style='font-size:small'>If you use this app in your research, please cite:</p>"
				+ "<p style='font-family:Courier,monospace;font-size:small'>"
				+ "Merico D, Isserlin R, Stueker O, Emili A, Bader GD<BR>"
				+ "Enrichment Map: A Network-Based Method for <BR>"
				+ "Gene-Set Enrichment Visualization and Interpretation<BR>" + "<i>PLoS One. 2010 Nov 15;5(11)</i><BR>"
				+ "</p><BR>"
				+ "</body></html>");

		setContentPane(editorPane);
	}

	
	private class HyperlinkAction implements HyperlinkListener {
		@Override
		public void hyperlinkUpdate(HyperlinkEvent event) {
			if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
				browser.openURL(event.getURL().toString());
		}
	}
}
