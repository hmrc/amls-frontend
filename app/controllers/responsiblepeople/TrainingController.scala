package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.responsiblepeople.{ResponsiblePeople, Training}
import utils.RepeatingSection

import scala.concurrent.Future

trait TrainingController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            response =>
              val form = (for {
                responsiblePeople <- response
                training <- responsiblePeople.training
              } yield Form2[Training](training)).getOrElse(EmptyForm)
              Ok(views.html.responsiblepeople.training(form, edit, index))
          }
      }
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request => {
          Form2[Training](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(views.html.responsiblepeople.training(f, edit, index)))
            case ValidForm(_, data) =>
              for {
                _ <- updateData[ResponsiblePeople](index) {
                  case Some(rp) => Some(rp.training(data))
                  case _ => Some(ResponsiblePeople(training = Some(data)))
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

object TrainingController extends TrainingController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
