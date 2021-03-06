/**
 * Copyright (C) 2012 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.engine.process.document;

import static org.bonitasoft.engine.matchers.ListElementMatcher.nameAre;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.CommonAPITest;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.bar.BarResource;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder;
import org.bonitasoft.engine.bpm.bar.InvalidBusinessArchiveFormatException;
import org.bonitasoft.engine.bpm.data.DataInstance;
import org.bonitasoft.engine.bpm.document.ArchivedDocument;
import org.bonitasoft.engine.bpm.document.ArchivedDocumentNotFoundException;
import org.bonitasoft.engine.bpm.document.ArchivedDocumentsSearchDescriptor;
import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.bpm.document.DocumentCriterion;
import org.bonitasoft.engine.bpm.document.DocumentException;
import org.bonitasoft.engine.bpm.document.DocumentNotFoundException;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.bpm.document.DocumentsSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceCriterion;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.InvalidProcessDefinitionException;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException;
import org.bonitasoft.engine.bpm.process.impl.AutomaticTaskDefinitionBuilder;
import org.bonitasoft.engine.bpm.process.impl.DocumentBuilder;
import org.bonitasoft.engine.bpm.process.impl.DocumentDefinitionBuilder;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.bpm.process.impl.UserTaskDefinitionBuilder;
import org.bonitasoft.engine.bpm.supervisor.ProcessSupervisor;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.expression.InvalidExpressionException;
import org.bonitasoft.engine.identity.Group;
import org.bonitasoft.engine.identity.Role;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserMembership;
import org.bonitasoft.engine.operation.LeftOperandBuilder;
import org.bonitasoft.engine.operation.OperationBuilder;
import org.bonitasoft.engine.operation.OperatorType;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.test.APITestUtil;
import org.bonitasoft.engine.test.annotation.Cover;
import org.bonitasoft.engine.test.annotation.Cover.BPMNConcept;
import org.bonitasoft.engine.test.check.CheckNbPendingTaskOf;
import org.bonitasoft.engine.test.wait.WaitForStep;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Nicolas Chabanoles
 * @author Baptiste Mesta
 */
public class DocumentIntegrationTest extends CommonAPITest {

    private static final String PASSWORD = "bpm";

    private static final String USERNAME = "matti";

    private static final String ACTOR = "actor";

    private static int processVersion = 0;

    private User user;

    @Before
    public void beforeTest() throws BonitaException {
        login();
        user = createUser(USERNAME, PASSWORD);
        logout();
        loginWith(USERNAME, PASSWORD);
    }

    @After
    public void afterTest() throws BonitaException {
        deleteUser(user);
        logout();
    }

    @Test
    public void attachADocumentToProcessInstanceTest() throws BonitaException {
        final ProcessInstance pi = ensureAProcessInstanceIsStarted(user);
        Document attachment;
        try {
            final String documentName = "newDocument";
            final Document doc = buildReferenceToExternalDocument();
            attachment = getProcessAPI().attachDocument(pi.getId(), documentName, doc.getContentFileName(), doc.getContentMimeType(), doc.getUrl());
            final Document attachedDoc = getProcessAPI().getDocument(attachment.getId());
            assertIsSameDocument(attachment, attachedDoc);
            assertFalse(attachedDoc.hasContent());
        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }
    }

    @Test
    public void attachADocumentAndItsContentToProcessInstanceTest() throws BonitaException {
        final ProcessInstance pi = ensureAProcessInstanceIsStarted(user);
        Document attachment;
        try {
            final String documentName = "newDocument";
            final Document doc = buildDocument(documentName);
            final byte[] documentContent = generateContent(doc);
            attachment = getProcessAPI().attachDocument(pi.getId(), documentName, doc.getContentFileName(), doc.getContentMimeType(), documentContent);
            final Document attachedDoc = getProcessAPI().getDocument(attachment.getId());
            assertIsSameDocument(attachment, attachedDoc);
            final byte[] attachedContent = getProcessAPI().getDocumentContent(attachedDoc.getContentStorageId());
            assertTrue(Arrays.equals(documentContent, attachedContent));
            assertTrue(attachedDoc.hasContent());
        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }
    }

    private ProcessInstance ensureAProcessInstanceIsStarted(final BusinessArchive businessArchive, final User user) throws BonitaException {
        final ProcessDefinition processDefinition = getProcessAPI().deploy(businessArchive);
        addMappingOfActorsForUser(ACTOR, user.getId(), processDefinition);
        getProcessAPI().enableProcess(processDefinition.getId());
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        assertTrue(processInstance != null);
        return processInstance;
    }

    private ProcessInstance ensureAProcessInstanceIsStarted(final User user) throws BonitaException {
        return ensureAProcessInstanceIsStarted(getNormalBar(), user);
    }

    protected BusinessArchive getNormalBar() throws InvalidProcessDefinitionException, InvalidBusinessArchiveFormatException {
        final DesignProcessDefinition designProcessDefinition = APITestUtil.createProcessDefinitionWithHumanAndAutomaticSteps("My_Process",
                String.valueOf(processVersion++), Arrays.asList("step1", "step2"), Arrays.asList(true, true), ACTOR, false);
        return new BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(designProcessDefinition).done();
    }

    private Document buildDocument(final String documentName) {
        final String now = String.valueOf(System.currentTimeMillis());
        final String fileName = now + ".txt";
        final DocumentBuilder builder = new DocumentBuilder().createNewInstance(documentName, false);
        builder.setFileName(fileName);
        builder.setContentMimeType("plain/text");
        return builder.done();
    }

    private byte[] generateContent(final Document doc) {
        return doc.getName().getBytes();
    }

    private void assertIsSameDocument(final Document attachment, final Document attachedDoc) {
        assertEquals("IDs are not the same!", attachment.getId(), attachedDoc.getId());
        assertEquals("Process instances IDs are not the same!", attachment.getProcessInstanceId(), attachedDoc.getProcessInstanceId());
        assertEquals("Names are not the same!", attachment.getName(), attachedDoc.getName());
        assertEquals("Authors are not the same!", attachment.getAuthor(), attachedDoc.getAuthor());
        assertEquals("Creation dates are not the same!", attachment.getCreationDate(), attachedDoc.getCreationDate());
        assertEquals("Has content flags are not the same!", attachment.hasContent(), attachedDoc.hasContent());
        assertEquals("File names are not the same!", attachment.getContentFileName(), attachedDoc.getContentFileName());
        assertEquals("Mime types are not the same!", attachment.getContentMimeType(), attachedDoc.getContentMimeType());
        assertEquals("Content storage IDs are not the same!", attachment.getContentStorageId(), attachedDoc.getContentStorageId());
        assertEquals("URL are not the same!", attachment.getUrl(), attachedDoc.getUrl());
    }

    private void assertIsSameDocument(final Document attachedDoc, final long processInstanceId, final String name, final long author, final boolean hasContent,
            final String fileName, final String mimeType, final String url) {
        assertEquals("Process instances IDs are not the same!", processInstanceId, attachedDoc.getProcessInstanceId());
        assertEquals("Names are not the same!", name, attachedDoc.getName());
        assertEquals("Authors are not the same!", author, attachedDoc.getAuthor());
        assertEquals("Has content flags are not the same!", hasContent, attachedDoc.hasContent());
        assertEquals("File names are not the same!", fileName, attachedDoc.getContentFileName());
        assertEquals("Mime types are not the same!", mimeType, attachedDoc.getContentMimeType());
        assertEquals("URL are not the same!", url, attachedDoc.getUrl());
    }

