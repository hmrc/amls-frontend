package controllers.declaration

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.declaration.AddPerson

trait DeclarationController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def get() = Authorised.async {
    implicit authcontext => implicit request =>
      dataCacheConnector.fetch[AddPerson](AddPerson.key) map {
        case Some(addPerson) =>
          val name = s"${addPerson.firstName} ${addPerson.middleName mkString} ${addPerson.lastName}"
          Ok(views.html.declaration.declare(name))
        case _ =>
          Redirect(routes.AddPersonController.get())
      }
  }

}

object DeclarationController extends DeclarationController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
