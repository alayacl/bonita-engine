/**
 * Copyright (C) 2013 BonitaSoft S.A.
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
package org.bonitasoft.engine.expression.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.commons.ClassReflector;
import org.bonitasoft.engine.commons.ReflectException;
import org.bonitasoft.engine.expression.NonEmptyContentExpressionExecutorStrategy;
import org.bonitasoft.engine.expression.exception.SExpressionDependencyMissingException;
import org.bonitasoft.engine.expression.exception.SExpressionEvaluationException;
import org.bonitasoft.engine.expression.exception.SInvalidExpressionException;
import org.bonitasoft.engine.expression.model.ExpressionKind;
import org.bonitasoft.engine.expression.model.SExpression;

/**
 * @author Elias Ricken de Medeiros
 */
public class JavaMethodCallExpressionExecutorStrategy extends NonEmptyContentExpressionExecutorStrategy {

    @Override
    public Object evaluate(final SExpression expression, final Map<String, Object> dependencyValues, final Map<Integer, Object> resolvedExpressions)
            throws SExpressionEvaluationException, SExpressionDependencyMissingException {
        final SExpression dependency = expression.getDependencies().get(0);
        final Object object = resolvedExpressions.get(dependency.getDiscriminant());
        Object callResult = null;
        try {
            callResult = ClassReflector.invokeGetter(object, expression.getContent());
        } catch (final ReflectException e) {
            throw new SExpressionEvaluationException(e);
        }
        return callResult;
    }

    @Override
    public void validate(final SExpression expression) throws SInvalidExpressionException {
        super.validate(expression);
        if (expression.getDependencies() == null || expression.getDependencies().size() != 1) {
            throw new SInvalidExpressionException("An expression of type " + TYPE_JAVA_METHOD_CALL
                    + " must have exactly one dependency. This dependency represents the object where the method will be called. Expression:" + expression);
        }
    }

    @Override
    public ExpressionKind getExpressionKind() {
        return KIND_JAVA_METHOD_CALL;
    }

    @Override
    public List<Object> evaluate(final List<SExpression> expressions, final Map<String, Object> dependencyValues, final Map<Integer, Object> resolvedExpressions)
            throws SExpressionEvaluationException, SExpressionDependencyMissingException {
        final List<Object> result = new ArrayList<Object>(2);
        for (final SExpression expression : expressions) {
            result.add(evaluate(expression, dependencyValues, resolvedExpressions));
        }
        return result;
    }

    @Override
    public boolean mustPutEvaluatedExpressionInContext() {
        return false;
    }

}
