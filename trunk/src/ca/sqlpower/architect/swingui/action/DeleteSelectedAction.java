package ca.sqlpower.architect.swingui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.ArchitectRuntimeException;
import ca.sqlpower.architect.LockedColumnException;
import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLObject;
import ca.sqlpower.architect.SQLRelationship;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.architect.qfa.ArchitectExceptionReportFactory;
import ca.sqlpower.architect.swingui.ASUtils;
import ca.sqlpower.architect.swingui.ArchitectFrame;
import ca.sqlpower.architect.swingui.ArchitectSwingConstants;
import ca.sqlpower.architect.swingui.DBTree;
import ca.sqlpower.architect.swingui.PlayPen;
import ca.sqlpower.architect.swingui.PlayPenComponent;
import ca.sqlpower.architect.swingui.Relationship;
import ca.sqlpower.architect.swingui.Selectable;
import ca.sqlpower.architect.swingui.SwingUserSettings;
import ca.sqlpower.architect.swingui.TablePane;
import ca.sqlpower.architect.swingui.event.SelectionEvent;
import ca.sqlpower.architect.swingui.event.SelectionListener;

public class DeleteSelectedAction extends AbstractAction implements SelectionListener {
	private static final Logger logger = Logger.getLogger(DeleteSelectedAction.class);

	/**
	 * The PlayPen instance that is associated with this Action.
	 */
	protected PlayPen pp;

	/**
	 * The DBTree instance that is associated with this Action.
	 */
	protected DBTree dbt; 
	
