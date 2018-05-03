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

import config.AppConfig
import models.enrolment.{EnrolmentIdentifier, GovernmentGatewayEnrolment}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}
import utils.AmlsSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentStubConnectorSpec extends AmlsSpec {

  def fixture = new {
    implicit val hc = HeaderCarrier()

    val enrolments = Seq(GovernmentGatewayEnrolment("HMRC-MLR-ORG",
      List(EnrolmentIdentifier("MLRRefNumber", "AV23456789")), ""))

    val http = mock[HttpGet]
    val config = mock[AppConfig]
    val connector = new EnrolmentStubConnector(http, config)
  }

  "The Enrolment Stub Connector" must {
    "get the enrolments from the stubs service" in {
      val f = fixture

      when {
        f.http.GET[Seq[GovernmentGatewayEnrolment]](any())(any(), any(), any())
      } thenReturn Future.successful(f.enrolments)

      val result = await(f.connector.enrolments)

      result mustBe f.enrolments
    }
  }
}
