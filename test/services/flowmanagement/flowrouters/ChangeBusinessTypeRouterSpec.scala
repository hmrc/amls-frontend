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

package services.flowmanagement.flowrouters

import cats.data.OptionT
import cats.implicits._
import controllers.businessmatching.updateservice.add.{routes => addRoutes}
import controllers.businessmatching.updateservice.remove.{routes => removeRoutes}
import models.businessmatching._
import models.businessmatching.updateservice.{Add, Remove}
import models.flowmanagement.ChangeBusinesTypesPageId
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.mvc.Results.Redirect
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import utils.{AmlsSpec, DependencyMocks}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ChangeBusinessTypeRouterSpec extends AmlsSpec {

  trait Fixture extends DependencyMocks {

    val mockBusinessMatchingService = mock[BusinessMatchingService]

    val router = new ChangeBusinessTypeRouter(mockBusinessMatchingService)

    when {
      mockBusinessMatchingService.getModel(any(), any(), any())
    } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
      activities = Some(BusinessActivities(Set(BillPaymentServices, MoneyServiceBusiness)))
    ))
  }

  "getRoute" must {
    "return the 'BusinessTypes selection' page (SelectBusinessTypeController)" when {
      "the user is on the 'What do you want to do' page (ChangeServicesPageId) and selects Add" in new Fixture {

        val result = router.getRoute(ChangeBusinesTypesPageId, Add)

        redirectLocation(result) mustBe Some(addRoutes.SelectBusinessTypeController.get().url)

      }
    }

    "return the 'What do you want to remove' page (RemoveBusinessTypesController)" when {
      "the user is on the 'What do you want to do' page (ChangeServicesPageId)" +
        " and selects Remove and has more than one Business Type" in new Fixture {

        val result = await(router.getRoute(ChangeBusinesTypesPageId, Remove))

        result mustBe Redirect(removeRoutes.RemoveBusinessTypesController.get())
      }
    }

    "return the 'Unable to remove' page (UnableToRemoveBusinessTypesController)" when {
      "the user is on the 'What do you want to do' page (ChangeServicesPageId)" +
        " and selects Remove but has only one Business Type" in new Fixture {

        when {
          mockBusinessMatchingService.getModel(any(), any(), any())
        } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
          activities = Some(BusinessActivities(Set(BillPaymentServices)))
        ))

        val result = await(router.getRoute(ChangeBusinesTypesPageId, Remove))

        result mustBe Redirect(removeRoutes.UnableToRemoveBusinessTypesController.get())
      }
    }
  }
}
