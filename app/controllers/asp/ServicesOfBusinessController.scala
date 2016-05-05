package controllers.asp

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.asp.{ServicesOfBusiness, Asp}
import views.html.asp._

import scala.concurrent.Future

trait ServicesOfBusinessController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Asp](Asp.key) map {
        response =>
          val form = (for {
            business <- response
            setOfServices <- business.services
          } yield Form2[ServicesOfBusiness](setOfServices)).getOrElse(EmptyForm)
          Ok(services_of_business(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    import play.api.data.mapping.forms.Rules._
    implicit authContext => implicit request =>
      Form2[ServicesOfBusiness](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(services_of_business(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessServices <- dataCacheConnector.fetch[Asp](Asp.key)
            _ <- dataCacheConnector.save[Asp](Asp.key,
              businessServices.services(data))
          } yield edit match {
            case true =>
              Redirect(routes.OtherBusinessTaxMattersController.get()) //TODO need to change this to SummaryController once we get the checkyouranswers page
            case false =>
              Redirect(routes.OtherBusinessTaxMattersController.get())

          }
      }
  }
}

object ServicesOfBusinessController extends ServicesOfBusinessController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}

