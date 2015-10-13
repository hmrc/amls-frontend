package controllers.auth

import config.ApplicationConfig
import uk.gov.hmrc.play.frontend.auth.GovernmentGateway

object AmlsGovernmentGateway extends GovernmentGateway {
  override def login: String = ApplicationConfig.loginUrl
}
