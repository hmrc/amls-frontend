package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.aboutthebusiness.{CorporationTaxRegistered, AboutTheBusiness}
import views.html.aboutthebusiness.corporation_tax_registered

import scala.concurrent.Future

trait CorporationTaxRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
          response =>
          val form: Form2[CorporationTaxRegistered] = (for {
            aboutTheBusiness <- response
            corporationTaxRegistered <- aboutTheBusiness.corporationTaxRegistered
          } yield Form2[CorporationTaxRegistered](corporationTaxRegistered)).getOrElse(EmptyForm)
          Ok(corporation_tax_registered(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CorporationTaxRegistered](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(corporation_tax_registered(f, edit)))
        case ValidForm(_, data) =>
          for {
            aboutTheBusiness <- dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCacheConnector.save[AboutTheBusiness](AboutTheBusiness.key,
              aboutTheBusiness.corporationTaxRegistered(data)
            )
          } yield edit match {
            case true =>  Redirect(routes.SummaryController.get())
            case false => Redirect(routes.ConfirmRegisteredOfficeController.get())
          }
      }
    }
  }
}

object CorporationTaxRegisteredController extends CorporationTaxRegisteredController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
