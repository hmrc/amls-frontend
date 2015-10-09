package controllers.auth

import play.api.Play
import play.api.Play.current
import uk.gov.hmrc.play.config.RunMode

object ExternalUrls extends RunMode {
  val companyAuthHost = s"${Play.configuration.getString(s"govuk-tax.$env.services.auth.company-auth.host").getOrElse("")}"
  val loginCallback = Play.configuration.getString(s"govuk-tax.$env.services.auth.login-callback.url").getOrElse("/anti-money-laundering/login")
  val loginPath = s"${Play.configuration.getString(s"govuk-tax.$env.services.auth.login_path").getOrElse("")}"
  val signIn = s"$companyAuthHost/account/$loginPath?continue=$loginCallback"
  val signOut = s"$companyAuthHost/account/sign-out"

}
