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

package controllers.supervision

import controllers.actions.SuccessfulAuthAction
import forms.supervision.SupervisionEndReasonsFormProvider
import models.supervision._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures

import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.{AmlsSpec, DependencyMocks}
import views.html.supervision.SupervisionEndReasonsView

class SupervisionEndReasonsControllerSpec extends AmlsSpec  with ScalaFutures with Injecting {

  trait Fixture extends DependencyMocks{
    self => val request = addToken(authRequest)
    lazy val view = inject[SupervisionEndReasonsView]
    val controller = new SupervisionEndReasonsController(
      mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[SupervisionEndReasonsFormProvider],
      view = view)
  }

  "SupervisionEndReasonsController" must {

    "on get display the SupervisionEndReasons page" in new Fixture {

      mockCacheFetch[Supervision](None)

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(messages("supervision.supervision_end_reasons.title"))

      val document = Jsoup.parse(contentAsString(result))
      document.select("textarea[name=endingReason]").`val` must be("")
    }


    "on get display the SupervisionEndReasons page with pre populated data" in new Fixture {
      val start = Some(SupervisionStart(new LocalDate(1990, 2, 24))) //scalastyle:off magic.number
      val end = Some(SupervisionEnd(new LocalDate(1998, 2, 24))) //scalastyle:off magic.number

      mockCacheFetch[Supervision](Some(Supervision(
        Some(AnotherBodyYes("Name", start, end, Some(SupervisionEndReasons("Reason")))),
        None,
        None,
        Some(ProfessionalBodyYes("details"))
      )))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("textarea[name=endingReason]").`val` must be("Reason")
    }

    "on get display the SupervisionEndReasons page with empty form when there is no data" in new Fixture {
      mockCacheFetch[Supervision](Some(Supervision(
        Some(AnotherBodyNo),
        None,
        None,
        Some(ProfessionalBodyYes("details"))
      )))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("textarea[name=endingReason]").`val` must be("")
    }

    "on post with valid data" in new Fixture {
      val start = Some(SupervisionStart(new LocalDate(1990, 2, 24))) //scalastyle:off magic.number
      val end = Some(SupervisionEnd(new LocalDate(1998, 2, 24))) //scalastyle:off magic.number

      val newRequest = FakeRequest(POST, routes.SupervisionEndReasonsController.post().url)
      .withFormUrlEncodedBody(
        "anotherBody" -> "true",
        "endingReason" -> "Reason")

      mockCacheFetch[Supervision](Some(Supervision(Some(AnotherBodyYes("Name", start, end)))))

      mockCacheSave[Supervision]

      mockCacheGetEntry[Supervision](Some(Supervision(anotherBody = Some(AnotherBodyYes("Name", start, end)))), Supervision.key)

      val result = controller.post()(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ProfessionalBodyMemberController.get().url))
    }

    "on post with valid data in edit mode if supervision is incomplete" in new Fixture {
      val start = Some(SupervisionStart(new LocalDate(1990, 2, 24))) //scalastyle:off magic.number
      val end = Some(SupervisionEnd(new LocalDate(1998, 2, 24))) //scalastyle:off magic.number

      val newRequest = FakeRequest(POST, routes.SupervisionEndReasonsController.post().url)
      .withFormUrlEncodedBody(
        "anotherBody" -> "true",
        "endingReason" -> "Reason")

      mockCacheFetch[Supervision](Some(Supervision(Some(AnotherBodyYes("Name", start, end)))))

      mockCacheSave[Supervision]

      mockCacheGetEntry[Supervision](Some(Supervision(anotherBody = Some(AnotherBodyYes("Name", start, end, Some(SupervisionEndReasons("Reason")))))), Supervision.key)

      val result = controller.post(true)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ProfessionalBodyMemberController.get().url))
    }

    "on post with valid data in edit mode if supervision is complete" in new Fixture with SupervisionValues {

      val start = Some(SupervisionStart(new LocalDate(1990, 2, 24))) //scalastyle:off magic.number
      val end = Some(SupervisionEnd(new LocalDate(1998, 2, 24))) //scalastyle:off magic.number

      val newRequest = FakeRequest(POST, routes.SupervisionEndReasonsController.post().url)
      .withFormUrlEncodedBody(
        "anotherBody" -> "true",
        "endingReason" -> "Reason")

      mockCacheFetch[Supervision](Some(Supervision(Some(AnotherBodyYes("Name", start, end)))))

      mockCacheSave[Supervision]

      mockCacheGetEntry[Supervision](Some(completeModel), Supervision.key)

      val result = controller.post(true)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get.url))
    }

  }
}


