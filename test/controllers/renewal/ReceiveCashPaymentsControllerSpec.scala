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

package controllers.renewal

import connectors.DataCacheConnector
import models.renewal.{PaymentMethods, ReceiveCashPayments, Renewal}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import services.{RenewalService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.concurrent.Future

class ReceiveCashPaymentsControllerSpec extends GenericTestHelper with MockitoSugar {

  lazy val mockDataCacheConnector = mock[DataCacheConnector]
  lazy val mockRenewalService = mock[RenewalService]

  val receiveCashPayments = ReceiveCashPayments(
    Some(PaymentMethods(true, true,Some("other"))
  ))

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ReceiveCashPaymentsController (
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      renewalService = mockRenewalService
    )

    when(mockRenewalService.getRenewal(any(),any(),any()))
      .thenReturn(Future.successful(None))

    when(mockRenewalService.updateRenewal(any())(any(),any(),any()))
      .thenReturn(Future.successful(new CacheMap("", Map.empty)))
  }

  "ReceiveCashPaymentsController" when {

    "get is called" must {
      "load the page" when {
        "renewal data is found for receiving payments and pre-populate the data" in new Fixture {

          when(mockRenewalService.getRenewal(any(),any(),any()))
            .thenReturn(Future.successful(Some(Renewal(receiveCashPayments = Some(receiveCashPayments)))))

          val result = controller.get()(request)
          status(result) mustEqual OK

          val page = Jsoup.parse(contentAsString(result))
          page.select("input[type=radio][name=receivePayments][value=true]").hasAttr("checked") must be(true)
          page.select("input[type=radio][name=receivePayments][value=false]").hasAttr("checked") must be(false)

        }

        "no renewal data is found and show an empty form" in new Fixture {
          val result = controller.get()(request)
          status(result) mustEqual OK

          val page = Jsoup.parse(contentAsString(result))
          page.select("input[type=radio][name=receivePayments][value=true]").hasAttr("checked") must be(false)
          page.select("input[type=radio][name=receivePayments][value=false]").hasAttr("checked") must be(false)

        }
      }
    }

    "post is called" must {
      "show a bad request with an invalid request" in new Fixture {

        val result = controller.post()(request)
        status(result) mustEqual BAD_REQUEST
      }

      "redirect to summary on successful submission" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "receivePayments" -> "false"
        )

        val result = controller.post(true)(newRequest)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
      }

    }

  }
}
