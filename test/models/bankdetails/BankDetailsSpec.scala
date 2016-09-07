package models.bankdetails

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap


class BankDetailsSpec extends PlaySpec with MockitoSugar {

  val cache = mock[CacheMap]
  val emptyBankDetails: Option[BankDetails] = None

  val accountType = PersonalAccount
  val accountTypePartialModel = BankDetails(Some(accountType), None)
  val accountTypeJson = Json.obj("bankAccountType" -> "01")
  val accountTypeNew = BelongsToBusiness

  val bankAccount = BankAccount("My Account", UKAccount("111111", "11-11-11"))
  val bankAccountPartialModel = BankDetails(None, Some(bankAccount))
  val bankAccountJson = Json.obj(
    "accountName" -> "My Account",
    "isUK" -> true,
    "accountNumber" -> "111111",
    "sortCode" -> "11-11-11")
  val bankAccountNew = BankAccount("My Account", UKAccount("123456", "78-90-12"))

  val completeModel = BankDetails(Some(accountType), Some(bankAccount))
  val completeJson = Json.obj(
    "bankAccountType" -> "01",
    "accountName" -> "My Account",
    "isUK" -> true,
    "accountNumber" -> "111111",
    "sortCode" -> "11-11-11")


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
      val bankAccount = BankAccount("My Account", UKAccount("123456", "78-90-12"))
      val bankDetails = BankDetails(Some(accountType), Some(bankAccount))

      bankDetails.isComplete must be(true)
    }

    "return false when BankDetails contains incomplete data" in {
      val bankDetails = BankDetails(Some(accountType), None)

      bankDetails.isComplete must be(false)
    }

    "return false when BankDetails no data" in {
      val bankDetails = BankDetails(None, None)

      bankDetails.isComplete must be(true)
    }
  }

  "Section" must {
    "return a NotStarted Section when there is no data at all" in {
      val notStartedSection = Section("bankdetails", NotStarted, false, controllers.bankdetails.routes.BankAccountAddController.get(true))

      when(cache.getEntry[Seq[BankDetails]](meq("bank-details"))(any())) thenReturn None

      BankDetails.section(cache) must be(notStartedSection)
    }
    "return a Completed Section when model is complete" in {
      val complete = Seq(completeModel)
      val completedSection = Section("bankdetails", Completed, false, controllers.bankdetails.routes.SummaryController.get(true))

      when(cache.getEntry[Seq[BankDetails]](meq("bank-details"))(any())) thenReturn Some(complete)

      BankDetails.section(cache) must be(completedSection)
    }
    "return a Completed Section when model is empty (no bank details)" in {
      val complete = Seq()
      val completedSection = Section("bankdetails", Completed, false, controllers.bankdetails.routes.SummaryController.get(true))

      when(cache.getEntry[Seq[BankDetails]](meq("bank-details"))(any())) thenReturn Some(complete)

      BankDetails.section(cache) must be(completedSection)
    }
    "return a Started Section when model is incomplete" in {
      val incomplete = Seq(accountTypePartialModel)
      val startedSection = Section("bankdetails", Started, false, controllers.bankdetails.routes.WhatYouNeedController.get(1))

      when(cache.getEntry[Seq[BankDetails]](meq("bank-details"))(any())) thenReturn Some(incomplete)

      BankDetails.section(cache) must be(startedSection)
    }
  }

  "anyChanged" must {
    "return false" when {
      "no BankDetails in the sequence have changed" in {
        val res = BankDetails.anyChanged(Seq(BankDetails(Some(accountType), Some(bankAccount),false)))
        res must be(false)
      }
    }
    "return true" when {
      "at least one BankDetails in the sequence has changed" in {
        val res = BankDetails.anyChanged(Seq(BankDetails(Some(accountType), Some(bankAccount),true)))
        res must be(true)
      }
    }
  }

  "BankDetails class" when {
    "bankAccountType value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.bankAccountType(accountType)
          res must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.bankAccountType(accountTypeNew)
          res.hasChanged must be(true)
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
