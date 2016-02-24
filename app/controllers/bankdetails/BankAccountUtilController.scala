package controllers.bankdetails

import connectors.DataCacheConnector
import controllers.BaseController
import models.bankdetails.BankDetails
import play.api.libs.json
import typeclasses.MongoKey
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait BankAccountUtilController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def getBankDetails[T]
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: json.Format[T],
   key: MongoKey[T]
  ): Future[Seq[T]] = {
    dataCacheConnector.fetchDataShortLivedCache[Seq[T]](key()) map {
      _.fold(Seq.empty[T]) {
        identity
      }
    }
  }

  def getBankDetails[T]
  (index: Int)
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: json.Format[T],
   key: MongoKey[T]
  ): Future[Option[T]] = {
    getBankDetails[T] map {
      case accounts if index > 0 && index <= accounts.length + 1 => accounts lift (index - 1)
      case _ => None
    }
  }

  protected def updateBankDetails[T]
  (index: Int)
  (fn: Option[T] => Option[T])
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: json.Format[T],
   key: MongoKey[T]
  ): Future[_] =
    getBankDetails[T] map {
      accounts => {
        putBankDetails(accounts.patch(index - 1, fn(accounts.lift(index - 1)).toSeq, 1))
      }
    }

  protected def putBankDetails[T](accounts: Seq[T])
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: json.Format[T],
   key: MongoKey[T]
  ): Future[_] =
    dataCacheConnector.saveDataShortLivedCache[Seq[T]](key(), accounts)
}

