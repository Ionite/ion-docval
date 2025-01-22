# 1.2.1

* Fixed issue with keyword derivation if UBLVersionID element is present in the document
* Fixed signal handling: check for availability of SIGHUP
* Improved logging


# 1.2.0

The ValidatorManager now also keeps a list of Document Type Names,
if initialized with a configuration data object.
When validating, this name is added to the Validation Result.
The HttpServer includes this value in its response, as "document_type"
in case of JSON, and <DocumenType> in case of XML.

# 1.1.0

* Added a number of exceptions to the default 'version' field in the 
KeyworDeriver. To match the document type identifier specifications for 
certain Peppol documents, a number of exceptions have been added. Of 
course, you can always use custom keywords in your configuration and 
application, this change only affects the use of automatic keyword 
derivation. The default values for the 'version' field (the final part 
of the keyword) is as follows for the following documents:
  * 1.0 for Peppol Transaction Statistics Report
  * 1.1 for Peppol End User Statistics Report
  * 2.3 for Peppol Logistics Receipt Advice
  * 2.3 for Peppol Logistics Weight Statement
  * 2.3 for Peppol Logistics Transport Execution Plan Request
  * 2.3 for Peppol Logistics Transport Execution Plan
  * 2.3 for Peppol Logistics Waybill
  * 2.3 for Peppol Logistics Transportation Status Request
  * 2.3 for Peppol Logistics Transportation Status

# 1.0.1

Bugfixes:
* Fixed issue where a reload would not remove or add keywords from the validation manager (issue [#2](https://github.com/Ionite/ion-docval/issues/2))

# 1.0

Initial Release
