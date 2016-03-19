package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{TypeOfBusiness, BusinessMatching}
import views.html.businessmatching.type_of_business

import scala.concurrent.Future

trait TypeOfBusinessController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map {
        response =>
          val form: Form2[TypeOfBusiness] = (for {
            businessMatching <- response
            business <- businessMatching.typeOfBusiness
          } yield Form2[TypeOfBusiness](business)).getOrElse(EmptyForm)
          Ok(type_of_business(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[TypeOfBusiness](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(type_of_business(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
            _ <- dataCacheConnector.save[BusinessMatching](BusinessMatching.key,
              businessMatching.typeOfBusiness(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.RegisterServicesController.get())
          }
      }
    }
  }
}

object TypeOfBusinessController extends TypeOfBusinessController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
