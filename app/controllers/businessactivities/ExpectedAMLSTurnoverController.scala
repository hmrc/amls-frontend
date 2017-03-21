package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.ExpectedAMLSTurnover
import models.businessactivities.{BusinessActivities, _}
import services.StatusService
import utils.ControllerHelper
import views.html.businessactivities._
import models.businessmatching._

import scala.concurrent.Future

trait ExpectedAMLSTurnoverController extends BaseController {

  val dataCacheConnector: DataCacheConnector
  implicit val statusService:StatusService

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ControllerHelper.allowedToEdit flatMap {
        case true => dataCacheConnector.fetchAll map {
          optionalCache =>
            (for {
              cache <- optionalCache
              businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            } yield {
              (for {
                businessActivities <- cache.getEntry[BusinessActivities](BusinessActivities.key)
                expectedTurnover <- businessActivities.expectedAMLSTurnover
              } yield Ok(expected_amls_turnover(Form2[ExpectedAMLSTurnover](expectedTurnover), edit, businessMatching.activities)))
                .getOrElse (Ok(expected_amls_turnover(EmptyForm, edit, businessMatching.activities)))
            }) getOrElse Ok(expected_amls_turnover(EmptyForm, edit, None))
        }
        case false => Future.successful(NotFound(notFoundView))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[ExpectedAMLSTurnover](request.body) match {
        case f: InvalidForm =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
          } yield {
            BadRequest(expected_amls_turnover(f, edit, businessMatching.activities))
          }

        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivities.expectedAMLSTurnover(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.BusinessFranchiseController.get())
          }
      }
    }
  }
}

object ExpectedAMLSTurnoverController extends ExpectedAMLSTurnoverController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override implicit val statusService: StatusService = StatusService
}
