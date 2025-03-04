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
import forms.businessactivities.NCARegisteredFormProvider
import models.businessactivities.{BusinessActivities, NCARegistered}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.AmlsSpec
import views.html.businessactivities.NCARegisteredView

import scala.concurrent.Future

class NCARegisteredControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)
    lazy val view: NCARegisteredView             = inject[NCARegisteredView]
    val controller                               = new NCARegisteredController(
      dataCacheConnector = mock[DataCacheConnector],
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[NCARegisteredFormProvider],
      view = view
    )
  }

  val emptyCache: Cache = Cache.empty

  "NCARegisteredController" when {

    "get is called" must {
      "load the NCA Registered page with an empty form" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val htmlValue: Document = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("ncaRegistered").hasAttr("checked")   must be(false)
        htmlValue.getElementById("ncaRegistered-2").hasAttr("checked") must be(false)
      }

      "load Yes when ncaRegistered from mongoCache returns True" in new Fixture {

        val ncaRegistered: Option[NCARegistered] = Some(NCARegistered(true))
        val activities: BusinessActivities       = BusinessActivities(ncaRegistered = ncaRegistered)

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(Future.successful(Some(activities)))

        val result: Future[Result] = controller.get()(request)
        status(result) must be(OK)

        val htmlValue: Document = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("ncaRegistered").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {

      "successfully redirect to the page on selection of 'Yes' when edit mode is on" in new Fixture {

        val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, routes.NCARegisteredController.post(false).url)
            .withFormUrlEncodedBody("ncaRegistered" -> "true")

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result: Future[Result] = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get.url))
      }

      "successfully redirect to the page on selection of 'Yes' when edit mode is off" in new Fixture {
        val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] = test
          .FakeRequest(POST, routes.NCARegisteredController.post(false).url)
          .withFormUrlEncodedBody("ncaRegistered" -> "true")

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result: Future[Result] = controller.post(false)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.RiskAssessmentController.get().url))
      }

    }

    "successfully redirect to the page on selection of Option 'No' when edit mode is on" in new Fixture {

      val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] = test
        .FakeRequest(POST, routes.NCARegisteredController.post(false).url)
        .withFormUrlEncodedBody(
          "ncaRegistered" -> "false"
        )
      when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result: Future[Result] = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get.url))
    }

    "successfully redirect to the page on selection of Option 'No' when edit mode is off" in new Fixture {
      val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] = test
        .FakeRequest(POST, routes.NCARegisteredController.post(false).url)
        .withFormUrlEncodedBody(
          "ncaRegistered" -> "false"
        )
      when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result: Future[Result] = controller.post(false)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.RiskAssessmentController.get().url))
    }

    "on post invalid data show error" in new Fixture {

      val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] = test
        .FakeRequest(POST, routes.NCARegisteredController.post(false).url)
        .withFormUrlEncodedBody("ncaRegistered" -> "foo")
      when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
    }

    "on post empty data show error" in new Fixture {

      val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] = test
        .FakeRequest(POST, routes.NCARegisteredController.post(false).url)
        .withFormUrlEncodedBody("ncaRegistered" -> "")
      when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
    }
  }
}
