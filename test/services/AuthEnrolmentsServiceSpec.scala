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

package services

import config.ApplicationConfig
import connectors.{EnrolmentStubConnector, TaxEnrolmentsConnector}
import generators.{AmlsReferenceNumberGenerator, BaseGenerator}
import models.enrolment.{AmlsEnrolmentKey, EnrolmentIdentifier, GovernmentGatewayEnrolment, TaxEnrolment}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import utils.AmlsSpec

import scala.concurrent.Future

class AuthEnrolmentsServiceSpec
    extends AmlsSpec
    with ScalaFutures
    with IntegrationPatience
    with AmlsReferenceNumberGenerator
    with BaseGenerator {

  // scalastyle:off magic.number
  trait Fixture {
    val enrolmentStore         = mock[TaxEnrolmentsConnector]
    val enrolmentStubConnector = mock[EnrolmentStubConnector]
    val config                 = mock[ApplicationConfig]

    val groupId = stringOfLengthGen(10).sample.get

    val service = new AuthEnrolmentsService(enrolmentStore, config, enrolmentStubConnector)

    val enrolmentsList = List[GovernmentGatewayEnrolment](
      GovernmentGatewayEnrolment(
        "HMCE-VATVAR-ORG",
        List[EnrolmentIdentifier](EnrolmentIdentifier("VATRegNo", "000000000")),
        "Activated"
      ),
      GovernmentGatewayEnrolment(
        "HMRC-MLR-ORG",
        List[EnrolmentIdentifier](EnrolmentIdentifier("MLRRefNumber", amlsRegistrationNumber)),
        "Activated"
      )
    )

    when(config.enrolmentStubsEnabled) thenReturn false
  }

  "AuthEnrolmentsService" must {

    "connect to the stubs microservice when enabled and enrolments were returned by auth" in new Fixture {
      when(config.enrolmentStubsEnabled) thenReturn true

      when {
        enrolmentStubConnector.enrolments(eqTo(groupId))(any(), any())
      } thenReturn Future.successful(enrolmentsList)

      whenReady(service.amlsRegistrationNumber(Some(amlsRegistrationNumber), Some(groupId))) { result =>
        result mustBe Some(amlsRegistrationNumber)
      }
    }

    "return an AMLS registration number" in new Fixture {

      whenReady(service.amlsRegistrationNumber(Some(amlsRegistrationNumber), Some(groupId))) { number =>
        number.get mustEqual amlsRegistrationNumber
      }
    }

    "create an enrolment" in new Fixture {

      when {
        service.enrolmentStore.enrol(any(), any(), any())(any(), any())
      } thenReturn Future.successful(HttpResponse(OK, ""))

      val postcode = postcodeGen.sample.get

      whenReady(service.enrol(amlsRegistrationNumber, postcode, Some(groupId), "12345678")) { _ =>
        val enrolment = TaxEnrolment("12345678", postcode)
        verify(enrolmentStore)
          .enrol(eqTo(AmlsEnrolmentKey(amlsRegistrationNumber)), eqTo(enrolment), any())(any(), any())
      }
    }

    "de-enrol the user and return true" in new Fixture {

      when {
        enrolmentStore.deEnrol(eqTo(amlsRegistrationNumber), any())(any(), any())
      } thenReturn Future.successful(HttpResponse(NO_CONTENT, ""))

      when {
        enrolmentStore.removeKnownFacts(eqTo(amlsRegistrationNumber))(any(), any())
      } thenReturn Future.successful(HttpResponse(NO_CONTENT, ""))

      whenReady(service.deEnrol(amlsRegistrationNumber, Some("GROUP_ID"))) { result =>
        result mustBe true
        verify(enrolmentStore).removeKnownFacts(eqTo(amlsRegistrationNumber))(any(), any())
        verify(enrolmentStore).deEnrol(eqTo(amlsRegistrationNumber), any())(any(), any())
      }
    }
  }

  "AuthEnrolmentsService for new auth" must {

    "return an AMLS registration number from stubs" in new Fixture {
      when(config.enrolmentStubsEnabled) thenReturn true

      when {
        enrolmentStubConnector.enrolments(eqTo(groupId))(any(), any())
      } thenReturn Future.successful(enrolmentsList)

      whenReady(service.amlsRegistrationNumber(None, Some(groupId))) { result =>
        result mustBe Some(amlsRegistrationNumber)
      }
    }

    "return None from stubs if no amls number from request and stubs disabled" in new Fixture {
      when(config.enrolmentStubsEnabled) thenReturn false

      when {
        enrolmentStubConnector.enrolments(eqTo(groupId))(any(), any())
      } thenReturn Future.successful(enrolmentsList)

      whenReady(service.amlsRegistrationNumber(None, Some(groupId))) { result =>
        result mustBe None
      }
    }

    "return an AMLS registration number from request even if stubs are enabled" in new Fixture {
      when(config.enrolmentStubsEnabled) thenReturn true

      whenReady(service.amlsRegistrationNumber(Some(amlsRegistrationNumber), Some(groupId))) { result =>
        result mustBe Some(amlsRegistrationNumber)
      }
    }
  }
}
