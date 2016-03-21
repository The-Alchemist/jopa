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
package cz.cvut.kbss.jopa.model;

import cz.cvut.kbss.jopa.exceptions.NoResultException;
import cz.cvut.kbss.jopa.exceptions.NoUniqueResultException;
import cz.cvut.kbss.jopa.exceptions.OWLPersistenceException;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.jopa.model.query.Parameter;
import cz.cvut.kbss.jopa.model.query.TypedQuery;
import cz.cvut.kbss.jopa.query.QueryHolder;
import cz.cvut.kbss.jopa.sessions.ConnectionWrapper;
import cz.cvut.kbss.jopa.sessions.MetamodelProvider;
import cz.cvut.kbss.jopa.sessions.UnitOfWork;
import cz.cvut.kbss.jopa.utils.ErrorUtils;
import cz.cvut.kbss.ontodriver.ResultSet;
import cz.cvut.kbss.ontodriver.Statement;
import cz.cvut.kbss.ontodriver.exception.OntoDriverException;

import java.net.URI;
import java.util.*;

public class TypedQueryImpl<ResultElement> implements TypedQuery<ResultElement> {

    private final QueryHolder query;
    private final Set<URI> contexts;
    private final Class<ResultElement> resultType;
    private final ConnectionWrapper connection;
    private final MetamodelProvider metamodelProvider;

    private UnitOfWork uow;

    private boolean useBackupOntology;
    private int maxResults;

    public TypedQueryImpl(final QueryHolder query, final Class<ResultElement> resultType,
                          final ConnectionWrapper connection,
                          MetamodelProvider metamodelProvider) {
        this.query = Objects.requireNonNull(query, ErrorUtils.constructNPXMessage("query"));
        this.resultType = Objects.requireNonNull(resultType, ErrorUtils.constructNPXMessage("resultType"));
        this.connection = Objects.requireNonNull(connection,
                ErrorUtils.constructNPXMessage("connection"));
        this.metamodelProvider = Objects.requireNonNull(metamodelProvider,
                ErrorUtils.constructNPXMessage("metamodelProvider"));
        this.contexts = new HashSet<>();
        this.maxResults = Integer.MAX_VALUE;
    }

    public void setUnitOfWork(UnitOfWork uow) {
        this.uow = uow;
    }

    @Override
    public List<ResultElement> getResultList() {
        if (maxResults == 0) {
            return Collections.emptyList();
        }
        List<ResultElement> list;
        try {
            list = getResultListImpl(maxResults);
        } catch (OntoDriverException e) {
            throw queryEvaluationException(e);
        }

        return list;
    }

    private OWLPersistenceException queryEvaluationException(OntoDriverException e) {
        final String executedQuery = query.assembleQuery();
        return new OWLPersistenceException("Exception caught when evaluating query " + executedQuery, e);
    }

    @Override
    public ResultElement getSingleResult() {
        try {
            // call it with maxResults = 2 just to see whether there are
            // multiple results
            final List<ResultElement> res = getResultListImpl(2);
            if (res.isEmpty()) {
                throw new NoResultException("No result found for query " + query);
            }
            if (res.size() > 1) {
                throw new NoUniqueResultException("Multiple results found for query " + query);
            }
            return res.get(0);
        } catch (OntoDriverException e) {
            throw queryEvaluationException(e);
        }
    }

    @Override
    public TypedQuery<ResultElement> setMaxResults(int maxResults) {
        if (maxResults < 0) {
            throw new IllegalArgumentException(
                    "Cannot set maximum number of results to less than 0.");
        }
        this.maxResults = maxResults;
        return this;
    }

    @Override
    public int getMaxResults() {
        return maxResults;
    }

    @Override
    public Parameter<?> getParameter(int position) {
        return query.getParameter(position);
    }

    @Override
    public Parameter<?> getParameter(String name) {
        return query.getParameter(name);
    }

    @Override
    public Set<Parameter<?>> getParameters() {
        return query.getParameters();
    }

    @Override
    public boolean isBound(Parameter<?> parameter) {
        return query.getParameterValue(parameter) != null;
    }

    @Override
    public Object getParameterValue(int position) {
        final Parameter<?> param = query.getParameter(position);
        return getParameterValue(param);
    }

    @Override
    public Object getParameterValue(String name) {
        final Parameter<?> param = query.getParameter(name);
        return getParameterValue(param);
    }

