package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{ContactDetails, ResponsiblePeople}
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.contact_details

import scala.concurrent.Future

trait ContactDetailsController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        getData[ResponsiblePeople](index) map {
          case Some(ResponsiblePeople(Some(personName), _, Some(name), _, _, _, _, _, _, _, _, _, _, _,_))
          => Ok(contact_details(Form2[ContactDetails](name), edit, index, fromDeclaration, personName.titleName))
          case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _,_))
          => Ok(contact_details(EmptyForm, edit, index, fromDeclaration, personName.titleName))
          case _
          => NotFound(notFoundView)
        }
    }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request => {

        Form2[ContactDetails](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePeople](index) map { rp =>
              BadRequest(views.html.responsiblepeople.contact_details(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            for {
              result <- updateDataStrict[ResponsiblePeople](index) { rp =>
                rp.contactDetails(data)
              }
            } yield edit match {
              case true => Redirect(routes.DetailedAnswersController.get(index))
              case false => Redirect(routes.CurrentAddressController.get(index, edit, fromDeclaration))
            }
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
      }

    }
}

object ContactDetailsController extends ContactDetailsController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
