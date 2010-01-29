package cz.cvut.kbss.owlpersistence.model;

import java.util.Map;

import cz.cvut.kbss.owlpersistence.model.metamodel.Metamodel;

public interface EntityManagerFactory {
	/**
	 * Create a new application-managed EntityManager. This method returns a new
	 * EntityManager instance each time it is invoked. The isOpen method will
	 * return true on the returned instance.
	 * 
	 * @return entity manager instance
	 * @throws IllegalStateException
	 *             if the entity manager factory has been closed
	 */
	public EntityManager createEntityManager();

	/**
	 * Create a new EntityManager with the specified Map of properties. This
	 * method returns a new EntityManager instance each time it is invoked. The
	 * isOpen method will return true on the returned instance.
	 */
	public EntityManager createEntityManager(Map<String, String> map);

	// TODO JPA 2.0 getCriteriaBuilder

	/**
	 * Return an instance of Metamodel interface for access to the metamodel of
	 * the persistence unit.
	 * 
	 * @return Metamodel instance
	 * @throws IllegalStateException
	 *             if the entity manager factory has been closed
	 */
	public Metamodel getMetamodel();

	/**
	 * Close the factory, releasing any resources that it holds. After a factory
	 * instance is closed, all methods invoked on it will throw an
	 * IllegalStateException, except for isOpen, which will return false. Once
	 * an EntityManagerFactory has been closed, all its entity managers are
	 * considered to be in the closed state.
	 */
	public void close();

	/**
	 * Indicates whether the factory is open. Returns true until the factory has
	 * been closed.
	 * 
	 * @return true if the entity manager is open
	 * @throws IllegalStateException
	 *             if the entity manager factory has been closed
	 */
	public boolean isOpen();

	public Map<String, String> getProperties();

	// TODO JPA 2.0 getCache

	public PersistenceUnitUtil getPersistenceUnitUtil();
}