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
package cz.cvut.kbss.ontodriver.owlapi.connector;

import cz.cvut.kbss.ontodriver.OntologyStorageProperties;
import cz.cvut.kbss.ontodriver.config.Configuration;
import cz.cvut.kbss.ontodriver.owlapi.OwlapiDataSource;
import cz.cvut.kbss.ontodriver.owlapi.environment.Generator;
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
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

public class BasicStorageConnectorTest {

    private static final URI ONTOLOGY_URI = URI.create("http://krizik.felk.cvut.cz/ontologies/jopa/connector");

    private BasicStorageConnector connector;

    private OWLOntologyManager manager;
    private OWLOntology ontology;

    @After
    public void tearDown() throws Exception {
        if (connector != null) {
            connector.close();
        }
    }

    private OntologyStorageProperties initStorageProperties(URI filePath, URI logicalUri) {
        return OntologyStorageProperties.ontologyUri(logicalUri != null ? logicalUri : ONTOLOGY_URI).physicalUri(
                filePath).driver(OwlapiDataSource.class.getCanonicalName()).build();
    }

    @Test
    public void loadsExistingOntology() throws Exception {
        final URI physicalUri = initOntology(Collections.emptySet());
        this.connector = new BasicStorageConnector(new Configuration(initStorageProperties(physicalUri, null)));
        assertNotNull(connector);
        assertTrue(connector.isOpen());
        final OntologySnapshot snapshot = connector.getOntologySnapshot();
        assertNotNull(snapshot.getOntology());
        assertNotNull(snapshot.getOntologyManager());
        assertNotNull(snapshot.getDataFactory());
    }

    private URI initOntology(Set<OWLAxiom> axioms) throws Exception {
        final File targetFile = Files.createTempFile("connectortest", ".owl").toFile();
        targetFile.deleteOnExit();
        final OWLOntologyManager om = OWLManager.createOWLOntologyManager();
        final OWLOntology o = om.createOntology(IRI.create(ONTOLOGY_URI));
        om.addAxioms(o, axioms);
        om.saveOntology(o, IRI.create(targetFile));
        this.manager = om;
        this.ontology = o;
        return targetFile.toURI();
    }

    @Test(expected = InvalidOntologyIriException.class)
    public void throwsExceptionWhenLoadedOntologyHasDifferentIri() throws Exception {
        final URI physicalUri = initOntology(Collections.emptySet());
        final URI logicalUri = URI.create("http://krizik.felk.cvut.cz/ontologies/jopa/different");
        this.connector = new BasicStorageConnector(new Configuration(initStorageProperties(physicalUri, logicalUri)));
    }

    @Test
    public void createsNewFileForOntologyWithNonExistentPhysicalLocation() throws Exception {
        final File f = new File(System.getProperty(
                "java.io.tmpdir") + File.separator + "connectortest" + System.currentTimeMillis() + ".owl");
        assertFalse(f.exists());
        final URI physicalUri = f.toURI();
        this.connector = new BasicStorageConnector(new Configuration(initStorageProperties(physicalUri, null)));
        assertNotNull(connector);
        assertTrue(f.exists());
        f.deleteOnExit();
    }

