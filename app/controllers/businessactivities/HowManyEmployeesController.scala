package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, HowManyEmployees}
import models.status.Renewal
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessactivities._

import scala.concurrent.Future

trait HowManyEmployeesController extends BaseController {

  def dataCacheConnector: DataCacheConnector
  val statusService: StatusService

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
            status <- statusService.getStatus
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivities.howManyEmployees(data))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => status match {
              case Renewal => Redirect(routes.CustomersOutsideUKController.get())
              case _ => Redirect(routes.TransactionRecordController.get())
            }
          }
      }
    }
  }
}

object HowManyEmployeesController extends HowManyEmployeesController {
  // $COVERAGE-OFF$
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val authConnector: AuthConnector = AMLSAuthConnector
  override val statusService: StatusService = StatusService
}
