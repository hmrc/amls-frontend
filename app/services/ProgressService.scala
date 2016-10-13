package services

import config.ApplicationConfig
import connectors.DataCacheConnector
import models.aboutthebusiness.AboutTheBusiness
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.{BusinessActivities => _, _}
import models.hvd.Hvd
import models.moneyservicebusiness.{MoneyServiceBusiness => Msb}
import models.estateagentbusiness.EstateAgentBusiness
import models.registrationprogress.{NotStarted, Section}
import models.responsiblepeople.ResponsiblePeople
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait ProgressService {

  private[services] def cacheConnector: DataCacheConnector

  private def dependentSections(implicit cache: CacheMap): Set[Section] =
    (for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      ba <- bm.activities
    } yield ba.businessActivities.foldLeft[Set[Section]](Set.empty) {
      (m, n) => n match {
        case AccountancyServices =>
          m + Asp.section + Supervision.section
        case EstateAgentBusinessService =>
          m + EstateAgentBusiness.section
        case HighValueDealing if ApplicationConfig.hvdToggle =>
          m + Hvd.section
        case MoneyServiceBusiness =>
          bm.msbServices.fold(m)(x => m + Msb.section)
        case TrustAndCompanyServices =>
          m + Tcsp.section + Supervision.section
        case _ => m
      }
        // TODO Error instead of empty map
    }) getOrElse Set.empty

  private def mandatorySections(implicit cache: CacheMap): Seq[Section] =
    Seq(
      BusinessMatching.section,
      AboutTheBusiness.section,
      BusinessActivities.section,
      BankDetails.section,
      TradingPremises.section
    )

  //TODO: Move this into mandatory sections once toggle is removed.
  private def responsiblePeople(implicit cache: CacheMap): Seq[Section] =
    ApplicationConfig.responsiblePeopleToggle match {
      case true =>
        Seq(ResponsiblePeople.section)
      case _ =>
        Seq.empty
    }

  def sections
  (implicit
   hc: HeaderCarrier,
   ac: AuthContext,
   ec: ExecutionContext
  ): Future[Seq[Section]] =
    cacheConnector.fetchAll map {
      optionCache =>
        optionCache map {
          cache =>
            sections(cache)
        } getOrElse Seq.empty
    }

  def sections(cache : CacheMap) : Seq[Section] = {
      mandatorySections(cache) ++
      responsiblePeople(cache) ++
      dependentSections(cache)
  }
}

object ProgressService extends ProgressService {
  override private[services] val cacheConnector = DataCacheConnector
}
