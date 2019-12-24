/*
 * Copyright (c) 2008-2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.addon.cubaaws.s3;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultInt;
import com.haulmont.cuba.core.config.defaults.DefaultString;
import com.haulmont.cuba.core.global.Secret;

/**
 * Configuration parameters interface used by cubaaws add-on.
 */
@Source(type = SourceType.APP)
public interface AmazonS3Config extends Config {

    /**
     * @return Amazon S3 access key.
     */
    @Property("cuba.amazonS3.accessKey")
    String getAccessKey();

    /**
     * @return Amazon S3 secret access key.
     */
    @Secret
    @Property("cuba.amazonS3.secretAccessKey")
    String getSecretAccessKey();

    /**
     * @return Amazon S3 region.
     */
    @Property("cuba.amazonS3.region")
    String getRegionName();

    /**
     * @return Amazon S3 bucket name.
     */
    @Property("cuba.amazonS3.bucket")
    String getBucket();

    /**
     * @return Amazon S3 chunk size.
     */
    @Property("cuba.amazonS3.chunkSize")
    @DefaultInt(8192)
    int getChunkSize();

    /**
     * @return Return custom S3 storage endpoint URL
     */
    @Property("cuba.amazonS3.endpointUrl")
    @DefaultString("")
    String getEndpointUrl();
}