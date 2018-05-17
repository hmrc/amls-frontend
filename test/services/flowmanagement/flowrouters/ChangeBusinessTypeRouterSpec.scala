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

import models.businessmatching.updateservice.{Add, Remove}
import models.flowmanagement.ChangeServicesPageId
import controllers.businessmatching.updateservice.remove.{routes => removeRoutes}
import controllers.businessmatching.updateservice.add.{routes => addRoutes}
import play.api.test.Helpers._
import utils.{AmlsSpec, DependencyMocks}

import scala.concurrent.ExecutionContext.Implicits.global

class ChangeBusinessTypeRouterSpec extends AmlsSpec {

  trait Fixture extends DependencyMocks {

    val router = new ChangeBusinessTypeRouter

  }

  "getRoute" must {
    "return the 'Activities selection' page (SelectActivitiesController)" when {
      "ChangeBusinessType is Add" +
        " and there is more than 1 business type" in new Fixture {

        val result = router.getRoute(ChangeServicesPageId, Add)

        redirectLocation(result) mustBe Some(addRoutes.SelectBusinessTypeController.get().url)

      }
    }

    "return the 'Which types to remove' page (RemoveBusinessTypesController)" when {
      "ChangeBusinessType is Remove" in new Fixture {

        val result = router.getRoute(ChangeServicesPageId, Remove)

        redirectLocation(result) mustBe Some(removeRoutes.RemoveBusinessTypesController.get().url)

      }
    }

    "return the 'unable to remove' page (UnableToRemoveBusinessTypesController)" when {
      "ChangeBusinessType is Remove" +
        " and there is only 1 business type" in new Fixture {

        val result = router.getRoute(ChangeServicesPageId, Remove)

        redirectLocation(result) mustBe Some(removeRoutes.UnableToRemoveBusinessTypesController.get().url)

      }
    }
  }

}
