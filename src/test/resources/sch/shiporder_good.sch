<?xml version="1.0" encoding="UTF-8"?>
<schema xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron sample and test file for the siphorder test files in ion-docval</title>

  <pattern xmlns="http://purl.oclc.org/dsdl/schematron" id="Sample">
    <rule context="shipto" flag="fatal">
        <assert test="name" flag="fatal">A shiporder shipto value must have a name</assert>
        <assert test="address" flag="fatal">A shiporder shipto value must have an address</assert>
        <assert test="city" flag="fatal">A shiporder shipto value must have a city</assert>
        <assert test="country" flag="fatal">A shiporder shipto value must have a country</assert>
    </rule>
    <rule context="item">
        <assert test="title" flag="fatal">A shiporder item must have a title</assert>
        <assert test="note" flag="warning">A shiporder item should have a note</assert>
        <assert test="xs:decimal(price) &gt; 5.0" flag="warning">A shiporder price should be greater than 1.0</assert>
    </rule>
  </pattern>
</schema>