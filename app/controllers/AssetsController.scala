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

package controllers

import com.google.inject.{Inject, Singleton}
import models.autocomplete.LocationGraphTransformer
import play.api.Environment
import play.api.http.HttpErrorHandler
import play.api.mvc.Result

@Singleton
class AssetsController @Inject() (
  errorHandler: HttpErrorHandler,
  env: Environment,
  transformer: LocationGraphTransformer,
  meta: AssetsMetadata
) extends AssetsBuilder(errorHandler, meta) {

  lazy val countriesJson = transformer
    .transform(
      models.countries.map(_.code).toSet ++ Set(
        "ENG",
        "GBN",
        "NIR",
        "SCT",
        "WLS",
        "Deutschland",
        "UK",
        "U.K"
      )
    )

  def countries = Action {
    countriesJson.fold[Result](InternalServerError) { j =>
      Ok(j.toString()).as("application/json")
    }
  }
}
