package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.aboutthebusiness.{RegisteredOfficeUK, AboutTheBusiness, RegisteredOffice}

import scala.concurrent.Future

trait RegisteredOfficeController extends BaseController  {

  val dataCacheConnector: DataCacheConnector
  val preSelectUK = RegisteredOfficeUK("","",Some(""), Some(""), "")

  def get(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key) map {
        case Some(AboutTheBusiness(_ , _, _, Some(data), _)) =>
          Ok(views.html.registered_office_or_main_place(Form2[RegisteredOffice](data), edit))
        case _ =>
          Ok(views.html.registered_office_or_main_place(Form2[RegisteredOffice](preSelectUK), edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[RegisteredOffice](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.registered_office_or_main_place(f, edit)))
        case ValidForm(_, data) => {
          for {
            aboutTheBusiness <-
              dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
            _ <-
              dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key,
              aboutTheBusiness.registeredOffice(data)
            )
          } yield Redirect(routes.ContactingYouController.get(edit))
        }
      }
  }
}

object RegisteredOfficeController extends RegisteredOfficeController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
