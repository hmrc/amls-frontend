package controllers.aboutthebusiness

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.aboutthebusiness._
import views.html.aboutthebusiness._

import scala.concurrent.Future

trait VATRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        case Some(AboutTheBusiness(_, Some(data), _, _, _)) =>
          Ok(vat_registered(Form2[VATRegistered](data), edit))
        case _ =>
          Ok(vat_registered(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[VATRegistered](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(vat_registered(f, edit)))
        case ValidForm(_, data) =>
          for {
            aboutTheBusiness <- dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCacheConnector.save[AboutTheBusiness](AboutTheBusiness.key,
              aboutTheBusiness.vatRegistered(data)
            )
          } yield edit match {
            case true =>  Redirect(routes.SummaryController.get())
            case false => Redirect(routes.ConfirmRegisteredOfficeController.get())
          }
      }
    }
  }
}

object VATRegisteredController extends VATRegisteredController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}