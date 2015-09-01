package config

import uk.gov.hmrc.play.config.ServicesConfig

object ApplicationConfig extends ServicesConfig {

  private def getConfigString(key: String) =
    getConfString(key, "")

  private lazy val contactHost = baseUrl("contact-frontend")

  lazy val assetsPrefix = getConfigString(s"assets.url") + getConfigString(s"assets.version")

  lazy val analyticsToken = Some(getConfigString(s"analytics.token"))
  lazy val analyticsHost = getConfigString(s"analytics.host")

  lazy val betaFeedbackUrl =
    contactHost + getConfigString(s"contact-frontend.beta-feedback-url.authenticated")
  lazy val betaFeedbackUnauthenticatedUrl =
    contactHost + getConfigString(s"contact-frontend.beta-feedback-url.unauthenticated")

  lazy val reportAProblemUrl = contactHost + getConfigString(s"contact-frontend.report-a-problem-url")
}
