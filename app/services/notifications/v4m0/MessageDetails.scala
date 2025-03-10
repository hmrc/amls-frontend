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

package services.notifications.v4m0

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
      case _                                       => throw new Exception("An Unknown Exception has occurred, v4m0:static():MessageDetails")
    }

  def endDate(contactType: ContactType, endDate: String, url: String, referenceNumber: String): String =
    contactType match {
      case ApplicationApproval      =>
        s"""<p class="govuk-body">Your application to register has been approved. You’re now registered until $endDate.</p><p class="govuk-body">Your anti-money laundering registration number is: $referenceNumber.</p><p class="govuk-body">You can find details of your registration on your <a href="$url">status page</a>.</p>"""
      case RenewalApproval          =>
        s"""<p class="govuk-body">Your annual fee has been paid.</p><p class="govuk-body">To continue to be registered with HMRC you will need to repeat this process next year. Your next annual fee will be due before $endDate.</p><p class="govuk-body">HMRC will contact you again to remind you to complete the renewal questions and pay your annual fee.</p><p class="govuk-body">You can find your registration details on your status page.</p>"""
      case AutoExpiryOfRegistration =>
        s"""<p class="govuk-body">Your registration for supervision under the Money Laundering, Terrorist Finance and Transfer of Funds (Information on the Payer) Regulations 2017 was cancelled on $endDate. This is because you have not paid your annual fee.</p><p class="govuk-body">Your status page will show your supervision has expired. As a result, you are no longer registered for supervision with HMRC.</p><p class="govuk-body">Your registration was cancelled under Regulation 60(3)(a) with reference to Regulation 59(1)(c)(ii): failure to pay a charge imposed by HMRC under Part 11, Regulation 102(1)(c).</p><p class="govuk-body">You need to be <a href="https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register#businesses-already-supervised-for-money-laundering-purposes">registered with a supervisory body</a> if the Money Laundering Regulations apply to your business. If you are not registered for supervision, you may be subject to civil sanctions or criminal proceedings if you continue to trade in activities covered by the Money Laundering, Terrorist Finance and Transfer of Funds (Information on the Payer) Regulations 2017.</p><p class="govuk-body"><h2 class="govuk-heading-m">If you disagree with this decision</h2><p class="govuk-body">You have 30 days to:</p><ul class="govuk-list govuk-list--bullet"><li>ask for a review</li><li>appeal directly to an independent tribunal, if you do not want a review</li></ul></p><p class="govuk-body">HMRC reviews are carried out by an independent team. You will be notified of the outcome of the review within 45 days unless another time is agreed. If you are not satisfied with the conclusion of the review, you still have a right of appeal to a tribunal. These are administered by the Tribunal Service within 30 days of the notification of the outcome of the review.</p><p class="govuk-body">Find out how to request a review, and information about what you can do <a href="https://www.gov.uk/guidance/money-laundering-regulations-appeals-and-penalties#if-you-disagree-with-an-hmrc-decision">if you disagree with an HMRC decision</a>.</p><p class="govuk-body">Find further information about <a href="https://www.gov.uk/tax-tribunal">making an appeal to the tribunal</a>.</p>"""
      case RenewalReminder          =>
        s"""<p class="govuk-body">It is time to renew your annual supervision registration with HMRC. You need to complete the renewal questions and pay your annual fee by $endDate.</p><p class="govuk-body">Start this process from your status page by selecting ‘start your renewal’.</p><p class="govuk-body">You need to answer the questions and pay your annual fee in full. The annual fee is imposed under Regulation 102(1)(c) of the Money Laundering, Terrorist Finance and Transfer of Funds (Information on the Payer) Regulations 2017.</p><p class="govuk-body">If you do not pay your fees before this date, your registration will be cancelled.</p><p class="govuk-body">If we cancel your registration, you will no longer be supervised by HMRC.  You need to be <a href="https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register#businesses-already-supervised-for-money-laundering-purposes">registered with a supervisory body</a> if the Money Laundering Regulations apply to your business. You may be subject to civil sanctions or criminal proceedings if you continue to trade in activities covered by the Money Laundering, Terrorist Finance and Transfer of Funds (Information on the Payer) Regulations 2017.</p>"""
      case _                        => throw new Exception("An Unknown Exception has occurred, v4m0:endDate():MessageDetails")
    }

  def reminder(contactType: ContactType, paymentAmount: String, referenceNumber: String): String =
    contactType match {
      case ReminderToPayForVariation     =>
        s"""<p class="govuk-body">You need to pay $paymentAmount for the recent changes made to your details.</p><p class="govuk-body">Your payment reference is: $referenceNumber.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"""
      case ReminderToPayForRenewal       =>
        s"""<p class="govuk-body">You need to pay $paymentAmount for your annual fee to continue your registration with HMRC.</p><p class="govuk-body">Your payment reference is: $referenceNumber.</p><p class="govuk-body">Find out <a href="/anti-money-laundering/how-to-pay">how to pay your fees</a>.</p><p class="govuk-body">If you have already paid, please ignore this message.</p><p class="govuk-body">If you do not pay this fee when it is due, HMRC will cancel your registration.</p><p class="govuk-body">If your registration is cancelled, you are no longer registered for supervision with HMRC. You need to be registered with an <a href="https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register#businesses-already-supervised-for-money-laundering-purposes">appropriate supervisory body</a> if the Money Laundering Regulations apply to your business. You may be subject to civil sanctions or criminal proceedings if you continue to trade in activities covered by the Money Laundering, Terrorist Finance and Transfer of Funds (Information on the Payer) Regulations 2017.</p>"""
      case ReminderToPayForApplication   =>
        s"""<p class="govuk-body">You need to pay $paymentAmount for your application to register with HM Revenue and Customs.</p><p class="govuk-body">Your payment reference is: $referenceNumber.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"""
      case ReminderToPayForManualCharges =>
        s"""<p class="govuk-body">You need to pay $paymentAmount for the recent charge added to your account.</p><p class="govuk-body">Your payment reference is: $referenceNumber.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"""
      case _                             => throw new Exception("An Unknown Exception has occurred, v4m0:reminder():MessageDetails")
    }

}
