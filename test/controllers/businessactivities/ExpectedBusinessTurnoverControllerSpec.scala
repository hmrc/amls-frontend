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

package controllers.businessactivities

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.businessactivities.ExpectedBusinessTurnoverFormProvider
import models.businessactivities.{BusinessActivities, ExpectedBusinessTurnover}
import models.status.NotCompleted
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.StatusService
import services.cache.Cache
import utils.AmlsSpec
import views.html.businessactivities.ExpectedBusinessTurnoverView

import scala.concurrent.{ExecutionContext, Future}

class ExpectedBusinessTurnoverControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture {
    self =>
    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)
    implicit val ec: ExecutionContext            = inject[ExecutionContext]

    lazy val view: ExpectedBusinessTurnoverView = inject[ExpectedBusinessTurnoverView]
    val controller                              = new ExpectedBusinessTurnoverController(
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      statusService = mock[StatusService],
      cc = mockMcc,
      formProvider = inject[ExpectedBusinessTurnoverFormProvider],
      view = view
    )
  }

  val emptyCache: Cache = Cache.empty

  "ExpectedBusinessTurnoverControllerSpec" when {

    "get is called" must {
      "display the Expected Business Turnover page with an empty form when there is no existing data" in new Fixture {

        when(controller.dataCacheConnector.fetch[ExpectedBusinessTurnover](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(controller.statusService.getStatus(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val html: String       = contentAsString(result)
        val document: Document = Jsoup.parse(html)

        ExpectedBusinessTurnover.all.foreach { value =>
          document.select(s"input[value=${value.toString}]").hasAttr("checked") must be(false)
        }
      }

      "display the Expected Business Turnover page with pre populated data" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(
            Future.successful(Some(BusinessActivities(expectedBusinessTurnover = Some(ExpectedBusinessTurnover.First))))
          )

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("input[value=zeroPlus]").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {

      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.ExpectedBusinessTurnoverController.post().url).withFormUrlEncodedBody(
              "expectedBusinessTurnover" -> ""
            )

          val result: Future[Result] = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }

      "respond with SEE_OTHER" when {
        "edit is false and redirect to the ExpectedAMLSTurnoverController" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.ExpectedBusinessTurnoverController.post().url).withFormUrlEncodedBody(
              "expectedBusinessTurnover" -> "zeroPlus"
            )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
            .thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result: Future[Result] = controller.post(false)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.businessactivities.routes.ExpectedAMLSTurnoverController.get().url)
          )
        }

        "edit is true and redirect to the SummaryController" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.ExpectedBusinessTurnoverController.post(true).url).withFormUrlEncodedBody(
              "expectedBusinessTurnover" -> "zeroPlus"
            )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
            .thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result: Future[Result] = controller.post(true)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get.url))
        }
      }
    }
  }
}
