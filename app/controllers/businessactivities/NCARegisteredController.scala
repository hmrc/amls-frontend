package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, NCARegistered}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessactivities._

import scala.concurrent.Future

trait NCARegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form: Form2[NCARegistered] = (for {
            businessActivities <- response
            ncaRegistered <- businessActivities.ncaRegistered
          } yield Form2[NCARegistered](ncaRegistered)).getOrElse(EmptyForm)
          Ok(nca_registered(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[NCARegistered](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(nca_registered(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivities.ncaRegistered(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.RiskAssessmentController.get())
          }
      }
    }
  }
}

object NCARegisteredController extends NCARegisteredController {
  // $COVERAGE-OFF$
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}