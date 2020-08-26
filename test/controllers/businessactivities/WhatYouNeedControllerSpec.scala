/*
 * Copyright 2020 HM Revenue & Customs
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

import controllers.actions.SuccessfulAuthAction
import models.businessmatching.{AccountancyServices, BusinessActivities, BusinessMatching}
import models.status.{ReadyForRenewal, RenewalSubmitted, SubmissionDecisionApproved, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import views.html.businessactivities.what_you_need

import scala.concurrent.Future

class WhatYouNeeControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends DependencyMocks {
    self => val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[what_you_need]
    val controller = new WhatYouNeedController(
      dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      statusService = mockStatusService,
      authConnector = mock[AuthConnector],
      cc = mockMcc,
      what_you_need = view)
  }

  "WhatYouNeedController" must {
    "get" must {
      "redirect to InvolvedInOtherController" when {
        "creating a new submission" in new Fixture {
          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(BusinessMatching(None, Some(BusinessActivities(Set(AccountancyServices))), None, None, None, None))))

          mockApplicationStatus(SubmissionReadyForReview)

          val result = controller.get(request)
          status(result) must be(OK)

          val doc = Jsoup.parse(contentAsString(result))

          doc.getElementById("ba-whatyouneed-button").attr("href") mustBe routes.InvolvedInOtherController.get().url
        }

        "performing a variation" in new Fixture {
          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(BusinessMatching(None, Some(BusinessActivities(Set(AccountancyServices))), None, None, None, None))))

          mockApplicationStatus(SubmissionDecisionApproved)

          val result = controller.get(request)
          status(result) must be(OK)

          val doc = Jsoup.parse(contentAsString(result))

          doc.getElementById("ba-whatyouneed-button").attr("href") mustBe routes.InvolvedInOtherController.get().url
        }

        "in a renewal pending status" in new Fixture {
          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(BusinessMatching(None, Some(BusinessActivities(Set(AccountancyServices))), None, None, None, None))))

          mockApplicationStatus(ReadyForRenewal(None))

          val result = controller.get(request)
          status(result) must be(OK)

          val doc = Jsoup.parse(contentAsString(result))

          doc.getElementById("ba-whatyouneed-button").attr("href") mustBe routes.InvolvedInOtherController.get().url
        }

        "in a renewal submitted status" in new Fixture {
          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(BusinessMatching(None, Some(BusinessActivities(Set(AccountancyServices))), None, None, None, None))))

          mockApplicationStatus(RenewalSubmitted(None))

          val result = controller.get(request)
          status(result) must be(OK)

          val doc = Jsoup.parse(contentAsString(result))

          doc.getElementById("ba-whatyouneed-button").attr("href") mustBe routes.InvolvedInOtherController.get().url
        }
      }
      "Throw an error" when {
        "bm details cannot be fetched" in new Fixture {
          when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any(), any()))
            .thenReturn(Future.successful(None))
          mockApplicationStatus(SubmissionReadyForReview)

          val result = controller.get(request)
          status(result) must be(INTERNAL_SERVER_ERROR)
        }
      }
    }
  }
}
