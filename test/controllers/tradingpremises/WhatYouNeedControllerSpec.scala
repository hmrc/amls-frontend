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

package controllers.tradingpremises

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService.{ChequeCashingNotScrapMetal, TransmittingMoney}
import models.businessmatching._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import utils.AmlsSpec
import views.html.tradingpremises.WhatYouNeedView

import scala.concurrent.Future

class WhatYouNeedControllerSpec extends AmlsSpec with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = app.injector.instanceOf[WhatYouNeedView]
    val controller = new WhatYouNeedController(
      mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      view = view
    )
  }

  "WhatYouNeedController" must {

    "load the what you need page" in new Fixture {
      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any())) thenReturn (
        Future.successful(
          Some(
            BusinessMatching(
              None,
              Some(BusinessActivities(Set(MoneyServiceBusiness))),
              Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
              None,
              None,
              None
            )
          )
        )
      )

      val result = controller.get(1)(request)
      status(result) mustBe OK
    }

    "load the what you need page when msb selected as an option in business matching" in new Fixture {
      val BusinessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness))
      val bm                      = Some(BusinessMatching(activities = Some(BusinessActivitiesModel)))

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any())) thenReturn (Future.successful(
        bm
      ))
      val result = controller.get(1)(request)
      status(result)          must be(OK)
      contentAsString(result) must include(messages("tradingpremises.whatyouneed.agents.sub.heading"))

    }

    "load the what you need page when msb is not selected" in new Fixture {
      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any())) thenReturn {
        Future.successful(
          Some(
            BusinessMatching(
              None,
              Some(BusinessActivities(Set(ArtMarketParticipant))),
              Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal))),
              None,
              None,
              None
            )
          )
        )
      }

      val result = controller.get(1)(request)
      status(result) must be(OK)
      contentAsString(result) mustNot include(messages("tradingpremises.whatyouneed.agents.sub.heading"))

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
