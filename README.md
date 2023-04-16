
# ion-docval source repository

## Introduction

ion-docval is an XML document validator written in Java, which supports validating documents against multiple validation files, and supports multiple validation formats. These formats are:

- XML Schema (.xsd files)
- Schematron (.sch files)
- SVRL Transformation files (.xsl/.xslt files, but only those for SVRL, i.e. those created from Schematron files)

With ion-docval, you can run a local validation service without having to upload your documents to an online validation tool.

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

For information on how to run, use, or integrate ion-docval, see the [manual](docs/manual.md)

## License

This software is licensed under the MIT license. See [LICENSE](LICENSE) for more information.
