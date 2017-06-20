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

package models.payments

import play.api.mvc.{Call, Request}

case class ReturnLocation(url: String, returnUrl: String)

object ReturnLocation {

  def apply(url: String)(implicit request: Request[_]) =
    new ReturnLocation(url, buildRedirectUrl(url))

  def apply(call: Call)(implicit request: Request[_]) =
    new ReturnLocation(call.url, buildRedirectUrl(call.url))

  private def scheme(request: Request[_]) = if (request.secure) "https" else "http"

  private def buildRedirectUrl(url: String)(implicit request: Request[_]): String = {
    "^localhost".r.findFirstIn(request.host) match {
      case Some(_) => s"${scheme(request)}://${request.host}$url"
      case _ => url
    }
  }
}
