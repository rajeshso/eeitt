package uk.gov.hmrc.eeitt.services

import uk.gov.hmrc.play.test.UnitSpec

class EtmpDataParserSpec extends UnitSpec {

  // Specification of ETMP files can be found here: https://confluence.tools.tax.service.gov.uk/display/AF/ETMP+data
  // there are more rules that are tested here but they relate to fields we don't really care about for now

  "Parsing individual lines of a flat file containing ETMP business users" should {
    "succeed if line matches expected format for UK" in {
      val validLine = "001|regNum|taxReg|taxRegDesc|orgType|orgTypeDesc|orgName|title|name1|name2|postcode|GB"
      noException should be thrownBy EtmpDataParser.parseBusinessUserLine(validLine)
    }

    "succeed if line matches expected format including no country code" in {
      val validLine = "001|regNum|taxReg|taxRegDesc|orgType|orgTypeDesc|orgName|title|name1|name2|postcode|"
      noException should be thrownBy EtmpDataParser.parseBusinessUserLine(validLine)
    }

    "succeed if line matches expected format including no postcode outside UK" in {
      val validLine = "001|regNum|taxReg|taxRegDesc|orgType|orgTypeDesc|orgName|title|name1|name2||IT"
      noException should be thrownBy EtmpDataParser.parseBusinessUserLine(validLine)
    }

    "fail if registration number (aka customer identification number) is missing" in {
      val missingOrEmptyRegNumber = List(
        "001||taxReg|taxRegDesc|orgType|orgTypeDesc|orgName|title|name1|name2|postcode|countryCode",
        "001|     |taxReg|taxRegDesc|orgType|orgTypeDesc|orgName|title|name1|name2|postcode|countryCode"
      )

      missingOrEmptyRegNumber.foreach { line =>
        val exception = intercept[LineParsingException] { EtmpDataParser.parseBusinessUserLine(line) }
        exception.msg shouldBe s"Missing registrationNumber in line: $line"
      }
    }

    "fail if customer is from the UK but doesn't have a postcode" in {
      val missingOrEmptyPostcode = List(
        "001|regNum|taxReg|taxRegDesc|orgType|orgTypeDesc|orgName|title|name1|name2||GB",
        "001|regNum|taxReg|taxRegDesc|orgType|orgTypeDesc|orgName|title|name1|name2|         |GB"
      )

      missingOrEmptyPostcode.foreach { line =>
        val exception = intercept[LineParsingException] { EtmpDataParser.parseBusinessUserLine(line) }
        exception.msg shouldBe s"Missing postcode for a UK entity in line: $line"
      }
    }

    "fail if line contains incorrect number of tokens" in {
      val invalidLine = "foo|bar"

      val exception = intercept[LineParsingException] {
        EtmpDataParser.parseBusinessUserLine(invalidLine)
      }

      exception.msg shouldBe s"Failed to parse due to unexpected format of line: $invalidLine"
    }

  }

  "Parsing a flat file with ETMP business users data" should {
    "be successful" in {
      val flatFile =
        """
          >00|CUSTOMER_DATA|ETMP|MDTP|20161103|141116
          >001|XTAL00000100044 |ZAGL|Aggregate Levy (AGL)|7|Limited Company|Organisation1||||BN12 4XL|GB
          >001|XMAP0000010005|ZAPD|Air Passenger Duty (APD)|7|Limited Company|Organisation2||||BN12 4XL|GB
          >001| XRIP00000100053|ZIPT|Insurance premium tax (IPT)|1|Sole Proprietor||Mr|Name11|Name21||IT
          >001|XSBD00440000020|ZBD|Bingo Duty (BD)|7|Limited Company|Organisation3||||BN12 4XL|GB
          >001|XTAL00000100044|ZAGL|Aggregate Levy (AGL)|7|Limited Company|Organisation1||||BN12 4XL|GB
          >001|XALF00000100019|ZLFT|Landfill Tax Registration Number|1|Sole Proprietor||Mr|Name13|Name23||RO
          >001|XDLD00000100001|ZLD|Lottery Duty (LD)|1|Sole Proprietor||Mr|Name12|Name22||RO
          >99|CUSTOMER_DATA|000000007
          >
        """.stripMargin('>')

      EtmpDataParser.parseFileWithBusinessUsers(flatFile).size shouldBe 7
    }
  }

