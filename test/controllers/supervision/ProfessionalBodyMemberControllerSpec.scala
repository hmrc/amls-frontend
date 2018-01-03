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

import models.supervision._
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

class ProfessionalBodyMemberControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {self =>
    val request = addToken(authRequest)

    val controller = new ProfessionalBodyMemberController (
      dataCacheConnector = mockCacheConnector,
      authConnector = self.authConnector
    )

    mockCacheSave[Supervision]

  }

  "ProfessionalBodyMemberController" must {

    "load the page Is your business a member of a professional body?" in new Fixture  {

      mockCacheFetch[Supervision](None)

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("supervision.memberofprofessionalbody.title"))

    }

    "lod the page Is your business a member of a professional body? with pre-populate data" in new Fixture  {

      mockCacheFetch[Supervision](Some(Supervision(
        professionalBodyMember = Some(ProfessionalBodyMemberYes(Set(AccountingTechnicians, CharteredCertifiedAccountants)))
      )))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=01]").hasAttr("checked") must be(true)
      document.select("input[value=02]").hasAttr("checked") must be(true)

    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "isAMember" -> "true",
        "businessType[0]" -> "01",
        "businessType[1]" -> "02"
      )

      mockCacheFetch[Supervision](None)

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.PenalisedByProfessionalController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "isAMember" -> "true",
        "businessType[0]" -> "01",
        "businessType[1]" -> "02",
        "businessType[2]" -> "03",
        "specifyOtherBusiness" -> "test"
      )

      mockCacheFetch[Supervision](None)

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "businessType[0]" -> "01",
        "businessType[1]" -> "02"
      )

      mockCacheFetch[Supervision](None)

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#isAMember]").html() must include(Messages("error.required.supervision.business.a.member"))
    }

  }

}
