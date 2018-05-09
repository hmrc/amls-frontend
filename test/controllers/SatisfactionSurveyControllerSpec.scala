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

package controllers

import models.businessactivities._
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.AmlsSpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.{AuditResult, AuditConnector}
import utils.AuthorisedFixture
import org.mockito.Matchers._
import org.mockito.Mockito._

import scala.concurrent.Future

class SatisfactionSurveyControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new SatisfactionSurveyController (
      mock[AuditConnector],
      self.authConnector
    )

    def model: Option[BusinessActivities] = None

  }

  val emptyCache = CacheMap("", Map.empty)

  "ExpectedAMLSTurnoverController" must {

    "on get display the survey page" in new Fixture {

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("survey.satisfaction.lbl.01"))
      contentAsString(result) must include(Messages("survey.satisfaction.lbl.02"))
      contentAsString(result) must include(Messages("survey.satisfaction.lbl.03"))
      contentAsString(result) must include(Messages("survey.satisfaction.lbl.04"))
      contentAsString(result) must include(Messages("survey.satisfaction.lbl.05"))
    }

    "on post with valid data go to the status page with answers audited" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "satisfaction" -> "01",
        "details" -> ""
      )

      when(controller.auditConnector.sendEvent(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.routes.LandingController.get().url))
    }

    "on post with valid data go to the status page when audit fails" in new Fixture {

      when(controller.auditConnector.sendEvent(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception()))

      val newRequest = request.withFormUrlEncodedBody(
        "satisfaction" -> "01",
        "details" -> ""
      )

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.routes.LandingController.get().url))
    }


    "on post with invalid data" in new Fixture {

      val result = controller.post(true)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe BAD_REQUEST
      document.select(".amls-error-summary").size mustEqual 1
    }

  }
}
