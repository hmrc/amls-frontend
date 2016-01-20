package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.aboutthebusiness._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

trait ContactingYouController extends BaseController {

  val dataCache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        aboutTheBusiness <-
        dataCache.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
      } yield aboutTheBusiness match {
        case Some(AboutTheBusiness(_, _, Some(details), Some(registeredOffice), _)) =>
          Ok(views.html.contacting_you(Form2[ContactingYou](details), registeredOffice, edit))
        case Some(AboutTheBusiness(_, _, None, Some(registeredOffice), _)) =>
          Ok(views.html.contacting_you(EmptyForm, registeredOffice, edit))
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
            dataCache.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
          } yield aboutTheBusiness match {
            case Some(AboutTheBusiness(_, _, _, Some(registeredOffice), _)) =>
              BadRequest(views.html.contacting_you(f, registeredOffice, edit))
            case _ =>
              Redirect(routes.ContactingYouController.get(edit))
          }
        case ValidForm(_, data) =>
          for {
            aboutTheBusiness <- dataCache.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCache.saveDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key,
              aboutTheBusiness.contactingYou(data)
            )
          } yield data.letterToThisAddress match {
            case true => Redirect(routes.SummaryController.get()) //TODO Go to the Summary Page
            case false => Redirect(routes.CorrespondenceAddressController.get(edit))
          }
      }
  }
}

object ContactingYouController extends ContactingYouController {
  override val authConnector = AMLSAuthConnector
  override val dataCache = DataCacheConnector
}
