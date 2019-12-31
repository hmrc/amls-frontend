/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.tradingpremises

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import generators.tradingpremises.TradingPremisesGenerator
import models.businessmatching.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import models.tradingpremises._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec

import scala.concurrent.Future

class AgentCompanyDetailsControllerSpec extends AmlsSpec with TradingPremisesGenerator {

  trait Fixture {
    self => val request = addToken(authRequest)

    val controller = new AgentCompanyDetailsController (mock[DataCacheConnector], SuccessfulAuthAction, ds = commonDependencies, messagesApi, cc = mockMcc)
  }

  "AgentCompanyDetailsController" when {

    val emptyCache = CacheMap("", Map.empty)
    val mockCacheMap = mock[CacheMap]

    "get is called" must {
      "display agent company name Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

      }

      "display saved content" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(agentCompanyDetails = Some(AgentCompanyDetails("test", Some("12345678"))))))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[type=text]").first().`val`() must be("test")
      }

      "respond with NOT_FOUND" when {
        "there is no data at all at that index" in new Fixture {
          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          val result = controller.get(1)(request)

          status(result) must be(NOT_FOUND)
        }
      }
    }

    "post is called" must {
      "respond with NOT_FOUND" when {
        "there is no data at all at that index" in new Fixture {
          val newRequest = requestWithUrlEncodedBody(
            "agentCompanyName" -> "text",
            "companyRegistrationNumber" -> "12345678"
          )

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(TradingPremises())))

          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(99)(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }

      "respond with SEE_OTHER" when {
        "edit is false and given valid data" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "agentCompanyName" -> "text",
            "companyRegistrationNumber" -> "12345678"
          )
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremisesGen.sample.get)))

          when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.ConfirmAddressController.get(1).url))
        }

        "edit is true and given valid data" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "agentCompanyName" -> "text",
            "companyRegistrationNumber" -> "12345678"
          )

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(TradingPremises())))

          when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(1, true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(1).url))

        }
      }

      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "agentCompanyName" -> "11111111111" * 40
          )

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.invalid.tp.agent.company.details"))

        }

        "given missing mandatory field" in new Fixture {
          val newRequest = requestWithUrlEncodedBody(
            "agentCompanyName" -> " "
          )

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.required.tp.agent.company.details"))
        }
      }

      "set the hasChanged flag to true" in new Fixture {

        val newRequest = requestWithUrlEncodedBody("agentCompanyName" -> "text", "companyRegistrationNumber" -> "12345678")

        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(tradingPremisesWithHasChangedFalse, TradingPremises())))

        when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(1)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1, false).url))

        verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
          any(),
          any(),
          meq(Seq(tradingPremisesWithHasChangedFalse.copy(
            hasChanged = true,
            agentName = None,
            agentCompanyDetails = Some(AgentCompanyDetails("text", Some("12345678"))),
            agentPartnership = None
          ), TradingPremises())))(any(), any())
      }
    }
  }

  val address = Address("1", "2",None,None,"asdfasdf")
  val year =1990
  val month = 2
  val day = 24
  val date = new LocalDate(year, month, day)

  val ytp = YourTradingPremises("tradingName1", address, Some(true), Some(date))
  val ytp1 = YourTradingPremises("tradingName2", address, Some(true), Some(date))
  val ytp2 = YourTradingPremises("tradingName3", address, Some(true), Some(date))
  val ytp3 = YourTradingPremises("tradingName3", address, Some(true), Some(date))


  val businessStructure = SoleProprietor
  val testAgentName = AgentName("test")
  val testAgentCompanyName = AgentCompanyDetails("test", Some("12345678"))
  val testAgentPartnership = AgentPartnership("test")
  val wdbd = WhatDoesYourBusinessDo(
    Set(
      BillPaymentServices,
      EstateAgentBusinessService,
      MoneyServiceBusiness)
  )
  val msbServices = TradingPremisesMsbServices(Set(TransmittingMoney, CurrencyExchange))

  val tradingPremisesWithHasChangedFalse = TradingPremises(
    Some(RegisteringAgentPremises(true)),
    Some(ytp),
    Some(businessStructure),
    Some(testAgentName),
    Some(testAgentCompanyName),
    Some(testAgentPartnership),
    Some(wdbd),
    Some(msbServices),
    false
  )
}
