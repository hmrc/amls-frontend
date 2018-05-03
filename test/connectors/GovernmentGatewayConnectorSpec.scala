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

import exceptions.{DuplicateEnrolmentException, InvalidEnrolmentCredentialsException}
import generators.{AmlsReferenceNumberGenerator, BaseGenerator, GovernmentGatewayGenerator}
import models.governmentgateway.EnrolmentRequest
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{CorePost, HttpResponse}
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}
import utils.{DependencyMocks, AmlsSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GovernmentGatewayConnectorSpec extends AmlsSpec
  with AmlsReferenceNumberGenerator
  with BaseGenerator
  with GovernmentGatewayGenerator
  with ScalaFutures {

  //noinspection ScalaStyle
  implicit val defaultPatience =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(500, Millis))

  trait Fixture extends DependencyMocks {
    val connector = new GovernmentGatewayConnector {
      override protected[connectors] val http = mock[CorePost]
      override protected val enrolUrl = "/testurl"
      override private[connectors] val audit = mock[Audit]
    }

    val fn: DataEvent => Unit = d => {}
    when(connector.audit.sendDataEvent) thenReturn fn

    def mockHttpCall(response: Future[HttpResponse]) = when {
      connector.http.POST[EnrolmentRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any())
    } thenReturn response
  }

  "enrol" when {
    "called" must {
      "enrol the user via an HTTP call" in new Fixture {
        mockHttpCall(Future.successful(HttpResponse(OK)))

        whenReady(connector.enrol(enrolmentRequestGen.sample.get)) { result =>
          result.status mustBe OK
        }
      }

      "throw DuplicateEnrolmentException when a duplicate enrolment message was received" in new Fixture {
        mockHttpCall(Future.failed(new Exception(GovernmentGatewayConnector.duplicateEnrolmentMessage)))

        intercept[DuplicateEnrolmentException] {
          await(connector.enrol(enrolmentRequestGen.sample.get))
        }
      }

      "throw InvalidEnrolmentCredentialsException when an invalid credentials message was received" in new Fixture {
        mockHttpCall(Future.failed(new Exception(GovernmentGatewayConnector.invalidCredentialsMessage)))

        intercept[InvalidEnrolmentCredentialsException] {
          await(connector.enrol(enrolmentRequestGen.sample.get))
        }
      }
    }
  }

}
