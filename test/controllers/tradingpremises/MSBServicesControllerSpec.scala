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
import forms.tradingpremises.MSBServicesFormProvider
import models.TradingPremisesSection
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, TransmittingMoney}
import models.businessmatching.{BusinessMatching, BusinessMatchingMsbServices}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected}
import models.tradingpremises.TradingPremisesMsbService.{ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange => TPCurrencyExchange, ForeignExchange, TransmittingMoney => TPTransmittingMoney}
import models.tradingpremises.{TradingPremises, TradingPremisesMsbService, TradingPremisesMsbServices => TPMsbServices}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.StatusService
import services.cache.Cache
import utils.AmlsSpec
import views.html.tradingpremises.MSBServicesView

import scala.concurrent.Future

class MSBServicesControllerSpec extends AmlsSpec with ScalaFutures with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val cache: DataCacheConnector = mock[DataCacheConnector]
    lazy val view                 = inject[MSBServicesView]
    val controller                = new MSBServicesController(
      dataCacheConnector = cache,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      statusService = mock[StatusService],
      cc = mockMcc,
      formProvider = inject[MSBServicesFormProvider],
      view = view,
      error = errorView
    )

    val mockCacheMap = mock[Cache]
    val emptyCache   = Cache.empty
    val model        = TradingPremises()

    val tp = TradingPremises(
      lineId = Some(1),
      msbServices = Some(
        TPMsbServices(
          Set(TPTransmittingMoney)
        )
      )
    )

    when(
      controller.statusService
        .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
    ).thenReturn(Future.successful(SubmissionDecisionRejected))

  }

  "MSBServicesController" must {

    "show an empty form on get with no data in store" in new Fixture {

      when(cache.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(
          Some(
            BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange))))
          )
        )

      val result = controller.get(1)(request)

      status(result) mustBe OK

    }

    "show a prefilled form when there is data in the store" in new Fixture {

      when(cache.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(
          Some(
            Seq(
              TradingPremises(
                msbServices = Some(TPMsbServices(Set(TPTransmittingMoney, TPCurrencyExchange)))
              )
            )
          )
        )

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(
          Some(
            BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange))))
          )
        )

      val result   = controller.get(1)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

      document.select("input[type=checkbox][checked]").size mustBe 2
      document.select(s"input[value=${TPTransmittingMoney.toString}]").hasAttr("checked") mustBe true
      document.select(s"input[value=${TPCurrencyExchange.toString}]").hasAttr("checked") mustBe true
      document.select(".govuk-error-summary").size mustBe 0
    }

    "respond with NOT_FOUND" when {

      "the index is out of bounds" in new Fixture {

        val newRequest = FakeRequest(POST, routes.MSBServicesController.post(50, false, false).url)
          .withFormUrlEncodedBody(
            "value[1]" -> TPTransmittingMoney.toString
          )

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[TradingPremises](any(), any(), any())(any()))
          .thenReturn(Future.successful(Cache.empty))

        val result = controller.post(50)(newRequest)
        status(result) must be(NOT_FOUND)
      }

      "there is no data at all at that index" in new Fixture {
        when(cache.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(None)

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(
            Some(
              BusinessMatching(msbServices =
                Some(BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange)))
              )
            )
          )

        val result = controller.get(1, false)(request)

        status(result) must be(NOT_FOUND)
      }

    }

    "return a Bad Request with errors on invalid submission" in new Fixture {

      val newRequest = FakeRequest(POST, routes.MSBServicesController.post(1, false, false).url)
        .withFormUrlEncodedBody(
          "value[1]" -> "invalid"
        )

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any())) thenReturn (Future.successful(
        None
      ))

      val result = controller.post(1)(newRequest)

      status(result) mustBe BAD_REQUEST

    }

    "redirect to DetailedAnswersController" when {

      "on valid submission" in new Fixture {

        val newRequest = FakeRequest(POST, routes.MSBServicesController.post(1, false, false).url)
          .withFormUrlEncodedBody(
            "value[1]" -> TPTransmittingMoney.toString
          )

        when(cache.fetch[Seq[TradingPremises]](any(), any())(any())).thenReturn(
          Future.successful(
            Some(
              Seq(
                TradingPremises(
                  msbServices = Some(TPMsbServices(Set(TPTransmittingMoney, TPCurrencyExchange)))
                )
              )
            )
          )
        )

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
          .thenReturn(Future.successful(Cache.empty))

        val result = controller.post(1, edit = false)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.CheckYourAnswersController.get(1).url)
      }

    }

    "redirect to Detailed Answers" when {

      "adding 'Transmitting Money' as a service during edit" in new Fixture {

        val newRequest = FakeRequest(POST, routes.MSBServicesController.post(1, true, false).url)
          .withFormUrlEncodedBody(
            "value[1]" -> TPCurrencyExchange.toString,
            "value[2]" -> TPTransmittingMoney.toString,
            "value[3]" -> ForeignExchange.toString,
            "value[4]" -> ChequeCashingScrapMetal.toString
          )

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
          .thenReturn(Future.successful(Cache.empty))

        val result = controller.post(1, edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.CheckYourAnswersController.get(1).url)
      }

      "adding 'CurrencyExchange' as a service during edit" in new Fixture {

        val newRequest = FakeRequest(POST, routes.MSBServicesController.post(1, true, false).url)
          .withFormUrlEncodedBody(
            "value[1]" -> ChequeCashingScrapMetal.toString,
            "value[2]" -> ChequeCashingNotScrapMetal.toString,
            "value[3]" -> ForeignExchange.toString
          )

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
          .thenReturn(Future.successful(Cache.empty))

        val result = controller.post(1, edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.CheckYourAnswersController.get(1).url)
      }

    }

    "redirect to Check Your Answers" when {

      "adding 'Cheque Cashing' as a service during edit" in new Fixture {

        Seq[TradingPremisesMsbService](ChequeCashingNotScrapMetal, ChequeCashingScrapMetal) foreach { model =>
          val newRequest = FakeRequest(POST, routes.MSBServicesController.post(1, true, false).url)
            .withFormUrlEncodedBody(
              "value[1]" -> TPCurrencyExchange.toString,
              "value[2]" -> TPTransmittingMoney.toString,
              "value[3]" -> model.toString
            )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises(msbServices = None)))))

          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
            .thenReturn(Future.successful(Cache.empty))

          val result = controller.post(1, edit = true)(newRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.CheckYourAnswersController.get(1).url)
        }
      }

    }

    "redirect to the dateOfChange page" when {

      "services have changed for a variation" in new Fixture {

        val newRequest = FakeRequest(POST, routes.MSBServicesController.post(1, true, false).url)
          .withFormUrlEncodedBody(
            "value[1]" -> TPTransmittingMoney.toString,
            "value[2]" -> TPCurrencyExchange.toString
          )

        when(
          controller.statusService
            .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
        ) thenReturn Future.successful(SubmissionDecisionApproved)

        when(cache.fetch[Seq[TradingPremises]](any(), any())(any())).thenReturn(Future.successful(Some(Seq(tp))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
          .thenReturn(Future.successful(Cache.empty))

        val result = controller.post(1, edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatDoesYourBusinessDoController.dateOfChange(1).url)
      }

      "the services have changed for a ready for renewal status" in new Fixture {

        val newRequest = FakeRequest(POST, routes.MSBServicesController.post(1, true, false).url)
          .withFormUrlEncodedBody(
            "value[1]" -> TPTransmittingMoney.toString,
            "value[2]" -> TPCurrencyExchange.toString
          )

        when(
          controller.statusService
            .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
        ) thenReturn Future.successful(ReadyForRenewal(None))

        when(cache.fetch[Seq[TradingPremises]](any(), any())(any())).thenReturn(Future.successful(Some(Seq(tp))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
          .thenReturn(Future.successful(Cache.empty))

        val result = controller.post(1, edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatDoesYourBusinessDoController.dateOfChange(1).url)
      }

      "the MsbServices haven't changed, but a change from previous services page has been flagged" in new Fixture {

        val newRequest = FakeRequest(POST, routes.MSBServicesController.post(1, true, true).url)
          .withFormUrlEncodedBody(
            "value[1]" -> TPTransmittingMoney.toString
          )

        when(
          controller.statusService
            .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
        ) thenReturn Future.successful(SubmissionDecisionApproved)

        when(cache.fetch[Seq[TradingPremises]](any(), any())(any())).thenReturn(Future.successful(Some(Seq(tp))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
          .thenReturn(Future.successful(Cache.empty))

        val result = controller.post(1, edit = true, changed = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatDoesYourBusinessDoController.dateOfChange(1).url)
      }

    }

    "redirect to the CheckYourAnswersController" when {
      "editing, and the services have changed for a record that hasn't been submitted yet" in new Fixture {

        val tpNone = TradingPremises(
          lineId = None, // record hasn't been submitted
          msbServices = Some(
            TPMsbServices(
              Set(TPTransmittingMoney)
            )
          )
        )

        val newRequest = FakeRequest(POST, routes.MSBServicesController.post(1, true, false).url)
          .withFormUrlEncodedBody(
            "value[1]" -> TPTransmittingMoney.toString,
            "value[2]" -> TPCurrencyExchange.toString
          )

        when(
          controller.statusService
            .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
        ) thenReturn Future.successful(SubmissionDecisionApproved)

        when(cache.fetch[Seq[TradingPremises]](any(), any())(any())).thenReturn(Future.successful(Some(Seq(tpNone))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
          .thenReturn(Future.successful(Cache.empty))

        val result = controller.post(1, edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.CheckYourAnswersController.get(1).url)
      }
    }

    "set the hasChanged flag to true" in new Fixture {

      val newRequest = FakeRequest(POST, routes.MSBServicesController.post(1, false, false).url)
        .withFormUrlEncodedBody(
          "value[1]" -> TPTransmittingMoney.toString,
          "value[2]" -> TPCurrencyExchange.toString,
          "value[3]" -> ChequeCashingNotScrapMetal.toString
        )

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any(), any())(any()))
        .thenReturn(Future.successful(Cache.empty))

      val result = controller.post(1)(newRequest)

      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.CheckYourAnswersController.get(1).url))

      verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
        any(),
        any(),
        meq(
          Seq(
            TradingPremisesSection.tradingPremisesWithHasChangedFalse.copy(
              hasChanged = true,
              msbServices =
                Some(TPMsbServices(Set(TPTransmittingMoney, TPCurrencyExchange, ChequeCashingNotScrapMetal)))
            )
          )
        )
      )(any())
    }

  }
}
