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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.{Answer, OngoingStubbing}
import org.scalatestplus.mockito.MockitoSugar
import services.cache.Cache

import scala.concurrent.Future

trait CacheMocks extends MockitoSugar {

  implicit val mockCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  implicit val mockCacheMap: Cache                    = mock[Cache]

  mockCacheFetchAll

  def mockCacheFetch[T](item: Option[T], key: Option[String] = None)(implicit
    cache: DataCacheConnector
  ): OngoingStubbing[Future[Option[T]]] = key match {
    case Some(k) =>
      when {
        cache.fetch[T](any(), eqTo(k))(any())
      } thenReturn Future.successful(item)

    case _ =>
      when {
        cache.fetch[T](any(), any())(any())
      } thenReturn Future.successful(item)
  }

  def mockCacheGetEntry[T](item: Option[T], key: String): OngoingStubbing[Option[T]] = when {
    mockCacheMap.getEntry[T](eqTo(key))(any())
  } thenReturn item

  def mockCacheFetchAll(implicit cache: DataCacheConnector): Any = when {
    cache.fetchAll(any())
  } thenReturn Future.successful(Some(mockCacheMap))

  def mockCacheSave[T](implicit cache: DataCacheConnector): OngoingStubbing[Future[Cache]] = when {
    cache.save[T](any(), any(), any())(any())
  } thenReturn Future.successful(mockCacheMap)

  def mockCacheRemoveByKey[T](implicit cache: DataCacheConnector) = when {
    cache.removeByKey(any(), any())
  } thenReturn Future.successful(mockCacheMap)

  def mockCacheSave[T](item: T, key: Option[String] = None)(implicit
    cache: DataCacheConnector
  ): OngoingStubbing[Future[Cache]] = key match {
    case Some(k) =>
      when {
        cache.save[T](any(), eqTo(k), eqTo(item))(any())
      } thenReturn Future.successful(mockCacheMap)
    case _       =>
      when {
        cache.save[T](any(), any(), eqTo(item))(any())
      } thenReturn Future.successful(mockCacheMap)
  }

  def mockCacheUpdate[T](key: Option[String] = None, dbModel: T)(implicit
    cache: DataCacheConnector
  ): OngoingStubbing[Future[Option[T]]] = key match {
    case Some(k) =>
      val funcCaptor = ArgumentCaptor.forClass(classOf[Option[T] => T])

      when {
        cache.update[T](any(), eqTo(k))(funcCaptor.capture())(any())
      } thenAnswer new Answer[Future[Option[T]]] {
        override def answer(invocation: InvocationOnMock): Future[Option[T]] =
          Future.successful(Some(funcCaptor.getValue()(Some(dbModel))))
      }
  }

}
