
# ion-docval binary distribution

## Introduction

ion-docval is an XML document validator written in Java, which supports validating documents against multiple validation files, and supports multiple validation formats. These formats are:

- XML Schema (.xsd files)
- Schematron (.sch files)
- SVRL Transformation files (.xsl/.xslt files, but only those for SVRL, i.e. those created from Schematron files)

With ion-docval, you can run a local validation service without having to upload your documents to an online validation tool.

This is the same software that is used as part of the [NPa Peppol Test Tool](https://test.peppolautoriteit.nl/validate).


## Source code

This is the binary distribution for ion-docval. The source code can be found [on github](https://github.com/ionite/ion-docval).


## Manual

For information on how to run, use, or integrate ion-docval, see the [manual](docs/manual.md)

## License

This software is licensed under the MIT license. See [LICENSE](LICENSE) for more information.

This binary distribution also includes a number of dependencies that have their own licenses. See the [Third Party](THIRD-PARTY.txt) file for an overview of these licenses, and the [licenses directory](docs/licenses) for all complete licenses.