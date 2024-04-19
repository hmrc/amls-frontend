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

package models.tradingpremises

import models.businessmatching.BusinessActivity._
import models.registrationprogress._
import models.tradingpremises.BusinessStructure._
import models.tradingpremises.TradingPremisesMsbService._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, StatusConstants}

import java.time.LocalDate

class TradingPremisesSpec extends AmlsSpec {

  val ytp = YourTradingPremises(
    "foo",
    Address(
      "1",
      Some("2"),
      None,
      None,
      "asdfasdf"
    ),
    Some(true),
    Some(LocalDate.of(1990, 2, 24))
  )
  val businessStructure = SoleProprietor
  val agentName = AgentName("test")
  val agentCompanyName = AgentCompanyDetails("test", Some("12345678"))
  val agentPartnership = AgentPartnership("test")
  val wdbd = WhatDoesYourBusinessDo(
    Set(
      BillPaymentServices,
      EstateAgentBusinessService,
      MoneyServiceBusiness)
  )
  val msbServices = TradingPremisesMsbServices(Set(TransmittingMoney, CurrencyExchange))
  val completeModel = TradingPremises(
    Some(RegisteringAgentPremises(true)),
    Some(ytp),
    Some(businessStructure),
    Some(agentName),
    Some(agentCompanyName),
    Some(agentPartnership),
    Some(wdbd),
    Some(msbServices),
    false,
    Some(123456),
    Some("Added"),
    Some(ActivityEndDate(LocalDate.of(1999, 1, 1))),
    hasAccepted = true
  )

  val incompleteModel = TradingPremises(Some(RegisteringAgentPremises(true)),
    Some(ytp), Some(businessStructure), Some(agentName), None, None, None, None)

  val completeJson = Json.obj("registeringAgentPremises" -> Json.obj("agentPremises" -> true),
    "yourTradingPremises" -> Json.obj("tradingName" -> "foo",
      "addressLine1" -> "1",
      "addressLine2" -> "2",
      "postcode" -> "asdfasdf",
      "isResidential" -> true,
      "startDate" -> "1990-02-24"),
    "businessStructure" -> Json.obj("agentsBusinessStructure" -> "01"),
    "agentName" -> Json.obj("agentName" -> "test"),
    "agentCompanyDetails" -> Json.obj("agentCompanyName" -> "test", "companyRegistrationNumber" -> "12345678"),
    "agentPartnership" -> Json.obj("agentPartnership" -> "test"),
    "whatDoesYourBusinessDoAtThisAddress" -> Json.obj("activities" -> Json.arr("02", "03", "05")),
    "msbServices" -> Json.obj("msbServices" -> Json.arr("01", "02")),
    "hasChanged" -> false,
    "lineId" -> 123456,
    "status" -> "Added",
    "endDate" -> Json.obj("endDate" -> "1999-01-01"),
    "hasAccepted" -> true
  )

