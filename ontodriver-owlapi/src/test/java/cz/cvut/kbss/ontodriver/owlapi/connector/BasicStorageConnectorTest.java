package cz.cvut.kbss.ontodriver.owlapi.connector;

import cz.cvut.kbss.ontodriver.OntologyConnectorType;
import cz.cvut.kbss.ontodriver.OntologyStorageProperties;
import cz.cvut.kbss.ontodriver.owlapi.exception.InvalidOntologyIriException;
import cz.cvut.kbss.ontodriver.owlapi.util.MutableAddAxiom;
import org.junit.After;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.*;

public class BasicStorageConnectorTest {

    private static final URI ONTOLOGY_URI = URI.create("http://krizik.felk.cvut.cz/ontologies/jopa/connector");

    private AbstractConnector connector;

    @After
    public void tearDown() throws Exception {
        if (connector != null) {
            connector.close();
        }
    }

    private OntologyStorageProperties initStorageProperties(URI filePath, URI logicalUri) {
        return OntologyStorageProperties.connectorType(
                OntologyConnectorType.OWLAPI).ontologyUri(logicalUri != null ? logicalUri : ONTOLOGY_URI).physicalUri(
                filePath).build();
    }

    @Test
    public void loadsExistingOntology() throws Exception {
        final URI physicalUri = initOntology();
        this.connector = new BasicStorageConnector(initStorageProperties(physicalUri, null),
                Collections.<String, String>emptyMap());
        assertNotNull(connector);
        assertTrue(connector.isOpen());
        final OntologyStructures snapshot = connector.getOntologySnapshot();
        assertNotNull(snapshot.getOntology());
        assertEquals(IRI.create(ONTOLOGY_URI), snapshot.getOntology().getOntologyID().getOntologyIRI().get());
        assertNotNull(snapshot.getOntologyManager());
        assertNotNull(snapshot.getDataFactory());
    }

    private URI initOntology() throws Exception {
        final File targetFile = Files.createTempFile("connectortest", ".owl").toFile();
        targetFile.deleteOnExit();
        final OWLOntologyManager om = OWLManager.createOWLOntologyManager();
        final OWLOntology o = om.createOntology(IRI.create(ONTOLOGY_URI));
        om.saveOntology(o, IRI.create(targetFile));
        return targetFile.toURI();
    }

    @Test(expected = InvalidOntologyIriException.class)
    public void throwsExceptionWhenLoadedOntologyHasDifferentIri() throws Exception {
        final URI physicalUri = initOntology();
        final URI logicalUri = URI.create("http://krizik.felk.cvut.cz/ontologies/jopa/different");
        this.connector = new BasicStorageConnector(initStorageProperties(physicalUri, logicalUri),
                Collections.<String, String>emptyMap());
    }

    @Test
    public void createsNewFileForOntologyWithNonExistentPhysicalLocation() throws Exception {
        final File f = new File(System.getProperty(
                "java.io.tmpdir") + File.separator + "connectortest" + System.currentTimeMillis() + ".owl");
        assertFalse(f.exists());
        final URI physicalUri = f.toURI();
        this.connector = new BasicStorageConnector(initStorageProperties(physicalUri, null),
                Collections.<String, String>emptyMap());
        assertNotNull(connector);
        assertTrue(f.exists());
        f.deleteOnExit();
    }

    @Test
    public void getSnapshotReturnsDistinctSnapshots() throws Exception {
        final URI physicalUri = initOntology();
        this.connector = new BasicStorageConnector(initStorageProperties(physicalUri, null),
                Collections.<String, String>emptyMap());
        final OntologyStructures snapshotOne = connector.getOntologySnapshot();
        final OntologyStructures snapshotTwo = connector.getOntologySnapshot();

        assertNotSame(snapshotOne.getOntology(), snapshotTwo.getOntology());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionWhenTryingToGetSnapshotOfClosedConnector() throws Exception {
        final URI physicalUri = initOntology();
        this.connector = new BasicStorageConnector(initStorageProperties(physicalUri, null),
                Collections.<String, String>emptyMap());
        connector.close();
        assertFalse(connector.isOpen());
        connector.getOntologySnapshot();
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionWhenApplyChangesCalledOnClose() throws Exception {
        final URI physicalUri = initOntology();
        this.connector = new BasicStorageConnector(initStorageProperties(physicalUri, null),
                Collections.<String, String>emptyMap());
        connector.close();
        assertFalse(connector.isOpen());
        connector.applyChanges(Collections.<OWLOntologyChange>emptyList());
    }

    @Test
    public void applyChangesModifiesTheCentralOntology() throws Exception {
        final URI physicalUri = initOntology();
        this.connector = new BasicStorageConnector(initStorageProperties(physicalUri, null),
                Collections.<String, String>emptyMap());
        final OntologyStructures snapshot = connector.getOntologySnapshot();
        final OWLClass cls = addClassToOntology(snapshot);
        final OntologyStructures result = connector.getOntologySnapshot();
        assertTrue(result.getOntology().containsClassInSignature(cls.getIRI()));
    }

    private OWLClass addClassToOntology(OntologyStructures snapshot) {
        final OWLClass cls = snapshot.getDataFactory().getOWLClass(
                IRI.create("http://krizik.felk.cvut.cz/ontologies/jopa#OWClassA"));
        final OWLAxiom classDeclaration = snapshot.getDataFactory().getOWLDeclarationAxiom(cls);
        final MutableAddAxiom add = new MutableAddAxiom(snapshot.getOntology(), classDeclaration);

        connector.applyChanges(Collections.<OWLOntologyChange>singletonList(add));
        return cls;
    }

    @Test
    public void successfullySavesOntologyOnClose() throws Exception {
        final URI physicalUri = initOntology();
        final OntologyStorageProperties storageProperties = initStorageProperties(physicalUri, null);
        this.connector = new BasicStorageConnector(storageProperties, Collections.<String, String>emptyMap());
        final OWLClass cls = addClassToOntology(connector.getOntologySnapshot());
        connector.close();

        this.connector = new BasicStorageConnector(storageProperties, Collections.<String, String>emptyMap());
        final OntologyStructures res = connector.getOntologySnapshot();
        assertTrue(res.getOntology().containsClassInSignature(cls.getIRI()));
    }
}
