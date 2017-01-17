/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 * Contributors:
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.api.model.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.schema.types.ComplexTypeImpl;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * Property used to declare property removed from schema.
 *
 * @since 9.1
 */
public class RemovedProperty extends AbstractProperty {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(RemovedProperty.class);

    private final String fieldName;

    private final Property fallback;

    public RemovedProperty(Property parent, String fieldName) {
        this(parent, fieldName, null);
    }

    public RemovedProperty(Property parent, String fieldName, Property fallback) {
        super(parent);
        this.fieldName = fieldName;
        this.fallback = fallback;
    }

    @Override
    public void internalSetValue(Serializable value) throws PropertyException {
        StringBuilder msg = newRemovedMessage();
        if (fallback == null) {
            msg.append("Do nothing");
        } else {
            msg.append("Fallback on ").append(fallback);
        }
        log.warn(msg);
        if (fallback != null) {
            fallback.setValue(value);
        }
    }

    @Override
    public Serializable internalGetValue() throws PropertyException {
        StringBuilder msg = newRemovedMessage();
        if (fallback == null) {
            msg.append("Return null");
        } else {
            msg.append("Fallback on ").append(fallback);
        }
        log.warn(msg);
        if (fallback == null) {
            return null;
        }
        return fallback.getValue();
    }

    private StringBuilder newRemovedMessage() {
        return new StringBuilder().append("Field '")
                                  .append(getRemovedParent().getName())
                                  .append("' is marked as removed from '")
                                  .append(getSchema().getName())
                                  .append("' schema, don't use it anymore. ");
    }

    private RemovedProperty getRemovedParent() {
        RemovedProperty property = this;
        while (property.getParent() instanceof RemovedProperty) {
            property = (RemovedProperty) property.getParent();
        }
        return property;
    }

    @Override
    public String getName() {
        return fieldName;
    }

    @Override
    public Type getType() {
        if (fallback == null) {
            // TODO try to do something better - currently RemovedProperty is always a container if there's no fallback
            // Simulate a complex type
            return new ComplexTypeImpl(getSchema(), getSchema().getName(), fieldName);
        }
        return fallback.getType();
    }

    @Override
    public boolean isContainer() {
        // TODO try to do something better - currently RemovedProperty is always a container if there's no fallback
        return fallback == null || fallback.isContainer();
    }

    @Override
    public Collection<Property> getChildren() {
        throw new UnsupportedOperationException("Removed properties don't have children");
    }

    @Override
    public Property get(String name) throws PropertyNotFoundException {
        if (fallback != null) {
            // TODO do we wrap again the fallback child ?
            return fallback.get(name);
        } else if (name.matches("\\d+")) {
            return get(Integer.parseInt(name));
        }
        // TODO try to do something better - currently RemovedProperty is always a container if there's no fallback
        return new RemovedProperty(this, name);
    }

    @Override
    public Property get(int index) throws PropertyNotFoundException {
        // TODO try to do something better - currently RemovedProperty is always a container if there's no fallbackr
        return new RemovedProperty(this, fieldName);
    }

    @Override
    public Property addValue(Object value) throws PropertyException {
        throw new UnsupportedOperationException("Removed properties don't have children");
    }

    @Override
    public Property addValue(int index, Object value) throws PropertyException {
        throw new UnsupportedOperationException("Removed properties don't have children");
    }

    @Override
    public Property addEmpty() throws PropertyException {
        throw new UnsupportedOperationException("Removed properties don't have children");
    }

    @Override
    public Field getField() {
        throw new UnsupportedOperationException("Removed properties don't have field");
    }

    @Override
    public void accept(PropertyVisitor visitor, Object arg) throws PropertyException {
        // Nothing to do
    }

    @Override
    public boolean isSameAs(Property property) throws PropertyException {
        if (!(property instanceof RemovedProperty)) {
            return false;
        }
        RemovedProperty rp = (RemovedProperty) property;
        return Objects.equals(getSchema(), rp.getSchema()) && Objects.equals(getName(), getName())
                && Objects.equals(getRemovedParent().getName(), rp.getRemovedParent().getName());
    }

    @Override
    public Iterator<Property> getDirtyChildren() {
        throw new UnsupportedOperationException("Removed properties don't have children");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getPath().substring(1) + ")";
    }

}
