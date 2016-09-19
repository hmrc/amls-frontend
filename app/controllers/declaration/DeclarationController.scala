package controllers.declaration

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.declaration.AddPerson

trait DeclarationController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def get() = declarationView("submit.registration")
  def getWithAmendment() = declarationView("submit.amendment.registration")

  private def declarationView(title: String) = Authorised.async {
    implicit authcontext => implicit request =>
      dataCacheConnector.fetch[AddPerson](AddPerson.key) map {
        case Some(addPerson) =>
          val name = s"${addPerson.firstName} ${addPerson.middleName mkString} ${addPerson.lastName}"
          Ok(views.html.declaration.declare(title, name))
        case _ =>
          Redirect(routes.AddPersonController.get())
      }
  }

}

object DeclarationController extends DeclarationController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
