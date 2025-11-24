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

package services.notifications.v7m0

import models.notifications.ContactType
import models.notifications.ContactType._

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MessageDetails

object MessageDetails {
  val f: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

  def formatDate(date: String): String = LocalDate.parse(date).format(f)

  def static(contactType: ContactType, url: String): String =
    contactType match {
      case ApplicationAutorejectionForFailureToPay =>
        s"""<p class="govuk-body">Your application to be supervised by HM Revenue and Customs (HMRC) under The Money Laundering, Terrorist Financing and Transfer of Funds (Information on the Payer) Regulations 2017 has failed.</p><p class="govuk-body">As you’ve not paid the full fees due, your application has automatically expired.</p><p class="govuk-body">You need to be registered with a <a href="https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register">supervisory body</a> if Money Laundering Regulations apply to your business. If you’re not supervised you may be subject to penalties and criminal charges.</p><p class="govuk-body">If you still need to be registered with HMRC you should submit a new application immediately. You can apply from your account <a href="$url">status page</a>.</p>"""
      case RegistrationVariationApproval           =>
        s"""<p class="govuk-body">The recent changes made to your details have been approved.</p><p class="govuk-body">You can find details of your registration on your <a href="$url">status page</a>.</p>"""
      case DeRegistrationEffectiveDateChange       =>
        s"""<p class="govuk-body">The date your anti-money laundering supervision ended has been changed.</p><p class="govuk-body">You can see the new effective date on your <a href="$url">status page</a>.</p>"""
      case _                                       => throw new Exception("An Unknown Exception has occurred, v7m0:static():MessageDetails")
    }

  def endDate(contactType: ContactType, endDate: String, url: String, referenceNumber: String): String =
    contactType match {
      case ApplicationApproval      =>
        s"""<p class="govuk-body">Your application to register has been approved. You’re now registered until $endDate.</p><p class="govuk-body">Your anti-money laundering registration number is: $referenceNumber.</p><p class="govuk-body">You can find details of your registration on your <a href="$url">status page</a>.</p>"""
      case RenewalApproval          =>
        s"""<p class="govuk-body">Your annual fee has been paid.</p><p class="govuk-body">To continue to be registered with HMRC you will need to repeat this process next year. Your next annual fee will be due before $endDate.</p><p class="govuk-body">HMRC will contact you again to remind you to complete the renewal questions and pay your annual fee.</p><p class="govuk-body">You can find your registration details on your status page.</p>"""
      case AutoExpiryOfRegistration => registrationAutoExpiry
      case RenewalReminder          => reminderRenewal(endDate)
      case NewRenewalReminder       => newRenewalReminderMsg(endDate)
      case _                        => throw new Exception("An Unknown Exception has occurred, v7m0:endDate():MessageDetails")
    }

  def reminder(contactType: ContactType, paymentAmount: String, referenceNumber: String): String =
    contactType match {
      case ReminderToPayForVariation     =>
        s"""<p class="govuk-body">You need to pay $paymentAmount for the recent changes made to your details.</p><p class="govuk-body">Your payment reference is: $referenceNumber.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"""
      case ReminderToPayForRenewal       => reminderPayRenewal(referenceNumber)
      case ReminderToPayForApplication   =>
        s"""<p class="govuk-body">You need to pay $paymentAmount for your application to register with HM Revenue and Customs.</p><p class="govuk-body">Your payment reference is: $referenceNumber.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"""
      case ReminderToPayForManualCharges =>
        s"""<p class="govuk-body">You need to pay $paymentAmount for the recent charge added to your account.</p><p class="govuk-body">Your payment reference is: $referenceNumber.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"""
      case _                             => throw new Exception("An Unknown Exception has occurred, v7m0:reminder():MessageDetails")
    }

  val amlsRegistrationFeesGuidanceUrl = "https://www.gov.uk/guidance/money-laundering-regulations-registration-fees"
  val emailUrl                        = "mailto:MLRCIT@hmrc.gov.uk"

