package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.responsiblepeople.{PersonResidenceType, ResponsiblePeople}
import utils.RepeatingSection
import views.html.responsiblepeople._

import scala.concurrent.Future

trait PersonResidentTypeController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
    Authorised.async {
      implicit authContext => implicit request =>
        getData[ResponsiblePeople](index) map {
          response =>
            val form: Form2[PersonResidenceType] = (for {
              responsiblePeople <- response
              personResidence <- responsiblePeople.personResidenceType
            } yield Form2[PersonResidenceType](personResidence)).getOrElse(EmptyForm)
            Ok(person_residence_type(form, edit, index))
        }
    }
  }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          Form2[PersonResidenceType](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(person_residence_type(f, edit, index)))
            case ValidForm(_, data) =>
              for {
                _ <- updateData[ResponsiblePeople](index) {
                  case Some(ResponsiblePeople(Some(x), value, Some(y), Some(z))) => Some(ResponsiblePeople(Some(x), value, Some(y), Some(z)))
                  case _ => data
                }
              } yield edit match {
                case false => Redirect(routes.RegisteredForSelfAssessmentController.get(index, edit))
                case true  => Redirect(routes.SummaryController.get())
              }
          }
      }
    }
}

object PersonResidentTypeController extends PersonResidentTypeController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override def dataCacheConnector = DataCacheConnector
}

