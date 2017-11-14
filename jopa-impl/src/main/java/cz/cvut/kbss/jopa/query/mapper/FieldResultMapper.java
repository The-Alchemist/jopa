package cz.cvut.kbss.jopa.query.mapper;

import cz.cvut.kbss.jopa.exception.SparqlResultMappingException;
import cz.cvut.kbss.jopa.model.annotations.FieldResult;
import cz.cvut.kbss.jopa.model.metamodel.FieldSpecification;
import cz.cvut.kbss.jopa.sessions.UnitOfWork;
import cz.cvut.kbss.jopa.utils.EntityPropertiesUtils;
import cz.cvut.kbss.ontodriver.ResultSet;
import cz.cvut.kbss.ontodriver.exception.OntoDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

class FieldResultMapper {

    private static final Logger LOG = LoggerFactory.getLogger(FieldResultMapper.class);

    private final String variableName;

    private final FieldSpecification<?, ?> fieldSpec;

    FieldResultMapper(FieldResult fieldResult, FieldSpecification<?, ?> fieldSpec) {
        this.variableName = fieldResult.variable();
        this.fieldSpec = fieldSpec;
    }

    /**
     * Creates mappers for which no explicit {@link FieldResult} configuration exists.
     * <p>
     * Variable name is taken to be the same as the name of the field.
     */
    FieldResultMapper(FieldSpecification<?, ?> fieldSpec) {
        this.fieldSpec = fieldSpec;
        this.variableName = fieldSpec.getName();
    }

    String getVariableName() {
        return variableName;
    }

    FieldSpecification<?, ?> getFieldSpecification() {
        return fieldSpec;
    }

    /**
     * Maps value from the specified result set to the specified target object's field based on the mapping represented
     * by this instance.
     *
     * @param resultSet Result set with value to map
     * @param target    Target object on which the field will be set
     */
    void map(ResultSet resultSet, Object target, UnitOfWork uow) {
        final Optional<Object> value = getVariableValue(resultSet);
        value.ifPresent(val -> {
            verifyValueRange(val);
            EntityPropertiesUtils.setFieldValue(fieldSpec.getJavaField(), target, val);
        });
    }

    Optional<Object> getVariableValue(ResultSet resultSet) {
        try {
            return Optional.ofNullable(resultSet.getObject(variableName));
        } catch (OntoDriverException e) {
            LOG.warn(e.getMessage());
            return Optional.empty();
        }
    }

    private void verifyValueRange(Object value) {
        if (!fieldSpec.getJavaType().isAssignableFrom(value.getClass())) {
            throw new SparqlResultMappingException(
                    String.format("Value %s cannot be assigned to field of type %s.", value, fieldSpec.getJavaType()));
        }
    }
}