  def newRenewalReminderMsg(endDate: String): String =
    s"""
       |<p class="govuk-body">This is a reminder that you need to submit your annual declaration and pay your annual supervision fee. Please note that this is
       |a <strong>two stage process</strong>, and both stages <strong>must</strong> be competed to ensure we can process your fee.</p>
       |
       |<p class="govuk-body"><h3 class="govuk-heading-s">Stage 1 - Submit your declaration all your details and information are correct and up to date.
       |You must press the submit button to make that declaration which completes this stage.</h3></p>
       |
       |<p class="govuk-body">You start this stage from your status page on your anti-money laundering supervision account. You must check that all your details
       |and information are up to date and make any necessary changes to the details information you have previously provided.</p>
       |
       |<p class="govuk-body"><strong>Please note -</strong> Even if your previous details and information have not changed, you must press the submit button to complete stage 1
       |in order for us to be able to process your fee payment.</p>
       |
       |<p class="govuk-body"><h3 class="govuk-heading-s">Stage 2 - Pay your fee<h3><p>
       |<p class="govuk-body">Once you have completed stage 1, the system will inform you of your new annual fee and generate a payment reference number which
       |you must quote when you pay your fee.</p>
       |
       |<p class="govuk-body">HMRCs money laundering registration policy, and details of how to pay your fee can be found on
       |<a href="$amlsRegistrationFeesGuidanceUrl">Fees you’ll pay for money laundering supervision - GOV.UK (www.gov.uk)</a></p>
       |
       |<p class="govuk-body">You must pay your annual fee in full by ${formatDate(endDate)}.</p>
       |
       |<p class="govuk-body">If you are experiencing difficulties in carrying out these actions, please contact <a href="$emailUrl">MLRCIT@hmrc.gov.uk</a>
       |immediately.</p>
       |
       |<p class="govuk-body"><h3 class="govuk-heading-s">If you do not submit your declaration and pay your correct fee by the above date, your business registration will be
       |cancelled.</h3></p>
       |
       |<p class="govuk-body"><h3 class="govuk-heading-s">If your registration is cancelled and you still wish for your business to continue to be registered with HMRC, you
       |will need to submit a new registration application and pay the correct fees, including the new application charge
       |cancelled.</h3></p>
       |
       |<p class="govuk-body"><strong>Please note</strong> If you have paid the annual supervision fee before today but failed to submit your
       |annual declaration, you will not have received a payment reference number and our system may not recognise that payment has been made. This may lead
       |to your fee being shown as unpaid and your registration being cancelled. If this applies to you, you must submit a new application within 30 days.
       |Even if you have submitted your annual declaration, if you pay your fee without quoting your payment reference number this may lead to your fee being
       |shown as unpaid and your registration being cancelled.</p>
       |
       |<p class="govuk-body">If your registration is cancelled and you continue to undertake any activity covered by the Regulations, you and/or your business
       |may be subject to civil sanctions, including a fine, or criminal proceedings.</p>
       |
       |<p class="govuk-body">If you have taken the ALL steps outlined above, please ignore this message.</p>
       |""".stripMargin

  def reminderPayRenewal(referenceNumber: String): String =
    s"""
       |<p class="govuk-body">We note you have submitted the annual declaration but have failed to pay your annual supervision fee. You need to pay this fee to
       |continue your registration with HMRC. The amount you must pay was calculated when you submitted your declaration.
       |
       |<p class="govuk-body">HMRCs money laundering registration policy, and details of how to pay your fee can be found on
       |<a href="$amlsRegistrationFeesGuidanceUrl">Fees you’ll pay for money laundering supervision - GOV.UK (www.gov.uk)</a></p>
       |
       |<p class="govuk-body">When you pay your fee you must quote your payment reference number which is: $referenceNumber. <strong>Please note</strong> that
       |if you pay your fee without quoting your payment reference number we may not be able to match the payment to your business. This may lead to your fee
       |being shown as unpaid and your registration being cancelled.</p>
       |
       |<p class="govuk-body">If you are experiencing difficulties in carrying out these actions, please contact <a href="$emailUrl">MLRCIT@hmrc.gov.uk</a>.
       |</p>
       |
       |<p class="govuk-body">If you do not pay your fees within 28 days from the date of this message, HMRC will cancel your business’s registration.
       |If your registration is cancelled and you still wish for your business to continue to be registered with HMRC, you will need to submit a new
       |registration application and pay the correct fees, including the application charge.</p>
       |
       |<p class="govuk-body">If your registration is cancelled and you continue to undertake any activity covered by the Regulations, you and/or your
       |business may be subject to civil sanctions, such as a fine, or criminal proceedings.</p>
       |
       |<p class="govuk-body">If you have already paid the correct fee quoting your payment reference number, please ignore this message.</p>
       |""".stripMargin

  val guidanceRegisterRenewUrl    =
    "http://www.gov.uk/guidance/register-or-renew-your-money-laundering-supervision-with-hmrc"
  val disagreeAppealsPenaltiesUrl =
    "https://www.gov.uk/guidance/money-laundering-regulations-appeals-and-penalties#if-you-disagree-with-an-hmrc-decision"
  val guidanceRegistrationFeesUrl = "https://www.gov.uk/guidance/money-laundering-regulations-registration-fees"

