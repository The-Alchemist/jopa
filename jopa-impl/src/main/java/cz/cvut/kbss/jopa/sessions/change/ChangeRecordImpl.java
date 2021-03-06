/**
 * Copyright (C) 2016 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jopa.sessions.change;

import cz.cvut.kbss.jopa.model.metamodel.FieldSpecification;
import cz.cvut.kbss.jopa.sessions.ChangeRecord;

public class ChangeRecordImpl implements ChangeRecord {

    private final FieldSpecification<?, ?> attribute;

    private final Object newValue;

    public ChangeRecordImpl(FieldSpecification<?, ?> att, Object value) {
        assert att != null;
        this.attribute = att;
        this.newValue = value;
    }

    @Override
    public Object getNewValue() {
        return newValue;
    }

    @Override
    public FieldSpecification<?, ?> getAttribute() {
        return attribute;
    }

    @Override
    public String toString() {
        return "ChangeRecordImpl{" +
                "attributeName='" + attribute.getName() + '\'' +
                ", newValue=" + newValue +
                '}';
    }
}
