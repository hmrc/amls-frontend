package controllers.businessactivities

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.auth.AmlsRegime
import models.aboutthebusiness._
import models.businessmatching.BusinessActivities
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait BusinessInvolvedInOtherActivitiesController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key) map {
        case Some(BusinessActivities(_, Some(data), _, _, _)) =>
          Ok(views.html.business_reg_for_vat(Form2[BusinessInvolvedInOtherActivities](data), edit))
        case _ =>
          Ok(views.html.business_reg_for_vat(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[BusinessInvolvedInOtherActivities](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.business_reg_for_vat(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessactivities <- dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[BusinessActivities](BusinessActivities.key,
              businessactivities.businessInvolvedInOtherActivities(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.ConfirmRegisteredOfficeController.get())
          }
      }
    }
  }
}

object BusinessInvolvedInOtherActivitiesController extends BusinessInvolvedInOtherActivitiesController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}