  "Parsing individual lines of a flat file containing ETMP agents" should {
    "succeed if line matches expected format for UK" in {
      val validLine = "fileType|arn|AgentIdType|AgentIdTypeDesc|AgentOrgType|AgentOrgTypeDesc|AgentOrgName|AgentTitle|AgentName1|AgentName2|AgentPostcode|GB|CustRegNum|CustTaxReg|CustTaxRegDesc|CustOrgType|CustOrgTypeDesc|CustOrgName|CustTitle|CustName1|CustName2|CustPostcode|GB"
      noException should be thrownBy EtmpDataParser.parseAgentLine(validLine)
    }

    "succeed if line matches expected format including no agent country code" in {
      val validLine = "fileType|arn|AgentIdType|AgentIdTypeDesc|AgentOrgType|AgentOrgTypeDesc|AgentOrgName|AgentTitle|AgentName1|AgentName2|AgentPostcode||CustRegNum|CustTaxReg|CustTaxRegDesc|CustOrgType|CustOrgTypeDesc|CustOrgName|CustTitle|CustName1|CustName2|CustPostcode|GB"
      noException should be thrownBy EtmpDataParser.parseAgentLine(validLine)
    }

    "succeed if line matches expected format including no agent postcode code from abroad" in {
      val validLine = "fileType|arn|AgentIdType|AgentIdTypeDesc|AgentOrgType|AgentOrgTypeDesc|AgentOrgName|AgentTitle|AgentName1|AgentName2|AgentPostcode|IT|CustRegNum|CustTaxReg|CustTaxRegDesc|CustOrgType|CustOrgTypeDesc|CustOrgName|CustTitle|CustName1|CustName2|CustPostcode|GB"
      noException should be thrownBy EtmpDataParser.parseAgentLine(validLine)
    }

    "succeed if line matches expected format including no customer country code" in {
      val validLine = "fileType|arn|AgentIdType|AgentIdTypeDesc|AgentOrgType|AgentOrgTypeDesc|AgentOrgName|AgentTitle|AgentName1|AgentName2|AgentPostcode|GB|CustRegNum|CustTaxReg|CustTaxRegDesc|CustOrgType|CustOrgTypeDesc|CustOrgName|CustTitle|CustName1|CustName2|CustPostcode|"
      noException should be thrownBy EtmpDataParser.parseAgentLine(validLine)
    }

    "succeed if line matches expected format including no customer postcode code from abroad" in {
      val validLine = "fileType|arn|AgentIdType|AgentIdTypeDesc|AgentOrgType|AgentOrgTypeDesc|AgentOrgName|AgentTitle|AgentName1|AgentName2|AgentPostcode|GB|CustRegNum|CustTaxReg|CustTaxRegDesc|CustOrgType|CustOrgTypeDesc|CustOrgName|CustTitle|CustName1|CustName2||IT"
      noException should be thrownBy EtmpDataParser.parseAgentLine(validLine)
    }

    "fail if Agent Reference Number (arn) is missing" in {
      val missingOrEmptyARN = List(
        "fileType|    |AgentIdType|AgentIdTypeDesc|AgentOrgType|AgentOrgTypeDesc|AgentOrgName|AgentTitle|AgentName1|AgentName2|AgentPostcode|AgentCountryCode|CustRegNum|CustTaxReg|CustTaxRegDesc|CustOrgType|CustOrgTypeDesc|CustOrgName|CustTitle|CustName1|CustName2|CustPostcode|CustCountryCode",
        "fileType||AgentIdType|AgentIdTypeDesc|AgentOrgType|AgentOrgTypeDesc|AgentOrgName|AgentTitle|AgentName1|AgentName2|AgentPostcode|AgentCountryCode|CustRegNum|CustTaxReg|CustTaxRegDesc|CustOrgType|CustOrgTypeDesc|CustOrgName|CustTitle|CustName1|CustName2|CustPostcode|CustCountryCode"
      )

      missingOrEmptyARN.foreach { line =>
        val exception = intercept[LineParsingException] { EtmpDataParser.parseAgentLine(line) }
        exception.msg shouldBe s"Missing arn in line: $line"
      }
    }

    "fail if agent is from the UK but doesn't have a postcode" in {
      val missingOrEmptyPostcode = List(
        "fileType|arn|AgentIdType|AgentIdTypeDesc|AgentOrgType|AgentOrgTypeDesc|AgentOrgName|AgentTitle|AgentName1|AgentName2|       |GB|CustRegNum|CustTaxReg|CustTaxRegDesc|CustOrgType|CustOrgTypeDesc|CustOrgName|CustTitle|CustName1|CustName2|CustPostcode|CustCountryCode",
        "fileType|arn|AgentIdType|AgentIdTypeDesc|AgentOrgType|AgentOrgTypeDesc|AgentOrgName|AgentTitle|AgentName1|AgentName2||GB|CustRegNum|CustTaxReg|CustTaxRegDesc|CustOrgType|CustOrgTypeDesc|CustOrgName|CustTitle|CustName1|CustName2|CustPostcode|CustCountryCode"
      )

      missingOrEmptyPostcode.foreach { line =>
        val exception = intercept[LineParsingException] { EtmpDataParser.parseAgentLine(line) }
        exception.msg shouldBe s"Missing postcode for a UK entity in line: $line"
      }
    }

    "fail if customer part of an agent record is invalid" in {
      val missingRegNum = "fileType|arn|AgentIdType|AgentIdTypeDesc|AgentOrgType|AgentOrgTypeDesc|AgentOrgName|AgentTitle|AgentName1|AgentName2|AgentPostcode|AgentCountryCode||CustTaxReg|CustTaxRegDesc|CustOrgType|CustOrgTypeDesc|CustOrgName|CustTitle|CustName1|CustName2|CustPostcode|CustCountryCode"
      val exception = intercept[LineParsingException] { EtmpDataParser.parseAgentLine(missingRegNum) }
      exception.msg should include("Missing registrationNumber")
      // other problems with a customer part of the record are assumed to also be captured as we re-use a function
      // that is unit tested above
    }

    "fail if line contains incorrect number of tokens" in {
      val invalidLine = "not|enough|tokens"
      val exception = intercept[LineParsingException] { EtmpDataParser.parseAgentLine(invalidLine) }
      exception.msg should include(s"Failed to parse due to unexpected format of line: $invalidLine")
    }
  }

