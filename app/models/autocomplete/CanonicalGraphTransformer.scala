/*
 * Copyright 2018 HM Revenue & Customs
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

package models.autocomplete

import javax.inject.{Inject, Singleton}

import play.api.Environment
import play.api.libs.json._

@Singleton
class CanonicalGraphJsonLoader @Inject()(env: Environment) {
  private val fileName = "public/autocomplete/location-autocomplete-graph.json"

  def load = env.resourceAsStream(fileName) flatMap { stream =>
    val contents = scala.io.Source.fromInputStream(stream).mkString
    Json.parse(contents).asOpt[JsObject]
  }
}

@Singleton
class CanonicalGraphTransformer @Inject()(jsonLoader: CanonicalGraphJsonLoader) {

  def transform(whitelist: Set[String]): Option[JsObject] = {
    val filtered = jsonLoader.load.map {
      _.fields filter {
        case (code, _) => code.split(':')(1) match {
          case c if whitelist.contains(c) => true
          case _ => false
        }
        case _ => false
      } map { f =>
        (f._1, Json.toJsFieldJsValueWrapper(f._2))
      }
    }

    filtered map { f => Json.obj(f:_*) }
  }
}
