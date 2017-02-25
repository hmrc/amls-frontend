package connectors

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.Future

@Singleton
class AuthenticatorConnector @Inject()(http: HttpPost, config: ServicesConfig) {

  val serviceUrl = config.baseUrl("authenticator")

  def refreshProfile(implicit hc: HeaderCarrier) = {

    config.getConfBool("feature-toggle.refresh-profile", false) match {
      case true => http.POSTEmpty(s"$serviceUrl/authenticator/refresh-profile")
      case _ => Future.successful(HttpResponse(200))
    }

  }

}
