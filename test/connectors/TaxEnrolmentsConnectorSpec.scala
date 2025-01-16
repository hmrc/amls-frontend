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

package connectors

import config.ApplicationConfig
import exceptions.{DuplicateEnrolmentException, InvalidEnrolmentCredentialsException}
import generators.auth.UserDetailsGenerator
import generators.{AmlsReferenceNumberGenerator, BaseGenerator}
import models.enrolment.{AmlsEnrolmentKey, ErrorResponse, TaxEnrolment}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.verify
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.{AmlsSpec, HttpClientMocker}

class TaxEnrolmentsConnectorSpec extends AmlsSpec
  with ScalaFutures
  with AmlsReferenceNumberGenerator
  with UserDetailsGenerator
  with BaseGenerator {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  trait Fixture {fixture =>
    val baseUrl = "http://localhost:3001"

    def enrolmentStubsEnabled = false
    private val configuration: Configuration = Configuration.load(Environment.simple())
    val appConfig = new ApplicationConfig(configuration, new ServicesConfig(configuration)){
      override def enrolmentStubsEnabled: Boolean = fixture.enrolmentStubsEnabled
      override def enrolmentStoreUrl: String = baseUrl
//      override def enrolmentStubsUrl: String = serviceStub
    }

    val mocker = new HttpClientMocker()
    val auditConnector: AuditConnector = mock[AuditConnector]
    val groupIdentfier: String = stringOfLengthGen(10).sample.get
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val connector = new TaxEnrolmentsConnector(mocker.httpClient, appConfig, auditConnector)
//    val serviceStub = "tax-enrolments"
    val enrolKey: AmlsEnrolmentKey = AmlsEnrolmentKey(amlsRegistrationNumber)

    val enrolment: TaxEnrolment = TaxEnrolment("123456789", postcodeGen.sample.get)

    def jsonError(code: String, message: String): String = Json.toJson(ErrorResponse(code, message)).toString
  }

  "configuration" when {
    "stubbed" must {
      "return stubs base url" in new Fixture {
        override def enrolmentStubsEnabled = true
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

        val endpointUrl = url"$baseUrl/tax-enrolments/groups/$groupIdentfier/enrolments/${enrolKey.key}"
        val response: HttpResponse = HttpResponse(OK, "")
        mocker.mockPostJson(endpointUrl, enrolment, response)
        connector.enrol(enrolKey, enrolment, Some(groupIdentfier)).futureValue mustBe response
        verify(auditConnector).sendEvent(any())(any(), any())
        }
      }

      "throw an exception when no group identifier is available" in new Fixture {
        intercept[Exception] {
          await(connector.enrol(enrolKey, enrolment, None))
        }
      }

      "throws a DuplicateEnrolmentException when the enrolment has already been created" in new Fixture {

        val endpointUrl = url"$baseUrl/tax-enrolments/groups/$groupIdentfier/enrolments/${enrolKey.key}"
        val response: UpstreamErrorResponse = UpstreamErrorResponse(jsonError("ERROR_INVALID_IDENTIFIERS", "The enrolment identifiers provided were invalid"), BAD_REQUEST, BAD_REQUEST)
        mocker.mockPostJson(endpointUrl, enrolment, response)

        intercept[DuplicateEnrolmentException] {
          await(connector.enrol(enrolKey, enrolment, Some(groupIdentfier)))
        }
      }

      "throws a InvalidEnrolmentCredentialsException when the enrolment has the wrong type of role" in new Fixture {

        val endpointUrl = url"$baseUrl/tax-enrolments/groups/$groupIdentfier/enrolments/${enrolKey.key}"
        val response: UpstreamErrorResponse = UpstreamErrorResponse(jsonError("INVALID_CREDENTIAL_ID", "Invalid credential ID"), FORBIDDEN, FORBIDDEN)
        mocker.mockPostJson(endpointUrl, enrolment, response)

        intercept[InvalidEnrolmentCredentialsException] {
          await(connector.enrol(enrolKey, enrolment, Some(groupIdentfier)))
        }
      }
  }

  "deEnrol" when {
    "called" must {
      "call the ES9 API endpoint" in new Fixture {
        val endpointUrl = url"$baseUrl/tax-enrolments/groups/$groupIdentfier/enrolments/${enrolKey.key}"
        val response: HttpResponse = HttpResponse(NO_CONTENT, "")
        mocker.mockDelete(endpointUrl, response)
        connector.deEnrol(amlsRegistrationNumber, Some(groupIdentfier)).futureValue mustBe response
        verify(auditConnector).sendEvent(any())(any(), any())
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
        val endpointUrl = url"$baseUrl/tax-enrolments/enrolments/${enrolKey.key}"
        val response: HttpResponse = HttpResponse(NO_CONTENT, "")
        mocker.mockDelete(endpointUrl, response)

        connector.removeKnownFacts(amlsRegistrationNumber).futureValue mustBe response
        verify(auditConnector).sendEvent(any())(any(), any())
      }
    }
  }
}
