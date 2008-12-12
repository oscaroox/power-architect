/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.architect.swingui;

import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import ca.sqlpower.architect.ArchitectVersion;
import ca.sqlpower.swingui.AbstractNoEditDataEntryPanel;

public class AboutPanel extends AbstractNoEditDataEntryPanel {

	public JLabel content;

	public AboutPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.add(Messages.getString("AboutPanel.aboutTab"), initAboutTab()); //$NON-NLS-1$
        tabs.add(Messages.getString("AboutPanel.systemPropertiesTab"), new JScrollPane(initSysPropsTab())); //$NON-NLS-1$

        tabs.setSelectedIndex(0);
        add(tabs);
	}

    private JComponent initAboutTab() {
        JPanel pan = new JPanel();
		pan.setLayout(new FlowLayout());

        // Include the Power*Architect Icon!
        String realPath = "/icons/architect128.png"; //$NON-NLS-1$
        java.net.URL imgURL = ASUtils.class.getResource(realPath);

        if (imgURL != null) {
            ImageIcon imageIcon = new ImageIcon(imgURL, "Architect Logo"); //$NON-NLS-1$
            pan.add(new JLabel(imageIcon));
        }

		content = new JLabel("<html>Power*Architect "+ //$NON-NLS-1$
		                    ArchitectVersion.APP_VERSION+"<br><br>" + //$NON-NLS-1$
							Messages.getString("AboutPanel.copyright") + "<br>" + //$NON-NLS-1$ //$NON-NLS-2$
							"</html>"); //$NON-NLS-1$
		pan.add(content);
        return pan;
	}

    /**
     * A JTable model to display the system properties
     */
    private class SystemPropertiesTableModel extends AbstractTableModel {
        private Properties props = System.getProperties();
        private String[] keys;

        SystemPropertiesTableModel() {
            keys = props.keySet().toArray(new String[0]);
            Arrays.sort(keys);
        }

        public int getRowCount() {
            return keys.length;
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int row, int column) {
            switch(column) {
            case 0: return keys[row];
            case 1: return props.get(keys[row]);
            default: throw new IllegalArgumentException("Column count"); //$NON-NLS-1$
            }
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
            case 0: return Messages.getString("AboutPanel.systemProperty"); //$NON-NLS-1$
            case 1: return Messages.getString("AboutPanel.systemPropertyValue"); //$NON-NLS-1$
            default: throw new IllegalArgumentException("Column count"); //$NON-NLS-1$
            }
        }
    }

    private JComponent initSysPropsTab() {
        JTable table = new JTable(new SystemPropertiesTableModel());

        // tailor column 1 width
        table.getColumnModel().getColumn(0).setMinWidth(200);

        // Want column 2 wide enough to show CLASSPATH
        table.getColumnModel().getColumn(1).setMinWidth(2000);

        table.setSize(table.getPreferredSize());

        return table;
    }
}