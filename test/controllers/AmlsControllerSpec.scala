/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.GenericTestHelper

class AmlsControllerSpec extends GenericTestHelper {

    trait UnauthenticatedFixture extends MockitoSugar {
      self =>

      implicit val unauthenticatedRequest = FakeRequest()
      val request = addToken(unauthenticatedRequest)
      val mockAuthConnector = mock[AuthConnector]

      val controller = new AmlsController {
        override protected def authConnector: AuthConnector = mockAuthConnector
      }
    }

    "AmlsController" must {
      "load the unauthorised page with an unauthenticated request" in new UnauthenticatedFixture {
          val result = controller.unauthorised(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("unauthorised.title"))
        }

      "load the unauthorised role with an unauthenticated request" in new UnauthenticatedFixture {
        val result = controller.unauthorised_role(request)
        status(result) mustBe OK
        contentAsString(result) must include(Messages("unauthorised.title"))
      }
    }
}
