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

package services.flowmanagement.routing

import models.businessmatching.updateservice.{ChangeServices, ChangeServicesAdd}
import models.flowmanagement.WhatDoYouWantToDoPageId
import org.scalatestplus.play.PlaySpec
import services.flowmanagement.Router
import services.flowmanagement.routings._
import play.api.test.Helpers._

class ChangeServicesRouterSpec extends PlaySpec {

  trait Fixture {

    val router = ChangeServicesRouter.router

  }

  "getRoute" must {
    "return the 'service selection' page" when {
      "given the 'register a service' model" in new Fixture {

        val result = router.getRoute(WhatDoYouWantToDoPageId, ChangeServicesAdd)

        redirectLocation(result) mustBe Some(controllers.businessmatching.updateservice.add.routes.SelectActivitiesController.get().url)

      }
    }
  }

}
