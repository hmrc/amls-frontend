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

package services.notifications.v2m0

import models.notifications.ContactType
import models.notifications.ContactType._

class MessageDetails

object MessageDetails {

  def static(contactType: ContactType, url: String): String =
    contactType match {
      case ApplicationAutorejectionForFailureToPay =>
        s"""<p class="govuk-body">Your application to be supervised by HM Revenue and Customs (HMRC) under The Money Laundering, Terrorist Financing and Transfer of Funds (Information on the Payer) Regulations 2017 has failed.</p><p class="govuk-body">As you’ve not paid the full fees due, your application has automatically expired.</p><p class="govuk-body">You need to be registered with a <a href="https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register">supervisory body</a> if Money Laundering Regulations apply to your business. If you’re not supervised you may be subject to penalties and criminal charges.</p><p class="govuk-body">If you still need to be registered with HMRC you should submit a new application immediately. You can apply from your account <a href="$url">status page</a>.</p>"""
      case RegistrationVariationApproval           =>
        s"""<p class="govuk-body">The recent changes made to your details have been approved.</p><p class="govuk-body">You can find details of your registration on your <a href="$url">status page</a>.</p>"""
      case DeRegistrationEffectiveDateChange       =>
        s"""<p class="govuk-body">The date your anti-money laundering supervision ended has been changed.</p><p class="govuk-body">You can see the new effective date on your <a href="$url">status page</a>.</p>"""
      case _                                       => throw new Exception("An Unknown Exception has occurred, v2m0:static():MessageDetails")
    }

  def endDate(contactType: ContactType, endDate: String, url: String, referenceNumber: String): String =
    contactType match {
      case ApplicationApproval      =>
        s"""<p class="govuk-body">Your application to register has been approved. You’re now registered until $endDate.</p><p class="govuk-body">Your anti-money laundering registration number is: $referenceNumber.</p><p class="govuk-body">You can find details of your registration on your <a href="$url">status page</a>.</p>"""
      case RenewalApproval          =>
        s"""<p class="govuk-body">Your renewal has been approved. You’re now registered for supervision until $endDate.</p><p class="govuk-body">You can find details of your registration on your <a href="$url">status page</a>.</p>"""
      case AutoExpiryOfRegistration =>
        s"""<p class="govuk-body">Your registration to be supervised by HM Revenue and Customs (HMRC) under The Money Laundering, Terrorist Financing and Transfer of Funds (Information on the Payer) Regulations 2017 expired on $endDate.</p><p class="govuk-body">You need to be registered with a <a href="https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register">supervisory body</a> if Money Laundering Regulations apply to your business. If you’re not supervised you may be subject to penalties and criminal charges.</p><p class="govuk-body">If you still need to be registered with HMRC you should submit a new application immediately. You can apply from your <a href="$url">status page</a>.</p>"""
      case RenewalReminder          =>
        s"""<p class="govuk-body">You need to renew your registration before $endDate.</p><p class="govuk-body">You can renew from your <a href="$url">status page</a>.</p><p class="govuk-body">If you don’t renew and pay your fees before this date your registration will expire and you won’t be supervised by HM Revenue and Customs.</p>"""
      case _                        => throw new Exception("An Unknown Exception has occurred, v2m0:endDate():MessageDetails")
    }

  def reminder(contactType: ContactType, paymentAmount: String, referenceNumber: String): String =
    contactType match {
      case ReminderToPayForVariation     =>
        s"""<p class="govuk-body">You need to pay $paymentAmount for the recent changes made to your details.</p><p class="govuk-body">Your payment reference is: $referenceNumber.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"""
      case ReminderToPayForRenewal       =>
        s"""<p class="govuk-body">You need to pay $paymentAmount for your annual renewal.</p><p class="govuk-body">Your payment reference is: $referenceNumber.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"""
      case ReminderToPayForApplication   =>
        s"""<p class="govuk-body">You need to pay $paymentAmount for your application to register with HM Revenue and Customs.</p><p class="govuk-body">Your payment reference is: $referenceNumber.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"""
      case ReminderToPayForManualCharges =>
        s"""<p class="govuk-body">You need to pay $paymentAmount for the recent charge added to your account.</p><p class="govuk-body">Your payment reference is: $referenceNumber.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"""
      case _                             => throw new Exception("An Unknown Exception has occurred, v2m0:reminder():MessageDetails")
    }

}
