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
package cz.cvut.kbss.jopa.test.environment;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.JOPAPersistenceProperties;
import cz.cvut.kbss.ontodriver.config.OntoDriverProperties;

import java.util.HashMap;
import java.util.Map;

public class OwlapiPersistenceFactory {

    private static final StorageConfig storage = initStorage();
    private static final Map<String, String> defaultProperties = initProperties();

    public EntityManager getEntityManager(String repositoryName, boolean cacheEnabled, Map<String, String> properties) {
        assert properties != null;
        final Map<String, String> actualProperties = new HashMap<>(defaultProperties);
        actualProperties.putAll(properties);
        return TestEnvironment
                .getPersistenceConnector("Owlapi" + repositoryName, storage, cacheEnabled, actualProperties);
    }

    private static StorageConfig initStorage() {
        return new OwlapiStorageConfig();
    }

    private static Map<String, String> initProperties() {
        final Map<String, String> map = new HashMap<>();
        map.put(OntoDriverProperties.USE_TRANSACTIONAL_ONTOLOGY, Boolean.TRUE.toString());
        map.put(JOPAPersistenceProperties.LANG, "en");
        map.put(OntoDriverProperties.OWLAPI_REASONER_FACTORY_CLASS, PelletReasonerFactory.class.getName());
        return map;
    }
}
