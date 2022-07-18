/*
 * Copyright 2022 HM Revenue & Customs
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

package services.notifications.v5m0

import models.notifications.ContactType
import models.notifications.ContactType._

class MessageDetails

object MessageDetails {

  def static(contactType: ContactType, url: String): String = {
    contactType match {
      case ApplicationAutorejectionForFailureToPay => s"<p>Your application to be supervised by HM Revenue and Customs (HMRC) under The Money Laundering, Terrorist Financing and Transfer of Funds (Information on the Payer) Regulations 2017 has failed.</p><p>As you’ve not paid the full fees due, your application has automatically expired.</p><p>You need to be registered with a <a href=${ "\"" }https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register${ "\"" }>supervisory body</a> if Money Laundering Regulations apply to your business. If you’re not supervised you may be subject to penalties and criminal charges.</p><p>If you still need to be registered with HMRC you should submit a new application immediately. You can apply from your account <a href=${ "\"" + url + "\"" }>status page</a>.</p>"
      case RegistrationVariationApproval =>  s"<p>The recent changes made to your details have been approved.</p><p>You can find details of your registration on your <a href=${ "\"" + url + "\"" }>status page</a>.</p>"
      case DeRegistrationEffectiveDateChange => s"<p>The date your anti-money laundering supervision ended has been changed.</p><p>You can see the new effective date on your <a href=${ "\"" + url + "\"" }>status page</a>.</p>"
      case _ => throw new Exception("An Unknown Exception has occurred, v5m0:static():MessageDetails")
    }
  }

  def endDate(contactType: ContactType, endDate: String, url: String, referenceNumber: String): String = {
    contactType match {
      case ApplicationApproval => s"<p>Your application to register has been approved. You’re now registered until ${ endDate }.</p><p>Your anti-money laundering registration number is: ${ referenceNumber }.</p><p>You can find details of your registration on your <a href=${ "\"" + url + "\"" }>status page</a>.</p>"
      case RenewalApproval => s"<p>Your annual fee has been paid.</p><p>To continue to be registered with HMRC you will need to repeat this process next year. Your next annual fee will be due before ${ endDate }.</p><p>HMRC will contact you again to remind you to complete the renewal questions and pay your annual fee.</p><p>You can find your registration details on your status page.</p>"
      case AutoExpiryOfRegistration => s"<p>Your business’s registration with HMRC is cancelled. Despite previous reminders to pay the fees imposed under the money laundering regulations you have failed to do so.</p><p>The cancellation will come into effect in 14 days from the date of this notice to allow you to conclude relevant business.</p><p><h3>To be registered by HMRC you must submit a new registration application and pay the correct fees.</h3></p><p>If your business is not registered with a relevant supervisory body (such as HMRC), you and/or your business may be subject to civil sanctions, such as a fine, or criminal proceedings if you continue to trade in activities covered by the regulations.</p><p><h3>If you/the business disagree with this decision</h3></p><p>Under the regulations, HMRC must offer you a review of their decision.</p><p>You have 30 days to:<br><li>Contact us to ask for a HMRC review or</li><br><li>Appeal directly to an independent tribunal, if you do not want a review</li><br></p><p>HMRC reviews are carried out by an independent team. You will be notified of the outcome of the review within 45 days unless another time is agreed.</p><p>If you are not satisfied with the conclusion of the review, you still have a right of appeal to a tribunal. These are administered by the Tribunal Service and you will have 30 days to appeal following notification of the outcome of the review.</p><p>HMRC’s money laundering registration policy can be found on <a href= https://www.gov.uk>www.gov.uk, where you can also find</a> information about what you can do <a href= https://www.gov.uk/guidance/money-laundering-regulations-appeals-and-penalties>if you disagree with an HMRC decision, including</a> how to request a review of our decision or <a href=https://www.gov.uk/tax-tribunal>make appeal to the tribunal.</a></p>"
      case RenewalReminder => s"<p>It is time to pay your business’s annual fee.</p><p>You need to check and verify that your details are up to date and pay your annual fee in full by ${ endDate }.</p><p>Failing to pay your fee in a timely manner may lead to HMRC cancelling your registration.</p><p>Start this process from your status page. Completing this will tell you the amount of your annual fee.</p><p>HMRCs money laundering registration policy, and details of how to pay your fee can be found on <a href=https://www.gov.uk>www.gov.uk.</a></p><p> If your business is not registered with a relevant supervisory body (such as HMRC) you and/or your business may be subject to civil sanctions, such as a fine, or criminal proceedings if you continue to trade in activities covered by the Regulations.</p><p>If you have taken the steps outlined above, please ignore this message,</p>"
      case NewRenewalReminder => s"<p>It is time to pay your business’s annual fee.</p><p>You need to check and verify that your details are up to date and pay your annual fee in full by ${ endDate }. </p><p>If you do not pay your fees by the above date, HMRC will cancel your business’s registration.</p><p>Start this process from your status page. Completing this will tell you the amount of your annual fee.</p><p>HMRCs money laundering registration policy will assist you in understanding and calculating your fee.</p><p>Details of how to pay your fee can be found on www.gov.uk.</p><p>If you are experiencing difficulties in carrying out these actions, please contact MLRCIT@hmrc.gov.uk.</p><p>If your business is not registered with a relevant supervisory body (such as HMRC) you and/or your business may be subject to civil sanctions, such as a fine, or criminal proceedings if you continue to trade in activities covered by the Regulations.</p><p>If you have taken the steps outlined above, please ignore this message.</p>"
      case _ => throw new Exception("An Unknown Exception has occurred, v5m0:endDate():MessageDetails")
    }
  }

  def reminder(contactType: ContactType, paymentAmount: String, referenceNumber: String): String = {
    contactType match {
      case ReminderToPayForVariation => s"<p>You need to pay ${ paymentAmount } for the recent changes made to your details.</p><p>Your payment reference is: ${ referenceNumber }.</p><p>Find details of how to pay on your status page.</p><p>It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"
      case ReminderToPayForRenewal => s"<p>HMRC imposes an annual charge for supervision. You need to pay this to continue your registration with HMRC.</p><p>Your payment reference is: ${ referenceNumber }.</p><br><br><p>If you do not pay your fees within 28 working days from the date of this message, HMRC will cancel your business’s registration.</p><p>HMRCs money laundering registration policy, and details of how to pay your fee can be found on <a href=https://www.gov.uk>www.gov.uk.</a></p><p>If you are experiencing difficulties in carrying out these actions, please contact MLRCIT@hmrc.gov.uk.</p><p>If you continue to trade in activities covered by the Money Laundering Regulations and are not registered with the relevant supervisory body (such as HMRC), you may be subject to civil sanctions or criminal proceedings.</p><p>If you have already paid, please ignore this message.</p>"
      case ReminderToPayForApplication => s"<p>You need to pay ${ paymentAmount } for your application to register with HM Revenue and Customs.</p><p>Your payment reference is: ${ referenceNumber }.</p><p>Find details of how to pay on your status page.</p><p>It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"
      case ReminderToPayForManualCharges => s"<p>You need to pay ${ paymentAmount } for the recent charge added to your account.</p><p>Your payment reference is: ${ referenceNumber }.</p><p>Find details of how to pay on your status page.</p><p>It can take time for some payments to clear, so if you’ve already paid you can ignore this message.</p>"
      case _ => throw new Exception("An Unknown Exception has occurred, v5m0:reminder():MessageDetails")
    }
  }

}
