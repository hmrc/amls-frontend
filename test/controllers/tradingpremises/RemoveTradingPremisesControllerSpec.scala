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

package controllers.tradingpremises

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.tradingpremises.RemoveTradingPremisesFormProvider
import models.businessmatching.BusinessActivity.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import models.status._
import models.tradingpremises.BusinessStructure.SoleProprietor
import models.tradingpremises.TradingPremisesMsbService._
import models.tradingpremises._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.StatusService
import services.cache.Cache
import utils.{AmlsSpec, StatusConstants}
import views.html.tradingpremises.RemoveTradingPremisesView

import java.time.LocalDate
import scala.concurrent.Future

class RemoveTradingPremisesControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[RemoveTradingPremisesView]
    val controller = new RemoveTradingPremisesController(
      dataCacheConnector = mock[DataCacheConnector],
      statusService = mock[StatusService],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[RemoveTradingPremisesFormProvider],
      view = view,
      error = errorView
    )
  }

  "RemoveTradingPremisesController" must {

    val address = Address("1", None, None, None, "asdfasdf")
    val year    = 1990
    val month   = 2
    val day     = 24
    val date    = LocalDate.of(year, month, day)

    val ytp = YourTradingPremises("tradingName1", address, Some(true), Some(date))

    "successfully load remove trading premises page" when {

      "application status is approved" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(None, Some(ytp), lineId = Some(1234))))))

        when(
          controller.statusService
            .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
        )
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result        = controller.get(1, false)(request)
        val contentString = contentAsString(result)
        val doc           = Jsoup.parse(contentString)
        doc.getElementsMatchingOwnText(messages("lbl.day")).hasText must be(true)
      }

      "application status is ready for renewal" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(None, Some(ytp), lineId = Some(1234))))))

        when(
          controller.statusService
            .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
        )
          .thenReturn(Future.successful(ReadyForRenewal(None)))

        val result = controller.get(1, false)(request)

        val contentString = contentAsString(result)
        val doc           = Jsoup.parse(contentString)
        doc.getElementsMatchingOwnText(messages("lbl.day")).hasText must be(true)
      }

      "application status is ready for renewal amendment" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(None, Some(ytp), lineId = Some(1234))))))

        when(
          controller.statusService
            .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
        )
          .thenReturn(Future.successful(RenewalSubmitted(None)))

        val result = controller.get(1, false)(request)

        val contentString = contentAsString(result)
        val doc           = Jsoup.parse(contentString)
        doc.getElementsMatchingOwnText(messages("lbl.day")).hasText must be(true)
      }

      "application status is NotCompleted" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(None, Some(ytp))))))

        when(
          controller.statusService
            .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
        )
          .thenReturn(Future.successful(NotCompleted))

        val result        = controller.get(1, false)(request)
        val contentString = contentAsString(result)
        val doc           = Jsoup.parse(contentString)
        doc.getElementsMatchingOwnText(messages("lbl.day")).hasText must be(false)

      }
    }

    "successfully load remove trading premises page with no trading name" in new Fixture {

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises(None, None)))))

      when(
        controller.statusService
          .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
      )
        .thenReturn(Future.successful(SubmissionDecisionApproved))

      status(controller.get(1, false)(request)) mustBe OK
    }

    "respond with NOT_FOUND" when {
      "there is no data at all at that index" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
          .thenReturn(Future.successful(None))
        when(
          controller.statusService
            .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
        )
          .thenReturn(Future.successful(NotCompleted))

        val result = controller.get(1, false)(request)

        status(result) must be(NOT_FOUND)
      }
    }

    "not show the date field for an amendment or variation when the trading premises is new" in new Fixture {

      val tradingPremises = TradingPremises(lineId = None, yourTradingPremises = Some(ytp))

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
        .thenReturn(Future.successful(Some(Seq(tradingPremises))))

      when(
        controller.statusService
          .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
      ).thenReturn(Future.successful(SubmissionDecisionApproved))

      val result = controller.get(1)(request)

      status(result) must be(OK)
    }
  }

  it when {
    "remove is called" must {
      "respond with SEE_OTHER" when {
        "removing a trading premises from an application with status NotCompleted" in new Fixture {

          val emptyCache = Cache.empty

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(tradingPremisesList)))
          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(NotCompleted))

          val result = controller.remove(1, false)(request)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.tradingpremises.routes.YourTradingPremisesController.get().url)
          )

          verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
            any(),
            any(),
            meq(
              Seq(
                completeTradingPremises2,
                completeTradingPremises3
              )
            )
          )(any())
        }
        "removing a trading premises from an application with status SubmissionReady" in new Fixture {

          val emptyCache = Cache.empty

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(tradingPremisesList)))
          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.remove(1, false)(request)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.tradingpremises.routes.YourTradingPremisesController.get().url)
          )

          verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
            any(),
            any(),
            meq(
              Seq(
                completeTradingPremises2,
                completeTradingPremises3
              )
            )
          )(any())
        }

        "removing a trading premises (with line id) from an application with status SubmissionReadyForReview" in new Fixture {

          val emptyCache = Cache.empty
          val newRequest = FakeRequest(POST, routes.RemoveTradingPremisesController.remove(1).url)
            .withFormUrlEncodedBody(
              "endDate.day"   -> "1",
              "endDate.month" -> "1",
              "endDate.year"  -> "2001"
            )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(tradingPremisesList)))
          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = controller.remove(1, false)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.tradingpremises.routes.YourTradingPremisesController.get().url)
          )

          verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
            any(),
            any(),
            meq(
              Seq(
                completeTradingPremises1.copy(status = Some(StatusConstants.Deleted), hasChanged = true),
                completeTradingPremises2,
                completeTradingPremises3
              )
            )
          )(any())
        }

        "removing a trading premises (no line id) from an application with status SubmissionReadyForReview" in new Fixture {

          val emptyCache = Cache.empty

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises(lineId = None)))))

          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when(controller.dataCacheConnector.save(any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.remove(1, false)(request)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.tradingpremises.routes.YourTradingPremisesController.get().url)
          )
        }

        "removing a trading premises from an application with status SubmissionDecisionApproved" in new Fixture {

          val emptyCache = Cache.empty
          val newRequest = FakeRequest(POST, routes.RemoveTradingPremisesController.remove(1).url)
            .withFormUrlEncodedBody(
              "endDate.day"   -> "1",
              "endDate.month" -> "1",
              "endDate.year"  -> "2001"
            )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(tradingPremisesList)))
          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1, false)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.tradingpremises.routes.YourTradingPremisesController.get().url)
          )

          verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
            any(),
            any(),
            meq(
              Seq(
                completeTradingPremises1.copy(
                  status = Some(StatusConstants.Deleted),
                  hasChanged = true,
                  endDate = Some(ActivityEndDate(LocalDate.of(2001, 1, 1)))
                ),
                completeTradingPremises2,
                completeTradingPremises3
              )
            )
          )(any())
        }

        "removing a new trading premises (no line id) in an amendment or variation" in new Fixture {
          val emptyCache = Cache.empty

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises(lineId = None)))))

          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          when(controller.dataCacheConnector.save(any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.remove(1, false)(request)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.tradingpremises.routes.YourTradingPremisesController.get().url)
          )
        }
      }

      "respond with BAD_REQUEST" when {
        "removing a trading premises from an application with no date" in new Fixture {
          val emptyCache = Cache.empty

          val newRequest = FakeRequest(POST, routes.RemoveTradingPremisesController.remove(1, true).url)
            .withFormUrlEncodedBody(
              "endDate.day"   -> "",
              "endDate.month" -> "",
              "endDate.year"  -> ""
            )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(tradingPremisesList)))
          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1, true)(newRequest)
          status(result) must be(BAD_REQUEST)

        }
        "removing a trading premises from an application a year too great in length" in new Fixture {
          val emptyCache = Cache.empty

          val newRequest = FakeRequest(POST, routes.RemoveTradingPremisesController.remove(1, true).url)
            .withFormUrlEncodedBody(
              "endDate.day"   -> "1",
              "endDate.month" -> "12",
              "endDate.year"  -> "123456789"
            )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(tradingPremisesList)))
          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1, true)(newRequest)
          status(result) must be(BAD_REQUEST)

        }

        "removing a trading premises from an application with future date" in new Fixture {
          val emptyCache = Cache.empty

          val newRequest = FakeRequest(POST, routes.RemoveTradingPremisesController.remove(1, true).url)
            .withFormUrlEncodedBody(
              "endDate.day"   -> "15",
              "endDate.month" -> "1",
              "endDate.year"  -> "2030"
            )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(tradingPremisesList)))
          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1, true)(newRequest)
          status(result) must be(BAD_REQUEST)

        }

        "removing a trading premises from an application with end date before premises start date" in new Fixture {

          val emptyCache = Cache.empty

          val tradingPremisesEndDateList = Seq(completeTradingPremises1)

          val newRequest = FakeRequest(POST, routes.RemoveTradingPremisesController.remove(1, true).url)
            .withFormUrlEncodedBody(
              "endDate.day"   -> "15",
              "endDate.month" -> "1",
              "endDate.year"  -> "1989"
            )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(tradingPremisesEndDateList)))
          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(
            controller.statusService
              .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
          )
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1, true)(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

    }
  }

  val ytp = YourTradingPremises(
    "foo",
    Address(
      "1",
      None,
      None,
      None,
      "asdfasdf"
    ),
    Some(true),
    Some(LocalDate.of(1990, 2, 24))
  )

  val businessStructure        = SoleProprietor
  val agentName                = AgentName("test")
  val agentCompanyName         = AgentCompanyDetails("test", Some("12345678"))
  val agentPartnership         = AgentPartnership("test")
  val wdbd                     = WhatDoesYourBusinessDo(
    Set(BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness)
  )
  val msbServices              = TradingPremisesMsbServices(Set(TransmittingMoney, CurrencyExchange))
  val completeTradingPremises1 = TradingPremises(
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
    Some(ActivityEndDate(LocalDate.of(1999, 1, 1)))
  )
  val completeTradingPremises2 = TradingPremises(
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
    Some(ActivityEndDate(LocalDate.of(1999, 1, 1)))
  )
  val completeTradingPremises3 = TradingPremises(
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
    Some(ActivityEndDate(LocalDate.of(1999, 1, 1)))
  )

  val tradingPremisesList = Seq(completeTradingPremises1, completeTradingPremises2, completeTradingPremises3)
}
