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
import forms.supervision.WhichProfessionalBodyFormProvider
import models.supervision.ProfessionalBodies._
import models.supervision._
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import views.html.supervision.WhichProfessionalBodyView

class WhichProfessionalBodyControllerSpec extends PlaySpec with AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks with AuthorisedFixture { self =>

    val request    = addToken(authRequest)
    lazy val view  = inject[WhichProfessionalBodyView]
    val controller = new WhichProfessionalBodyController(
      mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[WhichProfessionalBodyFormProvider],
      view = view
    )
    mockCacheFetch[Supervision](Some(Supervision()))
    mockCacheSave[Supervision]
  }

  "WhichProfessionalBodyControllerSpec" when {

    "get" must {
      "display view" when {

        "form data exists" in new Fixture {

          mockCacheFetch[Supervision](
            Some(
              Supervision(
                professionalBodies = Some(ProfessionalBodies(Set(AssociationOfBookkeepers, Other("SomethingElse"))))
              )
            )
          )

          val result = controller.get()(request)

          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))

          document.title()                                                                         must include(messages("supervision.whichprofessionalbody.title"))
          document.select(s"input[value=${AssociationOfBookkeepers.toString}]").hasAttr("checked") must be(true)
          document.select(s"input[value=${Other("").toString}]").hasAttr("checked")                must be(true)
          document.select("input[name=specifyOtherBusiness]").`val`()                              must be("SomethingElse")

        }

        "form data is empty" in new Fixture {

          val result = controller.get()(request)

          status(result) must be(OK)

          Jsoup.parse(contentAsString(result)).title() must include(messages("supervision.whichprofessionalbody.title"))

          val document = Jsoup.parse(contentAsString(result))

          document.title() must include(messages("supervision.whichprofessionalbody.title"))

          document.select("input[type=checkbox]").hasAttr("checked")  must be(false)
          document.select("input[name=specifyOtherBusiness]").`val`() must be(empty)
        }
      }
    }

    "post" when {

      "valid data" must {

        "redirect to PenalisedByProfessionalController" when {
          "not in edit mode" in new Fixture {

            val newRequest = FakeRequest(POST, routes.WhichProfessionalBodyController.post().url)
              .withFormUrlEncodedBody(
                "businessType[0]" -> AccountingTechnicians.toString,
                "businessType[1]" -> CharteredCertifiedAccountants.toString
              )

            val result = controller.post()(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.PenalisedByProfessionalController.get().url))
          }
        }

        "redirect to SummaryController" when {
          "in edit mode" in new Fixture {

            val newRequest = FakeRequest(POST, routes.WhichProfessionalBodyController.post().url)
              .withFormUrlEncodedBody(
                "businessType[0]" -> AccountingTechnicians.toString,
                "businessType[1]" -> CharteredCertifiedAccountants.toString
              )

            val result = controller.post(true)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.SummaryController.get().url))
          }
        }
      }

      "invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {

          val newRequest = FakeRequest(POST, routes.WhichProfessionalBodyController.post().url)
            .withFormUrlEncodedBody("" -> "")

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }
    }
  }

  it must {
    "save the valid data to the supervision model" in new Fixture {

      val newRequest = FakeRequest(POST, routes.WhichProfessionalBodyController.post().url)
        .withFormUrlEncodedBody(
          "businessType[0]" -> AccountingTechnicians.toString,
          "businessType[1]" -> CharteredCertifiedAccountants.toString
        )

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)

      verify(controller.dataCacheConnector).save[Supervision](
        any(),
        any(),
        eqTo(
          Supervision(
            professionalBodies = Some(ProfessionalBodies(Set(AccountingTechnicians, CharteredCertifiedAccountants))),
            hasChanged = true
          )
        )
      )(any())
    }
  }
}
