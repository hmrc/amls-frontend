package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.aboutthebusiness._
import views.html.aboutthebusiness._

trait ContactingYouController extends BaseController {

  val dataCache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        aboutTheBusiness <-
        dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
      } yield aboutTheBusiness match {
        case Some(AboutTheBusiness(_, _, _, Some(details), Some(registeredOffice), _)) =>
          Ok(contacting_you(Form2[ContactingYou](details), registeredOffice, edit))
        case Some(AboutTheBusiness(_, _, _, None, Some(registeredOffice), _)) =>
          Ok(contacting_you(EmptyForm, registeredOffice, edit))
        case _ =>
          // TODO: Make sure this redirects to the right place
          Redirect(routes.ConfirmRegisteredOfficeController.get(edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ContactingYouForm](request.body) match {
        case f: InvalidForm =>
          for {
            aboutTheBusiness <-
            dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
          } yield aboutTheBusiness match {
            case Some(AboutTheBusiness(_, _, _, _, Some(registeredOffice), _)) =>
              BadRequest(contacting_you(f, registeredOffice, edit))
            case _ =>
              Redirect(routes.ContactingYouController.get(edit))
          }
        case ValidForm(_, data) =>
          for {
            aboutTheBusiness <- dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCache.save[AboutTheBusiness](AboutTheBusiness.key,
              aboutTheBusiness.contactingYou(data).correspondenceAddress(None)
            )
          } yield data.letterToThisAddress match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.CorrespondenceAddressController.get(edit))
          }
      }
  }
}

object ContactingYouController extends ContactingYouController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCache = DataCacheConnector
}