  "Parsing a flat file with ETMP agent data" should {
    "be successful" in {
      val flatFile =
        """
          >00|AGENT_DATA|ETMP|MDTP|20161103|141316
          >002|KARN0000086|ARN|Agent Reference Number(ARN)|7|Company|Agent1||||BN12 4XL|GB|XMAP00000100051|ZAPD|Air Passenger Duty (APD)|7|Limited Company|Organisation2||||BN12 4XL|GB
          >002|KARN0000086|ARN|Agent Reference Number(ARN)|7|Company|Agent1||||BN12 4XL|GB|XRIP00000100053|ZIPT|Insurance premium tax (IPT)|1|Sole Proprietor||Mr|Name11|Name21|1234567891|IT
          >002|KARN0000086|ARN|Agent Reference Number(ARN)|7|Company|Agent1||||BN12 4XL|GB|XSBD00440000020|ZBD|Bingo Duty (BD)|7|Limited Company|Organisation3||||BN12 4XL|GB
          >002|KARN0000087|ARN|Agent Reference Number(ARN)|7|Company|Agent2||||BN12 4XL|GB|XTAL00000100044|ZAGL|Aggregate Levy (AGL)|7|Limited Company|Organisation1||||BN12 4XL|GB
          >002|KARN0000087|ARN|Agent Reference Number(ARN)|7|Company|Agent2||||BN12 4XL|GB|XALF00000100019|ZLFT|Landfill Tax Registration Number|1|Sole Proprietor||Mr|Name13|Name23||RO
          >002|KARN0000088|ARN|Agent Reference Number(ARN)|1|Sole proprietor||Mr|AGName1|AGName2||RO|XDLD00000100001|ZLD|Lottery Duty (LD)|1|Sole Proprietor||Mr|Name12|Name22||RO
          >99|AGENT_DATA|000000006
          >
        """.stripMargin('>')

      val numberOfUniqueAgentsInTheFlatFile = 3 // as defined by unique ARNs
      EtmpDataParser.parseFileWithAgents(flatFile).size shouldBe numberOfUniqueAgentsInTheFlatFile
    }
  }

}