	public DeleteSelectedAction() {
		super("Delete Selected",
			  ASUtils.createJLFIcon("general/Delete",
								 "Delete Selected",
								 ArchitectFrame.getMainInstance().getSprefs().getInt(SwingUserSettings.ICON_SIZE, 24)));
		putValue(SHORT_DESCRIPTION, "Delete Selected");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		putValue(ACTION_COMMAND_KEY, ArchitectSwingConstants.ACTION_COMMAND_SRC_PLAYPEN);
		setEnabled(false);
	}
	
	
	/*
	 * This action takes care of handling Delete requests from the DBTree and Playpen.
	 * 
	 * Delete Policy (Playpen):
	 * - if more than 1 item is selected in the playpen, it does not try to delete individual columns
	 * - if only a single item is selected, it will attempt to delete columns (if that item was Table)
	 * 
	 * Delete Policy (DBTree): 
	 * 
	 * 
	 */
	public void actionPerformed(ActionEvent evt) {
		logger.debug("delete action detected!");
		logger.debug("ACTION COMMAND: " + evt.getActionCommand());

		if (evt.getActionCommand().equals(ArchitectSwingConstants.ACTION_COMMAND_SRC_PLAYPEN)) {

			logger.debug("delete action came from playpen");
			List <PlayPenComponent>items = pp.getSelectedItems();			

			if (items.size() < 1) {
				JOptionPane.showMessageDialog(pp, "No items to delete!");
			}				

			if (items.size() > 1) {
				// count how many relationships and tables there are
				int tCount = pp.getSelectedTables().size();
				int rCount = pp.getSelectedRelationShips().size();
				
				int decision = JOptionPane.showConfirmDialog(pp,
															 "Are you sure you want to delete these "
															 +tCount+" tables and "+rCount+" relationships?",
															 "Multiple Delete",
															 JOptionPane.YES_NO_OPTION);
				if (decision != JOptionPane.YES_OPTION ) {
					return;
				}								
			} else { // single selection, so we might be deleting columns
				boolean deletingColumns = false;
				Selectable item = (Selectable) items.get(0);
				if (item instanceof TablePane) {
					// make a list of columns to delete
					TablePane tp = (TablePane) item;
					List<SQLColumn> selectedColumns = null;
					try {
						selectedColumns = tp.getSelectedColumns();
						if (selectedColumns.size() > 0) {
							// don't fall through into Table/Relationship delete logic							
							deletingColumns = true;
						}
					} catch (ArchitectException ae) {
						JOptionPane.showMessageDialog(pp, ae.getMessage());
						return;
					}	
				
					try {
				
                        pp.startCompoundEdit("Delete");
						
						// now, delete the columns
						Iterator it2 = selectedColumns.iterator();
						while (it2.hasNext()) {
							SQLColumn sc = (SQLColumn) it2.next();
							try {
								tp.getModel().removeColumn(sc);
							} catch (LockedColumnException ex) {
								int decision = JOptionPane.showConfirmDialog(pp,
										"Could not delete the column " + sc.getName() + " because it is part of\n" +
										"the relationship \""+ex.getLockingRelationship()+"\".\n\n" +
										"Continue deleting remaining selected columns?",
										"Column is Locked",
										JOptionPane.YES_NO_OPTION);
								if (decision == JOptionPane.NO_OPTION) {
									return;
								}
							} catch (ArchitectException e) {
								logger.error("Unexpected exception encountered when attempting to delete column '"+
										sc+"' of table '"+sc.getParentTable()+"'");
								ASUtils.showExceptionDialog(pp, "Encountered a Problem Deleting the column", e, new ArchitectExceptionReportFactory());
							}
						}
					} finally {
						pp.endCompoundEdit("Ending multi-select");
					}
					
				}
				if (deletingColumns) { // we tried to delete 1 or more columns, so don't try to delete the table
					return;
				}
			}
			
			
			pp.startCompoundEdit("Delete");
			try {
				
				// items.size() > 0, user has OK'ed the delete
			   
                //We deselect the components first because relationships might be already
                //deleted when one of the table that it was attached to got deleted. 
                //Therefore deselecting them when it comes around in the item list would
                //cause an exception.
                for (PlayPenComponent ppc : items){
			       ppc.setSelected(false);
               }
                
                Iterator it = items.iterator();
				while (it.hasNext()) {
					Selectable item = (Selectable) it.next();
					logger.debug("next item for delete is: " + item.getClass().getName());
					if (item instanceof TablePane) {
						TablePane tp = (TablePane) item;
						pp.getDatabase().removeChild(tp.getModel());
					} else if (item instanceof Relationship) {
						Relationship r = (Relationship) item;
						logger.debug("trying to delete relationship " + r);
						SQLRelationship sr = r.getModel();
						sr.getPkTable().removeExportedKey(sr);						
					} else {
						JOptionPane.showMessageDialog((JComponent) item,
						"The selected item type is not recognised");
					}
				}
			} finally {
				pp.endCompoundEdit("Ending multi-select");
			}
			
		} else if (evt.getActionCommand().equals(ArchitectSwingConstants.ACTION_COMMAND_SRC_DBTREE)) {
			logger.debug("delete action came from dbtree");
			TreePath [] selections = dbt.getSelectionPaths();
			if (selections.length > 1) {
				int decision = JOptionPane.showConfirmDialog(dbt,
															 "Are you sure you want to delete the "
															 +selections.length+" selected items?",
															 "Multiple Delete",
															 JOptionPane.YES_NO_OPTION);
				if (decision == JOptionPane.NO_OPTION) {
					return;
				}
			}	
			
			pp.startCompoundEdit("Delete");
			try {
				// FIXME: parts of the following code look like they were cut'n'pasted from above... PURE EVIL!
				Iterator it = Arrays.asList(selections).iterator();
				while (it.hasNext()) {
					TreePath tp = (TreePath) it.next();
					SQLObject so = (SQLObject) tp.getLastPathComponent();
					if (so instanceof SQLTable) {
						SQLTable st = (SQLTable) so;
						pp.getDatabase().removeChild(st);
						pp.getTablePanes().remove(st.getName().toLowerCase());
					} else if (so instanceof SQLColumn) {
						SQLColumn sc = (SQLColumn)so;
						SQLTable st = sc.getParentTable();
						try {
							st.removeColumn(sc); 
						} catch (LockedColumnException ex) {
							int decision = JOptionPane.showConfirmDialog(dbt,
									"Could not delete the column " + sc.getName() 
									+ " because it is part of a relationship key.  Continue"
									+ " deleting of other selected items?",
									"Column is Locked",
									JOptionPane.YES_NO_OPTION);
							if (decision == JOptionPane.NO_OPTION) {
								return;
							}
						} catch (ArchitectException e) {
							logger.error("Unexpected exception encountered when attempting to delete column '"+
									sc+"' of table '"+sc.getParentTable()+"'");
							ASUtils.showExceptionDialog(pp, "Encountered a Problem Deleting the column", e, new ArchitectExceptionReportFactory());
						}
					} else if (so instanceof SQLRelationship) {
						SQLRelationship sr = (SQLRelationship) so;
						sr.getPkTable().removeExportedKey(sr);
						sr.getFkTable().removeImportedKey(sr);
					} else {
						JOptionPane.showMessageDialog(dbt, "The selected SQLObject type is not recognised: " + so.getClass().getName());
					}
				}
			} finally {
				pp.endCompoundEdit("Ending multi-select");
                
                /* 
                 * We need to disable the delete function right after
                 * since the "Delete Selected" function in the dbtree does
                 * not update the status of the delete function in the playpen
                 * tool bar and could lead to the button going into a bad state
                 * Therefore we disable it after the deletion is done and have 
                 * it update again (to maybe re-enable by the selectionlistener.
                 */
                setEnabled(false);
			}
			
		} else {
			logger.debug("delete action came from unknown source, so we do nothing.");
	  		// unknown action command source, do nothing
		}		
	}
	
