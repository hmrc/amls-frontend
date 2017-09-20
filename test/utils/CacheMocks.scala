/*
 * Copyright 2017 HM Revenue & Customs
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
import org.mockito.Matchers.{eq => eqTo, any}
import org.mockito.Mockito.when

import scala.concurrent.Future

trait CacheMocks {

  def mockCacheFetch[T](item: Option[T], key: Option[String] = None)(implicit cache: DataCacheConnector) = key match {
    case Some(k) => when {
      cache.fetch[T](eqTo(k))(any(), any(), any())
    } thenReturn Future.successful(item)

    case _ => when {
      cache.fetch[T](any())(any(), any(), any())
    } thenReturn Future.successful(item)
  }

}
