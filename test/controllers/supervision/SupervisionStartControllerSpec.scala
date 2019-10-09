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
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

class SupervisionStartControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends DependencyMocks{
    self => val request = addToken(authRequest)

    val controller = new SupervisionStartController(mockCacheConnector, authAction = SuccessfulAuthAction, ds = commonDependencies, cc = mockMcc)
  }

  "SupervisionStartController" must {

    "on get display the SupervisionStart page" in new Fixture {

      mockCacheFetch[Supervision](None)

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("supervision.supervision_start.title"))

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=startDate.day]").`val` must be("")
      document.select("input[name=startDate.month]").`val` must be("")
      document.select("input[name=startDate.year]").`val` must be("")
    }


    "on get display the SupervisionStart page with pre populated data" in new Fixture {
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
      document.select("input[name=startDate.day]").`val` must be("24")
      document.select("input[name=startDate.month]").`val` must be("2")
      document.select("input[name=startDate.year]").`val` must be("1990")
    }

    "on get display the SupervisionStart page with empty form when there is no data" in new Fixture {
      val start = new LocalDate(1990, 2, 24) //scalastyle:off magic.number
      val end = new LocalDate(1998, 2, 24)   //scalastyle:off magic.number

      mockCacheFetch[Supervision](Some(Supervision(
        Some(AnotherBodyNo),
        None,
        None,
        Some(ProfessionalBodyYes("details"))
      )))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=startDate.day]").`val` must be("")
      document.select("input[name=startDate.month]").`val` must be("")
      document.select("input[name=startDate.year]").`val` must be("")
    }

    "on post with valid data" in new Fixture {
      val start = Some(SupervisionStart(new LocalDate(1990, 2, 24))) //scalastyle:off magic.number
      val end = Some(SupervisionEnd(new LocalDate(1998, 2, 24))) //scalastyle:off magic.number

      val newRequest = requestWithUrlEncodedBody(
        "anotherBody" -> "true",
        "startDate.day" -> "24",
        "startDate.month" -> "2",
        "startDate.year" -> "1990")

      mockCacheFetch[Supervision](Some(Supervision(Some(AnotherBodyYes("Name")))))

      mockCacheSave[Supervision]

      mockCacheGetEntry[Supervision](Some(Supervision(anotherBody = Some(AnotherBodyYes("Name", start)))), Supervision.key)

      val result = controller.post()(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SupervisionEndController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {
      val start = Some(SupervisionStart(new LocalDate(1990, 2, 24))) //scalastyle:off magic.number
      val end = Some(SupervisionEnd(new LocalDate(1998, 2, 24))) //scalastyle:off magic.number

      val newRequest = requestWithUrlEncodedBody(
        "anotherBody" -> "true",
        "startDate.day" -> "24",
        "startDate.month" -> "2",
        "startDate.year" -> "1990")

      mockCacheFetch[Supervision](Some(Supervision(Some(AnotherBodyYes("Name", start, end)))))

      mockCacheSave[Supervision]

      mockCacheGetEntry[Supervision](Some(Supervision(Some(AnotherBodyYes("Name", start, end)))), Supervision.key)

      val result = controller.post(true)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      mockCacheFetch[Supervision](Some(Supervision(
        Some(AnotherBodyYes("Name")),
        None,
        None,
        Some(ProfessionalBodyYes("details"))
      )))

      val newRequest = requestWithUrlEncodedBody("" -> "")

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#startDate]").html() must be(Messages("error.expected.jodadate.format"))
    }
  }
}


