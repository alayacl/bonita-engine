package org.bonitasoft.engine.process;

import java.io.File;
import java.util.Arrays;
import java.util.Random;

import org.bonitasoft.engine.CommonAPITest;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.bar.BarResource;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.test.APITestUtil;
import org.bonitasoft.engine.test.TestStates;
import org.bonitasoft.engine.test.annotation.Cover;
import org.bonitasoft.engine.test.annotation.Cover.BPMNConcept;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Mesta
 * @author Elias Ricken de Medeiros
 * @author Frédéric Bouquet
 * @author Céline Souchet
 */
public class ProcessDeploymentTest extends CommonAPITest {

    private static final String PASSWORD = "bpm";

    private static final String USERNAME = "john";

    @After
    public void afterTest() throws BonitaException {
        logout();
    }

    @Before
    public void beforeTest() throws BonitaException {
        login();
    }

    @Test
    public void deployProcessInDisabledState() throws Exception {
        final DesignProcessDefinition designProcessDefinition = APITestUtil.createProcessDefinitionWithHumanAndAutomaticSteps(Arrays.asList("step1", "step2"),
                Arrays.asList(true, true));

        final User user = createUser(USERNAME, PASSWORD);
        final ProcessDefinition processDefinition = getProcessAPI().deploy(
                new BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(designProcessDefinition).done());
        addUserToFirstActorOfProcess(user.getId(), processDefinition);

        ProcessDeploymentInfo processDeploymentInfo = getProcessAPI().getProcessDeploymentInfo(processDefinition.getId());
        assertEquals(TestStates.getProcessDepInfoResolvedState(), processDeploymentInfo.getConfigurationState());
        getProcessAPI().enableProcess(processDefinition.getId());
        processDeploymentInfo = getProcessAPI().getProcessDeploymentInfo(processDefinition.getId());
        assertEquals(TestStates.getProcessDepInfoEnabledState(), processDeploymentInfo.getActivationState());

        getProcessAPI().disableProcess(processDefinition.getId());
        getProcessAPI().deleteProcess(processDefinition.getId());
        deleteUser(user);
    }

    @Test
    public void deployProcessFromFile() throws Exception {
        final DesignProcessDefinition designProcessDefinition = APITestUtil.createProcessDefinitionWithHumanAndAutomaticSteps(Arrays.asList("step1", "step2"),
                Arrays.asList(true, true));
        final BusinessArchive businessArchive = new BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(designProcessDefinition).done();
        final File tempFile = File.createTempFile("testbar", ".bar");
        tempFile.delete();
        BusinessArchiveFactory.writeBusinessArchiveToFile(businessArchive, tempFile);

        // read from the file
        final BusinessArchive readBusinessArchive = BusinessArchiveFactory.readBusinessArchive(tempFile);

        final User user = createUser(USERNAME, PASSWORD);
        final ProcessDefinition processDefinition = getProcessAPI().deploy(readBusinessArchive);
        addUserToFirstActorOfProcess(user.getId(), processDefinition);

        ProcessDeploymentInfo processDeploymentInfo = getProcessAPI().getProcessDeploymentInfo(processDefinition.getId());
        assertEquals(TestStates.getProcessDepInfoResolvedState(), processDeploymentInfo.getConfigurationState());
        getProcessAPI().enableProcess(processDefinition.getId());
        processDeploymentInfo = getProcessAPI().getProcessDeploymentInfo(processDefinition.getId());
        assertEquals(TestStates.getProcessDepInfoEnabledState(), processDeploymentInfo.getActivationState());

        getProcessAPI().disableProcess(processDefinition.getId());
        getProcessAPI().deleteProcess(processDefinition.getId());
        deleteUser(user);
    }

    @Test
    public void deployProcessWithUTF8Characteres() throws Exception {
        // Create process
        final DesignProcessDefinition designProcessDefinition1 = APITestUtil.createProcessDefinitionWithHumanAndAutomaticSteps(Arrays.asList("step1_1"),
                Arrays.asList(false));
        final ProcessDefinition processDefinition1 = deployAndEnableProcess(designProcessDefinition1);

        assertEquals(APITestUtil.PROCESS_NAME, processDefinition1.getName());
        // delete data for test
        disableAndDeleteProcess(processDefinition1);
    }

    @Test
    public void deployBigProcess() throws Exception {
        // Create process
        final DesignProcessDefinition designProcessDefinition1 = APITestUtil.createProcessDefinitionWithHumanAndAutomaticSteps(Arrays.asList("step1_1"),
                Arrays.asList(false));
        final byte[] bigContent = new byte[1024 * 1024 * 5];
        new Random().nextBytes(bigContent);

        final BusinessArchive businessArchive = new BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(designProcessDefinition1)
                .addClasspathResource(new BarResource("bigRessource", bigContent)).done();
        final ProcessDefinition processDefinition = getProcessAPI().deploy(businessArchive);
        getProcessAPI().enableProcess(processDefinition.getId());
        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = { ProcessAPI.class, ProcessDefinition.class }, concept = BPMNConcept.PROCESS, keywords = { "ProcessDefinition", "deploy" }, jira = "ENGINE-655", exceptions = { AlreadyExistsException.class })
    @Test(expected = AlreadyExistsException.class)
    public void deployProcess2Times() throws Exception {
        final User user = createUser(USERNAME, PASSWORD);
        // First process def with 2 instances:
        final DesignProcessDefinition designProcessDef1 = APITestUtil.createProcessDefinitionWithHumanAndAutomaticSteps(Arrays.asList("initTask1"),
                Arrays.asList(true));
        final ProcessDefinition processDef1 = deployAndEnableWithActor(designProcessDef1, APITestUtil.ACTOR_NAME, user);
        try {
            deployAndEnableWithActor(designProcessDef1, APITestUtil.ACTOR_NAME, user);
        } finally {
            deleteUser(user);
            disableAndDeleteProcess(processDef1);
        }
    }

}
