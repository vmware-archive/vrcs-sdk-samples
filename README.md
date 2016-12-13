# vRealize Code Stream Plug-In SDK Samples

## Overview
vRealize Code Stream (vRCS) is a Dev Ops release automation solution that ensures continuous development and integration of your applications. You create stages and collections of tasks in those stages, which is the pipeline. vRCS runs the tasks in each stage, and ensures that each task passes before the pipeline moves to the next stage of development.

The Java-based Plug-in SDK allows you to extend vRCS's capability and create your own customized tasks to add to your existing pipeline.

## Try it out

### Prerequisites

* A development machine running on Microsoft Windows, Apple Mac OS X or Linux. 
* A development appliance of vRCS installed in your environment (Recommended).
* JDK 8
* Maven 3.1.1+

### Build & Run

1. Clone this repository to your development machine
2. Go to the directory /lib from the root of the repository and build using `mvn clean install`
3. Go back to the root and build using `mvn clean install`
4. Go to the directory /samples/rest-sample-plugin and deploy the REST sample plug-in to your development appliance using `mvn clean deploy -Dbundle.deploy.host=VRCS_APPLIANCE_HOST`
5. When you sign into your development appliance you will see the sample plug-in available as a new Task and Endpoint

## Documentation

For more details on developing your own plugin using the SDK please refer to the vRCS Plug-in SDK [Development Guide](https://github.com/vmware/vrcs-sdk-samples/wiki/Home).

## Releases & Major Branches

The samples here are written against the vRCS Plug-in SDK version 2.2.0

## Contributing

The vrcs-sdk-samples project team welcomes contributions from the community. If you wish to contribute code and you have not
signed our contributor license agreement (CLA), our bot will update the issue when you open a Pull Request. For any
questions about the CLA process, please refer to our [FAQ](https://cla.vmware.com/faq). For more detailed information,
refer to [CONTRIBUTING](CONTRIBUTING.md).

## License

This product is licensed under the Apache 2.0 license. For more information please refert to [NOTICE](NOTICE.txt) and [LICENSE](LICENSE.txt).