  "TradingPremises" must {

    "return a TP model reflecting the provided data" when {
      "given 'your agent' data" in {
        val tp = TradingPremises(None, None, None)
        val newTP = tp.businessStructure(LimitedLiabilityPartnership)
        newTP must be(tp.copy(businessStructure = Some(LimitedLiabilityPartnership), hasChanged = true))
      }

      "given 'agent name' data" in {
        val tp = TradingPremises(None, None, None)
        val newTP = tp.agentName(agentName)
        newTP must be(tp.copy(agentName = Some(agentName), hasChanged = true))
      }

      "given 'agent company name' data" in {
        val tp = TradingPremises(None, None, None)
        val newTP = tp.agentCompanyDetails(agentCompanyName)
        newTP must be(tp.copy(agentCompanyDetails = Some(agentCompanyName), hasChanged = true))
      }

      "given 'agent partnership' data" in {
        val tp = TradingPremises(None, None, None)
        val newTP = tp.agentPartnership(agentPartnership)
        newTP must be(tp.copy(agentPartnership = Some(agentPartnership), hasChanged = true))
      }

      "given 'your trading premises' data" in {
        val tp = TradingPremises(None, None, None)
        val newTP = tp.yourTradingPremises(Some(ytp))
        newTP must be(tp.copy(yourTradingPremises = Some(ytp), hasChanged = true))
      }

      "given 'what does your business do' data" in {
        val tp = TradingPremises(None, None, None)
        val newTP = tp.whatDoesYourBusinessDoAtThisAddress(wdbd)
        newTP must be(tp.copy(whatDoesYourBusinessDoAtThisAddress = Some(wdbd), hasChanged = true))
      }

      "given 'agent premises' data" in {
        val tp = TradingPremises()
        val newTP = tp.registeringAgentPremises(RegisteringAgentPremises(true))
        newTP must be(tp.copy(registeringAgentPremises = Some(RegisteringAgentPremises(true)), hasChanged = true))
      }

      "given 'msb' data" in {
        val tp = TradingPremises(None, None, None)
        val newTP = tp.msbServices(Some(msbServices))
        newTP must be(tp.copy(msbServices = Some(msbServices), hasChanged = true))
      }
    }

    "Serialise as expected" in {
      Json.toJson(completeModel) must
        be(completeJson)
    }

    "deserialise correctly when hasChanged field is missing from the Json" in {
      (completeJson - "hasChanged").as[TradingPremises] must be(completeModel)
    }

    "Deserialise as expected" in {
      completeJson.as[TradingPremises] must be(completeModel)
      TradingPremises.writes.writes(completeModel) must be(completeJson)
    }

    "isComplete" must {
      "return true" when {
        "tradingPremises contains complete data" in {
          completeModel.isComplete must be(true)
        }

        "trading premises specify MSB and has sub-sectors" in {
          completeModel.copy(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(MoneyServiceBusiness))),
            msbServices = Some(TradingPremisesMsbServices(Set(TransmittingMoney)))
          ).isComplete mustBe true
        }
      }

