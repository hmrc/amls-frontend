package config

import play.api.Play
import uk.gov.hmrc.play.config.ServicesConfig

import play.api.Play.current

object ApplicationConfig extends ServicesConfig {

  private def getConfigString(key: String) = getConfString(key, throw new Exception(s"Could not find config '$key'"))

  lazy val contactHost = baseUrl("contact-frontend")
  lazy val authHost = baseUrl("auth")

  lazy val assetsPrefix = getConfigString(s"assets.url") + getConfigString(s"assets.version")

  lazy val analyticsToken = Some(getConfigString(s"analytics.token"))
  lazy val analyticsHost = getConfigString(s"analytics.host")

  lazy val betaFeedbackUrl = (if (env == "Prod") "" else contactHost) + getConfigString("contact-frontend.beta-feedback-url.authenticated")
  lazy val betaFeedbackUnauthenticatedUrl = (if (env == "Prod") "" else contactHost) + getConfigString("contact-frontend.beta-feedback-url.unauthenticated")

  lazy val reportAProblemUrl = contactHost + getConfigString("contact-frontend.report-a-problem-url")

  lazy val loginUrl = getConfigString("login.url")

  lazy val amlsUrl = baseUrl("amls")
  def subscriptionUrl(safeId: String) = s"$amlsUrl/amls/subscription/$safeId"

  lazy val businessCustomerUrl = getConfigString("business-customer.url")

  lazy val whitelist = Play.configuration.getStringSeq("whitelist") getOrElse Seq.empty

  lazy val ggUrl = baseUrl("government-gateway")
  lazy val enrolUrl = s"$ggUrl/enrol"
}
