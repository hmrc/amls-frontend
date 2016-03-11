package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessactivities.{BusinessActivities, _}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait IdentifySuspiciousActivityController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form = (for {
            businessActivities <- response
            identifySuspiciousActivity <- businessActivities.identifySuspiciousActivity
          } yield Form2[IdentifySuspiciousActivity](identifySuspiciousActivity)).getOrElse(EmptyForm)
          Ok(views.html.identify_suspicious_activity(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[IdentifySuspiciousActivity](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.identify_suspicious_activity(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[BusinessActivities](BusinessActivities.key,
              businessActivities.identifySuspiciousActivity(data)
            )
          } yield edit match {
              //todo : Implement the correct redirects when relevant pages are available
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.NCARegisteredController.get())

          }
      }
  }
}

object IdentifySuspiciousActivityController extends IdentifySuspiciousActivityController {
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}