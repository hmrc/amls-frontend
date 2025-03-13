/*
 * Copyright 2024 HM Revenue & Customs
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
import services.cache.Cache
import typeclasses.MongoKey

import scala.concurrent.{ExecutionContext, Future}

// $COVERAGE-OFF$
// Coverage has been turned off for these types until we remove the deprecated methods
trait RepeatingSection {

  def dataCacheConnector: DataCacheConnector

  def getData[T](cache: Cache, index: Int)(implicit formats: Format[T], key: MongoKey[T]): Option[T] =
    getData[T](cache) match {
      case data if index > 0 && index <= data.length + 1 => data lift (index - 1)
      case _                                             => None
    }

  def getData[T](credId: String, index: Int)(implicit
    formats: Format[T],
    key: MongoKey[T],
    ec: ExecutionContext
  ): Future[Option[T]] =
    getData[T](credId) map {
      case data if index > 0 && index <= data.length + 1 => data lift (index - 1)
      case _                                             => None
    }

  def getData[T](cache: Cache)(implicit formats: Format[T], key: MongoKey[T]): Seq[T] =
    cache.getEntry[Seq[T]](key()).fold(Seq.empty[T])(identity)

  def getData[T](credId: String)(implicit formats: Format[T], key: MongoKey[T], ec: ExecutionContext): Future[Seq[T]] =
    dataCacheConnector.fetch[Seq[T]](credId, key()) map { _.fold(Seq.empty[T])(identity) }

  def addData[T](credId: String, data: T)(implicit
    formats: Format[T],
    key: MongoKey[T],
    ec: ExecutionContext
  ): Future[Int] =
    getData[T](credId).flatMap { d =>
      if (!d.lastOption.contains(data)) {
        putData(credId, d :+ data) map { _ =>
          d.size + 1
        }
      } else {
        Future.successful(d.size)
      }
    }

  def fetchAllAndUpdateStrict[T](credId: String, index: Int)(
    fn: (Cache, T) => T
  )(implicit formats: Format[T], key: MongoKey[T], ec: ExecutionContext): Future[Option[Cache]] =
    dataCacheConnector.fetchAll(credId).flatMap {
      _.map { cacheMap =>
        cacheMap
          .getEntry[Seq[T]](key())
          .map { data =>
            putData(credId, data.patch(index - 1, Seq(fn(cacheMap, data(index - 1))), 1))
              .map(_ => Some(cacheMap))
          }
          .getOrElse(Future.successful(Some(cacheMap)))
      }.getOrElse(Future.successful(None))
    }

  protected def updateDataStrict[T](credId: String, index: Int)(
    fn: T => T
  )(implicit formats: Format[T], key: MongoKey[T], ec: ExecutionContext): Future[Cache] =
    getData[T](credId) flatMap { data =>
      putData(credId, data.patch(index - 1, Seq(fn(data(index - 1))), 1))
    }

  protected def removeDataStrict[T](credId: String, index: Int)(implicit
    formats: Format[T],
    key: MongoKey[T],
    ec: ExecutionContext
  ): Future[Cache] =
    getData(credId) flatMap { data =>
      putData(credId, data.patch(index - 1, Nil, 1))
    }

  protected def putData[T](credId: String, data: Seq[T])(implicit formats: Format[T], key: MongoKey[T]): Future[Cache] =
    dataCacheConnector.save[Seq[T]](credId, key(), data)

}
