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

package config

import javax.inject.Inject

import play.api.{Logger, Play}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.config.inject.{ServicesConfig => iServicesConfig}
import play.api.Play.{configuration, current}

trait ApplicationConfig {

  def amendmentsToggle: Boolean

  def release7: Boolean

  def refreshProfileToggle: Boolean

  def returnLinkToggle: Boolean

  def frontendBaseUrl: String

  def hasAcceptedToggle: Boolean

  def allowReregisterToggle: Boolean
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
  lazy val paymentsUrl = getConfigString("paymentsUrl")

  lazy val timeout = getInt("timeout.seconds")
  lazy val timeoutCountdown = getInt("timeout.countdown")

  def businessCustomerUrl = getConfigString("business-customer.url")

  lazy val whitelist = Play.configuration.getStringSeq("whitelist") getOrElse Seq.empty
  lazy val ggUrl = baseUrl("government-gateway")

  lazy val enrolUrl = s"$ggUrl/enrol"

  lazy val regFee = getConfigInt("amounts.registration")
  lazy val premisesFee = getConfigInt("amounts.premises")
  lazy val peopleFee = getConfigInt("amounts.people")

  def amendmentsToggle: Boolean = {
    val value = getConfBool("feature-toggle.amendments", false)
    value
  }

  override def release7: Boolean = {
    val value = getConfBool("feature-toggle.release7", false)
    value
  }

  override def refreshProfileToggle = getConfBool("feature-toggle.refresh-profile", false)

  override def frontendBaseUrl = {
    val secure = getConfBool("amls-frontend.public.secure", defBool = false)
    val scheme = if (secure) "https" else "http"
    val host = getConfString("amls-frontend.public.host", "")

    s"$scheme://$host"
  }

  override def returnLinkToggle = getConfBool("feature-toggle.return-link", false)

  override def hasAcceptedToggle = getConfBool("feature-toggle.has-accepted", false)

  override def allowReregisterToggle: Boolean = getConfBool("feature-toggle.allow-reregister", false)

}

class AppConfig @Inject()(val config: iServicesConfig) {
  def showFeesToggle = config.getConfBool("feature-toggle.show-fees", defBool = false)

  def enrolmentStoreToggle = config.getConfBool("feature-toggle.enrolment-store", defBool = false)

  def authUrl = config.baseUrl("auth")

  def enrolmentStoreUrl = config.baseUrl("enrolment-store-proxy")
}