package controllers.auth

import config.ApplicationConfig
import uk.gov.hmrc.play.frontend.auth.GovernmentGateway

object AmlsGovernmentGateway extends GovernmentGateway {
  override def loginURL: String = ApplicationConfig.loginUrl
  override def continueURL: String = ApplicationConfig.loginContinue
}
