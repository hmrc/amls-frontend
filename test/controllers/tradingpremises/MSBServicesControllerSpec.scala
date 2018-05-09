/*
 * Copyright 2018 HM Revenue & Customs
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
import models.{TradingPremisesSection}
import models.businessmatching.{BusinessMatching, BusinessMatchingMsbServices, TransmittingMoney, CurrencyExchange}
import models.tradingpremises.{TradingPremisesMsbServices => TPMsbServices, TransmittingMoney => TPTransmittingMoney, TradingPremises,
CurrencyExchange => TPCurrencyExchange, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, TradingPremisesMsbService}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, AmlsSpec}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class MSBServicesControllerSpec extends AmlsSpec with ScalaFutures with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val cache: DataCacheConnector = mock[DataCacheConnector]

    val controller = new MSBServicesController {
      override val dataCacheConnector: DataCacheConnector = self.cache

      override protected def authConnector: AuthConnector = self.authConnector

      override val statusService = mock[StatusService]


    }
    val mockCacheMap = mock[CacheMap]
    val emptyCache = CacheMap("", Map.empty)
    val model = TradingPremises()

    val tp = TradingPremises(
      lineId = Some(1),
      msbServices = Some(TPMsbServices(
        Set(TPTransmittingMoney)
      ))
    )

    when(controller.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(SubmissionDecisionRejected))

  }

  "MSBServicesController" must {

    "show an empty form on get with no data in store" in new Fixture {

      when(cache.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange))))))

      val result = controller.get(1)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

    }

    "show a prefilled form when there is data in the store" in new Fixture {

      when(cache.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(TradingPremises(
          msbServices = Some(TPMsbServices(Set(TPTransmittingMoney, TPCurrencyExchange)))
        ))))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange))))))

      val result = controller.get(1)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

      document.select("input[type=checkbox][checked]").size mustBe 2
      document.select("input[value=01]").hasAttr("checked") mustBe true
      document.select("input[value=02]").hasAttr("checked") mustBe true
      document.select(".amls-error-summary").size mustBe 0
    }

    "respond with NOT_FOUND" when {

      "the index is out of bounds" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "msbServices[0]" -> "01"
        )

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(new CacheMap("", Map.empty)))

        val result = controller.post(50)(newRequest)
        status(result) must be(NOT_FOUND)
      }

      "there is no data at all at that index" in new Fixture {
        when(cache.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(None)

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange))))))

          val result = controller.get(1, false)(request)

          status(result) must be(NOT_FOUND)
        }

    }

    "return a Bad Request with errors on invalid submission" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "invalid"
      )

      when (controller.dataCacheConnector.fetch[BusinessMatching](any())(any(),any(),any())) thenReturn(Future.successful(None))

      val result = controller.post(1)(newRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe BAD_REQUEST

    }

    "redirect to PremisesRegisteredController" when {

      "on valid submission" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "msbServices[0]" -> "01"
        )

        when(cache.fetch[Seq[TradingPremises]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises(
          msbServices = Some(TPMsbServices(Set(TPTransmittingMoney, TPCurrencyExchange)))
        )))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

        val result = controller.post(1, edit = false)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.PremisesRegisteredController.get(1).url)
      }

    }

    "redirect to Detailed Answers" when {

      "adding 'Transmitting Money' as a service during edit" in new Fixture {

        val currentModel = TradingPremises(
          msbServices = Some(TPMsbServices(
            Set(ChequeCashingNotScrapMetal)
          ))
        )

        val newModel = currentModel.copy(
          msbServices = Some(TPMsbServices(
            Set(TPTransmittingMoney, TPCurrencyExchange, ChequeCashingScrapMetal, ChequeCashingNotScrapMetal)
          ))
        )

        val newRequest = request.withFormUrlEncodedBody(
          "msbServices[0]" -> "01",
          "msbServices[1]" -> "02",
          "msbServices[2]" -> "03",
          "msbServices[3]" -> "04"
        )

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises()))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

        val result = controller.post(1, edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.SummaryController.getIndividual(1).url)
      }

      "adding 'CurrencyExchange' as a service during edit" in new Fixture {

        val currentModel = TradingPremises(
          msbServices = Some(TPMsbServices(
            Set(ChequeCashingNotScrapMetal)
          ))
        )

        val newModel = currentModel.copy(
          msbServices = Some(TPMsbServices(
            Set(TPCurrencyExchange, ChequeCashingScrapMetal, ChequeCashingNotScrapMetal)
          ))
        )

        val newRequest = request.withFormUrlEncodedBody(
          "msbServices[1]" -> "02",
          "msbServices[2]" -> "03",
          "msbServices[3]" -> "04"
        )

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises()))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

        val result = controller.post(1, edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.SummaryController.getIndividual(1).url)
      }

    }

    "redirect to Check Your Answers" when {

      "adding 'Cheque Cashing' as a service during edit" in new Fixture {

        Seq[(TradingPremisesMsbService, String)]((ChequeCashingNotScrapMetal, "03"), (ChequeCashingScrapMetal, "04")) foreach {
          case (model, id) =>
            val currentModel = TradingPremises(
              msbServices = Some(TPMsbServices(
                Set(TPTransmittingMoney, TPCurrencyExchange)
              ))
            )

            val newRequest = request.withFormUrlEncodedBody(
              "msbServices[1]" -> "01",
              "msbServices[2]" -> "02",
              "msbServices[3]" -> id
            )

            when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
              (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises(msbServices = None)))))

            when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

            val result = controller.post(1, edit = true)(newRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(routes.SummaryController.getIndividual(1).url)
        }
      }

    }

    "redirect to the dateOfChange page" when {

      "services have changed for a variation" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "msbServices[0]" -> "01",
          "msbServices[1]" -> "02"
        )

        when(controller.statusService.getStatus(any(), any(), any())) thenReturn Future.successful(SubmissionDecisionApproved)

        when(cache.fetch[Seq[TradingPremises]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tp))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

        val result = controller.post(1, edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatDoesYourBusinessDoController.dateOfChange(1).url)
      }

      "the services have changed for a ready for renewal status" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "msbServices[0]" -> "01",
          "msbServices[1]" -> "02"
        )

        when(controller.statusService.getStatus(any(), any(), any())) thenReturn Future.successful(ReadyForRenewal(None))

        when(cache.fetch[Seq[TradingPremises]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tp))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

        val result = controller.post(1, edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatDoesYourBusinessDoController.dateOfChange(1).url)
      }

      "the MsbServices haven't changed, but a change from previous services page has been flagged" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "msbServices[0]" -> "01"
        )

        when(controller.statusService.getStatus(any(), any(), any())) thenReturn Future.successful(SubmissionDecisionApproved)

        when(cache.fetch[Seq[TradingPremises]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tp))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

        val result = controller.post(1, edit = true, changed = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatDoesYourBusinessDoController.dateOfChange(1).url)
      }

    }

    "redirect to the SummaryController" when {
      "editing, and the services have changed for a record that hasn't been submitted yet" in new Fixture {

        val tpNone = TradingPremises(
          lineId = None,                    // record hasn't been submitted
          msbServices = Some(TPMsbServices(
            Set(TPTransmittingMoney)
          ))
        )

        val newRequest = request.withFormUrlEncodedBody(
          "msbServices[0]" -> "01",
          "msbServices[1]" -> "02"
        )

        when(controller.statusService.getStatus(any(), any(), any())) thenReturn Future.successful(SubmissionDecisionApproved)

        when(cache.fetch[Seq[TradingPremises]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(tpNone))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

        val result = controller.post(1, edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.SummaryController.getIndividual(1).url)
      }
    }

    "set the hasChanged flag to true" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[1]" -> "01",
        "msbServices[2]" -> "02",
        "msbServices[3]" -> "03"
      )

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(1)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.PremisesRegisteredController.get(1).url))

      verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
        any(),
        meq(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse.copy(
          hasChanged = true,
          msbServices = Some(TPMsbServices(Set(TPTransmittingMoney, TPCurrencyExchange, ChequeCashingNotScrapMetal)))
        ))))(any(), any(), any())
    }

  }
}
