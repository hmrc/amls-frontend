package controllers

import config.{AMLSAuthConnector, AmlsShortLivedCache, ApplicationConfig}
import models.{AmendVariationResponse, SubscriptionResponse}
import models.aboutthebusiness.AboutTheBusiness
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.BusinessMatching
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.responsiblepeople.ResponsiblePeople
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import play.api.mvc.{Call, Request}
import services.{AuthEnrolmentsService, LandingService}
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait LandingController extends BaseController {

  private[controllers] def landingService: LandingService

  private[controllers] def enrolmentsService: AuthEnrolmentsService

  val shortLivedCache: ShortLivedCache = AmlsShortLivedCache

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      enrolmentsService.amlsRegistrationNumber flatMap {
        case Some(amlsRegistrationNumber) => landingService.cacheMap flatMap {
          case Some(cacheMap) => {
            if (dataHasChanged(cacheMap)) {
              (cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key), cacheMap.getEntry[AmendVariationResponse](AmendVariationResponse.key)) match {
                case (Some(_), _) => refreshAndRedirect(amlsRegistrationNumber)
                case (_, Some(_)) => refreshAndRedirect(amlsRegistrationNumber)
                case _ => Future.successful(Redirect(controllers.routes.StatusController.get()))
              }
            } else {
              refreshAndRedirect(amlsRegistrationNumber)
            }
          }
          case _ => refreshAndRedirect(amlsRegistrationNumber)
        }
        case _ => getPreSubmission
      }
  }

  def getPreSubmission(implicit authContext: AuthContext, request: Request[_]) = {
    landingService.cacheMap flatMap {
      case Some(cache) => preApplicationComplete(cache)
      case None => landingService.reviewDetails flatMap {
        case Some(rd) =>
          landingService.updateReviewDetails(rd) map {
            _ => Redirect(controllers.businessmatching.routes.BusinessTypeController.get())
          }
        case None =>
          Future.successful(Redirect(Call("GET", ApplicationConfig.businessCustomerUrl)))
      }
    }
  }

  private def preApplicationComplete(cache: CacheMap)(implicit authContext: AuthContext, headerCarrier: HeaderCarrier) = {
    (for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
    } yield bm.isComplete match {
      case (true) => Future.successful(Redirect(controllers.routes.StatusController.get()))
      case _ => {
        shortLivedCache.remove(authContext.user.oid).map { http =>
          http.status match {
            case NO_CONTENT => Redirect(controllers.routes.LandingController.get())
            case _ => throw new Exception("Cannot remove pre application data")
          }
        }
      }
    }).getOrElse(Future.successful(Redirect(controllers.routes.LandingController.get())))
  }

  private def refreshAndRedirect(amlsRegistrationNumber: String)(implicit authContext: AuthContext, headerCarrier: HeaderCarrier) = {
    landingService.refreshCache(amlsRegistrationNumber) map {
      cache => Redirect(controllers.routes.StatusController.get())
    }
  }


  private def dataHasChanged(cacheMap: CacheMap) = {
    Seq(
      cacheMap.getEntry[Asp](Asp.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[Seq[BankDetails]](BankDetails.key).fold(false) {
        _.exists(_.hasChanged)
      },
      cacheMap.getEntry[BusinessActivities](BusinessActivities.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[BusinessMatching](BusinessMatching.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[EstateAgentBusiness](EstateAgentBusiness.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key).fold(false) {
        _.exists(_.hasChanged)
      },
      cacheMap.getEntry[Supervision](Supervision.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[Tcsp](Tcsp.key).fold(false) {
        _.hasChanged
      },
      cacheMap.getEntry[Seq[TradingPremises]](TradingPremises.key).fold(false) {
        _.exists(_.hasChanged)
      },
      cacheMap.getEntry[Hvd](Hvd.key).fold(false) {
        _.hasChanged
      }
    ).exists(identity)
  }

}

object LandingController extends LandingController {
  // $COVERAGE-OFF$
  override private[controllers] val landingService = LandingService
  override private[controllers] val enrolmentsService = AuthEnrolmentsService
  override protected val authConnector = AMLSAuthConnector
  override val shortLivedCache: ShortLivedCache = AmlsShortLivedCache
}
