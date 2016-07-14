package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessactivities.{BusinessActivities, _}
import models.businessmatching.BusinessMatching
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessactivities._

import scala.concurrent.Future

trait InvolvedInOtherController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchAll map {
        optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
          } yield {
            (for {
              businessActivities <- cache.getEntry[BusinessActivities](BusinessActivities.key)
              involvedInOther <- businessActivities.involvedInOther
            } yield Ok(involved_in_other_name(Form2[InvolvedInOther](involvedInOther), edit, businessMatching)))
              .getOrElse (Ok(involved_in_other_name(EmptyForm, edit, businessMatching)))
          }) getOrElse Ok(involved_in_other_name(EmptyForm, edit, None))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[InvolvedInOther](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(involved_in_other_name(f, edit, None)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key, getUpdatedBA(businessActivities, data))

          } yield edit match {
            case true => data match {
              case InvolvedInOtherYes(_) => Redirect(routes.ExpectedBusinessTurnoverController.get(edit))
              case InvolvedInOtherNo => Redirect(routes.ExpectedAMLSTurnoverController.get(edit))
            }
            case false => data match {
              case InvolvedInOtherYes(_) => Redirect(routes.ExpectedBusinessTurnoverController.get())
              case InvolvedInOtherNo => Redirect(routes.ExpectedAMLSTurnoverController.get())
            }
          }
      }
    }
  }

  private def getUpdatedBA(businessActivities: Option[BusinessActivities], data: InvolvedInOther): BusinessActivities = {
    (businessActivities, data) match {
      case (Some(ba), InvolvedInOtherYes(_)) => ba.copy(involvedInOther = Some(data))
      case (Some(ba), InvolvedInOtherNo) => ba.copy(involvedInOther = Some(data), expectedBusinessTurnover = None)
      case (_, _) => BusinessActivities(involvedInOther= Some(data))
    }
  }
}


object InvolvedInOtherController extends InvolvedInOtherController {
  // $COVERAGE-OFF$
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}
