package cz.cvut.kbss.owlpersistence;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cz.cvut.kbss.owlpersistence.owlapi.OWLPersistence;
import cz.cvut.kbss.owlpersistence.EntityManager;
import cz.cvut.kbss.owlpersistence.OWLClass;
import cz.cvut.kbss.owlpersistence.OWLObjectProperty;
import cz.cvut.kbss.owlpersistence.OWLDataProperty;
import cz.cvut.kbss.owlpersistence.RDFSLabel;

public aspect OWLPersistenceAspect {

	private static final Log log = LogFactory
			.getLog(OWLPersistenceAspect.class);

	pointcut getter() : get( @(OWLObjectProperty || OWLDataProperty) * * ) && within(@OWLClass *);

	pointcut setter() : set( @(OWLObjectProperty || OWLDataProperty || RDFSLabel) * * ) && within(@OWLClass *);

	private final EntityManager m = OWLPersistence.getEntityManager();

	after() : setter()  {
		try {
			final Object object = thisJoinPoint.getTarget();
			final Field field = object.getClass().getDeclaredField(
					thisJoinPoint.getSignature().getName());

			field.setAccessible(true);

			if (m.contains(object)) {
				if (log.isDebugEnabled()) {
					log
							.debug("*** Saving " + field.getName() + " of "
									+ object.getClass() + ":" + object.hashCode());
				}
				m.saveReference(object, field);
			}
		} catch (NoSuchFieldException e) {
			log.error(e, e);
		}

	}

	before() : getter()  {
		try {
			final Object object = thisJoinPoint.getTarget();
			final Field field = object.getClass().getDeclaredField(
					thisJoinPoint.getSignature().getName());

			field.setAccessible(true);

			if (m.contains(object)) {
				if (log.isDebugEnabled()) {
					log.debug("*** Fetching " + field.getName() + " of "
							+ object.getClass() + ":" + object.hashCode());
				}
				
				m.loadReference(object, field);
			}
		} catch (NoSuchFieldException e) {
			log.error(e, e);
		}
	}
}