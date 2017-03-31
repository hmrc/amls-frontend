package controllers.renewal

import javax.inject.Inject

import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.renewal.{MsbThroughput, Renewal}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.msb_total_throughput

import scala.concurrent.Future

class MsbThroughputController @Inject()(val authConnector: AuthConnector, renewals: RenewalService) extends BaseController {

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(msb_total_throughput(EmptyForm)))
  }

  def post() = Authorised.async {
    implicit authContext => implicit request =>
      Form2[MsbThroughput](request.body) match {
        case form: InvalidForm => Future.successful(BadRequest(msb_total_throughput(form)))
        case ValidForm(_, model) =>
          for {
            renewal <- renewals.getRenewal
            _ <- renewals.updateRenewal(renewal.getOrElse(Renewal()).msbThroughput(model))
          } yield {
            Redirect(controllers.renewal.routes.SummaryController.get())
          }
      }
  }
}
