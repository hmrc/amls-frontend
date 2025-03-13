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

package controllers.deregister

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.deregister.DeregistrationReasonFormProvider
import models.businessmatching.BusinessActivity.HighValueDealing
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.deregister.DeregistrationReason
import models.deregister.DeregistrationReason.HVDPolicyOfNotAcceptingHighValueCashPayments
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.AuthEnrolmentsService
import services.cache.Cache
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.deregister.DeregistrationReasonView

import scala.concurrent.Future

class DeregistrationReasonControllerSpec extends AmlsSpec with Injecting {

  trait TestFixture extends AuthorisedFixture {
    self =>

    val request            = addToken(authRequest)
    val authService        = mock[AuthEnrolmentsService]
    val dataCacheConnector = mock[DataCacheConnector]
    lazy val view          = inject[DeregistrationReasonView]
    lazy val controller    = new DeregistrationReasonController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      dataCacheConnector = dataCacheConnector,
      cc = mockMcc,
      formProvider = inject[DeregistrationReasonFormProvider],
      view = view
    )
  }

  "get" should {
    "display list of all reasons" in new TestFixture {

      val businessMatchingWithHvd = BusinessMatching(
        activities = Some(BusinessActivities(Set(HighValueDealing)))
      )

      when(
        dataCacheConnector.fetch[BusinessMatching](credId = any(), key = eqTo(BusinessMatching.key))(formats = any())
      )
        .thenReturn(Future.successful(Some(businessMatchingWithHvd)))

      val result: Future[Result] = controller.get()(request)

      status(result)          must be(OK)
      contentAsString(result) must include(messages("deregistration.reason.heading"))
      val document = Jsoup.parse(contentAsString(result))
      DeregistrationReason.all foreach { reason =>
        document.getElementById(reason.toString).hasAttr("checked") must be(false)
      }
      document.getElementById("specifyOtherReason").`val`() must be("")
    }
  }

  "display list of reasons without HVD (when it is not present in business activities)" in new TestFixture {
    val businessMatchingWithoutHvd = BusinessMatching(
      activities = Some(BusinessActivities(Set()))
    )

    when(dataCacheConnector.fetch[BusinessMatching](credId = any(), key = eqTo(BusinessMatching.key))(formats = any()))
      .thenReturn(Future.successful(Some(businessMatchingWithoutHvd)))

    val result: Future[Result] = controller.get()(request)

    status(result)          must be(OK)
    contentAsString(result) must include(messages("deregistration.reason.heading"))
    val document = Jsoup.parse(contentAsString(result))
    DeregistrationReason.all diff Seq(HVDPolicyOfNotAcceptingHighValueCashPayments) foreach { reason =>
      document.getElementById(reason.toString).hasAttr("checked") must be(false)
    }
    document.getElementById("specifyOtherReason").`val`() must be("")
  }

  "submitting should cache user selection and navigate to the CYA page" in new TestFixture {
    val deregistrationReason: DeregistrationReason = DeregistrationReason.OutOfScope

    when(
      dataCacheConnector.save[DeregistrationReason](
        credId = any(),
        key = eqTo(DeregistrationReason.key),
        data = eqTo(deregistrationReason)
      )(any())
    )
      .thenReturn(Future.successful(mock[Cache]))

    val postRequest = FakeRequest(POST, routes.DeregistrationReasonController.post().url)
      .withFormUrlEncodedBody(
        "deregistrationReason" -> deregistrationReason.toString
      )

    val result = controller.post()(postRequest)
    status(result)           must be(SEE_OTHER)
    redirectLocation(result) must be(
      Some(controllers.deregister.routes.DeregistrationCheckYourAnswersController.get.url)
    )
  }

  "submitting without selection results in bad request" in new TestFixture {

    val businessMatchingWithoutHvd = BusinessMatching(
      activities = Some(BusinessActivities(Set()))
    )
    when(dataCacheConnector.fetch[BusinessMatching](credId = any(), key = eqTo(BusinessMatching.key))(formats = any()))
      .thenReturn(Future.successful(Some(businessMatchingWithoutHvd)))

    val postRequest = FakeRequest(POST, routes.DeregistrationReasonController.post().url)
      .withFormUrlEncodedBody(
        // empty body
      )

    val result = controller.post()(postRequest)
    status(result) must be(BAD_REQUEST)
    val content: String = contentAsString(result)
    content must include(messages("deregistration.reason.heading"))
    content must include("Select why you’re deregistering the business")
  }
}
