package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.responsiblepeople.{ResponsiblePeople, Training}
import utils.{BooleanFormReadWrite, RepeatingSection}
import play.api.Logger

import scala.concurrent.Future

trait FitAndProperController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector
  val FIELDNAME = "hasAlreadyPassedFitAndProper"
  implicit val boolWrite = BooleanFormReadWrite.formWrites(FIELDNAME)
  implicit val boolRead = BooleanFormReadWrite.formRule(FIELDNAME)

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            response =>
              val form = (for {
                responsiblePeople <- response
                fitAndProper <- responsiblePeople.hasAlreadyPassedFitAndProper
              } yield Form2[Boolean](fitAndProper))
                .getOrElse(EmptyForm)

              Ok(views.html.responsiblepeople.fit_and_proper(form, edit, index))
          }
      }
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request => {
          Form2[Boolean](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(views.html.responsiblepeople.fit_and_proper(f, edit, index)))
            case ValidForm(_, data) =>
              for {
                _ <- updateData[ResponsiblePeople](index) {
                  case Some(rp) => Some(rp.hasAlreadyPassedFitAndProper(data))
                  case _ => Some(ResponsiblePeople(hasAlreadyPassedFitAndProper = Some(data)))
                }
              } yield {
                  edit match {
                    case false => Redirect (routes.PersonRegisteredController.get (index))
                    case true => Redirect (routes.DetailedAnswersController.get(index))
                  }
              }
          }
        }
      }
    }

}

object FitAndProperController extends FitAndProperController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
