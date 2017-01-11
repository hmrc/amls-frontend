package utils

import config.ApplicationConfig
import connectors.DataCacheConnector
import models.aboutthebusiness.AboutTheBusiness
import models.responsiblepeople.ResponsiblePeople
import models.tradingpremises.TradingPremises
import org.joda.time.LocalDate
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DateOfChangeHelper {

  val dataCacheConnector: DataCacheConnector = DataCacheConnector

  def redirectToDateOfChange[A](a: Option[A], b: A) = ApplicationConfig.release7 && !a.contains(b)

  def compareToStartDate(implicit hc: HeaderCarrier,
                         authContext: AuthContext
                        ): Future[Option[LocalDate]] = {
    dataCacheConnector.fetchAll map { cacheMapO =>
      val startDates = (for {
        cacheMap <- cacheMapO
        aboutTheBusiness <- cacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key)
        tradingPremises <- cacheMap.getEntry[Seq[TradingPremises]](TradingPremises.key)
        responsiblePeople <- cacheMap.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
      } yield {
        val tpStartDates = tradingPremises.map{ tp =>
          tp.yourTradingPremises.map(yp => yp.startDate)
        }
        val rpStartDates = responsiblePeople.map{ rp =>
          rp.positions.flatMap(ps => ps.startDate)
        }
        val atbStartDate = aboutTheBusiness.activityStartDate.map{ activityStartDate =>
          activityStartDate.startDate
        }
        tpStartDates ++: rpStartDates :+ atbStartDate
      }).getOrElse(Seq.empty)

      startDates.sortWith{ (a,b) => (a,b) match {
        case (Some(x), Some(y)) => x.isAfter(y)
        case _ => false
      }}.head
    }
  }

}