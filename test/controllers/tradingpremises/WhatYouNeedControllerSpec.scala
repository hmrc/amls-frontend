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

package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessmatching._
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class WhatYouNeedControllerSpec extends GenericTestHelper with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new WhatYouNeedController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  "WhatYouNeedController" must {

    "load the what you need page" in new Fixture {
      when (controller.dataCacheConnector.fetch[BusinessMatching](any())(any(),any(),any())) thenReturn(Future.successful(None))
        val result = controller.get(1)(request)
      status(result) mustBe OK
    }

    "load the what you need page when msb selected as an option in business matching" in new Fixture {
      val BusinessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
      val bm = Some(BusinessMatching(activities = Some(BusinessActivitiesModel)))

      when (controller.dataCacheConnector.fetch[BusinessMatching](any())(any(),any(),any())) thenReturn(Future.successful(bm))
      val result = controller.get(1)(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("tradingpremises.whatyouneed.agents.sub.heading"))

    }
  }

  it must {
    "use correct services" in new Fixture {
      WhatYouNeedController.authConnector must be(AMLSAuthConnector)
    }
  }
}
