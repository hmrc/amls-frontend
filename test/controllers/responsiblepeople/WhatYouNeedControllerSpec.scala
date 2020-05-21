/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.responsiblepeople

import controllers.actions.SuccessfulAuthAction
import models.businessmatching.{BusinessActivities, BusinessMatching, MoneyServiceBusiness, TelephonePaymentService, TrustAndCompanyServices}
import models.status.SubmissionReadyForReview
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import play.api.i18n.Messages
import play.api.test.Helpers._

import scala.concurrent.Future

class WhatYouNeedControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends DependencyMocks {
    self => val request = addToken(authRequest)

    val controller = new WhatYouNeedController (
      dataCacheConnector = mockCacheConnector, authAction = SuccessfulAuthAction, ds = commonDependencies, cc = mockMcc)
  }
  "WhatYouNeedController" must {

    "get" must {

      "load the page" in new Fixture {
        val BusinessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
        val bm = Some(BusinessMatching(activities = Some(BusinessActivitiesModel)))

        when (controller.dataCacheConnector.fetch[BusinessMatching](any(),any())(any(),any())) thenReturn(Future.successful(bm))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val pageTitle = Messages("title.wyn") + " - " +
          Messages("summary.responsiblepeople") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        contentAsString(result) must include(pageTitle)
      }
    }
    "Throw an error" when {
      "bm details cannot be fetched" in new Fixture {
        when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(1)(request)
        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }
  }
}
