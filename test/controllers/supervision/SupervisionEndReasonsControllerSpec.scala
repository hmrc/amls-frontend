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

package controllers.supervision

import controllers.actions.SuccessfulAuthAction
import models.supervision._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, DependencyMocks}
import views.html.supervision.supervision_end_reasons

class SupervisionEndReasonsControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends DependencyMocks{
    self => val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[supervision_end_reasons]
    val controller = new SupervisionEndReasonsController(
      mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      supervision_end_reasons = view)
  }

  "SupervisionEndReasonsController" must {

    "on get display the SupervisionEndReasons page" in new Fixture {

      mockCacheFetch[Supervision](None)

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("supervision.supervision_end_reasons.title"))

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
      document.select("textarea[name=endingReason]").`val` must be("")
    }

    "on post with valid data" in new Fixture {
      val start = Some(SupervisionStart(new LocalDate(1990, 2, 24))) //scalastyle:off magic.number
      val end = Some(SupervisionEnd(new LocalDate(1998, 2, 24))) //scalastyle:off magic.number

      val newRequest = requestWithUrlEncodedBody(
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

      val newRequest = requestWithUrlEncodedBody(
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

      val newRequest = requestWithUrlEncodedBody(
        "anotherBody" -> "true",
        "endingReason" -> "Reason")

      mockCacheFetch[Supervision](Some(Supervision(Some(AnotherBodyYes("Name", start, end)))))

      mockCacheSave[Supervision]

      mockCacheGetEntry[Supervision](Some(completeModel), Supervision.key)

      val result = controller.post(true)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

  }
}


