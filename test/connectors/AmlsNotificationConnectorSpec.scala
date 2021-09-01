/*
 * Copyright 2021 HM Revenue & Customs
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
import models.notifications._
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmlsNotificationConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  val safeId = "SAFEID"
  val accountTypeId = ("org","id")
  val amlsRegistrationNumber = "amlsRefNumber"
  val dateTime = new DateTime(1479730062573L, DateTimeZone.UTC)

  implicit val hc = HeaderCarrier()

  private trait Fixture {
    val connector = new AmlsNotificationConnector (mock[HttpClient], mock[ApplicationConfig])
  }

  "AmlsNotificationConnector" must {
    "retrieve notifications" when {
      "given amlsRegNo" in new Fixture {

        val response = Seq(
          NotificationRow(None, None, None, true, new DateTime(1981, 12, 1, 1, 3, DateTimeZone.UTC), false, "amlsRefNumber", "1", IDType(""))
        )

        when {
          connector.http.GET[Seq[NotificationRow]](any(), any(), any())(any(), any(), any())
        } thenReturn Future.successful(response)

        whenReady(connector.fetchAllByAmlsRegNo(amlsRegistrationNumber, accountTypeId)) {
          _ mustBe response
        }
      }

      "given safeId" in new Fixture {
        val safeId = "AA1234567891234"
        val response = Seq(
          NotificationRow(None, None, None, true, new DateTime(1981, 12, 1, 1, 3, DateTimeZone.UTC), false, "XJML00000200000", "1", IDType(""))
        )

        when {
          connector.http.GET[Seq[NotificationRow]](any(), any(), any())(any(), any(), any())
        } thenReturn Future.successful(response)

        whenReady(connector.fetchAllBySafeId(safeId, accountTypeId)) {
          _ mustBe response
        }
      }
    }

    "the call to notification service is successful (using Amls Reg No)" must {
      "return the response" in new Fixture {

        when(connector.http.GET[NotificationDetails](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(NotificationDetails(
            Some(ContactType.MindedToReject),
            Some(Status(Some(StatusType.Approved),
            Some(RejectedReason.FailedToPayCharges))),
            Some("Text of the message"),
            false,
            dateTime
          )))

        whenReady(connector.getMessageDetailsByAmlsRegNo(amlsRegistrationNumber, "NOTIFICATIONID", accountTypeId)) { result =>
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

        when(connector.http.GET[NotificationDetails](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.failed(new BadRequestException("GET of blah returned status 400.")))

        whenReady(connector.getMessageDetailsByAmlsRegNo(amlsRegistrationNumber, "NOTIFICATIONID", accountTypeId).failed) { exception =>
          exception mustBe a[BadRequestException]
        }
      }
    }

    "the call to notification service returns Not Found (when using amls reg no)" must {
      "return a None " in new Fixture {

        when(connector.http.GET[NotificationDetails](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.failed(new NotFoundException("GET of blah returned status 404.")))

        whenReady(connector.getMessageDetailsByAmlsRegNo(amlsRegistrationNumber, "NOTIFICATIONID", accountTypeId)) { result =>
          result must be (None)
        }
      }
    }
  }
}
