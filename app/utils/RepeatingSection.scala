package utils

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader
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

  def getData[T]
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: Format[T],
   key: MongoKey[T],
   ec: ExecutionContext
  ): Future[Seq[T]] = {
    dataCacheConnector.fetch[Seq[T]](key()) map { x =>
      x.fold(Seq.empty[T]) {
        identity
      }
    }
  }

  def addData[T](data : T)
    (implicit
     user: AuthContext,
     hc: HeaderCarrier,
     formats: Format[T],
     key: MongoKey[T],
     ec: ExecutionContext): Future[Int] = {
    getData[T].flatMap { d =>
      if (!d.lastOption.contains(Some(data))) {
        putData(d :+ data) map {
          _ => d.size + 1
        }
      } else {Future.successful(d.size)}
    }
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

  protected def fetchAllAndUpdate[T]
  (index: Int)
  (fn: (CacheMap, Option[T]) => Option[T])
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: Format[T],
   key: MongoKey[T],
   ec: ExecutionContext
  ) : Future[Option[CacheMap]] = {
    dataCacheConnector.fetchAll.map[Option[CacheMap]] {
      optionalCacheMap => optionalCacheMap.map[CacheMap] {
        cacheMap => {
            cacheMap.getEntry[Seq[T]](key()).map {
              data => putData(data.patch(index-1, fn(cacheMap, data.lift(index-1)).toSeq, 1))
            }
            cacheMap
          }
        }
      }
    }

  def fetchAllAndUpdateStrict[T]
  (index: Int)
  (fn: (CacheMap, T) => T)
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: Format[T],
   key: MongoKey[T],
   ec: ExecutionContext
  ) : Future[Option[CacheMap]] = {
    dataCacheConnector.fetchAll.map[Option[CacheMap]] {
      optionalCacheMap => optionalCacheMap.map[CacheMap] {
        cacheMap => {
          cacheMap.getEntry[Seq[T]](key()).map {
            data => {
              putData(data.patch(index - 1, Seq(fn(cacheMap, data(index - 1))), 1))
            }
          }
          cacheMap
        }
      }
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

  protected def updateDataStrict[T]
  (index: Int)
  (fn: T => T)
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: Format[T],
   key: MongoKey[T],
   ec: ExecutionContext
  ): Future[_] =
    getData[T] flatMap {
      data => {
        putData(data.patch(index - 1, Seq(fn(data(index - 1))), 1))
      }
    }

  protected def removeDataStrict[T]
  (index: Int)
  (implicit
   user: AuthContext,
   hc: HeaderCarrier,
   formats: Format[T],
   key: MongoKey[T],
   ec: ExecutionContext
  ): Future[_] =
    getData[T] map {
      data => {
        putData(data.patch(index - 1, Nil, 1))
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
  ): Future[_] ={
    dataCacheConnector.save[Seq[T]](key(), data)}
}

