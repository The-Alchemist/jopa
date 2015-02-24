package cz.cvut.kbss.jopa.oom;

import cz.cvut.kbss.ontodriver_new.descriptors.AxiomValueDescriptor;
import cz.cvut.kbss.ontodriver_new.descriptors.ReferencedListValueDescriptor;
import cz.cvut.kbss.ontodriver_new.descriptors.SimpleListValueDescriptor;
import cz.cvut.kbss.ontodriver_new.model.Assertion;
import cz.cvut.kbss.ontodriver_new.model.Value;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class OOMTestUtils {

	private OOMTestUtils() {
		throw new AssertionError();
	}

	public static AxiomValueDescriptor getAxiomValueDescriptor(
			AxiomValueGatherer builder) throws Exception {
		final Field descriptorField = AxiomValueGatherer.class
				.getDeclaredField("axiomDescriptor");
		descriptorField.setAccessible(true);
		return (AxiomValueDescriptor) descriptorField.get(builder);
	}

	@SuppressWarnings("unchecked")
	public static List<SimpleListValueDescriptor> getSimpleListValueDescriptors(
			AxiomValueGatherer builder) throws Exception {
		final Field descriptorField = AxiomValueGatherer.class
				.getDeclaredField("simpleListDescriptors");
		descriptorField.setAccessible(true);
		return (List<SimpleListValueDescriptor>) descriptorField.get(builder);
	}

	@SuppressWarnings("unchecked")
	public static List<ReferencedListValueDescriptor> getReferencedListValueDescriptors(
			AxiomValueGatherer builder) throws Exception {
		final Field descriptorsField = AxiomValueGatherer.class
				.getDeclaredField("referencedListDescriptors");
		descriptorsField.setAccessible(true);
		return (List<ReferencedListValueDescriptor>) descriptorsField
				.get(builder);
	}

	public static Set<URI> getTypesToAdd(AxiomValueGatherer builder) throws Exception {
		final Field typesToAddField = AxiomValueGatherer.class
				.getDeclaredField("typesToAdd");
		typesToAddField.setAccessible(true);
		return (Set<URI>) typesToAddField.get(builder);
	}

	public static Set<URI> getTypesToRemove(AxiomValueGatherer builder) throws Exception {
		final Field typesToRemoveField = AxiomValueGatherer.class
				.getDeclaredField("typesToRemove");
		typesToRemoveField.setAccessible(true);
		return (Set<URI>) typesToRemoveField.get(builder);
	}

    public static Map<Assertion, Set<Value<?>>> getPropertiesToAdd(AxiomValueGatherer builder) throws Exception {
        final Field propertiesToAddField = AxiomValueGatherer.class.getDeclaredField("propertiesToAdd");
        propertiesToAddField.setAccessible(true);
        return (Map<Assertion, Set<Value<?>>>) propertiesToAddField.get(builder);
    }

    public static Map<Assertion, Set<Value<?>>> getPropertiesToRemove(AxiomValueGatherer builder) throws Exception {
        final Field propertiesToRemoveField = AxiomValueGatherer.class.getDeclaredField("propertiesToRemove");
        propertiesToRemoveField.setAccessible(true);
        return (Map<Assertion, Set<Value<?>>>) propertiesToRemoveField.get(builder);
    }
}