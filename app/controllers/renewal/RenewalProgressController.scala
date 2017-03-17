package controllers.renewal

import javax.inject.{Inject, Singleton}

import controllers.BaseController
import play.api.mvc.AnyContent
import play.mvc.Result
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.renewal_progress

import scala.concurrent.Future

@Singleton
class RenewalProgressController @Inject()(val authConnector: AuthConnector) extends BaseController {

  def get() = Authorised.async {
    implicit authContext => implicit request =>

    Future.successful(Ok(renewal_progress()))
  }

}
