package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.businessmatching.{BusinessMatching, BusinessActivities}
import scala.concurrent.Future

trait RegisterServicesController  extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>

      dataCacheConnector.fetchDataShortLivedCache[BusinessMatching](BusinessMatching.key) map {
        case Some(BusinessMatching(Some(data))) =>
          Ok(views.html.what_you_need_to_register(Form2[BusinessActivities](data), edit))
        case _ => Ok(views.html.what_you_need_to_register(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    import play.api.data.mapping.forms.Rules._
    implicit authContext => implicit request => {
      Form2[BusinessActivities](request.body) match {
        case invalidForm : InvalidForm =>
          Future.successful(BadRequest(views.html.what_you_need_to_register(invalidForm, edit)))
        case ValidForm(_, data) =>
          for {
            businessMatching <- dataCacheConnector.fetchDataShortLivedCache[BusinessMatching](BusinessMatching.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[BusinessMatching](BusinessMatching.key,
              businessMatching.activities(data))
          } yield edit match {
            case true =>
              Redirect(routes.SummaryController.get())
            case false => {
              Redirect(routes.SummaryController.get())
            }
          }
      }
    }
  }

}

object RegisterServicesController extends RegisterServicesController   {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}