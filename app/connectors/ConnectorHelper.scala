package connectors

import uk.gov.hmrc.domain.{CtUtr, SaUtr, Org}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{CtAccount, SaAccount, OrgAccount}

object ConnectorHelper {

  protected[connectors] def accountTypeAndId(implicit ac: AuthContext): (String, String) = {
    val accounts = ac.principal.accounts

    accounts.org match {
      case Some(OrgAccount(_, Org(ref))) =>("org", ref)
      case _ => accounts.sa match {
        case Some(SaAccount(_, SaUtr(ref))) => ("sa", ref)
        case _ =>  accounts.ct match {
          case Some(CtAccount(_, CtUtr(ref))) => ("ct", ref)
          case _ =>throw new IllegalArgumentException("authcontext does not contain any of the expected account types")
        }
      }
    }
  }
}

