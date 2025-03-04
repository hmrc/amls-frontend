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

package controllers.responsiblepeople

import controllers.actions.SuccessfulAuthAction
import models.businessmatching.BusinessActivity.{MoneyServiceBusiness, TelephonePaymentService, TrustAndCompanyServices}
import models.businessmatching.{BusinessActivities, BusinessMatching}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.Injecting
import utils.{AmlsSpec, DependencyMocks}
import views.html.responsiblepeople.WhatYouNeedView

import scala.concurrent.Future

class WhatYouNeedControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[WhatYouNeedView]
    val controller = new WhatYouNeedController(
      dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      view = view
    )
  }

  "WhatYouNeedController" must {

    "get" must {

      "load the page" in new Fixture {
        val BusinessActivitiesModel =
          BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
        val bm                      = Some(BusinessMatching(activities = Some(BusinessActivitiesModel)))

        when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any())) thenReturn (Future.successful(
          bm
        ))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val pageTitle = messages("title.wyn") + " - " +
          messages("summary.responsiblepeople") + " - " +
          messages("title.amls") + " - " + messages("title.gov")

        contentAsString(result) must include(pageTitle)
      }
    }

    "throw an error when data cannot be fetched" in new Fixture {
      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(None))

      a[Exception] must be thrownBy {
        ScalaFutures.whenReady(controller.get(1)(request))(x => x)
      }
    }
  }

}
