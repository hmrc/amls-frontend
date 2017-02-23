package connectors

import javax.inject.Inject

import com.google.inject.ImplementedBy
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.Future

@ImplementedBy(classOf[DesAuthenticatorConnector])
trait AuthenticatorConnector {
  def refreshProfile(implicit hc: HeaderCarrier): Future[HttpResponse]
}

class DesAuthenticatorConnector @Inject()(http: HttpPost, config: ServicesConfig) extends AuthenticatorConnector {

  def refreshProfile(implicit hc: HeaderCarrier) = {

    http.POSTEmpty(s"${config.baseUrl("authenticator")}/refresh-profile")

  }

}
