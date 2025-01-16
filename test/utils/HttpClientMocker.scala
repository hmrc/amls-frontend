/*
 * Copyright 2025 HM Revenue & Customs
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

import izumi.reflect.Tag
import org.scalamock.handlers.{CallHandler2, CallHandler4}
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import play.api.libs.ws.BodyWritable
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}


class HttpClientMocker extends MockFactory {

  val httpClient: HttpClientV2 = mock[HttpClientV2]("mockHttp")
  private val requestBuilder: RequestBuilder = mock[RequestBuilder]("mockRequestBuilder")

  def mockGet[Res: HttpReads](url: URL, response: Res): CallHandler2[HttpReads[Res], ExecutionContext, Future[Res]] = {
    (httpClient.get(_: URL)(_: HeaderCarrier)).expects(url, *).returning(requestBuilder)
    mockExecute(response)
  }

  def mockPost[B : Writes, Res: HttpReads](url: URL, requestBody: B, response: Res): CallHandler2[HttpReads[Res], ExecutionContext, Future[Res]] = {
    (httpClient.post(_: URL)(_: HeaderCarrier)).expects(url, *).returning(requestBuilder)
    mockWithBody(requestBody)
    mockExecute(response)
  }

  def mockPut[B : Writes, Res: HttpReads](url: URL, requestBody: B, response: Res): CallHandler2[HttpReads[Res], ExecutionContext, Future[Res]] = {
    (httpClient.put(_: URL)(_: HeaderCarrier)).expects(url, *).returning(requestBuilder)
    mockWithBody(requestBody)
    mockExecute(response)
  }

  private def mockExecute[T: HttpReads](response: T) =
    (requestBuilder.execute[T](_: HttpReads[T], _: ExecutionContext))
      .expects(*, *)
      .returning(Future.successful(response))

  private def mockWithBody[B : Writes](requestBody: B): CallHandler4[JsValue, BodyWritable[JsValue], Tag[JsValue], ExecutionContext, RequestBuilder] = {
    val jsonBody: JsValue = Json.toJson(requestBody)
    (requestBuilder
      .withBody(_: JsValue)(_: BodyWritable[JsValue], _: izumi.reflect.Tag[JsValue], _: ExecutionContext))
      .expects(jsonBody, *, *, *)
      .returning(requestBuilder)
  }

}
