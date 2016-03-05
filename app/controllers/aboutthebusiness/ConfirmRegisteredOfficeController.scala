package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{KeystoreConnector$, DataCacheConnector}
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.aboutthebusiness.{AboutTheBusiness, BusinessCustomerDetails, ConfirmRegisteredOffice}

import scala.concurrent.Future

trait ConfirmRegisteredOfficeController extends BaseController {

  def dataCache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        registeredOffice <-
        dataCache.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
      } yield registeredOffice match {
        case Some(AboutTheBusiness(_, _, _, Some(data), _)) =>
          Ok(views.html.confirm_registered_office_or_main_place(EmptyForm, data))
        case _ =>
          Redirect(routes.RegisteredOfficeController.get())
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ConfirmRegisteredOffice](request.body) match {
        case f: InvalidForm =>
          for {
            aboutTheBusiness <-
            dataCache.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
          } yield aboutTheBusiness match {
            case Some(AboutTheBusiness(_, _, _, Some(registeredOffice), _)) =>
              BadRequest(views.html.confirm_registered_office_or_main_place(f, registeredOffice))
            case _ =>
              Redirect(routes.RegisteredOfficeController.get(edit))
          }
        case ValidForm(_, data) =>
          data.isRegOfficeOrMainPlaceOfBusiness match {
            case true => Future.successful(Redirect(routes.ContactingYouController.get(edit)))
            case false => Future.successful(Redirect(routes.RegisteredOfficeController.get(edit)))
          }
      }
  }
}

object ConfirmRegisteredOfficeController extends ConfirmRegisteredOfficeController {
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
