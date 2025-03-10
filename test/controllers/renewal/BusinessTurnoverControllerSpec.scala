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
import forms.renewal.BusinessTurnoverFormProvider
import models.renewal.{BusinessTurnover, Renewal}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.RenewalService
import services.cache.Cache
import utils.AmlsSpec
import views.html.renewal.BusinessTurnoverView

import scala.concurrent.Future

class BusinessTurnoverControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with ScalaFutures
    with BeforeAndAfterEach
    with Injecting {

  lazy val mockDataCacheConnector = mock[DataCacheConnector]
  lazy val mockRenewalService     = mock[RenewalService]

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val mockCacheMap = mock[Cache]

    val emptyCache = Cache.empty

    lazy val view  = inject[BusinessTurnoverView]
    val controller = new BusinessTurnoverController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService = mockRenewalService,
      cc = mockMcc,
      formProvider = inject[BusinessTurnoverFormProvider],
      view = view
    )

    when(mockRenewalService.getRenewal(any()))
      .thenReturn(Future.successful(Some(Renewal(businessTurnover = Some(BusinessTurnover.First)))))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRenewalService, mockDataCacheConnector)
  }

  val emptyCache = Cache.empty

  "BusinessTurnoverControllerSpec" must {

    "when get is called" must {
      "on get display the  Business Turnover page" in new Fixture {

        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(messages("renewal.business-turnover.title"))
      }

      "on get display the  Business Turnover page with pre populated data" in new Fixture {

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[value=zeroPlus]").hasAttr("checked") must be(true)
      }

    }

    "when post is called" must {

      "return 400" when {

        "given no data" in new Fixture {

          val newRequest = FakeRequest(POST, routes.BusinessTurnoverController.post().url).withFormUrlEncodedBody(
            "businessTurnover" -> ""
          )

          verifyNoInteractions(mockDataCacheConnector, mockRenewalService)

          status(controller.post()(newRequest)) must be(BAD_REQUEST)
        }

        "given bad data" in new Fixture {

          val newRequest = FakeRequest(POST, routes.BusinessTurnoverController.post().url).withFormUrlEncodedBody(
            "businessTurnover" -> "foo"
          )

          verifyNoInteractions(mockDataCacheConnector, mockRenewalService)

          status(controller.post()(newRequest)) must be(BAD_REQUEST)
        }
      }

      "on post with valid data" in new Fixture {

        val newRequest = FakeRequest(POST, routes.BusinessTurnoverController.post().url).withFormUrlEncodedBody(
          "businessTurnover" -> "zeroPlus"
        )

        when(controller.dataCacheConnector.fetch[BusinessTurnover](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.AMLSTurnoverController.get().url))
      }

      "on post with valid data in edit mode" in new Fixture {

        val newRequest = FakeRequest(POST, routes.BusinessTurnoverController.post().url).withFormUrlEncodedBody(
          "businessTurnover" -> "tenMillionPlus"
        )

        when(controller.dataCacheConnector.fetch[BusinessTurnover](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get.url))
      }
    }
  }
}
