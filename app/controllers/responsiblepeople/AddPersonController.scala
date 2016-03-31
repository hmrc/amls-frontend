package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.AddPerson

import scala.concurrent.Future

trait AddPersonController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[AddPerson](AddPerson.key) map {
        case Some(addPerson) =>
          Ok(views.html.responsiblepeople.add_person(Form2[AddPerson](addPerson)))
        case _ =>
          Ok(views.html.responsiblepeople.add_person(EmptyForm))
      }
  }

  def post() = Authorised.async {
    implicit authContext => implicit request => {
      Form2[AddPerson](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.responsiblepeople.add_person(f)))
      }
    }
  }

}

object AddPersonController extends AddPersonController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
