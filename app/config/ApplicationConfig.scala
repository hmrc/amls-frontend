/*
 * Copyright 2017 HM Revenue & Customs
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

package config

import play.api.{Logger, Play}
import uk.gov.hmrc.play.config.ServicesConfig
import play.api.Play.current

trait ApplicationConfig {
  def enrolmentToggle: Boolean

  def notificationsToggle: Boolean

  def amendmentsToggle: Boolean

  def release7: Boolean

  def refreshProfileToggle: Boolean

  def paymentsUrlLookupToggle: Boolean

  def renewalsToggle: Boolean

  def allowWithdrawalToggle: Boolean

  def allowDeRegisterToggle: Boolean
}


object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  private def getConfigString(key: String) = getConfString(key, throw new Exception(s"Could not find config '$key'"))

  private def getConfigInt(key: String) = getConfInt(key, throw new Exception(s"Could not find config '$key'"))

  lazy val contactHost = baseUrl("contact-frontend")
  lazy val authHost = baseUrl("auth")

  lazy val assetsPrefix = getConfigString(s"assets.url") + getConfigString(s"assets.version")

  lazy val analyticsToken = Some(getConfigString(s"analytics.token"))
  lazy val analyticsHost = getConfigString(s"analytics.host")

  lazy val betaFeedbackUrl = (if (env == "Prod") "" else contactHost) + getConfigString("contact-frontend.beta-feedback-url.authenticated")
  lazy val betaFeedbackUnauthenticatedUrl = (if (env == "Prod") "" else contactHost) + getConfigString("contact-frontend.beta-feedback-url.unauthenticated")

  lazy val reportAProblemUrl = contactHost + getConfigString("contact-frontend.report-a-problem-url")

  lazy val loginUrl = getConfigString("login.url")
  lazy val logoutUrl = getConfigString("logout.url")
  lazy val loginContinue = getConfigString("login.continue")

  lazy val amlsUrl = baseUrl("amls")
  lazy val subscriptionUrl = s"$amlsUrl/amls/subscription"

  lazy val feePaymentUrl = s"$amlsUrl/amls/payment"

  lazy val notificationsUrl = baseUrl("amls-notification")
  lazy val allNotificationsUrl = s"$notificationsUrl/amls-notification"

  lazy val authUrl = baseUrl("auth")

  def businessCustomerUrl = getConfigString("business-customer.url")

  lazy val whitelist = Play.configuration.getStringSeq("whitelist") getOrElse Seq.empty

  lazy val ggUrl = baseUrl("government-gateway")
  lazy val enrolUrl = s"$ggUrl/enrol"

  lazy val paymentsUrl = getConfigString("paymentsUrl")

  lazy val regFee = getConfigInt("amounts.registration")
  lazy val premisesFee = getConfigInt("amounts.premises")
  lazy val peopleFee = getConfigInt("amounts.people")

  override def notificationsToggle = getConfBool("feature-toggle.notifications", false)

  def amendmentsToggle: Boolean = {
    val value = getConfBool("feature-toggle.amendments", false)
    value
  }

  override def enrolmentToggle: Boolean = {
    val value = getConfBool("feature-toggle.gg-enrolment", false)
    value
  }

  override def release7:Boolean = {
    val value = getConfBool("feature-toggle.release7", false)
    value
  }

  override def renewalsToggle:Boolean = getConfBool("feature-toggle.renewals", false)

  override def refreshProfileToggle = getConfBool("feature-toggle.refresh-profile", false)

  override def paymentsUrlLookupToggle = getConfBool("feature-toggle.payments-url-lookup", false)

  override def allowWithdrawalToggle = getConfBool("feature-toggle.allow-withdrawal", false)

  override def allowDeRegisterToggle = getConfBool("feature-toggle.allow-deregister", false)
}