    private IllegalStateException unboundParam(Object param) {
        return new IllegalStateException("Parameter " + param + " is not bound.");
    }

    @Override
    public <T> T getParameterValue(Parameter<T> parameter) {
        if (!isBound(parameter)) {
            throw unboundParam(parameter);
        }
        return (T) query.getParameterValue(parameter);
    }

    @Override
    public TypedQuery<ResultElement> setParameter(int position, Object value) {
        query.setParameter(query.getParameter(position), value);
        return this;
    }

    @Override
    public TypedQuery<ResultElement> setParameter(int position, String value, String language) {
        query.setParameter(query.getParameter(position), value, language);
        return this;
    }

    @Override
    public TypedQuery<ResultElement> setParameter(String name, Object value) {
        query.setParameter(query.getParameter(name), value);
        return this;
    }

    @Override
    public TypedQuery<ResultElement> setParameter(String name, String value, String language) {
        query.setParameter(query.getParameter(name), value, language);
        return this;
    }

    @Override
    public <T> TypedQuery<ResultElement> setParameter(Parameter<T> parameter, T value) {
        query.setParameter(parameter, value);
        return this;
    }

    @Override
    public TypedQuery<ResultElement> setParameter(Parameter<String> parameter, String value, String language) {
        query.setParameter(parameter, value, language);
        return this;
    }

    private List<ResultElement> getResultListImpl(int maxResults) throws OntoDriverException {
        assert maxResults > 0;
        final Statement stmt = connection.createStatement();
        if (useBackupOntology) {
            stmt.useOntology(Statement.StatementOntology.CENTRAL);
        } else {
            stmt.useOntology(Statement.StatementOntology.TRANSACTIONAL);
        }
        URI[] arr = new URI[contexts.size()];
        arr = contexts.toArray(arr);
        try (ResultSet rs = stmt.executeQuery(query.assembleQuery(), arr)) {
            final List<ResultElement> res = new ArrayList<>();
            // TODO register this as observer on the result set so that additional results can be loaded asynchronously
            int cnt = 0;
            final URI ctx = arr.length > 0 ? arr[0] : null;
            final boolean isTypeManaged = metamodelProvider.isTypeManaged(resultType);
            while (rs.hasNext() && cnt < maxResults) {
                rs.next();
                if (isTypeManaged) {
                    res.add(loadEntityInstance(rs, ctx));
                } else {
                    res.add(loadResultValue(rs));
                }
                cnt++;
            }
            return res;
        }
    }

    private ResultElement loadEntityInstance(ResultSet resultSet, URI context) throws OntoDriverException {
        if (uow == null) {
            throw new IllegalStateException("Cannot load entity instance without Unit of Work.");
        }
        final URI uri = URI.create(resultSet.getString(0));
        // TODO Setting the context like this won't work for queries over multiple contexts
        final EntityDescriptor descriptor = new EntityDescriptor(context);

        final ResultElement entity = uow.readObject(resultType, uri, descriptor);
        if (entity == null) {
            throw new OWLPersistenceException(
                    "Fatal error, unable to load entity for primary key already found by query "
                            + query);
        }
        return entity;
    }

    private ResultElement loadResultValue(ResultSet resultSet) throws OntoDriverException {
        try {
            return resultSet.getObject(0, resultType);
        } catch (OntoDriverException e) {
            throw new OWLPersistenceException("Unable to map the query result to class " + resultType, e);
        }
    }

    @Override
    public TypedQuery<ResultElement> addContext(URI context) {
        Objects.requireNonNull(context, ErrorUtils.constructNPXMessage("context"));
        contexts.add(context);
        return this;
    }

    @Override
    public TypedQuery<ResultElement> addContexts(Collection<URI> contexts) {
        Objects.requireNonNull(contexts, ErrorUtils.constructNPXMessage("contexts"));
        this.contexts.addAll(contexts);
        return this;
    }

    @Override
    public TypedQuery<ResultElement> clearContexts() {
        contexts.clear();
        return this;
    }

    /**
     * Sets ontology used for processing of this query. </p>
     *
     * @param useBackupOntology If true, the backup (central) ontology is used, otherwise the transactional ontology is
     *                          used (default)
     */
    public void setUseBackupOntology(boolean useBackupOntology) {
        this.useBackupOntology = useBackupOntology;
    }
}