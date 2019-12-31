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

import config.ApplicationConfig
import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.RunMode
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.AmlsSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestOnlyStubConnectorSpec extends AmlsSpec
  with MustMatchers
  with ScalaFutures
  with MockitoSugar {

  // scalastyle:off magic.number
  trait Fixture {
    val http = mock[HttpClient]
    val config = mock[ApplicationConfig]
    val connector = new TestOnlyStubConnector(http, mock[ApplicationConfig],  mock[Configuration], mock[RunMode])
  }

  "The TestOnly Stub Connector" must {
    "clear the state from the stubs service" in new Fixture {

      when {
        http.DELETE[HttpResponse](any(), any())(any(), any(), any())
      } thenReturn Future.successful(HttpResponse(NO_CONTENT))

      whenReady(connector.clearState()) { _ =>
        verify(http).DELETE[HttpResponse](any(), any())(any(), any(), any())
      }
    }
  }
}
