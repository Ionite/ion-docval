**This project has moved to Codeberg:** https://codeberg.org/ionite/ion-docval

# ion-docval - Ionite Document Validator

## Introduction

ion-docval is an XML document validation library and toolset.

Features:
- validate an XML document against one or more definition files
  - Supports XML Schema (.xsd) files
  - Supports Schematron (.sch) files
  - Supports SVRL Transformation files (.xsl/.xslt) files (i.e. those generated from Schematron)
- Multiple integration options:
  - Integratable .jar file
  - direct command-line application
  - (local) server for bulk processing, as well as integration with non-java applications

With ion-docval, you can run a local validation service without having to upload your documents to an online validation tool, and while this is a Java program (in order to fully support XSLT 2 and XPath 2), it has been designed to be used in non-Java environments.

This is the same software that is used as part of the [NPa Peppol Test Tool](https://test.peppolautoriteit.nl/validate).

## Distribution

This is the source repository of ion-docval. If you are looking for the pre-compiled binary distribution, go [here](https://github.com/ionite/ion-docval/releases)

## Building

In order to build this software, you need [Maven](https://maven.apache.org).

To (re)build the main source code:

    mvn clean compile package

To build everything and create a distribution zipfile:

    mvn package javadoc:javadoc license:add-third-party license:aggregate-download-licenses assembly:single

## Manual

For information on how to run, use, or integrate ion-docval, see the [manual](https://ion-docval.ionite.net/manual/introduction/)


## License

This software is licensed under the MIT license. See [LICENSE](LICENSE) for more information.

## Support

This software is free. Our time is not.

If you need help setting things up, if you have any feature requests, if you encounter bugs that need to be solved quickly, or if you simply want to make sure this project will be maintained in the future, you can [Contact Ionite](mailto:contact@ionite.net) to discuss a support or development contract.