    @Test
    public void createProcessWithUrlDocument() throws BonitaException {
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance("createProcessWithUrlDocument", "1.0");
        final String actorName = "doctor";
        designProcessDefinition.addActor(actorName);
        designProcessDefinition.addUserTask("step1", actorName);
        final String docName = "myRtfDocument";
        final String url = "http://intranet.bonitasoft.com/private/docStorage/anyValue";
        final DocumentDefinitionBuilder documentDefinition = designProcessDefinition.addDocumentDefinition(docName);
        documentDefinition.addUrl(url);
        final BusinessArchive businessArchive = new BusinessArchiveBuilder().createNewBusinessArchive()
                .setProcessDefinition(designProcessDefinition.getProcess()).done();
        final ProcessDefinition processDefinition = getProcessAPI().deploy(businessArchive);
        addMappingOfActorsForUser(actorName, user.getId(), processDefinition);
        getProcessAPI().enableProcess(processDefinition.getId());
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        try {
            final SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 10).filter(DocumentsSearchDescriptor.PROCESSINSTANCE_ID, processInstance.getId());
            final SearchResult<Document> docs = getProcessAPI().searchDocuments(sob.done());
            final Document actualDoc = docs.getResult().get(0);
            assertEquals(docName, actualDoc.getName());
            assertEquals(url, actualDoc.getUrl());
            assertTrue("Document Content filename should NOT be valuated", null == actualDoc.getContentFileName());
        } finally {
            disableAndDeleteProcess(processDefinition);
        }
    }

    @Test
    public void attachAnExternalDocumentReferenceToProcessInstanceTest() throws BonitaException {
        final ProcessInstance pi = ensureAProcessInstanceIsStarted(user);
        Document attachment;
        try {
            final Document doc = buildReferenceToExternalDocument();
            final String documentName = "newAttachment";
            attachment = getProcessAPI().attachDocument(pi.getId(), documentName, doc.getContentFileName(), doc.getContentMimeType(), doc.getUrl());
            final Document attachedDoc = getProcessAPI().getDocument(attachment.getId());
            assertIsSameDocument(attachment, attachedDoc);

        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }
    }

    private Document buildReferenceToExternalDocument() {
        final String now = String.valueOf(System.currentTimeMillis());
        final String documentName = now + "-series";
        final DocumentBuilder builder = new DocumentBuilder().createNewInstance(documentName, true);
        builder.setURL("http://tinyurl.com/7n77prz");
        return builder.done();
    }

    @Test
    public void attachAnExternalDocumentReferenceToProcessInstanceAsNewVersionTest() throws BonitaException {
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user);
        Document attachment;
        try {
            final String documentName = getAttachmentDocumentName(pi);
            final Document doc = buildReferenceToExternalDocument();
            attachment = getProcessAPI().attachNewDocumentVersion(pi.getId(), documentName, doc.getContentFileName(), doc.getContentMimeType(), doc.getUrl());
            final Document attachedDoc = getProcessAPI().getDocument(attachment.getId());
            assertIsSameDocument(attachment, attachedDoc);
        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }
    }

    private ProcessInstance ensureAProcessInstanceIsStartedWithDocumentAttached(final User user) throws BonitaException {
        final String documentName = String.valueOf(System.currentTimeMillis());
        final Document doc = buildDocument(documentName);
        return ensureAProcessInstanceIsStartedWithDocumentAttached(user, documentName, doc.getContentFileName());
    }

    private ProcessInstance ensureAProcessInstanceIsStartedWithDocumentAttached(final User user, final String documentName, final String fileName)
            throws BonitaException {
        final ProcessInstance pi = ensureAProcessInstanceIsStarted(user);
        final Document doc = buildDocument(documentName);
        getProcessAPI().attachDocument(pi.getId(), documentName, fileName, doc.getContentMimeType(), documentName.getBytes());
        return pi;
    }

    private String getAttachmentDocumentName(final ProcessInstance pi) throws BonitaException {
        final Document attachment = getAttachmentWithoutItsContent(pi);
        return attachment.getName();
    }

    private Document getAttachmentWithoutItsContent(final ProcessInstance pi) throws BonitaException {
        final List<Document> attachments = getProcessAPI().getLastVersionOfDocuments(pi.getId(), 0, 1, DocumentCriterion.DEFAULT);
        assertTrue("No attachments found!", attachments != null && attachments.size() == 1);
        return attachments.get(0);
    }

    @Test
    public void attachADocumentAndItsContentToProcessInstanceAsNewVersionTest() throws BonitaException {
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user);

        try {
            final String documentName = getAttachmentDocumentName(pi);
            final Document doc = buildDocument(documentName);
            final Document attachment = getProcessAPI().attachNewDocumentVersion(pi.getId(), documentName, doc.getContentFileName(), doc.getContentMimeType(),
                    documentName.getBytes());
            final Document attachedDoc = getProcessAPI().getDocument(attachment.getId());
            assertIsSameDocument(attachment, attachedDoc);

        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }
    }

    @Test
    public void getDocumentContentTest() throws Exception {
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user);
        try {
            final Document attachment = getAttachmentWithoutItsContent(pi);
            final byte[] docContent = getProcessAPI().getDocumentContent(attachment.getContentStorageId());
            assertNotNull(docContent);
            assertTrue(docContent.length > 0);

        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }
    }

    @Test
    public void getLastDocumentTest() throws Exception {
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user);

        Document attachment;
        Document lastVersion;
        try {
            final String documentName = getAttachmentDocumentName(pi);
            attachment = getAttachmentWithoutItsContent(pi);
            lastVersion = getProcessAPI().getLastDocument(pi.getId(), documentName);
            assertIsSameDocument(attachment, lastVersion);

            final Document doc = buildReferenceToExternalDocument();
            final Document newVersion = getProcessAPI().attachNewDocumentVersion(pi.getId(), documentName, doc.getContentFileName(), doc.getContentMimeType(),
                    doc.getUrl());
            lastVersion = getProcessAPI().getLastDocument(pi.getId(), documentName);
            assertIsSameDocument(newVersion, lastVersion);

        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }
    }

    @Test
    public void getDocumentOnProcessWithDocumentInDefinitionUsingBarResource() throws Exception {
        final ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder().createNewInstance("MyProcessWithDocumentsInBar", "1.0");
        builder.addUserTask("step1", ACTOR);
        builder.addActor(ACTOR);
        builder.addDocumentDefinition("myDoc").addContentFileName("myPdfModifiedName.pdf").addDescription("a cool pdf document").addMimeType("application/pdf")
                .addFile("myPdf.pdf");
        final byte[] pdfContent = new byte[] { 5, 0, 1, 4, 6, 5, 2, 3, 1, 5, 6, 8, 4, 6, 6, 3, 2, 4, 5 };
        final BusinessArchive businessArchive = new BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(builder.getProcess())
                .addDocumentResource(new BarResource("myPdf.pdf", pdfContent)).done();
        final ProcessInstance processInstance = ensureAProcessInstanceIsStarted(businessArchive, user);
        try {
            final Document attachment = getAttachmentWithoutItsContent(processInstance);
            assertIsSameDocument(attachment, processInstance.getId(), "myDoc", user.getId(), true, "myPdfModifiedName.pdf", "application/pdf",
                    attachment.getUrl());
            final byte[] docContent = getProcessAPI().getDocumentContent(attachment.getContentStorageId());
            assertTrue(Arrays.equals(pdfContent, docContent));
        } finally {
            waitForUserTask("step1", processInstance);
            disableAndDeleteProcess(processInstance.getProcessDefinitionId());
        }
    }

    @Test
    public void getDocumentUsingExpression() throws Exception {
        final ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder().createNewInstance("MyProcessWithDocumentsInBar", "1.0");
        final AutomaticTaskDefinitionBuilder automaticTask = builder.addAutomaticTask("setDataTask");
        automaticTask.addOperation(new LeftOperandBuilder().createNewInstance("myDocRef").done(), OperatorType.ASSIGNMENT, "=", null,
                new ExpressionBuilder().createDocumentReferenceExpression("myDoc"));
        automaticTask.addOperation(
                new LeftOperandBuilder().createNewInstance("docFileName").done(),
                OperatorType.ASSIGNMENT,
                "=",
                null,
                new ExpressionBuilder().createGroovyScriptExpression("myScript", "myDoc.getFileName()", String.class.getName(),
                        new ExpressionBuilder().createDocumentReferenceExpression("myDoc")));
        builder.addUserTask("step1", ACTOR);
        builder.addActor(ACTOR);
        builder.addData("myDocRef", Document.class.getName(), null);
        builder.addData("docFileName", String.class.getName(), null);
        builder.addDocumentDefinition("myDoc").addContentFileName("myPdfModifiedName.pdf").addDescription("a cool pdf document").addMimeType("application/pdf")
                .addFile("myPdf.pdf");
        builder.addTransition("setDataTask", "step1");
        final byte[] pdfContent = new byte[] { 5, 0, 1, 4, 6, 5, 2, 3, 1, 5, 6, 8, 4, 6, 6, 3, 2, 4, 5 };
        final BusinessArchive businessArchive = new BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(builder.getProcess())
                .addDocumentResource(new BarResource("myPdf.pdf", pdfContent)).done();
        final ProcessInstance pi = ensureAProcessInstanceIsStarted(businessArchive, user);
        try {
            final WaitForStep waitForStep = waitForStep("step1", pi);
            final long step1 = waitForStep.getResult().getId();
            final DataInstance activityDataInstance = getProcessAPI().getActivityDataInstance("myDocRef", step1);
            final Document docRef = (Document) activityDataInstance.getValue();

            final Document attachment = getAttachmentWithoutItsContent(pi);
            assertEquals(attachment, docRef);
            assertEquals("myPdfModifiedName.pdf", getProcessAPI().getActivityDataInstance("docFileName", step1).getValue());
        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }
    }

    @Test
    public void getDocumentOnProcessWithDocumentInDefinitionUsingUrl() throws Exception {
        final String url = "http://plop.org/file.pdf";
        final ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder().createNewInstance("MyProcessWithExternalDocuments", "1.0");
        builder.addUserTask("step1", ACTOR);
        builder.addActor(ACTOR);
        builder.addDocumentDefinition("myDoc").addContentFileName("file.pdf").addDescription("a cool pdf document").addMimeType("application/pdf").addUrl(url);
        final BusinessArchive businessArchive = new BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(builder.getProcess()).done();
        final ProcessInstance processInstance = ensureAProcessInstanceIsStarted(businessArchive, user);
        try {
            final Document attachment = getAttachmentWithoutItsContent(processInstance);
            assertIsSameDocument(attachment, processInstance.getId(), "myDoc", user.getId(), false, "file.pdf", "application/pdf", url);
        } finally {
            // Clean up
            waitForUserTask("step1", processInstance);
            disableAndDeleteProcess(processInstance.getProcessDefinitionId());
        }
    }

    @Test
    public void getDocumentAtProcessInstantiation() throws Exception {
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user);
        try {
            final Document beforeUpdate = getAttachmentWithoutItsContent(pi);
            final Document doc = buildDocument(beforeUpdate.getName());
            getProcessAPI().attachNewDocumentVersion(pi.getId(), beforeUpdate.getName(), doc.getContentFileName(), doc.getContentMimeType(),
                    "contentOfTheDoc".getBytes());
            final Document afterUpdate = getAttachmentWithoutItsContent(pi);
            assertNotSame(beforeUpdate, afterUpdate);
            final Document documentAtProcessInstantiation = getProcessAPI().getDocumentAtProcessInstantiation(pi.getId(), afterUpdate.getName());
            assertEquals(beforeUpdate, documentAtProcessInstantiation);
        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }
    }

    @Test
    public void getDocumentAtActivityInstanceCompletion() throws Exception {
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user);
        try {
            final Document beforeUpdate = getAttachmentWithoutItsContent(pi);
            final Document doc = buildDocument(beforeUpdate.getName());
            getProcessAPI().attachNewDocumentVersion(pi.getId(), beforeUpdate.getName(), doc.getContentFileName(), doc.getContentMimeType(),
                    "contentOfTheDoc".getBytes());
            final Document afterUpdate = getAttachmentWithoutItsContent(pi);
            assertNotSame(beforeUpdate, afterUpdate);
            assertTrue(new CheckNbPendingTaskOf(getProcessAPI(), 50, 500, false, 1, user).waitUntil());
            final List<HumanTaskInstance> pendingHumanTaskInstances = getProcessAPI().getPendingHumanTaskInstances(user.getId(), 0, 1,
                    ActivityInstanceCriterion.DEFAULT);
            final HumanTaskInstance humanTaskInstance = pendingHumanTaskInstances.get(0);
            assignAndExecuteStep(humanTaskInstance, user.getId());

            final Document doc2 = buildDocument(beforeUpdate.getName());
            getProcessAPI().attachNewDocumentVersion(pi.getId(), beforeUpdate.getName(), doc2.getContentFileName(), doc2.getContentMimeType(),
                    "contentOfTheDoc".getBytes());
            final Document afterUpdate2 = getAttachmentWithoutItsContent(pi);
            final Document documentAtActivityInstanciation = getProcessAPI().getDocumentAtActivityInstanceCompletion(humanTaskInstance.getId(),
                    afterUpdate2.getName());
            assertEquals(afterUpdate, documentAtActivityInstanciation);
        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }

    }

    @Test
    public void getNumberOfDocument() throws Exception {
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user);

        try {
            final long initialNbOfDocument = getProcessAPI().getNumberOfDocuments(pi.getId());
            final String documentName = "anotherDocumentReference";
            final Document doc = buildReferenceToExternalDocument();

            getProcessAPI().attachDocument(pi.getId(), documentName, doc.getContentFileName(), doc.getContentMimeType(), doc.getUrl());
            final long currentNbOfDocument = getProcessAPI().getNumberOfDocuments(pi.getId());
            assertEquals("Invalid number of attachments!", initialNbOfDocument + 1, currentNbOfDocument);

        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }
    }

    @Test
    public void getNumberOfDocumentAfterAddingDocumentValue() throws Exception {
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user);

        try {
            final long initialNbOfDocument = getProcessAPI().getNumberOfDocuments(pi.getId());
            final String documentName = "anotherDocumentValue";
            final Document doc = buildDocument(documentName);

            getProcessAPI().attachDocument(pi.getId(), documentName, doc.getContentFileName(), doc.getContentMimeType(), documentName.getBytes());
            final long currentNbOfDocument = getProcessAPI().getNumberOfDocuments(pi.getId());
            assertEquals("Invalid number of attachments!", initialNbOfDocument + 1, currentNbOfDocument);

        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }
    }

    @Test
    public void testSearchDocuments() throws Exception {
        // add a new document, search it.
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user);
        searchOptionsBuilder.filter(DocumentsSearchDescriptor.PROCESSINSTANCE_ID, pi.getId());
        searchOptionsBuilder.sort(DocumentsSearchDescriptor.DOCUMENT_NAME, Order.ASC);
        final SearchResult<Document> documentSearch = getProcessAPI().searchDocuments(searchOptionsBuilder.done());
        assertEquals(1, documentSearch.getCount());
        assertEquals(pi.getId(), documentSearch.getResult().get(0).getProcessInstanceId());
        assertEquals(user.getId(), documentSearch.getResult().get(0).getAuthor());

        disableAndDeleteProcess(pi.getProcessDefinitionId());
    }

    @Cover(classes = { SearchOptionsBuilder.class, ProcessAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "SearchDocuments", "Apostrophe" }, jira = "ENGINE-366, ENGINE-594")
    @Test
    public void testSearchDocumentsWithApostrophe() throws Exception {
        searchDocumentsWithApostrophe("'documentName", "fileName");
        searchDocumentsWithApostrophe("documentName", "'fileName");
    }

    private void searchDocumentsWithApostrophe(final String documentName, final String fileName) throws Exception {
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user, documentName, fileName);

        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        searchOptionsBuilder.sort(DocumentsSearchDescriptor.DOCUMENT_NAME, Order.ASC);
        searchOptionsBuilder.searchTerm("'");
        final SearchResult<Document> documentSearch = getProcessAPI().searchDocuments(searchOptionsBuilder.done());

        assertEquals(1, documentSearch.getCount());
        assertEquals(pi.getId(), documentSearch.getResult().get(0).getProcessInstanceId());
        assertEquals(user.getId(), documentSearch.getResult().get(0).getAuthor());
        disableAndDeleteProcess(pi.getProcessDefinitionId());
    }

    @Test
    @Ignore("Created bug : ENGINE-594")
    public void documentsAreDeletedWhenProcessIsDeleted() throws Exception {
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user, "test", "test.txt");
        disableAndDeleteProcess(pi.getProcessDefinitionId());

        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        searchOptionsBuilder.sort(DocumentsSearchDescriptor.DOCUMENT_NAME, Order.ASC);
        final SearchResult<Document> documentSearch = getProcessAPI().searchDocuments(searchOptionsBuilder.done());

        assertEquals(0, documentSearch.getCount());
    }

    @Test
    public void testSearchDocumentsSupervisedBy() throws Exception {
        // search document by supervisor before create supervisor.
        SearchOptionsBuilder searchOptionsBuilder;
        searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user);
        searchOptionsBuilder.filter(DocumentsSearchDescriptor.PROCESSINSTANCE_ID, pi.getId());
        searchOptionsBuilder.sort(DocumentsSearchDescriptor.DOCUMENT_NAME, Order.ASC);
        SearchResult<Document> documentSearch = getProcessAPI().searchDocumentsSupervisedBy(user.getId(), searchOptionsBuilder.done());
        assertEquals(0, documentSearch.getCount());

        // create supervisor.
        final long processDefinitionId = getProcessAPI().getProcessDefinitionIdFromProcessInstanceId(pi.getId());
        final ProcessSupervisor supervisor = createSupervisor(processDefinitionId, user.getId());

        // search again.
        searchOptionsBuilder.filter(DocumentsSearchDescriptor.PROCESSINSTANCE_ID, pi.getId());
        documentSearch = getProcessAPI().searchDocumentsSupervisedBy(user.getId(), searchOptionsBuilder.done());
        assertEquals(1, documentSearch.getCount());
        assertEquals(pi.getId(), documentSearch.getResult().get(0).getProcessInstanceId());
        assertEquals(user.getId(), documentSearch.getResult().get(0).getAuthor());

        // add supervisor by role and group
        final User supervisor1 = createUser("supervisor", "bpm");
        final Map<String, Object> map = createSupervisorByRoleAndGroup(processDefinitionId, supervisor1.getId());
        final ProcessSupervisor supervisorByRole = (ProcessSupervisor) map.get("supervisorByRole");
        final ProcessSupervisor supervisorByGroup = (ProcessSupervisor) map.get("supervisorByGroup");
        final Role role = (Role) map.get("roleId");
        final Group group = (Group) map.get("groupId");
        final UserMembership membership = (UserMembership) map.get("membership");
        assertEquals(supervisorByRole.getRoleId(), role.getId());
        assertEquals(supervisorByGroup.getGroupId(), group.getId());
        assertEquals(membership.getUserId(), supervisor1.getId());
        assertEquals(membership.getRoleId(), role.getId());
        assertEquals(membership.getGroupId(), group.getId());

        searchOptionsBuilder.filter(DocumentsSearchDescriptor.PROCESSINSTANCE_ID, pi.getId());
        documentSearch = getProcessAPI().searchDocumentsSupervisedBy(user.getId(), searchOptionsBuilder.done());
        assertEquals(1, documentSearch.getCount());
        assertEquals(pi.getId(), documentSearch.getResult().get(0).getProcessInstanceId());
        assertEquals(user.getId(), documentSearch.getResult().get(0).getAuthor());

        deleteSupervisor(supervisor.getSupervisorId());
        disableAndDeleteProcess(pi.getProcessDefinitionId());
        deleteRoleGroupSupervisor(map, supervisor1.getId());
        deleteUser(supervisor1);
    }

    @Test
    public void testSearchArchivedDocuments() throws Exception {
        // first time search, no document in archive table.
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user);
        SearchOptionsBuilder searchOptionsBuilder;
        searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        searchOptionsBuilder.filter(ArchivedDocumentsSearchDescriptor.PROCESSINSTANCE_ID, pi.getId());
        SearchResult<ArchivedDocument> documentSearch = getProcessAPI().searchArchivedDocuments(searchOptionsBuilder.done());
        assertEquals(0, documentSearch.getCount());

        try {
            final Document beforeUpdate = getAttachmentWithoutItsContent(pi);
            final Document doc = buildDocument(beforeUpdate.getName());
            getProcessAPI().attachNewDocumentVersion(pi.getId(), beforeUpdate.getName(), doc.getContentFileName(), doc.getContentMimeType(),
                    "contentOfTheDoc".getBytes());
            final Document afterUpdate = getAttachmentWithoutItsContent(pi);
            getProcessAPI().getDocumentAtProcessInstantiation(pi.getId(), afterUpdate.getName());

            // search again. exist 1 document in archive table.
            searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
            searchOptionsBuilder.filter(ArchivedDocumentsSearchDescriptor.PROCESSINSTANCE_ID, pi.getId());
            searchOptionsBuilder.sort(ArchivedDocumentsSearchDescriptor.DOCUMENT_NAME, Order.ASC);
            documentSearch = getProcessAPI().searchArchivedDocuments(searchOptionsBuilder.done());
            assertEquals(1, documentSearch.getCount());
            assertEquals(pi.getId(), documentSearch.getResult().get(0).getProcessInstanceId());
            assertEquals(user.getId(), documentSearch.getResult().get(0).getDocumentAuthor());

            // search with term:
            searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
            searchOptionsBuilder.searchTerm(afterUpdate.getName());
            documentSearch = getProcessAPI().searchArchivedDocuments(searchOptionsBuilder.done());
            assertEquals(1, documentSearch.getCount());
            assertEquals(afterUpdate.getName(), documentSearch.getResult().get(0).getName());

        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }
    }

    @Cover(classes = { SearchOptionsBuilder.class, ProcessAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "SearchArchivedDocuments", "Apostrophe" }, jira = "ENGINE-366")
    @Test
    public void testSearchArchivedDocumentsWithApostropheInTheDocumentName() throws Exception {
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user, "a'", "a");
        SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        SearchResult<ArchivedDocument> documentSearch = getProcessAPI().searchArchivedDocuments(searchOptionsBuilder.done());
        assertEquals(0, documentSearch.getCount());

        try {
            final Document beforeUpdate = getAttachmentWithoutItsContent(pi);
            final Document doc = buildDocument(beforeUpdate.getName());
            getProcessAPI().attachNewDocumentVersion(pi.getId(), beforeUpdate.getName(), doc.getContentFileName(), doc.getContentMimeType(),
                    "contentOfTheDoc".getBytes());
            final Document afterUpdate = getAttachmentWithoutItsContent(pi);
            getProcessAPI().getDocumentAtProcessInstantiation(pi.getId(), afterUpdate.getName());

            // search with term:
            searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
            searchOptionsBuilder.searchTerm("a'");
            documentSearch = getProcessAPI().searchArchivedDocuments(searchOptionsBuilder.done());
            assertEquals(1, documentSearch.getCount());
            assertEquals(afterUpdate.getName(), documentSearch.getResult().get(0).getName());

        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }
    }

    @Cover(classes = { SearchOptionsBuilder.class, ProcessAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "SearchArchivedDocuments", "Apostrophe" }, jira = "ENGINE-366")
    @Test
    public void testSearchArchivedDocumentsWithApostropheInTheFileName() throws Exception {
        final ProcessInstance pi = ensureAProcessInstanceIsStartedWithDocumentAttached(user, "b", "b'");
        SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        SearchResult<ArchivedDocument> documentSearch = getProcessAPI().searchArchivedDocuments(searchOptionsBuilder.done());
        assertEquals(0, documentSearch.getCount());

        try {
            final Document beforeUpdate = getAttachmentWithoutItsContent(pi);
            final Document doc = buildDocument(beforeUpdate.getName());
            getProcessAPI().attachNewDocumentVersion(pi.getId(), beforeUpdate.getName(), doc.getContentFileName(), doc.getContentMimeType(),
                    "contentOfTheDoc".getBytes());
            final Document afterUpdate = getAttachmentWithoutItsContent(pi);
            getProcessAPI().getDocumentAtProcessInstantiation(pi.getId(), afterUpdate.getName());

            // search with term:
            searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
            searchOptionsBuilder.searchTerm("b'");
            documentSearch = getProcessAPI().searchArchivedDocuments(searchOptionsBuilder.done());
            assertEquals(1, documentSearch.getCount());
            assertEquals(afterUpdate.getName(), documentSearch.getResult().get(0).getName());

        } finally {
            disableAndDeleteProcess(pi.getProcessDefinitionId());
        }
    }

    @Test
    public void testSearchArchivedDocumentsSupervisedBy() throws BonitaException {
        // add into archive, without supervisor.
        final ProcessInstance pi1 = ensureAProcessInstanceIsStartedWithDocumentAttached(user);
        final ProcessInstance pi2 = ensureAProcessInstanceIsStartedWithDocumentAttached(user);
        final Document beforeUpdate = getAttachmentWithoutItsContent(pi2);
        final Document doc = buildDocument(beforeUpdate.getName());
        getProcessAPI().attachNewDocumentVersion(pi2.getId(), beforeUpdate.getName(), doc.getContentFileName(), doc.getContentMimeType(),
                "contentOfTheDoc".getBytes());
        final Document afterUpdate = getAttachmentWithoutItsContent(pi2);
        getProcessAPI().getDocumentAtProcessInstantiation(pi2.getId(), afterUpdate.getName());

        // search archive table with supervisor, result is 0.
        SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        searchOptionsBuilder.filter(ArchivedDocumentsSearchDescriptor.PROCESSINSTANCE_ID, pi2.getId());
        searchOptionsBuilder.sort(ArchivedDocumentsSearchDescriptor.DOCUMENT_NAME, Order.ASC);
        SearchResult<ArchivedDocument> documentSearch = getProcessAPI().searchArchivedDocumentsSupervisedBy(user.getId(), searchOptionsBuilder.done());
        assertEquals(0, documentSearch.getCount());

        // create supervisor, add supervisor by role and group
        final long processDefinitionId = getProcessAPI().getProcessDefinitionIdFromProcessInstanceId(pi2.getId());
        final ProcessSupervisor supervisor = createSupervisor(processDefinitionId, user.getId());
        final Map<String, Object> map = createSupervisorByRoleAndGroup(pi2.getId(), user.getId());

        // search again. exist 1 document in archive table with supervisor.
        searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        searchOptionsBuilder.filter(ArchivedDocumentsSearchDescriptor.PROCESSINSTANCE_ID, pi2.getId());
        searchOptionsBuilder.sort(ArchivedDocumentsSearchDescriptor.DOCUMENT_NAME, Order.ASC);
        documentSearch = getProcessAPI().searchArchivedDocumentsSupervisedBy(user.getId(), searchOptionsBuilder.done());
        assertEquals(1, documentSearch.getCount());
        assertEquals(pi2.getId(), documentSearch.getResult().get(0).getProcessInstanceId());
        assertEquals(user.getId(), documentSearch.getResult().get(0).getDocumentAuthor());

        disableAndDeleteProcess(pi1.getProcessDefinitionId());
        disableAndDeleteProcess(pi2.getProcessDefinitionId());
        deleteSupervisor(supervisor.getSupervisorId());
        deleteRoleGroupSupervisor(map, user.getId());
    }

    @Test
    public void testGetArchivedVersionOfDocuments() throws BonitaException {
        // add new document
        final ProcessInstance pi1 = ensureAProcessInstanceIsStartedWithDocumentAttached(user);
        // search archive document. result is 0.
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        searchOptionsBuilder.filter(ArchivedDocumentsSearchDescriptor.PROCESSINSTANCE_ID, pi1.getId());
        final SearchResult<ArchivedDocument> documentSearch = getProcessAPI().searchArchivedDocuments(searchOptionsBuilder.done());
        assertEquals(0, documentSearch.getCount());

        // archive document
        try {
            final Document beforeUpdate = getAttachmentWithoutItsContent(pi1);
            final Document doc = buildDocument(beforeUpdate.getName());
            getProcessAPI().attachNewDocumentVersion(pi1.getId(), beforeUpdate.getName(), doc.getContentFileName(), doc.getContentMimeType(),
                    "contentOfTheDoc".getBytes());
            getAttachmentWithoutItsContent(pi1);

            // get archived document
            final ArchivedDocument archivedDocument = getProcessAPI().getArchivedVersionOfProcessDocument(beforeUpdate.getId());
            assertNotNull(archivedDocument.getArchiveDate());
            assertEquals(beforeUpdate.getId(), archivedDocument.getSourceObjectId());
            assertEquals(pi1.getId(), archivedDocument.getProcessInstanceId());
        } finally {
            disableAndDeleteProcess(pi1.getProcessDefinitionId());
        }
    }

    @Test(expected = ArchivedDocumentNotFoundException.class)
    public void testGetArchivedDocumentNotFound() throws BonitaException {
        getProcessAPI().getArchivedProcessDocument(123456789l);
    }

    @Test
    public void testGetArchivedDocument() throws BonitaException {
        // add new document
        final ProcessInstance pi1 = ensureAProcessInstanceIsStartedWithDocumentAttached(user);
        // search archive document. result is 0.
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        searchOptionsBuilder.filter(ArchivedDocumentsSearchDescriptor.PROCESSINSTANCE_ID, pi1.getId());
        final SearchResult<ArchivedDocument> documentSearch = getProcessAPI().searchArchivedDocuments(searchOptionsBuilder.done());
        assertEquals(0, documentSearch.getCount());

        // archive document
        try {
            final Document beforeUpdate = getAttachmentWithoutItsContent(pi1);
            final Document doc = buildDocument(beforeUpdate.getName());
            getProcessAPI().attachNewDocumentVersion(pi1.getId(), beforeUpdate.getName(), doc.getContentFileName(), doc.getContentMimeType(),
                    "contentOfTheDoc".getBytes());
            getAttachmentWithoutItsContent(pi1);
            final ArchivedDocument archivedDocument = getProcessAPI().getArchivedVersionOfProcessDocument(beforeUpdate.getId());
            assertEquals(archivedDocument, getProcessAPI().getArchivedProcessDocument(archivedDocument.getId()));
        } finally {
            disableAndDeleteProcess(pi1.getProcessDefinitionId());
        }
    }

    @Test
    public void testCountAttachmentWithSomeAttachments() throws BonitaException {
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        final long initialNbOfDocument = getProcessAPI().countAttachments(searchOptionsBuilder.done());
        final ProcessInstance pi1 = ensureAProcessInstanceIsStartedWithDocumentAttached(user);
        final long numberOfAttachments = getProcessAPI().countAttachments(searchOptionsBuilder.done());
        assertEquals(1 + initialNbOfDocument, numberOfAttachments);
        disableAndDeleteProcess(pi1.getProcessDefinitionId());
    }

    @Cover(classes = DocumentValue.class, concept = BPMNConcept.DOCUMENT, jira = "ENGINE-631", keywords = { "document", "operation", "update" }, story = "update an existing document using operation")
    @Test
    public void updateExistingDocumentWithOperation() throws Exception {

        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance("procWithStringIndexes", "1.0");
        final String actorName = "doctor";
        designProcessDefinition.addActor(actorName).addDescription("The doc'");
        designProcessDefinition.addUserTask("step1", actorName);
        final Expression groovyThatCreateDocumentContent = new ExpressionBuilder().createGroovyScriptExpression("script",
                "return new org.bonitasoft.engine.bpm.document.DocumentValue(\"updated Content\".getBytes(), \"plain/text\", \"updatedContent.txt\");",
                DocumentValue.class.getName());
        designProcessDefinition.addAutomaticTask("step2").addOperation(
                new OperationBuilder().createNewInstance().setRightOperand(groovyThatCreateDocumentContent).setType(OperatorType.DOCUMENT_CREATE_UPDATE)
                        .setLeftOperand("textFile", false).done());
        designProcessDefinition.addUserTask("step3", actorName);
        designProcessDefinition.addTransition("step1", "step2");
        designProcessDefinition.addTransition("step2", "step3");
        designProcessDefinition.addDocumentDefinition("textFile").addContentFileName("myUnmodifiedTextFile.pdf").addDescription("a cool text document")
                .addMimeType("plain/text").addFile("myUnmodifiedTextFile.txt");
        final byte[] textContent = "Unmodified content".getBytes();
        final BusinessArchive businessArchive = new BusinessArchiveBuilder().createNewBusinessArchive()
                .setProcessDefinition(designProcessDefinition.getProcess()).addDocumentResource(new BarResource("myUnmodifiedTextFile.txt", textContent))
                .done();
        final ProcessDefinition processDefinition = deployAndEnableWithActor(businessArchive, actorName, user);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        // before update
        final HumanTaskInstance humanTaskInstance = waitForPendingTasks(user.getId(), 1).get(0);
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        searchOptionsBuilder.filter(DocumentsSearchDescriptor.PROCESSINSTANCE_ID, processInstance.getId());
        final SearchOptions searchOptions = searchOptionsBuilder.done();
        SearchResult<Document> searchDocuments = getProcessAPI().searchDocuments(searchOptions);
        assertEquals(1, searchDocuments.getCount());
        final Document initialDocument = searchDocuments.getResult().iterator().next();
        final byte[] documentContent = getProcessAPI().getDocumentContent(initialDocument.getContentStorageId());
        assertEquals("Unmodified content", new String(documentContent));

        // update
        assignAndExecuteStep(humanTaskInstance, user.getId());
        waitForPendingTasks(user.getId(), 1);

        // after update
        searchDocuments = getProcessAPI().searchDocuments(searchOptions);
        assertEquals(1, searchDocuments.getCount());
        final Document newDocument = searchDocuments.getResult().iterator().next();
        assertEquals("textFile", newDocument.getName());
        assertEquals("updatedContent.txt", newDocument.getContentFileName());
        assertEquals("plain/text", newDocument.getContentMimeType());
        final byte[] newDocumentContent = getProcessAPI().getDocumentContent(newDocument.getContentStorageId());
        assertEquals("updated Content", new String(newDocumentContent));

        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = DocumentValue.class, concept = BPMNConcept.DOCUMENT, jira = "ENGINE-978", keywords = { "document", "operation", "update" }, story = "update an existing document using operation")
    @Test
    public void updateExistingDocumentWithNullOperation() throws Exception {

        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance("procWithStringIndexes", "1.0");
        final String actorName = "doctor";
        designProcessDefinition.addActor(actorName).addDescription("The doc'");
        designProcessDefinition.addUserTask("step1", actorName);
        final Expression groovyThatReturnNull = new ExpressionBuilder().createGroovyScriptExpression("script", "return null;", DocumentValue.class.getName());
        designProcessDefinition.addAutomaticTask("step2").addOperation(
                new OperationBuilder().createNewInstance().setRightOperand(groovyThatReturnNull).setType(OperatorType.DOCUMENT_CREATE_UPDATE)
                        .setLeftOperand("textFile", false).done());
        designProcessDefinition.addUserTask("step3", actorName);
        designProcessDefinition.addTransition("step1", "step2");
        designProcessDefinition.addTransition("step2", "step3");
        designProcessDefinition.addDocumentDefinition("textFile").addContentFileName("myUnmodifiedTextFile.pdf").addDescription("a cool text document")
                .addMimeType("plain/text").addFile("myUnmodifiedTextFile.txt");
        final byte[] textContent = "Unmodified content".getBytes();
        final BusinessArchive businessArchive = new BusinessArchiveBuilder().createNewBusinessArchive()
                .setProcessDefinition(designProcessDefinition.getProcess()).addDocumentResource(new BarResource("myUnmodifiedTextFile.txt", textContent))
                .done();
        final ProcessDefinition processDefinition = deployAndEnableWithActor(businessArchive, actorName, user);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        // before update
        final HumanTaskInstance humanTaskInstance = waitForPendingTasks(user.getId(), 1).get(0);
        final Document initialDocument = getProcessAPI().getLastDocument(processInstance.getId(), "textFile");
        final byte[] documentContent = getProcessAPI().getDocumentContent(initialDocument.getContentStorageId());
        assertEquals("Unmodified content", new String(documentContent));

        // update
        assignAndExecuteStep(humanTaskInstance, user.getId());
        waitForPendingTasks(user.getId(), 1);

        // after update
        assertEquals("textFile", getProcessAPI().getArchivedVersionOfProcessDocument(initialDocument.getId()).getName());
        try {
            getProcessAPI().getLastDocument(processInstance.getId(), "textFile");
            fail();
        } catch (final DocumentNotFoundException e) {
            // ok
        } finally {
            disableAndDeleteProcess(processDefinition);
        }
    }

    @Cover(classes = DocumentValue.class, concept = BPMNConcept.DOCUMENT, jira = "ENGINE-631", keywords = { "document", "operation", "update" }, story = "update an existing document url using operation")
    @Test
    public void updateExistingDocumentUrlWithOperation() throws Exception {

        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance("procWithStringIndexes", "1.0");
        final String actorName = "doctor";
        designProcessDefinition.addActor(actorName).addDescription("The doc'");
        designProcessDefinition.addUserTask("step1", actorName);
        designProcessDefinition.addAutomaticTask("step2").addOperation(
                new OperationBuilder().createNewInstance().setRightOperand(getDocumentValueExpressionWithUrl("http://www.example.com/new_url.txt"))
                        .setType(OperatorType.DOCUMENT_CREATE_UPDATE).setLeftOperand("textFile", false).done());
        designProcessDefinition.addUserTask("step3", actorName);
        designProcessDefinition.addTransition("step1", "step2");
        designProcessDefinition.addTransition("step2", "step3");
        designProcessDefinition.addDocumentDefinition("textFile").addContentFileName("myUnmodifiedTextFile.pdf").addDescription("a cool text document")
                .addMimeType("plain/text").addUrl("http://www.example.com/original_url.txt");
        final BusinessArchive businessArchive = new BusinessArchiveBuilder().createNewBusinessArchive()
                .setProcessDefinition(designProcessDefinition.getProcess()).done();
        final ProcessDefinition processDefinition = deployAndEnableWithActor(businessArchive, actorName, user);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        // before update
        final HumanTaskInstance humanTaskInstance = waitForPendingTasks(user.getId(), 1).get(0);
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        searchOptionsBuilder.filter(DocumentsSearchDescriptor.PROCESSINSTANCE_ID, processInstance.getId());
        final SearchOptions searchOptions = searchOptionsBuilder.done();
        SearchResult<Document> searchDocuments = getProcessAPI().searchDocuments(searchOptions);
        assertEquals(1, searchDocuments.getCount());
        final Document initialDocument = searchDocuments.getResult().iterator().next();
        assertEquals("http://www.example.com/original_url.txt", initialDocument.getUrl());

        // update
        assignAndExecuteStep(humanTaskInstance, user.getId());
        waitForPendingTasks(user.getId(), 1);

        // after update
        searchDocuments = getProcessAPI().searchDocuments(searchOptions);
        assertEquals(1, searchDocuments.getCount());
        final Document newDocument = searchDocuments.getResult().iterator().next();
        assertEquals("http://www.example.com/new_url.txt", newDocument.getUrl());

        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = DocumentValue.class, concept = BPMNConcept.DOCUMENT, jira = "ENGINE-631", keywords = { "document", "operation", "update" }, story = "create a document document using operation")
    @Test
    public void createDocumentWithOperation() throws Exception {
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance("procWithStringIndexes", "1.0");
        final String actorName = "doctor";
        designProcessDefinition.addData("documentValue", DocumentValue.class.getName(), null);
        designProcessDefinition.addActor(actorName).addDescription("The doc'");
        designProcessDefinition.addAutomaticTask("step0").addOperation(
                new OperationBuilder().createSetDataOperation("documentValue", new ExpressionBuilder().createDocumentReferenceExpression("textFile")));
        designProcessDefinition.addUserTask("step1", actorName);
        final Expression groovyThatCreateDocumentContent = new ExpressionBuilder().createGroovyScriptExpression("script",
                "return new org.bonitasoft.engine.bpm.document.DocumentValue(\"updated Content\".getBytes(), \"plain/text\", \"updatedContent.txt\");",
                DocumentValue.class.getName());
        // designProcessDefinition.addAutomaticTask("step2").addOperation(
        // new OperationBuilder().createNewInstance().setRightOperand(groovyThatCreateDocumentContent).setType(OperatorType.DOCUMENT_CREATE_UPDATE)
        // .setLeftOperand("textFile", false).done());
        designProcessDefinition.addAutomaticTask("step2").addOperation(new OperationBuilder().createSetDocument("textFile", groovyThatCreateDocumentContent));
        designProcessDefinition.addUserTask("step3", actorName);
        designProcessDefinition.addTransition("step0", "step1");
        designProcessDefinition.addTransition("step1", "step2");
        designProcessDefinition.addTransition("step2", "step3");
        final ProcessDefinition processDefinition = deployAndEnableWithActor(designProcessDefinition.done(), actorName, user);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        // before update
        final HumanTaskInstance humanTaskInstance = waitForPendingTasks(user.getId(), 1).get(0);
        // document value expression should return null when document don't exists
        assertNull(getProcessAPI().getProcessDataInstance("documentValue", processInstance.getId()).getValue());
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        searchOptionsBuilder.filter(DocumentsSearchDescriptor.PROCESSINSTANCE_ID, processInstance.getId());
        final SearchOptions searchOptions = searchOptionsBuilder.done();
        SearchResult<Document> searchDocuments = getProcessAPI().searchDocuments(searchOptions);
        assertEquals(0, searchDocuments.getCount());

        // update
        assignAndExecuteStep(humanTaskInstance, user.getId());
        waitForPendingTasks(user.getId(), 1);

        // after update
        searchDocuments = getProcessAPI().searchDocuments(searchOptions);
        assertEquals(1, searchDocuments.getCount());
        final Document newDocument = searchDocuments.getResult().iterator().next();
        assertEquals("textFile", newDocument.getName());
        assertEquals("updatedContent.txt", newDocument.getContentFileName());
        assertEquals("plain/text", newDocument.getContentMimeType());
        final byte[] newDocumentContent = getProcessAPI().getDocumentContent(newDocument.getContentStorageId());
        assertEquals("updated Content", new String(newDocumentContent));
        disableAndDeleteProcess(processDefinition);

    }

    @Cover(classes = DocumentValue.class, concept = BPMNConcept.DOCUMENT, jira = "ENGINE-975", keywords = { "document", "operation", "create", "URL" }, story = "create a document using operation and URL")
    @Test
    public void createDocumentWithOperationUsingURL() throws Exception {
        // deploy and instantiate process
        final String url = "http://www.example.com/new_url.txt";
        final ProcessDefinition processDefinition = deployProcessWithURLDocumentCreateOperation("textFile", url);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        // before update
        final HumanTaskInstance humanTaskInstance = waitForPendingTasks(user.getId(), 1).get(0);
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 45);
        searchOptionsBuilder.filter(DocumentsSearchDescriptor.PROCESSINSTANCE_ID, processInstance.getId());
        final SearchOptions searchOptions = searchOptionsBuilder.done();
        SearchResult<Document> searchDocuments = getProcessAPI().searchDocuments(searchOptions);
        assertEquals(0, searchDocuments.getCount());

        // update
        assignAndExecuteStep(humanTaskInstance, user.getId());
        waitForPendingTasks(user.getId(), 1);

        // after update
        searchDocuments = getProcessAPI().searchDocuments(searchOptions);
        assertEquals(1, searchDocuments.getCount());
        final Document newDocument = searchDocuments.getResult().iterator().next();
        assertEquals("textFile", newDocument.getName());
        assertEquals(url, newDocument.getUrl());

        // cleaup
        disableAndDeleteProcess(processDefinition);

    }

    private ProcessDefinition deployProcessWithURLDocumentCreateOperation(final String documentName, final String url) throws BonitaException {
        final String actorName = "doctor";
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance("simpleProcess", "1.0");
        designProcessDefinition.addActor(actorName).addDescription("The doctor");
        designProcessDefinition.addUserTask("step1", actorName);
        designProcessDefinition.addAutomaticTask("step2").addOperation(
                new OperationBuilder().createNewInstance().setRightOperand(getDocumentValueExpressionWithUrl(url)).setType(OperatorType.DOCUMENT_CREATE_UPDATE)
                        .setLeftOperand(documentName, false).done());
        designProcessDefinition.addUserTask("step3", actorName);
        designProcessDefinition.addTransition("step1", "step2");
        designProcessDefinition.addTransition("step2", "step3");
        return deployAndEnableWithActor(designProcessDefinition.done(), actorName, user);
    }

    @Cover(classes = Document.class, concept = BPMNConcept.DOCUMENT, jira = "ENGINE-652", keywords = { "document", "sort" }, story = "get last version of document, sorted")
    @Test
    public void getLastVersionOfDocumentsOfAProcess() throws Exception {

        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance("procWithStringIndexes", "1.0");
        final String actorName = "doctor";
        designProcessDefinition.addActor(actorName).addDescription("The doc'");
        UserTaskDefinitionBuilder userTaskBuilder = designProcessDefinition.addUserTask("step1", actorName);
        userTaskBuilder.addOperation(new OperationBuilder().createNewInstance()
                .setRightOperand(getDocumentValueExpressionWithUrl("http://www.example.com/new_url.txt")).setType(OperatorType.DOCUMENT_CREATE_UPDATE)
                .setLeftOperand("textFile2", false).done());
        userTaskBuilder = designProcessDefinition.addUserTask("step2", actorName);
        userTaskBuilder.addOperation(new OperationBuilder().createNewInstance()
                .setRightOperand(getDocumentValueExpressionWithUrl("http://www.example.com/new_url.txt")).setType(OperatorType.DOCUMENT_CREATE_UPDATE)
                .setLeftOperand("textFile4", false).done());
        designProcessDefinition.addDocumentDefinition("textFile2").addContentFileName("myFile3.pdf").addDescription("a cool text document")
                .addMimeType("application/atom+xml").addUrl("http://www.example.com/original_url5.txt");
        designProcessDefinition.addDocumentDefinition("textFile1").addContentFileName("myFile1.pdf").addDescription("a cool text document")
                .addMimeType("plain/csv").addUrl("http://www.example.com/original_url3.txt");
        designProcessDefinition.addDocumentDefinition("textFile3").addContentFileName("myFile4.pdf").addDescription("a cool text document")
                .addMimeType("plain/text").addUrl("http://www.example.com/original_url2.txt");
        designProcessDefinition.addDocumentDefinition("textFile4").addContentFileName("myFile2.pdf").addDescription("a cool text document")
                .addMimeType("application/pdf").addUrl("http://www.example.com/original_url4.txt");
        designProcessDefinition.addDocumentDefinition("textFile5").addContentFileName("myFile5.pdf").addDescription("a cool text document")
                .addMimeType("plain/xml").addUrl("http://www.example.com/original_url1.txt");
        designProcessDefinition.addUserTask("step3", actorName);
        designProcessDefinition.addTransition("step1", "step2");
        designProcessDefinition.addTransition("step2", "step3");
        final ProcessDefinition processDefinition = deployAndEnableWithActor(designProcessDefinition.done(), actorName, user);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        HumanTaskInstance humanTaskInstance = waitForPendingTasks(user.getId(), 1).get(0);
        check(processInstance, 1, 2, 3, 4, 5, DocumentCriterion.NAME_ASC);
        check(processInstance, 5, 4, 3, 2, 1, DocumentCriterion.NAME_DESC);
        check(processInstance, 1, 4, 2, 3, 5, DocumentCriterion.FILENAME_ASC);
        check(processInstance, 5, 3, 2, 4, 1, DocumentCriterion.FILENAME_DESC);
        check(processInstance, 2, 4, 1, 3, 5, DocumentCriterion.MIMETYPE_ASC);
        check(processInstance, 5, 3, 1, 4, 2, DocumentCriterion.MIMETYPE_DESC);
        check(processInstance, 5, 3, 1, 4, 2, DocumentCriterion.URL_ASC);
        check(processInstance, 2, 4, 1, 3, 5, DocumentCriterion.URL_DESC);

        final User john = createUser("john", "bpm");
        logout();
        loginWith("john", "bpm");
        assignAndExecuteStep(humanTaskInstance, john.getId());
        humanTaskInstance = waitForPendingTasks(user.getId(), 1).get(0);

        // user id of john > matti
        check(processInstance, 1, 3, 4, 5, 2, DocumentCriterion.AUTHOR_ASC);
        check(processInstance, 2, 1, 3, 4, 5, DocumentCriterion.AUTHOR_DESC);

        // Assign and execute step2
        assignAndExecuteStep(humanTaskInstance, john.getId());
        waitForPendingTasks(user.getId(), 1);

        // special check because date can be too close depending on systems
        final List<Document> dateAsc = getProcessAPI().getLastVersionOfDocuments(processInstance.getId(), 0, 10, DocumentCriterion.CREATION_DATE_ASC);
        assertEquals("textFile2", dateAsc.get(3).getName());
        assertEquals("textFile4", dateAsc.get(4).getName());
        final List<Document> dateDesc = getProcessAPI().getLastVersionOfDocuments(processInstance.getId(), 0, 10, DocumentCriterion.CREATION_DATE_DESC);
        assertEquals("textFile2", dateDesc.get(1).getName());
        assertEquals("textFile4", dateDesc.get(0).getName());

        disableAndDeleteProcess(processDefinition);
        deleteUser(john);
    }

    @Cover(classes = Document.class, concept = BPMNConcept.DOCUMENT, jira = "ENGINE-929", keywords = { "document", "name" }, story = "Start a process with a long name (number of characters > 255)")
    @Test(expected = InvalidProcessDefinitionException.class)
    public void testStartProcessWithLongSizeDocumentName() throws Exception {
        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance("processWithDocumentWithLongName", "1.0");
        processBuilder.addActor(ACTOR).addUserTask("step1", ACTOR);

        // Build fileName with 256 characters
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            builder.append("aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeee"); // + 50 characters
        }
        builder.append("12.pdf");
        final String fileName = builder.toString();
        assertEquals(256, fileName.length());

        // Build document
        processBuilder.addDocumentDefinition("doc").addFile("myPdf.pdf").addContentFileName(fileName).addMimeType("application/octet-stream");

        processBuilder.getProcess();
    }

    @Cover(classes = Document.class, concept = BPMNConcept.DOCUMENT, jira = "ENGINE-929", keywords = { "document", "name" }, story = "Start a process with file name with maximum number of characters authorized.")
    @Test
    public void testStartProcessWithMaxSizeDocumentName() throws Exception {
        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance("processWithDocumentWithLongName", "1.0");
        processBuilder.addActor(ACTOR).addUserTask("step1", ACTOR);

        // Build fileName with 255 characters
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            builder.append("aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeee"); // + 50 characters
        }
        builder.append("1.pdf");
        final String fileName = builder.toString();
        assertEquals(255, fileName.length());

        // Build document
        final byte[] pdfContent = "Some document content".getBytes();
        processBuilder.addDocumentDefinition("doc").addFile("myPdf.pdf").addContentFileName(fileName).addMimeType("application/octet-stream");
        final BusinessArchive businessArchive = new BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(processBuilder.getProcess())
                .addDocumentResource(new BarResource("myPdf.pdf", pdfContent)).done();

        final ProcessDefinition processDefinition = getProcessAPI().deploy(businessArchive);
        addMappingOfActorsForUser(ACTOR, user.getId(), processDefinition);
        getProcessAPI().enableProcess(processDefinition.getId());
        getProcessAPI().startProcess(processDefinition.getId());

        disableAndDeleteProcess(processDefinition.getId());
    }

    @Cover(classes = Document.class, concept = BPMNConcept.DOCUMENT, jira = "ENGINE-929", keywords = { "document", "url" }, story = "Start a process with a long url (number of characters > 255)")
    @Test(expected = InvalidProcessDefinitionException.class)
    public void testStartProcessWithLongSizeDocumentURL() throws Exception {
        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance("processWithDocumentWithLongName", "1.0");
        processBuilder.addActor(ACTOR).addUserTask("step1", ACTOR);

        // Build URL with 256 characters
        final StringBuilder builder = new StringBuilder("http://intranet.bonitasoft.com/private/docStorage/");
        for (int i = 0; i < 4; i++) {
            builder.append("aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeee"); // + 50 characters
        }
        builder.append("123456");
        final String url = builder.toString();
        assertEquals(256, url.length());

        // Build document
        final String docName = "myRtfDocument";
        final DocumentDefinitionBuilder documentDefinition = processBuilder.addDocumentDefinition(docName);
        documentDefinition.addUrl(url);

        processBuilder.getProcess();
    }

    @Cover(classes = Document.class, concept = BPMNConcept.DOCUMENT, jira = "ENGINE-929", keywords = { "document", "url" }, story = "Start a process with url with maximum number of characters authorized.")
    @Test
    public void testStartProcessWithMaxSizeDocumentURL() throws Exception {
        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance("processWithDocumentWithLongName", "1.0");
        processBuilder.addActor(ACTOR).addUserTask("step1", ACTOR);

        // Build URL with 255 characters
        final StringBuilder builder = new StringBuilder("http://intranet.bonitasoft.com/private/docStorage/");
        for (int i = 0; i < 4; i++) {
            builder.append("aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeee"); // + 50 characters
        }
        builder.append("12345");
        final String url = builder.toString();
        assertEquals(255, url.length());

        // Build document
        final String docName = "myRtfDocument";
        final DocumentDefinitionBuilder documentDefinition = processBuilder.addDocumentDefinition(docName);
        documentDefinition.addUrl(url);
        final BusinessArchive businessArchive = new BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(processBuilder.getProcess())
                .done();

        final ProcessDefinition processDefinition = getProcessAPI().deploy(businessArchive);
        addMappingOfActorsForUser(ACTOR, user.getId(), processDefinition);
        getProcessAPI().enableProcess(processDefinition.getId());
        getProcessAPI().startProcess(processDefinition.getId());

        disableAndDeleteProcess(processDefinition.getId());

    }

    private Expression getDocumentValueExpressionWithUrl(final String url) throws InvalidExpressionException {
        return new ExpressionBuilder().createGroovyScriptExpression("script", "return new org.bonitasoft.engine.bpm.document.DocumentValue(\"" + url + "\");",
                DocumentValue.class.getName());
    }

    private void check(final ProcessInstance processInstance, final int one, final int two, final int three, final int four, final int five,
            final DocumentCriterion documentCriterion) throws ProcessInstanceNotFoundException, DocumentException {
        final List<Document> lastVersionOfDocuments = getProcessAPI().getLastVersionOfDocuments(processInstance.getId(), 0, 10, documentCriterion);
        assertThat("the order was not respected for " + documentCriterion, lastVersionOfDocuments,
                nameAre("textFile" + one, "textFile" + two, "textFile" + three, "textFile" + four, "textFile" + five));
    }

}
