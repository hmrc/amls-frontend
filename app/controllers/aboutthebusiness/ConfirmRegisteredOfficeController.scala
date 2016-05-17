package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.aboutthebusiness.{RegisteredOffice, ActivityStartDate, AboutTheBusiness, ConfirmRegisteredOffice}
import views.html.aboutthebusiness._

import scala.concurrent.Future

trait ConfirmRegisteredOfficeController extends BaseController {

  def dataCache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        response =>
          val regOffice: Option[RegisteredOffice] = (for {
            aboutTheBusiness <- response
            registeredOffice <- aboutTheBusiness.registeredOffice
          } yield Option[RegisteredOffice](registeredOffice)).getOrElse(None)
          regOffice match {
            case Some(data) => Ok(confirm_registered_office_or_main_place(EmptyForm, data))
            case _ => Redirect(routes.RegisteredOfficeController.get())
          }
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ConfirmRegisteredOffice](request.body) match {
        case f: InvalidForm =>
          dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
            response =>
              val regOffice: Option[RegisteredOffice] = (for {
                aboutTheBusiness <- response
                registeredOffice <- aboutTheBusiness.registeredOffice
              } yield Option[RegisteredOffice](registeredOffice)).getOrElse(None)
              regOffice match {
                case Some(data) => BadRequest(confirm_registered_office_or_main_place(f, data))
                case _ => Redirect(routes.RegisteredOfficeController.get(edit))
              }
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
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
