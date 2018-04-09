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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import models.businessmatching._
import models.flowmanagement.AddServiceFlowModel
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class SelectActivitiesControllerSpec extends GenericTestHelper {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)
    val mockBusinessMatchingService = mock[BusinessMatchingService]

    val controller = new SelectActivitiesController(
      self.authConnector,
      mockCacheConnector,
      createRouter[AddServiceFlowModel],
      mockBusinessMatchingService
    )

    when {
      controller.businessMatchingService.getModel(any(), any(), any())
    } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
      activities = Some(BusinessActivities(Set(BillPaymentServices)))
    ))

    when {
      controller.businessMatchingService.getSubmittedBusinessActivities(any(), any(), any())
    } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(BillPaymentServices))

    mockCacheFetch(Some(AddServiceFlowModel(Some(BillPaymentServices), Some(true))), Some(AddServiceFlowModel.key))
  }

  "SelectActivitiesController" when {

    "get is called" must {
      "return OK with select_activities view" in new Fixture {

        val result = controller.get()(request)

        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("businessmatching.updateservice.selectactivities.title"))
      }
    }

    "post" must {
      "return a bad request when no data has been posted" in new Fixture {

        val result = controller.post()(request.withFormUrlEncodedBody())

        status(result) mustBe BAD_REQUEST
      }

      "return the next page in the flow when valid data has been posted" in new Fixture {
        mockCacheUpdate(Some(AddServiceFlowModel.key), AddServiceFlowModel())
        mockCacheSave[AddServiceFlowModel](AddServiceFlowModel(Some(HighValueDealing)), Some(AddServiceFlowModel.key))

        val result = controller.post()(request.withFormUrlEncodedBody(
          "businessActivities[]" -> "04"
        ))

        status(result) mustBe SEE_OTHER
      }
    }
  }
}
