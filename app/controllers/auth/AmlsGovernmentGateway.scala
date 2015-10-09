package controllers.auth

import uk.gov.hmrc.play.frontend.auth.GovernmentGateway

object AmlsGovernmentGateway extends GovernmentGateway {
  override def login: String = ExternalUrls.signIn
}
