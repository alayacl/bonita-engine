/**
 * Copyright (C) 2011-2012 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.data.instance.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.SourceVersion;

import org.bonitasoft.engine.data.instance.exception.SDataInstanceException;
import org.bonitasoft.engine.data.instance.model.SDataInstance;
import org.bonitasoft.engine.data.instance.model.archive.SADataInstance;
import org.bonitasoft.engine.expression.NonEmptyContentExpressionExecutorStrategy;
import org.bonitasoft.engine.expression.exception.SExpressionDependencyMissingException;
import org.bonitasoft.engine.expression.exception.SExpressionEvaluationException;
import org.bonitasoft.engine.expression.exception.SInvalidExpressionException;
import org.bonitasoft.engine.expression.model.ExpressionKind;
import org.bonitasoft.engine.expression.model.SExpression;

/**
 * @author Zhao Na
 */
public class DataExpressionExecutorStrategy extends NonEmptyContentExpressionExecutorStrategy {

    public static final String TIME = "time";

    private final DataInstanceService dataService;

    public DataExpressionExecutorStrategy(final DataInstanceService dataService) {
        this.dataService = dataService;
    }

    @Override
    public Object evaluate(final SExpression expression, final Map<String, Object> map, final Map<Integer, Object> resolvedExpressions)
            throws SExpressionDependencyMissingException, SExpressionEvaluationException {
        return evaluate(Collections.singletonList(expression), map, resolvedExpressions).get(0);
    }

    @Override
    public void validate(final SExpression expression) throws SInvalidExpressionException {
        // $ can be part of variable name
        super.validate(expression);
        if (!SourceVersion.isIdentifier(expression.getContent())) {
            throw new SInvalidExpressionException(expression.getContent() + " is not a valid data name in expression: " + expression);
        }
    }

    @Override
    public ExpressionKind getExpressionKind() {
        return KIND_VARIABLE;
    }

    @Override
    public List<Object> evaluate(final List<SExpression> expressions, final Map<String, Object> dependencyValues, final Map<Integer, Object> resolvedExpressions)
            throws SExpressionDependencyMissingException, SExpressionEvaluationException {
        long containerId;
        String containerType;
        final int maxExpressionSize = expressions.size();
        final ArrayList<String> dataNames = new ArrayList<String>(maxExpressionSize);
        final HashMap<String, Serializable> results = new HashMap<String, Serializable>(maxExpressionSize);
        for (final SExpression sExpression : expressions) {
            final String dataName = sExpression.getContent();
            if (dependencyValues.containsKey(dataName)) {
                final Serializable value = (Serializable) dependencyValues.get(dataName);
                results.put(dataName, value);
            } else {
                dataNames.add(dataName);
            }
        }

        if (dataNames.isEmpty()) {
            return buildExpressionResultSameOrderAsInputList(expressions, results);
        }
        if (dependencyValues != null && dependencyValues.containsKey(CONTAINER_ID_KEY) && dependencyValues.containsKey(CONTAINER_TYPE_KEY)) {
            try {
                containerId = (Long) dependencyValues.get(CONTAINER_ID_KEY);
                containerType = (String) dependencyValues.get(CONTAINER_TYPE_KEY);
                final Long time;
                if ((time = (Long) dependencyValues.get(TIME)) != null) {
                    final List<SADataInstance> dataInstances = dataService.getSADataInstances(containerId, containerType, dataNames, time);
                    for (final SADataInstance dataInstance : dataInstances) {
                        dataNames.remove(dataInstance.getName());
                        results.put(dataInstance.getName(), dataInstance.getValue());
                    }
                }
                final List<SDataInstance> dataInstances = dataService.getDataInstances(dataNames, containerId, containerType);
                for (final SDataInstance dataInstance : dataInstances) {
                    dataNames.remove(dataInstance.getName());
                    results.put(dataInstance.getName(), dataInstance.getValue());
                }
                if (!dataNames.isEmpty()) {
                    throw new SExpressionEvaluationException("some data were not found " + dataNames);
                }
                return buildExpressionResultSameOrderAsInputList(expressions, results);
            } catch (final SDataInstanceException e) {
                throw new SExpressionEvaluationException(e);
            }
        } else {
            throw new SExpressionDependencyMissingException("The context to evaluate the data '" + dataNames + "' was not set");
        }
    }

    private List<Object> buildExpressionResultSameOrderAsInputList(final List<SExpression> expressions, final Map<String, Serializable> results) {
        final ArrayList<Object> list = new ArrayList<Object>(expressions.size());
        for (final SExpression expression : expressions) {
            list.add(results.get(expression.getContent()));
        }
        return list;
    }

    @Override
    public boolean mustPutEvaluatedExpressionInContext() {
        return true;
    }

}
