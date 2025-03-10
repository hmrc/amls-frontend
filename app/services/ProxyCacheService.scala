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

package services

import connectors.DataCacheConnector
import models.amp.Amp
import models.eab.Eab
import play.api.libs.json.{JsObject, JsValue, Json}
import services.cache.Cache

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProxyCacheService @Inject() (cacheConnector: DataCacheConnector)(implicit ec: ExecutionContext) {

  // AMP
  def getAmp(credId: String): Future[Option[JsValue]] =
    for {
      amp <- cacheConnector.fetch[Amp](credId, Amp.key)
    } yield amp match {
      case Some(amp) => Some(Json.toJson(amp))
      case _         => None
    }

  def setAmp(credId: String, body: JsValue): Future[Cache] = {
    val jsonObject: JsObject = body.as[JsObject]
    val ampData              = jsonObject.value("data").as[JsObject]

    for {
      existing <- cacheConnector.fetch[Amp](credId, Amp.key)
      result   <- cacheConnector.save[Amp](credId, Amp.key, existing.getOrElse(Amp()).data(ampData))
    } yield result
  }

  // EAB
  def getEab(credId: String): Future[Option[JsValue]] =
    for {
      eab <- cacheConnector.fetch[Eab](credId, Eab.key)
    } yield eab match {
      case Some(eab) => Some(Json.toJson(eab))
      case _         => None
    }

  def setEab(credId: String, body: JsValue): Future[Cache] = {
    val jsonObject: JsObject = body.as[JsObject]
    val eabData              = jsonObject.value("data").as[JsObject]

    for {
      existing <- cacheConnector.fetch[Eab](credId, Eab.key)
      result   <- cacheConnector.save[Eab](credId, Eab.key, existing.getOrElse(Eab()).data(eabData))
    } yield result
  }
}
