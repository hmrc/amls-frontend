package controllers.renewal

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.renewal.{CustomersOutsideUK, Renewal}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal._
import services.RenewalService

import scala.concurrent.Future

@Singleton
class CustomersOutsideUKController @Inject()(val dataCacheConnector: DataCacheConnector, val authConnector: AuthConnector,
                                             val renewalService: RenewalService
) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      renewalService.getRenewal map {
        response =>
          val form: Form2[CustomersOutsideUK] = (for {
            renewal <- response
            customers <- renewal.customersOutsideUK
          } yield Form2[CustomersOutsideUK](customers)).getOrElse(EmptyForm)
          Ok(customers_outside_uk(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[CustomersOutsideUK](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(customers_outside_uk(f, edit)))
        case ValidForm(_, data) => {
          for {
            renewal <- renewalService.getRenewal
            _ <- renewalService.updateRenewal(renewal.getOrElse(Renewal()).customersOutsideUK(data))
          } yield {
              Redirect(routes.SummaryController.get())
            }
          }
        }
      }
}


