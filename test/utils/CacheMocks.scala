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
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

trait CacheMocks extends MockitoSugar {

  implicit val mockCacheConnector = mock[DataCacheConnector]
  implicit val mockCacheMap = mock[CacheMap]

  mockCacheFetchAll

  def mockCacheFetch[T](item: Option[T], key: Option[String] = None)(implicit cache: DataCacheConnector) = key match {
    case Some(k) => when {
      cache.fetch[T](any(), eqTo(k))(any(), any())
    } thenReturn Future.successful(item)

    case _ => when {
      cache.fetch[T](any(), any())(any(), any())
    } thenReturn Future.successful(item)
  }

  def mockCacheGetEntry[T](item: Option[T], key: String)(implicit cache: CacheMap) = when {
    mockCacheMap.getEntry[T](eqTo(key))(any())
  } thenReturn item

  def mockCacheFetchAll(implicit cache: DataCacheConnector) = when {
    cache.fetchAll(any())(any[HeaderCarrier])
  } thenReturn Future.successful(Some(mockCacheMap))

  def mockCacheSave[T](implicit cache: DataCacheConnector) = when {
    cache.save[T](any(), any(), any())(any[HeaderCarrier], any())
  } thenReturn Future.successful(mockCacheMap)

  def mockCacheRemoveByKey[T](implicit cache: DataCacheConnector) = when {
    cache.removeByKey[T](any(), any())(any(), any())
  } thenReturn Future.successful(mockCacheMap)

  def mockCacheSave[T](item: T, key: Option[String] = None)(implicit cache: DataCacheConnector) = key match {
    case Some(k) => when {
      cache.save[T](any(), eqTo(k), eqTo(item))(any(), any())
    } thenReturn Future.successful(mockCacheMap)
    case _ => when {
      cache.save[T](any(), any(), eqTo(item))(any(), any())
    } thenReturn Future.successful(mockCacheMap)
  }

  def mockCacheRemoveByKey[T](item: T, key: Option[String] = None)(implicit cache: DataCacheConnector) = key match {
    case Some(k) => when {
      cache.removeByKey[T](any(), eqTo(k))(any(), any())
    } thenReturn Future.successful(mockCacheMap)
    case _ => when {
      cache.removeByKey[T](any(), any())(any(), any())
    } thenReturn Future.successful(mockCacheMap)
  }

  def mockCacheUpdate[T](key: Option[String] = None, dbModel: T)(implicit cache: DataCacheConnector) = key match {
    case Some(k) =>
      val funcCaptor = ArgumentCaptor.forClass(classOf[Option[T] => T])

      when {
        cache.update[T](any(), eqTo(k))(funcCaptor.capture())(any(), any())
      } thenAnswer new Answer[Future[Option[T]]] {
        override def answer(invocation: InvocationOnMock): Future[Option[T]] = {
          Future.successful(Some(funcCaptor.getValue()(Some(dbModel))))
        }
      }
  }

}
