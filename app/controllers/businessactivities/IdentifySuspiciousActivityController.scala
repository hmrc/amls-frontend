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
    implicit authContext => implicit request => Future.successful(Ok(views.html.identify_suspicious_activity(EmptyForm, edit)))
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request => Future.successful(NotFound)
  }
}

object IdentifySuspiciousActivityController extends IdentifySuspiciousActivityController {
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}