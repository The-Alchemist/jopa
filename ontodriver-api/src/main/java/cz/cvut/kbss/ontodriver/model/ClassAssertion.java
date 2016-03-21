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
package cz.cvut.kbss.ontodriver.model;

import cz.cvut.kbss.ontodriver.util.Vocabulary;

import java.net.URI;

class ClassAssertion extends Assertion {

    private static final long serialVersionUID = 7238514047567604200L;

    static final URI RDF_TYPE = URI.create(Vocabulary.RDF_TYPE);

    ClassAssertion(boolean isInferred) {
        super(RDF_TYPE, isInferred);
    }

    @Override
    public AssertionType getType() {
        return AssertionType.CLASS;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        return prime * super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        return true;
    }
}