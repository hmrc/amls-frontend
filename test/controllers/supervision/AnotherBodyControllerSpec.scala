/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.supervision

import models.supervision._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

class AnotherBodyControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val controller = new AnotherBodyController(mockCacheConnector, authConnector = self.authConnector)
  }

  "AnotherBodyControllerController" must {

    "on get display the Another Body page" in new Fixture {

      mockCacheFetch[Supervision](None)

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("supervision.another_body.title"))
    }


    "on get display the Another Body page with pre populated data" in new Fixture {
      val start = Some(SupervisionStart(new LocalDate(1990, 2, 24)))
      //scalastyle:off magic.number
      val end = Some(SupervisionEnd(new LocalDate(1998, 2, 24))) //scalastyle:off magic.number

      mockCacheFetch[Supervision](Some(Supervision(
        Some(AnotherBodyYes("Name", start, end, Some(SupervisionEndReasons("Reason")))), None, None, Some(ProfessionalBodyYes("details")))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=anotherBody][checked]").`val` mustEqual "true"
      document.select("input[name=supervisorName]").`val` must be("Name")
    }

    "on get display the Another Body page with empty form when there is no data" in new Fixture {
      val start = new LocalDate(1990, 2, 24) //scalastyle:off magic.number
      val end = new LocalDate(1998, 2, 24) //scalastyle:off magic.number

      mockCacheFetch[Supervision](Some(Supervision(
        Some(AnotherBodyNo),
        None,
        None,
        Some(ProfessionalBodyYes("details"))
      )))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))

      document.select("input[name=anotherBody][checked]").`val` mustEqual "false"
      document.select("input[name=supervisorName]").`val` must be("")
    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("anotherBody" -> "true", "supervisorName" -> "Name")

      mockCacheFetch[Supervision](None)

      mockCacheSave[Supervision]

      mockCacheGetEntry[Supervision](Some(Supervision(anotherBody = Some(AnotherBodyYes(supervisorName = "Name")))), Supervision.key)

      val result = controller.post()(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SupervisionStartController.get().url))
    }

    "on post with valid data for AnotherBodyYes" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("anotherBody" -> "true", "supervisorName" -> "Name")

      mockCacheFetch[Supervision](None)

      mockCacheSave[Supervision]

      mockCacheGetEntry[Supervision](Some(Supervision(anotherBody = Some(AnotherBodyYes(supervisorName = "Name")))), Supervision.key)

      val result = controller.post(true)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SupervisionStartController.get().url))
    }

    "on post with valid data for AnotherBodyYes" when {
      "redirect to next question when supervision is incomplete" in new Fixture {
        val start = Some(SupervisionStart(new LocalDate(1990, 2, 24))) //scalastyle:off magic.number
        val end = Some(SupervisionEnd(new LocalDate(1998, 2, 24))) //scalastyle:off magic.number

        val newRequest = request.withFormUrlEncodedBody("anotherBody" -> "true", "supervisorName" -> "Name")

        mockCacheFetch[Supervision](None)

        mockCacheSave[Supervision]

        mockCacheGetEntry[Supervision](Some(Supervision(anotherBody = Some(AnotherBodyYes("Name", start, end, Some(SupervisionEndReasons("Reason")))))), Supervision.key)

        val result = controller.post(true)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SupervisionStartController.get().url))
      }

      "redirect to summary when supervision is complete" in new Fixture with SupervisionValues {
        val start = Some(SupervisionStart(new LocalDate(1990, 2, 24))) //scalastyle:off magic.number
        val end = Some(SupervisionEnd(new LocalDate(1998, 2, 24))) //scalastyle:off magic.number

        val newRequest = request.withFormUrlEncodedBody("anotherBody" -> "true", "supervisorName" -> "Name")

        mockCacheFetch[Supervision](None)

        mockCacheSave[Supervision]

        mockCacheGetEntry[Supervision](Some(completeModel), Supervision.key)

        val result = controller.post(true)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))
      }
    }

    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody()

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#anotherBody]").html() must be(Messages("error.required.supervision.anotherbody"))
    }

    "on post with valid data in edit mode for AnotherBodyNo" in new Fixture with SupervisionValues {

      val newRequest = request.withFormUrlEncodedBody(
        "anotherBody" -> "false"
      )

      mockCacheFetch[Supervision](None)

      mockCacheGetEntry[Supervision](Some(completeModel.copy(anotherBody = Some(AnotherBodyNo))), Supervision.key)

      mockCacheSave[Supervision]

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.supervision.routes.SummaryController.get().url))
    }

    "on post with valid data for AnotherBodyNo" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "anotherBody" -> "false"
      )

      mockCacheFetch[Supervision](None)

      mockCacheGetEntry[Supervision](Some(Supervision(anotherBody = Some(AnotherBodyNo))), Supervision.key)

      mockCacheSave[Supervision]

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.supervision.routes.ProfessionalBodyMemberController.get().url))
    }
  }
}


