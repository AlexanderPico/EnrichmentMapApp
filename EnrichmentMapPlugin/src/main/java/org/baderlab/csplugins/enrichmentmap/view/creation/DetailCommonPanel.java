package org.baderlab.csplugins.enrichmentmap.view.creation;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.ArrayList;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.baderlab.csplugins.enrichmentmap.AfterInjection;
import org.baderlab.csplugins.enrichmentmap.view.util.FileBrowser;
import org.baderlab.csplugins.enrichmentmap.view.util.SwingUtil;
import org.baderlab.csplugins.enrichmentmap.view.util.dialog.Message;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.LookAndFeelUtil;

import com.google.inject.Inject;

@SuppressWarnings("serial")
public class DetailCommonPanel extends JPanel implements DetailPanel {

	@Inject private PathTextField.Factory pathTextFactory;
	
	private PathTextField gmtText;
	private PathTextField expressionsText;
	private PathTextField classText;
	
	
	@Override
	public String getIcon() {
		return IconManager.ICON_FILE_O;
	}

	@Override
	public String getDisplayName() {
		return "Common Files (included in all data sets)";
	}
	
	@Override
	public String getDataSetName() {
		return null;
	}

	@Override
	public JPanel getPanel() {
		return this;
	}
	
	@AfterInjection
	private void createContents() {
		gmtText = pathTextFactory.create("GMT File:", FileBrowser.Filter.GMT);
		expressionsText = pathTextFactory.create("Expressions:", FileBrowser.Filter.EXPRESSION);
		classText = pathTextFactory.create("Class File:", FileBrowser.Filter.CLASS);
		
		JButton resetButton = new JButton("Clear");
		SwingUtil.makeSmall(resetButton);
		resetButton.addActionListener(e -> reset());
		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
	   		
		layout.setHorizontalGroup(
			layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.TRAILING)
					.addComponent(gmtText.getLabel())
					.addComponent(expressionsText.getLabel())
					.addComponent(classText.getLabel())
				)
				.addGroup(layout.createParallelGroup(Alignment.LEADING, true)
					.addComponent(gmtText.getTextField())
					.addComponent(expressionsText.getTextField())
					.addComponent(classText.getTextField())
					.addComponent(resetButton, Alignment.TRAILING)
				)
				.addGroup(layout.createParallelGroup()
					.addComponent(gmtText.getBrowseButton())
					.addComponent(expressionsText.getBrowseButton())
					.addComponent(classText.getBrowseButton())
				)
		);
		
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(gmtText.getLabel())
					.addComponent(gmtText.getTextField())
					.addComponent(gmtText.getBrowseButton())
				)
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(expressionsText.getLabel())
					.addComponent(expressionsText.getTextField())
					.addComponent(expressionsText.getBrowseButton())
				)
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(classText.getLabel())
					.addComponent(classText.getTextField())
					.addComponent(classText.getBrowseButton())
				)
				.addComponent(resetButton)
		);
		
   		if(LookAndFeelUtil.isAquaLAF())
   			setOpaque(false);
	}
	

	@Override
	public List<Message> validateInput(MasterDetailDialogPage parent) {
		gmtText.hideError();
		expressionsText.hideError();
		
		List<Message> messages = new ArrayList<>(2);
		if(!gmtText.emptyOrReadable())
			messages.add(Message.error(gmtText.showError("GMT file path is not valid.")));
		if(!expressionsText.emptyOrReadable())
			messages.add(Message.error(expressionsText.showError("Expressions file path is not valid.")));
		if(!classText.emptyOrReadable())
			messages.add(Message.error(expressionsText.showError("Class file path is not valid.")));
		
		return messages;
	}
	
	
	public String getGmtFile() {
		return gmtText.getText();
	}
	
	public String getExprFile() {
		return expressionsText.getText();
	}
	
	public String getClassFile() {
		return classText.getText();
	}
	
	public boolean hasExprFile() {
		return !isNullOrEmpty(getExprFile());
	}
	
	public boolean hasGmtFile() {
		return !isNullOrEmpty(getGmtFile());
	}
	
	public boolean hasClassFile() {
		return !isNullOrEmpty(getClassFile());
	}

	public void reset() {
		gmtText.setText("");
		expressionsText.setText("");
		classText.setText("");
	}
	
}
