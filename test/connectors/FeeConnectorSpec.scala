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
import generators.AmlsReferenceNumberGenerator
import models.ResponseType.SubscriptionResponseType
import models._
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.Org
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FeeConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures with AmlsReferenceNumberGenerator {

  val connector = new FeeConnector(
    http = mock[HttpGet],
    appConfig = mock[AppConfig]
  )

  val safeId = "SAFEID"

  implicit val hc = HeaderCarrier()
  implicit val ac = AuthContext(
    LoggedInUser(
      "UserName",
      None,
      None,
      None,
      CredentialStrength.Weak,
      ConfidenceLevel.L50, ""),
    Principal(
      None,
      Accounts(org = Some(OrgAccount("Link", Org("TestOrgRef"))))),
    None,
    None,
    None, None)

  "FeeConnector" must {

    val feeResponse = FeeResponse(
      SubscriptionResponseType,
      amlsRegistrationNumber,
      150.00,
      Some(100.0),
      300.0,
      550.0,
      Some("XA000000000000"),
      None,
      new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC)
    )

    "successfully receive feeResponse" in {

      when {
        connector.feePaymentUrl
      } thenReturn "/amls/feePaymentUrl"

      when {
        connector.http.GET[FeeResponse](eqTo(s"${connector.feePaymentUrl}/org/TestOrgRef/$amlsRegistrationNumber"))(any(),any(), any())
      } thenReturn Future.successful(feeResponse)

      whenReady(connector.feeResponse(amlsRegistrationNumber)){
        _ mustBe feeResponse
      }
    }
  }
}
