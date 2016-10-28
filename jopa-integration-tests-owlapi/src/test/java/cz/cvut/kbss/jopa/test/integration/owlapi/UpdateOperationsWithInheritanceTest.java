package cz.cvut.kbss.jopa.test.integration.owlapi;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.test.environment.Triple;
import cz.cvut.kbss.jopa.test.environment.OwlapiDataPersist;
import cz.cvut.kbss.jopa.test.environment.OwlapiPersistenceFactory;
import cz.cvut.kbss.jopa.test.runner.UpdateOperationsWithInheritanceRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class UpdateOperationsWithInheritanceTest extends UpdateOperationsWithInheritanceRunner {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateOperationsWithInheritanceTest.class);

    private final OwlapiPersistenceFactory persistenceFactory;
    private final OwlapiDataPersist dataPersist;

    public UpdateOperationsWithInheritanceTest() {
        super(LOG);
        this.persistenceFactory = new OwlapiPersistenceFactory();
        this.dataPersist = new OwlapiDataPersist();
    }

    @Override
    protected EntityManager getEntityManager(String repositoryName, boolean cacheEnabled) {
        return getEntityManager(repositoryName, cacheEnabled, Collections.emptyMap());
    }

    @Override
    protected EntityManager getEntityManager(String repositoryName, boolean cacheEnabled,
                                             Map<String, String> properties) {
        return persistenceFactory.getEntityManager(repositoryName, cacheEnabled, properties);
    }

    @Override
    protected void persistTestData(Collection<Triple> data, EntityManager em) throws Exception {
        dataPersist.persistTestData(data, em);
    }
}
