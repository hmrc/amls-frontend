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

package controllers.renewal

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.renewal.CashPaymentsCustomersNotMetFormProvider
import models.renewal._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.RenewalService
import services.cache.Cache
import utils.AmlsSpec
import views.html.renewal.CashPaymentsCustomersNotMetView

import scala.concurrent.Future

class CashPaymentsCustomersNotMetControllerSpec extends AmlsSpec with Injecting {

  lazy val mockDataCacheConnector = mock[DataCacheConnector]
  lazy val mockRenewalService     = mock[RenewalService]

  val receiveCashPayments      = CashPayments(
    CashPaymentsCustomerNotMet(true),
    Some(HowCashPaymentsReceived(PaymentMethods(true, true, Some("other"))))
  )
  val doNotreceiveCashPayments = CashPayments(CashPaymentsCustomerNotMet(false), None)

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[CashPaymentsCustomersNotMetView]
    val controller = new CashPaymentsCustomersNotMetController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService = mockRenewalService,
      cc = mockMcc,
      formProvider = inject[CashPaymentsCustomersNotMetFormProvider],
      view = view
    )

    when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))

    when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(Cache.empty))
  }

  "CashPaymentsCustomersNotMet controller" when {
    "get is called" must {
      "load the page if business is receiving payments from customers not met in person" in new Fixture {
        when(mockRenewalService.getRenewal(any()))
          .thenReturn(Future.successful(Some(Renewal(receiveCashPayments = Some(receiveCashPayments)))))

        val result = controller.get()(request)
        status(result) mustEqual OK

        val page = Jsoup.parse(contentAsString(result))
        page.select("input[type=radio][name=receiveCashPayments][value=true]").hasAttr("checked")  must be(true)
        page.select("input[type=radio][name=receiveCashPayments][value=false]").hasAttr("checked") must be(false)
      }

      "load the page if business is not receiving payments from customers not met in person" in new Fixture {
        when(mockRenewalService.getRenewal(any()))
          .thenReturn(Future.successful(Some(Renewal(receiveCashPayments = Some(doNotreceiveCashPayments)))))

        val result = controller.get()(request)
        status(result) mustEqual OK

        val page = Jsoup.parse(contentAsString(result))
        page.select("input[type=radio][name=receiveCashPayments][value=true]").hasAttr("checked")  must be(false)
        page.select("input[type=radio][name=receiveCashPayments][value=false]").hasAttr("checked") must be(true)
      }

      "show an empty form if no renewal data is found for this question" in new Fixture {
        val result = controller.get()(request)
        status(result) mustEqual OK

        val page = Jsoup.parse(contentAsString(result))
        page.select("input[type=radio][name=receiveCashPayments][value=true]").hasAttr("checked")  must be(false)
        page.select("input[type=radio][name=receiveCashPayments][value=false]").hasAttr("checked") must be(false)
      }
    }

    "post is called" when {
      "an invalid request is made" must {
        "show a bad request" in new Fixture {
          val result = controller.post()(request)

          status(result) mustEqual BAD_REQUEST
        }
      }

      "a valid request is made" must {
        "redirect to summary page if false is passed in the form" in new Fixture {
          val newRequest = FakeRequest(POST, routes.CashPaymentsCustomersNotMetController.post().url)
            .withFormUrlEncodedBody(
              "receiveCashPayments" -> "false"
            )

          val result = controller.post()(newRequest)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustEqual Some(routes.SummaryController.get.url)
        }
      }

      "a valid request is made" must {
        "redirect to HowCashPaymentsReceivedController if true is passed in the form" in new Fixture {
          val newRequest = FakeRequest(POST, routes.CashPaymentsCustomersNotMetController.post().url)
            .withFormUrlEncodedBody(
              "receiveCashPayments" -> "true"
            )

          val result = controller.post()(newRequest)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustEqual Some(routes.HowCashPaymentsReceivedController.get().url)
        }
      }
    }
  }
}
