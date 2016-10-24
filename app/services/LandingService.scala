package services

import connectors.{AmlsConnector, DataCacheConnector, KeystoreConnector}
import models.ViewResponse
import models.aboutthebusiness.AboutTheBusiness
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.responsiblepeople.ResponsiblePeople
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import play.api.libs.json.{Format, Writes}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait LandingService {

  private[services] def cacheConnector: DataCacheConnector
  private[services] def keyStore: KeystoreConnector
  private[services] def desConnector: AmlsConnector

  @deprecated("fetch the cacheMap itself instead", "")
  def hasSavedForm
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext,
   ac: AuthContext
  ): Future[Boolean] =
    cacheConnector.fetchAll map {
      case Some(_) => true
      case None => false
    }

  def cacheMap
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext,
   ac: AuthContext
  ): Future[Option[CacheMap]] =
    cacheConnector.fetchAll

  def refreshCache(amlsRefNumber: String)
                  (implicit
                   authContext: AuthContext,
                   hc: HeaderCarrier,
                   ec: ExecutionContext
  ): Future[CacheMap] = {
    desConnector.view(amlsRefNumber) flatMap { viewResponse =>
      cacheConnector.save[BusinessMatching](BusinessMatching.key, viewResponse.businessMatchingSection) flatMap {
        _ => cacheConnector.save[Option[EstateAgentBusiness]](EstateAgentBusiness.key, viewResponse.eabSection) flatMap {
          _ => cacheConnector.save[Option[Seq[TradingPremises]]](TradingPremises.key, viewResponse.tradingPremisesSection) flatMap {
            _ => cacheConnector.save[AboutTheBusiness](AboutTheBusiness.key, viewResponse.aboutTheBusinessSection) flatMap {
              _ => cacheConnector.save[Seq[BankDetails]](BankDetails.key, viewResponse.bankDetailsSection) flatMap {
                _ => cacheConnector.save[AddPerson](AddPerson.key, viewResponse.aboutYouSection) flatMap {
                  _ => cacheConnector.save[BusinessActivities](BusinessActivities.key, viewResponse.businessActivitiesSection) flatMap {
                    _ => cacheConnector.save[Option[Seq[ResponsiblePeople]]](ResponsiblePeople.key, viewResponse.responsiblePeopleSection) flatMap {
                      _ => cacheConnector.save[Option[Tcsp]](Tcsp.key, viewResponse.tcspSection) flatMap {
                        _ => cacheConnector.save[Option[Asp]](Asp.key, viewResponse.aspSection) flatMap {
                          _ => cacheConnector.save[Option[MoneyServiceBusiness]](MoneyServiceBusiness.key, viewResponse.msbSection) flatMap {
                            _ => cacheConnector.save[Option[Hvd]](Hvd.key, viewResponse.hvdSection) flatMap {
                              _ => cacheConnector.save[Option[Supervision]](Supervision.key, viewResponse.supervisionSection)
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  def reviewDetails
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext
  ): Future[Option[ReviewDetails]] =
    keyStore.optionalReviewDetails

  /* TODO: Consider if there's a good way to stop
   * this from just overwriting whatever is in Business Matching,
   * shouldn't be a problem as this should only happen when someone
   * first comes into the Application from Business Customer FE
   */
  def updateReviewDetails
  (reviewDetails: ReviewDetails)
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext,
   ac: AuthContext
  ): Future[CacheMap] = {
    val bm = BusinessMatching(reviewDetails = Some(reviewDetails))
    val atb = AboutTheBusiness(registeredOffice = Some(reviewDetails.businessAddress))
    cacheConnector.save[BusinessMatching](BusinessMatching.key, bm) flatMap {
      _ => cacheConnector.save[AboutTheBusiness](AboutTheBusiness.key, atb)
    }
  }
}

object LandingService extends LandingService {
  // $COVERAGE-OFF$
  override private[services] def cacheConnector = DataCacheConnector
  override private[services] def keyStore = KeystoreConnector
  override private[services] def desConnector = AmlsConnector
  // $COVERAGE-ON$
}
