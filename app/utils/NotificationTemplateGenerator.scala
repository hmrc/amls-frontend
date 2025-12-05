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

package utils

import com.google.common.annotations.VisibleForTesting
import config.ApplicationConfig
import controllers.CommonPlayDependencies
import models.notifications.ContactType.{DeRegistrationEffectiveDateChange, MindedToReject, MindedToRevoke, NoLongerMindedToReject, NoLongerMindedToRevoke, RejectionReasons, RevocationReasons}
import models.notifications.{ContactType, NotificationDetails, NotificationParams, NotificationRow}
import models.status.{SubmissionDecisionRejected, SubmissionStatus}
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.mvc.Request
import play.twirl.api.{Html, Template5}
import uk.gov.hmrc.govukfrontend.views.Aliases.Table
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.HeadCell
import views.notifications._

import javax.inject.{Inject, Singleton}

@Singleton
class NotificationTemplateGenerator @Inject() (
  v1m0: V1M0,
  v2m0: V2M0,
  v3m0: V3M0,
  v4m0: V4M0,
  v5m0: V5M0,
  v6m0: V6M0,
  v7m0: V7M0,
  cpd: CommonPlayDependencies
) {

  type NotificationViewTemplate5 = Template5[NotificationParams, Request[_], Messages, Lang, ApplicationConfig, Html]

  implicit val lang: Lang                         = Lang.defaultLang
  implicit val msgsApi: MessagesApi               = cpd.messagesApi
  implicit val messagesProvider: MessagesProvider =
    MessagesImpl(lang, msgsApi)
  implicit val messages: Messages                 = messagesProvider.messages
  implicit val appConfig: ApplicationConfig       = cpd.amlsConfig

  @VisibleForTesting val templateMsgVersionsToVersionedViews: Map[String, VersionedView] =
    Map("v1m0" -> v1m0, "v2m0" -> v2m0, "v3m0" -> v3m0, "v4m0" -> v4m0, "v5m0" -> v5m0, "v6m0" -> v6m0, "v7m0" -> v7m0)

  def contactTypeToView(
    contactType: ContactType,
    reference: (String, String),
    businessName: String,
    details: NotificationDetails,
    status: SubmissionStatus,
    templateVersion: String
  )(implicit request: Request[_]): Html = {

    val msgText = details.messageText.getOrElse("")

    val (amlsRefNo, safeId) = reference

    contactType match {
      case MindedToRevoke =>
        render(
          "minded_to_revoke",
          NotificationParams(msgContent = msgText, amlsRefNo = Some(amlsRefNo), businessName = Some(businessName)),
          templateVersion
        )

      case MindedToReject =>
        render(
          "minded_to_reject",
          NotificationParams(msgContent = msgText, safeId = Some(safeId), businessName = Some(businessName)),
          templateVersion
        )

      case RejectionReasons =>
        render(
          "rejection_reasons",
          NotificationParams("", msgText, Some(safeId), None, Some(businessName), Some(details.dateReceived)),
          templateVersion
        )

      case RevocationReasons =>
        render(
          "revocation_reasons",
          NotificationParams("", msgText, None, Some(amlsRefNo), Some(businessName), Some(details.dateReceived)),
          templateVersion
        )

      case NoLongerMindedToReject =>
        render(
          "no_longer_minded_to_reject",
          NotificationParams(msgContent = msgText, safeId = Some(safeId)),
          templateVersion
        )

      case NoLongerMindedToRevoke =>
        render(
          "no_longer_minded_to_revoke",
          NotificationParams(msgContent = msgText, amlsRefNo = Some(amlsRefNo)),
          templateVersion
        )

      case _ =>
        (status, contactType) match {
          case (SubmissionDecisionRejected, _) | (_, DeRegistrationEffectiveDateChange) =>
            render(
              "message_details",
              NotificationParams(details.subject(templateVersion), msgText, Some(safeId), None, None, None),
              templateVersion
            )
          case _                                                                        =>
            render(
              "message_details",
              NotificationParams(msgTitle = details.subject(templateVersion), msgContent = msgText),
              templateVersion
            )
        }
    }
  }

  def toTable(rowsWithIndex: Seq[(NotificationRow, Int)], id: String, isPrevRegistration: Boolean): Table = {
    val tableHeader = Seq(
      headCell("subject", isPrevRegistration),
      headCell("category", isPrevRegistration),
      headCell("date", isPrevRegistration)
    )
    Table(
      rows = rowsWithIndex.map { case (row, index) => row.asTableRows(id, index) },
      head = if (rowsWithIndex.nonEmpty) Some(tableHeader) else None
    )
  }

  private def headCell(id: String, isPreviousRegistration: Boolean): HeadCell =
    HeadCell(
      content = Text(messages(s"notification.$id")),
      attributes =
        if (isPreviousRegistration) Map("id" -> s"$id-previousMessages") else Map("id" -> s"$id-currentMessages")
    )

  private def render(templateName: String, notificationParams: NotificationParams, templateVersion: String)(implicit
    request: Request[_]
  ): Html =
    try
      templateMsgVersionsToVersionedViews(templateVersion)
        .viewFromTemplateFilename(templateName)
        .asInstanceOf[NotificationViewTemplate5]
        .render(notificationParams, request, messages, lang, appConfig)
    catch {
      case _: NoSuchElementException => throw new RuntimeException(s"Notification version $templateVersion not found")
    }
}
