package controllers.declaration

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.aboutyou.YourDetails
import models.declaration.AddPerson
import views.html.declaration.add_person

import scala.concurrent.Future

trait AddPersonController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[AddPerson](AddPerson.key) map {
        case Some(addPerson) =>
          Ok(add_person(Form2[AddPerson](addPerson), edit))
        case _ =>
          Ok(add_person(EmptyForm, edit))
      }
  }


  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[YourDetails](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(add_person(f, edit)))
        case ValidForm(_, data) =>
          for {
            addPerson <- dataCacheConnector.fetch[AddPerson](AddPerson.key)
            _ <- dataCacheConnector.save[AddPerson](AddPerson.key, addPerson.get)
          } yield Redirect(routes.AddPersonController.get(edit))
      }
    }
  }


}

object AddPersonController extends AddPersonController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}

