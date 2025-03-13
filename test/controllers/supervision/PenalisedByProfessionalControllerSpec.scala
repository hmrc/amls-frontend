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
import forms.supervision.PenalisedByProfessionalFormProvider
import models.supervision.{ProfessionalBodyYes, Supervision}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.{AmlsSpec, DependencyMocks}
import views.html.supervision.PenalisedByProfessionalView

class PenalisedByProfessionalControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with ScalaFutures
    with Injecting
    with BeforeAndAfterEach {

  trait Fixture extends DependencyMocks { self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[PenalisedByProfessionalView]
    val controller = new PenalisedByProfessionalController(
      dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[PenalisedByProfessionalFormProvider],
      view = view
    )

    reset(mockCacheConnector)
    mockCacheSave[Supervision]
  }

  "PenalisedByProfessionalController" must {

    "on get display the Penalised By Professional Body page" in new Fixture {

      mockCacheFetch[Supervision](None)

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(messages("supervision.penalisedbyprofessional.title"))
    }

    "on get display the Penalised By Professional Body page with pre populated data" in new Fixture {

      mockCacheFetch[Supervision](
        Some(
          Supervision(
            None,
            None,
            None,
            Some(ProfessionalBodyYes(""))
          )
        )
      )

      val result = controller.get()(request)
      status(result) must be(OK)
      Jsoup.parse(contentAsString(result)).getElementById("penalised").hasAttr("checked") mustBe true
    }

    "redirect to Penalty Details page" when {

      "answer is yes on post with valid data" in new Fixture {

        val newRequest = FakeRequest(POST, routes.PenalisedByProfessionalController.post().url)
          .withFormUrlEncodedBody("penalised" -> "true")

        mockCacheFetch[Supervision](None)

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.supervision.routes.PenaltyDetailsController.get().url))
      }

      "answer is yes on post with valid data in edit mode" in new Fixture {

        val newRequest = FakeRequest(POST, routes.PenalisedByProfessionalController.post(true).url)
          .withFormUrlEncodedBody("penalised" -> "true")

        mockCacheFetch[Supervision](None)

        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.supervision.routes.PenaltyDetailsController.get(true).url))
      }
    }

    "redirect to Summary page" when {

      "answer is no on post with valid data" in new Fixture {

        val newRequest = FakeRequest(POST, routes.PenalisedByProfessionalController.post().url)
          .withFormUrlEncodedBody("penalised" -> "false")

        mockCacheFetch[Supervision](None)

        val result = controller.post()(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.supervision.routes.SummaryController.get().url))
      }

      "answer is no on post with valid data in edit mode" in new Fixture {

        val newRequest = FakeRequest(POST, routes.PenalisedByProfessionalController.post(true).url)
          .withFormUrlEncodedBody("penalised" -> "false")

        mockCacheFetch[Supervision](None)

        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.supervision.routes.SummaryController.get().url))
      }

      "when answer given is the same as previously cached answer" in new Fixture {

        val newRequest = FakeRequest(POST, routes.PenalisedByProfessionalController.post(true).url)
          .withFormUrlEncodedBody("penalised" -> "true")

        mockCacheFetch[Supervision](Some(Supervision(None, None, None, Some(ProfessionalBodyYes("description")))))

        val result = controller.post(true)(newRequest)
        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.supervision.routes.SummaryController.get().url))

        verify(mockCacheConnector, never()).save(any(), any(), any())(any())
      }
    }

    "on post with invalid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.PenalisedByProfessionalController.post().url)
        .withFormUrlEncodedBody("penalised" -> "details")

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
    }
  }
}
