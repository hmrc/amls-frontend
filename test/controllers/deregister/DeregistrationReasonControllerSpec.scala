/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.deregister

import connectors.{AmlsConnector, DataCacheConnector}
import org.mockito.Matchers.{eq => eqTo}
import org.scalatestplus.play.OneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, StatusService}
import utils.{AuthorisedFixture, GenericTestHelper}

class DeregistrationReasonControllerSpec extends GenericTestHelper with OneAppPerSuite {

  override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.allow-deregister" -> false)
    .build()

  trait TestFixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val amlsConnector = mock[AmlsConnector]
    val dataCacheConnector = mock[DataCacheConnector]
    val authService = mock[AuthEnrolmentsService]
    val statusService = mock[StatusService]

    lazy val controller = new DeregistrationReasonController(authConnector, amlsConnector, authService, dataCacheConnector, statusService)
  }

  "The WithdrawalReasonController" when {
    "the GET method is called" must {
      "return 404 not found" in new TestFixture {
        status(controller.get(request)) mustBe NOT_FOUND
      }
    }
  }
}