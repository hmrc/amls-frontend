package models.bankdetails

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.StatusConstants


class BankDetailsSpec extends PlaySpec with MockitoSugar {

  val cache = mock[CacheMap]
  val emptyBankDetails: Option[BankDetails] = None

  val accountType = PersonalAccount
  val accountTypePartialModel = BankDetails(Some(accountType), None)
  val accountTypeJson = Json.obj("bankAccountType" -> Json.obj("bankAccountType" -> "01"), "hasChanged" -> false)
  val accountTypeNew = BelongsToBusiness

  val bankAccount = BankAccount("My Account", UKAccount("111111", "11-11-11"))
  val bankAccountPartialModel = BankDetails(None, Some(bankAccount))
  val bankAccountJson = Json.obj("bankAccount" -> Json.obj(
    "accountName" -> "My Account",
    "isUK" -> true,
    "accountNumber" -> "111111",
    "sortCode" -> "11-11-11"),
    "hasChanged" -> false)
  val bankAccountNew = BankAccount("My Account", UKAccount("123456", "78-90-12"))

  val completeModel = BankDetails(Some(accountType), Some(bankAccount))
  val completeJson = Json.obj(
    "bankAccountType" -> Json.obj("bankAccountType" -> "01"),
    "bankAccount" -> Json.obj("accountName" -> "My Account",
      "isUK" -> true,
      "accountNumber" -> "111111",
      "sortCode" -> "11-11-11"),
    "hasChanged" -> false)
  val completeModelChanged = BankDetails(Some(accountType), Some(bankAccount), true)
  val completeJsonChanged = Json.obj(
    "bankAccountType" -> Json.obj("bankAccountType" -> "01"),
    "bankAccount" -> Json.obj("accountName" -> "My Account",
      "isUK" -> true,
      "accountNumber" -> "111111",
      "sortCode" -> "11-11-11"),
    "hasChanged" -> true)


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

    "return a Completed Section when model is complete and has not changed" in {
      val complete = Seq(completeModel)
      val completedSection = Section("bankdetails", Completed, false, controllers.bankdetails.routes.SummaryController.get(true))

      when(cache.getEntry[Seq[BankDetails]](meq("bank-details"))(any())) thenReturn Some(complete)

      BankDetails.section(cache) must be(completedSection)
    }

    "return a Completed Section when model is complete and has changed" in {
      val completeChangedModel = BankDetails(Some(accountType), Some(bankAccount), true)

      val completedSection = Section("bankdetails", Completed, true, controllers.bankdetails.routes.SummaryController.get(true))

      when(cache.getEntry[Seq[BankDetails]](meq("bank-details"))(any())) thenReturn Some(Seq(completeChangedModel))

      BankDetails.section(cache) must be(completedSection)
    }

    "return a Started Section when model is incomplete" in {
      val incomplete = Seq(accountTypePartialModel)
      val startedSection = Section("bankdetails", Started, false, controllers.bankdetails.routes.WhatYouNeedController.get(1))

      when(cache.getEntry[Seq[BankDetails]](meq("bank-details"))(any())) thenReturn Some(incomplete)

      BankDetails.section(cache) must be(startedSection)
    }

    "return a completed Section when model is complete with No bankaccount option selected" in {
      val noBankAcount = Seq(BankDetails(None, None))
      val startedSection = Section("bankdetails", Started, false, controllers.bankdetails.routes.WhatYouNeedController.get(1))

      when(cache.getEntry[Seq[BankDetails]](meq("bank-details"))(any())) thenReturn Some(noBankAcount)

      BankDetails.section(cache) must be(startedSection)
    }

    "Amendment and Variation flow" when {
      "the section is complete with all the bank details being removed" must {
        "successfully redirect to what you need page" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[BankDetails]](meq(BankDetails.key))(any()))
            .thenReturn(Some(Seq(BankDetails(status = Some(StatusConstants.Deleted), hasChanged = true), BankDetails(status = Some(StatusConstants.Deleted), hasChanged = true))))
          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(NotStarted)
          section.call must be(controllers.bankdetails.routes.BankAccountAddController.get(true))
        }
      }

      "the section is complete with one of the bank details object being removed" must {
        "successfully redirect to check your answers page" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[BankDetails]](meq(BankDetails.key))(any()))
            .thenReturn(Some(Seq(BankDetails(status = Some(StatusConstants.Deleted), hasChanged = true), completeModel)))
          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(Completed)
          section.call must be(controllers.bankdetails.routes.SummaryController.get(true))
        }
      }

      "the section is complete with all the bank details unchanged" must {
        "successfully redirect to check your answers page" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[BankDetails]](meq(BankDetails.key))(any()))
            .thenReturn(Some(Seq(completeModel, completeModel)))
          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(false)
          section.status must be(Completed)
          section.call must be(controllers.bankdetails.routes.SummaryController.get(true))
        }
      }

      "the section is complete with all the bank details being modified" must {
        "successfully redirect to check your answers page" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[BankDetails]](meq(BankDetails.key))(any()))
            .thenReturn(Some(Seq(completeModelChanged, completeModelChanged)))
          val section = BankDetails.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(Completed)
          section.call must be(controllers.bankdetails.routes.SummaryController.get(true))
        }
      }
    }
  }


  it when {
    val completeModel = BankDetails(Some(PersonalAccount), Some(BankAccount("ACCOUNTNAME", UKAccount("ACCOUNTNUMBER", "SORTCODE"))))
    val incompleteModel = BankDetails(Some(PersonalAccount), None)

    "the section consistes of just 1 empty Bank details" must {
      "return a result indicating NotStarted" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[BankDetails]](meq(BankDetails.key))(any()))
          .thenReturn(Some(Seq(BankDetails())))

        BankDetails.section(mockCacheMap).status must be(models.registrationprogress.NotStarted)
      }
    }

    "the section consists of a partially complete model followed by a completely empty one" must {


      "return a result indicating partial completeness" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[BankDetails]](meq(BankDetails.key))(any()))
          .thenReturn(Some(Seq(incompleteModel, BankDetails())))

        BankDetails.section(mockCacheMap).status must be(models.registrationprogress.Started)
      }
    }

    "the section consists of a complete model followed by an empty one" must {
      "return a result indicating completeness" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[BankDetails]](meq(BankDetails.key))(any()))
          .thenReturn(Some(Seq(completeModel, BankDetails())))

        BankDetails.section(mockCacheMap).status must be(models.registrationprogress.Completed)
      }
    }
  }

  "anyChanged" must {
    val originalBankDetails = Seq(BankDetails(Some(accountType), Some(bankAccount), false))
    val originalBankDetailsChanged = Seq(BankDetails(Some(accountType), Some(bankAccountNew), true))
    val addedNewBankDetails = Seq(BankDetails(Some(accountType), Some(bankAccount), false), BankDetails(Some(accountType), Some(bankAccountNew), false))

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

  "BankDetails class" when {
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
