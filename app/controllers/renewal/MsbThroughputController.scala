package controllers.renewal

import javax.inject.Inject

import controllers.BaseController
import forms.EmptyForm
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class MsbThroughputController @Inject()(val authConnector: AuthConnector) extends BaseController {

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.renewal.msb_total_throughput(EmptyForm)))
  }

  def post() = Authorised.async {
    implicit authContext => implicit request => ???
  }

}
