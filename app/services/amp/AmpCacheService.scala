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

package services.amp

import connectors.DataCacheConnector
import javax.inject.Inject
import models.amp.Amp
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AmpCacheService @Inject()(cacheConnector: DataCacheConnector)
                               (implicit ec: ExecutionContext){

  def get(credId: String)(implicit hc: HeaderCarrier): Future[Option[JsValue]] = {
    for {
      amp <- cacheConnector.fetch[Amp](credId, Amp.key)
    } yield amp match {
      case Some(amp) => Some(Json.toJson(amp))
      case _ => None
    }
  }

  def set(credId: String, body: JsValue)(implicit hc: HeaderCarrier) = {
    val jsonObject: JsObject = body.as[JsObject]
    val ampData = jsonObject.value("data").as[JsObject]

    for {
      existing <- cacheConnector.fetch[Amp](credId, Amp.key)
      result   <- cacheConnector.save[Amp](credId, Amp.key, existing.getOrElse(Amp(ampData)))
    } yield result
  }
}
