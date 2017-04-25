package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, RiskAssessmentPolicy}
import models.businessmatching.{AccountancyServices, BusinessMatching}
import utils.ControllerHelper
import views.html.businessactivities._

import scala.concurrent.Future

trait RiskAssessmentController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form: Form2[RiskAssessmentPolicy] = (for {
            businessActivities <- response
            riskAssessmentPolicy <- businessActivities.riskAssessmentPolicy
          } yield Form2[RiskAssessmentPolicy](riskAssessmentPolicy)).getOrElse(EmptyForm)
          Ok(risk_assessment_policy(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    import jto.validation.forms.Rules._
    implicit authContext => implicit request =>
      Form2[RiskAssessmentPolicy](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(risk_assessment_policy(f, edit)))
        case ValidForm(_, data) => {
          dataCacheConnector.fetchAll map {
            optionalCache =>
              (for {
                cache <- optionalCache
                bmBusinessActivity <- ControllerHelper.getBusinessActivity(cache.getEntry[BusinessMatching](BusinessMatching.key))
                businessActivity <- cache.getEntry[BusinessActivities](BusinessActivities.key)
              } yield {
                dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
                  businessActivity.riskAssessmentPolicy(data))
                redirectDependingOnEdit(edit, bmBusinessActivity)
              }).getOrElse(Redirect(routes.SummaryController.get()))
          }
        }
      }
  }

  private def redirectDependingOnEdit(edit: Boolean, bmBusinessActivity: models.businessmatching.BusinessActivities) = edit match {
    case true => Redirect(routes.SummaryController.get())
    case false => bmBusinessActivity.businessActivities.contains(AccountancyServices) match {
      case true => Redirect(routes.SummaryController.get())
      case false => Redirect(routes.AccountantForAMLSRegulationsController.get())
    }
  }
}

object RiskAssessmentController extends RiskAssessmentController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
