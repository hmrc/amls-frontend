package controllers.declaration

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.declaration.AddPerson
import play.api.i18n.Messages

trait DeclarationController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def get() = declarationView(("declaration.declaration.title","submit.registration"))
  def getWithAmendment() = declarationView(("declaration.declaration.amendment.title","submit.amendment.application"))

  private def declarationView(headings: (String,String)) = Authorised.async {
    implicit authcontext => implicit request =>
      dataCacheConnector.fetch[AddPerson](AddPerson.key) map {
        case Some(addPerson) =>
          val name = s"${addPerson.firstName} ${addPerson.middleName mkString} ${addPerson.lastName}"
          Ok(views.html.declaration.declare(headings, name))
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
