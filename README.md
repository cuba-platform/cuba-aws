[![license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)


# Overview

CUBA Amazon add-on enables using supported AWS services, for now it supports:

* Amazon Simple Storage Service (S3).

# Installation

The add-on can be added to your project in one of the ways described below. Installation from the Marketplace is the simplest way. The last version of the add-on compatible with the used version of the platform will be installed. Also, you can install the add-on by coordinates choosing the required version of the add-on from the table.

In case you want to install the add-on by manual editing or by building from sources see the complete add-ons installation guide in CUBA Platform documentation [CUBA Platform documentation](https://doc.cuba-platform.com/manual-latest/manual.html#app_components_usage).

## From the Marketplace

1. Open your application in CUBA Studio. Check the latest version of CUBA Studio on the  [CUBA Platform site](https://www.cuba-platform.com/download/previous-studio/).
2. Go to *CUBA -> Marketplace* in the main menu.

  ![marketplace](img/marketplace.png)

3. Find the CUBA Amazon add-on there.

  ![addons](img/addons.png)

4. Click *Install* and apply the changes.
The add-on corresponding to the used platform version will be installed.

## By Coordinates

1. Open your application in CUBA Studio. Check the latest version of CUBA Studio on the [CUBA Platform site](https://www.cuba-platform.com/download/previous-studio/).

2. Go to *CUBA -> Marketplace* in the main menu.

3. Click the icon in the upper-right corner.

    ![by-coordinates](img/by-coordinates.png)

4. Paste the add-on coordinates in the corresponding field as follows:

   `com.haulmont.addon.cubaaws:cubaaws-global:<add-on version>`

   where `<add-on version>` is compatible with the used version of the CUBA platform.

  | Platform Version | Add-on Version |
|------------------|----------------|
| 7.2            | 7.2         |

5. Click *Install* and apply the changes. The add-on will be installed to your project.

# Configuration

 1. To add Amazon S3 support, you need to register `AmazonS3FileStorage` class in the `spring.xml` file in the `core` module:

 ```xml
     <bean name="cuba_FileStorage" class="com.haulmont.addon.cubaaws.s3.AmazonS3FileStorage"/>
 ```

 2. Next, you should define your Amazon settings in the `app.properties` file in the `core` module:

 * `cuba.amazonS3.accessKey` - Amazon S3 access key.

 * `cuba.amazonS3.secretAccessKey` - Amazon S3 secret access key.

 * `cuba.amazonS3.region` - Amazon S3 region.

 * `cuba.amazonS3.bucket` - Amazon S3 bucket name.

 * `cuba.amazonS3.chunkSize` - Amazon S3 chunk size.
 
 * `cuba.amazonS3.endpointUrl` (optional) - Endpoint URL for Amazon S3 compatible storages.

 The `accessKey` and `secretAccessKey` should be those of your AWS IAM user account, not the AWS account itself. You can find the correct credentials on the *Users* tab of your AWS console.

 **Example:**
 ```properties
 cuba.amazonS3.accessKey = AAAABBBBCCCCDD11CC22
 cuba.amazonS3.secretAccessKey = AbCD+eFFGK3iAB9Ca9BCAB7ddDDABcabCabc9aBC
 cuba.amazonS3.region = eu-north-1
 cuba.amazonS3.bucket = s3-ec2-test
 cuba.amazonS3.chunkSize = 5500
 ```

 **Example for Yandex Cloud Object storage**
 ```properties
 cuba.amazonS3.accessKey = AAAABBBBCCCCDD11CC22
 cuba.amazonS3.secretAccessKey = AbCD+eFFGK3iAB9Ca9BCAB7ddDDABcabCabc9aBC
 cuba.amazonS3.region = ru-central1
 cuba.amazonS3.bucket = test
 cuba.amazonS3.chunkSize = 5500
 cuba.amazonS3.endpointUrl = https://storage.yandexcloud.net
```

