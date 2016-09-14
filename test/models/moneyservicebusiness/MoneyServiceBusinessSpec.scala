package models.moneyservicebusiness

import models.Country
import models.businessmatching.{ChequeCashingScrapMetal, MsbServices, BusinessMatching}
import models.registrationprogress.{Started, Completed, NotStarted, Section}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap
import org.mockito.Mockito._
import play.api.mvc.Call

class MoneyServiceBusinessSpec extends PlaySpec with MockitoSugar with MoneyServiceBusinessTestData {

  "MoneyServiceBusiness" should {

    "have an implicit conversion from Option which" when {

      "called with None" should {

        "return a default version of MoneyServiceBusiness" in {

          val res: MoneyServiceBusiness = None
          res must be(emptyModel)
        }
      }

      "called with a concrete value" should {
        "return the value passed in extracted from the option" in {
          val res: MoneyServiceBusiness = Some(completeModel)
          res must be(completeModel)
        }
      }
    }

    "have a section function that" when {
      implicit val cacheMap = mock[CacheMap]

      "model is empty" should {
        "return a NotStarted Section" in {
          when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)) thenReturn None
          MoneyServiceBusiness.section must be(Section(MoneyServiceBusiness.key, NotStarted, false,  controllers.msb.routes.WhatYouNeedController.get()))
        }
      }

      "model is incomplete" should {
        "return a NotStarted Section" in {
          when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key)) thenReturn Some(BusinessMatching(msbServices = Some(MsbServices(
            Set(ChequeCashingScrapMetal)))))
          MoneyServiceBusiness.section must be(Section(MoneyServiceBusiness.key, Started, false,  controllers.msb.routes.WhatYouNeedController.get()))
        }
      }

      "model is complete" should {
        "return a Completed Section" in {
          when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)) thenReturn Some(completeModel)
          MoneyServiceBusiness.section must be(Section(MoneyServiceBusiness.key, Completed, false,  controllers.msb.routes.SummaryController.get()))
        }
      }
    }

    "have an isComplete function that" must {

      "correctly show if the model is complete" in {
        completeModel.isComplete(true, true) must be(true)
      }

      "correctly show if the model is incomplete" in {
        emptyModel.isComplete(false, false) must be(false)
      }
    }

    "Serialise to expected Json" when {
      "model is complete" in {
        Json.toJson(completeModel) must be(completeJson)
      }
    }

    "Deserialise from Json as expected" when {
      "model is complete" in {
        completeJson.as[MoneyServiceBusiness] must be(completeModel)
      }
    }
  }
}

trait MoneyServiceBusinessTestData {

  private val businessUseAnIPSP = BusinessUseAnIPSPYes("name", "123456789123456")
  private val sendTheLargestAmountsOfMoney = SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))

  val completeModel = MoneyServiceBusiness(
    throughput = Some(ExpectedThroughput.Second),
    businessUseAnIPSP = Some(businessUseAnIPSP),
    identifyLinkedTransactions = Some(IdentifyLinkedTransactions(true)),
    Some(WhichCurrencies(
      Seq("USD", "GBP", "EUR"),
      Some(BankMoneySource("bank names")),
      Some(WholesalerMoneySource("Wholesaler Names")),
      true)),
    businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("123456")),
    sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
    fundsTransfer = Some(FundsTransfer(true)),
    branchesOrAgents = Some(BranchesOrAgents(Some(Seq(Country("United Kingdom", "GB"))))),
    sendTheLargestAmountsOfMoney = Some(sendTheLargestAmountsOfMoney),
    mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
    transactionsInNext12Months = Some(TransactionsInNext12Months("12345678963")),
    ceTransactionsInNext12Months = Some(CETransactionsInNext12Months("12345678963"))
  )

  val emptyModel = MoneyServiceBusiness(None)


  val completeJson = Json.obj(
    "throughput" -> Json.obj("throughput" -> "02"),
    "businessUseAnIPSP" -> Json.obj(
      "useAnIPSP" -> true,
      "name" -> "name",
      "referenceNumber" -> "123456789123456"
    ),
    "identifyLinkedTransactions" -> Json.obj("linkedTxn" -> true),
    "whichCurrencies" -> Json.obj(
      "currencies" -> Json.arr("USD", "GBP", "EUR"),
      "bankMoneySource" -> "Yes",
      "bankNames" -> "bank names",
      "wholesalerMoneySource" -> "Yes",
      "wholesalerNames" -> "Wholesaler Names",
      "customerMoneySource" -> "Yes"
    ),
    "businessAppliedForPSRNumber" -> Json.obj(
      "appliedFor" -> true,
      "regNumber" -> "123456"
    ),
    "sendMoneyToOtherCountry" -> Json.obj("money" -> true),
    "fundsTransfer" -> Json.obj("transferWithoutFormalSystems" -> true),
    "branchesOrAgents" -> Json.obj("hasCountries" -> true,"countries" ->Json.arr("GB")),
    "transactionsInNext12Months" -> Json.obj("txnAmount" -> "12345678963"),
    "fundsTransfer" -> Json.obj("transferWithoutFormalSystems" -> true),
    "mostTransactions" -> Json.obj("mostTransactionsCountries" -> Seq("GB")),
    "sendTheLargestAmountsOfMoney" -> Json.obj("country_1" ->"GB"),
    "ceTransactionsInNext12Months" -> Json.obj("ceTransaction" -> "12345678963"),
    "hasChanged" -> false
  )

  val emptyJson = Json.obj("msbServices" -> Json.arr())
}
