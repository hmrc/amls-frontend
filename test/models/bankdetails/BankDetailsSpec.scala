/*
 * Copyright 2023 HM Revenue & Customs
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

import models.CharacterSets
import models.bankdetails.BankDetails._
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import play.api.libs.json.Json
import utils.{AmlsSpec, DependencyMocks, StatusConstants}

class BankDetailsSpec extends AmlsSpec with CharacterSets with DependencyMocks with BankDetailsModels {

  val emptyBankDetails: Option[BankDetails] = None

  val accountTypePartialModel = BankDetails(Some(accountType), None)
  val accountTypeNew = BelongsToBusiness

  val bankAccountPartialModel = BankDetails(None, None, Some(bankAccount))

  val bankAccountNew = BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount("123456", "11-11-11")))

  val incompleteModel = BankDetails(Some(accountType), None)

  "BankDetails" must {
    "serialise" when {
      "given complete model" in {
        Json.toJson[BankDetails](completeModel) must be(completeJson)
      }
      "has the hasChanged flag set as true" in {
        Json.toJson[BankDetails](completeModelChanged)(BankDetails.writes) must be(completeJsonChanged)
        Json.toJson[BankDetails](completeModelChanged) must be(completeJsonChanged)
      }
      "partially complete model" which {
        "contains only accountType" in {
          Json.toJson[BankDetails](accountTypePartialModel) must be(accountTypeJson)
        }
        "contains only bankAccount" in {
          Json.toJson[BankDetails](bankAccountPartialModel) must be(bankAccountJson)
        }
      }
    }

    "deserialise" when {
      "given complete model" in {
        completeJson.as[BankDetails] must be(completeModel)
      }
      "hasChanged field is missing from the Json" in {
        (completeJson - "hasChanged").as[BankDetails] must be(completeModel)
      }
      "has the hasChanged flag set as true" in {
        completeJsonChanged.as[BankDetails] must be(completeModelChanged)
      }
      "partially complete model" which {
        "contains only accountType" in {
          accountTypeJson.as[BankDetails] must be(accountTypePartialModel)
        }
        "contains only bankAccount" in {
          bankAccountJson.as[BankDetails] must be(bankAccountPartialModel)
        }
      }
      "given old format" in {
        oldCompleteJson.as[BankDetails] must be(completeModel)
      }
    }

  }

  "isComplete" must {
    "return true" when {
      "given complete model" in {
        val bankAccount = BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount("123456", "00-00-00")))
        val bankDetails = BankDetails(Some(accountType), Some("name"), Some( bankAccount), hasAccepted = true)
        bankDetails.isComplete must be(true)
      }

      "empty model" in {
        val bankDetails = BankDetails()
        bankDetails.isComplete must be(true)
      }

      "given empty model with accepted" in {
        val bankDetails = BankDetails(None, None, hasAccepted = true)
        bankDetails.isComplete must be(true)
      }
    }

    "return false" when {
      "given incomplete model" in {
        val bankDetails = BankDetails(Some(accountType), None)
        bankDetails.isComplete must be(false)
      }
    }

    "return false" when {
      "given incomplete UK BankAccount" in {
        val bankAccount = BankAccount(Some(BankAccountIsUk(true)), None, None)
        val bankDetails = BankDetails(Some(accountType), Some("name"), Some( bankAccount), hasAccepted = true)
        bankDetails.isComplete must be(false)
      }

      "given incomplete Non UK BankAccount without IBAN" in {
        val bankAccount = BankAccount(Some(BankAccountIsUk(false)), Some(BankAccountHasIban(false)), None)
        val bankDetails = BankDetails(Some(accountType), Some("name"), Some( bankAccount), hasAccepted = true)
        bankDetails.isComplete must be(false)
      }

      "given incomplete Non UK BankAccount without IBAN answer" in {
        val bankAccount = BankAccount(Some(BankAccountIsUk(false)), None, None)
        val bankDetails = BankDetails(Some(accountType), Some("name"), Some( bankAccount), hasAccepted = true)
        bankDetails.isComplete must be(false)
      }

      "given incomplete UK BankAccount with IBAN" in {
        val bankAccount = BankAccount(Some(BankAccountIsUk(false)), Some(BankAccountHasIban(true)), None)
        val bankDetails = BankDetails(Some(accountType), Some("name"), Some( bankAccount), hasAccepted = true)
        bankDetails.isComplete must be(false)
      }
    }
  }

  "getBankAccountDescription" must {
    "return the correct uk account descriptions" in {

      val bankDetailsPersonal = BankDetails(Some(PersonalAccount), None, Some(bankAccount))
      val bankDetailsBelongstoBusiness = bankDetailsPersonal.copy(bankAccountType = Some(BelongsToBusiness))
      val bankDetailsBelongstoOtherBusiness = bankDetailsPersonal.copy(bankAccountType = Some(BelongsToOtherBusiness))
      val bankDetailsNoBankAccountUsed = bankDetailsPersonal.copy(bankAccountType = Some(NoBankAccountUsed))

      BankDetails.getBankAccountDescription(bankDetailsPersonal) must be(messages("bankdetails.accounttype.uk.lbl.01"))
      BankDetails.getBankAccountDescription(bankDetailsBelongstoBusiness) must be(messages("bankdetails.accounttype.uk.lbl.02"))
      BankDetails.getBankAccountDescription(bankDetailsBelongstoOtherBusiness) must be(messages("bankdetails.accounttype.uk.lbl.03"))
      BankDetails.getBankAccountDescription(bankDetailsNoBankAccountUsed) must be(messages("bankdetails.accounttype.uk.lbl.04"))
   }
  "return the correct non-uk account descriptions" in {

    val bankDetailsPersonal = BankDetails(Some(PersonalAccount), None, Some(BankAccount(Some(BankAccountIsUk(false)), Some(BankAccountHasIban(false)), Some(NonUKAccountNumber("ABCDEFGHIJKLMNOPQRSTUVWXYZABCD")))))
    val bankDetailsBelongstoBusiness = bankDetailsPersonal.copy(bankAccountType = Some(BelongsToBusiness))
    val bankDetailsBelongstoOtherBusiness = bankDetailsPersonal.copy(bankAccountType = Some(BelongsToOtherBusiness))
    val bankDetailsNoBankAccountUsed = bankDetailsPersonal.copy(bankAccountType = Some(NoBankAccountUsed))

    BankDetails.getBankAccountDescription(bankDetailsPersonal) must be(messages("bankdetails.accounttype.nonuk.lbl.01"))
    BankDetails.getBankAccountDescription(bankDetailsBelongstoBusiness) must be(messages("bankdetails.accounttype.nonuk.lbl.02"))
    BankDetails.getBankAccountDescription(bankDetailsBelongstoOtherBusiness) must be(messages("bankdetails.accounttype.nonuk.lbl.03"))
    BankDetails.getBankAccountDescription(bankDetailsNoBankAccountUsed) must be(messages("bankdetails.accounttype.nonuk.lbl.04"))
 }

  "return the correct description wheere there are no account numbers present" in {

    val bankDetailsPersonal = BankDetails(Some(PersonalAccount), None, None)
    val bankDetailsBelongstoBusiness = bankDetailsPersonal.copy(bankAccountType = Some(BelongsToBusiness))
    val bankDetailsBelongstoOtherBusiness = bankDetailsPersonal.copy(bankAccountType = Some(BelongsToOtherBusiness))
    val bankDetailsNoBankAccountUsed = bankDetailsPersonal.copy(bankAccountType = Some(NoBankAccountUsed))

    BankDetails.getBankAccountDescription(bankDetailsPersonal) must be(messages("bankdetails.accounttype.lbl.01"))
    BankDetails.getBankAccountDescription(bankDetailsBelongstoBusiness) must be(messages("bankdetails.accounttype.lbl.02"))
    BankDetails.getBankAccountDescription(bankDetailsBelongstoOtherBusiness) must be(messages("bankdetails.accounttype.lbl.03"))
    BankDetails.getBankAccountDescription(bankDetailsNoBankAccountUsed) must be(messages("bankdetails.accounttype.lbl.04"))
 }
}

  "Section" must {

    "return a NotStarted Section" when {
      "there is no data at all" in {
        val notStartedSection = Section("bankdetails", NotStarted, false, controllers.bankdetails.routes.WhatYouNeedController.get)

        mockCacheGetEntry[Seq[BankDetails]](None, BankDetails.key)

        BankDetails.section(mockCacheMap) must be(notStartedSection)
      }
    }

    "return a Completed Section" when {
      "model is complete and has not changed" in {
        val complete = Seq(completeModel)
        val completedSection = Section("bankdetails", Completed, false, controllers.bankdetails.routes.YourBankAccountsController.get)

        mockCacheGetEntry[Seq[BankDetails]](Some(complete), BankDetails.key)

        BankDetails.section(mockCacheMap) must be(completedSection)
      }

      "model is complete and has changed" in {
        val completeChangedModel = BankDetails(Some(accountType), Some("name"), Some(bankAccount), true, hasAccepted = true)

        val completedSection = Section("bankdetails", Completed, true, controllers.bankdetails.routes.YourBankAccountsController.get)

        mockCacheGetEntry[Seq[BankDetails]](Some(Seq(completeChangedModel)), BankDetails.key)

        BankDetails.section(mockCacheMap) must be(completedSection)
      }

      "model is complete with No bankaccount option selected" in {
        val completedSection = Section("bankdetails", Completed, false, controllers.bankdetails.routes.YourBankAccountsController.get)

        mockCacheGetEntry[Seq[BankDetails]](Some(Seq.empty), BankDetails.key)

        val section = BankDetails.section(mockCacheMap)
        section.hasChanged must be(false)
        section.status must be(Completed)
        BankDetails.section(mockCacheMap) must be(completedSection)
      }

      "model is complete with only deleted bankaccounts that have not changed" in {
        val deleted = Seq(completeModel.copy(status = Some(StatusConstants.Deleted)))
        val completedSection = Section("bankdetails", Completed, false, controllers.bankdetails.routes.YourBankAccountsController.get)

        mockCacheGetEntry[Seq[BankDetails]](Some(deleted), BankDetails.key)

        val section = BankDetails.section(mockCacheMap)
        section.hasChanged must be(false)
        section.status must be(Completed)
        BankDetails.section(mockCacheMap) must be(completedSection)
      }

      "model is complete with only deleted bankaccounts that have changed" in {
        val deleted = Seq(completeModel.copy(status = Some(StatusConstants.Deleted), hasChanged = true, hasAccepted = true))
        val completedSection = Section("bankdetails", Completed, true, controllers.bankdetails.routes.YourBankAccountsController.get)

        mockCacheGetEntry[Seq[BankDetails]](Some(deleted), BankDetails.key)

        val section = BankDetails.section(mockCacheMap)
        section.hasChanged must be(true)
        section.status must be(Completed)
        BankDetails.section(mockCacheMap) must be(completedSection)
      }
    }

    "return a Started Section when model is incomplete" in {
      val incomplete = Seq(accountTypePartialModel)
      val startedSection = Section("bankdetails", Started, false, controllers.bankdetails.routes.YourBankAccountsController.get)

      mockCacheGetEntry[Seq[BankDetails]](Some(incomplete), BankDetails.key)

      BankDetails.section(mockCacheMap) must be(startedSection)
    }

    "set hasChanged and hasAccepted when updating bankAccountType set to None" in {
      val result = completeModel.bankAccountType(None)

      result.hasAccepted mustBe false
      result.hasChanged mustBe true
      result.bankAccountType mustBe None
    }

    "Amendment and Variation flow" must {

      "redirect to Your Bank Accounts page" when {
        "the section is complete with one of the bank details object being removed" in {

          mockCacheGetEntry[Seq[BankDetails]](Some(Seq(
            BankDetails(status = Some(StatusConstants.Deleted), hasChanged = true, hasAccepted = true), completeModel)),
            BankDetails.key
          )
          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(Completed)
          section.call must be(controllers.bankdetails.routes.YourBankAccountsController.get)
        }

        "the section is complete with all the bank details unchanged" in {

          mockCacheGetEntry[Seq[BankDetails]](Some(Seq(completeModel, completeModel)), BankDetails.key)

          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(false)
          section.status must be(Completed)
          section.call must be(controllers.bankdetails.routes.YourBankAccountsController.get)
        }

        "the section is complete with all the bank details being modified" in {

          mockCacheGetEntry[Seq[BankDetails]](Some(Seq(completeModelChanged, completeModelChanged)), BankDetails.key)

          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(Completed)
          section.call must be(controllers.bankdetails.routes.YourBankAccountsController.get)
        }

      }

      "redirect to What You Need" when {
        "there is no bank account data" in {

          mockCacheGetEntry(None,
            BankDetails.key)

          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(false)
          section.status must be(NotStarted)
          section.call must be(controllers.bankdetails.routes.WhatYouNeedController.get)
        }
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
          val res = completeModel.bankAccount(Some(bankAccount))
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.bankAccount(Some(bankAccountNew))
          res.hasChanged must be(true)
          res.bankAccount must be(Some(bankAccountNew))
        }
      }
    }
  }

}

trait BankDetailsModels {

  val accountType = PersonalAccount
  val accountTypeJson = Json.obj(
    "bankAccountType" -> Json.obj(
      "bankAccountType" -> "01"
    ),
    "hasChanged" -> false,
    "refreshedFromServer" -> false,
    "hasAccepted" -> false
  )

  val bankAccount = BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount("111111", "00-00-00")))
  val bankAccountJson = Json.obj(
    "bankAccount" -> Json.obj(
      "isUK" -> true,
      "accountNumber" -> "111111",
      "sortCode" -> "00-00-00"
    ),
    "hasChanged" -> false,
    "refreshedFromServer" -> false,
    "hasAccepted" -> false
  )

  val completeModel = BankDetails(Some(accountType), Some("bankName"), Some(bankAccount), hasAccepted = true)
  val completeJson = Json.obj(
    "bankAccountType" -> Json.obj(
      "bankAccountType" -> "01"),
    "accountName" -> "bankName",
    "bankAccount" -> Json.obj(
      "isUK" -> true,
      "accountNumber" -> "111111",
      "sortCode" -> "00-00-00"),
    "hasChanged" -> false,
    "refreshedFromServer" -> false,
    "hasAccepted" -> true)

  val completeModelChanged = BankDetails(Some(accountType), Some("anotherName"), Some(bankAccount), true, hasAccepted = true)
  val completeJsonChanged = Json.obj(
    "bankAccountType" -> Json.obj(
      "bankAccountType" -> "01"),
    "accountName" -> "anotherName",
    "bankAccount" -> Json.obj(
      "isUK" -> true,
      "accountNumber" -> "111111",
      "sortCode" -> "00-00-00"),
    "hasChanged" -> true,
    "refreshedFromServer" -> false,
    "hasAccepted" -> true)

  val oldCompleteJson = Json.obj(
    "bankAccountType" -> Json.obj(
      "bankAccountType" -> "01"),
    "bankAccount" -> Json.obj(
      "accountName" -> "bankName",
      "isUK" -> true,
      "accountNumber" -> "111111",
      "sortCode" -> "00-00-00"),
    "hasChanged" -> false,
    "refreshedFromServer" -> false,
    "hasAccepted" -> true)

}