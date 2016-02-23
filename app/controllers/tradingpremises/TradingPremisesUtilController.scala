package controllers.tradingpremises

import connectors.DataCacheConnector
import controllers.BaseController
import models.tradingpremises.TradingPremises
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait TradingPremisesUtilController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def getPremisesDetails(index: Int)(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[TradingPremises]] = {
    getPremisesDetails map {
      case premises if index > 0 && index <= premises.length + 1 => premises lift (index - 1)
      case _ => None
    }
  }

  private def getPremisesDetails(implicit user: AuthContext, hc: HeaderCarrier): Future[Seq[TradingPremises]] = {
    dataCacheConnector.fetchDataShortLivedCache[Seq[TradingPremises]](TradingPremises.key) map {
      _.fold(Seq.empty[TradingPremises]) {
        identity
      }
    }
  }

  protected def updatePremisesDetails(index: Int, acc: TradingPremises)
                                     (implicit user: AuthContext, hc: HeaderCarrier): Future[_] =
    updatePremisesDetails(index, Seq(acc))

  private def updatePremisesDetails(index: Int, value: Seq[TradingPremises])
                                   (implicit user: AuthContext, hc: HeaderCarrier): Future[_] =
    getPremisesDetails map { premises =>
      putPremisesDetails(premises.patch(index - 1, value, 1))
    }

  private def putPremisesDetails(premises: Seq[TradingPremises])
                                (implicit user: AuthContext, hc: HeaderCarrier): Future[_] =
    dataCacheConnector.saveDataShortLivedCache[Seq[TradingPremises]](TradingPremises.key, premises)

}

