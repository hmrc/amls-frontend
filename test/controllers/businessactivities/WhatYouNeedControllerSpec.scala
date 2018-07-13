/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.businessactivities

import models.status.{ReadyForRenewal, RenewalSubmitted, SubmissionDecisionApproved, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}

class WhatYouNeeControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)
    val controller = new WhatYouNeedController(self.authConnector, mockStatusService)
  }

  "WhatYouNeedController" must {
    "get" must {
      "load the page with the correct 'next page' link" when {
        "creating a new submission" in new Fixture {
          mockApplicationStatus(SubmissionReadyForReview)

          val result = controller.get(request)
          status(result) must be(OK)

          val doc = Jsoup.parse(contentAsString(result))

          doc.getElementById("ba-whatyouneed-button").attr("href") mustBe routes.InvolvedInOtherController.get().url
        }

        "performing a variation" in new Fixture {
          mockApplicationStatus(SubmissionDecisionApproved)

          val result = controller.get(request)
          status(result) must be(OK)

          val doc = Jsoup.parse(contentAsString(result))

          doc.getElementById("ba-whatyouneed-button").attr("href") mustBe routes.BusinessFranchiseController.get().url
        }

        "in a renewal pending status" in new Fixture {
          mockApplicationStatus(ReadyForRenewal(None))

          val result = controller.get(request)
          status(result) must be(OK)

          val doc = Jsoup.parse(contentAsString(result))

          doc.getElementById("ba-whatyouneed-button").attr("href") mustBe routes.BusinessFranchiseController.get().url
        }

        "in a renewal submitted status" in new Fixture {
          mockApplicationStatus(RenewalSubmitted(None))

          val result = controller.get(request)
          status(result) must be(OK)

          val doc = Jsoup.parse(contentAsString(result))

          doc.getElementById("ba-whatyouneed-button").attr("href") mustBe routes.BusinessFranchiseController.get().url
        }

      }

    }
  }
}