      "return false" when {
        "tradingPremises contains incomplete data" in {
          val tradingPremises = TradingPremises(Some(RegisteringAgentPremises(true)), None)
          tradingPremises.isComplete must be(false)
        }

        "tradingPremises no data" in {
          val tradingPremises = TradingPremises(None, None, hasAccepted = true)
          tradingPremises.isComplete mustBe false
        }

        "trading premises specifies MSB but has no sub-sectors" in {
          completeModel.copy(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(MoneyServiceBusiness))),
            msbServices = Some(TradingPremisesMsbServices(Set.empty))
          ).isComplete mustBe false
        }
      }

    }

    "return label based on address details" in {

      completeModel.label must be(Some("foo, 1, 2, asdfasdf"))

    }
  }

  "Amendment and Variation flow" when {
    "the taskRow is complete with all the trading premises being removed" must {
      "successfully redirect to what you need page" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(
            TradingPremises(status = Some(StatusConstants.Deleted), hasChanged = true),
            TradingPremises(status = Some(StatusConstants.Deleted), hasChanged = true)
          )))

        val taskRow = TradingPremises.taskRow(mockCacheMap, messages)

        taskRow.hasChanged must be(true)
        taskRow.status must be(NotStarted)
        taskRow.href must be(controllers.tradingpremises.routes.TradingPremisesAddController.get(true).url)
        taskRow.tag must be(TaskRow.notStartedTag)
      }
    }

    "the taskRow is complete with all the trading premises being removed and has one incomplete model" must {
      "successfully redirect to your trading premises page" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(
            completeModel.copy(status = Some(StatusConstants.Deleted), hasChanged = true, hasAccepted = true),
            completeModel.copy(status = Some(StatusConstants.Deleted), hasChanged = true, hasAccepted = true),
            TradingPremises(Some(RegisteringAgentPremises(true)), None, hasAccepted = true)
          )))

        val taskRow = TradingPremises.taskRow(mockCacheMap, messages)

        taskRow.hasChanged must be(true)
        taskRow.status must be(Started)
        taskRow.href must be(controllers.tradingpremises.routes.YourTradingPremisesController.get().url)
        taskRow.tag must be(TaskRow.incompleteTag)
      }
    }

    "the taskRow is complete with one of the trading premises object being removed" must {
      "successfully redirect to your trading premises page" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(
            completeModel.copy(status = Some(StatusConstants.Deleted), hasChanged = true, hasAccepted = true),
            completeModel.copy(hasChanged = true))))

        val taskRow = TradingPremises.taskRow(mockCacheMap, messages)

        taskRow.hasChanged must be(true)
        taskRow.status must be(Updated)
        taskRow.href must be(controllers.tradingpremises.routes.YourTradingPremisesController.get().url)
        taskRow.tag must be(TaskRow.updatedTag)
      }
    }

    "the taskRow is complete with all the trading premises unchanged" must {
      "successfully redirect to your trading premises page" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(completeModel, completeModel)))
        val taskRow = TradingPremises.taskRow(mockCacheMap, messages)

        taskRow.hasChanged must be(false)
        taskRow.status must be(Completed)
        taskRow.href must be(controllers.tradingpremises.routes.YourTradingPremisesController.get().url)
        taskRow.tag must be(TaskRow.completedTag)
      }
    }

    "the taskRow is complete with all the trading premises being modified" must {
      "successfully redirect to your trading premises page" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(
            completeModel.copy(status = Some(StatusConstants.Updated), hasChanged = true, hasAccepted = true),
            completeModel.copy(status = Some(StatusConstants.Updated), hasChanged = true, hasAccepted = true))
          ))

        val taskRow = TradingPremises.taskRow(mockCacheMap, messages)

        taskRow.hasChanged must be(true)
        taskRow.status must be(Updated)
        taskRow.href must be(controllers.tradingpremises.routes.YourTradingPremisesController.get().url)
        taskRow.tag must be(TaskRow.updatedTag)
      }
    }
  }

  it when {

    "the taskRow consists of just 1 empty Trading premises" must {
      "return a result indicating NotStarted" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(TradingPremises())))

        TradingPremises.taskRow(mockCacheMap, messages).status must be(models.registrationprogress.NotStarted)
      }
    }

    "the taskRow consists of a partially complete model followed by a completely empty one" must {
      "return a result indicating partial completeness" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(incompleteModel, TradingPremises())))

        TradingPremises.taskRow(mockCacheMap, messages).status must be(models.registrationprogress.Started)
      }
    }

    "the taskRow consists of a complete model followed by an empty one" must {
      "return a result indicating partial completeness" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(completeModel, TradingPremises(hasAccepted = true))))

        TradingPremises.taskRow(mockCacheMap, messages).status must be(models.registrationprogress.Started)
      }
    }

    "the taskRow consists of a complete model followed by a totally empty one with no changes or acceptance" must {
      "return a result indicating completeness" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(completeModel, TradingPremises())))

        TradingPremises.taskRow(mockCacheMap, messages).status must be(models.registrationprogress.Completed)
      }
    }

    "has a completed model, an empty one and an incomplete one" when {
      "return the correct index" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(
            completeModel,
            TradingPremises(hasAccepted = true),
            incompleteModel)))

        TradingPremises.taskRow(mockCacheMap, messages).href must be(controllers.tradingpremises.routes.YourTradingPremisesController.get().url)
      }
    }

  }

  "anyChanged" must {
    val originalBankDetails = Seq(TradingPremises(None, None))
    val originalBankDetailsChanged = Seq(TradingPremises(None, None, hasChanged = true))

    "return false" when {
      "no BankDetails within the sequence have changed" in {
        val res = TradingPremises.anyChanged(originalBankDetails)
        res must be(false)
      }
    }
    "return true" when {
      "at least one BankDetails within the sequence has changed" in {
        val res = TradingPremises.anyChanged(originalBankDetailsChanged)
        res must be(true)
      }
    }
  }

  "TradingPremises.filter" must {
    "filter out any TradingPremises() instances which have the 'Deleted' status" in {

      val completeModelDeletedStatus = completeModel.copy(status = Some(StatusConstants.Deleted), hasChanged = true, hasAccepted = true)
      val emptyTradingPremises = TradingPremises()
      val tradingPremisesSeq: Seq[TradingPremises] = Seq(completeModel, completeModelDeletedStatus, completeModel, emptyTradingPremises)


      val result = TradingPremises.filter(tradingPremisesSeq)

      result must be(Seq(completeModel, completeModel))
    }
  }

}
