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
import models.notifications.ContactType.{ApplicationAutorejectionForFailureToPay, DeRegistrationEffectiveDateChange, RegistrationVariationApproval}

object MessageDetails {
  def static(contactType: ContactType, url: String): String = {
    contactType match {
      case ApplicationAutorejectionForFailureToPay => s"<p>Your application to be supervised by HM Revenue and Customs (HMRC) under The Money Laundering, Terrorist Financing and Transfer of Funds (Information on the Payer) Regulations 2017 has failed.</p><p>As you’ve not paid the full fees due, your application has automatically expired.</p><p>You need to be registered with a <a href=${ "\"" }https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register${ "\"" }>supervisory body</a> if Money Laundering Regulations apply to your business. If you’re not supervised you may be subject to penalties and criminal charges.</p><p>If you still need to be registered with HMRC you should submit a new application immediately. You can apply from your account <a href=${ "\"" + url + "\"" }>status page</a>.</p>"
      case RegistrationVariationApproval =>  s"<p>The recent changes made to your details have been approved.</p><p>You can find details of your registration on your <a href=${ "\"" + url + "\"" }>status page</a>.</p>"
      case DeRegistrationEffectiveDateChange => s"<p>The date your anti-money laundering supervision ended has been changed.</p><p>You can see the new effective date on your <a href=${ "\"" + url + "\"" }>status page</a>.</p>"
    }
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
