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
import models.bankdetails.BankDetails._
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import org.mockito.Matchers.{eq => meq}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import utils.{DependencyMocks, StatusConstants}

class BankDetailsSpec extends PlaySpec with MockitoSugar with CharacterSets with OneAppPerSuite with DependencyMocks with BankDetailsModels {

  val emptyBankDetails: Option[BankDetails] = None

  val accountTypePartialModel = BankDetails(Some(accountType), None)
  val accountTypeNew = BelongsToBusiness

  val bankAccountPartialModel = BankDetails(None, None, Some(bankAccount))

  val bankAccountNew = UKAccount("123456", "00-00-00")

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
    }

  }

  "isComplete" must {
    "return true" when {
      "given complete model" in {
        val bankAccount = UKAccount("123456", "00-00-00")
        val bankDetails = BankDetails(Some(accountType), Some("name"), Some(bankAccount), hasAccepted = true)

        bankDetails.isComplete must be(true)
      }
    }

    "return false" when {
      "given incomplete model" in {
        val bankDetails = BankDetails(Some(accountType), None)

        bankDetails.isComplete must be(false)
      }

      "given empty model" in {
        val bankDetails = BankDetails(None, None, hasAccepted = true)

        bankDetails.isComplete must be(true)
      }
    }

  }

  "Section" must {

    "return a NotStarted Section" when {
      "there is no data at all" in {
        val notStartedSection = Section("bankdetails", NotStarted, false, controllers.bankdetails.routes.BankAccountAddController.get(true))

        mockCacheGetEntry[Seq[BankDetails]](None, BankDetails.key)

        BankDetails.section(mockCacheMap) must be(notStartedSection)
      }
    }

    "return a Completed Section" when {
      "model is complete and has not changed" in {
        val complete = Seq(completeModel)
        val completedSection = Section("bankdetails", Completed, false, controllers.bankdetails.routes.SummaryController.get(true))

        mockCacheGetEntry[Seq[BankDetails]](Some(complete), BankDetails.key)

        BankDetails.section(mockCacheMap) must be(completedSection)
      }

      "model is complete and has changed" in {
        val completeChangedModel = BankDetails(Some(accountType), Some("name"), Some(bankAccount), true, hasAccepted = true)

        val completedSection = Section("bankdetails", Completed, true, controllers.bankdetails.routes.SummaryController.get(true))

        mockCacheGetEntry[Seq[BankDetails]](Some(Seq(completeChangedModel)), BankDetails.key)

        BankDetails.section(mockCacheMap) must be(completedSection)
      }

      "model is complete with No bankaccount option selected" in {
        val noBankAccount = Seq(BankDetails(None, None, None, true, false, None, true))
        val completedSection = Section("bankdetails", Completed, true, controllers.bankdetails.routes.SummaryController.get(true))

        mockCacheGetEntry[Seq[BankDetails]](Some(noBankAccount), BankDetails.key)

        val section = BankDetails.section(mockCacheMap)
        section.hasChanged must be(true)
        section.status must be(Completed)
        BankDetails.section(mockCacheMap) must be(completedSection)
      }
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

    "Amendment and Variation flow" must {

      "redirect to Check Your Answers" when {
        "the section is complete with one of the bank details object being removed" in {

          mockCacheGetEntry[Seq[BankDetails]](Some(Seq(
            BankDetails(status = Some(StatusConstants.Deleted), hasChanged = true, hasAccepted = true), completeModel)),
            BankDetails.key
          )
          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(Completed)
          section.call must be(controllers.bankdetails.routes.SummaryController.get(true))
        }

        "the section is complete with all the bank details unchanged" in {

          mockCacheGetEntry[Seq[BankDetails]](Some(Seq(completeModel, completeModel)), BankDetails.key)

          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(false)
          section.status must be(Completed)
          section.call must be(controllers.bankdetails.routes.SummaryController.get(true))
        }

        "the section is complete with all the bank details being modified" in {

          mockCacheGetEntry[Seq[BankDetails]](Some(Seq(completeModelChanged, completeModelChanged)), BankDetails.key)

          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(Completed)
          section.call must be(controllers.bankdetails.routes.SummaryController.get(true))
        }

      }

      "redirect to What You Need" when {
        "the section is complete with all the bank details being removed" in {

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

  val bankAccount = UKAccount("111111", "00-00-00")
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

}