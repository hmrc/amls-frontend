package utils

import connectors.DataCacheConnector
import play.api.libs.json.Format
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait RepeatingSection {

  def dataCacheConnector: DataCacheConnector

  def getData[T]
  (cache: CacheMap, index: Int)
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: Format[T],
   key: MongoKey[T],
   ec: ExecutionContext
  ): Option[T] =
    getData[T](cache) match {
      case data if index > 0 && index <= data.length + 1 => data lift (index - 1)
      case _ => None
    }

  def getData[T]
  (cache: CacheMap)
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: Format[T],
   key: MongoKey[T],
   ec: ExecutionContext
  ): Seq[T] =
    cache.getEntry[Seq[T]](key())
      .fold(Seq.empty[T]) {
        identity
      }

  def updateData[T]
  (cache: CacheMap, index: Int)
  (fn: Option[T] => Option[T])
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: Format[T],
   key: MongoKey[T],
   ec: ExecutionContext
  ): Future[_] = {
    val data = getData[T](cache)
    putData(data.patch(index - 1, fn(data.lift(index - 1)).toSeq, 1))
  }

  def getData[T]
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: Format[T],
   key: MongoKey[T],
   ec: ExecutionContext
  ): Future[Seq[T]] = {
    dataCacheConnector.fetch[Seq[T]](key()) map {
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
   formats: Format[T],
   key: MongoKey[T],
   ec: ExecutionContext
  ): Future[Option[T]] = {
    getData[T] map {
      case data if index > 0 && index <= data.length + 1 => data lift (index - 1)
      case _ => None
    }
  }

  protected def updateData[T]
  (index: Int)
  (fn: Option[T] => Option[T])
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: Format[T],
   key: MongoKey[T],
   ec: ExecutionContext
  ): Future[_] =
    getData[T] map {
      data => {
        putData(data.patch(index - 1, fn(data.lift(index - 1)).toSeq, 1))
      }
    }

  protected def putData[T]
  (data: Seq[T])
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: Format[T],
   key: MongoKey[T],
   ec: ExecutionContext
  ): Future[_] =
    dataCacheConnector.save[Seq[T]](key(), data)
}

