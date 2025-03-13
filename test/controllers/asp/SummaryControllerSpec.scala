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

package controllers.asp

import controllers.actions.SuccessfulAuthAction
import models.asp.Asp
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.{AmlsSpec, DependencyMocks}
import views.html.asp.SummaryView

import scala.concurrent.{ExecutionContext, Future}

class SummaryControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends DependencyMocks {
    self =>
    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)

    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    lazy val summaryView: SummaryView = app.injector.instanceOf[SummaryView]
    val controller                    = new SummaryController(
      mockCacheConnector,
      mockServiceFlow,
      mockStatusService,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      summaryView
    )

    mockCacheSave[Asp]

    when {
      mockStatusService.isPreSubmission(any(), any(), any())(any(), any(), any())
    } thenReturn Future.successful(true)
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {

      val model: Asp = Asp(None, None)

      mockCacheFetch[Asp](Some(model))

      val result: Future[Result] = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      mockCacheFetch[Asp](None)

      val result: Future[Result] = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }

  "Post" must {
    "load the Asp model and set hasAccepted to true" in new Fixture {
      val postRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        FakeRequest(POST, routes.SummaryController.post().url).withFormUrlEncodedBody("" -> "")

      val model: Asp = Asp(None, None)
      mockCacheFetch(Some(model))

      val result: Future[Result] = controller.post()(postRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)

      verify(mockCacheConnector).save[Asp](any(), eqTo(Asp.key), eqTo(model.copy(hasAccepted = true)))(any())
    }
  }
}
