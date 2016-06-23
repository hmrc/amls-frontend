package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{ExpectedAMLSTurnover}
import models.businessactivities.{BusinessActivities, _}
import views.html.businessactivities._
import models.businessmatching.{BusinessMatching, BusinessActivity}


import scala.concurrent.Future

trait ExpectedAMLSTurnoverController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchAll map {
          optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            mlrActivities <- businessMatching.activities
          } yield {
            (for {
              businessActivities <- cache.getEntry[BusinessActivities](BusinessActivities.key)
              expectedTurnover <- businessActivities.expectedAMLSTurnover
            } yield Ok(expected_amls_turnover(Form2[ExpectedAMLSTurnover](expectedTurnover), edit, mlrActivities.businessActivities)))
              .getOrElse (Ok(expected_amls_turnover(EmptyForm, edit, mlrActivities.businessActivities)))
          }) getOrElse Ok(expected_amls_turnover(EmptyForm, edit, Set.empty))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[ExpectedAMLSTurnover](request.body) match {
        case f: InvalidForm =>
          dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key).map {
            businessMatchingOpt =>
              val activities: Set[BusinessActivity] = (for {
                businessMatching <- businessMatchingOpt
                mlrActivities <- businessMatching.activities
              } yield mlrActivities.businessActivities).getOrElse(Set.empty)
              BadRequest(expected_amls_turnover(f, edit, activities))
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
}
