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
import models.notifications._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.HttpClientMocker

import java.time.ZoneOffset.UTC
import java.time.{Instant, LocalDateTime}
import scala.concurrent.ExecutionContext.Implicits.global

class AmlsNotificationConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  val safeId                          = "SAFEID"
  val accountTypeId: (String, String) = ("org", "id")
  val (accountType, accountId)        = accountTypeId

  val amlsRegistrationNumber  = "amlsRefNumber"
  val dateTime: LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(1479730062573L), UTC)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private trait Fixture {
    val mocker                               = new HttpClientMocker()
    private val configuration: Configuration = Configuration.load(Environment.simple())
    private val config                       = new ApplicationConfig(configuration, new ServicesConfig(configuration))
    val connector                            = new AmlsNotificationConnector(mocker.httpClient, config)
    val baseUrl                              = "http://localhost:8942/amls-notification"
  }

  "AmlsNotificationConnector" must {
    "retrieve notifications" when {
      "given amlsRegNo" in new Fixture {

        val response: Seq[NotificationRow] = Seq(
          NotificationRow(
            None,
            None,
            None,
            variation = true,
            LocalDateTime.of(1981, 12, 1, 1, 3),
            isRead = false,
            "amlsRefNumber",
            "1",
            IDType("")
          )
        )

        mocker.mockGet[Seq[NotificationRow]](url"$baseUrl/$accountType/$accountId/$amlsRegistrationNumber", response)
        whenReady(connector.fetchAllByAmlsRegNo(amlsRegistrationNumber, accountTypeId)) {
          _ mustBe response
        }
      }

      "given safeId" in new Fixture {
        val safeId                         = "AA1234567891234"
        val response: Seq[NotificationRow] = Seq(
          NotificationRow(
            None,
            None,
            None,
            variation = true,
            LocalDateTime.of(1981, 12, 1, 1, 3),
            isRead = false,
            "XJML00000200000",
            "1",
            IDType("")
          )
        )

        mocker.mockGet[Seq[NotificationRow]](url"$baseUrl/$accountType/$accountId/safeId/$safeId", response)

        whenReady(connector.fetchAllBySafeId(safeId, accountTypeId)) {
          _ mustBe response
        }
      }
    }

    "the call to notification service is successful (using Amls Reg No)" must {
      "return the response" in new Fixture {

        val response: NotificationDetails = NotificationDetails(
          Some(ContactType.MindedToReject),
          Some(Status(Some(StatusType.Approved), Some(RejectedReason.FailedToPayCharges))),
          Some("Text of the message"),
          variation = false,
          dateTime
        )

        mocker.mockGet[Option[NotificationDetails]](
          url"$baseUrl/$accountType/$accountId/$amlsRegistrationNumber/NOTIFICATIONID",
          Some(response)
        )

        connector
          .getMessageDetailsByAmlsRegNo(
            amlsRegistrationNumber = amlsRegistrationNumber,
            contactNumber = "NOTIFICATIONID",
            accountTypeId = accountTypeId
          )
          .futureValue
          .value mustBe response

      }
    }

    "the call to notification service returns a Bad Request" must {
      "Fail the future with an upstream 5xx exception (using amls reg no)" in new Fixture {

        mocker.mockGet[Option[NotificationDetails]](
          url"$baseUrl/$accountType/$accountId/$amlsRegistrationNumber/NOTIFICATIONID",
          new BadRequestException("GET of blah returned status 400.")
        )

        whenReady(
          connector.getMessageDetailsByAmlsRegNo(amlsRegistrationNumber, "NOTIFICATIONID", accountTypeId).failed
        ) { exception =>
          exception mustBe a[BadRequestException]
        }
      }
    }

    "the call to notification service returns Not Found (when using amls reg no)" must {
      "return a None " in new Fixture {

        mocker.mockGet[Option[NotificationDetails]](
          url"$baseUrl/$accountType/$accountId/$amlsRegistrationNumber/NOTIFICATIONID",
          None
        )

        whenReady(
          connector.getMessageDetailsByAmlsRegNo(
            amlsRegistrationNumber = amlsRegistrationNumber,
            contactNumber = "NOTIFICATIONID",
            accountTypeId = accountTypeId
          )
        ) { result =>
          result must be(None)
        }
      }
    }
  }
}
