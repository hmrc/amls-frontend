/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.hvd

import controllers.actions.SuccessfulAuthAction
import models.businessmatching.HighValueDealing
import models.businessmatching.updateservice.ServiceChangeRegister
import models.hvd._
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, DependencyMocks}
import views.html.hvd.summary

import scala.concurrent.Future

class SummaryControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    implicit val headerCarrier = HeaderCarrier()
    lazy val view = app.injector.instanceOf[summary]
    lazy val controller =
      new SummaryController(
        authAction = SuccessfulAuthAction,
        ds = commonDependencies,
        mockCacheConnector,
        mockStatusService,
        mockServiceFlow,
        cc = mockMcc,
        summary = view)

    val day = 15
    val month = 2
    val year = 1956

    val completeModel = Hvd(Some(CashPayment(CashPaymentOverTenThousandEuros(true), Some(CashPaymentFirstDate(new LocalDate(year, month, day))))),
      Some(Products(Set(Alcohol, Tobacco, Other("test")))),
      Some(ExciseGoods(true)),
      Some(HowWillYouSellGoods(Set(Wholesale, Retail, Auction))),
      Some(PercentageOfCashPaymentOver15000.Fifth),
      Some(true),
      Some(PaymentMethods(courier = true, direct = true, other = Some("foo"))),
      Some(LinkedCashPayments(true))
    )

    mockIsNewActivityNewAuth(false)
    mockCacheFetch[ServiceChangeRegister](None, Some(ServiceChangeRegister.key))

    when {
      controller.statusService.isPreSubmission(any[Option[String]](), any[(String, String)](), any[String]())(any(), any())
    } thenReturn Future.successful(true)
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {

      val model = Hvd(None)

      when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
        .thenReturn(Future.successful(NotCompleted))
      when(controller.dataCache.fetch[Hvd](any(), any())
        (any(), any())).thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("summary.checkyouranswers.title"))
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCache.fetch[Hvd](any(), any())( any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }

    "show edit link for involved in other, turnover expected from activities and amls turnover expected page" when {
      "application in variation mode" in new Fixture {

        when(controller.dataCache.fetch[Hvd](any(), eqTo(Hvd.key))
          (any(), any())).thenReturn(Future.successful(Some(completeModel)))

        when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))

        val answerRows = document.getElementsByClass("cya-summary-list__row").toArray(Array[Element]())
        answerRows.size mustBe 9

        for ( el <- answerRows ) {
          el.getElementsByTag("a").hasClass("change-answer") must be(true)
        }
      }
    }

    "show edit link" when {
      "application not in variation mode" in new Fixture {
        when(controller.dataCache.fetch[Hvd](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(completeModel)))

        when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        val elements = document.getElementsByTag("section").iterator
        while (elements.hasNext) {
          elements.next().getElementsByTag("a").hasClass("change-answer") must be(true)
        }
      }

      "in variation mode and also in the new service flow" in new Fixture {
        when(controller.dataCache.fetch[Hvd]( any(), eqTo(Hvd.key))
          (any(), any())).thenReturn(Future.successful(Some(completeModel)))

        when(controller.statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        mockIsNewActivityNewAuth(true, Some(HighValueDealing))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        val elements = document.getElementsByTag("section").iterator

        while (elements.hasNext) {
          elements.next().getElementsByTag("a").hasClass("change-answer") must be(true)
        }
      }
    }
  }

  "POST" must {
    "update the hasAccepted flag on the model" in new Fixture {
      val cache = mock[CacheMap]

      when {
        (controller.dataCache.fetch[Hvd](any(), any())(any(), any()))
      } thenReturn Future.successful(Some(completeModel.copy(hasAccepted = false)))

      when {
        controller.dataCache.save[Hvd](any(), any(), any())(any(), any())
      } thenReturn Future.successful(cache)

      val result = controller.post()(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get.url)

      val captor = ArgumentCaptor.forClass(classOf[Hvd])
      verify(controller.dataCache).save[Hvd](any(), eqTo(Hvd.key), captor.capture())(any(), any())
      captor.getValue.hasAccepted mustBe true
    }
  }
}
