/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Mode.Mode
import play.api.{Application, Configuration, Environment, Play}
import uk.gov.hmrc.play.config.ServicesConfig

trait ApplicationConfig {

  def refreshProfileToggle: Boolean

  def frontendBaseUrl: String
}

object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration

  private def getConfigString(key: String) = getConfString(key, throw new Exception(s"Could not find config '$key'"))
  private def getConfigInt(key: String) = getConfInt(key, throw new Exception(s"Could not find config '$key'"))

  val contactFormServiceIdentifier = "AMLS"

  lazy val contactHost = baseUrl("contact-frontend")
  lazy val authHost = baseUrl("auth")
  lazy val assetsPrefix = getConfigString(s"assets.url") + getConfigString(s"assets.version")

  lazy val analyticsToken = Some(getConfigString(s"analytics.token"))
  lazy val analyticsHost = getConfigString(s"analytics.host")

  lazy val betaFeedbackUrl = (if (env == "Prod") "" else contactHost) + getConfigString("contact-frontend.beta-feedback-url.authenticated")
  lazy val betaFeedbackUnauthenticatedUrl = (if (env == "Prod") "" else contactHost) + getConfigString("contact-frontend.beta-feedback-url.unauthenticated")

  val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  lazy val loginUrl = getConfigString("login.url")
  lazy val logoutUrl = getConfigString("logout.url")
  lazy val loginContinue = getConfigString("login.continue")

  lazy val amlsUrl = baseUrl("amls")
  lazy val subscriptionUrl = s"$amlsUrl/amls/subscription"

  lazy val notificationsUrl = baseUrl("amls-notification")
  lazy val allNotificationsUrl = s"$notificationsUrl/amls-notification"
  lazy val paymentsUrl:String = getConfigString("paymentsUrl")

  lazy val timeout = getInt("timeout.seconds")
  lazy val timeoutCountdown = getInt("timeout.countdown")

  lazy val ampWhatYouNeedUrl = s"${getConfString("amls-art-market-participant-frontend.url", "")}/what-you-need"
  lazy val ampSummeryUrl     = s"${getConfString("amls-art-market-participant-frontend.url", "")}/check-your-answers"

  def businessCustomerUrl = getConfigString("business-customer.url")

  private implicit lazy val app:Application = Play.current
  lazy val whitelist = Play.configuration.getStringSeq("whitelist") getOrElse Seq.empty

  lazy val ggUrl = baseUrl("government-gateway")

  lazy val enrolUrl = s"$ggUrl/enrol"
  
  lazy val mongoCacheUpdateUrl = baseUrl("amls-stub") + getConfigString("amls-stub.get-file-url")
  lazy val testOnlyStubsUrl = baseUrl("test-only") + getConfigString("test-only.get-base-url")

  // The following values are used by the Fee Guidance Controller currently toggled off with feature-toggle.show-fees
  lazy val regFee = getConfigInt("amounts.registration")
  lazy val premisesFee = getConfigInt("amounts.premises")
  lazy val peopleFeeRate = getConfigInt("amounts.people")
  lazy val approvalCheckPeopleFeeRate = getConfigInt("amounts.approval-check-rate")

  override def refreshProfileToggle = getConfBool("feature-toggle.refresh-profile", false)

  override def frontendBaseUrl = {
    val secure = getConfBool("amls-frontend.public.secure", defBool = false)
    val scheme = if (secure) "https" else "http"
    val host = getConfString("amls-frontend.public.host", "")

    s"$scheme://$host"
  }
}

class AppConfig @Inject()(environment: Environment, val runModeConfiguration: Configuration, baseConfig: Configuration)
  extends ApplicationConfig with ServicesConfig {

  override protected def mode: Mode = environment.mode

  def amlsUrl = baseUrl("amls")

  def subscriptionUrl = s"$amlsUrl/amls/subscription"

  def enrolmentStoreToggle = getConfBool("feature-toggle.enrolment-store", defBool = false)

  def fxEnabledToggle = getConfBool("feature-toggle.fx-enabled", defBool = false)

  def authUrl = baseUrl("auth")

  def enrolmentStoreUrl = baseUrl("tax-enrolments")

  def enrolmentStubsEnabled: Boolean = getConfBool("enrolment-stubs.enabled", defBool = false)

  def enrolmentStubsUrl = baseUrl("enrolment-stubs")

  def feePaymentUrl = s"$amlsUrl/amls/payment"

  def notificationsUrl = baseUrl("amls-notification")

  def allNotificationsUrl = s"$notificationsUrl/amls-notification"

  def ggUrl = baseUrl("government-gateway")

  def enrolUrl = s"$ggUrl/enrol"

  val mongoEncryptionEnabled = baseConfig.getBoolean("appCache.mongo.encryptionEnabled") getOrElse true
  val mongoAppCacheEnabled = baseConfig.getBoolean("appCache.mongo.enabled") getOrElse false
  val cacheExpiryInSeconds = baseConfig.getInt("appCache.expiryInSeconds") getOrElse 60

  override def refreshProfileToggle: Boolean = getConfBool("feature-toggle.refresh-profile", false)

  override def frontendBaseUrl = {
    val secure = getConfBool("amls-frontend.public.secure", defBool = false)
    val scheme = if (secure) "https" else "http"
    val host = getConfString("amls-frontend.public.host", "")

    s"$scheme://$host"
  }
}