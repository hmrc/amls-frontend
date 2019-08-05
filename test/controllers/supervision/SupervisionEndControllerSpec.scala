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

import controllers.actions.SuccessfulAuthAction
import models.supervision._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocksNewAuth}

class SupervisionEndControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocksNewAuth {
    self =>
    val request = addToken(authRequest)

    val controller = new SupervisionEndController(mockCacheConnector, authAction = SuccessfulAuthAction)
  }

  "SupervisionEndController" must {

    "on get display the SupervisionEnd page" in new Fixture {

      mockCacheFetch[Supervision](None)

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("supervision.supervision_end.title"))

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=endDate.day]").`val` must be("")
      document.select("input[name=endDate.month]").`val` must be("")
      document.select("input[name=endDate.year]").`val` must be("")
    }


    "on get display the SupervisionEnd page with pre populated data" in new Fixture {
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
      document.select("input[name=endDate.day]").`val` must be("24")
      document.select("input[name=endDate.month]").`val` must be("2")
      document.select("input[name=endDate.year]").`val` must be("1998")
    }

    "on get display the SupervisionEnd page with empty form when there is no data" in new Fixture {
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
      document.select("input[name=endDate.day]").`val` must be("")
      document.select("input[name=endDate.month]").`val` must be("")
      document.select("input[name=endDate.year]").`val` must be("")
    }

    "on post with valid data" in new Fixture {
      val start = Some(SupervisionStart(new LocalDate(1990, 2, 24))) //scalastyle:off magic.number
      val end = Some(SupervisionEnd(new LocalDate(1998, 2, 24))) //scalastyle:off magic.number

      val newRequest = request.withFormUrlEncodedBody(
        "anotherBody" -> "true",
        "endDate.day" -> "24",
        "endDate.month" -> "2",
        "endDate.year" -> "1998")

      mockCacheFetch[Supervision](Some(Supervision(Some(AnotherBodyYes("Name", start)))))

      mockCacheSave[Supervision]

      mockCacheGetEntry[Supervision](Some(Supervision(anotherBody = Some(AnotherBodyYes("Name", start, end)))), Supervision.key)

      val result = controller.post()(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SupervisionEndReasonsController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {
      val start = Some(SupervisionStart(new LocalDate(1990, 2, 24))) //scalastyle:off magic.number
      val end = Some(SupervisionEnd(new LocalDate(1998, 2, 24))) //scalastyle:off magic.number

      val newRequest = request.withFormUrlEncodedBody(
        "anotherBody" -> "true",
        "endDate.day" -> "24",
        "endDate.month" -> "2",
        "endDate.year" -> "1998")

      mockCacheFetch[Supervision](Some(Supervision(Some(AnotherBodyYes("Name", start)))))

      mockCacheSave[Supervision]

      mockCacheGetEntry[Supervision](Some(Supervision(anotherBody = Some(AnotherBodyYes("Name", start, end, Some(SupervisionEndReasons("Reason")))))), Supervision.key)

      val result = controller.post(true)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      mockCacheFetch[Supervision](Some(Supervision(
        Some(AnotherBodyYes("Name", Some(SupervisionStart(new LocalDate(2005, 2, 24))))),
        None,
        None,
        Some(ProfessionalBodyYes("details"))
      )))

      val newRequest = request.withFormUrlEncodedBody()

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#endDate]").html() must be(Messages("error.expected.jodadate.format"))
    }

  }
}
