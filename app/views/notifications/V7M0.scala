/*
 * Copyright 2025 HM Revenue & Customs
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

package views.notifications

import views.html.notifications.v7m0._

import javax.inject.Inject

class V7M0 @Inject() (
  val messageDetailsView: MessageDetailsView,
  val mindedToRejectView: MindedToRejectView,
  val mindedToRevokeView: MindedToRevokeView,
  val noLongerMindedToRejectView: NoLongerMindedToRejectView,
  val noLongerMindedToRevokeView: NoLongerMindedToRevokeView,
  val rejectionReasonsView: RejectionReasonsView,
  val revocationReasonsView: RevocationReasonsView
) extends VersionedView {

  override def viewFromTemplateFilename(templateName: String): NotificationViewScalaTemplate5 =
    templateName match {
      case "message_details"            => messageDetailsView
      case "minded_to_reject"           => mindedToRejectView
      case "minded_to_revoke"           => mindedToRevokeView
      case "no_longer_minded_to_reject" => noLongerMindedToRejectView
      case "no_longer_minded_to_revoke" => noLongerMindedToRevokeView
      case "rejection_reasons"          => rejectionReasonsView
      case "revocation_reasons"         => revocationReasonsView
      case _                            => throw new RuntimeException(s"Message template $templateName not found")
    }
}
