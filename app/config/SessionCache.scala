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

package config

import com.google.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpReadsInstances.throwOnFailure
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http._
import uk.gov.hmrc.mongo.cache.CacheIdType.SessionCacheId.NoSessionException
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

// The code in this trait was migrated verbatim from http-caching-client.
// This supports access to keystore to clear the business-customer-frontend keystore cache, which
// is currently required for acceptance testing purposes.
trait SessionCache {
  def httpClientV2: HttpClientV2
  def defaultSource: String
  def baseUri: String
  def domain: String

  private val noSession = Future.failed[String](NoSessionException)

  val legacyRawReads: HttpReads[HttpResponse] =
    throwOnFailure(readEitherOf(readRaw))

  private def cacheId(implicit hc: HeaderCarrier): Future[String] =
    hc.sessionId.fold(noSession)(c => Future.successful(c.value))

  protected def buildUri(source: String, id: String): String =
    s"$baseUri/$domain/$source/$id"

  def delete(uri: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val url = new URL(uri)
    httpClientV2
      .delete(url"$url")
      .execute[HttpResponse](implicitly[HttpReads[HttpResponse]], implicitly[ExecutionContext])
  }

  def remove()(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[HttpResponse] =
    for {
      c <- cacheId
      result <- delete(buildUri(defaultSource, c))
    } yield result
}

class BusinessCustomerSessionCache @Inject()(val configuration: Configuration,
                                             val httpClientV2: HttpClientV2)
                                             extends ServicesConfig(configuration) with SessionCache {
  override def defaultSource: String = getConfString("cachable.session-cache.review-details.cache", "business-customer-frontend")
  override def baseUri = baseUrl("cachable.session-cache")
  override def domain = getConfString("cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))
}


