package cz.cvut.kbss.jopa.test.utils;

import java.io.File;
import java.net.URI;

import cz.cvut.kbss.jopa.test.TestEnvironment;
import cz.cvut.kbss.ontodriver.OntologyConnectorType;
import cz.cvut.kbss.ontodriver.OntologyStorageProperties;

/**
 * Persistent storage configuration for Jena accessed single-file storage.
 * 
 * @author ledvima1
 * 
 */
public class JenaStorageConfig extends StorageConfig {

	protected static final OntologyConnectorType TYPE = OntologyConnectorType.JENA;

	public JenaStorageConfig() {
		super();
	}

	@Override
	public OntologyStorageProperties createStorageProperties(int index) {
		assert index >= 0;
		assert name != null;
		assert directory != null;

		String base = name + TYPE.toString() + index;
		final URI ontoUri = URI.create(TestEnvironment.IRI_BASE + base);
		final File url = new File(directory + File.separator + base + ".owl");
		TestEnvironment.removeOldTestFiles(url);
		final URI physicalUri = url.toURI();

		return new OntologyStorageProperties(ontoUri, physicalUri, TYPE);
	}

}
