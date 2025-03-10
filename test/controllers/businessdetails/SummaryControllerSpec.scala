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

package controllers.businessdetails

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails.BusinessDetails
import models.businessmatching.{BusinessMatching, BusinessType}
import models.status.SubmissionReady
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.Injecting
import services.StatusService
import services.cache.Cache
import utils.AmlsSpec
import utils.businessdetails.CheckYourAnswersHelper
import views.html.businessdetails.CheckYourAnswersView

import scala.concurrent.Future

class SummaryControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[CheckYourAnswersView]
    val controller = new SummaryController(
      dataCache = mock[DataCacheConnector],
      statusService = mock[StatusService],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      cyaHelper = inject[CheckYourAnswersHelper],
      view = view
    )

    val testBusinessName = "Ubunchews Accountancy Services"

    val testReviewDetails = ReviewDetails(
      testBusinessName,
      Some(BusinessType.LimitedCompany),
      Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")),
      "XE0001234567890"
    )

    val testBusinessMatch = BusinessMatching(
      reviewDetails = Some(testReviewDetails)
    )

    val model = BusinessDetails(None, None, None, None)
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {

      when(controller.dataCache.fetch[BusinessMatching](any(), meq(BusinessMatching.key))(any()))
        .thenReturn(Future.successful(Some(testBusinessMatch)))

      when(controller.dataCache.fetch[BusinessDetails](any(), meq(BusinessDetails.key))(any()))
        .thenReturn(Future.successful(Some(model)))

      when(
        controller.statusService
          .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
      )
        .thenReturn(Future.successful(SubmissionReady))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      when(controller.dataCache.fetch[BusinessDetails](any(), meq(BusinessDetails.key))(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCache.fetch[BusinessMatching](any(), meq(BusinessMatching.key))(any()))
        .thenReturn(Future.successful(Some(testBusinessMatch)))

      when(
        controller.statusService
          .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
      )
        .thenReturn(Future.successful(SubmissionReady))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }

  }

  "post is called" must {
    "respond with OK and redirect to the registration progress page" when {

      "all questions are complete" in new Fixture {

        val emptyCache = Cache.empty

        val newRequest = requestWithUrlEncodedBody("hasAccepted" -> "true")

        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any()))
          .thenReturn(Future.successful(Some(model.copy(hasAccepted = false))))

        when(controller.dataCache.save[BusinessDetails](any(), meq(BusinessDetails.key), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))
      }
    }
  }
}
