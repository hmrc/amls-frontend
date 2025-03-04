/*
 * Copyright 2024 HM Revenue & Customs
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

package services.encryption

import models.responsiblepeople.{PersonName, ResponsiblePerson}
import org.scalatest.TryValues
import utils.AmlsSpec

import java.nio.charset.StandardCharsets.UTF_8

class CryptoServiceSpec extends AmlsSpec with TryValues {

  val cryptoService = new CryptoService(appConfig, applicationCrypto)

  ".decrypt" must {
    "decrypt an encrypted value" in {
      // Given
      val encryptedString         =
        "9DafAT+wxEkATqIvyRvpP8BGbiOGEBPE1Z1S60McZtDFzTIQ7p/1g0fdKkT6Pb4Fkd8Wjk5iUBrKUafr4o5DytSA+lxvsO5d5lwFLpwmu0EyrpurRXPYEqIOV0gyAz4OuFtFOGVMCbRXhnLis45zqCZcoRsTPNZRvpKBSulHyU4ZJdvlez6dugfo9obQTcEnxG5m+Bn15W2tHyWtiF8iYR2wenALcysMGfnDvBadMfXXKfH9tia+kOhHN2YD3nclElynYM7SxmcRc2L3/dugbP9ftuhDJC81EE4HBQlWP5J30b/QbkzuLy7LoBHt513mW8iZJv/0sCNCiH189ZpHOuHqt5BYPP6uEpZ4y86hy+gJPo7pG/yrIu3dDYG7W6mKIfox7LYjvHe+aaj7z5B2VuodnLBpfdJcuXGmjN8vn58TATPN/t/U6ok8yOfCQE3LyBKWu5yHF5YaAzX89NyAJdsVAN3iwwdB6Nxf8zZmw/Zsr//I+01u/jNwDTw6TASg3pG+KC9FuPProz4bju1LtBohdK8s7F/5SBGA/sy8RQcPVQwXGTFu+j8GW6XoaZYBRYLA94oolHMEAbjvlZBavzwAFlu2s4mllGO6khl6+Gw="
      val expectedDecryptedString =
        "{\"involvedInOther\":false,\"expectedAMLSTurnover\":\"04\",\"businessFranchise\":true,\"franchiseName\":\"FranchiserName1\",\"isRecorded\":true,\"transactionTypes\":{\"types\":[\"01\",\"02\",\"03\"],\"software\":\"Test package\"},\"isOutside\":true,\"countries\":[\"AD\",\"GB\"],\"ncaRegistered\":true,\"hasWrittenGuidance\":true,\"hasPolicy\":true,\"riskassessments\":[\"02\",\"01\"],\"employeeCount\":\"12345678901\",\"employeeCountAMLSSupervision\":\"11223344556\",\"hasChanged\":false,\"hasAccepted\":true}"

      // When
      val decryptedString = cryptoService.decrypt(encryptedString)

      // Then
      decryptedString mustEqual expectedDecryptedString
    }

    "handle an unencrypted value" in {
      // Given
      val unencryptedString = "TradingPremises"

      // When
      val decryptedString = cryptoService.decrypt(unencryptedString)

      // Then
      decryptedString mustEqual unencryptedString
    }
  }

  ".doubleDecryptJsonString" must {
    "decrypt a doubly encrypted JSON string" in {
      // Given
      val doubleEncryptedJsonString   =
        "KQky4pyaAaGKHJPoDoZ+4EwBU0gkhLRBGaardzogtKX+p5ZDElKen/QvW6oFYVkOk8qQG7gnZSPNart+WS6WZDZkjU4nN7lfLoHl26oC3SJLQYrUNgGhqwMmhFiVMiNdUooqIUbqi5d/Ia2/6CJjQdp+0LEOPlcMQ/kFsk1CrKR1zPQiX0yyCDrEKCHu2vWL/iFjDS2BajfS9tH4ERIMl+Fo4nFkuNbISNwwwNPudLgKTj2Aj+H/LA7eSzu9Km2kjQSlb706hVZH7KLtXCCbZHXkpvOxxUlUYD/pcKYm4hQcNBOACC4AJH3I2gz9qTe02XhpgF1TbqnZBDepsoiUYEqfoW7o6U3iDlKqXseeLZRu3q7CticsZXN3ixwdKKAVVD5ZScxpijgj8JmeZ50C10kYi2CW8L6U6AzT7AINGyRZw+itPQI5vWFNFfnk6WNc7s0a8c62NYOLF0WlfGb/mIhWCGBkRVHh0wDWNpoYyrkUh6PMNV3zKIUf3R+WJqxdITqSOGONItrQmCwJUNMYm01GlU7nSkdRLRcmhS/oYA24feMNsfAsxj8d8uGf6wF9dTeYPrVOcs9TMql+PqunqOnboVBDAzbFy8Dd1f3Fb539Vn6LcoOBevy5FHvPPI6X3iwvact5r8UcLB2AFNS72PfgPnRhNpWll0xld8cD7eOrxfPW1SEgczUGXS4mgabOOdopEDT5wuwYmBtfpz78P0UvHyqxK8qpnhvnR9vDcafi/f9pgehxmlevmw29dKFpSi9lBeMlySbk30DYYmkIia+fOLTJCAD2E0DzLpry1JjwgEGfbKlG6J228Wq1Tq+0"
      val expectedDecryptedJsonString =
        "{\"involvedInOther\":false,\"expectedAMLSTurnover\":\"04\",\"businessFranchise\":true,\"franchiseName\":\"FranchiserName1\",\"isRecorded\":true,\"transactionTypes\":{\"types\":[\"01\",\"02\",\"03\"],\"software\":\"Test package\"},\"isOutside\":true,\"countries\":[\"AD\",\"GB\"],\"ncaRegistered\":true,\"hasWrittenGuidance\":true,\"hasPolicy\":true,\"riskassessments\":[\"02\",\"01\"],\"employeeCount\":\"12345678901\",\"employeeCountAMLSSupervision\":\"11223344556\",\"hasChanged\":false,\"hasAccepted\":true}"

      // When
      val decryptedJsonString = cryptoService.doubleDecryptJsonString(doubleEncryptedJsonString)

      // Then
      decryptedJsonString.value mustEqual expectedDecryptedJsonString
    }

    "decrypt a singly encrypted JSON string" when {
      "JSON string is an object" in {
        // Given
        val encryptedJsonString         =
          "Zw/bw0YBxBLKyIyConnnlBLZ34/lT0pPMDr+EwRa2z9knJUt4gI0NfYy56IF+WFa7wvDbq41iTG5WBp27WwSchnTNCpDKgcJ/55ROItt8xCbXZp1fZT/ftflBcse8QQnaPT/2XxbsoIaJMPwoaBSczgai6YXRJFnm/pTm4gmGz2VpLGX7DwD+ozGaH3uwTva9emgrYkEAeEKrr146zPpxZWksZfsPAP6jMZofe7BO9rj3DrQgm+GlEDNFGpoReIpG3S0Wc3LTtRsk+DWO1iyaSPzTSRrB3wE/YnMdrUotRzVM3QPoHxrLreEOe63zy/SFlNJe7IQvfNZ4sWNQCVKF3WCO3U1tNn0iU56v6gAADN2TzbsXYyUjVs9QKdwtB9zxRC/0301R6YZWdYyaarNeqt0cEkpdGXf0N5Tr4camD4WR4Y82eGYgHeEjCnJsUKZ0hYanY6KR1zw3WkxUlAYx9W61KvMQKro1VL7VkcMOAxzU2jNtnUhGxagOIqgKhasRfMHofyi4zruVhg2izHG7UDCXQGJVs0FE1t+0pQ2cTnX03XUthXjBRCe4qe6+Ha1wihCnDTAkNffyDIKczO8vmfqcf7UqLMVIft70rwFbng="
        val expectedDecryptedJsonString =
          "{\"tcspTypes\":{\"serviceProviders\":[\"03\",\"02\",\"01\",\"05\",\"04\"]},\"onlyOffTheShelfCompsSold\":{\"onlyOffTheShelfCompsSold\":true},\"complexCorpStructureCreation\":{\"complexCorpStructureCreation\":true},\"providedServices\":{\"services\":[\"08\",\"03\",\"06\",\"02\",\"07\",\"01\",\"05\",\"04\"],\"details\":\"SpecifyOther\"},\"doesServicesOfAnotherTCSP\":true,\"servicesOfAnotherTCSP\":{\"servicesOfAnotherTCSP\":true,\"mlrRefNumber\":\"111111111111111\"},\"hasChanged\":false,\"hasAccepted\":true}"

        // When
        val decryptedString = cryptoService.doubleDecryptJsonString(encryptedJsonString)

        // Then
        decryptedString.value mustEqual expectedDecryptedJsonString
      }

      "JSON string is an array" in {
        // Given
        val encryptedJsonString         =
          "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"
        val expectedDecryptedJsonString =
          "[{\"registeringAgentPremises\":{\"agentPremises\":true},\"yourTradingPremises\":{\"tradingName\":\"TradingName\",\"postcode\":\"NE99 1ZZ\",\"addressLine1\":\"AddressLine1\",\"addressLine2\":\"AddressLine2\",\"addressLine3\":\"AddressLine3\",\"addressLine4\":\"AddressLine4\",\"isResidential\":true,\"startDate\":\"2001-01-01\"},\"businessStructure\":{\"agentsBusinessStructure\":\"01\"},\"agentName\":{\"agentName\":\"AgentLegalEntityName\"},\"whatDoesYourBusinessDoAtThisAddress\":{\"activities\":[\"04\",\"01\",\"03\",\"02\",\"07\",\"08\",\"05\",\"06\"]},\"msbServices\":{\"msbServices\":[\"02\",\"03\",\"05\",\"01\",\"04\"]},\"hasChanged\":false,\"lineId\":333333,\"hasAccepted\":true},{\"registeringAgentPremises\":{\"agentPremises\":true},\"yourTradingPremises\":{\"tradingName\":\"Trade\",\"postcode\":\"NE99 1ZZ\",\"addressLine1\":\"a\",\"addressLine2\":\"a\",\"addressLine3\":\"a\",\"addressLine4\":\"a\",\"isResidential\":true,\"startDate\":\"1967-08-13\"},\"businessStructure\":{\"agentsBusinessStructure\":\"01\"},\"agentName\":{\"agentName\":\"Legal Agent\"},\"whatDoesYourBusinessDoAtThisAddress\":{\"activities\":[\"04\",\"01\",\"03\",\"02\",\"07\",\"08\",\"05\",\"06\"]},\"msbServices\":{\"msbServices\":[\"02\",\"03\",\"05\",\"01\",\"04\"]},\"hasChanged\":false,\"lineId\":444444,\"hasAccepted\":true},{\"registeringAgentPremises\":{\"agentPremises\":true},\"yourTradingPremises\":{\"tradingName\":\"TradingName\",\"postcode\":\"NE99 1ZZ\",\"addressLine1\":\"AgentAddressLine1\",\"addressLine2\":\"AgentAddressLine2\",\"addressLine3\":\"AgentAddressLine3\",\"addressLine4\":\"AgentAddressLine4\",\"isResidential\":true,\"startDate\":\"2001-01-01\"},\"businessStructure\":{\"agentsBusinessStructure\":\"01\"},\"agentName\":{\"agentName\":\"AgentLegalEntityName2\"},\"whatDoesYourBusinessDoAtThisAddress\":{\"activities\":[\"04\",\"01\",\"03\",\"02\",\"07\",\"08\",\"05\",\"06\"]},\"msbServices\":{\"msbServices\":[\"02\",\"03\",\"05\",\"01\",\"04\"]},\"hasChanged\":false,\"lineId\":555555,\"hasAccepted\":true},{\"registeringAgentPremises\":{\"agentPremises\":true},\"yourTradingPremises\":{\"tradingName\":\"TName\",\"postcode\":\"NE99 1ZZ\",\"addressLine1\":\"AgentAddressLine1\",\"addressLine2\":\"AgentAddressLine2\",\"addressLine3\":\"AgentAddressLine3\",\"addressLine4\":\"AgentAddressLine4\",\"isResidential\":true,\"startDate\":\"2001-01-01\"},\"businessStructure\":{\"agentsBusinessStructure\":\"02\"},\"agentCompanyDetails\":{\"agentCompanyName\":\"AnotherAgentLegalEntityName2\",\"companyRegistrationNumber\":\"12345678\"},\"whatDoesYourBusinessDoAtThisAddress\":{\"activities\":[\"04\",\"01\",\"03\",\"02\",\"07\",\"08\",\"05\",\"06\"]},\"msbServices\":{\"msbServices\":[\"02\",\"03\",\"05\",\"01\",\"04\"]},\"hasChanged\":false,\"lineId\":666666,\"hasAccepted\":true},{\"registeringAgentPremises\":{\"agentPremises\":false},\"yourTradingPremises\":{\"tradingName\":\"OwnBusinessTradingName\",\"postcode\":\"NE99 1ZZ\",\"addressLine1\":\"OwnBusinessAddressLine1\",\"addressLine2\":\"OwnBusinessAddressLine2\",\"addressLine3\":\"OwnBusinessAddressLine3\",\"addressLine4\":\"OwnBusinessAddressLine4\",\"isResidential\":false,\"startDate\":\"2001-01-01\"},\"whatDoesYourBusinessDoAtThisAddress\":{\"activities\":[\"04\",\"01\",\"03\",\"02\",\"07\",\"08\",\"05\",\"06\"]},\"msbServices\":{\"msbServices\":[\"02\",\"03\",\"05\",\"01\",\"04\"]},\"hasChanged\":false,\"lineId\":111111,\"hasAccepted\":true},{\"registeringAgentPremises\":{\"agentPremises\":false},\"yourTradingPremises\":{\"tradingName\":\"OwnBusinessTradingName1\",\"postcode\":\"NE99 1ZZ\",\"addressLine1\":\"OB11AddressLine1\",\"addressLine2\":\"OB1AddressLine2\",\"addressLine3\":\"OB1AddressLine3\",\"addressLine4\":\"OB1AddressLine4\",\"isResidential\":false,\"startDate\":\"2001-01-01\"},\"whatDoesYourBusinessDoAtThisAddress\":{\"activities\":[\"04\",\"01\",\"03\",\"02\",\"07\",\"08\",\"05\",\"06\"]},\"msbServices\":{\"msbServices\":[\"02\",\"03\",\"05\",\"01\",\"04\"]},\"hasChanged\":false,\"lineId\":222222,\"hasAccepted\":true}]"

        // When
        val decryptedString = cryptoService.doubleDecryptJsonString(encryptedJsonString)

        // Then
        decryptedString.value mustEqual expectedDecryptedJsonString
      }
    }

    "decrypt an encrypted string at most twice" in {
      // Given
      val responsiblePerson     = ResponsiblePerson(personName = Some(PersonName("David", Some("Dolores"), "Smith")))
      val jsonString            = ResponsiblePerson.writes.writes(responsiblePerson).toString()
      val singlyEncryptedString = cryptoService.encryptJsonString(jsonString)
      val doubleEncryptedString = cryptoService.encryptJsonString(singlyEncryptedString.toString())
      val tripleEncryptedString = cryptoService.encryptJsonString(doubleEncryptedString.toString())

      // When
      val decryptedString = cryptoService.doubleDecryptJsonString(tripleEncryptedString.toString())

      // Then
      decryptedString.value mustEqual singlyEncryptedString.toString()
    }
  }

  "decrypting bytes" must {
    "decipher an enciphered string" in {
      // Given
      val encryptedJsonString         =
        "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"
      val expectedDecryptedJsonString =
        "[{\"registeringAgentPremises\":{\"agentPremises\":true},\"yourTradingPremises\":{\"tradingName\":\"TradingName\",\"postcode\":\"NE99 1ZZ\",\"addressLine1\":\"AddressLine1\",\"addressLine2\":\"AddressLine2\",\"addressLine3\":\"AddressLine3\",\"addressLine4\":\"AddressLine4\",\"isResidential\":true,\"startDate\":\"2001-01-01\"},\"businessStructure\":{\"agentsBusinessStructure\":\"01\"},\"agentName\":{\"agentName\":\"AgentLegalEntityName\"},\"whatDoesYourBusinessDoAtThisAddress\":{\"activities\":[\"04\",\"01\",\"03\",\"02\",\"07\",\"08\",\"05\",\"06\"]},\"msbServices\":{\"msbServices\":[\"02\",\"03\",\"05\",\"01\",\"04\"]},\"hasChanged\":false,\"lineId\":333333,\"hasAccepted\":true},{\"registeringAgentPremises\":{\"agentPremises\":true},\"yourTradingPremises\":{\"tradingName\":\"Trade\",\"postcode\":\"NE99 1ZZ\",\"addressLine1\":\"a\",\"addressLine2\":\"a\",\"addressLine3\":\"a\",\"addressLine4\":\"a\",\"isResidential\":true,\"startDate\":\"1967-08-13\"},\"businessStructure\":{\"agentsBusinessStructure\":\"01\"},\"agentName\":{\"agentName\":\"Legal Agent\"},\"whatDoesYourBusinessDoAtThisAddress\":{\"activities\":[\"04\",\"01\",\"03\",\"02\",\"07\",\"08\",\"05\",\"06\"]},\"msbServices\":{\"msbServices\":[\"02\",\"03\",\"05\",\"01\",\"04\"]},\"hasChanged\":false,\"lineId\":444444,\"hasAccepted\":true},{\"registeringAgentPremises\":{\"agentPremises\":true},\"yourTradingPremises\":{\"tradingName\":\"TradingName\",\"postcode\":\"NE99 1ZZ\",\"addressLine1\":\"AgentAddressLine1\",\"addressLine2\":\"AgentAddressLine2\",\"addressLine3\":\"AgentAddressLine3\",\"addressLine4\":\"AgentAddressLine4\",\"isResidential\":true,\"startDate\":\"2001-01-01\"},\"businessStructure\":{\"agentsBusinessStructure\":\"01\"},\"agentName\":{\"agentName\":\"AgentLegalEntityName2\"},\"whatDoesYourBusinessDoAtThisAddress\":{\"activities\":[\"04\",\"01\",\"03\",\"02\",\"07\",\"08\",\"05\",\"06\"]},\"msbServices\":{\"msbServices\":[\"02\",\"03\",\"05\",\"01\",\"04\"]},\"hasChanged\":false,\"lineId\":555555,\"hasAccepted\":true},{\"registeringAgentPremises\":{\"agentPremises\":true},\"yourTradingPremises\":{\"tradingName\":\"TName\",\"postcode\":\"NE99 1ZZ\",\"addressLine1\":\"AgentAddressLine1\",\"addressLine2\":\"AgentAddressLine2\",\"addressLine3\":\"AgentAddressLine3\",\"addressLine4\":\"AgentAddressLine4\",\"isResidential\":true,\"startDate\":\"2001-01-01\"},\"businessStructure\":{\"agentsBusinessStructure\":\"02\"},\"agentCompanyDetails\":{\"agentCompanyName\":\"AnotherAgentLegalEntityName2\",\"companyRegistrationNumber\":\"12345678\"},\"whatDoesYourBusinessDoAtThisAddress\":{\"activities\":[\"04\",\"01\",\"03\",\"02\",\"07\",\"08\",\"05\",\"06\"]},\"msbServices\":{\"msbServices\":[\"02\",\"03\",\"05\",\"01\",\"04\"]},\"hasChanged\":false,\"lineId\":666666,\"hasAccepted\":true},{\"registeringAgentPremises\":{\"agentPremises\":false},\"yourTradingPremises\":{\"tradingName\":\"OwnBusinessTradingName\",\"postcode\":\"NE99 1ZZ\",\"addressLine1\":\"OwnBusinessAddressLine1\",\"addressLine2\":\"OwnBusinessAddressLine2\",\"addressLine3\":\"OwnBusinessAddressLine3\",\"addressLine4\":\"OwnBusinessAddressLine4\",\"isResidential\":false,\"startDate\":\"2001-01-01\"},\"whatDoesYourBusinessDoAtThisAddress\":{\"activities\":[\"04\",\"01\",\"03\",\"02\",\"07\",\"08\",\"05\",\"06\"]},\"msbServices\":{\"msbServices\":[\"02\",\"03\",\"05\",\"01\",\"04\"]},\"hasChanged\":false,\"lineId\":111111,\"hasAccepted\":true},{\"registeringAgentPremises\":{\"agentPremises\":false},\"yourTradingPremises\":{\"tradingName\":\"OwnBusinessTradingName1\",\"postcode\":\"NE99 1ZZ\",\"addressLine1\":\"OB11AddressLine1\",\"addressLine2\":\"OB1AddressLine2\",\"addressLine3\":\"OB1AddressLine3\",\"addressLine4\":\"OB1AddressLine4\",\"isResidential\":false,\"startDate\":\"2001-01-01\"},\"whatDoesYourBusinessDoAtThisAddress\":{\"activities\":[\"04\",\"01\",\"03\",\"02\",\"07\",\"08\",\"05\",\"06\"]},\"msbServices\":{\"msbServices\":[\"02\",\"03\",\"05\",\"01\",\"04\"]},\"hasChanged\":false,\"lineId\":222222,\"hasAccepted\":true}]"

      // When
      val decryptedBytes = cryptoService.decryptAsBytes(encryptedJsonString)

      // Then
      decryptedBytes.get mustEqual expectedDecryptedJsonString
        .replaceAll("\\\\", "")
        .stripPrefix("\"")
        .stripSuffix("\"")
        .getBytes(UTF_8)
    }

    "provide failure containing exception" when {
      "decryption fails" in {
        // Given
        val encryptedString = "rwiheowr@£$@£WEeiwbr"

        // When
        val result = cryptoService.decryptAsBytes(encryptedString)

        // Then
        result.failure.exception must have message "javax.crypto.IllegalBlockSizeException: Input length must be multiple of 16 when decrypting with padded cipher"
      }
    }
  }
}
