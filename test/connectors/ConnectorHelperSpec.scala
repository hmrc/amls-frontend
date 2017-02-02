package connectors

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{CtUtr, SaUtr}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.{Principal, LoggedInUser, AuthContext}

class ConnectorHelperSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  "return sa account type and reference" when {
    "Logged in user is sa account type" in {

      implicit val saAcct = AuthContext(
        LoggedInUser(
          "UserName",
          None,
          None,
          None,
          CredentialStrength.Weak,
          ConfidenceLevel.L50, ""),
        Principal(
          None,
          Accounts(sa = Some(SaAccount("Link", SaUtr("saRef"))))),
        None,
        None,
        None, None)
      ConnectorHelper.accountTypeAndId(saAcct) must be(("sa", "saRef"))
    }
  }

  "return ct account type and reference" when {
    "Logged in user is ct account type" in {

      implicit val ctAcct = AuthContext(
        LoggedInUser(
          "UserName",
          None,
          None,
          None,
          CredentialStrength.Weak,
          ConfidenceLevel.L50, ""),
        Principal(
          None,
          Accounts(ct = Some(CtAccount("Link", CtUtr("ctRef"))))),
        None,
        None,
        None, None)
      ConnectorHelper.accountTypeAndId(ctAcct) must be(("ct", "ctRef"))
    }
  }

  "fail on not finding correct accountType" in {

    implicit val ctAcct = AuthContext(
      LoggedInUser(
        "UserName",
        None,
        None,
        None,
        CredentialStrength.Weak,
        ConfidenceLevel.L50, ""),
      Principal(
        None,
        Accounts(ct = None)),
      None,
      None,
      None, None)
    an[IllegalArgumentException] should be thrownBy ConnectorHelper.accountTypeAndId(ctAcct)
  }
}
