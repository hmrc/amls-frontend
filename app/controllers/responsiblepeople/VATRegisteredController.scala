package controllers.responsiblepeople

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.{ResponsiblePeople, SoleProprietorOfAnotherBusiness, VATRegistered}
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople._

import scala.concurrent.Future

trait VATRegisteredController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext =>
        implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, Some(vat), _, _, _, _, _, _,_, _)) => Ok(vat_registered(Form2[VATRegistered](vat),
              edit, index, fromDeclaration, personName.titleName))
            case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _,_)) => Ok(vat_registered(EmptyForm,
              edit, index, fromDeclaration, personName.titleName))
            case _ => NotFound(notFoundView)
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

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) flatMap { rp =>
            Form2[VATRegistered](request.body.asFormUrlEncoded.get ++ getExtradInfo(rp)) match {
              case f: InvalidForm =>
                  Future.successful(BadRequest(vat_registered(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp))))
              case ValidForm(_, data) => {
                for {
                  _ <- updateDataStrict[ResponsiblePeople](index) { rp =>
                    rp.vatRegistered(data)
                  }
                } yield edit match {
                  case true => Redirect(routes.DetailedAnswersController.get(index))
                  case false => Redirect(routes.RegisteredForSelfAssessmentController.get(index, edit, fromDeclaration))
                }
              }.recoverWith {
                case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
              }
            }
          }
    }
}

object VATRegisteredController extends VATRegisteredController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
