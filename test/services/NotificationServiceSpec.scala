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

package services

import connectors.AmlsNotificationConnector
import models.notifications.ContactType._
import models.notifications.{ContactType, IDType, NotificationDetails, NotificationRow}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class NotificationServiceSpec extends AmlsSpec with MockitoSugar with GeneratorDrivenPropertyChecks{

  implicit val hc = HeaderCarrier()

  trait Fixture extends AuthorisedFixture {

    implicit val authContext = mock[AuthContext]

    val amlsNotificationConnector = mock[AmlsNotificationConnector]

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[AmlsNotificationConnector].to(amlsNotificationConnector))
      .bindings(bind[MessagesApi].to(messagesApi)).build()

    val service = injector.instanceOf[NotificationService]

    val testNotifications = NotificationRow(
      status = None,
      contactType = None,
      contactNumber = None,
      variation = false,
      receivedAt = new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC),
      isRead = false,
      amlsRegistrationNumber = "XJML00000200000",
      templatePackageVersion = "1",
      _id = IDType("132456")
    )

    val accountTypeId = ("org","id")

    val dateTime = new DateTime(1479730062573L, DateTimeZone.UTC)

    val testList = Seq(
      testNotifications.copy(contactType = Some(ApplicationApproval), receivedAt = new DateTime(1981, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(variation = true, receivedAt = new DateTime(1976, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(RenewalApproval), receivedAt = new DateTime(2016, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(RejectionReasons), receivedAt = new DateTime(2001, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications,
      testNotifications.copy(contactType = Some(RevocationReasons), receivedAt = new DateTime(1998, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(AutoExpiryOfRegistration), receivedAt = new DateTime(2017, 11, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(ReminderToPayForApplication), receivedAt = new DateTime(2012, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(ReminderToPayForVariation), receivedAt = new DateTime(2017, 12, 1, 3, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(ReminderToPayForRenewal), receivedAt = new DateTime(2017, 12, 3, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(ReminderToPayForManualCharges), receivedAt = new DateTime(2007, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(RenewalReminder), receivedAt = new DateTime(1991, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(MindedToReject), receivedAt = new DateTime(1971, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(MindedToRevoke), receivedAt = new DateTime(2017, 10, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(NoLongerMindedToReject), receivedAt = new DateTime(2003, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(NoLongerMindedToRevoke), receivedAt = new DateTime(2002, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(Others), receivedAt = new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC))
    )

  }

  val messageWithAmountRefNumberAndStatus = "parameter1-1234|parameter2-ABC1234|Status-04-Approved"
  val messageWithDateAndRefNumber = "parameter1-31/07/2018|parameter2-ABC1234"
  val messageWithDate = "parameter1-31/07/2018"

  val contactTypes = List[(ContactType, Option[String])](
    (ApplicationApproval, Some(messageWithDateAndRefNumber)),
    (RenewalApproval, Some(messageWithDate)),
    (RejectionReasons, Some(messageWithDateAndRefNumber)),
    (RevocationReasons, Some(messageWithDateAndRefNumber)),
    (AutoExpiryOfRegistration,Some(messageWithDate)),
    (ReminderToPayForApplication,Some(messageWithAmountRefNumberAndStatus)),
    (ReminderToPayForRenewal,Some(messageWithAmountRefNumberAndStatus)),
    (ReminderToPayForVariation,Some(messageWithAmountRefNumberAndStatus)),
    (ReminderToPayForManualCharges,Some(messageWithAmountRefNumberAndStatus)),
    (RenewalReminder,Some(messageWithDate)),
    (MindedToRevoke, Some(messageWithDateAndRefNumber)),
    (MindedToReject, Some(messageWithDateAndRefNumber)),
    (NoLongerMindedToReject, Some(messageWithDateAndRefNumber)),
    (NoLongerMindedToRevoke, Some(messageWithDateAndRefNumber)),
    (RegistrationVariationApproval, Some(messageWithDateAndRefNumber)),
    (ApplicationAutorejectionForFailureToPay, Some(messageWithDateAndRefNumber)),
    (DeRegistrationEffectiveDateChange, Some(messageWithDate)),
    (Others,Some(messageWithDateAndRefNumber)),
    (NoSubject, Some(messageWithDate)),
    (NoSubject, None))

  "The Notification Service" must {
    "get all notifications in order" in new Fixture {

      when(amlsNotificationConnector.fetchAllBySafeId(any(), any())(any(), any()))
        .thenReturn(Future.successful(testList))

      val result = await(service.getNotifications("testNo", accountTypeId))
      result.head.receivedAt mustBe new DateTime(2017, 12, 3, 1, 3, DateTimeZone.UTC)
    }

    "return content of the notification for every type of notification" in new Fixture {

      for(cType <- contactTypes) {
        when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(NotificationDetails(
            messageText = cType._2,
            contactType = Some(ApplicationApproval),
            status = None,
            variation = false,
            receivedAt = new DateTime(2017, 12, 3, 1, 3, DateTimeZone.UTC)))))

        val result = await(service.getMessageDetails("", "", cType._1,"v1m0", accountTypeId))
        result mustBe defined
        result.value.messageText mustBe defined
      }
    }
  }
}
