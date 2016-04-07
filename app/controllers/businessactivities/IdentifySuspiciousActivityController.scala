package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessactivities.{BusinessActivities, _}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessactivities._

import scala.concurrent.Future

trait IdentifySuspiciousActivityController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form: Form2[IdentifySuspiciousActivity] = (for {
            businessActivities <- response
            identifySuspiciousActivity <- businessActivities.identifySuspiciousActivity
          } yield Form2[IdentifySuspiciousActivity](identifySuspiciousActivity)).getOrElse(EmptyForm)
          Ok(identify_suspicious_activity(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[IdentifySuspiciousActivity](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(identify_suspicious_activity(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
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
  // $COVERAGE-OFF$
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}