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

package controllers.bankdetails

import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.bankdetails.BankDetails
import models.status.{NotCompleted, SubmissionReady, SubmissionStatus}
import play.api.mvc.MessagesControllerComponents
import utils.RepeatingSection

abstract class BankDetailsController(ds: CommonPlayDependencies, val cc: MessagesControllerComponents)
    extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  implicit class BankDetailsSyntax(model: BankDetails) {
    def canEdit(status: SubmissionStatus): Boolean = status match {
      case SubmissionReady | NotCompleted               => true
      case _ if !model.hasAccepted || !model.isComplete => true
      case _                                            => false
    }
  }
}
