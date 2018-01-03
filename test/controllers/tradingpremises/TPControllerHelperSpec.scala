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

package controllers.tradingpremises

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.cache.client.CacheMap
import org.mockito.Mockito._
import org.mockito.Matchers._
import cats.implicits._
import cats._
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import play.api.mvc.Results._
import play.api.test.FakeRequest
import utils.StatusConstants

class TPControllerHelperSpec extends PlaySpec with MockitoSugar {

  trait TestFixture {
    implicit val request = FakeRequest()
    val cache = mock[CacheMap]

    def setUpTradingPremise(model: Option[TradingPremises]) = when {
      cache.getEntry[Seq[TradingPremises]](any())(any())
    } thenReturn (model match {
      case Some(x) => Some(Seq(x))
      case _ => Some(Seq.empty[TradingPremises])
    })
  }

  "The trading premises controller helper" must {
    "redirect to the WhereAreTradingPremises controller" when {
     "the business is an agent" in new TestFixture {

       setUpTradingPremise(TradingPremises(registeringAgentPremises = RegisteringAgentPremises(true).some, status = StatusConstants.Unchanged.some).some)

       val result = TPControllerHelper.redirectToNextPage(cache.some, 1, edit = false)

       result mustBe Redirect(controllers.tradingpremises.routes.WhereAreTradingPremisesController.get(1))

     }
    }
  }

}
