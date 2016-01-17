package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.aboutthebusiness.{RegisteredOfficeOrMainPlaceOfBusinessUK, RegisteredOfficeOrMainPlaceOfBusiness, AboutTheBusiness}

import scala.concurrent.Future

trait RegisteredOfficeOrMainPlaceOfBusinessController extends BaseController  {

  val dataCacheConnector: DataCacheConnector

  val model = RegisteredOfficeOrMainPlaceOfBusinessUK("", "", None, None, "")

  def get(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key) map {
        case Some(AboutTheBusiness(_ , _, Some(data))) =>  Ok(views.html.registered_office_or_main_place
            (Form2[RegisteredOfficeOrMainPlaceOfBusiness](data), edit))
        case _ =>  Ok(views.html.registered_office_or_main_place(Form2[RegisteredOfficeOrMainPlaceOfBusiness](model), edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[RegisteredOfficeOrMainPlaceOfBusiness](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.registered_office_or_main_place(f, edit)))
        case ValidForm(_, data) => {
          for {
            aboutTheBusiness <- dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key
              , aboutTheBusiness.registeredOfficeOrMainPlaceOfBusiness(data))

          } yield (Redirect(routes.BusinessRegisteredForVATController.get()))
        }
      }
  }
}

object RegisteredOfficeOrMainPlaceOfBusinessController extends RegisteredOfficeOrMainPlaceOfBusinessController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
