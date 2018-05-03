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

import models.businessmatching.updateservice.ChangeServicesAdd
import models.flowmanagement.ChangeServicesPageId
import play.api.test.Helpers._
import utils.{DependencyMocks, AmlsSpec}

import scala.concurrent.ExecutionContext.Implicits.global

class ChangeServicesRouterSpec extends AmlsSpec {

  trait Fixture extends DependencyMocks {

    val router = new ChangeServicesRouter

  }

  "getRoute" must {
    "return the 'Activities selection' page (SelectActivitiesController)" when {
      "given the 'register a service' model" in new Fixture {

        val result = router.getRoute(ChangeServicesPageId, ChangeServicesAdd)

        redirectLocation(result) mustBe Some(controllers.businessmatching.updateservice.add.routes.SelectActivitiesController.get().url)

      }
    }
  }

}
