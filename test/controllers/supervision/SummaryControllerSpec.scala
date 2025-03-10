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
import models.asp.Asp
import models.supervision.{Supervision, SupervisionValues}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.Injecting
import services.cache.Cache
import utils.supervision.CheckYourAnswersHelper
import utils.{AmlsSpec, DependencyMocks}
import views.html.supervision.CheckYourAnswersView

import scala.concurrent.Future

class SummaryControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks with SupervisionValues {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = app.injector.instanceOf[CheckYourAnswersView]
    val controller = new SummaryController(
      mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      cyaHelper = inject[CheckYourAnswersHelper],
      view = view,
      error = errorView
    )

    val model = Supervision(None)
  }

  "Get" must {
    "load the summary page when section data is available and section is complete" in new Fixture {

      when(controller.dataCacheConnector.fetch[Supervision](any(), any())(any()))
        .thenReturn(Future.successful(Some(completeModel)))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main summary page when section data is available but incomplete" in new Fixture {
      when(controller.dataCacheConnector.fetch[Supervision](any(), any())(any()))
        .thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {
      when(controller.dataCacheConnector.fetch[Asp](any(), any())(any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }

  "POST" must {
    "update the hasAccepted flag on the model" in new Fixture {
      val cache = mock[Cache]

      when {
        controller.dataCacheConnector.fetch[Supervision](any(), any())(any())
      } thenReturn Future.successful(Some(model.copy(hasAccepted = false)))

      when {
        controller.dataCacheConnector.save[Supervision](any(), eqTo(Supervision.key), any())(any())
      } thenReturn Future.successful(cache)

      val result = controller.post()(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)

      val captor = ArgumentCaptor.forClass(classOf[Supervision])
      verify(controller.dataCacheConnector).save[Supervision](any(), eqTo(Supervision.key), captor.capture())(any())
      captor.getValue.hasAccepted mustBe true
    }
  }
}
