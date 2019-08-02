/*
 * Copyright 2019 HM Revenue & Customs
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

import audit.{ESDeEnrolFailureEvent, ESRemoveKnownFactsFailureEvent}
import config.{AppConfig, WSHttp}
import exceptions.{DuplicateEnrolmentException, InvalidEnrolmentCredentialsException}
import generators.auth.UserDetailsGenerator
import generators.{AmlsReferenceNumberGenerator, BaseGenerator}
import models.enrolment.{AmlsEnrolmentKey, ErrorResponse, TaxEnrolment}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HttpResponse, Upstream4xxResponse, Upstream5xxResponse}
import utils.AmlsSpec
import uk.gov.hmrc.play.audit.model.DataEvent
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import play.api.libs.json.Json
import uk.gov.hmrc.audit.HandlerResult
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxEnrolmentsConnectorSpec extends AmlsSpec
  with ScalaFutures
  with AmlsReferenceNumberGenerator
  with UserDetailsGenerator
  with BaseGenerator {

  trait Fixture {

    val http = mock[WSHttp]
    val appConfig = mock[AppConfig]
    val authConnector = mock[AuthConnector]
    val auditConnector = mock[AuditConnector]

    val connector = new TaxEnrolmentsConnector(http, appConfig, authConnector, auditConnector)
    val baseUrl = "http://localhost:3001"
    val serviceStub = "tax-enrolments"
    val userDetails = userDetailsGen.sample.get
    val enrolKey = AmlsEnrolmentKey(amlsRegistrationNumber)

    when {
      appConfig.enrolmentStoreUrl
    } thenReturn baseUrl

    when {
      appConfig.enrolmentStubsUrl
    } thenReturn serviceStub

    when {
      authConnector.userDetails(any(), any(), any())
    } thenReturn Future.successful(userDetails)

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
        val endpointUrl = s"$baseUrl/${serviceStub}/groups/${userDetails.groupIdentifier.get}/enrolments/${enrolKey.key}"

        when {
          http.POST[TaxEnrolment, HttpResponse](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future.successful(HttpResponse(OK))

        whenReady(connector.enrol(enrolKey, enrolment)) { _ =>
          verify(authConnector).userDetails(any(), any(), any())
          verify(http).POST[TaxEnrolment, HttpResponse](eqTo(endpointUrl), eqTo(enrolment), any())(any(), any(), any(), any())
          verify(auditConnector).sendEvent(any())(any(), any())
        }
      }

      "throw an exception when no group identifier is available" in new Fixture {
        when {
          authConnector.userDetails(any(), any(), any())
        } thenReturn Future.successful(userDetails.copy(groupIdentifier = None))

        intercept[Exception] {
          await(connector.enrol(enrolKey, enrolment))
        }
      }

      "throws a DuplicateEnrolmentException when the enrolment has already been created" in new Fixture {
        when {
          http.POST[TaxEnrolment, HttpResponse](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future.failed(Upstream4xxResponse(jsonError("ERROR_INVALID_IDENTIFIERS", "The enrolment identifiers provided were invalid"), BAD_REQUEST, BAD_REQUEST))

        intercept[DuplicateEnrolmentException] {
          await(connector.enrol(enrolKey, enrolment))
        }
      }

      "throws a InvalidEnrolmentCredentialsException when the enrolment has the wrong type of role" in new Fixture {
        when {
          http.POST[TaxEnrolment, HttpResponse](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future.failed(Upstream4xxResponse(jsonError("INVALID_CREDENTIAL_ID", "Invalid credential ID"), FORBIDDEN, FORBIDDEN))

        intercept[InvalidEnrolmentCredentialsException] {
          await(connector.enrol(enrolKey, enrolment))
        }
      }
    }
  }

  "deEnrol" when {
    "called" must {
      "call the ES9 API endpoint" in new Fixture {
        val authority = mock[Authority]
        val endpointUrl = s"$baseUrl/${serviceStub}/groups/${userDetails.groupIdentifier.get}/enrolments/${enrolKey.key}"

        when {
          http.DELETE[HttpResponse](any())(any(), any(), any())
        } thenReturn Future.successful(HttpResponse(NO_CONTENT))

        whenReady(connector.deEnrol(amlsRegistrationNumber)) { _ =>
          verify(authConnector).userDetails(any(), any(), any())
          verify(http).DELETE[HttpResponse](eqTo(endpointUrl))(any(), any(), any())
          verify(auditConnector).sendEvent(any())(any(), any())
        }
      }

      "throw an exception when there is no group identifier" in new Fixture {
        val details = userDetailsGen.sample.get.copy(groupIdentifier = None)

        when(authConnector.userDetails(any(), any(), any())).thenReturn(Future.successful(details))

        intercept[Exception] {
          await(connector.deEnrol(amlsRegistrationNumber))
        } match {
          case ex => ex.getMessage mustBe "Group identifier is unavailable"
        }
      }

      "audits the exception when a 400 error is encountered" in new Fixture {
        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        val auditResult = AuditResult.fromHandlerResult(HandlerResult.Success)

        when {
          http.DELETE[HttpResponse](any())(any(), any(), any())
        } thenReturn Future.failed(Upstream4xxResponse(jsonError("BAD_REQUEST", "Some 400 error"), BAD_REQUEST, BAD_REQUEST))

        when {
          auditConnector.sendEvent(captor.capture())(any(), any())
        } thenReturn Future.successful(auditResult)

        intercept[Upstream4xxResponse] {
          await(connector.deEnrol(amlsRegistrationNumber))
        } match {
          case ex => {
            val event = ESDeEnrolFailureEvent(ex, enrolKey.key, amlsRegistrationNumber)
            verify(auditConnector, times(1)).sendEvent(any())(any(), any())
            val capturedEvent = captor.getValue
            capturedEvent.auditSource mustEqual event.auditSource
            capturedEvent.auditType mustEqual event.auditType
            capturedEvent.detail mustEqual event.detail
            ex.message must include("Some 400 error")
          }
        }
      }

      "audits the exception when a 500 error is encountered" in new Fixture {
        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        val auditResult = AuditResult.fromHandlerResult(HandlerResult.Success)

        when {
          http.DELETE[HttpResponse](any())(any(), any(), any())
        } thenReturn Future.failed(Upstream5xxResponse(jsonError("INTERNAL_SERVER_ERROR", "Some 500 error"), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))

        when {
          auditConnector.sendEvent(captor.capture())(any(), any())
        } thenReturn Future.successful(auditResult)

        intercept[Upstream5xxResponse] {
          await(connector.deEnrol(amlsRegistrationNumber))
        } match {
          case ex => {
            val event = ESDeEnrolFailureEvent(ex, enrolKey.key, amlsRegistrationNumber)
            verify(auditConnector, times(1)).sendEvent(any())(any(), any())
            val capturedEvent = captor.getValue
            capturedEvent.auditSource mustEqual event.auditSource
            capturedEvent.auditType mustEqual event.auditType
            capturedEvent.detail mustEqual event.detail
            ex.message must include("Some 500 error")
          }
        }
      }
    }
  }

  "removeKnownFacts" when {
    "called" must {
      "call the ES7 API endpoint" in new Fixture {

        val endpointUrl = s"$baseUrl/${serviceStub}/enrolments/${enrolKey.key}"

        when {
          http.DELETE[HttpResponse](any())(any(), any(), any())
        } thenReturn Future.successful(HttpResponse(NO_CONTENT))

        whenReady(connector.removeKnownFacts(amlsRegistrationNumber)) { _ =>
          verify(http).DELETE[HttpResponse](eqTo(endpointUrl))(any(), any(), any())
          verify(auditConnector).sendEvent(any())(any(), any())
        }
      }

      "audits the exception when a 400 error is encountered" in new Fixture {
        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        val auditResult = AuditResult.fromHandlerResult(HandlerResult.Success)

        when {
          http.DELETE[HttpResponse](any())(any(), any(), any())
        } thenReturn Future.failed(Upstream4xxResponse(jsonError("BAD_REQUEST", "Some 400 error"), BAD_REQUEST, BAD_REQUEST))

        when {
          auditConnector.sendEvent(captor.capture())(any(), any())
        } thenReturn Future.successful(auditResult)

        intercept[Upstream4xxResponse] {
          await(connector.removeKnownFacts(amlsRegistrationNumber))
        } match {
          case ex => {
            val event = ESRemoveKnownFactsFailureEvent(ex, enrolKey.key, amlsRegistrationNumber)
            verify(auditConnector, times(1)).sendEvent(any())(any(), any())
            val capturedEvent = captor.getValue
            capturedEvent.auditSource mustEqual event.auditSource
            capturedEvent.auditType mustEqual event.auditType
            capturedEvent.detail mustEqual event.detail
            ex.message must include("Some 400 error")
          }
        }
      }

      "audits the exception when a 500 error is encountered" in new Fixture {
        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        val auditResult = AuditResult.fromHandlerResult(HandlerResult.Success)

        when {
          http.DELETE[HttpResponse](any())(any(), any(), any())
        } thenReturn Future.failed(Upstream5xxResponse(jsonError("INTERNAL_SERVER_ERROR", "Some 500 error"), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))

        when {
          auditConnector.sendEvent(captor.capture())(any(), any())
        } thenReturn Future.successful(auditResult)

        intercept[Upstream5xxResponse] {
          await(connector.removeKnownFacts(amlsRegistrationNumber))
        } match {
          case ex => {
            val event = ESRemoveKnownFactsFailureEvent(ex, enrolKey.key, amlsRegistrationNumber)
            verify(auditConnector, times(1)).sendEvent(any())(any(), any())
            val capturedEvent = captor.getValue
            capturedEvent.auditSource mustEqual event.auditSource
            capturedEvent.auditType mustEqual event.auditType
            capturedEvent.detail mustEqual event.detail
            ex.message must include("Some 500 error")
          }
        }
      }
    }
  }
}
