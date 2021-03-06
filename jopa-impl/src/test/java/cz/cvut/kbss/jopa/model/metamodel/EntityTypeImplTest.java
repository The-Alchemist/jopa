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
package cz.cvut.kbss.jopa.model.metamodel;

import cz.cvut.kbss.jopa.environment.*;
import cz.cvut.kbss.jopa.model.IRI;
import cz.cvut.kbss.jopa.model.annotations.InheritanceType;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class EntityTypeImplTest {

    private static Class<OWLClassA> cls;
    private static IRI classIri;
    private static String className;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        cls = OWLClassA.class;
        classIri = IRI.create(OWLClassA.getClassIri());
        className = OWLClassA.class.getName();
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAttributeThrowsIAEWhenAttributeIsNotPresent() throws Exception {
        final EntityType<OWLClassA> et = new EntityTypeImpl<>(className, cls, classIri);
        et.getAttribute("someUnknownAttribute");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDeclaredAttributeThrowsIAEWhenAttributeIsNotPresent() throws Exception {
        final EntityType<OWLClassA> et = new EntityTypeImpl<>(className, cls, classIri);
        et.getDeclaredAttribute("someUnknownAttribute");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFieldSpecificationThrowsIAEWhenAttributeIsNotPresent() throws Exception {
        final EntityType<OWLClassA> et = new EntityTypeImpl<>(className, cls, classIri);
        et.getFieldSpecification("someUnknownAttribute");
    }

    @Test
    public void getFieldSpecificationReturnsTypesIfNameMatches() throws Exception {
        final EntityTypeImpl<OWLClassA> et = new EntityTypeImpl<>(className, cls, classIri);
        final TypesSpecification typesSpec = mock(TypesSpecification.class);
        when(typesSpec.getName()).thenReturn(OWLClassA.getTypesField().getName());
        when(typesSpec.getJavaField()).thenReturn(OWLClassA.getTypesField());
        et.addDirectTypes(typesSpec);

        assertEquals(typesSpec, et.getFieldSpecification(typesSpec.getName()));
    }

    @Test
    public void setSupertypeSetsInheritanceStrategyFromTheSupertypeWhenItIsEntity() throws Exception {
        final EntityTypeImpl<OWLClassR> rEntityType = new EntityTypeImpl<>(OWLClassR.class.getName(),
                OWLClassR.class, IRI.create(OWLClassR.getClassIri()));
        final EntityTypeImpl<OWLClassS> sEntityType = spy(new EntityTypeImpl<>(OWLClassS.class.getName(),
                OWLClassS.class, IRI.create(OWLClassS.getClassIri())));
        sEntityType.setInheritanceType(InheritanceType.TRY_FIRST);
        rEntityType.setSupertype(sEntityType);

        assertEquals(InheritanceType.TRY_FIRST, rEntityType.getInheritanceType());
    }

    @Test
    public void setSupertypeSkipsInheritanceStrategyWhenSupertypeIsNotEntity() throws Exception {
        final EntityTypeImpl<OWLClassQ> qEntityType = new EntityTypeImpl<>(OWLClassQ.class.getName(), OWLClassQ.class,
                IRI.create(OWLClassQ.getClassIri()));
        final MappedSuperclassTypeImpl<QMappedSuperclass> superclassType = new MappedSuperclassTypeImpl<>(
                QMappedSuperclass.class);
        qEntityType.setSupertype(superclassType);
        assertNull(qEntityType.getInheritanceType());
    }
}
