/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import controllers.actions.SuccessfulAuthAction
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.BodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.AmlsSpec
import views.html.{unauthorised, unauthorised_role}

class AmlsControllerSpec extends AmlsSpec {

    trait UnauthenticatedFixture extends MockitoSugar {
      self =>

      implicit val unauthenticatedRequest = FakeRequest()
      val request = addToken(unauthenticatedRequest)
      lazy val view1 = app.injector.instanceOf[unauthorised]
      lazy val view2 = app.injector.instanceOf[unauthorised_role]
      val controller = new AmlsController(SuccessfulAuthAction,
        commonDependencies,
        mockMcc,
        messagesApi,
        mock[BodyParsers.Default],
        unauthorisedView = view1,
        unauthorisedRole = view2)
    }

    "AmlsController" must {
      "load the unauthorised page with an unauthenticated request" in new UnauthenticatedFixture {
          val result = controller.unauthorised(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("unauthorised.title"))
        }

      "load the unauthorised role with an unauthenticated request" in new UnauthenticatedFixture {
        val result = controller.unauthorised_role(request)
        status(result) mustBe UNAUTHORIZED
        contentAsString(result) must include(Messages("unauthorised.title"))
      }
    }
}
