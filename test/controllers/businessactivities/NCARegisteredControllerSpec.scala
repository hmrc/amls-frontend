/*
 * Copyright 2018 HM Revenue & Customs
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
import models.businessactivities.{NCARegistered, BusinessActivities}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future


class NCARegisteredControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new NCARegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "NCARegisteredController" when {

    "get is called" must {
      "load the NCA Registered page with an empty form" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("ncaRegistered-true").hasAttr("checked") must be(false)
        htmlValue.getElementById("ncaRegistered-false").hasAttr("checked") must be(false)
      }

      "load Yes when ncaRegistered from save4later returns True" in new Fixture {

        val ncaRegistered = Some(NCARegistered(true))
        val activities = BusinessActivities(ncaRegistered = ncaRegistered)

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(activities)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("ncaRegistered-true").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {

      "successfully redirect to the page on selection of 'Yes' when edit mode is on" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("ncaRegistered" -> "true")

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
      }

      "successfully redirect to the page on selection of 'Yes' when edit mode is off" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody("ncaRegistered" -> "true")

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(false)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessactivities.routes.RiskAssessmentController.get().url))
      }

    }

    "successfully redirect to the page on selection of Option 'No' when edit mode is on" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "ncaRegistered" -> "false"
      )
      when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
    }

    "successfully redirect to the page on selection of Option 'No' when edit mode is off" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "ncaRegistered" -> "false"
      )
      when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(false)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.RiskAssessmentController.get().url))
    }


    "on post invalid data show error" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody()
      when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required.ba.select.nca"))

    }
  }
}
