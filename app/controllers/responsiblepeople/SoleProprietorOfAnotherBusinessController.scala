package controllers.responsiblepeople

import javax.inject.Inject

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.responsiblepeople.{SoleProprietorOfAnotherBusiness, ResponsiblePeople}
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{RepeatingSection, ControllerHelper}

import scala.concurrent.Future

class SoleProprietorOfAnotherBusinessController @Inject()(
                                                           val dataCacheConnector: DataCacheConnector,
                                                           val authConnector: AuthConnector) extends RepeatingSection with BaseController{

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        getData[ResponsiblePeople](index) map {rp =>
          Ok(views.html.responsiblepeople.sole_proprietor(EmptyForm, true, 0, true, ControllerHelper.rpTitleName(rp)))
        }
    }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    import jto.validation.forms.Rules._
    implicit authContext => implicit request =>
      Form2[SoleProprietorOfAnotherBusiness](request.body) match {
        case f: InvalidForm =>
          getData[ResponsiblePeople](index) map { rp =>
            BadRequest(views.html.responsiblepeople.sole_proprietor(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
          }
        case ValidForm(_, data) => {
          dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key).map {
            case Some(_) => {
              edit match {
                case true => Redirect(routes.DetailedAnswersController.get(index))
                case false => {
                  data.soleProprietorOfAnotherBusiness match {
                    case true => Redirect(routes.VATRegisteredController.get(index, edit, fromDeclaration))
                    case false => Redirect(routes.RegisteredForSelfAssessmentController.get(index, edit, fromDeclaration))
                  }
                }
              }
            }
            case None => NotFound(notFoundView)
          }

        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

}