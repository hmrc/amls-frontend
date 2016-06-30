package config

import play.api.{Logger, Play}
import uk.gov.hmrc.play.config.ServicesConfig
import play.api.Play.current

object ApplicationConfig extends ServicesConfig {

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

  lazy val amlsUrl = baseUrl("amls")
  lazy val subscriptionUrl = s"$amlsUrl/amls/subscription"

  lazy val businessCustomerUrl = getConfigString("business-customer.url")

  lazy val whitelist = Play.configuration.getStringSeq("whitelist") getOrElse Seq.empty

  lazy val ggUrl = baseUrl("government-gateway")
  lazy val enrolUrl = s"$ggUrl/enrol"

  lazy val paymentsUrl = getConfigString("paymentsUrl")

  lazy val regFee = getConfigInt("amounts.registration")
  lazy val premisesFee = getConfigInt("amounts.premises")
  lazy val peopleFee = getConfigInt("amounts.people")

  val hvdToggle: Boolean = {
    val value = getConfBool("feature-toggle.hvd", false)
    Logger.info("s[ApplicationConfig][hvd]")
    value
  }

  val responsiblePeopleToggle: Boolean = {
    val value = getConfBool("feature-toggle.responsible-people", false)
    Logger.info(s"[ApplicationConfig][responsible-people] $value")
    value
  }

  val enrolmentToggle: Boolean = {
    val value = getConfBool("feature-toggle.gg-enrolment", false)
    Logger.info(s"[ApplicationConfig][gg-enrolment] $value")
    value
  }
}
