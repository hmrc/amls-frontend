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
import models.businessactivities.{BusinessActivities, NCARegistered}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import utils.AmlsSpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import views.html.businessactivities.nca_registered

import scala.concurrent.Future


class NCARegisteredControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture {
    self => val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[nca_registered]
    val controller = new NCARegisteredController (
      dataCacheConnector = mock[DataCacheConnector],
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      nca_registered = view)
  }

  val emptyCache = CacheMap("", Map.empty)

  "NCARegisteredController" when {

    "get is called" must {
      "load the NCA Registered page with an empty form" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("ncaRegistered-true").hasAttr("checked") must be(false)
        htmlValue.getElementById("ncaRegistered-false").hasAttr("checked") must be(false)
      }

      "load Yes when ncaRegistered from mongoCache returns True" in new Fixture {

        val ncaRegistered = Some(NCARegistered(true))
        val activities = BusinessActivities(ncaRegistered = ncaRegistered)

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(activities)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("ncaRegistered-true").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {

      "successfully redirect to the page on selection of 'Yes' when edit mode is on" in new Fixture {

        val newRequest = requestWithUrlEncodedBody("ncaRegistered" -> "true")

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
      }

      "successfully redirect to the page on selection of 'Yes' when edit mode is off" in new Fixture {
        val newRequest = requestWithUrlEncodedBody("ncaRegistered" -> "true")

        when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(false)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.RiskAssessmentController.get().url))
      }

    }

    "successfully redirect to the page on selection of Option 'No' when edit mode is on" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "ncaRegistered" -> "false"
      )
      when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
    }

    "successfully redirect to the page on selection of Option 'No' when edit mode is off" in new Fixture {
      val newRequest = requestWithUrlEncodedBody(
        "ncaRegistered" -> "false"
      )
      when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(false)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.RiskAssessmentController.get().url))
    }


    "on post invalid data show error" in new Fixture {

      val newRequest = requestWithUrlEncodedBody("" -> "")
      when(controller.dataCacheConnector.fetch[BusinessActivities](any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required.ba.select.nca"))

    }
  }
}
