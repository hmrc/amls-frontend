package controllers.declaration

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.declaration.AddPerson

import scala.concurrent.Future

trait AddPersonController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[AddPerson](AddPerson.key) map {
        case Some(addPerson) =>
          Ok(views.html.declaration.add_person(Form2[AddPerson](addPerson)))
        case _ =>
          Ok(views.html.declaration.add_person(EmptyForm))
      }
  }


  def post() = Authorised.async {
    implicit authContext => implicit request => {
      Form2[AddPerson](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.declaration.add_person(f)))
        case ValidForm(_, data) =>
          for {
            _ <- dataCacheConnector.save[AddPerson](AddPerson.key, data)
          } yield Redirect(routes.DeclarationController.get())
      }
    }
  }

}

object AddPersonController extends AddPersonController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
