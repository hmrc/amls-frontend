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
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.experience_training

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

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        businessActivitiesData flatMap {
          activities =>
            getData[ResponsiblePeople](index) map {
              case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, Some(experienceTraining), _, _, _, _, _,_, _))
              => Ok(experience_training(Form2[ExperienceTraining](experienceTraining), activities, edit, index, fromDeclaration, personName.titleName))
              case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _,_))
              => Ok(experience_training(EmptyForm, activities, edit, index, fromDeclaration, personName.titleName))
              case _
              => NotFound(notFoundView)
            }
        }
    }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request => {
        businessActivitiesData flatMap {
          activities =>
            Form2[ExperienceTraining](request.body) match {
              case f: InvalidForm =>
                getData[ResponsiblePeople](index) map {rp =>
                  BadRequest(views.html.responsiblepeople.experience_training(f, activities, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
                }
              case ValidForm(_, data) => {
                for {
                  result <- updateDataStrict[ResponsiblePeople](index) { rp =>
                    rp.experienceTraining(data)
                  }
                } yield edit match {
                  case true => Redirect(routes.DetailedAnswersController.get(index))
                  case false => Redirect(routes.TrainingController.get(index, edit, fromDeclaration))
                }
              }.recoverWith {
                case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
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
