/*
 * Copyright 2021 HM Revenue & Customs
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

package models

import config.ApplicationConfig
import play.api.libs.json.{JsString, Writes}
import play.api.mvc.Call

case class ReturnLocation(url: String, absoluteUrl: String)

object ReturnLocation {

  def apply(url: String)(implicit appConfig: ApplicationConfig) =
    new ReturnLocation(url, publicRedirectUrl(url))

  def apply(call: Call)(implicit appConfig: ApplicationConfig) =
    new ReturnLocation(call.url, publicRedirectUrl(call.url))

  private def publicRedirectUrl(url: String)(implicit appConfig: ApplicationConfig) = s"${appConfig.frontendBaseUrl}$url"

  implicit val locationWrites = new Writes[ReturnLocation] {
    override def writes(o: ReturnLocation) = JsString(o.absoluteUrl)
  }

}
