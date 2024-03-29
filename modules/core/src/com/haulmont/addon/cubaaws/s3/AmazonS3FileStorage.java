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


import com.google.common.base.Strings;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.core.app.FileStorageAPI;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.sys.events.AppContextStartedEvent;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;

public class AmazonS3FileStorage implements FileStorageAPI {

    private static final Logger log = LoggerFactory.getLogger(AmazonS3FileStorage.class);

    @Inject
    protected AmazonS3Config amazonS3Config;

    protected AtomicReference<S3Client> s3ClientReference = new AtomicReference<>();

    @EventListener
    protected void initS3Client(AppContextStartedEvent event) {
        refreshS3Client();
    }

    protected AwsCredentialsProvider getAwsCredentialsProvider() {
        if (getAccessKey() != null && getSecretAccessKey() != null) {
            AwsCredentials awsCredentials = AwsBasicCredentials.create(getAccessKey(), getSecretAccessKey());
            return StaticCredentialsProvider.create(awsCredentials);
        } else {
            return DefaultCredentialsProvider.builder().build();
        }
    }

    public void refreshS3Client() {
        AwsCredentialsProvider awsCredentialsProvider = getAwsCredentialsProvider();
        if (Strings.isNullOrEmpty(amazonS3Config.getEndpointUrl())) {
            s3ClientReference.set(S3Client.builder()
                    .credentialsProvider(awsCredentialsProvider)
                    .region(Region.of(getRegionName()))
                    .build());
        } else {
            s3ClientReference.set(S3Client.builder()
                    .credentialsProvider(awsCredentialsProvider)
                    .endpointOverride(URI.create(amazonS3Config.getEndpointUrl()))
                    .region(Region.of(getRegionName()))
                    .build());
        }
    }

    @Override
    public long saveStream(FileDescriptor fileDescr, InputStream inputStream) throws FileStorageException {
        Preconditions.checkNotNullArgument(fileDescr.getSize());
        try {
            saveFile(fileDescr, IOUtils.toByteArray(inputStream));
        } catch (IOException e) {
            String message = String.format("Could not save file %s.",
                    getFileName(fileDescr));
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message, e);
        }
        return fileDescr.getSize();
    }

    @Override
    public void saveFile(FileDescriptor fileDescr, byte[] data) throws FileStorageException {
        checkNotNullArgument(data, "File content is null");
        try {
            S3Client s3Client = s3ClientReference.get();
            int chunkSize = amazonS3Config.getChunkSize() * 1024;

            if (data.length == 0) {
                s3Client.putObject(PutObjectRequest.builder()
                        .bucket(getBucket())
                        .key(resolveFileName(fileDescr))
                        .build(), RequestBody.fromBytes(data));
                return;
            }

            CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                    .bucket(getBucket()).key(resolveFileName(fileDescr))
                    .build();
            CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createMultipartUploadRequest);

            List<CompletedPart> completedParts = new ArrayList<>();
            for (int i = 0; i * chunkSize < data.length; i++) {
                int partNumber = i + 1;
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(getBucket())
                        .key(resolveFileName(fileDescr))
                        .uploadId(response.uploadId())
                        .partNumber(partNumber)
                        .build();
                int endChunkPosition = Math.min(partNumber * chunkSize, data.length);
                byte[] chunkBytes = getChunkBytes(data, i * chunkSize, endChunkPosition);
                String eTag = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(chunkBytes)).eTag();
                CompletedPart part = CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(eTag)
                        .build();
                completedParts.add(part);
            }

            CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder().parts(completedParts).build();
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    CompleteMultipartUploadRequest.builder()
                            .bucket(getBucket())
                            .key(resolveFileName(fileDescr))
                            .uploadId(response.uploadId())
                            .multipartUpload(completedMultipartUpload).build();
            s3Client.completeMultipartUpload(completeMultipartUploadRequest);
        } catch (SdkException e) {
            log.error("Error saving file to S3 storage", e);
            String message = String.format("Could not save file %s.", getFileName(fileDescr));
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    protected byte[] getChunkBytes(byte[] data, int start, int end) {
        byte[] chunkBytes = new byte[end - start];
        System.arraycopy(data, start, chunkBytes, 0, end - start);
        return chunkBytes;
    }

    @Override
    public void removeFile(FileDescriptor fileDescr) throws FileStorageException {
        try {
            S3Client s3Client = s3ClientReference.get();
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(getBucket())
                    .key(resolveFileName(fileDescr))
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (SdkException e) {
            log.error("Error removing file from S3 storage", e);
            String message = String.format("Could not delete file %s.", getFileName(fileDescr));
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    @Override
    public InputStream openStream(FileDescriptor fileDescr) throws FileStorageException {
        InputStream is;
        try {
            S3Client s3Client = s3ClientReference.get();
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(getBucket())
                    .key(resolveFileName(fileDescr))
                    .build();
            is = s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream());
        } catch (SdkException e) {
            log.error("Error loading file from S3 storage", e);
            String message = String.format("Could not load file %s.", getFileName(fileDescr));
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
        return is;
    }

    @Override
    public byte[] loadFile(FileDescriptor fileDescr) throws FileStorageException {
        try (InputStream inputStream = openStream(fileDescr)) {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, fileDescr.getId().toString(), e);
        }
    }

    @Override
    public boolean fileExists(FileDescriptor fileDescr) {
        S3Client s3Client = s3ClientReference.get();
        ListObjectsV2Request listObjectsReqManual = ListObjectsV2Request.builder()
                .bucket(getBucket())
                .prefix(resolveFileName(fileDescr))
                .maxKeys(1)
                .build();
        ListObjectsV2Response listObjResponse = s3Client.listObjectsV2(listObjectsReqManual);
        return listObjResponse.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList())
                .contains(resolveFileName(fileDescr));
    }

    protected String resolveFileName(FileDescriptor fileDescr) {
        return getStorageDir(fileDescr.getCreateDate()) + "/" + getFileName(fileDescr);
    }

    /**
     * INTERNAL. Don't use in application code.
     */
    protected String getStorageDir(Date createDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(createDate);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        return String.format("%d/%s/%s", year,
                StringUtils.leftPad(String.valueOf(month), 2, '0'),
                StringUtils.leftPad(String.valueOf(day), 2, '0'));
    }

    protected String getFileName(FileDescriptor fileDescriptor) {
        if (StringUtils.isNotBlank(fileDescriptor.getExtension())) {
            return fileDescriptor.getId().toString() + "." + fileDescriptor.getExtension();
        } else {
            return fileDescriptor.getId().toString();
        }
    }

    protected String getRegionName() {
        return amazonS3Config.getRegionName();
    }

    protected String getBucket() {
        return amazonS3Config.getBucket();
    }

    protected String getSecretAccessKey() {
        return amazonS3Config.getSecretAccessKey();
    }

    protected String getAccessKey() {
        return amazonS3Config.getAccessKey();
    }
}