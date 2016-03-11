package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, NCARegistered}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait NCARegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form = (for {
            businessActivities <- response
            ncaRegistered <- businessActivities.ncaRegistered
          } yield Form2[NCARegistered](ncaRegistered)).getOrElse(EmptyForm)
          Ok(views.html.nca_registered(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[NCARegistered](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.nca_registered(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[BusinessActivities](BusinessActivities.key,
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
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}