package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.responsiblepeople.{ExperienceTraining, ResponsiblePeople}
import play.api.Logger
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.RepeatingSection

import scala.concurrent.Future

trait ExperienceTrainingController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  private def businessActivitiesData(implicit ac: AuthContext, hc: HeaderCarrier): Future[BusinessActivities] = {
    dataCacheConnector.fetchAll map {
      cache =>
        Logger.debug(cache.toString)
        (for {
          c <- cache
          businessMatching <- {
            val a = c.getEntry[BusinessMatching](BusinessMatching.key)
            Logger.debug(a.toString)
            a
          }
          activities <- businessMatching.activities
        } yield activities).getOrElse(BusinessActivities(Set.empty))
    }
  }

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          businessActivitiesData flatMap {
            activities =>
              getData[ResponsiblePeople](index) map {
                response =>
                  val form = (for {
                    responsiblePeople <- response
                    experienceTraining <- responsiblePeople.experienceTraining
                  } yield Form2[ExperienceTraining](experienceTraining)).getOrElse(EmptyForm)
                  Ok(views.html.responsiblepeople.experience_training(form, activities, edit, index))
              }
          }
      }
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request => {
          businessActivitiesData flatMap {
            activities =>
              Form2[ExperienceTraining](request.body) match {
                case f: InvalidForm =>
                  Future.successful(BadRequest(views.html.responsiblepeople.experience_training(f, activities, edit, index)))
                case ValidForm(_, data) =>
                  for {
                    _ <- updateData[ResponsiblePeople](index) {
                      case Some(rp) => Some(rp.experienceTraining(data))
                      case _ => Some(ResponsiblePeople(experienceTraining = Some(data)))
                    }
                  } yield edit match {
                    case false => Redirect(routes.TrainingController.get(index, edit))
                    case true => Redirect(routes.DetailedAnswersController.get(index))
                  }
              }
          }
        }
      }
    }

}

object ExperienceTrainingController extends ExperienceTrainingController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
