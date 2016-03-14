package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities._
import views.html.businessactivities._

import scala.concurrent.Future

trait ExpectedBusinessTurnoverController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form: Form2[ExpectedBusinessTurnover] = (for {
            businessActivities <- response
            expectedTurnover <- businessActivities.expectedBusinessTurnover
          } yield Form2[ExpectedBusinessTurnover](expectedTurnover)).getOrElse(EmptyForm)
          Ok(expected_business_turnover(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[ExpectedBusinessTurnover](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(expected_business_turnover(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivities.expectedBusinessTurnover(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.ExpectedAMLSTurnoverController.get())
          }
      }
    }
  }
}

object ExpectedBusinessTurnoverController extends ExpectedBusinessTurnoverController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
