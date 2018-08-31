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

package views.notifications.v1

import models.notifications.ContactType._
import models.notifications.StatusType._
import models.notifications.{IDType, NotificationParams, NotificationRow, Status}
import org.joda.time.DateTime
import org.scalatest.MustMatchers
import utils.AmlsSpec
import views.Fixture

class your_messages_flattenSpec extends AmlsSpec with MustMatchers {

    trait ViewFixture extends Fixture {
        implicit val requestWithToken = addToken(request)
        val currentNotifications: Seq[NotificationRow] = Seq(
            NotificationRow(None, None, None, variation = true, new DateTime(2018, 1, 1, 0, 0), isRead = true, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(Expired), None)), None, None, variation = true, new DateTime(2018, 1, 2, 0, 0), isRead = false, "XA000", IDType("id")),
            NotificationRow(None, Some(ApplicationApproval), None, variation = true, new DateTime(2018, 1, 3, 0, 0), isRead = true, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(Approved), None)), Some(RenewalApproval), None, variation = true, new DateTime(2018, 1, 4, 0, 0), isRead = false, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(Rejected), None)), Some(ReminderToPayForVariation), None, variation = true, new DateTime(2018, 1, 5, 0, 0), isRead = true, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(Revoked), None)), Some(RevocationReasons), None, variation = true, new DateTime(2018, 1, 6, 0, 0), isRead = false, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(DeRegistered), None)), Some(AutoExpiryOfRegistration), None, variation = true, new DateTime(2018, 1, 7, 0, 0), isRead = true, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(Expired), None)), Some(MindedToReject), None, variation = true, new DateTime(2018, 1, 8, 0, 0), isRead = false, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(Approved), None)), Some(NoLongerMindedToRevoke), None, variation = true, new DateTime(2018, 1, 9, 0, 0), isRead = true, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(Rejected), None)), Some(RegistrationVariationApproval), None, variation = true, new DateTime(2018, 1, 10, 0, 0), isRead = false, "XA000", IDType("id"))
        )
        val previousNotifications: Seq[NotificationRow] = Seq(
            NotificationRow(None, None, None, variation = true, new DateTime(2018, 1, 1, 0, 0), isRead = true, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(Expired), None)), None, None, variation = true, new DateTime(2018, 1, 2, 0, 0), isRead = false, "XA000", IDType("id")),
            NotificationRow(None, Some(ApplicationApproval), None, variation = true, new DateTime(2018, 1, 3, 0, 0), isRead = true, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(Approved), None)), Some(RenewalApproval), None, variation = true, new DateTime(2018, 1, 4, 0, 0), isRead = false, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(Rejected), None)), Some(ReminderToPayForVariation), None, variation = true, new DateTime(2018, 1, 5, 0, 0), isRead = true, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(Revoked), None)), Some(RevocationReasons), None, variation = true, new DateTime(2018, 1, 6, 0, 0), isRead = false, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(DeRegistered), None)), Some(AutoExpiryOfRegistration), None, variation = true, new DateTime(2018, 1, 7, 0, 0), isRead = true, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(Expired), None)), Some(MindedToReject), None, variation = true, new DateTime(2018, 1, 8, 0, 0), isRead = false, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(Approved), None)), Some(NoLongerMindedToRevoke), None, variation = true, new DateTime(2018, 1, 9, 0, 0), isRead = true, "XA000", IDType("id")),
            NotificationRow(Some(Status(Some(Rejected), None)), Some(RegistrationVariationApproval), None, variation = true, new DateTime(2018, 1, 10, 0, 0), isRead = false, "XA000", IDType("id"))
        )

        val notificationParams = NotificationParams(businessName = "businessName", currentApplicationNotification = currentNotifications, previousApplicationNotification = previousNotifications)
        val notificationParamsPreviousEmpty = NotificationParams(businessName = "businessName", currentApplicationNotification = currentNotifications)
        val notificationParamsCurrentEmpty = NotificationParams(businessName = "businessName", previousApplicationNotification = previousNotifications)
        val notificationParamsEmpty = NotificationParams(businessName = "businessName")
    }

    "your_messages flattened view" must {
        "be the same as non-flattened view when there are current and previous notifications" in new ViewFixture {
            val viewV1 = views.html.notifications.v1.your_messages(notificationParams)
            val htmlV1 = viewV1.body.filterNot(Set('\n', '\t', ' ').contains)

            val view = views.html.notifications.your_messages("businessName", currentNotifications, previousNotifications)
            val htmlUnflattened = view.body.filterNot(Set('\n', '\t', ' ').contains)

            htmlV1 mustEqual htmlUnflattened
        }

        "be the same as non-flattened view when there are current notifications only" in new ViewFixture {
            val viewV1 = views.html.notifications.v1.your_messages(notificationParamsPreviousEmpty)
            val htmlV1 = viewV1.body.filterNot(Set('\n', '\t', ' ').contains)

            val view = views.html.notifications.your_messages("businessName", currentNotifications, Seq())
            val htmlUnflattened = view.body.filterNot(Set('\n', '\t', ' ').contains)

            htmlV1 mustEqual htmlUnflattened
        }

        "be the same as non-flattened view when there are previous notifications only" in new ViewFixture {
            val viewV1 = views.html.notifications.v1.your_messages(notificationParamsCurrentEmpty)
            val htmlV1 = viewV1.body.filterNot(Set('\n', '\t', ' ').contains)

            val view = views.html.notifications.your_messages("businessName", Seq(), previousNotifications)
            val htmlUnflattened = view.body.filterNot(Set('\n', '\t', ' ').contains)

            htmlV1 mustEqual htmlUnflattened
        }

        "be the same as non-flattened view when there are neither current or previous notifications" in new ViewFixture {
            val viewV1 = views.html.notifications.v1.your_messages(notificationParamsEmpty)
            val htmlV1 = viewV1.body.filterNot(Set('\n', '\t', ' ').contains)

            val view = views.html.notifications.your_messages("businessName", Seq(), Seq())
            val htmlUnflattened = view.body.filterNot(Set('\n', '\t', ' ').contains)

            htmlV1 mustEqual htmlUnflattened
        }
    }

}
