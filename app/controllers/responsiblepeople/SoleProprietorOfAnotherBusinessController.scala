package controllers.responsiblepeople

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{ResponsiblePeople, SoleProprietorOfAnotherBusiness, VATRegistered}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.sole_proprietor

import scala.concurrent.Future

@Singleton
class SoleProprietorOfAnotherBusinessController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                          val authConnector: AuthConnector) extends RepeatingSection with BaseController {

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext =>
        implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _,_, Some(soleProprietorOfAnotherBusiness)))
            => Ok(sole_proprietor(Form2[SoleProprietorOfAnotherBusiness](soleProprietorOfAnotherBusiness), edit, index, fromDeclaration, personName.titleName))
            case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _,_, _))
            => Ok(sole_proprietor(EmptyForm, edit, index, fromDeclaration, personName.titleName))
            case _
            => NotFound(notFoundView)
          }
    }

  def getExtradInfo(rp: Option[ResponsiblePeople]) = {
    rp match {
      case Some(person) => person.personName match {
        case Some(name) => Map("personName" -> Seq(name.fullName))
        case _ => Map.empty[String, Seq[String]]
      }
      case None => Map.empty[String, Seq[String]]
    }
  }

  def getVatRegData(rp: ResponsiblePeople, data: SoleProprietorOfAnotherBusiness): Option[VATRegistered] = {
     data.soleProprietorOfAnotherBusiness match {
      case true => rp.vatRegistered
      case false => None
    }
  }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    import jto.validation.forms.Rules._
    implicit authContext =>
      implicit request =>

        getData[ResponsiblePeople](index) flatMap { rp =>
          Form2[SoleProprietorOfAnotherBusiness](request.body.asFormUrlEncoded.get ++ getExtradInfo(rp)) match {
            case f: InvalidForm => Future.successful(BadRequest(sole_proprietor(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp))))
            case ValidForm(_, data) => {
              for {
                result <- updateDataStrict[ResponsiblePeople](index) { rp =>
                  rp.copy(soleProprietorOfAnotherBusiness = Some(data), vatRegistered = getVatRegData(rp, data))
                }
              } yield (edit, data.soleProprietorOfAnotherBusiness) match {
                case (_, true) => Redirect(routes.VATRegisteredController.get(index, edit, fromDeclaration))
                case (true, false) => Redirect(routes.DetailedAnswersController.get(index))
                case (false, false) => Redirect(routes.RegisteredForSelfAssessmentController.get(index, edit, fromDeclaration))
              }
            }.recoverWith {
              case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
            }
          }
        }
  }
}