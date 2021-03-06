package org.bonitasoft.engine.bpm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.core.process.definition.model.SGatewayType;
import org.bonitasoft.engine.core.process.instance.api.GatewayInstanceService;
import org.bonitasoft.engine.core.process.instance.model.SGatewayInstance;
import org.bonitasoft.engine.core.process.instance.model.builder.SGatewayInstanceBuilder;
import org.bonitasoft.engine.transaction.TransactionService;
import org.junit.Test;

/**
 * @author Feng Hui
 * @author Zhao Na
 */
public class GatewayInstanceServiceIntegrationTest extends CommonBPMServicesTest {

    private static BPMServicesBuilder bpmServicesBuilder;

    @Override
    protected GatewayInstanceService gatewayInstanceService() {
        return getServicesBuilder().getGatewayInstanceService();
    }

    protected static TransactionService getTransactionService() {
        return getServicesBuilder().getTransactionService();
    }

    static {
        bpmServicesBuilder = new BPMServicesBuilder();
    }

    @Test
    public void testCreateAndGetGatewayInstance() throws SBonitaException {
        final SGatewayInstance gatewayInstance = bpmServicesBuilder.getBPMInstanceBuilders().getSGatewayInstanceBuilder()
                .createNewInstance("Gateway1", 1, 1, 1, SGatewayType.EXCLUSIVE, 2, 3, 3).setStateId(1).setHitBys("a,b,c").done();

        insertGatewayInstance(gatewayInstance);

        final SGatewayInstance gatewayInstanceRes = getGatewayInstanceFromDB(gatewayInstance.getId());

        checkGateway(gatewayInstance, gatewayInstanceRes, 2, 3);
    }

    private SGatewayInstance getGatewayInstanceFromDB(final Long gatewayId) throws SBonitaException {
        getTransactionService().begin();
        final SGatewayInstance gatewayInstanceRes = gatewayInstanceService().getGatewayInstance(gatewayId);
        getTransactionService().complete();
        return gatewayInstanceRes;
    }

    private void checkGateway(final SGatewayInstance gatewayInstance, final SGatewayInstance gatewayInstanceRes, final long expectedProcessDefinitionId,
            final long expectedProcessInstanceId) {
        assertNotNull(gatewayInstance);
        final SGatewayInstanceBuilder gatewayInstanceBuilder = bpmServicesBuilder.getBPMInstanceBuilders().getSGatewayInstanceBuilder();
        final long actualProcessDefinitionId = gatewayInstanceRes.getLogicalGroup(gatewayInstanceBuilder.getProcessDefinitionIndex());
        final long actualProcessInstanceId = gatewayInstanceRes.getLogicalGroup(gatewayInstanceBuilder.getRootProcessInstanceIndex());
        assertEquals(expectedProcessDefinitionId, actualProcessDefinitionId);
        assertEquals(expectedProcessInstanceId, actualProcessInstanceId);
        assertEquals(gatewayInstance, gatewayInstanceRes);
    }

    private void updateGatewayState(final SGatewayInstance gatewayInstance, final int stateId) throws SBonitaException {
        getTransactionService().begin();
        final SGatewayInstance gatewayInstance2 = gatewayInstanceService().getGatewayInstance(gatewayInstance.getId());
        gatewayInstanceService().setState(gatewayInstance2, stateId);
        getTransactionService().complete();
    }

    private void updateGatewayHitbys(final SGatewayInstance gatewayInstance, final long transitionIndex) throws SBonitaException {
        getTransactionService().begin();
        final SGatewayInstance gatewayInstance2 = gatewayInstanceService().getGatewayInstance(gatewayInstance.getId());
        gatewayInstanceService().hitTransition(gatewayInstance2, transitionIndex);
        getTransactionService().complete();
    }

    @Test
    public void testCheckMergingCondition() throws SBonitaException {
        // it's implement need to be improved
    }

    @Test
    public void testSetState() throws SBonitaException {
        final SGatewayInstance gatewayInstance = bpmServicesBuilder.getBPMInstanceBuilders().getSGatewayInstanceBuilder()
                .createNewInstance("Gateway1", 1, 1, 1, SGatewayType.EXCLUSIVE, 2, 3, 3).setStateId(1).setHitBys("a,b,c").done();

        insertGatewayInstance(gatewayInstance);

        final SGatewayInstance gatewayInstanceRes = getGatewayInstanceFromDB(gatewayInstance.getId());

        checkGateway(gatewayInstance, gatewayInstanceRes, 2, 3);

        updateGatewayState(gatewayInstanceRes, 2);

        final SGatewayInstance gatewayInstanceRes2 = getGatewayInstanceFromDB(gatewayInstance.getId());
        assertNotNull(gatewayInstanceRes2);
        assertEquals(2, gatewayInstanceRes2.getStateId());
    }

    @Test
    public void testHitTransition() throws SBonitaException {
        final SGatewayInstance gatewayInstance = bpmServicesBuilder.getBPMInstanceBuilders().getSGatewayInstanceBuilder()
                .createNewInstance("Gateway1", 1, 1, 1, SGatewayType.EXCLUSIVE, 2, 3, 3).setStateId(1).setHitBys("1,2,3").done();

        insertGatewayInstance(gatewayInstance);

        final SGatewayInstance gatewayInstanceRes = getGatewayInstanceFromDB(gatewayInstance.getId());

        checkGateway(gatewayInstance, gatewayInstanceRes, 2, 3);

        updateGatewayHitbys(gatewayInstanceRes, 4);

        final SGatewayInstance gatewayInstanceRes2 = getGatewayInstanceFromDB(gatewayInstance.getId());
        assertNotNull(gatewayInstanceRes2);
        assertEquals("1,2,3,4", gatewayInstanceRes2.getHitBys());
    }
}
