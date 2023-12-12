/*
 * Copyright 2023 HM Revenue & Customs
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

import config.ApplicationConfig
import controllers.actions.SuccessfulAuthAction
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.AmlsSpec
import views.html.submission._

import scala.concurrent.Future

class SubmissionErrorControllerSpec extends AmlsSpec with ScalaFutures with Injecting {

  implicit lazy val config: ApplicationConfig = inject[ApplicationConfig]

  val controller: SubmissionErrorController = new SubmissionErrorController(
    SuccessfulAuthAction,
    commonDependencies,
    mockMcc,
    inject[DuplicateEnrolmentView],
    inject[DuplicateSubmissionView],
    inject[WrongCredentialTypeView],
    inject[BadRequestView]
  )

  def contactUrl(str: String) =
    s"http://localhost:9250/contact/report-technical-problem?service=AMLS&referrerUrl=http%3A%2F%2Flocalhost%3A9222%2Fanti-money-laundering%2Fsubscribe%2F$str"

  def getLink(res: Future[Result]) = Jsoup.parse(contentAsString(res)).getElementById("report-link")

  "SubmissionErrorController" when {

    "duplicateEnrolment is called" must {

      "render the correct page and link to contact-frontend" in {

        val result = controller.duplicateEnrolment()(
          FakeRequest("GET", controllers.routes.SubmissionErrorController.duplicateEnrolment().url)
        )

        status(result) mustBe OK

        getLink(result).attr("href") mustBe contactUrl("duplicate-enrolment")
      }
    }

    "duplicateSubmission is called" must {

      "render the correct page and link to contact-frontend" in {

        val result = controller.duplicateSubmission()(
          FakeRequest("GET", controllers.routes.SubmissionErrorController.duplicateSubmission().url)
        )

        status(result) mustBe OK
        getLink(result).attr("href") mustBe contactUrl("duplicate-submission")
      }
    }

    "wrongCredentialType is called" must {

      "render the correct page and link to contact-frontend" in {

        val result = controller.wrongCredentialType()(
          FakeRequest("GET", controllers.routes.SubmissionErrorController.wrongCredentialType().url)
        )

        status(result) mustBe OK
        getLink(result).attr("href") mustBe contactUrl("wrong-credential-type")
      }
    }

    "badRequest is called" must {

      "render the correct page and link to contact-frontend" in {

        val result = controller.badRequest()(
          FakeRequest("GET", controllers.routes.SubmissionErrorController.badRequest().url)
        )

        status(result) mustBe OK
        getLink(result).attr("href") mustBe contactUrl("bad-request")
      }
    }
  }
}
