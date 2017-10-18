/*
 * Copyright 2017 HM Revenue & Customs
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

import models.notifications._
import org.joda.time.{DateTime, DateTimeZone, LocalDateTime}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.Org
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.http._
import org.joda.time.{DateTimeZone, LocalDateTime}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier, HttpGet, HttpPost, NotFoundException }

class AmlsNotificationConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  val safeId = "SAFEID"
  val amlsRegistrationNumber = "AMLSREGNO"
  val dateTime = new DateTime(1479730062573L, DateTimeZone.UTC)

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

  private trait Fixture {
    val mockConnector =  mock[HttpGet]

    val connector = new AmlsNotificationConnector {
      override private[connectors] def httpGet: HttpGet = mockConnector
      override private[connectors] def httpPost: HttpPost = mock[HttpPost]
      override private[connectors] def baseUrl: String = "amls-notification"
    }
  }

  "AmlsNotificationConnector" must {
    "retrieve notifications" when {
      "given amlsRegNo" in new Fixture {
        val amlsRegistrationNumber = "XAML00000000000"
        val response = Seq(
          NotificationRow(None, None, None, true, new DateTime(1981, 12, 1, 1, 3, DateTimeZone.UTC), false, "XJML00000200000", IDType(""))
        )
        val url = s"${connector.baseUrl}/org/TestOrgRef/$amlsRegistrationNumber"

        when {
          connector.httpGet.GET[Seq[NotificationRow]](eqTo(url))(any(), any())
        } thenReturn Future.successful(response)

        whenReady(connector.fetchAllByAmlsRegNo(amlsRegistrationNumber)) {
          _ mustBe response
        }
      }

      "given safeId" in new Fixture {
        val safeId = "AA1234567891234"
        val response = Seq(
          NotificationRow(None, None, None, true, new DateTime(1981, 12, 1, 1, 3, DateTimeZone.UTC), false, "XJML00000200000", IDType(""))
        )
        val url = s"${connector.baseUrl}/org/TestOrgRef/safeId/$safeId"

        when {
          connector.httpGet.GET[Seq[NotificationRow]](eqTo(url))(any(), any())
        } thenReturn Future.successful(response)

        whenReady(connector.fetchAllBySafeId(safeId)) {
          _ mustBe response
        }
      }
    }

    "the call to notification service is successful (using Amls Reg No)" must {
      "return the response" in new Fixture {

        val url = s"${connector.baseUrl}/org/TestOrgRef/$amlsRegistrationNumber/NOTIFICATIONID"

        when(connector.httpGet.GET[NotificationDetails](eqTo(url))(any(), any()))
          .thenReturn(Future.successful(NotificationDetails(
            Some(ContactType.MindedToReject),
            Some(Status(Some(StatusType.Approved),
            Some(RejectedReason.FailedToPayCharges))),
            Some("Text of the message"),
            false,
            dateTime
          )))

        whenReady(connector.getMessageDetailsByAmlsRegNo(amlsRegistrationNumber, "NOTIFICATIONID")) { result =>
          result must be (Some(NotificationDetails(
            Some(ContactType.MindedToReject),
            Some(Status(Some(StatusType.Approved),
            Some(RejectedReason.FailedToPayCharges))),
            Some("Text of the message"),
            false,
            dateTime
          )))
        }
      }
    }

    "the call to notification service returns a Bad Request" must {
      "Fail the future with an upstream 5xx exception (using amls reg no)" in new Fixture {
        val url = s"${connector.baseUrl}/org/TestOrgRef/$amlsRegistrationNumber/NOTIFICATIONID"

        when(connector.httpGet.GET[NotificationDetails](eqTo(url))(any(), any()))
          .thenReturn(Future.failed(new BadRequestException("GET of blah returned status 400.")))

        whenReady(connector.getMessageDetailsByAmlsRegNo(amlsRegistrationNumber, "NOTIFICATIONID").failed) { exception =>
          exception mustBe a[BadRequestException]
        }
      }
    }

    "the call to notification service returns Not Found (when using amls reg no)" must {
      "return a None " in new Fixture {

        val url = s"${connector.baseUrl}/org/TestOrgRef/$amlsRegistrationNumber/NOTIFICATIONID"

        when(connector.httpGet.GET[NotificationDetails](eqTo(url))(any(), any()))
          .thenReturn(Future.failed(new NotFoundException("GET of blah returned status 404.")))

        whenReady(connector.getMessageDetailsByAmlsRegNo(amlsRegistrationNumber, "NOTIFICATIONID")) { result =>
          result must be (None)
        }
      }
    }
  }
}
