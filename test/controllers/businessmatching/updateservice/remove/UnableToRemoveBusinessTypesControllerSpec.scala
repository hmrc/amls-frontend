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

package controllers.businessmatching.updateservice.remove

import models.businessmatching.{BusinessActivities, BusinessMatching, MoneyServiceBusiness}
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, status}
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import play.api.test.Helpers._

class UnableToRemoveBusinessTypesControllerSpec extends AmlsSpec {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val controller = new UnableToRemoveBusinessTypesController(
      authConnector = self.authConnector,
      dataCacheConnector = mockCacheConnector
    )
  }

  "get" when {
    "called with one activity" must {
      "return an OK status" when {
        "with the correct content" in new Fixture {

          val oneActivityBusiness = BusinessMatching(activities = Some(BusinessActivities(Set(MoneyServiceBusiness))))

          mockCacheFetch[BusinessMatching](
            Some(oneActivityBusiness),
            Some(BusinessMatching.key))

          val result = controller.get()(request)

          status(result) mustBe OK
          contentAsString(result) must include(Messages("businessmatching.updateservice.removeactivitiesinformation.heading", Messages("businessmatching.registerservices.servicename.lbl.05")))
        }
      }
    }

    "called with no activities" must {
      "return internal server error" in new Fixture {

        val noneActivityBusiness = BusinessMatching(activities = None)

        mockCacheFetch[BusinessMatching](
          Some(noneActivityBusiness),
          Some(BusinessMatching.key))

        val result = controller.get()(request)

        status(result) mustBe INTERNAL_SERVER_ERROR

      }

    }
  }

}