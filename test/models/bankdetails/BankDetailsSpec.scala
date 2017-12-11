/*
 * Copyright 2017 HM Revenue & Customs
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

package models.bankdetails

import jto.validation._
import models.CharacterSets
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{DependencyMocks, StatusConstants}
import models.bankdetails.BankDetails._
import play.api.test.FakeApplication

class BankDetailsSpec extends PlaySpec with MockitoSugar with CharacterSets with OneAppPerSuite with DependencyMocks {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.has-accepted" -> true))

  val emptyBankDetails: Option[BankDetails] = None

  val accountType = PersonalAccount
  val accountTypePartialModel = BankDetails(Some(accountType), None)
  val accountTypeJson = Json.obj("bankAccountType" -> Json.obj("bankAccountType" -> "01"), "hasChanged" -> false, "refreshedFromServer" -> false, "hasAccepted" -> false)
  val accountTypeNew = BelongsToBusiness

  val bankAccount = BankAccount("My Account", UKAccount("111111", "00-00-00"))
  val bankAccountPartialModel = BankDetails(None, None, Some(bankAccount))
  val bankAccountJson = Json.obj("bankAccount" -> Json.obj(
    "accountName" -> "My Account",
    "isUK" -> true,
    "accountNumber" -> "111111",
    "sortCode" -> "00-00-00"),
    "hasChanged" -> false,
    "refreshedFromServer" -> false,
    "hasAccepted" -> false)

  val bankAccountNew = BankAccount("My Account", UKAccount("123456", "00-00-00"))

  val completeModel = BankDetails(Some(accountType), Some("bankName"), Some(bankAccount), hasAccepted = true)
  val incompleteModel = BankDetails(Some(accountType), None)
  val completeJson = Json.obj(
    "bankAccountType" -> Json.obj("bankAccountType" -> "01"),
    "accountName" -> "bankName",
    "bankAccount" -> Json.obj("accountName" -> "My Account",
      "isUK" -> true,
      "accountNumber" -> "111111",
      "sortCode" -> "00-00-00"),
    "hasChanged" -> false,
    "refreshedFromServer" -> false,
    "hasAccepted" -> true)
  val completeModelChanged = BankDetails(Some(accountType), Some("anotherName"), Some(bankAccount), true, hasAccepted = true)
  val completeJsonChanged = Json.obj(
    "bankAccountType" -> Json.obj("bankAccountType" -> "01"),
    "accountName" -> "anotherName",
    "bankAccount" -> Json.obj("accountName" -> "My Account",
      "isUK" -> true,
      "accountNumber" -> "111111",
      "sortCode" -> "00-00-00"),
    "hasChanged" -> true,
    "refreshedFromServer" -> false,
    "hasAccepted" -> true)

  "BankDetails with complete model" must {
    "Serialise as expected" in {
      Json.toJson[BankDetails](completeModel) must be(completeJson)
    }
    "deserialise as expected" in {
      completeJson.as[BankDetails] must be(completeModel)
    }
    "deserialise correctly when hasChanged field is missing from the Json" in {
      (completeJson - "hasChanged").as[BankDetails] must
        be(completeModel)
    }
  }

  "BankDetails with complete model which has the hasChanged flag set as true" must {
    "Serialise as expected" in {
      Json.toJson[BankDetails](completeModelChanged)(BankDetails.writes) must be(completeJsonChanged)
      Json.toJson[BankDetails](completeModelChanged) must be(completeJsonChanged)
    }
    "deserialise as expected" in {
      completeJsonChanged.as[BankDetails] must be(completeModelChanged)
    }
  }

  "Bank details with partially complete model containing only accountType" must {
    "serialise as expected" in {
      Json.toJson[BankDetails](accountTypePartialModel) must be(accountTypeJson)
    }
    "Deserialise as expected" in {
      accountTypeJson.as[BankDetails] must be(accountTypePartialModel)
    }
  }

  "Bank details with partially complete model containing only bankAccount" must {
    "serialise as expected" in {
      Json.toJson[BankDetails](bankAccountPartialModel) must be(bankAccountJson)
    }
    "Deserialise as expected" in {
      bankAccountJson.as[BankDetails] must be(bankAccountPartialModel)
    }
  }

  "isComplete" must {
    "return true when BankDetails contains complete data" in {
      val bankAccount = BankAccount("My Account", UKAccount("123456", "00-00-00"))
      val bankDetails = BankDetails(Some(accountType), Some("name"), Some(bankAccount), hasAccepted = true)

      bankDetails.isComplete must be(true)
    }

    "return false when BankDetails contains incomplete data" in {
      val bankDetails = BankDetails(Some(accountType), None)

      bankDetails.isComplete must be(false)
    }

    "return false when BankDetails no data" in {
      val bankDetails = BankDetails(None, None, hasAccepted = true)

      bankDetails.isComplete must be(true)
    }
  }

  "Section" must {

    "return a NotStarted Section when there is no data at all" in {
      val notStartedSection = Section("bankdetails", NotStarted, false, controllers.bankdetails.routes.BankAccountAddController.get(true))

      mockCacheGetEntry[Seq[BankDetails]](None, BankDetails.key)

      BankDetails.section(mockCacheMap) must be(notStartedSection)
    }

    "return a Completed Section when model is complete and has not changed" in {
      val complete = Seq(completeModel)
      val completedSection = Section("bankdetails", Completed, false, controllers.bankdetails.routes.SummaryController.get(true))

      mockCacheGetEntry[Seq[BankDetails]](Some(complete), BankDetails.key)

      BankDetails.section(mockCacheMap) must be(completedSection)
    }

    "return a Completed Section when model is complete and has changed" in {
      val completeChangedModel = BankDetails(Some(accountType), Some("name"), Some(bankAccount), true, hasAccepted = true)

      val completedSection = Section("bankdetails", Completed, true, controllers.bankdetails.routes.SummaryController.get(true))

      mockCacheGetEntry[Seq[BankDetails]](Some(Seq(completeChangedModel)), BankDetails.key)

      BankDetails.section(mockCacheMap) must be(completedSection)
    }

    "return a completed Section when model is complete with No bankaccount option selected" in {
      val noBankAccount = Seq(BankDetails(None, None, None, true, false, None, true))
      val completedSection = Section("bankdetails", Completed, true, controllers.bankdetails.routes.SummaryController.get(true))

      mockCacheGetEntry[Seq[BankDetails]](Some(noBankAccount), BankDetails.key)

      val section = BankDetails.section(mockCacheMap)
      section.hasChanged must be(true)
      section.status must be(Completed)
      BankDetails.section(mockCacheMap) must be(completedSection)
    }

    "return a Started Section when model is incomplete" in {
      val incomplete = Seq(accountTypePartialModel)
      val startedSection = Section("bankdetails", Started, false, controllers.bankdetails.routes.WhatYouNeedController.get(1))

      mockCacheGetEntry[Seq[BankDetails]](Some(incomplete), BankDetails.key)

      BankDetails.section(mockCacheMap) must be(startedSection)
    }

    "return a result indicating NotStarted" when {
      "the section consists of just 1 empty Bank details" in {

        mockCacheGetEntry[Seq[BankDetails]](Some(Seq(BankDetails())), BankDetails.key)

        BankDetails.section(mockCacheMap).status must be(models.registrationprogress.NotStarted)
      }
    }

    "return a result indicating partial completeness" when {
      "the section consists of a partially complete model followed by a completely empty one" in {

        mockCacheGetEntry[Seq[BankDetails]](Some(Seq(incompleteModel, BankDetails())), BankDetails.key)

        BankDetails.section(mockCacheMap).status must be(models.registrationprogress.Started)
      }
    }

    "return a result indicating completeness" when {
      "the section consists of a complete model followed by an empty one" in {

        mockCacheGetEntry[Seq[BankDetails]](Some(Seq(completeModel, BankDetails(hasAccepted = true))), BankDetails.key)

        BankDetails.section(mockCacheMap).status must be(models.registrationprogress.Completed)
      }
    }

    "return the correct index of the section" when {
      "the section has a completed model, an empty one and an incomplete one" in {

        mockCacheGetEntry[Seq[BankDetails]](Some(Seq(completeModel, BankDetails(), incompleteModel)), BankDetails.key)

        BankDetails.section(mockCacheMap).call.url must be(controllers.bankdetails.routes.WhatYouNeedController.get(2).url)
      }
    }
    
    "Amendment and Variation flow" when {
      "the section is complete with all the bank details being removed" must {
        "successfully redirect to what you need page" in {


          mockCacheGetEntry(Some(Seq(
            BankDetails(status = Some(StatusConstants.Deleted), hasChanged = true),
            BankDetails(status = Some(StatusConstants.Deleted), hasChanged = true))),
            BankDetails.key)

          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(NotStarted)
          section.call must be(controllers.bankdetails.routes.BankAccountAddController.get(true))
        }
      }

      "the section is complete with one of the bank details object being removed" must {
        "successfully redirect to check your answers page" in {

          mockCacheGetEntry[Seq[BankDetails]](Some(Seq(
            BankDetails(status = Some(StatusConstants.Deleted), hasChanged = true, hasAccepted = true), completeModel)),
            BankDetails.key
          )
          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(Completed)
          section.call must be(controllers.bankdetails.routes.SummaryController.get(true))
        }
      }

      "the section is complete with all the bank details unchanged" must {
        "successfully redirect to check your answers page" in {

          mockCacheGetEntry[Seq[BankDetails]](Some(Seq(completeModel, completeModel)), BankDetails.key)

          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(false)
          section.status must be(Completed)
          section.call must be(controllers.bankdetails.routes.SummaryController.get(true))
        }
      }

      "the section is complete with all the bank details being modified" must {
        "successfully redirect to check your answers page" in {

          mockCacheGetEntry[Seq[BankDetails]](Some(Seq(completeModelChanged, completeModelChanged)), BankDetails.key)

          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(Completed)
          section.call must be(controllers.bankdetails.routes.SummaryController.get(true))
        }
      }

      "exclude Nobank account and deleted bank accounts before sending to ETMP" in {

        val completeModel = BankDetails(Some(accountType), None, Some(bankAccount), status = Some(StatusConstants.Deleted))
        val completeModelChanged = BankDetails(Some(accountType), None, Some(bankAccount), true)
        val NoBankAccount = BankDetails(None,None,None, true)

        val bankAccts = Seq(completeModel, completeModelChanged, NoBankAccount)
        val bankDtls = bankAccts.filterNot(x => x.status.contains(StatusConstants.Deleted) || x.bankAccountType.isEmpty)
        val test = bankDtls.nonEmpty match {
          case true => Some(bankDtls)
          case false => Some(Seq.empty)
        }
        test must be(Some(Seq(completeModelChanged)))
      }
    }
  }

  "anyChanged" must {
    val originalBankDetails = Seq(BankDetails(Some(accountType), None, Some(bankAccount), false))
    val originalBankDetailsChanged = Seq(BankDetails(Some(accountType), None, Some(bankAccountNew), true))

    "return false" when {
      "no BankDetails within the sequence have changed" in {
        val res = BankDetails.anyChanged(originalBankDetails)
        res must be(false)
      }
    }
    "return true" when {
      "at least one BankDetails within the sequence has changed" in {
        val res = BankDetails.anyChanged(originalBankDetailsChanged)
        res must be(true)
      }
    }
  }

  it when {
    "bankAccountType value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.bankAccountType(Some(accountType))
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.bankAccountType(Some(accountTypeNew))
          res.hasChanged must be(true)
          BankDetails.anyChanged(Seq(res)) must be(true)
          res.bankAccountType must be(Some(accountTypeNew))
        }
      }
    }

    "bankAccount value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.bankAccount(bankAccount)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.bankAccount(bankAccountNew)
          res.hasChanged must be(true)
          res.bankAccount must be(Some(bankAccountNew))
        }
      }
    }
  }

  "ibanType" must {
    "validate IBAN supplied " in {
      Account.ibanType.validate("IBAN_0000000000000") must be(Valid("IBAN_0000000000000"))
    }

    "fail validation if IBAN is longer than the permissible length" in {
      Account.ibanType.validate("12345678901234567890123456789012345678901234567890") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.iban")))))
    }

    "fail validation if IBAN contains invalid characters" in {
      Account.ibanType.validate("ab{}kfg  ") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.iban")))))
    }

    "fail validation if IBAN contains only whitespace" in {
      Account.ibanType.validate("    ") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.iban")))))
    }
  }

  "nonUKBankAccountNumberType" must {
    "validate Non UK Account supplied " in {
      Account.nonUKBankAccountNumberType.validate("IND00000000000000") must be(Valid("IND00000000000000"))
    }

    "fail validation if Non UK Account is longer than the permissible length" in {
      Account.nonUKBankAccountNumberType.validate("12345678901234567890123456789012345678901234567890") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.account")))))
    }

    "fail validation if Non UK Account no contains invalid characters" in {
      Account.nonUKBankAccountNumberType.validate("ab{}kfg  ") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.account")))))
    }

    "fail validation if Non UK Account no contains only whitespace" in {
      Account.nonUKBankAccountNumberType.validate("    ") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.account")))))
    }
  }

  "ukBankAccountNumberType" must {

    "validate when 8 digits are supplied " in {
      Account.ukBankAccountNumberType.validate("00000000") must be(Valid("00000000"))
    }

    "fail validation when less than 8 characters are supplied" in {
      Account.ukBankAccountNumberType.validate("123456") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.accountnumber")))))
    }

    "fail validation when more than 8 characters are supplied" in {
      Account.ukBankAccountNumberType.validate("1234567890") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.max.length.bankdetails.accountnumber")))))
    }
  }

  "sortCodeType" must {

    "validate when 6 digits are supplied without - " in {
      Account.sortCodeType.validate("000000") must be(Valid("000000"))
    }

    "fail validation when more than 6 digits are supplied without - " in {
      Account.sortCodeType.validate("87654321") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.sortcode")))))
    }

    "fail when 8 non digits are supplied with - " in {
      Account.sortCodeType.validate("ab-cd-ef") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.sortcode")))))
    }

    "pass validation when dashes are used to seperate number groups" in {
      Account.sortCodeType.validate("65-43-21") must be(Valid("654321"))
    }
    "pass validation when spaces are used to seperate number groups" in {
      Account.sortCodeType.validate("65 43 21") must be(Valid("654321"))
    }

    "fail validation for sort code with any other pattern" in {
      Account.sortCodeType.validate("8712341241431243124124654321") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.sortcode"))))
      )
    }
  }

  "accountNameType" must {

    "be mandatory" in {
      BankAccount.accountNameType.validate("") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.bankdetails.accountname")))))
    }

    "accept all characters from the allowed set" in {
      BankAccount.accountNameType.validate(digits.mkString("")) must be(Valid(digits.mkString("")))
      BankAccount.accountNameType.validate(alphaUpper.mkString("")) must be(Valid(alphaUpper.mkString("")))
      BankAccount.accountNameType.validate(alphaLower.mkString("")) must be(Valid(alphaLower.mkString("")))
      BankAccount.accountNameType.validate(extendedAlphaUpper.mkString("")) must be(Valid(extendedAlphaUpper.mkString("")))
      BankAccount.accountNameType.validate(extendedAlphaLower.mkString("")) must be(Valid(extendedAlphaLower.mkString("")))
      BankAccount.accountNameType.validate(symbols1.mkString("")) must be(Valid(symbols1.mkString("")))
      BankAccount.accountNameType.validate(symbols2.mkString("")) must be(Valid(symbols2.mkString("")))
      BankAccount.accountNameType.validate(symbols6.mkString("")) must be(Valid(symbols6.mkString("")))
    }

    "be not more than 40 characters" in {
      BankAccount.accountNameType.validate("This name is definitely longer than 10 characters." * 17) must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.accountname"))))
      )
    }

    "not allow characters from other sets" in {
      BankAccount.accountNameType.validate(symbols5.mkString("")) must be (
        Invalid(Seq(Path -> Seq(ValidationError("err.text.validation"))))
      )
    }
  }

}
