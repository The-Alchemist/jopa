package cz.cvut.kbss.jopa.test.integration.owlapi;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.test.environment.OwlapiDataPersist;
import cz.cvut.kbss.jopa.test.environment.OwlapiPersistenceFactory;
import cz.cvut.kbss.jopa.test.environment.Triple;
import cz.cvut.kbss.jopa.test.runner.DeleteOperationsWithInheritanceRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class DeleteOperationsWithInheritanceTest extends DeleteOperationsWithInheritanceRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteOperationsWithInheritanceTest.class);

    private final OwlapiPersistenceFactory persistenceFactory;
    private final OwlapiDataPersist dataPersist;

    public DeleteOperationsWithInheritanceTest() throws Exception {
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

    /**
     * Ignored due to bug in OWL2Query, see {@link #verifyIndividualWasRemoved(URI)}
     */
    @Ignore
    @Test
    @Override
    public void testRemoveEntityWithMappedSuperclass() {
        super.testRemoveEntityWithMappedSuperclass();
    }

    /**
     * Ignored due to bug in OWL2Query, see {@link #verifyIndividualWasRemoved(URI)}
     */
    @Ignore
    @Test
    @Override
    public void testRemoveEntityWithEntitySuperclass() {
        super.testRemoveEntityWithEntitySuperclass();
    }
}
