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

import connectors.AuthConnector
import models.enrolment.{EnrolmentIdentifier, GovernmentGatewayEnrolment}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

class AuthEnrolmentsServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with IntegrationPatience{

  object AuthEnrolmentsService extends AuthEnrolmentsService {
    override private[services] val authConnector: AuthConnector = mock[AuthConnector]
  }

  implicit val hc = mock[HeaderCarrier]
  implicit val ac = mock[AuthContext]

  private val amlsRegistrationNumber = "XXML00000000000"

  private val enrolmentsList = List[GovernmentGatewayEnrolment](GovernmentGatewayEnrolment("HMCE-VATVAR-ORG",
    List[EnrolmentIdentifier](EnrolmentIdentifier("VATRegNo", "000000000")), "Activated"), GovernmentGatewayEnrolment("HMRC-MLR-ORG",
    List[EnrolmentIdentifier](EnrolmentIdentifier("MLRRefNumber", amlsRegistrationNumber)), "Activated"))

  "AuthEnrolmentsService" must {

    "return an AMLS regsitration number" in {

      when(AuthEnrolmentsService.authConnector.enrollments(any())(any(),any())).thenReturn(Future.successful(enrolmentsList))
      when(ac.enrolmentsUri).thenReturn(Some("uri"))
      whenReady(AuthEnrolmentsService.amlsRegistrationNumber){
        number => number.get mustEqual(amlsRegistrationNumber)
      }

    }

  }

}
