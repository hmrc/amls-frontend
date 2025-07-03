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

package models.moneyservicebusiness

import models.Country
import models.businessmatching.BusinessMatchingMsbService._
import models.businessmatching._
import models.registrationprogress._
import org.mockito.Mockito._
import play.api.libs.json.Json
import services.cache.Cache
import utils.AmlsSpec

class MoneyServiceBusinessSpec extends AmlsSpec with MoneyServiceBusinessTestData {

  "MoneyServiceBusiness" should {

    "have an implicit conversion from Option which" when {

      "called with None" should {

        "return a default version of MoneyServiceBusiness" in {

          val res: MoneyServiceBusiness = None
          res must be(emptyMsb)
        }
      }

      "called with a concrete value" should {
        "return the value passed in extracted from the option" in {
          val res: MoneyServiceBusiness = Some(completeMsb)
          res must be(completeMsb)
        }
      }
    }

    "return the correct TaskRow" when {
      implicit val cacheMap = mock[Cache]

      "model has been updated" should {
        "return an Updated TaskRow" in {

          when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key)) thenReturn Some(
            BusinessMatching(
              msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingScrapMetal)))
            )
          )
          when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)) thenReturn Some(
            completeMsb.copy(
              hasChanged = true,
              hasAccepted = true
            )
          )
          MoneyServiceBusiness.taskRow must be(
            TaskRow(
              MoneyServiceBusiness.key,
              controllers.msb.routes.SummaryController.get.url,
              true,
              Updated,
              TaskRow.updatedTag
            )
          )
        }
      }

      "model is empty" should {
        "return a NotStarted TaskRow" in {
          when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)) thenReturn None
          when(cacheMap.getEntry[MoneyServiceBusiness](BusinessMatching.key)) thenReturn None
          MoneyServiceBusiness.taskRow must be(
            TaskRow(
              MoneyServiceBusiness.key,
              controllers.msb.routes.WhatYouNeedController.get.url,
              false,
              NotStarted,
              TaskRow.notStartedTag
            )
          )
        }
      }

      "model is incomplete" should {
        "return an Incomplete TaskRow" in {
          when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key)) thenReturn Some(
            BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingScrapMetal))))
          )
          when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)) thenReturn
            Some(MoneyServiceBusiness(throughput = Some(ExpectedThroughput.Second)))
          MoneyServiceBusiness.taskRow must be(
            TaskRow(
              MoneyServiceBusiness.key,
              controllers.msb.routes.WhatYouNeedController.get.url,
              false,
              Started,
              TaskRow.incompleteTag
            )
          )
        }
      }

      "model is complete" should {
        "return a Completed TaskRow" in {
          val respUrl = controllers.msb.routes.SummaryController.get.url
          when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key)) thenReturn Some(
            BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingScrapMetal))))
          )
          when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)) thenReturn Some(completeMsb)
          MoneyServiceBusiness.taskRow must be(
            TaskRow(
              MoneyServiceBusiness.key,
              controllers.routes.YourResponsibilitiesUpdateController.get(respUrl).url,
              false,
              Completed,
              TaskRow.completedTag
            )
          )
        }
      }
    }

    "have an isComplete function that" must {

      "correctly show if the model is complete" in {
        completeMsb.isComplete(true, true, true) must be(true)
      }

      "correctly show if the model is incomplete" in {
        emptyMsb.isComplete(false, false, true) must be(false)
      }

      "show as incomplete where agents or countries and no countries" in {
        incompleteMsbNoBranchesOrAgentsCountries.isComplete(true, true, true) must be(false)
      }
    }

    "Serialise to expected Json" when {
      "model is complete" in {
        Json.toJson(completeMsb) must be(completeJson)
      }
    }

    "Deserialise from Json as expected" when {
      "model is complete" in {
        completeJson.as[MoneyServiceBusiness] must be(completeMsb)
      }
    }
  }
}

trait MoneyServiceBusinessTestData {

  private val businessUseAnIPSP            = BusinessUseAnIPSPYes("name", "123456789123456")
  private val sendTheLargestAmountsOfMoney = SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))

  val completeMsb = MoneyServiceBusiness(
    throughput = Some(ExpectedThroughput.Second),
    businessUseAnIPSP = Some(businessUseAnIPSP),
    identifyLinkedTransactions = Some(IdentifyLinkedTransactions(true)),
    Some(
      WhichCurrencies(
        Seq("USD", "GBP", "EUR"),
        Some(UsesForeignCurrenciesYes),
        Some(
          MoneySources(Some(BankMoneySource("Bank Name")), Some(WholesalerMoneySource("Wholesaler Name")), Some(true))
        )
      )
    ),
    sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
    fundsTransfer = Some(FundsTransfer(true)),
    branchesOrAgents = Some(
      BranchesOrAgents(
        BranchesOrAgentsHasCountries(true),
        Some(BranchesOrAgentsWhichCountries(Seq(Country("United Kingdom", "GB"))))
      )
    ),
    sendTheLargestAmountsOfMoney = Some(sendTheLargestAmountsOfMoney),
    mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
    transactionsInNext12Months = Some(TransactionsInNext12Months("12345678963")),
    ceTransactionsInNext12Months = Some(CETransactionsInNext12Months("12345678963")),
    fxTransactionsInNext12Months = Some(FXTransactionsInNext12Months("12345678963")),
    false,
    true
  )

  val incompleteMsbNoBranchesOrAgentsCountries = completeMsb.copy(
    branchesOrAgents = Some(BranchesOrAgents(BranchesOrAgentsHasCountries(true), None))
  )

  val emptyMsb = MoneyServiceBusiness(None)

  val completeJson = Json.obj(
    "throughput"                   -> Json.obj("throughput" -> "02"),
    "businessUseAnIPSP"            -> Json.obj(
      "useAnIPSP"       -> true,
      "name"            -> "name",
      "referenceNumber" -> "123456789123456"
    ),
    "identifyLinkedTransactions"   -> Json.obj("linkedTxn" -> true),
    "whichCurrencies"              -> Json.obj(
      "currencies"            -> Json.arr("USD", "GBP", "EUR"),
      "usesForeignCurrencies" -> UsesForeignCurrenciesYes.asInstanceOf[UsesForeignCurrencies],
      "moneySources"          -> Json.obj(
        "bankMoneySource"       -> "Yes",
        "bankNames"             -> "Bank Name",
        "wholesalerMoneySource" -> "Yes",
        "wholesalerNames"       -> "Wholesaler Name",
        "customerMoneySource"   -> "Yes"
      )
    ),
    "sendMoneyToOtherCountry"      -> Json.obj("money" -> true),
    "fundsTransfer"                -> Json.obj("transferWithoutFormalSystems" -> true),
    "branchesOrAgents"             -> Json.obj("hasCountries" -> true, "countries" -> Json.arr("GB")),
    "transactionsInNext12Months"   -> Json.obj("txnAmount" -> "12345678963"),
    "fundsTransfer"                -> Json.obj("transferWithoutFormalSystems" -> true),
    "mostTransactions"             -> Json.obj("mostTransactionsCountries" -> Seq("GB")),
    "sendTheLargestAmountsOfMoney" -> Json.obj("country_1" -> "GB"),
    "ceTransactionsInNext12Months" -> Json.obj("ceTransaction" -> "12345678963"),
    "fxTransactionsInNext12Months" -> Json.obj("fxTransaction" -> "12345678963"),
    "hasChanged"                   -> false,
    "hasAccepted"                  -> true
  )

  val emptyJson = Json.obj("msbServices" -> Json.arr())
}
