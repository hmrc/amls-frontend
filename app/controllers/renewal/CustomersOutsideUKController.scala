package controllers.renewal

import javax.inject.{Inject, Singleton}

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, CustomersOutsideUK}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal._

import scala.concurrent.Future

@Singleton
class CustomersOutsideUKController @Inject()(val dataCacheConnector: DataCacheConnector, val authConnector: AuthConnector
) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form: Form2[CustomersOutsideUK] = (for {
            businessActivities <- response
            customers <- businessActivities.customersOutsideUK
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
            businessActivity <-
            dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivity.customersOutsideUK(data)
            )
          } yield Redirect(routes.SummaryController.get())
        }
      }
  }
}
