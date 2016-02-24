package utils

import connectors.DataCacheConnector
import play.api.libs.json
import typeclasses.MongoKey
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait RepeatingSection {

  val dataCacheConnector: DataCacheConnector

  def getData[T]
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: json.Format[T],
   key: MongoKey[T],
   ec :ExecutionContext
  ): Future[Seq[T]] = {
    dataCacheConnector.fetchDataShortLivedCache[Seq[T]](key()) map {
      _.fold(Seq.empty[T]) {
        identity
      }
    }
  }

  def getData[T]
  (index: Int)
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: json.Format[T],
   key: MongoKey[T],
   ec :ExecutionContext
  ): Future[Option[T]] = {
    getData[T] map {
      case accounts if index > 0 && index <= accounts.length + 1 => accounts lift (index - 1)
      case _ => None
    }
  }

  protected def updateData[T]
  (index: Int)
  (fn: Option[T] => Option[T])
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: json.Format[T],
   key: MongoKey[T],
   ec :ExecutionContext
  ): Future[_] =
    getData[T] map {
      accounts => {
        putData(accounts.patch(index - 1, fn(accounts.lift(index - 1)).toSeq, 1))
      }
    }

  protected def putData[T]
  (accounts: Seq[T])
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: json.Format[T],
   key: MongoKey[T],
   ec :ExecutionContext
  ): Future[_] =
    dataCacheConnector.saveDataShortLivedCache[Seq[T]](key(), accounts)
}

