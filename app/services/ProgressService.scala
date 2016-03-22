package services

import connectors.DataCacheConnector
import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.{BusinessActivities => _, _}
import models.estateagentbusiness.EstateAgentBusiness
import models.registrationprogress.{IsComplete, Section}
import models.tradingpremises.TradingPremises
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait ProgressService {

  private[services] def cacheConnector: DataCacheConnector

  private def isComplete(key: String)(implicit cache: CacheMap): Boolean =
    cache.getEntry[IsComplete](key).fold(false) { _.isComplete }

  private def dependentSections(implicit cache: CacheMap): Seq[Section] =
    (for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      ba <- bm.activities
    } yield ba.businessActivities.foldLeft[Seq[Section]](Seq.empty) {
      (m, n) => n match {
        case AccountancyServices =>
          // TODO
          m :+ Section("asp", false, controllers.routes.RegistrationProgressController.get())
        case EstateAgentBusinessService =>
          m :+ EstateAgentBusiness.section
        case HighValueDealing =>
          // TODO
          m :+ Section("hvd", false, controllers.routes.RegistrationProgressController.get())
        case MoneyServiceBusiness =>
          // TODO
          m :+ Section("msb", false, controllers.routes.RegistrationProgressController.get())
        case TrustAndCompanyServices =>
          // TODO
          m :+ Section("tcsp", false, controllers.routes.RegistrationProgressController.get())
        case _ => m
      }
        // TODO Error instead of empty map
    }) getOrElse Seq.empty

  def sections
  (implicit
   hc: HeaderCarrier,
   ac: AuthContext,
   ec: ExecutionContext
  ): Future[Seq[Section]] =
    cacheConnector.fetchAll map {
      optionCache =>
        optionCache map {
          implicit cache =>
            Seq(
              BusinessMatching.section,
              AboutTheBusiness.section,
              BusinessActivities.section,
              BankDetails.section,
              TradingPremises.section
            ) ++ dependentSections
        } getOrElse Seq.empty
    }
}

object ProgressService extends ProgressService {
  override private[services] val cacheConnector = DataCacheConnector
}