package org.baderlab.csplugins.enrichmentmap.view.heatmap.table;

import static org.cytoscape.util.swing.LookAndFeelUtil.isAquaLAF;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.baderlab.csplugins.enrichmentmap.view.heatmap.HeatMapContentPanel;
import org.baderlab.csplugins.enrichmentmap.view.heatmap.RankingOption;
import org.baderlab.csplugins.enrichmentmap.view.util.SwingUtil;
import org.cytoscape.util.swing.IconManager;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class ColumnHeaderRankOptionRenderer implements TableCellRenderer {

	@Inject private IconManager iconManager;
	
	private final int colIndex;
	private final HeatMapContentPanel heatMapPanel;
	
	private SortOrder sortOrder;
	private MouseListener mouseListener;
	
	public interface Factory {
		ColumnHeaderRankOptionRenderer create(HeatMapContentPanel heatMapPanel, int colIndex);
	}
	
	@Inject
	public ColumnHeaderRankOptionRenderer(@Assisted HeatMapContentPanel heatMapPanel, @Assisted int colIndex) {
		this.colIndex = colIndex;
		this.heatMapPanel = heatMapPanel;
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, final Object value, boolean isSelected, boolean hasFocus, int row, int col) {
		// Convert RankingOption to display String
		String text = null;
		if(value instanceof RankingOption) {
			RankingOption rankingOption = (RankingOption) value;
			text = rankingOption.getTableHeaderText();
		} else if(value instanceof RankOptionErrorHeader) {
			RankOptionErrorHeader headerValue = (RankOptionErrorHeader) value;
			text = headerValue.getRankingOption().getTableHeaderText();
		}
		
		// Use the default renderer to paint the header nicely (with sort arrows)
		JTableHeader header = table.getTableHeader();
		TableCellRenderer delegate = table.getTableHeader().getDefaultRenderer();
		Component component = delegate.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, col);
		
		// Create the panel
		JButton button = new JButton("Sort");
		SwingUtil.makeSmall(button);
		if (isAquaLAF())
			button.putClientProperty("JButton.buttonType", "gradient");
		button.addActionListener(e -> menuButtonClicked(table, button));
		
		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.add(button, BorderLayout.CENTER);
		buttonPanel.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		buttonPanel.setForeground(header.getForeground());   
		buttonPanel.setBackground(header.getBackground());   
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.setForeground(header.getForeground());   
		panel.setBackground(header.getBackground());   
		panel.add(component, BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.NORTH);
		
		if(value instanceof RankOptionErrorHeader) {
			JLabel icon = new JLabel(IconManager.ICON_TIMES_CIRCLE);
			icon.setForeground(Color.RED.darker());
			icon.setFont(iconManager.getIconFont(13.0f));
			icon.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
			icon.setOpaque(false);
			panel.add(icon, BorderLayout.WEST);
		}
		
		// Add mouse listener
		if(mouseListener != null) {
			header.removeMouseListener(mouseListener);
		}
		
		header.addMouseListener(mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            	int col = header.columnAtPoint(e.getPoint());
            	if(col == colIndex)
            		if(e.getY() <= button.getHeight())
            			button.doClick();
					else
						sortColumn(table);
            	else
            		sortOrder = null;
            }
		});
		
		return panel;
	}
	
	private SortOrder nextSortOrder() {
		if(sortOrder == null || sortOrder == SortOrder.DESCENDING)
			return sortOrder = SortOrder.ASCENDING;
		else
			return sortOrder = SortOrder.DESCENDING;
	}
	
	private void sortColumn(JTable table) {
		TableRowSorter<?> sorter = ((TableRowSorter<?>)table.getRowSorter());
		RowSorter.SortKey sortKey = new RowSorter.SortKey(colIndex, nextSortOrder());
		sorter.setSortKeys(Arrays.asList(sortKey));
		sorter.sort();
	}
	
	private void menuButtonClicked(JTable table, JButton button) {
		JTableHeader header = table.getTableHeader();
		
		List<RankingOption> rankOptions = heatMapPanel.getAllRankingOptions();
		
		JPopupMenu menu = new JPopupMenu();
		for(RankingOption rankOption : rankOptions) {
			JMenuItem item = new JCheckBoxMenuItem(rankOption.getName());
			item.setSelected(rankOption == heatMapPanel.getSelectedRankingOption());
			SwingUtil.makeSmall(item);
			menu.add(item);
			item.addActionListener(e ->
				heatMapPanel.setSelectedRankingOption(rankOption)
			);
		}
		
		int y = button.getHeight();
		int x = 0;
		for(int i = 0; i < colIndex; i++) {
			TableColumn tableColumn = table.getColumnModel().getColumn(i);
			x += tableColumn.getWidth();
		}
		menu.show(header, x, y);
	}
	
	public void dispose(JTableHeader header) {
		if(mouseListener != null)
			header.removeMouseListener(mouseListener);
	}
}
