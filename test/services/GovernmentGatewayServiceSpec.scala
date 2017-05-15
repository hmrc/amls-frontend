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

package services

import connectors.GovernmentGatewayConnector
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import play.api.http.Status._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class GovernmentGatewayServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object GovernmentGatewayService extends GovernmentGatewayService {
    override private[services] val ggConnector = mock[GovernmentGatewayConnector]
  }

  "GovernmentGatewayService" must {

    implicit val hc = HeaderCarrier()

    "successfully enrol" in {

      val response = HttpResponse(OK)

      when {
        GovernmentGatewayService.ggConnector.enrol(any())(any(), any(), any())
      } thenReturn Future.successful(response)

      whenReady (GovernmentGatewayService.enrol("mlrRefNo", "safeId")) {
        result =>
          result must equal (response)
      }
    }
  }
}
