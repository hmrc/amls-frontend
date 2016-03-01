package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{TurnerOverExpectIn12Months, BusinessActivities}

import scala.concurrent.Future

trait TurnerOverExpectIn12MonthsController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key) map {
        case Some(BusinessActivities(_, Some(data))) => Ok(views.html.turn_over_expect_in_12_months(Form2[TurnerOverExpectIn12Months](data), edit))
        case _ => Ok(views.html.turn_over_expect_in_12_months(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[TurnerOverExpectIn12Months](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.turn_over_expect_in_months(f, edit)))
        case ValidForm(_, data) =>
          for {
            aboutYou <- dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[BusinessActivities](BusinessActivities.key,
              businessActivities.turnoverOverExpectIn12MOnths(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.TurnerOverExpectIn12MonthsController.get())
          }
      }
    }
  }
}

object TurnerOverExpectIn12MonthsController extends TurnerOverExpectIn12MonthsController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
