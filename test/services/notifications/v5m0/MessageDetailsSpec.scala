/*
 * Copyright 2023 HM Revenue & Customs
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

package services.notifications.v5m0

import connectors.AmlsNotificationConnector
import models.notifications.ContactType._
import models.notifications.{ContactType, IDType, NotificationDetails, NotificationRow}
import org.joda.time.{DateTime, DateTimeZone}
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
      receivedAt = new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC),
      false,
      "XJML00000200000",
      "1",
      IDType("132456")
    )

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


  "The Notification Service" must {

    "templateVersion = v5m0" when {

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
            "v5m0",
            accountTypeId
          ))

          result.get.messageText.get mustBe (
            """<p>Your application to be supervised by HM Revenue and Customs (HMRC) under The Money Laundering, Terrorist Financing and Transfer of Funds (Information on the Payer) Regulations 2017 has failed.</p>""" +
              """<p>As you’ve not paid the full fees due, your application has automatically expired.</p>""" +
              """<p>You need to be registered with a <a href="https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register">supervisory body</a>""" +
              """ if Money Laundering Regulations apply to your business. If you’re not supervised you may be subject to penalties and criminal charges.</p>""" +
              """<p>If you still need to be registered with HMRC you should submit a new application immediately. You can apply from your account """ +
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
            "v5m0",
            accountTypeId
          ))

          result.get.messageText.get mustBe (
            """<p>The recent changes made to your details have been approved.</p>""" +
              """<p>You can find details of your registration on your <a href="""" +
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
            "v5m0",
            accountTypeId
          ))

          result.get.messageText.get mustBe (
            """<p>The date your anti-money laundering supervision ended has been changed.</p>""" +
              """<p>You can see the new effective date on your <a href="""" +
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

          val result = await(service.getMessageDetails("regNo", "id", ContactType.ReminderToPayForVariation, "v5m0", accountTypeId))

          result.get.messageText.get mustBe "<p>You need to pay £1234.00 for the recent changes made to your details.</p><p>Your payment reference is: ABC1234.</p><p>Find details of how to pay on your status page.</p><p>It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"
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

          val result = await(service.getMessageDetails("regNo", "id", ContactType.ReminderToPayForApplication, "v5m0", accountTypeId))

          result.get.messageText.get mustBe "<p>You need to pay £1234.00 for your application to register with HM Revenue and Customs.</p><p>Your payment reference is: ABC1234.</p><p>Find details of how to pay on your status page.</p><p>It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"
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

          val result = await(service.getMessageDetails("regNo", "id", ContactType.ReminderToPayForRenewal, "v5m0", accountTypeId))

          result.get.messageText.get mustBe "<p>HMRC imposes an annual charge for supervision. You need to pay this to continue your registration with HMRC.</p><p>Your payment reference is: ABC1234.</p><br><br><p>If you do not pay your fees within 28 working days from the date of this message, HMRC will cancel your business’s registration.</p><p>HMRCs money laundering registration policy, and details of how to pay your fee can be found on <a href=https://www.gov.uk>www.gov.uk.</a></p><p>If you are experiencing difficulties in carrying out these actions, please contact MLRCIT@hmrc.gov.uk.</p><p>If you continue to trade in activities covered by the Money Laundering Regulations and are not registered with the relevant supervisory body (such as HMRC), you may be subject to civil sanctions or criminal proceedings.</p><p>If you have already paid, please ignore this message.</p>"
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

          val result = await(service.getMessageDetails("regNo", "id", ContactType.ReminderToPayForManualCharges, "v5m0", accountTypeId))

          result.get.messageText.get mustBe "<p>You need to pay £1234.00 for the recent charge added to your account.</p><p>Your payment reference is: ABC12" +
            "34.</p><p>Find details of how to pay on your status page.</p><p>It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"
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

          val result = await(service.getMessageDetails("regNo", "id", ContactType.ApplicationApproval, "v5m0", accountTypeId))

          result.get.messageText.get mustBe (s"<p>Your application to register has been approved. You’re now registered until 2018-07-31.</p><p>Your anti-money laundering registration number is: ABC1234.</p><p>You can find details of your registration on your <a href=${"\"" + controllers.routes.StatusController.get().url + "\""}>status page</a>.</p>")
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

          val result = await(service.getMessageDetails("regNo", "id", ContactType.RenewalApproval, "v5m0", accountTypeId))

          result.get.messageText.get mustBe (s"<p>Your annual fee has been paid.</p><p>To continue to be registered with HMRC you will need to repeat this process next year. Your next annual fee will be due before 2018-07-31.</p><p>HMRC will contact you again to remind you to complete the renewal questions and pay your annual fee.</p><p>You can find your registration details on your status page.</p>")
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

          val result = await(service.getMessageDetails("regNo", "id", ContactType.AutoExpiryOfRegistration, "v5m0", accountTypeId))

          result.get.messageText.get mustBe (s"<p>Your business’s registration with HMRC is cancelled. Despite previous reminders to pay the fees imposed under the money laundering regulations you have failed to do so.</p><p>The cancellation will come into effect in 14 days from the date of this notice to allow you to conclude relevant business.</p><p><h3>To be registered by HMRC you must submit a new registration application and pay the correct fees.</h3></p><p>If your business is not registered with a relevant supervisory body (such as HMRC), you and/or your business may be subject to civil sanctions, such as a fine, or criminal proceedings if you continue to trade in activities covered by the regulations.</p><p><h3>If you/the business disagree with this decision</h3></p><p>Under the regulations, HMRC must offer you a review of their decision.</p><p>You have 30 days to:<br><li>Contact us to ask for a HMRC review or</li><br><li>Appeal directly to an independent tribunal, if you do not want a review</li><br></p><p>HMRC reviews are carried out by an independent team. You will be notified of the outcome of the review within 45 days unless another time is agreed.</p><p>If you are not satisfied with the conclusion of the review, you still have a right of appeal to a tribunal. These are administered by the Tribunal Service and you will have 30 days to appeal following notification of the outcome of the review.</p><p>HMRC’s money laundering registration policy can be found on <a href= https://www.gov.uk>www.gov.uk, where you can also find</a> information about what you can do <a href= https://www.gov.uk/guidance/money-laundering-regulations-appeals-and-penalties>if you disagree with an HMRC decision, including</a> how to request a review of our decision or <a href=https://www.gov.uk/tax-tribunal>make appeal to the tribunal.</a></p>")
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

          val result = await(service.getMessageDetails("regNo", "id", ContactType.RenewalReminder, "v5m0", accountTypeId))

          result.get.messageText.get mustBe (s"<p>It is time to pay your business’s annual fee.</p><p>You need to check and verify that your details are up to date and pay your annual fee in full by 31-07-2018.</p><p>Failing to pay your fee in a timely manner may lead to HMRC cancelling your registration.</p><p>Start this process from your status page. Completing this will tell you the amount of your annual fee.</p><p>HMRCs money laundering registration policy, and details of how to pay your fee can be found on <a href=https://www.gov.uk>www.gov.uk.</a></p><p> If your business is not registered with a relevant supervisory body (such as HMRC) you and/or your business may be subject to civil sanctions, such as a fine, or criminal proceedings if you continue to trade in activities covered by the Regulations.</p><p>If you have taken the steps outlined above, please ignore this message.</p>")

        }

        "contact type is NewRenewalReminder" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(NotificationDetails(
              Some(NewRenewalReminder),
              None,
              Some(messageWithDate),
              true,
              dateTime
            ))))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.NewRenewalReminder, "v5m0", accountTypeId))

          result.get.messageText.get mustBe (s"<p>It is time to pay your business’s annual fee.</p><p>You need to check and verify that your details are up to date and pay your annual fee in full by 31-07-2018.</p><p>If you do not pay your fees by the above date, HMRC will cancel your business’s registration.</p><p>Start this process from your status page. Completing this will tell you the amount of your annual fee.</p><p>HMRCs money laundering registration policy will assist you in understanding and calculating your fee.</p><p>Details of how to pay your fee can be found on <a href=https://www.gov.uk>www.gov.uk.</a></p><p>If you are experiencing difficulties in carrying out these actions, please contact MLRCIT@hmrc.gov.uk.</p><p>If your business is not registered with a relevant supervisory body (such as HMRC) you and/or your business may be subject to civil sanctions, such as a fine, or criminal proceedings if you continue to trade in activities covered by the Regulations.</p><p>If you have taken the steps outlined above, please ignore this message.</p>")

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

          val result = await(service.getMessageDetails("regNo", "id", ContactType.MindedToRevoke, "v5m0", accountTypeId))

          result.get.messageText.get mustBe CustomAttributeProvider.commonMark(message)
        }

      }

      "return None" when {
        "getMessageDetails returns None and message is of type with end date only message" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.RenewalReminder, "v5m0", accountTypeId))

          result mustBe None

        }

        "getMessageDetails returns None and message is of type with end date and ref number message" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.ApplicationApproval, "v5m0", accountTypeId))

          result mustBe None

        }

        "getMessageDetails returns None and message is of type with ref number, amount and status message" in new Fixture {

          when(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(None))

          val result = await(service.getMessageDetails("regNo", "id", ContactType.ReminderToPayForVariation, "v5m0", accountTypeId))

          result mustBe None

        }

      }

    }

  }

}
