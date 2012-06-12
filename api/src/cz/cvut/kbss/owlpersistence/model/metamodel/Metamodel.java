/**
 * Copyright (C) 2011 Czech Technical University in Prague
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

package cz.cvut.kbss.owlpersistence.model.metamodel;

import java.util.Set;

import cz.cvut.kbss.owlpersistence.UnusedJPA;

/**
 * Provides access to the metamodel of persistent entities in the persistence
 * unit.
 */
public interface Metamodel {
	/**
	 * Return the metamodel entity type representing the entity.
	 * 
	 * @param cls
	 *            the type of the represented entity
	 * @return the metamodel entity type
	 * @throws IllegalArgumentException
	 *             if not an entity
	 */
	<X> EntityType<X> entity(Class<X> cls);

	/**
	 * Return the metamodel managed type representing the entity, mapped
	 * superclass, or embeddable class.
	 * 
	 * @param cls
	 *            the type of the represented managed class
	 * @return the metamodel managed type
	 * @throws IllegalArgumentException
	 *             if not a managed class
	 */
	@UnusedJPA
	@Deprecated
	<X> ManagedType<X> managedType(Class<X> cls);

	/**
	 * Return the metamodel embeddable type representing the embeddable class.
	 * 
	 * @param cls
	 *            the type of the represented embeddable class
	 * @return the metamodel embeddable type
	 * @throws IllegalArgumentException
	 *             if not an embeddable class
	 */
	@UnusedJPA
	@Deprecated
	<X> EmbeddableType<X> embeddable(Class<X> cls);

	/**
	 * Return the metamodel managed types.
	 * 
	 * @return the metamodel managed types
	 */
	@UnusedJPA
	@Deprecated
	Set<ManagedType<?>> getManagedTypes();

	/**
	 * Return the metamodel entity types.
	 * 
	 * @return the metamodel entity types
	 */
	Set<EntityType<?>> getEntities();

	/**
	 * Return the metamodel embeddable types. Returns empty set if there are no
	 * embeddable types.
	 * 
	 * @return the metamodel embeddable types
	 */
	@UnusedJPA
	@Deprecated
	Set<EmbeddableType<?>> getEmbeddables();

	/**
	 * Get the set of classes that contain inferred attributes. These classes
	 * are handled specially since inferred attributes can be influenced by
	 * changes to any other attributes in any other entity.
	 * 
	 * @return The set of classes with inferred attributes.
	 */
	public Set<Class<?>> getInferredClasses();

	/**
	 * Returns true if the persistence can and should use AspectJ for weaving
	 * entities and their properties. AspectJ is essential to lazy loading
	 * functionality, this without it all entities and their properties should
	 * be eager loaded.
	 * 
	 * @return True if AspectJ is available and should be used.
	 */
	public boolean shouldUseAspectJ();
}