  val registrationAutoExpiry: String =
    s"""
       |<p class="govuk-body"><h3 class="govuk-heading-s">You/your business’s registration with HMRC has been cancelled for failing to pay the annual supervision fee imposed under
       |Regulation 102 of the Money Laundering, Terrorist Financing and Transfer of Funds (Information on the Payer) Regulations 2017. This cancellation
       |decision is made under regulation 60(3)(a) by virtue of regulation 59(1)(c)(ii).</h3></p>
       |
       |<p class="govuk-body">Our system shows that you have failed to complete the required 2 stage process as set out in previous reminders.</p>
       |
       |<p class="govuk-body"><strong>Stage 1 – You are</strong> required to submit your declaration confirming all your details and information are correct and
       |up to date.</p>
       |
       |<p class="govuk-body"><strong>Stage 2 – You are</strong> required to pay the correct fee.</p>
       |
       |<p class="govuk-body">The cancellation will come into effect 14 days from the date of this notice to allow you to conclude your relevant business</p>
       |
       |<p class="govuk-body"><h3 class="govuk-heading-s">If you continue to undertake any activity covered by the Regulations after your registration has been
       |cancelled, you and/or your business may be subject to civil sanctions, such as a fine, or criminal proceedings</h3></p>
       |
       |<p class="govuk-body">You may wish for your business to continue to be registered with HMRC. To enable you to do so you will need to submit a new
       |registration application and pay the correct fees, including the application charge. This will be the quickest and most efficient way to ensure you are
       |able to lawfully carry out your supervised business activity. You can find guidance on how to register here
       |<a href="$guidanceRegisterRenewUrl">Register-or-renew-your-money-laundering-supervision-with-hmrc</a>–GOV.UK (www.gov.uk).</p>
       |
       |<p class="govuk-body">If you are experiencing any issues applying for registration, please contact MLRCIT@hmrc.gov.uk.</p>
       |
       |<p class="govuk-body"><h3 class="govuk-heading-s">If you/ your business disagree with this decision</hr></p>
       |
       |<p class="govuk-body">You also have the right to request a HMRC internal review of this decision or to appeal to an independent tribunal.</p>
       |
       |<p class="govuk-body">You can also find information about what you can do if you disagree with an HMRC decision here -
       |<a href="$disagreeAppealsPenaltiesUrl">Money laundering supervision appeals and penalties - GOV.UK (www.gov.uk)</a>, including how to request a review
       |of our decision or make appeal to the tribunal.</p>
       |
       |<p class="govuk-body">If you would like further clarification on any of the issues raised above, please contact MLRCIT@hmrc.gov.uk.</p>
       |""".stripMargin

  def reminderRenewal(endDate: String): String =
    s"""
       |<p class="govuk-body">You/your business are currently registered with HMRC for anti-money laundering supervision. It is time to confirm your
       |registration details and information are up to date and pay your annual supervision fee. Please note that this is a <strong>two stage process</strong>
       |and both stages <strong>must</strong> be competed to ensure we can process your fee.</p>
       |
       |<p class="govuk-body"><h3 class="govuk-heading-s">Stage 1  - Submit your declaration confirming all your details and information are correct and up to
       |date. You must press the submit button to make that declaration which completes this stage.</h3></p>
       |
       |<p class="govuk-body">You start this stage from your status page on your anti-money laundering supervision account. You must check that all your
       |details and information are up to date and make any necessary changes to the details and information you have previously provided.</p>
       |
       |<p class="govuk-body"><strong>Please note -</strong> Even if your previous details have not changed, you must press the submit button to complete
       |stage 1 in order for us to be able to process your fee payment.</p>
       |
       |<p class="govuk-body"><h3 class="govuk-heading-s">Stage 2 – Pay your fee</h3></p>
       |
       |<p class="govuk-body">Once you have completed stage 1, the system will inform you of your new annual fee and generate a payment reference number which
       |you must quote when you pay your fee.</p>
       |
       |<p class="govuk-body">HMRCs money laundering registration policy, and details of how to pay your fee can be found on
       |<a href="$guidanceRegistrationFeesUrl">Fees you’ll pay for money laundering supervision - GOV.UK (www.gov.uk)</a></p>
       |
       |<p class="govuk-body">You must pay your annual fee in full by ${formatDate(endDate)}. Failing to submit your declaration and pay the correct fee in
       |full by this date may lead to HMRC cancelling your registration.</p>
       |
       |<p class="govuk-body"><strong>Please note</strong>, if you pay your fee without quoting your payment reference number we may not be able to match the
       |payment to your business. This may lead to your fee being shown as unpaid and your registration being cancelled. If your registration is cancelled
       |and you still wish for your business to continue to be registered with HMRC, you will need to submit a new registration application and pay the
       |correct fees, including the application charge.</p>
       |
       |<p class="govuk-body">If you are experiencing difficulties in carrying out these actions, please contact <a href="$emailUrl">MLRCIT@hmrc.gov.uk</a> immediately.</p>
       |
       |<p class="govuk-body">If you have taken ALL the steps outlined above, please ignore this message.</p>
       |""".stripMargin
}
