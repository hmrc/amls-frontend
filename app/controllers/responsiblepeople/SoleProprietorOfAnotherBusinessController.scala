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

  def test(startDate: Option[ResponsiblePeople], fieldName: String = "personName") = {
    startDate match {
      case Some(rp) => {
        val name = rp.personName
        Map(fieldName -> Seq(name))
      }
      case _ => Map.empty[String, Seq[String]]
    }
  }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    import jto.validation.forms.Rules._
    implicit authContext => implicit request =>

      getData[ResponsiblePeople](index) flatMap { rp =>

        val extraInfo = rp match {
          case Some(rp) => rp.personName match {
            case Some(name) => Map("personName" -> Seq(name.fullName))
            case _ => Map.empty[String, Seq[String]]
          }
          case None => Map.empty[String, Seq[String]]
        }

        Form2[SoleProprietorOfAnotherBusiness](request.body.asFormUrlEncoded.get ++ extraInfo) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.responsiblepeople.sole_proprietor(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp))))

          case ValidForm(_, data) => {
            dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key).map {
              case Some(_) => redirectDependingOnEdit(data, index, edit, fromDeclaration)
              case None => NotFound(notFoundView)
            }
          }
        }
      }
  }

  def redirectDependingOnEdit(data: SoleProprietorOfAnotherBusiness, index: Int, edit: Boolean, fromDeclaration: Boolean) = {
    edit match {
      case true => Redirect(routes.DetailedAnswersController.get(index))
      case false => redirectDependingOnFormResponse(data, index, edit, fromDeclaration)
    }
  }

  def redirectDependingOnFormResponse(data: SoleProprietorOfAnotherBusiness, index: Int, edit: Boolean, fromDeclaration: Boolean) = {
    data.soleProprietorOfAnotherBusiness match {
      case true => Redirect(routes.VATRegisteredController.get(index, edit, fromDeclaration))
      case false => Redirect(routes.RegisteredForSelfAssessmentController.get(index, edit, fromDeclaration))
    }
  }

}