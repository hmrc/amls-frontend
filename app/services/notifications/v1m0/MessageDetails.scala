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
import models.notifications.ContactType._

object MessageDetails {
  def static(contactType: ContactType, url: String): String = {
    contactType match {
      case ApplicationAutorejectionForFailureToPay => s"<p>Your application to be supervised by HM Revenue and Customs (HMRC) under The Money Laundering, Terrorist Financing and Transfer of Funds (Information on the Payer) Regulations 2017 has failed.</p><p>As you’ve not paid the full fees due, your application has automatically expired.</p><p>You need to be registered with a <a href=${ "\"" }https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register${ "\"" }>supervisory body</a> if Money Laundering Regulations apply to your business. If you’re not supervised you may be subject to penalties and criminal charges.</p><p>If you still need to be registered with HMRC you should submit a new application immediately. You can apply from your account <a href=${ "\"" + url + "\"" }>status page</a>.</p>"
      case RegistrationVariationApproval =>  s"<p>The recent changes made to your details have been approved.</p><p>You can find details of your registration on your <a href=${ "\"" + url + "\"" }>status page</a>.</p>"
      case DeRegistrationEffectiveDateChange => s"<p>The date your anti-money laundering supervision ended has been changed.</p><p>You can see the new effective date on your <a href=${ "\"" + url + "\"" }>status page</a>.</p>"
    }
  }

  def endDate(contactType: ContactType, endDate: String, url: String, referenceNumber: String): String = {
    contactType match {
      case ApplicationApproval => s"<p>Your application to register has been approved. You’re now registered until ${ endDate }.</p><p>Your anti-money laundering registration number is: ${ referenceNumber }.</p><p>You can find details of your registration on your <a href=${ "\"" + url + "\"" }>status page</a>.</p>"
      case RenewalApproval => s"<p>Your renewal has been approved. You’re now registered for supervision until ${ endDate }.</p><p>You can find details of your registration on your <a href=${ "\"" + url + "\"" }>status page</a>.</p>"
      case AutoExpiryOfRegistration => s"<p>Your registration to be supervised by HM Revenue and Customs (HMRC) under The Money Laundering, Terrorist Financing and Transfer of Funds (Information on the Payer) Regulations 2017 expired on ${ endDate }.</p><p>You need to be registered with a <a href=${ "\"" }https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register${ "\"" }>supervisory body</a> if Money Laundering Regulations apply to your business. If you’re not supervised you may be subject to penalties and criminal charges.</p><p>If you still need to be registered with HMRC you should submit a new application immediately. You can apply from your <a href=${ "\"" + url + "\"" }>status page</a>.</p>"
      case RenewalReminder => s"<p>You need to renew your registration before ${ endDate }.</p><p>You can renew from your <a href=${ "\"" + url + "\"" }>status page</a>.</p><p>If you don’t renew and pay your fees before this date your registration will expire and you won’t be supervised by HM Revenue and Customs.</p>"
    }
  }

  def reminder(contactType: ContactType, paymentAmount: String, referenceNumber: String): String = {
    contactType match {
      case ReminderToPayForVariation => s"<p>You need to pay ${ paymentAmount } for the recent changes made to your details.</p><p>Your payment reference is: ${ referenceNumber }.</p><p>Find details of how to pay on your online account home page.</p><p>It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"
      case ReminderToPayForRenewal => s"<p>You need to pay ${ paymentAmount } for your annual renewal.</p><p>Your payment reference is: ${ referenceNumber }.</p><p>Find details of how to pay on your online account home page.</p><p>It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"
      case ReminderToPayForApplication => s"<p>You need to pay ${ paymentAmount } for your application to register with HM Revenue and Customs.</p><p>Your payment reference is: ${ referenceNumber }.</p><p>Find details of how to pay on your online account home page.</p><p>It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"
      case ReminderToPayForManualCharges => s"<p>You need to pay ${ paymentAmount } for the recent charge added to your account.</p><p>Your payment reference is: ${ referenceNumber }.</p><p>Find details of how to pay on your online account home page.</p><p>It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"
    }
  }

}
