package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.tradingpremises.routes
import forms._
import models.businessmatching.{BusinessActivities, BusinessActivity, BusinessMatching}
import models.responsiblepeople.{ExperienceTraining, ResponsiblePeople}
import play.api.mvc.Result
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.RepeatingSection

import scala.concurrent.Future
import scala.util.Left

trait ExperienceTrainingController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  private def badata(index: Int, edit: Boolean)(implicit ac: AuthContext, hc: HeaderCarrier)
  : Future[Either[Result, (CacheMap, Set[BusinessActivity])]] = {
    dataCacheConnector.fetchAll map {
      cache =>
        type Tupe = (CacheMap, Set[BusinessActivity])
        (for {
          c <- cache
          bm <- c.getEntry[BusinessMatching](BusinessMatching.key)
          activities <- bm.activities flatMap {
            _.businessActivities match {
              case set if set.isEmpty => None
              case set => Some(set)
            }
          }
        } yield (c, activities))
          .fold[Either[Result, Tupe]] {
          // TODO: Need to think about what we should do in case of this error
          Left(Redirect(routes.ExperienceTrainingController.get(index, edit)))
        } {
          t => Right(t)
        }
    }
  }


  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>

          badata(index, edit) flatMap {

            case Right((c, activities)) =>
              val ba = BusinessActivities(activities)
              getData[ResponsiblePeople](index) map {
                response =>
                  val form = (for {
                    responsiblePeople <- response
                    experienceTraining <- responsiblePeople.experienceTraining
                  } yield Form2[ExperienceTraining](experienceTraining)).getOrElse(EmptyForm)
                  Ok(views.html.responsiblepeople.experience_training(form, ba, edit, index))
              }

            case Left(result) => Future.successful(result)
          }

      }
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request => {

          badata(index, edit) flatMap {
            case Right((c, activities)) =>
              val ba = BusinessActivities(activities)

              Form2[ExperienceTraining](request.body) match {
                case f: InvalidForm =>
                  Future.successful(BadRequest(views.html.responsiblepeople.experience_training(f, ba, edit, index)))
                case ValidForm(_, data) =>
                  for {
                    _ <- updateData[ResponsiblePeople](index) {
                      case Some(rp) => Some(rp.experienceTraining(data))
                      case _ => Some(ResponsiblePeople(experienceTraining = Some(data)))
                    }
                  } yield {
                    Redirect(routes.ExperienceTrainingController.get(index, edit))
                  }
              }
            case Left(result) => Future.successful(result)
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
