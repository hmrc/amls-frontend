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

package controllers.businessmatching.updateservice.remove

import controllers.actions.SuccessfulAuthAction
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.businessmatching.BusinessActivity.MoneyServiceBusiness
import play.api.test.Helpers.{contentAsString, status}
import utils.{AmlsSpec, DependencyMocks}
import play.api.test.Helpers._
import views.html.businessmatching.updateservice.remove.UnableToRemoveActivityView

class UnableToRemoveBusinessTypesControllerSpec extends AmlsSpec {

  trait Fixture extends DependencyMocks {
    self =>

    val request    = addToken(authRequest)
    lazy val view  = app.injector.instanceOf[UnableToRemoveActivityView]
    val controller = new UnableToRemoveBusinessTypesController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      dataCacheConnector = mockCacheConnector,
      cc = mockMcc,
      view = view
    )
  }

  "get" when {
    "called with one activity" must {
      "return an OK status" when {
        "with the correct content" in new Fixture {

          val oneActivityBusiness = BusinessMatching(activities = Some(BusinessActivities(Set(MoneyServiceBusiness))))

          mockCacheFetch[BusinessMatching](Some(oneActivityBusiness), Some(BusinessMatching.key))

          val result = controller.get()(request)

          status(result) mustBe OK
          contentAsString(result) must include(
            messages(
              "businessmatching.updateservice.removeactivitiesinformation.heading",
              messages("businessmatching.registerservices.servicename.lbl.06.phrased")
            )
          )
        }
      }
    }

    "called with no activities" must {
      "return internal server error" in new Fixture {

        val noneActivityBusiness = BusinessMatching(activities = None)

        mockCacheFetch[BusinessMatching](Some(noneActivityBusiness), Some(BusinessMatching.key))

        val result = controller.get()(request)

        status(result) mustBe INTERNAL_SERVER_ERROR

      }
    }
  }
}
