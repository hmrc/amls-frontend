package controllers.responsiblepeople

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.{VATRegistered, ResponsiblePeople}
import utils.RepeatingSection
import views.html.responsiblepeople._

import scala.concurrent.Future

trait VATRegisteredController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(_,_,_,_,_,_,Some(vat),_,_,_,_,_,_)) => Ok(vat_registered(Form2[VATRegistered](vat), edit,index))
            case Some(ResponsiblePeople(_,_,_,_,_,_,_,_,_,_,_,_,_)) => Ok(vat_registered(EmptyForm, edit,index))
            case _ => NotFound(notFoundView)
          }
      }
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          Form2[VATRegistered](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(vat_registered(f, edit, index)))
            case ValidForm(_, data) => {
              for {
                _ <- updateDataStrict[ResponsiblePeople](index) { rp =>
                  rp.vatRegistered(data)
                }
              } yield Redirect(routes.RegisteredForSelfAssessmentController.get(index, edit))
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
