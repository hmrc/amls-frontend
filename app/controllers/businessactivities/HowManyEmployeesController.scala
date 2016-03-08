package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, HowManyEmployees}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait HowManyEmployeesController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key) map {
          case Some(BusinessActivities(_, _, _, _, _, _, _, _, Some(data))) =>
            Ok(views.html.business_employees(Form2[HowManyEmployees](data), edit))
          case _ =>
            Ok(views.html.business_employees(EmptyForm, edit))
        }
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[HowManyEmployees](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.business_employees(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[BusinessActivities](BusinessActivities.key,
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
