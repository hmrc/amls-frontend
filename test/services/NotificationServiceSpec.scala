package services

import connectors.AmlsNotificationConnector
import models.confirmation.Currency
import models.notifications.ContactType.{ApplicationApproval, AutoExpiryOfRegistration, MindedToReject, MindedToRevoke, NoLongerMindedToReject, NoLongerMindedToRevoke, Others, RejectionReasons, ReminderToPayForApplication, ReminderToPayForManualCharges, ReminderToPayForRenewal, ReminderToPayForVariation, RenewalApproval, RenewalReminder, RevocationReasons}
import models.notifications.{NotificationDetails, ContactType, IDType, NotificationRow}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.{Messages, I18nSupport, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, GenericTestHelper}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class NotificationServiceSpec  extends GenericTestHelper with MockitoSugar{

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
      false,
      IDType("132456")
    )



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

  "The Notification Service" must {

    "get all notifications in order" in new Fixture {

      when(amlsNotificationConnector.fetchAllByAmlsRegNo(any())(any(),any(),any())).thenReturn(Future.successful(testList))

      val result = await(service.getNotifications("testNo"))
      result.head.receivedAt mustBe(new DateTime(2017, 12, 3, 1, 3, DateTimeZone.UTC))
    }

    "return static message details when contact type is auto-rejected for failure to pay" in new Fixture {

      val result = await(service.getMessageDetails("thing","thing",ContactType.ApplicationAutorejectionForFailureToPay))

      verifyZeroInteractions(amlsNotificationConnector)
      result.get.messageText.get mustBe(
          """<p>Your application to be supervised by HM Revenue and Customs (HMRC) under the Money Laundering regulations 2007 has been rejected.</p>""" +
            """<p>As you’ve not paid the full fees due, your application has automatically expired.</p>""" +
            """<p>You need to be registered with a <a href="https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register">supervisory body</a>""" +
            """ if Money Laundering Regulations apply to your business. If you’re not supervised you may be subject to penalties and criminal charges.</p>""" +
            """<p>If you still need to be registered with HMRC you should submit a new application immediately. You can apply from """ +
            """<a href="""" +
            controllers.routes.StatusController.get() +
            """">your account status page</a>.</p>"""
        )
    }

    "return static message details when contact type is registration variation approval" in new Fixture {

      val result = await(service.getMessageDetails("thing","thing",ContactType.RegistrationVariationApproval))

      verifyZeroInteractions(amlsNotificationConnector)
      result.get.messageText.get mustBe(
        """<p>The recent changes made to your details have been approved.</p>""" +
          """<p>You can find details of your registration on <a href="""" +
          controllers.routes.StatusController.get() +
          """">your status page</a>.</p>"""
        )
    }

    "return static message details when contact type is DeRegistrationEffectiveDateChange" in new Fixture {

      val result = await(service.getMessageDetails("thing","thing",ContactType.DeRegistrationEffectiveDateChange))

      verifyZeroInteractions(amlsNotificationConnector)
      result.get.messageText.get mustBe(
        """<p>The date your anti-money laundering supervision ended has been changed.</p>""" +
          """<p>You can see the new effective date on <a href="""" +
          controllers.routes.StatusController.get() +
          """">your status page</a>.</p>"""
        )
    }

    "return correct message content when contact type is ReminderToPayForVariation" in new Fixture {

      val reminderVariationMessage = "parameter1-1234|parameter2-ABC1234|Status-04-Approved"

      when(amlsNotificationConnector.getMessageDetails(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(
          NotificationDetails(Some(ReminderToPayForVariation),
            None,
            Some(reminderVariationMessage),
            true))))

      val result = await(service.getMessageDetails("regNo", "id", ContactType.ReminderToPayForVariation))

      result.get.messageText.get mustBe Messages("notification.reminder.to.pay.ReminderToPayForVariation",Currency(1234),"ABC1234")
    }

    "return correct message content when contact type is ReminderToPayForApplication" in new Fixture {

      val reminderVariationMessage = "parameter1-1234|parameter2-ABC1234|Status-04-Approved"

      when(amlsNotificationConnector.getMessageDetails(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(
          NotificationDetails(Some(ReminderToPayForApplication),
            None,
            Some(reminderVariationMessage),
            true))))

      val result = await(service.getMessageDetails("regNo", "id", ContactType.ReminderToPayForApplication))

      result.get.messageText.get mustBe Messages("notification.reminder.to.pay.ReminderToPayForApplication",Currency(1234),"ABC1234")
    }

    "return correct message content when contact type is ReminderToPayForRenewal" in new Fixture {

      val reminderVariationMessage = "parameter1-1234|parameter2-ABC1234|Status-04-Approved"

      when(amlsNotificationConnector.getMessageDetails(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(
          NotificationDetails(Some(ReminderToPayForRenewal),
            None,
            Some(reminderVariationMessage),
            true))))

      val result = await(service.getMessageDetails("regNo", "id", ContactType.ReminderToPayForRenewal))

      result.get.messageText.get mustBe Messages("notification.reminder.to.pay.ReminderToPayForRenewal",Currency(1234),"ABC1234")
    }

    "return correct message content when contact type is ReminderToPayForManualCharges" in new Fixture {

      val reminderVariationMessage = "parameter1-1234|parameter2-ABC1234|Status-04-Approved"

      when(amlsNotificationConnector.getMessageDetails(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(
          NotificationDetails(Some(ReminderToPayForManualCharges),
            None,
            Some(reminderVariationMessage),
            true))))

      val result = await(service.getMessageDetails("regNo", "id", ContactType.ReminderToPayForManualCharges))

      result.get.messageText.get mustBe Messages("notification.reminder.to.pay.ReminderToPayForManualCharges",Currency(1234),"ABC1234")
    }

    "return correct message content when contact type is not ReminderToPay or static content" in new Fixture {

      val message = "parameter1-1234|parameter2-ABC1234|Status-04-Approved"

      when(amlsNotificationConnector.getMessageDetails(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(
          NotificationDetails(Some(MindedToReject),
            None,
            Some(message),
            true))))

      val result = await(service.getMessageDetails("regNo", "id", ContactType.MindedToReject))

      result.get.messageText.get mustBe message
    }
  }
}
