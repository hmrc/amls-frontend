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
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

class WhichProfessionalBodyControllerSpec extends PlaySpec with GenericTestHelper with MockitoSugar {

  trait Fixture extends DependencyMocks with AuthorisedFixture { self =>

    val request = addToken(authRequest)

    val controller = new WhichProfessionalBodyController(
      mockCacheConnector,
      self.authConnector
    )

    mockCacheFetch[Supervision](Some(Supervision()))
    mockCacheSave[Supervision]

  }

  "WhichProfessionalBodyControllerSpec" when {

    "get" must {
      "display view" when {

        "form data exists" in new Fixture {

          mockCacheFetch[Supervision](Some(Supervision(
            professionalBodies = Some(ProfessionalBodies(Set(AssociationOfBookkeepers, Other("SomethingElse"))))
          )))

          val result = controller.get()(request)

          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))

          document.title() must include(Messages("supervision.whichprofessionalbody.title"))
          document.select("input[value=12]").hasAttr("checked") must be(true)
          document.select("input[value=14]").hasAttr("checked") must be(true)
          document.select("input[name=specifyOtherBusiness]").`val`() must be("SomethingElse")

        }

        "form data is empty" in new Fixture {

          val result = controller.get()(request)

          status(result) must be(OK)

          Jsoup.parse(contentAsString(result)).title() must include(Messages("supervision.whichprofessionalbody.title"))

          val document = Jsoup.parse(contentAsString(result))

          document.title() must include(Messages("supervision.whichprofessionalbody.title"))

          document.select("input[type=checkbox]").hasAttr("checked") must be(false)
          document.select("input[name=specifyOtherBusiness]").`val`() must be(empty)
        }
      }
    }

    "post" when {

      "valid data" must {

        "redirect to PenalisedByProfessionalController" when {
          "not in edit mode" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "businessType[0]" -> "01",
              "businessType[1]" -> "02"
            )

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.PenalisedByProfessionalController.get().url))
          }
        }

        "redirect to SummaryController" when {
          "in edit mode" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "businessType[0]" -> "01",
              "businessType[1]" -> "02"
            )

            val result = controller.post(true)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.SummaryController.get().url))
          }
        }

      }

      "invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody()

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }

    }

  }

  it must {
    "save the valid data to the supervision model" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "businessType[0]" -> "01",
        "businessType[1]" -> "02"
      )

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)

      verify(controller.dataCacheConnector).save[Supervision](any(),eqTo(Supervision(
        professionalBodies = Some(ProfessionalBodies(Set(AccountingTechnicians, CharteredCertifiedAccountants))),
        hasChanged = true
      )))(any(),any(),any())

    }
  }

}
