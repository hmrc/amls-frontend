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

package services

import connectors.{AuthConnector, Authority, EnrolmentStoreConnector}
import generators.{AmlsReferenceNumberGenerator, BaseGenerator}
import models.enrolment.{AmlsEnrolmentKey, EnrolmentIdentifier, EnrolmentStoreEnrolment, GovernmentGatewayEnrolment}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.frontend.auth.AuthContext
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts

class AuthEnrolmentsServiceSpec extends PlaySpec
  with MockitoSugar
  with ScalaFutures
  with IntegrationPatience
  with AmlsReferenceNumberGenerator
  with BaseGenerator {

  trait Fixture {
    val enrolmentStore = mock[EnrolmentStoreConnector]
    val service = new AuthEnrolmentsService(mock[AuthConnector], enrolmentStore)

    implicit val hc = mock[HeaderCarrier]
    implicit val ac = mock[AuthContext]

    val enrolmentsList = List[GovernmentGatewayEnrolment](GovernmentGatewayEnrolment("HMCE-VATVAR-ORG",
      List[EnrolmentIdentifier](EnrolmentIdentifier("VATRegNo", "000000000")), "Activated"), GovernmentGatewayEnrolment("HMRC-MLR-ORG",
      List[EnrolmentIdentifier](EnrolmentIdentifier("MLRRefNumber", amlsRegistrationNumber)), "Activated"))

  }

  "AuthEnrolmentsService" must {
    "return an AMLS registration number" in new Fixture {
      when(service.authConnector.enrollments(any())(any(),any())).thenReturn(Future.successful(enrolmentsList))
      when(ac.enrolmentsUri).thenReturn(Some("uri"))

      whenReady(service.amlsRegistrationNumber){
        number => number.get mustEqual amlsRegistrationNumber
      }
    }

    "create an enrolment" in new Fixture {
      when {
        service.authConnector.getCurrentAuthority(any(), any())
      } thenReturn Future.successful(Authority("", Accounts(), "/user-details", "/ids", "12345678"))

      when {
        service.enrolmentStore.enrol(any(), any())(any(), any(), any())
      } thenReturn Future.successful(HttpResponse(OK))

      val postcode = postcodeGen.sample.get

      whenReady(service.enrol(amlsRegistrationNumber, postcode)) { _ =>
        val enrolment = EnrolmentStoreEnrolment("12345678", postcode)
        verify(enrolmentStore).enrol(eqTo(AmlsEnrolmentKey(amlsRegistrationNumber)), eqTo(enrolment))(any(), any(), any())
      }
    }

    "de-enrol the user and return true" in new Fixture {

      when {
        enrolmentStore.deEnrol(eqTo(amlsRegistrationNumber))(any(), any(), any())
      } thenReturn Future.successful(HttpResponse(NO_CONTENT))

      when {
        enrolmentStore.removeKnownFacts(eqTo(amlsRegistrationNumber))(any(), any(), any())
      } thenReturn Future.successful(HttpResponse(NO_CONTENT))

      whenReady(service.deEnrol(amlsRegistrationNumber)) { result =>
        result mustBe true
        verify(enrolmentStore).removeKnownFacts(eqTo(amlsRegistrationNumber))(any(), any(), any())
        verify(enrolmentStore).deEnrol(eqTo(amlsRegistrationNumber))(any(), any(), any())
      }
    }
  }
}
