/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.packageloader.action;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.indices.breaker.CircuitBreakerService;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.tasks.TaskCancelledException;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.tasks.TaskManager;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.common.notifications.Level;
import org.elasticsearch.xpack.core.ml.action.AuditMlNotificationAction;
import org.elasticsearch.xpack.core.ml.inference.trainedmodel.ModelPackageConfig;
import org.elasticsearch.xpack.core.ml.packageloader.action.LoadTrainedModelPackageAction;
import org.hamcrest.CoreMatchers;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.elasticsearch.core.Strings.format;
import static org.elasticsearch.xpack.core.ml.MlTasks.MODEL_IMPORT_TASK_ACTION;
import static org.elasticsearch.xpack.core.ml.MlTasks.MODEL_IMPORT_TASK_TYPE;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransportLoadTrainedModelPackageTests extends ESTestCase {
    private static final String MODEL_IMPORT_FAILURE_MSG_FORMAT = "Model importing failed due to %s [%s]";

    public void testSendsFinishedUploadNotification() {
        var uploader = createUploader(null);
        var taskManager = mock(TaskManager.class);
        var task = mock(ModelDownloadTask.class);
        var client = mock(Client.class);

        TransportLoadTrainedModelPackage.importModel(client, () -> {}, createRequestWithWaiting(), uploader, task, ActionListener.noop());

        var notificationArg = ArgumentCaptor.forClass(AuditMlNotificationAction.Request.class);
        // 2 notifications- the start and finish messages
        verify(client, times(2)).execute(eq(AuditMlNotificationAction.INSTANCE), notificationArg.capture(), any());
        // Only the last message is captured
        assertThat(notificationArg.getValue().getMessage(), CoreMatchers.containsString("finished model import after"));
    }

    public void testSendsErrorNotificationForInternalError() throws Exception {
        ElasticsearchStatusException exception = new ElasticsearchStatusException("exception", RestStatus.INTERNAL_SERVER_ERROR);
        String message = format("Model importing failed due to [%s]", exception.toString());

        assertUploadCallsOnFailure(exception, message, Level.ERROR);
    }

    public void testSendsErrorNotificationForMalformedURL() throws Exception {
        MalformedURLException exception = new MalformedURLException("exception");
        String message = format(MODEL_IMPORT_FAILURE_MSG_FORMAT, "an invalid URL", exception.toString());

        assertUploadCallsOnFailure(exception, message, RestStatus.BAD_REQUEST, Level.ERROR);
    }

    public void testSendsErrorNotificationForURISyntax() throws Exception {
        URISyntaxException exception = mock(URISyntaxException.class);
        String message = format(MODEL_IMPORT_FAILURE_MSG_FORMAT, "an invalid URL syntax", exception.toString());

        assertUploadCallsOnFailure(exception, message, RestStatus.BAD_REQUEST, Level.ERROR);
    }

    public void testSendsErrorNotificationForIOException() throws Exception {
        IOException exception = mock(IOException.class);
        String message = format(MODEL_IMPORT_FAILURE_MSG_FORMAT, "an IOException", exception.toString());

        assertUploadCallsOnFailure(exception, message, RestStatus.SERVICE_UNAVAILABLE, Level.ERROR);
    }

    public void testSendsErrorNotificationForException() throws Exception {
        RuntimeException exception = mock(RuntimeException.class);
        String message = format(MODEL_IMPORT_FAILURE_MSG_FORMAT, "an Exception", exception.toString());

        assertUploadCallsOnFailure(exception, message, RestStatus.INTERNAL_SERVER_ERROR, Level.ERROR);
    }

    public void testSendsWarningNotificationForTaskCancelledException() throws Exception {
        TaskCancelledException exception = new TaskCancelledException("cancelled");
        String message = format("Model importing failed due to [%s]", exception.toString());

        assertUploadCallsOnFailure(exception, message, Level.WARNING);
    }

    public void testCallsOnResponseWithAcknowledgedResponse() throws Exception {
        var client = mock(Client.class);
        var taskManager = mock(TaskManager.class);
        var task = mock(ModelDownloadTask.class);
        ModelImporter uploader = createUploader(null);

        var responseRef = new AtomicReference<AcknowledgedResponse>();
        var listener = ActionListener.wrap(responseRef::set, e -> fail("received an exception: " + e.getMessage()));

        TransportLoadTrainedModelPackage.importModel(client, () -> {}, createRequestWithWaiting(), uploader, task, listener);
        assertThat(responseRef.get(), is(AcknowledgedResponse.TRUE));
    }

    public void testDoesNotCallListenerWhenNotWaitingForCompletion() {
        var uploader = mock(ModelImporter.class);
        var client = mock(Client.class);
        var task = mock(ModelDownloadTask.class);
        TransportLoadTrainedModelPackage.importModel(
            client,
            () -> {},
            createRequestWithoutWaiting(),
            uploader,
            task,
            ActionListener.running(ESTestCase::fail)
        );
    }

    public void testWaitForExistingDownload() {
        var taskManager = mock(TaskManager.class);
        var modelId = "foo";
        var task = new ModelDownloadTask(1L, MODEL_IMPORT_TASK_TYPE, MODEL_IMPORT_TASK_ACTION, modelId, new TaskId("node", 1L), Map.of());
        when(taskManager.getCancellableTasks()).thenReturn(Map.of(1L, task));

        var transportService = mock(TransportService.class);
        when(transportService.getTaskManager()).thenReturn(taskManager);

        var action = new TransportLoadTrainedModelPackage(
            transportService,
            mock(ClusterService.class),
            mock(ThreadPool.class),
            mock(ActionFilters.class),
            mock(Client.class),
            mock(CircuitBreakerService.class)
        );

        assertTrue(action.handleDownloadInProgress(modelId, true, ActionListener.noop()));
        verify(taskManager).registerRemovedTaskListener(any());
        assertThat(action.taskRemovedListenersByModelId.entrySet(), hasSize(1));
        assertThat(action.taskRemovedListenersByModelId.get(modelId), hasSize(1));

        // With wait for completion == false no new removed listener will be added
        assertTrue(action.handleDownloadInProgress(modelId, false, ActionListener.noop()));
        verify(taskManager, times(1)).registerRemovedTaskListener(any());
        assertThat(action.taskRemovedListenersByModelId.entrySet(), hasSize(1));
        assertThat(action.taskRemovedListenersByModelId.get(modelId), hasSize(1));

        assertFalse(action.handleDownloadInProgress("no-task-for-this-one", randomBoolean(), ActionListener.noop()));
    }

    private void assertUploadCallsOnFailure(Exception exception, String message, RestStatus status, Level level) throws Exception {
        var esStatusException = new ElasticsearchStatusException(message, status, exception);

        assertNotificationAndOnFailure(exception, esStatusException, message, level);
    }

    private void assertUploadCallsOnFailure(ElasticsearchException exception, String message, Level level) throws Exception {
        assertNotificationAndOnFailure(exception, exception, message, level);
    }

    private void assertNotificationAndOnFailure(
        Exception thrownException,
        ElasticsearchException onFailureException,
        String message,
        Level level
    ) throws Exception {
        var client = mock(Client.class);
        var taskManager = mock(TaskManager.class);
        var task = mock(ModelDownloadTask.class);
        ModelImporter uploader = createUploader(thrownException);

        var failureRef = new AtomicReference<Exception>();
        var listener = ActionListener.wrap(
            (AcknowledgedResponse response) -> { fail("received a acknowledged response: " + response.toString()); },
            failureRef::set
        );
        TransportLoadTrainedModelPackage.importModel(
            client,
            () -> taskManager.unregister(task),
            createRequestWithWaiting(),
            uploader,
            task,
            listener
        );

        var notificationArg = ArgumentCaptor.forClass(AuditMlNotificationAction.Request.class);
        // 2 notifications- the starting message and the failure
        verify(client, times(2)).execute(eq(AuditMlNotificationAction.INSTANCE), notificationArg.capture(), any());
        var notification = notificationArg.getValue();
        assertThat(notification.getMessage(), is(message)); // the last message is captured
        assertThat(notification.getLevel(), is(level)); // the last message is captured

        var receivedException = (ElasticsearchException) failureRef.get();
        assertThat(receivedException.toString(), is(onFailureException.toString()));
        assertThat(receivedException.status(), is(onFailureException.status()));
        assertThat(receivedException.getCause(), is(onFailureException.getCause()));

        verify(taskManager).unregister(task);
    }

    @SuppressWarnings("unchecked")
    private ModelImporter createUploader(Exception exception) {
        ModelImporter uploader = mock(ModelImporter.class);
        doAnswer(invocation -> {
            ActionListener<AcknowledgedResponse> listener = (ActionListener<AcknowledgedResponse>) invocation.getArguments()[0];
            if (exception != null) {
                listener.onFailure(exception);
            } else {
                listener.onResponse(AcknowledgedResponse.TRUE);
            }
            return null;
        }).when(uploader).doImport(any(ActionListener.class));

        return uploader;
    }

    private LoadTrainedModelPackageAction.Request createRequestWithWaiting() {
        return new LoadTrainedModelPackageAction.Request("id", mock(ModelPackageConfig.class), true);
    }

    private LoadTrainedModelPackageAction.Request createRequestWithoutWaiting() {
        return new LoadTrainedModelPackageAction.Request("id", mock(ModelPackageConfig.class), false);
    }
}
