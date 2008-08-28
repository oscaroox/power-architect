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

package ca.sqlpower.architect.undo;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Method;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;

/**
 * This is the generic edit class that dynamically modifies bean properties
 * according to the PropertyChangeEvent source and the property name.
 * 
 * @author kaiyi
 *
 */
public class PropertyChangeEdit extends AbstractUndoableEdit {
    
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(PropertyChangeEdit.class);

    private final PropertyChangeEvent sourceEvent;

    public PropertyChangeEdit(PropertyChangeEvent e) {
        this.sourceEvent = e;
    }

    /**
     * Sets the value of the property to be the old value
     */
    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        try {
            Method setter = PropertyUtils.getWriteMethod(PropertyUtils.getPropertyDescriptor(sourceEvent.getSource(), sourceEvent.getPropertyName()));
            setter.invoke(sourceEvent.getSource(), sourceEvent.getOldValue());

        } catch (Exception ex) {
            CannotUndoException wrapper = new CannotUndoException();
            wrapper.initCause(ex);
            throw wrapper;
        }
    }

    /**
     * Sets the value of the property to be the new value
     */
    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        try {
            Method setter = PropertyUtils.getWriteMethod(PropertyUtils.getPropertyDescriptor(sourceEvent.getSource(), sourceEvent.getPropertyName()));
            setter.invoke(sourceEvent.getSource(), sourceEvent.getNewValue());

        } catch (Exception ex) {
            CannotRedoException wrapper = new CannotRedoException();
            wrapper.initCause(ex);
            throw wrapper;
        }
    }

    @Override
    public String getPresentationName() {
        return "property change edit";
    }

    @Override
    public String toString() {
        return "Changing property: \"" + sourceEvent.getPropertyName() + "\" by "+sourceEvent;
    }
}
