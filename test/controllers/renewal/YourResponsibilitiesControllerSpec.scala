/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.renewal

import controllers.actions.SuccessfulAuthAction
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, BodyParsers}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.AmlsSpec
import views.html.renewal.YourResponsibilitiesView

class YourResponsibilitiesControllerSpec extends AmlsSpec {

  trait UnauthenticatedFixture extends MockitoSugar {
    self =>

    implicit val unauthenticatedRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val request                                                              = addToken(unauthenticatedRequest)
    lazy val view                                                           = app.injector.instanceOf[YourResponsibilitiesView]
    val controller                                                           = new YourResponsibilitiesController(
      SuccessfulAuthAction,
      commonDependencies,
      mockMcc,
      messagesApi,
      mock[BodyParsers.Default],
      view = view,
    )
  }

  "YourResponsibilitiesController" must {
    "load the unauthorised page with an unauthenticated request" in new UnauthenticatedFixture {
      val result = controller.get(request)
      status(result)          must be(OK)
      contentAsString(result) must include(messages("unauthorised.title"))
    }

    "load the unauthorised role with an unauthenticated request" in new UnauthenticatedFixture {
      val result = controller.get(request)
      status(result) mustBe UNAUTHORIZED
      contentAsString(result) must include(messages("unauthorised.title"))
    }
  }
}
