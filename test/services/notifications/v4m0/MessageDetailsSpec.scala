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

package services.notifications.v4m0

import connectors.AmlsNotificationConnector
import models.notifications.ContactType._
import models.notifications.{ContactType, IDType, NotificationDetails, NotificationRow}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import services.{CustomAttributeProvider, NotificationService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.AmlsSpec

import java.time.ZoneOffset.UTC
import java.time.{Instant, LocalDateTime}
import scala.concurrent.Future

class MessageDetailsSpec extends AmlsSpec with MockitoSugar {

  implicit val hc = HeaderCarrier()

  trait Fixture {

    val accountTypeId = ("org", "id")

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
      receivedAt = LocalDateTime.of(2017, 12, 1, 1, 3),
      false,
      "XJML00000200000",
      "1",
      IDType("132456")
    )

    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(1479730062573L), UTC)

    val testList = Seq(
      testNotifications.copy(contactType = Some(ApplicationApproval), receivedAt = LocalDateTime.of(1981, 12, 1, 1, 3)),
      testNotifications.copy(variation = true, receivedAt = LocalDateTime.of(1976, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(RenewalApproval), receivedAt = LocalDateTime.of(2016, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(RejectionReasons), receivedAt = LocalDateTime.of(2001, 12, 1, 1, 3)),
      testNotifications,
      testNotifications.copy(contactType = Some(RevocationReasons), receivedAt = LocalDateTime.of(1998, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(AutoExpiryOfRegistration), receivedAt = LocalDateTime.of(2017, 11, 1, 1, 3)),
      testNotifications.copy(contactType = Some(ReminderToPayForApplication), receivedAt = LocalDateTime.of(2012, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(ReminderToPayForVariation), receivedAt = LocalDateTime.of(2017, 12, 1, 3, 3)),
      testNotifications.copy(contactType = Some(ReminderToPayForRenewal), receivedAt = LocalDateTime.of(2017, 12, 3, 1, 3)),
      testNotifications.copy(contactType = Some(ReminderToPayForManualCharges), receivedAt = LocalDateTime.of(2007, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(RenewalReminder), receivedAt = LocalDateTime.of(1991, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(MindedToReject), receivedAt = LocalDateTime.of(1971, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(MindedToRevoke), receivedAt = LocalDateTime.of(2017, 10, 1, 1, 3)),
      testNotifications.copy(contactType = Some(NoLongerMindedToReject), receivedAt = LocalDateTime.of(2003, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(NoLongerMindedToRevoke), receivedAt = LocalDateTime.of(2002, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(Others), receivedAt = LocalDateTime.of(2017, 12, 1, 1, 3))
    )

  }

  val messageWithAmountRefNumberAndStatus = "parameter1-1234|parameter2-ABC1234|Status-04-Approved"
  val messageWithDateAndRefNumber = "parameter1-31/07/2018|parameter2-ABC1234"
  val messageWithDate = "parameter1-31/07/2018"


  "The Notification Service" must {

    "templateVersion = v4m0" when {

      "return static message details" when {

        "contact type is auto-rejected for failure to pay" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(NotificationDetails(
              Some(ContactType.ApplicationAutorejectionForFailureToPay),
              None,
              None,
              true,
              dateTime
            ))))

          val result = await(service.getMessageDetails(
            "thing",
            "thing",
            ContactType.ApplicationAutorejectionForFailureToPay,
            "v4m0",
            accountTypeId
          ))

          result.get.messageText.get mustBe (
            """<p class="govuk-body">Your application to be supervised by HM Revenue and Customs (HMRC) under The Money Laundering, Terrorist Financing and Transfer of Funds (Information on the Payer) Regulations 2017 has failed.</p>""" +
              """<p class="govuk-body">As you’ve not paid the full fees due, your application has automatically expired.</p>""" +
              """<p class="govuk-body">You need to be registered with a <a href="https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register">supervisory body</a>""" +
              """ if Money Laundering Regulations apply to your business. If you’re not supervised you may be subject to penalties and criminal charges.</p>""" +
              """<p class="govuk-body">If you still need to be registered with HMRC you should submit a new application immediately. You can apply from your account """ +
              """<a href="""" +
              controllers.routes.StatusController.get() +
              """">status page</a>.</p>"""
            )
        }

        "contact type is registration variation approval" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(NotificationDetails(
              Some(ContactType.RegistrationVariationApproval),
              None,
              None,
              true,
              dateTime
            ))))

          val result = await(service.getMessageDetails(
            "thing",
            "thing",
            ContactType.RegistrationVariationApproval,
            "v4m0",
            accountTypeId
          ))

          result.get.messageText.get mustBe (
            """<p class="govuk-body">The recent changes made to your details have been approved.</p>""" +
              """<p class="govuk-body">You can find details of your registration on your <a href="""" +
              controllers.routes.StatusController.get() +
              """">status page</a>.</p>"""
            )
        }

        "contact type is DeRegistrationEffectiveDateChange" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(NotificationDetails(
              Some(ContactType.DeRegistrationEffectiveDateChange),
              None,
              None,
              true,
              dateTime
            ))))

          val result = await(service.getMessageDetails(
            "thing",
            "thing",
            ContactType.DeRegistrationEffectiveDateChange,
            "v4m0",
            accountTypeId
          ))

          result.get.messageText.get mustBe (
            """<p class="govuk-body">The date your anti-money laundering supervision ended has been changed.</p>""" +
              """<p class="govuk-body">You can see the new effective date on your <a href="""" +
              controllers.routes.StatusController.get() +
              """">status page</a>.</p>"""
            )
        }

      }

      "return correct message content" when {

        "contact type is ReminderToPayForVariation" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(NotificationDetails(
              Some(ReminderToPayForVariation),
              None,
              Some(messageWithAmountRefNumberAndStatus),
              true,
              dateTime
            ))))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.ReminderToPayForVariation, "v4m0", accountTypeId))

          result.get.messageText.get mustBe """<p class="govuk-body">You need to pay £1234.00 for the recent changes made to your details.</p><p class="govuk-body">Your payment reference is: ABC1234.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"""
        }

        "contact type is ReminderToPayForApplication" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(NotificationDetails(
              Some(ReminderToPayForApplication),
              None,
              Some(messageWithAmountRefNumberAndStatus),
              true,
              dateTime
            ))))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.ReminderToPayForApplication, "v4m0", accountTypeId))

          result.get.messageText.get mustBe """<p class="govuk-body">You need to pay £1234.00 for your application to register with HM Revenue and Customs.</p><p class="govuk-body">Your payment reference is: ABC1234.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"""
        }

        "contact type is ReminderToPayForRenewal" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(NotificationDetails(
              Some(ReminderToPayForRenewal),
              None,
              Some(messageWithAmountRefNumberAndStatus),
              true,
              dateTime
            ))))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.ReminderToPayForRenewal, "v4m0", accountTypeId))

          result.get.messageText.get mustBe """<p class="govuk-body">You need to pay £1234.00 for your annual fee to continue your registration with HMRC.</p><p class="govuk-body">Your payment reference is: ABC1234.</p><p class="govuk-body">Find out <a href="/anti-money-laundering/how-to-pay">how to pay your fees</a>.</p><p class="govuk-body">If you have already paid, please ignore this message.</p><p class="govuk-body">If you do not pay this fee when it is due, HMRC will cancel your registration.</p><p class="govuk-body">If your registration is cancelled, you are no longer registered for supervision with HMRC. You need to be registered with an <a href="https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register#businesses-already-supervised-for-money-laundering-purposes">appropriate supervisory body</a> if the Money Laundering Regulations apply to your business. You may be subject to civil sanctions or criminal proceedings if you continue to trade in activities covered by the Money Laundering, Terrorist Finance and Transfer of Funds (Information on the Payer) Regulations 2017.</p>"""
        }

        "contact type is ReminderToPayForManualCharges" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(NotificationDetails(
              Some(ReminderToPayForManualCharges),
              None,
              Some(messageWithAmountRefNumberAndStatus),
              true,
              dateTime
            ))))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.ReminderToPayForManualCharges, "v4m0", accountTypeId))

          result.get.messageText.get mustBe """<p class="govuk-body">You need to pay £1234.00 for the recent charge added to your account.</p><p class="govuk-body">Your payment reference is: ABC1234.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"""
        }

        "contact type is ApplicationApproval" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(NotificationDetails(
              Some(ApplicationApproval),
              None,
              Some(messageWithDateAndRefNumber),
              true,
              dateTime
            ))))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.ApplicationApproval, "v4m0", accountTypeId))

          result.get.messageText.get mustBe s"""<p class="govuk-body">Your application to register has been approved. You’re now registered until 2018-07-31.</p><p class="govuk-body">Your anti-money laundering registration number is: ABC1234.</p><p class="govuk-body">You can find details of your registration on your <a href="${controllers.routes.StatusController.get().url}">status page</a>.</p>"""
        }

        "contact type is RenewalApproval" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(NotificationDetails(
              Some(RenewalApproval),
              None,
              Some(messageWithDate),
              true,
              dateTime
            ))))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.RenewalApproval, "v4m0", accountTypeId))

          result.get.messageText.get mustBe s"""<p class="govuk-body">Your annual fee has been paid.</p><p class="govuk-body">To continue to be registered with HMRC you will need to repeat this process next year. Your next annual fee will be due before 2018-07-31.</p><p class="govuk-body">HMRC will contact you again to remind you to complete the renewal questions and pay your annual fee.</p><p class="govuk-body">You can find your registration details on your status page.</p>"""
        }

        "contact type is AutoExpiryOfRegistration" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(NotificationDetails(
              Some(AutoExpiryOfRegistration),
              None,
              Some(messageWithDate),
              true,
              dateTime
            ))))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.AutoExpiryOfRegistration, "v4m0", accountTypeId))

          result.get.messageText.get mustBe s"""<p class="govuk-body">Your registration for supervision under the Money Laundering, Terrorist Finance and Transfer of Funds (Information on the Payer) Regulations 2017 was cancelled on 2018-07-31. This is because you have not paid your annual fee.</p><p class="govuk-body">Your status page will show your supervision has expired. As a result, you are no longer registered for supervision with HMRC.</p><p class="govuk-body">Your registration was cancelled under Regulation 60(3)(a) with reference to Regulation 59(1)(c)(ii): failure to pay a charge imposed by HMRC under Part 11, Regulation 102(1)(c).</p><p class="govuk-body">You need to be <a href="https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register#businesses-already-supervised-for-money-laundering-purposes">registered with a supervisory body</a> if the Money Laundering Regulations apply to your business. If you are not registered for supervision, you may be subject to civil sanctions or criminal proceedings if you continue to trade in activities covered by the Money Laundering, Terrorist Finance and Transfer of Funds (Information on the Payer) Regulations 2017.</p><p class="govuk-body"><h2 class="govuk-heading-m">If you disagree with this decision</h2><p class="govuk-body">You have 30 days to:</p><ul class="govuk-list govuk-list--bullet"><li>ask for a review</li><li>appeal directly to an independent tribunal, if you do not want a review</li></ul></p><p class="govuk-body">HMRC reviews are carried out by an independent team. You will be notified of the outcome of the review within 45 days unless another time is agreed. If you are not satisfied with the conclusion of the review, you still have a right of appeal to a tribunal. These are administered by the Tribunal Service within 30 days of the notification of the outcome of the review.</p><p class="govuk-body">Find out how to request a review, and information about what you can do <a href="https://www.gov.uk/guidance/money-laundering-regulations-appeals-and-penalties#if-you-disagree-with-an-hmrc-decision">if you disagree with an HMRC decision</a>.</p><p class="govuk-body">Find further information about <a href="https://www.gov.uk/tax-tribunal">making an appeal to the tribunal</a>.</p>"""
        }

        "contact type is RenewalReminder" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(NotificationDetails(
              Some(RenewalReminder),
              None,
              Some(messageWithDate),
              true,
              dateTime
            ))))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.RenewalReminder, "v4m0", accountTypeId))

          result.get.messageText.get mustBe s"""<p class="govuk-body">It is time to renew your annual supervision registration with HMRC. You need to complete the renewal questions and pay your annual fee by 2018-07-31.</p><p class="govuk-body">Start this process from your status page by selecting ‘start your renewal’.</p><p class="govuk-body">You need to answer the questions and pay your annual fee in full. The annual fee is imposed under Regulation 102(1)(c) of the Money Laundering, Terrorist Finance and Transfer of Funds (Information on the Payer) Regulations 2017.</p><p class="govuk-body">If you do not pay your fees before this date, your registration will be cancelled.</p><p class="govuk-body">If we cancel your registration, you will no longer be supervised by HMRC.  You need to be <a href="https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register#businesses-already-supervised-for-money-laundering-purposes">registered with a supervisory body</a> if the Money Laundering Regulations apply to your business. You may be subject to civil sanctions or criminal proceedings if you continue to trade in activities covered by the Money Laundering, Terrorist Finance and Transfer of Funds (Information on the Payer) Regulations 2017.</p>"""
        }

        "content message is ETMP markdown" in new Fixture {

          val message = "<P># Test Heading</P><P>* bullet 1</P><P>* bullet 2</P><P>* bullet 3</P>"

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(NotificationDetails(
              Some(MindedToReject),
              None,
              Some(message),
              true,
              dateTime
            ))))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.MindedToRevoke, "v4m0", accountTypeId))

          result.get.messageText.get mustBe CustomAttributeProvider.commonMark(message)
        }
      }

      "return None" when {
        "getMessageDetails returns None and message is of type with end date only message" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.RenewalReminder, "v4m0", accountTypeId))

          result mustBe None
        }

        "getMessageDetails returns None and message is of type with end date and ref number message" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.ApplicationApproval, "v4m0", accountTypeId))

          result mustBe None
        }

        "getMessageDetails returns None and message is of type with ref number, amount and status message" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.ReminderToPayForVariation, "v4m0", accountTypeId))

          result mustBe None
        }
      }

      "static is called" must {

        "throw an exception" when {

          "the wrong contact type is supplied" in {

            the[Exception] thrownBy {
              MessageDetails.static(Others, "/foo")
            } must have message ("An Unknown Exception has occurred, v4m0:static():MessageDetails")
          }
        }
      }

      "endDate is called" must {

        "throw an exception" when {

          "the wrong contact type is supplied" in {

            the[Exception] thrownBy {
              MessageDetails.endDate(Others, "01-01-2023", "/foo", "123456")
            } must have message ("An Unknown Exception has occurred, v4m0:endDate():MessageDetails")
          }
        }
      }

      "reminder is called" must {

        "throw an exception" when {

          "the wrong contact type is supplied" in {

            the[Exception] thrownBy {
              MessageDetails.reminder(Others, "123", "123456")
            } must have message ("An Unknown Exception has occurred, v4m0:reminder():MessageDetails")
          }
        }
      }
    }
  }
}
