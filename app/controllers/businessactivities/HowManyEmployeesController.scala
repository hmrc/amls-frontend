package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, HowManyEmployees}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessactivities._

import scala.concurrent.Future

trait HowManyEmployeesController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
          response =>
            val form: Form2[HowManyEmployees] = (for {
              businessActivities <- response
              employees <- businessActivities.howManyEmployees
            } yield Form2[HowManyEmployees](employees)).getOrElse(EmptyForm)
            Ok(business_employees(form, edit))
        }
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[HowManyEmployees](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_employees(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivities.employees(data))
          } yield edit match {
            case true => Redirect(routes.CustomersOutsideUKController.get())
            case false => Redirect(routes.CustomersOutsideUKController.get())
          }
      }
    }
  }
}

object HowManyEmployeesController extends HowManyEmployeesController {
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val authConnector: AuthConnector = AMLSAuthConnector
}
