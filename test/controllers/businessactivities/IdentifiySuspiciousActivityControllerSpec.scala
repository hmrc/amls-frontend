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
import forms.businessactivities.IdentifySuspiciousActivityFormProvider
import models.businessactivities.{BusinessActivities, IdentifySuspiciousActivity}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.AmlsSpec
import views.html.businessactivities.IdentifySuspiciousActivityView

import scala.concurrent.Future

class IdentifiySuspiciousActivityControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture {
    self =>
    val request: Request[AnyContentAsEmpty.type]  = addToken(authRequest)
    lazy val view: IdentifySuspiciousActivityView = inject[IdentifySuspiciousActivityView]
    val controller                                = new IdentifySuspiciousActivityController(
      dataCacheConnector = mock[DataCacheConnector],
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[IdentifySuspiciousActivityFormProvider],
      view = view
    )
  }

  "IdentifySuspiciousActivityController" when {

    "get is called" must {
      "display the Identify suspicious activity page with an empty form" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val page: Document = Jsoup.parse(contentAsString(result))

        page.select("input[type=radio][name=hasWrittenGuidance][value=true]").hasAttr("checked")  must be(false)
        page.select("input[type=radio][name=hasWrittenGuidance][value=false]").hasAttr("checked") must be(false)

      }

      "display the identify suspicious activity page with pre populated data" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(
            Future.successful(
              Some(
                BusinessActivities(
                  identifySuspiciousActivity = Some(IdentifySuspiciousActivity(true))
                )
              )
            )
          )

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val page: Document = Jsoup.parse(contentAsString(result))

        page.select("input[type=radio][name=hasWrittenGuidance][value=true]").hasAttr("checked") must be(true)

      }
    }

    "post is called" must {
      "on post with valid data" in new Fixture {

        val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, routes.IdentifySuspiciousActivityController.post(false).url)
            .withFormUrlEncodedBody(
              "hasWrittenGuidance" -> "true"
            )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
          .thenReturn(Future.successful(Cache(BusinessActivities.key, Map("" -> Json.obj()))))

        val result: Future[Result] = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.NCARegisteredController.get(false).url))
      }

      "on post with invalid data" in new Fixture {
        val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, routes.IdentifySuspiciousActivityController.post(false).url)
            .withFormUrlEncodedBody(
              "hasWrittenGuidance" -> "grrrrr"
            )

        val result: Future[Result] = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }

      "on post with empty data" in new Fixture {
        val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, routes.IdentifySuspiciousActivityController.post(false).url)
            .withFormUrlEncodedBody(
              "hasWrittenGuidance" -> ""
            )

        val result: Future[Result] = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }

      "on post with valid data in edit mode" in new Fixture {

        val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, routes.IdentifySuspiciousActivityController.post(true).url)
            .withFormUrlEncodedBody(
              "hasWrittenGuidance" -> "true"
            )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
          .thenReturn(Future.successful(Cache(BusinessActivities.key, Map("" -> Json.obj()))))

        val result: Future[Result] = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get.url))
      }
    }
  }
}
