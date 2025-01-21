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

package config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.Request
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URLEncoder

@Singleton
class ApplicationConfig @Inject()(configuration: Configuration, servicesConfig: ServicesConfig) {

  private def baseUrl(serviceName: String) = {
    val protocol = configuration.getOptional[String](s"microservice.services.$serviceName.protocol").getOrElse("http")
    val host = configuration.get[String](s"microservice.services.$serviceName.host")
    val port = configuration.get[String](s"microservice.services.$serviceName.port")
    s"$protocol://$host:$port"
  }

  private def getConfigString(key: String) = servicesConfig.getConfString(key, throw new Exception(s"Could not find config '$key'"))

  lazy val feedbackFrontendUrl = getConfigString("feedback-frontend.url")

  def contactFrontendReportUrl(implicit request: Request[_]): String = {
    getConfigString("contact-frontend.report-problem-url.non-js") +
      "&referrerUrl=" + URLEncoder.encode(frontendBaseUrl + request.uri, "utf-8")
  }

  def logoutUrl = getConfigString("logout.url")
  def logoutUrlWithFeedback = s"$logoutUrl?continue=${URLEncoder.encode(feedbackFrontendUrl, "utf-8")}"
  lazy val loginContinue = getConfigString("login.continue")

  lazy val paymentsUrl:String = getConfigString("paymentsUrl")

  lazy val timeout = servicesConfig.getInt("timeout.seconds")
  lazy val timeoutCountdown = servicesConfig.getInt("timeout.countdown")

  lazy val ampWhatYouNeedUrl = s"${servicesConfig.getConfString("amls-art-market-participant-frontend.url", "")}/what-you-need"
  lazy val ampSummaryUrl     = s"${servicesConfig.getConfString("amls-art-market-participant-frontend.url", "")}/check-your-answers"

  lazy val eabWhatYouNeedUrl = s"${servicesConfig.getConfString("amls-estate-agency-business-frontend.url", "")}/what-you-need"
  lazy val eabSummaryUrl     = s"${servicesConfig.getConfString("amls-estate-agency-business-frontend.url", "")}/check-your-answers"
  lazy val eabRedressUrl     = s"${servicesConfig.getConfString("amls-estate-agency-business-frontend.redress-url", "")}/change-redress-scheme"

  def businessCustomerUrl = getConfigString("business-customer.url")


  def amlsUrl = baseUrl("amls")

  def subscriptionUrl = s"$amlsUrl/amls/subscription"

  def enrolmentStoreToggle = servicesConfig.getConfBool("feature-toggle.enrolment-store", false)

  def fxEnabledToggle = servicesConfig.getConfBool("feature-toggle.fx-enabled", false)

  def enrolmentStoreUrl = baseUrl("tax-enrolments")

  def enrolmentStubsEnabled: Boolean = servicesConfig.getConfBool("enrolment-stubs.enabled", false)

  def enrolmentStubsUrl = baseUrl("enrolment-stubs")

  def feePaymentUrl = s"$amlsUrl/amls/payment"

  def notificationsUrl = baseUrl("amls-notification")

  def allNotificationsUrl = s"$notificationsUrl/amls-notification"

  val mongoEncryptionEnabled = configuration.getOptional[Boolean]("appCache.mongo.encryptionEnabled").getOrElse(true)
  val cacheExpiryInSeconds = configuration.getOptional[Int]("appCache.expiryInSeconds").getOrElse(60)

  def frontendBaseUrl = {
    val secure = servicesConfig.getConfBool("amls-frontend.public.secure", false)
    val scheme = if (secure) "https" else "http"
    val host = servicesConfig.getConfString("amls-frontend.public.host", "")

    s"$scheme://$host"
  }

  def encryptionKey = servicesConfig.getString("json.encryption.key")

  val amlsStubBaseUrl = baseUrl("amls-stub")

  lazy val payBaseUrl = s"${baseUrl("pay-api")}/pay-api"

  lazy val businessMatchingUrl = s"${baseUrl("business-customer")}/business-customer"

  val tradingPremisesVirtualOfficeLink = "https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register#premises-to-register"

  val tcspWhoNeedsToRegisterLink = "https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register#premises-to-register"

  val contactHmrcLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/money-laundering"

  val applicationWhoNeedsToRegisterLink = "https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register#businesses-already-supervised-for-money-laundering-purposes"

  val tradeInformationLink = "https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register"

  val legislationLink = "http://www.legislation.gov.uk/search"

  val tribunalLink = "https://www.gov.uk/tax-tribunal"

  val howToPayLink = "https://www.gov.uk/guidance/money-laundering-regulations-registration-fees#how-to-pay"

  val cardPaymentLink = "https://www.gov.uk/pay-tax-debit-credit-card"

  val waysToPayLink = "https://www.gov.uk/guidance/pay-money-laundering-regulations-fees-and-penalty-charges#ways-to-pay"

  val registerNewOrgLink = "/coafe/government-gateway/register?accountType=organisation&continue=%2Fanti-money-laundering&origin=amls-frontend"

  val nationalCrimeAgencyUrl = "https://sarsreporting.nationalcrimeagency.gov.uk/"

  val companiesHouseUrl = "https://find-and-update.company-information.service.gov.uk/"
}
