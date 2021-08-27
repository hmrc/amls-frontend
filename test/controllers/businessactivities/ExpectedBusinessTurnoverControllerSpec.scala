/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.businessactivities

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.businessactivities.{BusinessActivities, ExpectedBusinessTurnover}
import models.status.NotCompleted
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec
import views.html.businessactivities.expected_business_turnover

import scala.concurrent.{ExecutionContext, Future}

class ExpectedBusinessTurnoverControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture {
    self => val request = addToken(authRequest)
    implicit val ec = app.injector.instanceOf[ExecutionContext]

    lazy val view = app.injector.instanceOf[expected_business_turnover]
    val controller = new ExpectedBusinessTurnoverController (
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      statusService = mock[StatusService],
      cc = mockMcc,
      expected_business_turnover = view
    )
  }

  val emptyCache = CacheMap("", Map.empty)

  "ExpectedBusinessTurnoverControllerSpec" when {

    "get is called" must {
      "display the Expected Business Turnover page with an empty form when there is no existing data" in new Fixture {

        when(controller.dataCacheConnector.fetch[ExpectedBusinessTurnover](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.statusService.getStatus(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        val result = controller.get()(request)
        status(result) must be(OK)

        val html = contentAsString(result)
        val document = Jsoup.parse(html)

        document.select("input[value=01]").hasAttr("checked") must be(false)
        document.select("input[value=02]").hasAttr("checked") must be(false)
        document.select("input[value=03]").hasAttr("checked") must be(false)
        document.select("input[value=04]").hasAttr("checked") must be(false)
        document.select("input[value=05]").hasAttr("checked") must be(false)
        document.select("input[value=06]").hasAttr("checked") must be(false)
        document.select("input[value=07]").hasAttr("checked") must be(false)

      }

      "display the Expected Business Turnover page with pre populated data" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(BusinessActivities(expectedBusinessTurnover = Some(ExpectedBusinessTurnover.First)))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[value=01]").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {

      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "expectedBusinessTurnover" -> ""
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }

      "respond with SEE_OTHER" when {
        "edit is false and redirect to the ExpectedAMLSTurnoverController" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "expectedBusinessTurnover" -> "01"
          )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(false)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.ExpectedAMLSTurnoverController.get().url))
        }

        "edit is true and redirect to the SummaryController" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "expectedBusinessTurnover" -> "01"
          )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get.url))
        }
      }
    }
  }
}
