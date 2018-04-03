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

import cats.implicits._
import cats.data.OptionT
import models.businessmatching.{BillPaymentServices, BusinessActivity, HighValueDealing}
import models.flowmanagement.AddServiceFlowModel
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import org.mockito.Matchers.any
import org.mockito.Mockito.when

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SelectActivitiesControllerSpec extends GenericTestHelper {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    val request = addToken(authRequest)

    val controller = new SelectActivitiesController(
      authConnector,
      mockStatusService,
      mockCacheConnector,
      mock[BusinessMatchingService]
    )
  }

  "get" must {
    "return the view" in new Fixture {
      mockCacheFetch[AddServiceFlowModel](None)

      when {
        controller.businessMatchingService.getSubmittedBusinessActivities(any(), any(), any())
      } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(BillPaymentServices))

      val result = controller.get()(request)

      status(result) mustBe OK
    }
  }

}
