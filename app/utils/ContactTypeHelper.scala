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

import models.notifications.ContactType.{ApplicationAutorejectionForFailureToPay, DeRegistrationEffectiveDateChange, NoSubject, RegistrationVariationApproval}
import models.notifications.{ContactType, Status}
import models.notifications.StatusType.DeRegistered

object ContactTypeHelper {

  def getContactType(status: Option[Status], contactType: Option[ContactType], variation: Boolean): ContactType = {

    val statusReason = for {
      st     <- status
      reason <- st.statusReason
    } yield reason

    contactType.getOrElse(
      (status, statusReason, variation) match {
        case (Some(Status(Some(DeRegistered), _)), _, _) => DeRegistrationEffectiveDateChange
        case (_, Some(_), _)                             => ApplicationAutorejectionForFailureToPay
        case (_, _, true)                                => RegistrationVariationApproval
        case _                                           => NoSubject
      }
    )
  }
}
