/*
 * Copyright © 2015 TIBCO Software, Inc. All rights reserved.
 * http://community.jaspersoft.com/project/jaspermobile-android
 *
 * Unless you have purchased a commercial license agreement from TIBCO Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of TIBCO Jaspersoft Mobile for Android.
 *
 * TIBCO Jaspersoft Mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TIBCO Jaspersoft Mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with TIBCO Jaspersoft Mobile for Android. If not, see
 * <http://www.gnu.org/licenses/lgpl>.
 */

package com.jaspersoft.android.sdk.network;

import retrofit.Retrofit;

import java.util.concurrent.TimeUnit;

/**
 * @author Tom Koptel
 * @since 2.0
 */
public final class Server {
    private final String mBaseUrl;
    private final long mConnectTimeout;
    private final long mReadTimeout;

    private RetrofitFactory mRetrofitFactory;
    private HttpClientFactory mHttpClientFactory;

    private ServerRestApi mServerRestApi;

    private Server(String baseUrl, long connectTimeout, long readTimeout) {
        mBaseUrl = baseUrl;
        mConnectTimeout = connectTimeout;
        mReadTimeout = readTimeout;
    }

    public ServerRestApi infoApi() {
        if (mServerRestApi == null) {
            Retrofit retrofit = getRetrofitFactory()
                    .newRetrofit()
                    .build();

            mServerRestApi = new ServerRestApiImpl(retrofit);
        }
        return mServerRestApi;
    }

    public Client.Builder makeAuthorizedClient(Credentials credentials) {
        return new Client.Builder(this, credentials);
    }

    public static GenericBuilder newBuilder() {
        return new GenericBuilder();
    }

    public String getBaseUrl() {
        return mBaseUrl;
    }

    public long getConnectTimeout() {
        return mConnectTimeout;
    }

    public long getReadTimeout() {
        return mReadTimeout;
    }

    RetrofitFactory getRetrofitFactory() {
        if (mRetrofitFactory == null) {
            mRetrofitFactory = new RetrofitFactory(this);
        }
        return mRetrofitFactory;
    }

    HttpClientFactory getClientFactory() {
        if (mHttpClientFactory == null) {
            mHttpClientFactory = new HttpClientFactory(this);
        }
        return mHttpClientFactory;
    }

    public static class GenericBuilder {
        public OptionalBuilder withBaseUrl(String baseUrl) {
            return new OptionalBuilder(baseUrl);
        }
    }

    public static class OptionalBuilder {
        private final String mBaseUrl;

        private long connectTimeout = 10000;
        private long readTimeout = 10000;

        private OptionalBuilder(String baseUrl) {
            mBaseUrl = baseUrl;
        }

        public OptionalBuilder withConnectionTimeOut(long timeout, TimeUnit unit) {
            connectTimeout = unit.toMillis(timeout);
            return this;
        }

        public OptionalBuilder withReadTimeout(long timeout, TimeUnit unit) {
            readTimeout = unit.toMillis(timeout);
            return this;
        }

        public Server create() {
            return new Server(mBaseUrl, connectTimeout, readTimeout);
        }
    }
}
