package connectors

import javax.inject.{Inject, Singleton}

import play.api.Logger
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticatorConnector @Inject()(http: HttpPost, config: ServicesConfig) {

  val serviceUrl = config.baseUrl("authenticator")

  def refreshProfile(implicit hc: HeaderCarrier, ec: ExecutionContext) = {

    //noinspection SimplifyBooleanMatch
    config.getConfBool("feature-toggle.refresh-profile", defBool = false) match {
      case true =>
        http.POSTEmpty(s"$serviceUrl/authenticator/refresh-profile") map { response =>
          Logger.info("[AuthenticatorConnector] Current user profile was refreshed")
          response
        }
      case _ => Future.successful(HttpResponse(200))
    }

  }

}
