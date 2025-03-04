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

package controllers.withdrawal

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.withdrawal.WithdrawalReasonFormProvider
import models.withdrawal.WithdrawalReason
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.AuthEnrolmentsService
import services.cache.Cache
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.withdrawal.WithdrawalReasonView

import scala.concurrent.Future

class WithdrawalReasonControllerSpec extends AmlsSpec with Injecting {

  trait TestFixture extends AuthorisedFixture {
    self =>

    val request            = addToken(authRequest)
    val authService        = mock[AuthEnrolmentsService]
    val dataCacheConnector = mock[DataCacheConnector]
    lazy val view          = inject[WithdrawalReasonView]
    lazy val controller    = new WithdrawalReasonController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      enrolments = authService,
      dataCacheConnector = dataCacheConnector,
      cc = mockMcc,
      formProvider = inject[WithdrawalReasonFormProvider],
      view = view
    )
  }

  "get" should {
    "display list of all withdrawal reasons" in new TestFixture {

      val result: Future[Result] = controller.get()(request)

      status(result)          must be(OK)
      contentAsString(result) must include(messages("withdrawal.reason.heading"))
      val document = Jsoup.parse(contentAsString(result))
      WithdrawalReason.all foreach { reason =>
        document.getElementById(reason.toString).hasAttr("checked") must be(false)
      }
      document.getElementById("specifyOtherReason").`val`() must be("")
    }
  }

  "submitting should cache user selection and navigate to the CYA page" in new TestFixture {
    val withdrawalReason: WithdrawalReason = WithdrawalReason.OutOfScope

    when(
      dataCacheConnector
        .save[WithdrawalReason](credId = any(), key = eqTo(WithdrawalReason.key), data = eqTo(withdrawalReason))(any())
    )
      .thenAnswer { invocation =>
        Future.successful(mock[Cache])
      }

    val postRequest = FakeRequest(POST, routes.WithdrawalReasonController.post().url)
      .withFormUrlEncodedBody(
        "withdrawalReason" -> withdrawalReason.toString
      )

    val result = controller.post()(postRequest)

    status(result) must be(SEE_OTHER)

    val redirectLoc = redirectLocation(result)
    redirectLoc must be(Some(controllers.withdrawal.routes.WithdrawalCheckYourAnswersController.get.url))
  }

  "submitting without selection results in bad request" in new TestFixture {

    val postRequest = FakeRequest(POST, routes.WithdrawalReasonController.post().url)
      .withFormUrlEncodedBody()

    val result = controller.post()(postRequest)
    status(result) must be(BAD_REQUEST)
    val content: String = contentAsString(result)
    content must include(messages("withdrawal.reason.heading"))
    content must include("Select why you’re withdrawing your application")
  }

  "submitting with 'Other' reason should include the specified reason" in new TestFixture {
    val withdrawalReason: WithdrawalReason = WithdrawalReason.Other("some reason")

    when(
      dataCacheConnector
        .save[WithdrawalReason](credId = any(), key = eqTo(WithdrawalReason.key), data = eqTo(withdrawalReason))(any())
    )
      .thenAnswer { invocation =>
        Future.successful(mock[Cache])
      }

    val postRequest = FakeRequest(POST, routes.WithdrawalReasonController.post().url)
      .withFormUrlEncodedBody(
        "withdrawalReason"   -> WithdrawalReason.Other("some reason").toString,
        "specifyOtherReason" -> "some reason"
      )

    val result = controller.post()(postRequest)
    status(result) must be(SEE_OTHER)

    val redirectLoc = redirectLocation(result)
    redirectLoc must be(Some(controllers.withdrawal.routes.WithdrawalCheckYourAnswersController.get.url))
  }
}
