package controllers.aboutthebusiness

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.auth.AmlsRegime
import models.aboutthebusiness._
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait BusinessRegisteredForVATController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key) map {
        case Some(AboutTheBusiness(_, Some(data), _)) => Ok(views.html.business_reg_for_vat(Form2[RegisteredForVAT](data), edit))
        case _ => Ok(views.html.business_reg_for_vat(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[RegisteredForVAT](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.business_reg_for_vat(f, edit)))
        case ValidForm(_, data) =>
          for {
            aboutTheBusiness <- dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key,
              aboutTheBusiness.registeredForVAT(data)
            )
          } yield edit match {
              // TODO
//            case true => Redirect(routes.BusinessRegisteredForVATController.get())
            case false => Redirect(routes.ConfirmRegisteredOfficeOrMainPlaceOfBusinessController.get())
          }
      }
    }
  }
}

object BusinessRegisteredForVATController extends BusinessRegisteredForVATController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}