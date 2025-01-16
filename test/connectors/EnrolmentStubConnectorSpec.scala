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
import generators.BaseGenerator
import models.enrolment.{EnrolmentIdentifier, GovernmentGatewayEnrolment}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.{AmlsSpec, HttpClientMocker}

class EnrolmentStubConnectorSpec extends AmlsSpec with BaseGenerator {

  // scalastyle:off magic.number
  trait TestFixture {
    val enrolments: Seq[GovernmentGatewayEnrolment] = Seq(GovernmentGatewayEnrolment("HMRC-MLR-ORG",
      List(EnrolmentIdentifier("MLRRefNumber", "AV23456789")), ""))

    val mocker = new HttpClientMocker()
    private val configuration: Configuration = Configuration.load(Environment.simple())
    private val config = new ApplicationConfig(configuration, new ServicesConfig(configuration))
    val baseUrl = "htttp://sialala"
    val connector = new EnrolmentStubConnector(mocker.httpClient, config)
    val groupId: String = stringOfLengthGen(10).sample.get
  }

  "The Enrolment Stub Connector" must {
    "get the enrolments from the stubs service" in new TestFixture {
      mocker.mockGet(url"$baseUrl/auth/oid/$groupId/enrolments", enrolments)
      connector.enrolments(groupId).futureValue mustBe enrolments
    }
  }
}
