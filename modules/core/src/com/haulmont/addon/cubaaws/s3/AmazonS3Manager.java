/*
 * Copyright (c) 2008-2020 Haulmont.
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

import com.haulmont.cuba.core.app.FileStorageAPI;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component("cuba_AmazonS3ManagerMBean")
public class AmazonS3Manager implements AmazonS3ManagerMBean {
    @Inject
    private FileStorageAPI fileStorageAPI;

    @Override
    public String refreshS3Client() {
        if (fileStorageAPI instanceof AmazonS3FileStorage) {
            ((AmazonS3FileStorage) fileStorageAPI).refreshS3Client();
            return "Refreshed successfully";
        }
        return "Not an Amazon S3 file storage - refresh attempt ignored";
    }
}
