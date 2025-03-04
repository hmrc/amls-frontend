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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.cache.Cache
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import cats.implicits._
import config.ApplicationConfig
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import play.api.i18n.{Lang, Messages}
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Results._
import play.api.test.FakeRequest
import utils.StatusConstants
import views.html.ErrorView

class TPControllerHelperSpec extends PlaySpec with MockitoSugar {

  trait TestFixture {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val cache: Cache                                          = mock[Cache]
    implicit val lang: Lang                                   = mock[Lang]
    implicit val messages: Messages                           = mock[Messages]
    implicit val appConfig: ApplicationConfig                 = mock[ApplicationConfig]
    implicit val errorView: ErrorView                         = mock[ErrorView]

    def setUpTradingPremise(model: Option[TradingPremises]) = when {
      cache.getEntry[Seq[TradingPremises]](any())(any())
    } thenReturn (model match {
      case Some(x) => Some(Seq(x))
      case _       => Some(Seq.empty[TradingPremises])
    })
  }

  "The trading premises controller helper" must {
    "redirect to the WhereAreTradingPremises controller" when {
      "the business is an agent" in new TestFixture {

        setUpTradingPremise(
          TradingPremises(
            registeringAgentPremises = RegisteringAgentPremises(true).some,
            status = StatusConstants.Unchanged.some
          ).some
        )

        val result = TPControllerHelper.redirectToNextPage(cache.some, 1, edit = false)

        result mustBe Redirect(controllers.tradingpremises.routes.WhereAreTradingPremisesController.get(1))

      }
    }
  }

}
