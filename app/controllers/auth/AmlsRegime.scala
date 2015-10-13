package controllers.auth

import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.frontend.auth.{AuthenticationProvider, TaxRegime}

object AmlsRegime extends TaxRegime{
  override def isAuthorised(accounts: Accounts): Boolean = {
    accounts.ct.isDefined || accounts.org.isDefined || accounts.sa.isDefined
  }
  override def authenticationType: AuthenticationProvider = AmlsGovernmentGateway
  override def unauthorisedLandingPage: Option[String] = Some(controllers.routes.AmlsController.unauthorised().url)
}
