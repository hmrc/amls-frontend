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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HttpClient, HttpResponse, StringContextOps}
import utils.{AmlsSpec, HttpClientMocker}

import scala.concurrent.Future

class TestOnlyStubConnectorSpec extends AmlsSpec
  with Matchers
  with ScalaFutures
  with MockitoSugar {

  // scalastyle:off magic.number
  trait Fixture {

    val mocker = new HttpClientMocker
    val connector = new TestOnlyStubConnector(mocker.httpClient, appConfig, app.injector.instanceOf[Configuration])
  }

  "The TestOnly Stub Connector" must {
    "clear the state from the stubs service" in new Fixture {

      private val response: HttpResponse = HttpResponse(NO_CONTENT, "")
      mocker.mockDelete(url"http://localhost:8941/anti-money-laundering/test-only/clearstate", response)
      connector.clearState().futureValue mustBe response
    }
  }
}
