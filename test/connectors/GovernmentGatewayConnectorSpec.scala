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

import exceptions.{DuplicateEnrolmentException, InvalidEnrolmentCredentialsException}
import generators.{AmlsReferenceNumberGenerator, BaseGenerator, GovernmentGatewayGenerator}
import models.governmentgateway.EnrolmentRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import utils.{AmlsSpec, DependencyMocks}

import scala.concurrent.Future

class GovernmentGatewayConnectorSpec extends AmlsSpec
  with AmlsReferenceNumberGenerator
  with BaseGenerator
  with GovernmentGatewayGenerator
  with ScalaFutures {

  //noinspection ScalaStyle
  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(500, Millis))

  trait Fixture extends DependencyMocks {
    val audit: Audit = mock[Audit]

    val connector = new GovernmentGatewayConnector(mock[HttpClient], appConfig, mock[DefaultAuditConnector])

    def mockHttpCall(response: Future[HttpResponse]): OngoingStubbing[Future[HttpResponse]] = when {
      connector.http.POST[EnrolmentRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    } thenReturn response
  }

  "enrol" when {
    "called" must {
      "enrol the user via an HTTP call" in new Fixture {
        implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
        val request: EnrolmentRequest = enrolmentRequestGen.sample.get
        val response: HttpResponse = HttpResponse(OK, "")
        mockHttpCall(Future.successful(response))

        whenReady(connector.enrol(request)) { result =>
          result.status mustBe OK
        }
      }

      "throw DuplicateEnrolmentException when a duplicate enrolment message was received" in new Fixture {
        mockHttpCall(Future.failed(new Exception(connector.duplicateEnrolmentMessage)))

        intercept[DuplicateEnrolmentException] {
          await(connector.enrol(enrolmentRequestGen.sample.get))
        }
      }

      "throw InvalidEnrolmentCredentialsException when an invalid credentials message was received" in new Fixture {
        mockHttpCall(Future.failed(new Exception(connector.invalidCredentialsMessage)))

        intercept[InvalidEnrolmentCredentialsException] {
          await(connector.enrol(enrolmentRequestGen.sample.get))
        }
      }
    }
  }

}
