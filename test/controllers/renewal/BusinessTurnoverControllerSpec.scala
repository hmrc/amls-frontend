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
import models.renewal.{Renewal, BusinessTurnover}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class BusinessTurnoverControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val mockCacheMap = mock[CacheMap]

    val emptyCache = CacheMap("", Map.empty)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockRenewalService = mock[RenewalService]

    val controller = new BusinessTurnoverController(
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      renewalService = mockRenewalService
    )

    when(mockRenewalService.getRenewal(any(), any(), any()))
      .thenReturn(Future.successful(Some(Renewal(businessTurnover = Some(BusinessTurnover.First)))))
  }


  val emptyCache = CacheMap("", Map.empty)

  "BusinessTurnoverControllerSpec" must {

    "when get is called" must {
      "on get display the  Business Turnover page" in new Fixture {

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("renewal.business-turnover.title"))
      }

      "on get display the  Business Turnover page with pre populated data" in new Fixture {

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[value=01]").hasAttr("checked") must be(true)
      }

    }

    "when post is called" must {

      "on post with valid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "businessTurnover" -> "01"
        )

        when(controller.dataCacheConnector.fetch[BusinessTurnover](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(mockRenewalService.updateRenewal(any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.AMLSTurnoverController.get().url))
      }

      "on post with valid data in edit mode" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "businessTurnover" -> "01"
        )

        when(controller.dataCacheConnector.fetch[BusinessTurnover](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(mockRenewalService.updateRenewal(any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get().url))
      }

    }
  }
}
