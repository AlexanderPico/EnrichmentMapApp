package org.baderlab.csplugins.enrichmentmap.view.legend;

import static org.baderlab.csplugins.enrichmentmap.EMBuildProps.HELP_URL_LEGEND;
import static org.baderlab.csplugins.enrichmentmap.view.util.SwingUtil.invokeOnEDT;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.baderlab.csplugins.enrichmentmap.AfterInjection;
import org.baderlab.csplugins.enrichmentmap.model.EMDataSet;
import org.baderlab.csplugins.enrichmentmap.style.EMStyleOptions;
import org.baderlab.csplugins.enrichmentmap.view.util.FileBrowser;
import org.baderlab.csplugins.enrichmentmap.view.util.OpenPDFViewerTask;
import org.baderlab.csplugins.enrichmentmap.view.util.SwingUtil;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class LegendPanelMediator {

	@Inject private Provider<LegendPanel> legendPanelProvider;
	@Inject private CySwingApplication swingApplication;
	@Inject private IconManager iconManager;
	@Inject private DialogTaskManager dialogTaskManager;
	@Inject private FileUtil fileUtil;
	@Inject private CyServiceRegistrar serviceRegistrar;
	
	private JDialog dialog;
	private JButton exportPdfButton;
	
	
	public void showDialog(EMStyleOptions options, Collection<EMDataSet> filteredDataSets) {
		invokeOnEDT(() -> {
			updateDialog(options, filteredDataSets, false);
			
			if (dialog != null) {
				dialog.pack();
				dialog.setVisible(true);
			}
		});
	}
	
	public void hideDialog() {
		invokeOnEDT(() -> {
			if (dialog != null)
				dialog.setVisible(false);
		});
	}
	
	public JDialog getDialog() {
		return dialog;
	}
	
	@AfterInjection
	@SuppressWarnings("serial")
	private void init() {
		invokeOnEDT(() -> {
			dialog = new JDialog(swingApplication.getJFrame(), "EnrichmentMap Legend", ModalityType.MODELESS);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setMinimumSize(new Dimension(440, 380));
			
			JButton closeButton = new JButton(new AbstractAction("Close") {
				@Override public void actionPerformed(ActionEvent e) {
					dialog.dispose();
				}
			});
			
			// TODO
			Font iconFont = iconManager.getIconFont(13.0f);
			exportPdfButton = new JButton("Export to PDF");
			exportPdfButton.setIcon(SwingUtil.iconFromString(IconManager.ICON_EXTERNAL_LINK, iconFont));
			exportPdfButton.addActionListener(e -> exportPDF());
			
			JButton helpButton = SwingUtil.createOnlineHelpButton(HELP_URL_LEGEND, "Online manual...", serviceRegistrar);
			JPanel bottomPanel = LookAndFeelUtil.createOkCancelPanel(null, closeButton, exportPdfButton, helpButton);
			
			dialog.getContentPane().add(legendPanelProvider.get(), BorderLayout.CENTER);
			dialog.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
			
			LookAndFeelUtil.setDefaultOkCancelKeyStrokes(dialog.getRootPane(), null, closeButton.getAction());
			dialog.getRootPane().setDefaultButton(closeButton);
			dialog.setLocationRelativeTo(swingApplication.getJFrame());
		});
	}

	
	public void updateDialog(EMStyleOptions options, Collection<EMDataSet> filteredDataSets) {
		updateDialog(options, filteredDataSets, true);
	}
	
	private void updateDialog(EMStyleOptions options, Collection<EMDataSet> filteredDataSets, boolean onlyIfVisible) {
		if (onlyIfVisible && !dialog.isVisible())
			return;
		
		invokeOnEDT(() -> {
			exportPdfButton.setEnabled(options != null && options.getNetworkView() != null);
			legendPanelProvider.get().update(options, filteredDataSets);
			
			if(options != null && dialog.getWidth() < legendPanelProvider.get().getNodeLegendPanel().getPreferredSize().width)
				dialog.pack();
		});
	}
	
	
	private void exportPDF() {
		Optional<File> file = FileBrowser.promptForPdfExport(fileUtil, swingApplication.getJFrame());
		if(file.isPresent()) {
			ExportLegendPDFTask exportTask = new ExportLegendPDFTask(file.get(), legendPanelProvider.get());
			Task openPdfViewerTask = new OpenPDFViewerTask(file.get());
			dialogTaskManager.execute(new TaskIterator(exportTask, openPdfViewerTask));
		}
	}
	
}
