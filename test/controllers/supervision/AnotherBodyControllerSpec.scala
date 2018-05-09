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

package controllers.supervision

import models.supervision.{AnotherBodyYes, ProfessionalBodyYes, Supervision}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}


class AnotherBodyControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks{
    self => val request = addToken(authRequest)

    val controller = new AnotherBodyController {
      override val dataCacheConnector = mockCacheConnector
      override val authConnector = self.authConnector
    }
  }

  "PenalisedByProfessionalController" must {

    "on get display the Penalised By Professional Body page" in new Fixture {

      mockCacheFetch[Supervision](None)

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("supervision.another_body.title"))
    }


  "on get display the Penalised By Professional Body page with pre populated data" in new Fixture {
    val start = new LocalDate(1990, 2, 24) //scalastyle:off magic.number
    val end = new LocalDate(1998, 2, 24)   //scalastyle:off magic.number

    mockCacheFetch[Supervision](Some(Supervision(
      Some(AnotherBodyYes("Name", start, end, "Reason")),
      None,
      None,
      Some(ProfessionalBodyYes("details"))
    )))

    val result = controller.get()(request)
    status(result) must be(OK)
    contentAsString(result) must include ("Reason")

  }

  "on post with valid data" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody(
      "anotherBody" -> "true",
      "supervisorName" -> "Name",
      "startDate.day" -> "24",
      "startDate.month" -> "2",
      "startDate.year" -> "1990",
      "endDate.day" -> "24",
      "endDate.month" -> "2",
      "endDate.year" -> "1998",
      "endingReason" -> "Reason"
    )

    mockCacheFetch[Supervision](None)

    mockCacheSave[Supervision]

    val result = controller.post()(newRequest)
    status(result) must be(SEE_OTHER)
    redirectLocation(result) must be(Some(controllers.supervision.routes.ProfessionalBodyMemberController.get().url))
  }

  "on post with invalid data" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody()

    val result = controller.post()(newRequest)
    status(result) must be(BAD_REQUEST)

    val document = Jsoup.parse(contentAsString(result))
    document.select("a[href=#anotherBody]").html() must be(Messages("error.required.supervision.anotherbody"))
  }

   "on post with valid data in edit mode" in new Fixture {

     val newRequest = request.withFormUrlEncodedBody(
       "anotherBody" -> "false"
     )

     mockCacheFetch[Supervision](None)

     mockCacheSave[Supervision]

     val result = controller.post(true)(newRequest)
     status(result) must be(SEE_OTHER)
     redirectLocation(result) must be(Some(controllers.supervision.routes.SummaryController.get().url))
   }

  }

}


