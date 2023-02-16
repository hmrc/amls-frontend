/*
 * Copyright 2023 HM Revenue & Customs
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
import models.hvd.{Hvd, LinkedCashPayments}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, DependencyMocks}
import views.html.hvd.linked_cash_payments

import scala.concurrent.Future

class LinkedCashPaymentsControllerSpec extends AmlsSpec {

  trait Fixture extends DependencyMocks{
    self => val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[linked_cash_payments]
    val controller = new LinkedCashPaymentsController (
      mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      linked_cash_payments = view)
  }

  val emptyCache = CacheMap("", Map.empty)

  "LinkedCashPaymentsController" must {

    "successfully load UI for the first time" in new Fixture {
      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val title = Messages("hvd.identify.linked.cash.payment.title") + " - " +
        Messages("summary.hvd") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")
      val result = controller.get()(request)
      status(result) must be(OK)

      val htmlValue = Jsoup.parse(contentAsString(result))
      htmlValue.title mustBe title
    }

    "successfully load UI from mongoCache" in new Fixture {

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(Hvd(linkedCashPayment = Some(LinkedCashPayments(true))))))

      val title = Messages("hvd.identify.linked.cash.payment.title") + " - " +
        Messages("summary.hvd") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")
      val result = controller.get()(request)
      status(result) must be(OK)

      val htmlValue = Jsoup.parse(contentAsString(result))
      htmlValue.title mustBe title
      htmlValue.getElementById("linkedCashPayments-true").`val`() mustBe "true"
      htmlValue.getElementById("linkedCashPayments-false").`val`() mustBe "false"
    }

    "successfully redirect to nex page when submitted with valida data" in new Fixture {

      val newRequest = requestWithUrlEncodedBody("linkedCashPayments" -> "true")

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.ReceiveCashPaymentsController.get().url))
    }

    "successfully redirect to nex page when submitted with valida data in edit mode" in new Fixture {

      val newRequest = requestWithUrlEncodedBody("linkedCashPayments" -> "false")

      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get.url))
    }

    "fail with validation error when mandatory field is missing" in new Fixture {
      val newRequest = requestWithUrlEncodedBody(

      )
      when(controller.dataCacheConnector.fetch[Hvd](any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required.hvd.linked.cash.payment"))
    }

  }

}
