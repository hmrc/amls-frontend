/*
 * Copyright 2021 HM Revenue & Customs
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

import controllers.actions.SuccessfulAuthAction
import generators.tradingpremises.TradingPremisesGenerator
import models.DateOfChange
import models.businessmatching.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import models.status.{SubmissionDecisionApproved, SubmissionDecisionRejected}
import models.tradingpremises._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, DependencyMocks}
import views.html.date_of_change
import views.html.tradingpremises.agent_name

import scala.concurrent.{ExecutionContext, Future}

class AgentNameControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with TradingPremisesGenerator{

  trait Fixture extends DependencyMocks { self =>

    val request = addToken(authRequest)
    lazy val view1 = app.injector.instanceOf[agent_name]
    lazy val view2 = app.injector.instanceOf[date_of_change]
    val controller = new AgentNameController(
      mockCacheConnector,
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockStatusService,
      cc = mockMcc,
      agent_name = view1,
      date_of_change = view2,
      error = errorView
    )

    mockCacheFetchAll

    mockApplicationStatus(SubmissionDecisionRejected)

    mockCacheFetch[Seq[TradingPremises]](Some(Seq(tradingPremisesGen.sample.get)), Some(TradingPremises.key))
    mockCacheGetEntry[Seq[TradingPremises]](Some(Seq(tradingPremisesGen.sample.get)), TradingPremises.key)

    mockCacheSave[Seq[TradingPremises]]
    mockCacheSave[TradingPremises]
  }

  "AgentNameController" when {

    "get is called" must {
      "display agent name Page" in new Fixture {

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        val title = s"${Messages("tradingpremises.agentname.title")} - ${Messages("summary.tradingpremises")} - ${Messages("title.amls")} - ${Messages("title.gov")}"

        document.title() must be(title)
        document.select("input[type=text]").`val`() must be(empty)
      }

      "display main Summary Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(agentName = Some(AgentName("test")))))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        val title = s"${Messages("tradingpremises.agentname.title")} - ${Messages("summary.tradingpremises")} - ${Messages("title.amls")} - ${Messages("title.gov")}"

        document.title() must be(title)
        document.select("input[type=text]").`val`() must be("test")
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
            "agentName" -> "text",
            "agentDateOfBirth.day" -> "15",
            "agentDateOfBirth.month" -> "2",
            "agentDateOfBirth.year" -> "1956"
          )

          val result = controller.post(99)(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }

      "respond with SEE_OTHER" when {
        "edit is false and given valid data" in new Fixture {

          when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))  thenReturn Future.successful(SubmissionDecisionApproved)

          val newRequest = requestWithUrlEncodedBody(
            "agentName" -> "text",
            "agentDateOfBirth.day" -> "15",
            "agentDateOfBirth.month" -> "2",
            "agentDateOfBirth.year" -> "1956"
          )

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.ConfirmAddressController.get(1).url))
        }

        "edit is true and given valid data" in new Fixture {

          when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))  thenReturn Future.successful(SubmissionDecisionApproved)


          val newRequest = requestWithUrlEncodedBody(
            "agentName" -> "text",
            "agentDateOfBirth.day" -> "15",
            "agentDateOfBirth.month" -> "2",
            "agentDateOfBirth.year" -> "1956"
          )

          val result = controller.post(1, true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(1).url))

        }
      }

      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "agentName" -> ""
          )

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)

        }

      }

      "set the hasChanged flag to true" in new Fixture {

        val newRequest = requestWithUrlEncodedBody("agentName" -> "text",
          "agentDateOfBirth.day" -> "15",
          "agentDateOfBirth.month" -> "2",
          "agentDateOfBirth.year" -> "1956")

        when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))  thenReturn Future.successful(SubmissionDecisionApproved)

        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(tradingPremisesWithHasChangedFalse, TradingPremises())))

        val result = controller.post(1)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1, false).url))

        verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
          any(),
          any(),
          meq(Seq(tradingPremisesWithHasChangedFalse.copy(
            hasChanged = true,
            agentName = Some(AgentName("text",None,Some(new LocalDate(1956,2,15)))),
            agentCompanyDetails = None,
            agentPartnership = None
          ), TradingPremises())))(any(), any())
      }

      "go to the date of change page in edit mode" when {
        "the agent name has been changed and submission is successful" in new Fixture {

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremisesWithHasChangedFalse.copy(lineId = Some(1)))))

          when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))  thenReturn Future.successful(SubmissionDecisionApproved)

          val newRequest = requestWithUrlEncodedBody(
            "agentName" -> "someName",
            "agentDateOfBirth.day" -> "15",
            "agentDateOfBirth.month" -> "2",
            "agentDateOfBirth.year" -> "1956")

          val result = controller.post(1, true)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AgentNameController.dateOfChange(1).url))
        }
      }

      "go to the check your answers page in edit mode" when {
        "the agent name has been not changed and submission is successful" in new Fixture {

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremisesWithHasChangedFalse.copy(lineId = Some(1)))))

          when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))  thenReturn Future.successful(SubmissionDecisionApproved)

          val newRequest = requestWithUrlEncodedBody(
            "agentName" -> "test",
            "agentDateOfBirth.day" -> "24",
            "agentDateOfBirth.month" -> "2",
            "agentDateOfBirth.year" -> "1990")

          val result = controller.post(1, true)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(1).url))
        }
      }

      "redirect to WhereAreTradingPremises Page" when {
        "status is SubmissionDecisionApproved" in new Fixture {

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(tradingPremisesWithHasChangedFalse.copy(lineId = None))))

          when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))  thenReturn Future.successful(SubmissionDecisionApproved)

          val newRequest = requestWithUrlEncodedBody(
            "agentName" -> "someName",
            "agentDateOfBirth.day" -> "15",
            "agentDateOfBirth.month" -> "2",
            "agentDateOfBirth.year" -> "1956")

          val result = controller.post(1, false)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))
        }
      }

      "return view for Date of Change" in new Fixture {
        val result = controller.dateOfChange(1)(request)
        status(result) must be(OK)
      }

      "handle the date of change form post" when {
        "given valid data for a agent name" in new Fixture {

          val postRequest = requestWithUrlEncodedBody(
            "dateOfChange.year" -> "2010",
            "dateOfChange.month" -> "10",
            "dateOfChange.day" -> "01"
          )

          val name = AgentName("someName")
          val updatedName= name.copy(dateOfChange = Some(DateOfChange(new LocalDate(2010, 10, 1))))

          val yourPremises = mock[YourTradingPremises]
          when(yourPremises.startDate) thenReturn new Some(new LocalDate(2005, 1, 1))

          val premises = TradingPremises(agentName = Some(name), yourTradingPremises = Some(yourPremises))

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), meq(TradingPremises.key))(any(), any()))
            .thenReturn(Future.successful(Some(Seq(premises))))

          val result = controller.saveDateOfChange(1)(postRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(1).url))

          val captor = ArgumentCaptor.forClass(classOf[Seq[TradingPremises]])
          verify(controller.dataCacheConnector).save[Seq[TradingPremises]](any(), meq(TradingPremises.key), captor.capture())(any(), any())

          captor.getValue.head.agentName match {
            case Some(savedName: AgentName) => savedName must be(updatedName)
          }

        }

        "given a date of change which is before the activity start date" in new Fixture {
          val postRequest = requestWithUrlEncodedBody(
            "dateOfChange.year" -> "2003",
            "dateOfChange.month" -> "10",
            "dateOfChange.day" -> "01"
          )

          val yourPremises = mock[YourTradingPremises]
          when(yourPremises.startDate) thenReturn Some(new LocalDate(2005, 1, 1))

          val premises = TradingPremises(agentName = Some(AgentName("Trading Name")), yourTradingPremises = Some(yourPremises))

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), meq(TradingPremises.key))(any(), any()))
            .thenReturn(Future.successful(Some(Seq(premises))))

          val result = controller.saveDateOfChange(1)(postRequest)

          status(result) must be(BAD_REQUEST)
        }
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
  val testAgentName = AgentName(agentName = "test", agentDateOfBirth = Some(date))
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