    @Test
    public void getSnapshotReturnsDistinctSnapshots() throws Exception {
        final URI physicalUri = initOntology(Collections.emptySet());
        this.connector = new BasicStorageConnector(new Configuration(initStorageProperties(physicalUri, null)));
        final OntologySnapshot snapshotOne = connector.getOntologySnapshot();
        final OntologySnapshot snapshotTwo = connector.getOntologySnapshot();

        assertNotSame(snapshotOne.getOntology(), snapshotTwo.getOntology());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionWhenTryingToGetSnapshotOfClosedConnector() throws Exception {
        final URI physicalUri = initOntology(Collections.emptySet());
        this.connector = new BasicStorageConnector(new Configuration(initStorageProperties(physicalUri, null)));
        connector.close();
        assertFalse(connector.isOpen());
        connector.getOntologySnapshot();
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionWhenApplyChangesCalledOnClose() throws Exception {
        final URI physicalUri = initOntology(Collections.emptySet());
        this.connector = new BasicStorageConnector(new Configuration(initStorageProperties(physicalUri, null)));
        connector.close();
        assertFalse(connector.isOpen());
        connector.applyChanges(Collections.emptyList());
    }

    @Test
    public void applyChangesModifiesTheCentralOntology() throws Exception {
        final URI physicalUri = initOntology(Collections.emptySet());
        this.connector = new BasicStorageConnector(new Configuration(initStorageProperties(physicalUri, null)));
        final OntologySnapshot snapshot = connector.getOntologySnapshot();
        final OWLClass cls = addClassToOntology(snapshot);
        final OntologySnapshot result = connector.getOntologySnapshot();
        assertTrue(result.getOntology().containsClassInSignature(cls.getIRI()));
    }

    private OWLClass addClassToOntology(OntologySnapshot snapshot) {
        final OWLClass cls = snapshot.getDataFactory().getOWLClass(
                IRI.create("http://krizik.felk.cvut.cz/ontologies/jopa#OWClassA"));
        final OWLAxiom classDeclaration = snapshot.getDataFactory().getOWLDeclarationAxiom(cls);
        final MutableAddAxiom add = new MutableAddAxiom(snapshot.getOntology(), classDeclaration);

        connector.applyChanges(Collections.singletonList(add));
        return cls;
    }

    @Test
    public void successfullySavesOntologyOnClose() throws Exception {
        final URI physicalUri = initOntology(Collections.emptySet());
        final OntologyStorageProperties storageProperties = initStorageProperties(physicalUri, null);
        this.connector = new BasicStorageConnector(new Configuration(storageProperties));
        final OWLClass cls = addClassToOntology(connector.getOntologySnapshot());
        connector.close();

        this.connector = new BasicStorageConnector(new Configuration(storageProperties));
        final OntologySnapshot res = connector.getOntologySnapshot();
        assertTrue(res.getOntology().containsClassInSignature(cls.getIRI()));
    }

    @Test
    public void getSnapshotCreatesNewAnonymousTransactionalOntology() throws Exception {
        final URI physicalUri = initOntology(Collections.emptySet());
        final OntologyStorageProperties storageProperties = initStorageProperties(physicalUri, null);
        this.connector = new BasicStorageConnector(new Configuration(storageProperties));
        final OntologySnapshot snapshot = connector.getOntologySnapshot();
        assertTrue(snapshot.getOntology().getOntologyID().isAnonymous());
    }

    @Test
    public void getSnapshotCopiesAxiomsIntoTheTransactionOntology() throws Exception {
        final Set<OWLAxiom> axioms = Generator.generateAxioms();
        final URI physicalUri = initOntology(axioms);
        final OntologyStorageProperties storageProperties = initStorageProperties(physicalUri, null);
        this.connector = new BasicStorageConnector(new Configuration(storageProperties));
        final OntologySnapshot snapshot = connector.getOntologySnapshot();
        final Set<OWLAxiom> transactionalAxioms = snapshot.getOntology().getAxioms();
        assertTrue(transactionalAxioms.containsAll(axioms));
    }

    @Test
    public void closeSnapshotRemovesTransactionalOntologyFromManager() throws Exception {
        final URI physicalUri = initOntology(Collections.emptySet());
        final OntologyStorageProperties storageProperties = initStorageProperties(physicalUri, null);
        this.connector = new BasicStorageConnector(new Configuration(storageProperties));
        final OntologySnapshot snapshot = connector.getOntologySnapshot();
        final OWLOntology transactionalOntology = snapshot.getOntology();
        final OWLOntologyManager manager = snapshot.getOntologyManager(); // We know this is the root manager
        assertTrue(manager.contains(transactionalOntology));
        connector.closeSnapshot(snapshot);
        assertFalse(manager.contains(transactionalOntology));
    }

    @Test
    public void getSnapshotResolvesImportsOfTheTransactionalSnapshot() throws Exception {
        final URI physicalUri = initOntology(Collections.emptySet());
        final String importedOntoLocation = "http://onto.fel.cvut.cz/ontologies/ufo/current/ontology.xml";
        final IRI importedOntoIri = IRI.create("http://onto.fel.cvut.cz/ontologies/ufo");
        final OWLImportsDeclaration importDecl = manager.getOWLDataFactory()
                                                        .getOWLImportsDeclaration(IRI.create(importedOntoLocation));
        manager.applyChange(new AddImport(ontology, importDecl));
        manager.saveOntology(ontology, IRI.create(physicalUri));
        final OntologyStorageProperties storageProperties = initStorageProperties(physicalUri, null);
        this.connector = new BasicStorageConnector(new Configuration(storageProperties));
        final OntologySnapshot snapshot = connector.getOntologySnapshot();
        final Set<OWLOntology> imports = snapshot.getOntology().getImports();
        assertTrue(imports.size() > 0);
        final Optional<OWLOntology> imported = imports.stream().filter(imp -> imp.getOntologyID().getOntologyIRI().get()
                                                                                 .equals(importedOntoIri)).findAny();
        assertTrue(imported.isPresent());
    }
}
