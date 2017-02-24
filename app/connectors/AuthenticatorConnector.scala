package connectors

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.Future

@Singleton
class AuthenticatorConnector @Inject()(http: HttpPost, config: ServicesConfig) {

  def refreshProfile(implicit hc: HeaderCarrier) = {

    config.getConfBool("feature-toggle.refresh-profile", false) match {
      case true => http.POSTEmpty(s"${config.baseUrl("authenticator")}/authenticator/refresh-profile")
      case _ => Future.successful(HttpResponse(200))
    }

  }

}
