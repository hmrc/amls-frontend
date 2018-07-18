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

package connectors

import java.util.UUID

import config.{AppConfig, WSHttp}
import exceptions.{DuplicateEnrolmentException, InvalidEnrolmentCredentialsException}
import generators.auth.UserDetailsGenerator
import generators.{AmlsReferenceNumberGenerator, BaseGenerator}
import models.auth.UserDetails
import models.enrolment.{AmlsEnrolmentKey, EnrolmentStoreEnrolment, ErrorResponse}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, Upstream4xxResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentStoreConnectorSpec extends PlaySpec
  with MustMatchers
  with ScalaFutures
  with MockitoSugar
  with AmlsReferenceNumberGenerator
  with UserDetailsGenerator
  with BaseGenerator
  with OneAppPerSuite {

  trait Fixture {

    implicit val headerCarrier = HeaderCarrier()
    implicit val authContext = mock[AuthContext]

    val http = mock[WSHttp]
    val appConfig = mock[AppConfig]
    val authConnector = mock[AuthConnector]
    val auditConnector = mock[AuditConnector]

    val connector = new EnrolmentStoreConnector(http, appConfig, authConnector, auditConnector)
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

    val enrolment = EnrolmentStoreEnrolment("123456789", postcodeGen.sample.get)

    def jsonError(code: String, message: String): String = Json.toJson(ErrorResponse(code, message)).toString

  }

  "enrol" when {
    "called" must {
      "call the ES8 enrolment store endpoint to enrol the user" in new Fixture {
        val endpointUrl = s"$baseUrl/${serviceStub}/groups/${userDetails.groupIdentifier.get}/enrolments/${enrolKey.key}"

        when {
          http.POST[EnrolmentStoreEnrolment, HttpResponse](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future.successful(HttpResponse(OK))

        whenReady(connector.enrol(enrolKey, enrolment)) { _ =>
          verify(authConnector).userDetails(any(), any(), any())
          verify(http).POST[EnrolmentStoreEnrolment, HttpResponse](eqTo(endpointUrl), eqTo(enrolment), any())(any(), any(), any(), any())
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
          http.POST[EnrolmentStoreEnrolment, HttpResponse](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future.failed(Upstream4xxResponse(jsonError("ERROR_INVALID_IDENTIFIERS", "The enrolment identifiers provided were invalid"), BAD_REQUEST, BAD_REQUEST))

        intercept[DuplicateEnrolmentException] {
          await(connector.enrol(enrolKey, enrolment))
        }
      }

      "throws a InvalidEnrolmentCredentialsException when the enrolment has the wrong type of role" in new Fixture {
        when {
          http.POST[EnrolmentStoreEnrolment, HttpResponse](any(), any(), any())(any(), any(), any(), any())
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
    }
  }

}
