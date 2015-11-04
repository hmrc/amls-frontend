package builders

import org.mockito.Matchers
import org.mockito.Mockito._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._

import scala.concurrent.Future
object AuthBuilder {

  def createUserAuthContext(userId: String, userName: String): AuthContext = {
    AuthContext(authority = createUserAuthority(userId), nameFromSession = Some(userName))
  }

  def mockAuthorisedUser(userId: String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {Future.successful(Some(createUserAuthority(userId)))
    }
  }

  private def createUserAuthority(userId: String): Authority = {
    Authority(userId, Accounts(org= Some(OrgAccount("org/1234", Org("1234")))), None, None)
  }

  def mockUnAuthorisedUser(userId: String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val payeAuthority = Authority(userId, Accounts(paye = Some(PayeAccount(userId, Nino("AA026813B")))), None, None)
      Future.successful(Some(payeAuthority))
    }
  }

  def createUserAuthContextIndCt(userId: String, userName: String): AuthContext = {
    val ctAuthority = Authority(userId, Accounts(ct = Some(CtAccount(userId, CtUtr("543212300017")))), None, None)
    AuthContext(authority = ctAuthority, nameFromSession = Some(userName))
  }

}
