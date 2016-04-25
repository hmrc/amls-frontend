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
            response =>
              val form = (for {
                resp <- response
                vat <- resp.vatRegistered
              } yield Form2[VATRegistered](vat)).getOrElse(EmptyForm)
              Ok(vat_registered(form, edit, index))
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
            case ValidForm(_, data) =>
              for {
                _ <- updateData[ResponsiblePeople](index) {
                  case Some(rp) => Some(rp.vatRegistered(data))
                  case _ => Some(ResponsiblePeople(vatRegistered = Some(data)))
                }
              } yield edit match {
                case false => Redirect(routes.RegisteredForSelfAssessmentController.get(index, edit))
                case true  => Redirect(routes.DetailedAnswersController.get(index))
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
