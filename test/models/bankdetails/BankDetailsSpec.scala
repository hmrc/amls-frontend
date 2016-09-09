package models.bankdetails

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import org.mockito.Mockito._
import org.mockito.Matchers.{any, eq => meq}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap


class BankDetailsSpec extends PlaySpec with MockitoSugar {

  val accountType = PersonalAccount

  "BankDetails with complete model" must {

    val completeJson = Json.obj("bankAccountType" -> "01")
    val completeModel = BankDetails(Some(accountType), None)


    "Serialise as expected" in {
      Json.toJson[BankDetails](completeModel) must be(completeJson)
    }

    "Deserialise as expected" in {
      completeJson.as[BankDetails] must be(completeModel)
    }
  }

  "Bank details with partially complete model" must {
    val accontTypeJson = Json.obj("bankAccountType" -> "01")
    val accountTypeModel = BankDetails(Some(accountType), None)

    "serialise as expected with bankAccountType" in {
      Json.toJson[BankDetails](accountTypeModel) must be(accontTypeJson)
    }

    "Deserialise as expected with bankAccountType" in {
      accontTypeJson.as[BankDetails] must be(accountTypeModel)
    }
  }

  "None" when {

    val initial: Option[BankDetails] = None

    "merged with bankDetails" must {
      "return bank details with correct data set" in {
        val accountTypeNew = BelongsToOtherBusiness
        val result = initial.bankAccountType(accountTypeNew)

        result must be(BankDetails(Some(accountTypeNew), None))

      }
    }
  }

  "BankDetails with data" must {

    val initial = BankDetails(Some(accountType), None)

    "Merge with existing data set" must {
      "return merger result with new data" in {
        val accountTypeNew = BelongsToBusiness
        val result = initial.bankAccountType(accountTypeNew)

        result must be(BankDetails(Some(accountTypeNew), None))
      }
    }
  }

  "BankDetails with only Bank Details" must {

    val bankAccount = BankAccount("My Account", UKAccount("123456", "78-90-12"))
    val bankDetails = BankDetails(None, Some(bankAccount))

    "must Merge with existing data set" must {
      "and return merger result with new data" in {
        val accountTypeNew = BelongsToBusiness
        val result = bankDetails.bankAccountType(accountTypeNew)

        result must be(BankDetails(Some(accountTypeNew), Some(bankAccount)))
      }
    }
  }

  "BankDetails with Bank Account Type and Bank Details" must {

    val bankAccount = BankAccount("My Account", UKAccount("123456", "78-90-12"))
    val bankDetails = BankDetails(Some(accountType), Some(bankAccount))

    "Merge with existing data set" must {
      "return merger result with new data" in {
        val accountTypeNew = BelongsToBusiness
        val result = bankDetails.bankAccountType(accountTypeNew)

        result must be(BankDetails(Some(accountTypeNew), Some(bankAccount)))
      }
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
    val cache = mock[CacheMap]

    "return a NotStarted Section when model is empty" in {
      val notStartedSection = Section("bankdetails", NotStarted, false, controllers.bankdetails.routes.BankAccountAddController.get(true))

      when(cache.getEntry[Seq[BankDetails]](meq("bank-details"))(any())) thenReturn None

      BankDetails.section(cache) must be(notStartedSection)

    }

    "return a Completed Section when model is complete" in {

      val bankAccount = BankAccount("My Account", UKAccount("123456", "78-90-12"))
      val bankDetails = BankDetails(Some(accountType), Some(bankAccount))
      val complete = Seq(bankDetails)
      val completedSection = Section("bankdetails", Completed, false, controllers.bankdetails.routes.SummaryController.get(true))


      when(cache.getEntry[Seq[BankDetails]](meq("bank-details"))(any())) thenReturn Some(complete)

      BankDetails.section(cache) must be(completedSection)

    }

    "return a Started Section when model is incomplete" in {

      val bankDetails = BankDetails(Some(accountType), None)
      val incompleteTcsp = Seq(bankDetails)
      val startedSection = Section("bankdetails", Started, false, controllers.bankdetails.routes.WhatYouNeedController.get(1))

      when(cache.getEntry[Seq[BankDetails]](meq("bank-details"))(any())) thenReturn Some(incompleteTcsp)

      BankDetails.section(cache) must be(startedSection)
    }
  }
}
