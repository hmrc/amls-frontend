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
import forms.supervision.PenaltyDetailsFormProvider
import models.supervision.{ProfessionalBodyYes, Supervision}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.{AmlsSpec, DependencyMocks}
import views.html.supervision.PenaltyDetailsView

class PenaltyDetailsControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture extends DependencyMocks { self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[PenaltyDetailsView]
    val controller = new PenaltyDetailsController(
      dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[PenaltyDetailsFormProvider],
      view = view
    )

    mockCacheSave[Supervision]
  }

  "PenaltyDetailsController" must {

    "on get display the Penalty Details page" in new Fixture {

      mockCacheFetch[Supervision](None)

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(messages("supervision.penaltydetails.title"))
    }

    "on get display the Penalised By Professional Body page with pre populated data" in new Fixture {

      mockCacheFetch[Supervision](
        Some(
          Supervision(
            None,
            None,
            None,
            Some(ProfessionalBodyYes("details"))
          )
        )
      )

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include("details")

    }

    "on post with valid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.PenaltyDetailsController.post().url)
        .withFormUrlEncodedBody(
          "professionalBody" -> "details"
        )

      mockCacheFetch[Supervision](None)

      val result = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.supervision.routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.PenaltyDetailsController.post().url)
        .withFormUrlEncodedBody(
          "professionalBody" -> "§§§§"
        )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = FakeRequest(POST, routes.PenaltyDetailsController.post(true).url)
        .withFormUrlEncodedBody(
          "professionalBody" -> "details"
        )

      mockCacheFetch[Supervision](None)

      val result = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.supervision.routes.SummaryController.get().url))
    }
  }
}
