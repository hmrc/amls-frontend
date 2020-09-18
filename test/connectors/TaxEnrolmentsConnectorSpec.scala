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

package connectors

import config.ApplicationConfig
import exceptions.{DuplicateEnrolmentException, InvalidEnrolmentCredentialsException}
import generators.auth.UserDetailsGenerator
import generators.{AmlsReferenceNumberGenerator, BaseGenerator}
import models.enrolment.{AmlsEnrolmentKey, ErrorResponse, TaxEnrolment}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.http.HttpClient
import utils.AmlsSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxEnrolmentsConnectorSpec extends AmlsSpec
  with ScalaFutures
  with AmlsReferenceNumberGenerator
  with UserDetailsGenerator
  with BaseGenerator {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  trait Fixture {

    val http = mock[HttpClient]
    val appConfig = mock[ApplicationConfig]
    val auditConnector = mock[AuditConnector]
    val groupIdentfier = stringOfLengthGen(10).sample.get
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val connector = new TaxEnrolmentsConnector(http, appConfig, auditConnector)
    val baseUrl = "http://localhost:3001"
    val serviceStub = "tax-enrolments"
    val enrolKey = AmlsEnrolmentKey(amlsRegistrationNumber)

    when {
      appConfig.enrolmentStoreUrl
    } thenReturn baseUrl

    when {
      appConfig.enrolmentStubsUrl
    } thenReturn serviceStub

    val enrolment = TaxEnrolment("123456789", postcodeGen.sample.get)

    def jsonError(code: String, message: String): String = Json.toJson(ErrorResponse(code, message)).toString
  }

  "configuration" when {
    "stubbed" must {
      "return stubs base url" in new Fixture {
        when {
          appConfig.enrolmentStubsEnabled
        } thenReturn true

        connector.baseUrl mustBe s"${appConfig.enrolmentStubsUrl}/tax-enrolments"
      }
    }

    "not stubbed" must {
      "return tax enrolments base url" in new Fixture {
        connector.baseUrl mustBe s"${appConfig.enrolmentStoreUrl}/tax-enrolments"
      }
    }
  }

  "enrol" when {
    "called" must {
      "call the ES8 enrolment store endpoint to enrol the user" in new Fixture {

        val endpointUrl = s"$baseUrl/${serviceStub}/groups/$groupIdentfier/enrolments/${enrolKey.key}"

        when {
          http.POST[TaxEnrolment, HttpResponse](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future.successful(HttpResponse(OK))

        whenReady(connector.enrol(enrolKey, enrolment, Some(groupIdentfier))) { _ =>
          verify(http).POST[TaxEnrolment, HttpResponse](eqTo(endpointUrl), eqTo(enrolment), any())(any(), any(), any(), any())
          verify(auditConnector).sendEvent(any())(any(), any())
        }
      }

      "throw an exception when no group identifier is available" in new Fixture {
        intercept[Exception] {
          await(connector.enrol(enrolKey, enrolment, None))
        }
      }

      "throws a DuplicateEnrolmentException when the enrolment has already been created" in new Fixture {
        when {
          http.POST[TaxEnrolment, HttpResponse](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future.failed(Upstream4xxResponse(jsonError("ERROR_INVALID_IDENTIFIERS", "The enrolment identifiers provided were invalid"), BAD_REQUEST, BAD_REQUEST))

        intercept[DuplicateEnrolmentException] {
          await(connector.enrol(enrolKey, enrolment, Some(groupIdentfier)))
        }
      }

      "throws a InvalidEnrolmentCredentialsException when the enrolment has the wrong type of role" in new Fixture {
        when {
          http.POST[TaxEnrolment, HttpResponse](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future.failed(Upstream4xxResponse(jsonError("INVALID_CREDENTIAL_ID", "Invalid credential ID"), FORBIDDEN, FORBIDDEN))

        intercept[InvalidEnrolmentCredentialsException] {
          await(connector.enrol(enrolKey, enrolment, Some(groupIdentfier)))
        }
      }
    }
  }

  "deEnrol" when {
    "called" must {
      "call the ES9 API endpoint" in new Fixture {
        val endpointUrl = s"$baseUrl/${serviceStub}/groups/$groupIdentfier/enrolments/${enrolKey.key}"

        when {
          http.DELETE[HttpResponse](any(), any())(any(), any(), any())
        } thenReturn Future.successful(HttpResponse(NO_CONTENT))

        whenReady(connector.deEnrol(amlsRegistrationNumber, Some(groupIdentfier))) { _ =>
          verify(http).DELETE[HttpResponse](eqTo(endpointUrl), any())(any(), any(), any())
          verify(auditConnector).sendEvent(any())(any(), any())
        }
      }

      "throw an exception when there is no group identifier" in new Fixture {
        intercept[Exception] {
          await(connector.deEnrol(amlsRegistrationNumber, None))
        } match {
          case ex => ex.getMessage mustBe "Group identifier is unavailable"
        }
      }
    }
  }

  "removeKnownFacts" when {
    "called" must {
      "call the ES7 API endpoint" in new Fixture {

        val endpointUrl = s"$baseUrl/${serviceStub}/enrolments/${enrolKey.key}"

        when {
          http.DELETE[HttpResponse](any(), any())(any(), any(), any())
        } thenReturn Future.successful(HttpResponse(NO_CONTENT))

        whenReady(connector.removeKnownFacts(amlsRegistrationNumber)) { _ =>
          verify(http).DELETE[HttpResponse](eqTo(endpointUrl), any())(any(), any(), any())
          verify(auditConnector).sendEvent(any())(any(), any())
        }
      }
    }
  }

}
