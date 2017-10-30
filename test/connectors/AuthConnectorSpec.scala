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

package connectors

import models.enrolment.GovernmentGatewayEnrolment
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.{CoreGet, CorePost, HeaderCarrier, HttpGet}

class AuthConnectorSpec  extends PlaySpec with MockitoSugar with ScalaFutures {


  object TestAuthConnector extends AuthConnector {
    override private[connectors] def authUrl: String = ""
    override private[connectors] val http = mock[CoreGet]
  }

  "Auth Connector" must {
    "return list of government gateway enrolments" in {
      implicit val headerCarrier = HeaderCarrier()
      when(TestAuthConnector.http.GET[List[GovernmentGatewayEnrolment]](any())(any(),any(), any())).thenReturn(Future.successful(Nil))

      whenReady(TestAuthConnector.enrollments("thing")){
        results => results must equal(Nil)
      }
    }
  }

}
