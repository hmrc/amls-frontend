/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import connectors.DataCacheConnector
import play.api.libs.json.Format
import typeclasses.MongoKey
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

trait RepeatingSection {

  def dataCacheConnector: DataCacheConnector

  def getData[T](cache: CacheMap, index: Int)
                (implicit user: AuthContext, hc: HeaderCarrier, formats: Format[T], key: MongoKey[T], ec: ExecutionContext): Option[T] =
    getData[T](cache) match {
      case data if index > 0 && index <= data.length + 1 => data lift (index - 1)
      case _ => None
    }

  def getData[T](index: Int)
                (implicit user: AuthContext, hc: HeaderCarrier, formats: Format[T], key: MongoKey[T], ec: ExecutionContext): Future[Option[T]] = {
    getData[T] map {
      case data if index > 0 && index <= data.length + 1 => data lift (index - 1)
      case _ => None
    }
  }

  def getData[T](cache: CacheMap)
                (implicit user: AuthContext, hc: HeaderCarrier, formats: Format[T], key: MongoKey[T], ec: ExecutionContext): Seq[T] =
    cache.getEntry[Seq[T]](key()).fold(Seq.empty[T])(identity)

  def getData[T](implicit user: AuthContext, hc: HeaderCarrier, formats: Format[T], key: MongoKey[T], ec: ExecutionContext): Future[Seq[T]] = {
    dataCacheConnector.fetch[Seq[T]](key()) map { _.fold(Seq.empty[T])(identity) }
  }

  def addData[T](data: T)(implicit user: AuthContext, hc: HeaderCarrier, formats: Format[T], key: MongoKey[T], ec: ExecutionContext): Future[Int] = {
    getData[T].flatMap { d =>
      if (!d.lastOption.contains(data)) {
        putData(d :+ data) map {
          _ => d.size + 1
        }
      } else {
        Future.successful(d.size)
      }
    }
  }

  def fetchAllAndUpdateStrict[T](index: Int)(fn: (CacheMap, T) => T)
                                (implicit user: AuthContext, hc: HeaderCarrier, formats: Format[T], key: MongoKey[T], ec: ExecutionContext): Future[Option[CacheMap]] = {
    dataCacheConnector.fetchAll.flatMap {
      _.map {
        cacheMap =>
          cacheMap.getEntry[Seq[T]](key()).map {
            data =>
              putData(data.patch(index - 1, Seq(fn(cacheMap, data(index - 1))), 1))
                .map(_ => Some(cacheMap))
          }.getOrElse(Future.successful(Some(cacheMap)))
      }.getOrElse(Future.successful(None))
    }
  }

  protected def updateDataStrict[T](index: Int)(fn: T => T)
                                   (implicit user: AuthContext, hc: HeaderCarrier, formats: Format[T], key: MongoKey[T], ec: ExecutionContext): Future[CacheMap] =
    getData[T] flatMap {
      data => {
        putData(data.patch(index - 1, Seq(fn(data(index - 1))), 1))
      }
    }

  protected def removeDataStrict[T](index: Int)
                                   (implicit user: AuthContext, hc: HeaderCarrier, formats: Format[T], key: MongoKey[T], ec: ExecutionContext): Future[CacheMap] =
    getData[T] flatMap {
      data => {
        putData(data.patch(index - 1, Nil, 1))
      }
    }

  protected def putData[T](data: Seq[T])
                          (implicit user: AuthContext, hc: HeaderCarrier, formats: Format[T], key: MongoKey[T], ec: ExecutionContext): Future[CacheMap] =
    dataCacheConnector.save[Seq[T]](key(), data)

}

