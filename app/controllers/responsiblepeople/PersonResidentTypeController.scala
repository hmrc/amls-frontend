package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.Country
import models.responsiblepeople.{PersonResidenceType, ResponsiblePeople}
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.person_residence_type

import scala.concurrent.Future

trait PersonResidentTypeController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        getData[ResponsiblePeople](index) map {
          case Some(ResponsiblePeople(Some(personName), Some(residencyType), _, _, _, _, _, _, _, _, _, _, _, _, _))
          => Ok(person_residence_type(Form2[PersonResidenceType](residencyType), edit, index, fromDeclaration, personName.titleName))
          case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _))
          => Ok(person_residence_type(EmptyForm, edit, index, fromDeclaration, personName.titleName))
          case _
          => NotFound(notFoundView)
        }
    }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>

        Form2[PersonResidenceType](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePeople](index) map {rp =>
              BadRequest(person_residence_type(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            for {
              result <- updateDataStrict[ResponsiblePeople](index) { rp =>
                val nationality = rp.personResidenceType.fold[Option[Country]](None)(x => x.nationality)
                val updatedData = data.copy(nationality = nationality)
                rp.personResidenceType(updatedData)
              }
            } yield edit match {
              case true => Redirect(routes.DetailedAnswersController.get(index))
              case false => Redirect(routes.NationalityController.get(index, edit, fromDeclaration))
            }
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
    }
}

object PersonResidentTypeController extends PersonResidentTypeController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector

  override def dataCacheConnector = DataCacheConnector
}

