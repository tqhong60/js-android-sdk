/*
 * Copyright � 2015 TIBCO Software, Inc. All rights reserved.
 * http://community.jaspersoft.com/project/jaspermobile-android
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of Jaspersoft Mobile for Android.
 *
 * Jaspersoft Mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jaspersoft Mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Jaspersoft Mobile for Android. If not, see
 * <http://www.gnu.org/licenses/lgpl>.
 */
package com.jaspersoft.android.sdk.service.report;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.jaspersoft.android.sdk.network.api.ReportExecutionRestApi;
import com.jaspersoft.android.sdk.network.api.ReportExportRestApi;
import com.jaspersoft.android.sdk.network.entity.execution.AttachmentDescriptor;
import com.jaspersoft.android.sdk.network.entity.execution.ExecutionRequestOptions;
import com.jaspersoft.android.sdk.network.entity.execution.ExecutionStatus;
import com.jaspersoft.android.sdk.network.entity.execution.ExportDescriptor;
import com.jaspersoft.android.sdk.network.entity.execution.ReportExecutionDescriptor;
import com.jaspersoft.android.sdk.network.entity.export.ExportExecutionDescriptor;
import com.jaspersoft.android.sdk.service.auth.TokenProvider;
import com.jaspersoft.android.sdk.service.data.report.ReportMetadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * @author Tom Koptel
 * @since 2.0
 */
public final class ReportExecution {
    private final long mDelay;
    private final ReportExecutionRestApi mExecutionApi;
    private final ReportExportRestApi mExportApi;
    private final TokenProvider mTokenProvider;
    private final ReportExecutionDescriptor mState;
    private final ExecutionOptionsDataMapper mExecutionOptionsMapper;

    public ReportExecution(long delay,
                           ReportExecutionRestApi executionApi,
                           ReportExportRestApi exportApi,
                           TokenProvider tokenProvider,
                           ExecutionOptionsDataMapper executionOptionsMapper,
                           ReportExecutionDescriptor state) {
        mDelay = delay;
        mExecutionApi = executionApi;
        mExportApi = exportApi;
        mTokenProvider = tokenProvider;
        mExecutionOptionsMapper = executionOptionsMapper;
        mState = state;
    }


    @NonNull
    public ReportMetadata waitForReportCompletion() {
        try {
            return performAwaitFoReport();
        } catch (ExecutionException ex) {
            throw ex.adaptToClientException();
        }
    }

    @NonNull
    public ReportExport export(RunExportCriteria criteria) {
        try {
            return performExport(criteria);
        } catch (ExecutionException ex) {
            if (ex.isCancelled()) {
                /**
                 * Cancelled by technical reason. User applied Jive(for e.g. have applied new filter).
                 * Cancelled when report execution finished. This event flags that we need rerun export.
                 */
                try {
                    return performExport(criteria);
                } catch (ExecutionException nestedEx) {
                    throw nestedEx.adaptToClientException();
                }
            }
            throw ex.adaptToClientException();
        }
    }

    @NonNull
    private ReportExport performExport(RunExportCriteria criteria) {
        ExportExecutionDescriptor exportDetails = runExport(criteria);
        waitForExportReadyStatus(exportDetails);
        ReportExecutionDescriptor currentDetails = requestExecutionDetails();

        return createExport(currentDetails, exportDetails);
    }

    @NonNull
    private ReportExecutionDescriptor requestExecutionDetails() {
        return mExecutionApi.requestReportExecutionDetails(mTokenProvider.provideToken(), mState.getExecutionId());
    }

    private void waitForExportReadyStatus(ExportExecutionDescriptor exportDetails) {
        final String exportId = exportDetails.getExportId();
        final String executionId = mState.getExecutionId();
        final String reportUri = mState.getReportURI();

        Status status = Status.wrap(exportDetails.getStatus());
        while (!status.isReady()) {
            if (status.isCancelled()) {
                throw ExecutionException.exportCancelled(reportUri);
            }
            if (status.isFailed()) {
                throw ExecutionException.exportFailed(reportUri);
            }
            try {
                Thread.sleep(mDelay);
            } catch (InterruptedException ex) {
                throw ExecutionException.exportFailed(reportUri, ex);
            }
            ExecutionStatus exportStatus = mExportApi
                    .checkExportExecutionStatus(mTokenProvider.provideToken(), executionId, exportId);

            status = Status.wrap(exportStatus.getStatus());
        }
    }

    @NonNull
    private ReportExport createExport(ReportExecutionDescriptor currentDetails,
                                      ExportExecutionDescriptor exportDetails) {
        ExportDescriptor export = findExportDescriptor(currentDetails, exportDetails.getExportId());
        if (export == null) {
            throw ExecutionException.exportFailed(mState.getReportURI());
        }

        String executionId = currentDetails.getExecutionId();
        String exportId = exportDetails.getExportId();
        Collection<ReportAttachment> attachments = adaptAttachments(export);
        return new ReportExport(executionId, exportId, attachments, mTokenProvider, mExportApi);
    }

    private Collection<ReportAttachment> adaptAttachments(ExportDescriptor export) {
        String executionId = mState.getExecutionId();
        String exportId = export.getId();
        Set<AttachmentDescriptor> rawAttachments = export.getAttachments();
        Collection<ReportAttachment> attachments = new ArrayList<>(rawAttachments.size());
        for (AttachmentDescriptor attachment : rawAttachments) {
            ReportAttachment reportAttachment = new ReportAttachment(
                    attachment.getFileName(), executionId, exportId, mTokenProvider, mExportApi);
            attachments.add(reportAttachment);
        }
        return attachments;
    }

    @Nullable
    private ExportDescriptor findExportDescriptor(ReportExecutionDescriptor currentDetails, String exportId) {
        for (ExportDescriptor export : currentDetails.getExports()) {
            if (exportId.equals(export.getId())) {
                return export;
            }
        }
        return null;
    }

    @NonNull
    private ExportExecutionDescriptor runExport(RunExportCriteria criteria) {
        ExecutionRequestOptions options = mExecutionOptionsMapper.transformExportOptions(criteria);
        return mExportApi.runExportExecution(mTokenProvider.provideToken(), mState.getExecutionId(), options);
    }

    @NonNull
    private ReportMetadata performAwaitFoReport() {
        ReportExecutionDescriptor details = requestExecutionDetails();
        ReportExecutionDescriptor completeDetails = waitForReportReadyStart(details);
        return new ReportMetadata(completeDetails.getReportURI(),
                completeDetails.getTotalPages());
    }

    @NonNull
    private ReportExecutionDescriptor waitForReportReadyStart(final ReportExecutionDescriptor details) {
        String reportUri = details.getReportURI();
        Status status = Status.wrap(details.getStatus());

        ReportExecutionDescriptor resultDetails = details;
        while (!status.isReady()) {
            if (status.isCancelled()) {
                throw ExecutionException.reportCancelled(reportUri);
            }
            if (status.isFailed()) {
                throw ExecutionException.reportFailed(reportUri);
            }
            try {
                Thread.sleep(mDelay);
            } catch (InterruptedException ex) {
                throw ExecutionException.reportFailed(reportUri, ex);
            }
            resultDetails = requestExecutionDetails();
            status = Status.wrap(details.getStatus());
        }
        return resultDetails;
    }
}
