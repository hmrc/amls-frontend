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

  def formatDate(date: String): String = {
    val outputFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
    val parsedDate = try {
      val inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
      LocalDate.parse(date, inputFormatter)
    } catch {
      case _: Exception => LocalDate.parse(date)
    }

    parsedDate.format(outputFormatter)
  }

  def static(contactType: ContactType, url: String): String =
    contactType match {
      case ApplicationAutorejectionForFailureToPay =>
        s"""<p class="govuk-body">Your application to be supervised by HM Revenue and Customs (HMRC) under The Money Laundering, Terrorist Financing and Transfer of Funds (Information on the Payer) Regulations 2017 has failed.</p><p class="govuk-body">As you've not paid the full fees due, your application has automatically expired.</p><p class="govuk-body">You need to be registered with a <a href="https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register">supervisory body</a> if Money Laundering Regulations apply to your business. If you're not supervised you may be subject to penalties and criminal charges.</p><p class="govuk-body">If you still need to be registered with HMRC you should submit a new application immediately. You can apply from your account <a href="$url">status page</a>.</p>"""
      case RegistrationVariationApproval           =>
        s"""<p class="govuk-body">The recent changes made to your details have been approved.</p><p class="govuk-body">You can find details of your registration on your <a href="$url">status page</a>.</p>"""
      case DeRegistrationEffectiveDateChange       =>
        s"""<p class="govuk-body">The date your anti-money laundering supervision ended has been changed.</p><p class="govuk-body">You can see the new effective date on your <a href="$url">status page</a>.</p>"""
      case _                                       => throw new Exception("An Unknown Exception has occurred, v7m0:static():MessageDetails")
    }

  def endDate(contactType: ContactType, endDate: String, url: String, referenceNumber: String): String =
    contactType match {
      case ApplicationApproval      =>
        s"""<p class="govuk-body">Your application to register has been approved. You're now registered until $endDate.</p><p class="govuk-body">Your anti-money laundering registration number is: $referenceNumber.</p><p class="govuk-body">You can find details of your registration on your <a href="$url">status page</a>.</p>"""
      case RenewalApproval          =>
        s"""<p class="govuk-body">Your annual fee has been paid.</p><p class="govuk-body">To continue to be registered with HMRC you will need to repeat this process next year. Your next annual fee will be due before $endDate.</p><p class="govuk-body">HMRC will contact you again to remind you to complete the renewal questions and pay your annual fee.</p><p class="govuk-body">You can find your registration details on your status page.</p>"""
      case AutoExpiryOfRegistration => registrationAutoExpiry(endDate)
      case RenewalReminder          => reminderRenewal(endDate)
      case NewRenewalReminder       => newRenewalReminderMsg(endDate)
      case _                        => throw new Exception("An Unknown Exception has occurred, v7m0:endDate():MessageDetails")
    }

  def reminder(contactType: ContactType, paymentAmount: String, referenceNumber: String,dueDate:String): String =
    contactType match {
      case ReminderToPayForVariation     =>
        s"""<p class="govuk-body">You need to pay $paymentAmount for the recent changes made to your details.</p><p class="govuk-body">Your payment reference is: $referenceNumber.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you've already paid you can ignore this message.</p>"""
      case ReminderToPayForRenewal       => reminderPayRenewal(paymentAmount,referenceNumber,dueDate)
      case ReminderToPayForApplication   =>
        s"""<p class="govuk-body">You need to pay $paymentAmount for your application to register with HM Revenue and Customs.</p><p class="govuk-body">Your payment reference is: $referenceNumber.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you've already paid you can ignore this message.</p>"""
      case ReminderToPayForManualCharges =>
        s"""<p class="govuk-body">You need to pay $paymentAmount for the recent charge added to your account.</p><p class="govuk-body">Your payment reference is: $referenceNumber.</p><p class="govuk-body">Find details of how to pay on your status page.</p><p class="govuk-body">It can take time for some payments to clear, so if you've already paid you can ignore this message.</p>"""
      case _                             => throw new Exception("An Unknown Exception has occurred, v7m0:reminder():MessageDetails")
    }

  val amlsRegistrationFeesGuidanceUrl = "https://www.gov.uk/guidance/money-laundering-regulations-registration-fees"
  val emailUrl                        = "mailto:MLRCIT@hmrc.gov.uk"

  def newRenewalReminderMsg(endDate: String): String =
    s"""
       |<p class="govuk-body">This is a reminder that you need to confirm your details and pay your annual fee.</p>
       |<p class="govuk-body">If you do not do this by <strong>${formatDate(endDate)}</strong>, HMRC will cancel your registration.</p>
       |<p class="govuk-body">This is a two-stage process. You must complete both stages to stay registered.</p>
       |
       |<h2 class="govuk-heading-m">Stage 1: Confirm your details</h2>
       |<p class="govuk-body">Go to <a href="${controllers.routes.StatusController.get().url}" class="govuk-link">Your registration</a> and select 'Extend your supervision'.</p>
       |<p class="govuk-body">Confirm all your registration details are correct and up to date. If anything has changed, you can make an update.</p>
       |<p class="govuk-body">Even if none of your details have changed, you must still complete this step.</p>
       |<p class="govuk-body">When you have confirmed your details, you will be asked to make a declaration. You must select 'Accept and submit' to complete stage 1.</p>
       |
       |<h2 class="govuk-heading-m">Stage 2: Pay your fee</h2>
       |<p class="govuk-body">When you have completed stage 1, the system will display your annual fee and payment reference.</p>
       |<p class="govuk-body">You must quote your payment reference when you pay your fee. If you do not, your fee may show as unpaid and your registration may be cancelled.</p>
       |
       |<h2 class="govuk-heading-m">If you have paid your fee, but have not submitted your annual declaration</h2>
       |<p class="govuk-body">Our system may not recognise that a payment has been made and your registration may be cancelled.</p>
       |<p class="govuk-body">This is because until you submit your annual declaration, you do not have the correct payment reference.</p>
       |<h2 class="govuk-heading-m">If your registration is cancelled</h2>
       |<p class="govuk-body">You will need to submit a new registration application and pay the correct fees, including the application charge.</p>
       |<p class="govuk-body">If your registration is cancelled, you must not undertake any activity covered by the Money Laundering Regulations. Otherwise, you and/or your business may be subject to civil sanctions, such as a fine, or criminal proceedings.</p>
       |
       |<h2 class="govuk-heading-m">More information</h2>
       |<p class="govuk-body">For details of HMRC's registration policy and how to pay your fee, visit <a href="https://www.gov.uk/guidance/money-laundering-regulations-registration-fees" class="govuk-link">Fees you'll pay for money laundering supervision</a>.</p>
       |<p class="govuk-body">If you need help, contact us at <a href="mailto:MLRCIT@hmrc.gov.uk" class="govuk-link">MLRCIT@hmrc.gov.uk</a>.</p>
       |<p class="govuk-body">If you've already completed both stages, you can ignore this message.</p>
       |""".stripMargin

  def reminderPayRenewal(paymentAmount: String, referenceNumber: String,dueDate:String): String =
    s"""
       |<p class="govuk-body">You have submitted your annual declaration, but have not paid your annual supervision fee.</p>
       |<p class="govuk-body">You must pay this fee to continue your registration with HMRC.</p>
       |
       |<p class="govuk-body"><strong>Your fee:</strong> $paymentAmount</p>
       |<p class="govuk-body"><strong>Your payment reference:</strong> $referenceNumber</p>
       |<p class="govuk-body"><strong>Due date:</strong> $dueDate</p>
       |
       |<p class="govuk-body">You must quote your payment reference when you pay your fee. If you do not, your fee may show as unpaid and your registration may be cancelled.</p>
       |<p class="govuk-body">To pay your fee, go to <a href="https://www.tax.service.gov.uk/pay-online/money-laundering" class="govuk-link">Pay money laundering regulations fees</a>.</p>
       |
       |<h2 class="govuk-heading-m">If your registration is cancelled</h2>
       |<p class="govuk-body">You will need to submit a new registration application and pay the correct fees, including the application charge.</p>
       |<p class="govuk-body">If your registration is cancelled, you must not undertake any activity covered by the Money Laundering Regulations. Otherwise, you and/or your business may be subject to civil sanctions, such as a fine, or criminal proceedings.</p>
       |
       |<h2 class="govuk-heading-m">More information</h2>
       |<p class="govuk-body">For details of HMRC's registration policy and fees, visit <a href="https://www.gov.uk/guidance/money-laundering-regulations-registration-fees" class="govuk-link">Fees you'll pay for money laundering supervision</a>.</p>
       |<p class="govuk-body">If you need help, contact us at <a href="mailto:MLRCIT@hmrc.gov.uk" class="govuk-link">MLRCIT@hmrc.gov.uk</a>.</p>
       |<p class="govuk-body">If you've already paid your fee (quoting your payment reference), you can ignore this message.</p>
       |""".stripMargin

  val guidanceRegisterRenewUrl    =
    "http://www.gov.uk/guidance/register-or-renew-your-money-laundering-supervision-with-hmrc"
  val disagreeAppealsPenaltiesUrl =
    "https://www.gov.uk/guidance/money-laundering-regulations-appeals-and-penalties#if-you-disagree-with-an-hmrc-decision"
  val guidanceRegistrationFeesUrl = "https://www.gov.uk/guidance/money-laundering-regulations-registration-fees"

  def registrationAutoExpiry(endDate: String): String =
    s"""
       |<p class="govuk-body">Your or your business' registration with HMRC has been cancelled.</p>
       |<p class="govuk-body">This is because you did not pay the annual supervision fee imposed under Regulation 102 of the Money Laundering, Terrorist Financing and Transfer of Funds (Information on the Payer) Regulations 2017.</p>
       |<p class="govuk-body">This cancellation decision is made under regulation 60(3)(a) by virtue of regulation 59(1)(c)(ii).</p>
       |
       |<h2 class="govuk-heading-m">What happens next</h2>
       |<p class="govuk-body">The cancellation will come into effect on <strong>${formatDate(endDate)}</strong>. You may conclude any relevant business during this time.</p>
       |<p class="govuk-body">After <strong>${formatDate(endDate)}</strong>, you must not undertake any activity covered by the Money Laundering Regulations. Otherwise, you and/or your business may be subject to civil sanctions, such as a fine, or criminal proceedings.</p>
       |
       |<h2 class="govuk-heading-m">If you want to remain registered</h2>
       |<p class="govuk-body">You must submit a new application and pay all fees, including the application charge.</p>
       |<p class="govuk-body">Find more information at <a href="https://www.gov.uk/guidance/register-or-renew-your-money-laundering-supervision-with-hmrc" class="govuk-link">Register or update your money laundering supervision with HMRC</a>.</p>
       |
       |<h2 class="govuk-heading-m">If you disagree with this decision</h2>
       |<p class="govuk-body">You have the right to request an HMRC internal review or appeal to an independent tribunal.</p>
       |<p class="govuk-body">Find more information at <a href="https://www.gov.uk/guidance/money-laundering-regulations-appeals-and-penalties" class="govuk-link">Money laundering supervision appeals and penalties</a>.</p>
       |<p class="govuk-body">If you need further details or help, contact us at <a href="mailto:MLRCIT@hmrc.gov.uk" class="govuk-link">MLRCIT@hmrc.gov.uk</a>.</p>
       |""".stripMargin

  def reminderRenewal(endDate: String): String =
    s"""
       |<p class="govuk-body">You or your business are currently registered with HMRC for anti-money laundering supervision.</p>
       |<p class="govuk-body">It's time to confirm your details and pay your annual fee.</p>
       |<p class="govuk-body">If you do not do this by <strong>${formatDate(endDate)}</strong>, HMRC will cancel your registration.</p>
       |<p class="govuk-body">This is a two-stage process. You must complete both stages to stay registered.</p>
       |
       |<h2 class="govuk-heading-m">Stage 1: Confirm your details</h2>
       |<p class="govuk-body">Go to <a href="${controllers.routes.StatusController.get().url}" class="govuk-link">Your registration</a> and select 'Extend your supervision'.</p>
       |<p class="govuk-body">Confirm all your registration details are correct and up to date. If anything has changed, you can make an update.</p>
       |<p class="govuk-body">Even if none of your details have changed, you must still complete this step.</p>
       |<p class="govuk-body">When you have confirmed your details, you will be asked to make a declaration. You must select 'Accept and submit' to complete stage 1.</p>
       |
       |<h2 class="govuk-heading-m">Stage 2: Pay your fee</h2>
       |<p class="govuk-body">When you have completed stage 1, the system will display your annual fee and payment reference.</p>
       |<p class="govuk-body">You must quote your payment reference when you pay your fee. If you do not, your fee may show as unpaid and your registration may be cancelled.</p>
       |
       |<h2 class="govuk-heading-m">If your registration is cancelled</h2>
       |<p class="govuk-body">You will need to submit a new registration application and pay the correct fees, including the application charge.</p>
       |<p class="govuk-body">If your registration is cancelled, you must not undertake any activity covered by the Money Laundering Regulations. Otherwise, you and/or your business may be subject to civil sanctions, such as a fine, or criminal proceedings.</p>
       |
       |<h2 class="govuk-heading-m">More information</h2>
       |<p class="govuk-body">For details of HMRC's registration policy and how to pay your fee, visit <a href="https://www.gov.uk/guidance/money-laundering-regulations-registration-fees" class="govuk-link">Fees you'll pay for money laundering supervision</a>.</p>
       |<p class="govuk-body">If you need help, contact us at <a href="mailto:MLRCIT@hmrc.gov.uk" class="govuk-link">MLRCIT@hmrc.gov.uk</a>.</p>
       |<p class="govuk-body">If you've already completed both stages, you can ignore this message.</p>
       |""".stripMargin
}
