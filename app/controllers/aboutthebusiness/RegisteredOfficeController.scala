package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.aboutthebusiness.{RegisteredOfficeUK, AboutTheBusiness, RegisteredOffice}

import scala.concurrent.Future
import views.html.aboutthebusiness._

trait RegisteredOfficeController extends BaseController  {

  val dataCacheConnector: DataCacheConnector

  private val preSelectUK = RegisteredOfficeUK("","", None, None, "")

  def get(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        case Some(AboutTheBusiness(_ , _, _, _, Some(data), _)) =>
          edit match {
            case true => Ok(registered_office(Form2[RegisteredOffice](data), edit))
            case false => Ok(registered_office(Form2[RegisteredOffice](preSelectUK), edit))
          }
        case _ =>
          Ok(registered_office(Form2[RegisteredOffice](preSelectUK), edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[RegisteredOffice](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(registered_office(f, edit)))
        case ValidForm(_, data) => {
          for {
            aboutTheBusiness <-
              dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCacheConnector.save[AboutTheBusiness](AboutTheBusiness.key,
              aboutTheBusiness.registeredOffice(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.ContactingYouController.get(edit))
          }
        }
      }
  }
}

object RegisteredOfficeController extends RegisteredOfficeController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