	public void setPlayPen(PlayPen newPP) throws ArchitectException {
		if (pp != null) {
			pp.removeSelectionListener(this);
		} 
		pp = newPP;
		pp.addSelectionListener(this);
		
		setupAction(pp.getSelectedItems());
	}

	public void setDBTree(DBTree newDBT) {
		this.dbt = newDBT;
	}
	
	public void itemSelected(SelectionEvent e) {
		try {
			setupAction(pp.getSelectedItems());
		} catch (ArchitectException e1) {
			throw new ArchitectRuntimeException(e1);
		}
	}

	public void itemDeselected(SelectionEvent e) {
		try {
			setupAction(pp.getSelectedItems());
		} catch (ArchitectException e1) {
			throw new ArchitectRuntimeException(e1);
		}
	}

	/**
	 * Updates the tooltip and enabledness of this action based on how
	 * many items are in the selection list.  If there is only one
	 * selected item, tries to put its name in the tooltip too!
	 * @throws ArchitectException 
	 */
	private void setupAction(List selectedItems) throws ArchitectException {
		if (selectedItems.size() == 0) {
			setEnabled(false);
			putValue(SHORT_DESCRIPTION, "Delete Selected");
		} else if (selectedItems.size() == 1) {
			Selectable item = (Selectable) selectedItems.get(0);
			setEnabled(true);
			String name = "Selected";
			if (item instanceof TablePane) {
				TablePane tp = (TablePane) item;
				if (tp.getSelectedColumnIndex() >= 0) {
					try {
						List<SQLColumn> selectedColumns = tp.getSelectedColumns();
						if (selectedColumns.size() > 1) {
							name = selectedColumns.size()+" items";
						} else {
							name = tp.getModel().getColumn(tp.getSelectedColumnIndex()).getName();
						}
					} catch (ArchitectException ex) {
						logger.error("Couldn't get selected column name", ex);
					}
				} else {
					name = tp.getModel().getName();
				}
			} else if (item instanceof Relationship) {
				name = ((Relationship) item).getModel().getName();
			}
			putValue(SHORT_DESCRIPTION, "Delete "+name);
		} else {
			setEnabled(true);
			int numSelectedItems =0;
			for (Object item : selectedItems) {
				numSelectedItems++;
				if (item instanceof TablePane) {
					// Because the table pane is already counted we need to add one less 
					// than the columns unless there are no columns selected.  Then
					// We need to add 0
					numSelectedItems += Math.max(((TablePane) item).getSelectedColumns().size()-1, 0);				
				}
			}
			putValue(SHORT_DESCRIPTION, "Delete "+numSelectedItems+" items");            
		}        
	}
}
