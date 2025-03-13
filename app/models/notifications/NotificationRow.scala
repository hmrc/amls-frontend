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

package models.notifications

import models.notifications.ContactType._
import models.notifications.RejectedReason._
import play.api.i18n.Messages
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.custom.JsPathSupport.{localDateTimeReads, localDateTimeWrites}
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import utils.ContactTypeHelper

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

case class NotificationRow(
  status: Option[Status],
  contactType: Option[ContactType],
  contactNumber: Option[String],
  variation: Boolean,
  receivedAt: LocalDateTime,
  isRead: Boolean,
  amlsRegistrationNumber: String,
  templatePackageVersion: String,
  _id: IDType
) {

  def dateReceived: String =
    receivedAt.format(DateTimeFormatter.ofPattern("d MMMM Y"))

  def subject: String = contactType match {
    case Some(RejectionReasons) =>
      (for {
        st <- status
        sr <- st.statusReason match {
                case Some(NonCompliant) | Some(FitAndProperFailure) | Some(OtherRefused) =>
                  Some("notifications.rejr.title")
                case _                                                                   => None
              }
      } yield sr) getOrElse "notifications.fail.title"

    case _ =>
      val cType = ContactTypeHelper.getContactType(status, contactType, variation)
      templatePackageVersion match {
        case _ if cType == ApplicationAutorejectionForFailureToPay => "notifications.fail.title"
        case "v1m0" | "v2m0" | "v3m0" | "v4m0"                     => s"notifications.subject.$cType"
        case "v5m0"                                                => s"notifications.subject.v5.$cType"
        case "v6m0"                                                => s"notifications.subject.v6.$cType"
        case _                                                     => throw new Exception(s"Unknown template version $templatePackageVersion")
      }

  }

  def notificationType: String = ContactTypeHelper.getContactType(status, contactType, variation) match {
    case ApplicationApproval | RegistrationVariationApproval | RenewalApproval | RejectionReasons |
        ApplicationAutorejectionForFailureToPay | RevocationReasons | AutoExpiryOfRegistration |
        RegistrationVariationApproval | ApplicationAutorejectionForFailureToPay | DeRegistrationEffectiveDateChange =>
      "notifications.type.statusChange"
    case ReminderToPayForApplication | ReminderToPayForVariation | ReminderToPayForRenewal |
        ReminderToPayForManualCharges | RenewalReminder | NewRenewalReminder =>
      "notifications.type.reminder"
    case _ => "notifications.type.communication"
  }

  def asTableRows(id: String, index: Int)(implicit messages: Messages): Seq[TableRow] = {

    val link =
      s"""
         |<a id="hyper-link-$id-$index" class="govuk-link"
         |href="${controllers.routes.NotificationController.messageDetails(
          _id.id,
          utils.ContactTypeHelper.getContactType(
            status,
            contactType,
            variation
          ),
          amlsRegistrationNumber,
          templatePackageVersion
        )}
         |">
         |${messages(this.subject)}
         |</a>
         |""".stripMargin

    val rowClass = "govuk-!-width-one-third"

    Seq(
      TableRow(
        HtmlContent(link)
      ),
      TableRow(
        Text(messages(notificationType)),
        classes = rowClass
      ),
      TableRow(
        Text(messages(dateReceived)),
        classes = rowClass
      )
    )
  }
}

object NotificationRow {
  implicit val dateTimeFormat: Format[LocalDateTime] = Format(localDateTimeReads, localDateTimeWrites)

  val reads: Reads[NotificationRow] =
    (
      (JsPath \ "status").readNullable[Status] and
        (JsPath \ "contactType").readNullable[ContactType] and
        (JsPath \ "contactNumber").readNullable[String] and
        (JsPath \ "variation").read[Boolean] and
        (JsPath \ "receivedAt").read[LocalDateTime](localDateTimeReads) and
        (JsPath \ "isRead").read[Boolean] and
        (JsPath \ "amlsRegistrationNumber").read[String] and
        (JsPath \ "templatePackageVersion").read[String] and
        (JsPath \ "_id").read[IDType]
    )(NotificationRow.apply _)

  val writes: OWrites[NotificationRow] =
    (
      (JsPath \ "status").writeNullable[Status] and
        (JsPath \ "contactType").writeNullable[ContactType] and
        (JsPath \ "contactNumber").writeNullable[String] and
        (JsPath \ "variation").write[Boolean] and
        (JsPath \ "receivedAt").write[LocalDateTime](localDateTimeWrites) and
        (JsPath \ "isRead").write[Boolean] and
        (JsPath \ "amlsRegistrationNumber").write[String] and
        (JsPath \ "templatePackageVersion").write[String] and
        (JsPath \ "_id").write[IDType]
    )(unlift(NotificationRow.unapply))

  implicit val format: OFormat[NotificationRow] = OFormat(reads, writes)
}

case class IDType(id: String)

object IDType {

  implicit val read: Reads[IDType] = (__ \ "$oid").read[String].map(dateTime => new IDType(dateTime))

  implicit val write: Writes[IDType] = (dateTime: IDType) => Json.obj("$oid" -> dateTime.id)

}
