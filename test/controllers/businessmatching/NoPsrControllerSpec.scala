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

package controllers.businessmatching

import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import play.api.mvc.Results.Ok

//noinspection ScalaStyle
class NoPsrControllerSpec extends GenericTestHelper with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)

    lazy val controller = new NoPsrController {
      override protected def authConnector = self.authConnector

      override protected[businessmatching] lazy val statusService = self.mockStatusService
    }

  }

  "get" when {
    "called" must {
      "return an OK status" when {
        "application status is pre-application" in new Fixture {
          mockApplicationStatus(NotCompleted)

          val result = controller.get()(request)

          status(result) mustBe OK
          contentAsString(result) must include(Messages("businessmatching.cannotcontinuewiththeapplication.title"))
        }

        "application status is beyond pre-application" in new Fixture {
          mockApplicationStatus(SubmissionDecisionApproved)

          val result = controller.get()(request)
          status(result) mustBe OK
          contentAsString(result) must include(Messages("businessmatching.cannotchangeservices.title"))
        }
      }
    }
  }
}
