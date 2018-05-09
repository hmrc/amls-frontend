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

package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.Country
import models.aboutthebusiness.AboutTheBusiness
import models.businessactivities.BusinessActivities
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.status.SubmissionReady
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.AmlsSpec
import utils.AuthorisedFixture
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class SummaryControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new SummaryController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }

    val testBusinessName = "Ubunchews Accountancy Services"

    val testReviewDetails = ReviewDetails(
      testBusinessName,
      Some(BusinessType.LimitedCompany),
      Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")),
      "XE0001234567890"
    )

    val testBusinessMatch = BusinessMatching(
      reviewDetails = Some(testReviewDetails)
    )

    val model = AboutTheBusiness(None, None, None, None)
  }

  "Get" must {

    "use correct services" in new Fixture {
      SummaryController.authConnector must be(AMLSAuthConnector)
      SummaryController.dataCache must be(DataCacheConnector)
    }

    "load the summary page when section data is available" in new Fixture {

      when(controller.dataCache.fetch[BusinessMatching](meq(BusinessMatching.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(testBusinessMatch)))

      when(controller.dataCache.fetch[AboutTheBusiness](meq(AboutTheBusiness.key))
        (any(), any(), any())).thenReturn(Future.successful(Some(model)))

      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      when(controller.dataCache.fetch[AboutTheBusiness](meq(AboutTheBusiness.key))
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCache.fetch[BusinessMatching](meq(BusinessMatching.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(testBusinessMatch)))

      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }

  }

  "post is called" must {
    "respond with OK and redirect to the registration progress page" when {

      "all questions are complete" in new Fixture {

        val emptyCache = CacheMap("", Map.empty)

        val newRequest = request.withFormUrlEncodedBody( "hasAccepted" -> "true")

        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(model.copy(hasAccepted = false))))

        when(controller.dataCache.save[AboutTheBusiness](meq(AboutTheBusiness.key), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))
      }

    }
  }
}
