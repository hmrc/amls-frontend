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

package services.notifications.v1m0

import models.notifications.ContactType

object MessageDetails {
  def static(contactType: ContactType, url: String): String = {
//    s"notification.static.text.$contactType"
    ""
  }

  def endDate(contactType: ContactType, endDate: String, url: String, referenceNumber: String): String = {
    //            s"notification.message.with.end.date.$contactType"
    ""
  }

  def reminder(contactType: ContactType, paymentAmount: String, referenceNumber: String): String = {
    //            s"notification.reminder.to.pay.$contactType"
    ""
  }

}
