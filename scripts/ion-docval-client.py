#!/usr/bin/python3

# A sample client script for a client that talks to
# a running ion-docval-server instance.
#
# Prerequisites:
# - A running ion-docval-server
# - python3-lxml package
#

import argparse
import json
import requests
import socket
import struct
import sys

from lxml import etree
from typing import Union

PROTOCOL_VERSION = "Protocol: 1"

def main():
    pass

def xml_safe_fromstring(data: Union[str, bytes]) -> etree._Element:
    parser = etree.XMLParser(resolve_entities=False, no_network=True)
    if type(data) == str:
        data = data.encode('utf-8')
    document = etree.fromstring(data, parser=parser)
    for _ in document.iter(etree.Entity):
        raise DataError("Entities are not allowed in XML documents")
    return document

def get_document_from_sbdh(xml_bytes):
    """
    Returns the XML bytes of the document contained in the SBDH in the given file
    """
    doc = xml_safe_fromstring(xml_bytes)
    if doc.tag == "{http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader}StandardBusinessDocument":
        for child in doc.getchildren():
            if child.tag != "{http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader}StandardBusinessDocumentHeader":
                return etree.tostring(child)
        raise Exception("SBDH contains no child document")
    else:
        return xml_bytes
    sys.exit(0)

class DocValClient:
    def __init__(self, host, port, keyword=None):
        self.host = host
        self.port = port
        self.keyword = keyword
        
        #self.conn = ServerConnection(host, port)
    
    def validate_file(self, filename, strip_sbdh=False):
        print(f"Validating {filename}")
        with open(filename, 'rb') as inputfile:
            data = inputfile.read()
            
        if strip_sbdh:
            data = get_document_from_sbdh(data)
        
        return self.validate_data(data)

    def validate_data(self, data, keyword=None):
        if self.keyword is not None:
            keyword = self.keyword
        else:
            keyword = derive_keyword_from_xml(data)
        
        return self.send_document_to_server(data, keyword)

    def send_document_to_server(self, data, keyword):
        headers = {
            'Content-Type': 'application/xml',
            'Accept': 'application/json'
        }
        
        response = requests.post(f"http://{self.host}:{self.port}/api/validate", data=data, headers=headers)
        if (response.status_code == 200):
            return json.loads(response.content)
        result = {}
        return result


# Note: this derivation is done server-side if no keyword is passed; this
# code is only here to locally derive the keyword so that it can be
# used for configuration.
def derive_keyword_from_xml(xml_bytes):
    document = xml_safe_fromstring(xml_bytes)
    root_tag = etree.QName(document.tag)
    
    # Find
    doctype = "null"
    for path in [
        "{urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2}CustomizationID",
        "{urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100}ExchangedDocumentContext/{urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100}GuidelineSpecifiedDocumentContextParameter/{urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100}ID"
    ]:
        doctype_el = document.find(path)
        if doctype_el is not None and doctype_el.text is not None:
            doctype = doctype_el.text
            break

    # default versions
    if root_tag.namespace.startswith("urn:oasis:names:specification:ubl:"):
        version = "2.1"
    elif root_tag.namespace.startswith("urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:"):
        version = "D16B"
    else:
        version = "null"

    # override default version if specified in document
    for path in [
        "{urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2}UBLVersionID"
    ]:
        version_el = document.find(version)
        if version_el is not None and version_el.text is not None:
            version = version_el.text
    
    return f"{root_tag.namespace}::{root_tag.localname}##{doctype}::{version}"
    

def print_result_element(el_type, el):
        print(el_type)
        print(f"    Message: {el['message']}")
        print(f"       test: {el['test']}")
        if 'location' in el:
            print(f"   location: {el['location']}")
        if 'line' in el:
            print(f"       line: {el['line']}")
        if 'column' in el:
            print(f"     column: {el['column']}")
    
def print_result_data(result_data, show_details):
    print(f"Errors: {result_data['error_count']}")
    print(f"Warnings: {result_data['warning_count']}")
    print("")
    if show_details:
        for err in result_data['errors']:
            print_result_element("Error", err)
            print("")
        for warn in result_data['warnings']:
            print_result_element("Warning", warn)
            print("")
        print("")

def main():
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('-H', '--host', default="localhost",
                            help='Connect to the server at the given host (default localhost)')
    arg_parser.add_argument('-p', '--port', default="35792", type=int,
                            help='Connect to the server on the given port (default 35791)')
    arg_parser.add_argument('-k', '--keyword', help="Use the given keyword (default: derived from document)")
    arg_parser.add_argument('-d', '--details', action="store_true", default=False, help="Print additional information about errors and warnings")
    arg_parser.add_argument('-r', '--read-keyword', action="store_true", help="Don't validate the document, but derive and print its keyword")
    arg_parser.add_argument('document', help="The filename of the document to validate")
    arg_parser.add_argument('-s', '--strip-sbdh', action="store_true", help="If the given XML file is an SBDH, validate the XML contained in the XML instead of the full file itself")
    
    args = arg_parser.parse_args()
    
    if args.read_keyword:
        with open(args.document, 'rb') as infile:
            print(derive_keyword_from_xml(infile.read()))
        return 0
    else:
        client = DocValClient(args.host, args.port, args.keyword)
        result = client.validate_file(args.document, args.strip_sbdh)
        print_result_data(result, args.details)
        return result["error_count"]
    
if __name__ == '__main__':
    sys.exit(main())
