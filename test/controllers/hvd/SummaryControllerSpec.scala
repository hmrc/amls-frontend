/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.DataCacheConnector
import models.hvd._
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import utils.AuthorisedFixture
import org.mockito.ArgumentCaptor
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import play.api.mvc.Results.Redirect

import scala.concurrent.Future

class SummaryControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    implicit val authContext = mock[AuthContext]
    implicit val headerCarrier = HeaderCarrier()

    val controller = new SummaryController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService: StatusService = mock[StatusService]
    }

    val day = 15
    val month = 2
    val year = 1956

    val completeModel = Hvd(Some(CashPaymentYes(new LocalDate(year, month, day))),
      Some(Products(Set(Alcohol, Tobacco, Other("test")))),
      Some(ExciseGoods(true)),
      Some(HowWillYouSellGoods(Seq(Wholesale, Retail, Auction))),
      Some(PercentageOfCashPaymentOver15000.Fifth),
      Some(ReceiveCashPayments(Some(PaymentMethods(courier = true, direct = true, other = Some("foo"))))),
      Some(LinkedCashPayments(true))
    )
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {

      val model = Hvd(None)

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))
      when(controller.dataCache.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("summary.checkyouranswers.title"))
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCache.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }

    "hide edit link for involved in other, turnover expected from activities and amls turnover expected page" when {
      "application in variation mode" in new Fixture {

        when(controller.dataCache.fetch[Hvd](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(completeModel)))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))

        document.getElementsByTag("section").get(0).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(1).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(2).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(3).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(4).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(5).getElementsByTag("a").hasClass("change-answer") must be(false)
        document.getElementsByTag("section").get(6).getElementsByTag("a").hasClass("change-answer") must be(false)
      }
    }

    "show edit link" when {
      "application not in variation mode" in new Fixture {
        when(controller.dataCache.fetch[Hvd](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(completeModel)))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(NotCompleted))

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
        controller.dataCache.fetch[Hvd](any())(any(), any(), any())
      } thenReturn Future.successful(Some(completeModel.copy(hasAccepted = false)))

      when {
        controller.dataCache.save[Hvd](eqTo(Hvd.key), any())(any(), any(), any())
      } thenReturn Future.successful(cache)

      val result = controller.post()(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get.url)

      val captor = ArgumentCaptor.forClass(classOf[Hvd])
      verify(controller.dataCache).save[Hvd](eqTo(Hvd.key), captor.capture())(any(), any(), any())
      captor.getValue.hasAccepted mustBe true
    }
  }
}
