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

package controllers.bankdetails

import connectors.DataCacheConnector
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

class WhatYouNeedControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new WhatYouNeedController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "WhatYouNeedController" when {

    "get is called" must {

      "respond with SEE_OTHER and redirect to the 'what you need' page" in new Fixture {

        val result = controller.get(1)(request)

        status(result) must be(OK)

        val pageTitle = Messages("title.wyn") + " - " +
          Messages("summary.bankdetails") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        contentAsString(result) must include(pageTitle)
        contentAsString(result) must include(Messages("button.continue"))
      }
    }
  }
}
