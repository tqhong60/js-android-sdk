/*
 * Copyright (C) 2015 TIBCO Jaspersoft Corporation. All rights reserved.
 * http://community.jaspersoft.com/project/mobile-sdk-android
 *
 * Unless you have purchased a commercial license agreement from TIBCO Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of TIBCO Jaspersoft Mobile SDK for Android.
 *
 * TIBCO Jaspersoft Mobile SDK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TIBCO Jaspersoft Mobile SDK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with TIBCO Jaspersoft Mobile SDK for Android. If not, see
 * <http://www.gnu.org/licenses/lgpl>.
 */

package com.jaspersoft.android.sdk.service.repository;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.jaspersoft.android.sdk.service.repository.SearchCriteria.ALL;
import static com.jaspersoft.android.sdk.service.repository.SearchCriteria.DASHBOARD;
import static com.jaspersoft.android.sdk.service.repository.SearchCriteria.DEFAULT_LIMIT;
import static com.jaspersoft.android.sdk.service.repository.SearchCriteria.DEFAULT_OFFSET;
import static com.jaspersoft.android.sdk.service.repository.SearchCriteria.LEGACY_DASHBOARD;
import static com.jaspersoft.android.sdk.service.repository.SearchCriteria.REPORT;

/**
 * @author Tom Koptel
 * @since 2.0
 */
final class CriteriaMapper {
    private CriteriaMapper() {
    }

    @NonNull
    public static Map<String, Object> map(InternalCriteria criteria) {
        Map<String, Object> params = new HashMap<>();

        if (criteria.getLimit() != DEFAULT_LIMIT) {
            params.put("limit", String.valueOf(criteria.getLimit()));
        }

        if (criteria.getOffset() != DEFAULT_OFFSET) {
            params.put("offset", String.valueOf(criteria.getOffset()));
        }

        if (criteria.getRecursive() != null) {
            params.put("recursive", String.valueOf(criteria.getRecursive()));
        }

        if (criteria.getForceFullPage() != null) {
            params.put("forceFullPage", String.valueOf(criteria.getForceFullPage()));
        }

        if (criteria.getForceTotalCount() != null) {
            params.put("forceTotalCount", String.valueOf(criteria.getForceTotalCount()));
        }

        String query = criteria.getQuery();
        if (query != null && query.length() > 0) {
            params.put("q", query);
        }

        if (criteria.getSortBy() != null) {
            params.put("sortBy", criteria.getSortBy());
        }

        if (criteria.getFolderUri() != null) {
            params.put("folderUri", criteria.getFolderUri());
        }

        Set<String> types = populateTypes(criteria);
        if (!types.isEmpty()) {

            params.put("type", types);
        }

        return params;
    }

    private static Set<String> populateTypes(InternalCriteria criteria) {
        Set<String> types = new HashSet<>();

        int resourceMask = criteria.getResourceMask();
        boolean includeReport =
                (resourceMask & REPORT) == REPORT || (resourceMask & ALL) == ALL;
        if (includeReport) {
            types.add("reportUnit");
        }
        boolean includeDashboard =
                (resourceMask & DASHBOARD) == DASHBOARD || (resourceMask & ALL) == ALL;
        if (includeDashboard) {
            types.add("dashboard");
        }
        boolean includeLegacyDashboard =
                (resourceMask & LEGACY_DASHBOARD) == LEGACY_DASHBOARD || (resourceMask & ALL) == ALL;
        if (includeLegacyDashboard) {
            types.add("legacyDashboard");
        }

        return types;
    }
}
