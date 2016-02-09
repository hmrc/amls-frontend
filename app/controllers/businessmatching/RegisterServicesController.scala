package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, EmptyForm, InvalidForm, ValidForm}
import models.businessmatching.{BusinessMatching, BusinessActivities}
import scala.concurrent.Future

trait RegisterServicesController  extends BaseController {

  val dataCacheConnector: DataCacheConnector

//  def get(edit: Boolean = false) = Authorised.async {
//    implicit authContext => implicit request =>
//      dataCacheConnector.fetchDataShortLivedCache[BusinessMatching](BusinessMatching.key) map {
//        case Some(BusinessMatching(Some(data), None)) =>
//          Ok("dsjfhgdsjhg")
//        //Ok(views.html.what_you_need_to_register(Form2[BusinessActivities](data), edit))
//        case _ => Ok("dsjfhgdsjhg")//Ok(views.html.what_you_need_to_register(EmptyForm, edit))
//      }
//  }
//
//  def post(edit: Boolean = false) = Authorised.async {
//    implicit authContext => implicit request => {
//      Form2[BusinessActivities](request.body) match {
//        case invalidForm : InvalidForm =>
//          Future.successful(BadRequest/*BadRequest(views.html.what_you_need_to_register(invalidForm, edit))*/)
//        case ValidForm(data, _) =>
//          for {
//            businessMatching <- dataCacheConnector.fetchDataShortLivedCache[BusinessMatching](BusinessMatching.key)
//            _ <- dataCacheConnector.saveDataShortLivedCache[BusinessActivities](BusinessMatching.key, businessMatching.activities(data))
//          } yield  Redirect(controllers.routes.MainSummaryController.get())
//      }
//    }
//  }

}

object RegisterServicesController extends RegisterServicesController   {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}